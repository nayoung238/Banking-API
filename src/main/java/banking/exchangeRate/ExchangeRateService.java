package banking.exchangeRate;

import banking.account.enums.Currency;
import banking.common.exception.CustomException;
import banking.common.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

@RequiredArgsConstructor
@Service
@Slf4j
public class ExchangeRateService {

    private final ExchangeRateNaverService naverService;
    private final ExchangeRateMananaService mananaService;
    private final ExchangeRateGoogleFinanceScraper googleFinanceScraper;

    private final ConcurrentHashMap<Currency, ReentrantLock> currencyLocks = new ConcurrentHashMap<>();
    private final Map<Currency, ExchangeRateStatus> exchangeRateResults = new ConcurrentHashMap<>();

    // TODO: 동적 변경 예정
    private final long CACHE_EXPIRY_TIME = 2000;

    private DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
        .withZone(ZoneId.systemDefault());

    public BigDecimal getExchangeRate(Currency fromCurrency, Currency toCurrency) {
        if (fromCurrency.equals(toCurrency)) { //둘다 원화일 경우
            return BigDecimal.ONE;
        }

        Currency baseCurrency, quoteCurrency;
        boolean isInverse = false; // 역수 계산 여부 플래그

        if (fromCurrency.equals(Currency.KRW)) {  // 원화 -> 외화
            baseCurrency = toCurrency;
            quoteCurrency = fromCurrency;
        } else if (toCurrency.equals(Currency.KRW)) {  // 외화 -> 원화
            baseCurrency = fromCurrency;
            quoteCurrency = toCurrency;
            isInverse = true; // 역수 계산 필요
        } else {
            throw new CustomException(ErrorCode.EXCHANGE_RATE_FETCH_FAIL); // 외화 -> 외화
        }

        BigDecimal exchangeRate = null;
        long currentTime = System.currentTimeMillis();
        long timeout = 4000;
        long endTime = currentTime + timeout;
        boolean timeoutFlag = true;

        while (System.currentTimeMillis() < endTime) {
            // 로컬 캐시 사용
            if (isAvailableExchangeRate(baseCurrency)) {
                exchangeRate = getUpdatedExchangeRate(baseCurrency);
                timeoutFlag = false;
                break;
            }

            // 실시간 환율 데이터 업데이트 (스레드 1개로 제한)
            updateExchangeRate(baseCurrency, quoteCurrency);

            try {
                if (!isAvailableExchangeRate(baseCurrency)) {
                    // TODO: Context Switching 문제로 시간 변경 예정
                    Thread.sleep(150);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new RuntimeException("환율 조회 중 인터럽트 발생", e);
            }
        }

        if (timeoutFlag || exchangeRate.compareTo(BigDecimal.ZERO) <= 0) {
            log.warn("[ Timeout ] 현재 실시간 환율을 이용할 수 없습니다. " + Thread.currentThread().getName());
            throw new CustomException(ErrorCode.EXCHANGE_RATE_FETCH_FAIL);
        }

        return isInverse ? BigDecimal.ONE.divide(exchangeRate, 20, BigDecimal.ROUND_HALF_UP) : exchangeRate;
    }

    private boolean isAvailableExchangeRate(Currency currency) {
        return exchangeRateResults.containsKey(currency)
            && System.currentTimeMillis() - exchangeRateResults.get(currency).lastCachedTime < CACHE_EXPIRY_TIME;
    }

    private BigDecimal getUpdatedExchangeRate(Currency currency) {
        return exchangeRateResults.get(currency).exchangeRate;
    }

    private void updateExchangeRate(Currency baseCurrency, Currency quoteCurrency) {
        if(isAvailableExchangeRate(baseCurrency)) {
            return;
        }

        ReentrantLock lock = currencyLocks.computeIfAbsent(baseCurrency, k -> new ReentrantLock());
        if(lock.tryLock()) {
            try {
                // double checking
                if (isAvailableExchangeRate(baseCurrency)) {
                    return;
                }

                CompletableFuture
                    .supplyAsync(() -> naverService.getExchangeRate(baseCurrency, quoteCurrency))
                    .orTimeout(CACHE_EXPIRY_TIME, TimeUnit.MILLISECONDS)
                    .thenApply(result -> new BigDecimal(result.toString())
                        .setScale(2, RoundingMode.CEILING))
                    .thenApply(exchangeRate -> {
                        updateExchangeRateStatus(baseCurrency, exchangeRate);

                        log.info("[Main(Naver)] {} 환율 {} 업데이트, {}",
                            quoteCurrency,
                            exchangeRate,
                            formatter.format(Instant.ofEpochMilli(exchangeRateResults.get(baseCurrency).lastCachedTime)));
                        return exchangeRate;
                    })
                    .exceptionally(ex -> {
                        log.error("Naver API failed or timed out, calling Manana and Google... {}", ex.getMessage());
                        fallbackUpdate(baseCurrency, quoteCurrency);
                        return null;
                    }).join();
            } finally {
                lock.unlock();
            }
        } else {
            log.info("다른 스레드에 의해 {} 단위가 업데이트 중입니다.", baseCurrency);
        }
    }

    private void fallbackUpdate(Currency baseCurrency, Currency quoteCurrency) {
        CompletableFuture
            .anyOf(
//                CompletableFuture
//                    .supplyAsync(() -> mananaService.getExchangeRate(baseCurrency, quoteCurrency))
//                    .orTimeout(CACHE_EXPIRY_TIME, TimeUnit.MILLISECONDS),

                CompletableFuture
                    .supplyAsync(() -> googleFinanceScraper.getExchangeRate(baseCurrency, quoteCurrency))
                    .orTimeout(CACHE_EXPIRY_TIME, TimeUnit.MILLISECONDS))

            .thenApply(result -> new BigDecimal(result.toString())
                .setScale(2, RoundingMode.CEILING))
            .thenApply(exchangeRate -> {
                updateExchangeRateStatus(baseCurrency, exchangeRate);

                log.info("[Fallback] {} 환율 {} 업데이트, {}",
                    baseCurrency,
                    exchangeRate,
                    formatter.format(Instant.ofEpochMilli(exchangeRateResults.get(baseCurrency).lastCachedTime)));

                return exchangeRate;
            })
            .exceptionally(ex -> {
                log.error(ex.getMessage());
                throw new RuntimeException("Unable to fetch exchange rate", ex);
            })
            .join();
    }

    private void updateExchangeRateStatus(Currency baseCurrency, BigDecimal exchangeRate) {
        ExchangeRateStatus status = exchangeRateResults.computeIfAbsent(baseCurrency, k -> new ExchangeRateStatus());
        status.exchangeRate = exchangeRate;
        status.lastCachedTime = System.currentTimeMillis();
    }

    class ExchangeRateStatus {
        BigDecimal exchangeRate;
        Long lastCachedTime;

        public ExchangeRateStatus() {}
    }
}
