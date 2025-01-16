package SN.BANK.transfer.service;

import SN.BANK.account.entity.Account;
import SN.BANK.account.service.AccountService;
import SN.BANK.common.exception.CustomException;
import SN.BANK.common.exception.ErrorCode;
import SN.BANK.payment.entity.InoutList;
import SN.BANK.payment.entity.PaymentList;
import SN.BANK.payment.enums.InoutTag;
import SN.BANK.payment.enums.PaymentTag;
import SN.BANK.payment.repository.PaymentListRepository;
import SN.BANK.transfer.dto.request.TransferRequest;
import SN.BANK.transfer.dto.response.PaymentListResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
public class TransferService {

    private final AccountService accountService;
    private final PaymentListRepository paymentListRepository;
//    private final ExchangeRateService exchangeRateService;

    /**
     * 이체 기능
     */
    @Transactional
    public PaymentListResponse transfer(TransferRequest transferRequest) {

        BigDecimal amount = transferRequest.getAmount();

        // 1. from 계좌 검증
        // 1-1. 유효한 계좌인지
        // 1-2. 계좌 비밀번호가 맞는지 (+ 데이터 암복호화 기능 추가해야 함)
        // 1-3. 잔액이 보내려는 금액보다 크거나 같은지
        Account fromAccount = accountService.findValidAccount(transferRequest.getFromAccountId());

        if (!fromAccount.getPassword().equals(transferRequest.getAccountPassword())) {
            throw new CustomException(ErrorCode.INVALID_PASSWORD);
        }

        if (!isGreaterThanAmount(fromAccount, amount)) {
            throw new CustomException(ErrorCode.INSUFFICIENT_MONEY);
        }

        // 2. to 계좌 검증
        // 2-1. 유효한 계좌인지
        // 2-2. to 계좌가 from 계좌와 같은지
        Account toAccount = accountService.findValidAccount(transferRequest.getToAccountId());
        if (toAccount.equals(fromAccount)) {
            throw new CustomException(ErrorCode.INVALID_TRANSFER);
        }

        // 3. 환율 계산 (외부 API 이용)
        BigDecimal exchangeRate = BigDecimal.ONE;
        BigDecimal convertedAmount = amount;

        // 통화가 다른 경우에만 변경
        if (!fromAccount.getCurrency().equals(toAccount.getCurrency())) {
            /*
            from 계좌의 통화를 기준으로 (1) to 계좌의 통화 환율을 반환한다.
            ex) from 통화: KRW, to 통화: USD
            exchangeRate = 0.00068; -> 1원 당 0.00068 달러 의미
            즉, amount 가 10,000(won) 이면 convertedAmount 는 6.80($)가 됨
             */
//            exchangeRate = exchangeRateService.getExchangeRate(fromAccount.getCurrency(), toAccount.getCurrency());
//            convertedAmount = amount.multiply(exchangeRate).setScale(2, RoundingMode.HALF_UP); // HALF_UP: 반올림
        }

        // 4. 금액 변경
        BigDecimal restMoney = fromAccount.getMoney().subtract(amount);
        fromAccount.changeMoney(restMoney);

        BigDecimal addedMoney = toAccount.getMoney().add(convertedAmount);
        toAccount.changeMoney(addedMoney);

        // 5. 거래 내역 생성
        // 5-1. 입출금 내역이 들어갈 거래 내역
        // 5-2. 출금 내역
        // 5-3. 입금 내역

        PaymentList paymentList =
                new PaymentList(PaymentTag.이체, fromAccount.getId(), toAccount.getId(),
                        convertedAmount, exchangeRate, toAccount.getCurrency());

        InoutList outList = InoutList.builder()
                .inoutTag(InoutTag.출금)
                .amount(amount)
                .balance(restMoney)
                .accountId(fromAccount.getId())
                .build();

        InoutList inList = InoutList.builder()
                .inoutTag(InoutTag.입금)
                .amount(convertedAmount)
                .balance(addedMoney)
                .accountId(toAccount.getId())
                .build();

        paymentList.addInoutList(outList);
        paymentList.addInoutList(inList);

        PaymentList savedPaymentList = paymentListRepository.save(paymentList);

        return PaymentListResponse.of(savedPaymentList, restMoney);
    }

    public boolean isGreaterThanAmount(Account account, BigDecimal amount) {
        return account.getMoney().compareTo(amount) >= 0;
    }

}
