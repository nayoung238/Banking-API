package banking.exchangeRate;

import banking.account.enums.Currency;
import banking.common.exception.CustomException;
import banking.common.exception.ErrorCode;
import feign.FeignException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.concurrent.*;
import java.util.concurrent.locks.ReentrantLock;

@RequiredArgsConstructor
@Service
@Slf4j
public class ExchangeRateService {

    private final ExchangeRateNaverService naverService;
    private final ExchangeRateMananaService mananaService;
    private final ExchangeRateGoogleFinanceScraper googleFinanceScraper;

    private static final Map<String, ReentrantLock> currencyLocks = new ConcurrentHashMap<>();
    private static final Map<String, CompletableFuture<BigDecimal>> currencyFutures = new ConcurrentHashMap<>();
    private static final Map<String, ExchangeRateStatus> exchangeRateResults = new ConcurrentHashMap<>();

    private static final long TRY_LOCK_TIMEOUT = 1;
    private static final long CACHE_EXPIRY_TIME = 20000;
    private static final long FUTURE_TIMEOUT = 10000;

    private DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
        .withZone(ZoneId.systemDefault());

    public BigDecimal getExchangeRate(Currency baseCurrency, Currency quoteCurrency) {
        if (baseCurrency.equals(quoteCurrency)) {
            return BigDecimal.ONE;
        }

        if(isAvailableExchangeRate(baseCurrency, quoteCurrency)) {
            return exchangeRateResults.get(getCurrencyPairKey(baseCurrency, quoteCurrency)).exchangeRate;
        }

        String currencyPairKey = getCurrencyPairKey(baseCurrency, quoteCurrency);
        ReentrantLock lock = currencyLocks.computeIfAbsent(currencyPairKey, k -> new ReentrantLock());
        CompletableFuture<BigDecimal> future = currencyFutures.computeIfAbsent(currencyPairKey, k -> new CompletableFuture<>());
        try {
            if (lock.tryLock(TRY_LOCK_TIMEOUT, TimeUnit.MILLISECONDS)) {
                return fetchPrimaryExchangeRate(baseCurrency, quoteCurrency, lock, future);
            } else {
                return monitorExchangeRateUpdate(baseCurrency, quoteCurrency, lock, future);
            }
        } catch (InterruptedException e) {
            log.error("{} ReentrantLock 획득 중 스레드 인터럽트 발생", baseCurrency, e);
            Thread.currentThread().interrupt();
            throw new CustomException(ErrorCode.EXCHANGE_RATE_FETCH_FAIL);
        } catch (Exception e) {
            log.error("{} 환율 조회 중 예상치 못한 예외 발생", baseCurrency, e);
            throw new CustomException(ErrorCode.EXCHANGE_RATE_FETCH_FAIL);
        }
    }

    private BigDecimal fetchPrimaryExchangeRate(Currency baseCurrency, Currency quoteCurrency, ReentrantLock lock, CompletableFuture<BigDecimal> future) {
        String currencyPairKey = getCurrencyPairKey(baseCurrency, quoteCurrency);
        BigDecimal exchangeRate = null;
        try {
            exchangeRate = naverService.getExchangeRate(baseCurrency, quoteCurrency);
            updateExchangeRateStatus(baseCurrency, quoteCurrency, exchangeRate);

            log.info("[Main(Naver)] {}/{} 환율 {} 업데이트, {}",
                baseCurrency, quoteCurrency,
                exchangeRate,
                formatter.format(Instant.ofEpochMilli(exchangeRateResults.get(currencyPairKey).lastCachedTime)));
        } catch (FeignException e) {
            log.error("Naver API failed or timed out for {}/{}. Calling Manana and Google... {}", baseCurrency, quoteCurrency, e.getMessage());
            CompletableFuture<BigDecimal> fallbackFuture = fetchFallbackExchangeRate(baseCurrency, quoteCurrency);
            try {
                exchangeRate = fallbackFuture.join();
            } catch (CompletionException ex) {
                if (ex.getCause() instanceof CustomException) {
                    throw (CustomException) ex.getCause();
                } else {
                    log.error("Unexpected error during fallback exchange rate fetch", ex.getCause());
                    throw new CustomException(ErrorCode.EXCHANGE_RATE_FETCH_FAIL);
                }
            }
        } catch (Exception e) {
            log.error("{}/{} 환율 데이터를 가져올 수 없습니다.", baseCurrency, quoteCurrency, e);
            throw new CustomException(ErrorCode.EXCHANGE_RATE_FETCH_FAIL);
        } finally {
            if (exchangeRate != null) {
                future.complete(exchangeRate);
            } else {
                future.completeExceptionally(new CustomException(ErrorCode.EXCHANGE_RATE_FETCH_FAIL));
            }
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
        return exchangeRate;
    }

    private CompletableFuture<BigDecimal> fetchFallbackExchangeRate(Currency baseCurrency, Currency quoteCurrency) {
        String currencyPairKey = getCurrencyPairKey(baseCurrency, quoteCurrency);

        return CompletableFuture
            .anyOf(
                CompletableFuture
                    .supplyAsync(() -> mananaService.getExchangeRate(baseCurrency, quoteCurrency)),  // Timeout: 5000ms
                CompletableFuture
                    .supplyAsync(() -> googleFinanceScraper.getExchangeRate(baseCurrency, quoteCurrency))  // Timeout: 5000ms
            )
            .thenApply(result -> (BigDecimal) result)
            .thenApply(exchangeRate -> {
                updateExchangeRateStatus(baseCurrency, quoteCurrency, exchangeRate);

                log.info("[Fallback] {}/{} 환율 {} 업데이트, {}",
                    baseCurrency, quoteCurrency,
                    exchangeRateResults.get(currencyPairKey).exchangeRate,
                    formatter.format(Instant.ofEpochMilli(exchangeRateResults.get(currencyPairKey).lastCachedTime)));

                return exchangeRate;
            })
            .exceptionally(ex -> {
                log.error(ex.getMessage());
                throw new CustomException(ErrorCode.EXCHANGE_RATE_FETCH_FAIL);
            });
    }

    private BigDecimal monitorExchangeRateUpdate(Currency baseCurrency, Currency quoteCurrency, ReentrantLock lock, CompletableFuture<BigDecimal> future) throws InterruptedException {
        // double checking
        if(isAvailableExchangeRate(baseCurrency, quoteCurrency)) {
            return exchangeRateResults.get(getCurrencyPairKey(baseCurrency, quoteCurrency)).exchangeRate;
        }

        log.info("다른 스레드에 의해 {}/{} 환율이 업데이트 중입니다.", baseCurrency, quoteCurrency);

        BigDecimal exchangeRate = null;
        try {
             exchangeRate = future.orTimeout(FUTURE_TIMEOUT, TimeUnit.MILLISECONDS)
                .thenApply(result -> {
                    log.info("업데이트된 환율({}/{}: {}) 사용", baseCurrency, quoteCurrency, result);
                    return result;
                })
                .exceptionally(ex -> {
                    log.warn("{}/{} 환율 업데이트 대기 시간 초과, 직접 환율 Open API 시도", baseCurrency, quoteCurrency);
                    try {
                        // 생산자 스레드의 알 수 없는 오류로 직접 Open API 직접 호출, 소비자 -> 생산자 역할 전환
                        if (lock.tryLock(TRY_LOCK_TIMEOUT, TimeUnit.MILLISECONDS)) {
                            return fetchPrimaryExchangeRate(baseCurrency, quoteCurrency, lock, future);
                        } else {  // ReentrantLock 획득 실패, 마지막 대기 시도
                            return future.get(FUTURE_TIMEOUT, TimeUnit.MILLISECONDS);
                        }
                    } catch (InterruptedException e) {
                        log.error("ReentrantLock 획득 중 인터럽트 발생", e);
                        Thread.currentThread().interrupt();
                        throw new CustomException(ErrorCode.EXCHANGE_RATE_FETCH_FAIL);
                    } catch (TimeoutException e) {
                        log.error("환율 데이터 대기 시간 초과", e);
                        throw new CustomException(ErrorCode.EXCHANGE_RATE_FETCH_FAIL);
                    } catch (Exception e) {
                        log.error("환율 조회 중 예상치 못한 예외 발생", e);
                        throw new CustomException(ErrorCode.EXCHANGE_RATE_FETCH_FAIL);
                    }
                }).join();
        } catch (CompletionException e) {
            throw e.getCause() instanceof CustomException ? (CustomException) e.getCause() : e;
        } finally {
            if (exchangeRate != null) {
                future.complete(exchangeRate);
            } else {
                future.completeExceptionally(new CustomException(ErrorCode.EXCHANGE_RATE_FETCH_FAIL));
            }
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
        return exchangeRate;
    }

    private void updateExchangeRateStatus(Currency baseCurrency, Currency quoteCurrency, BigDecimal exchangeRate) {
        ExchangeRateStatus status = exchangeRateResults.computeIfAbsent(getCurrencyPairKey(baseCurrency, quoteCurrency), k -> new ExchangeRateStatus());
        status.exchangeRate = exchangeRate;
        status.lastCachedTime = System.currentTimeMillis();
    }

    private boolean isAvailableExchangeRate(Currency baseCurrency, Currency quoteCurrency) {
        String currencyPairKey = getCurrencyPairKey(baseCurrency, quoteCurrency);
        return exchangeRateResults.containsKey(currencyPairKey)
            && System.currentTimeMillis() - exchangeRateResults.get(currencyPairKey).lastCachedTime < CACHE_EXPIRY_TIME;
    }

    private String getCurrencyPairKey(Currency baseCurrency, Currency quoteCurrency) {
        return baseCurrency + "/" + quoteCurrency;
    }

    static class ExchangeRateStatus {
        BigDecimal exchangeRate;
        Long lastCachedTime;

        public ExchangeRateStatus() {}
    }
}
