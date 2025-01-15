package SN.BANK.payment.service;

import SN.BANK.common.exception.CustomException;
import SN.BANK.common.exception.ErrorCode;
import SN.BANK.domain.Account;
import SN.BANK.domain.enums.PaymentStatus;
import SN.BANK.payment.entity.PaymentList;
import SN.BANK.domain.enums.PaymentTag;
import SN.BANK.payment.dto.PaymentListResponseDto;
import SN.BANK.payment.repository.PaymentListRepository;
import SN.BANK.payment.dto.PaymentRequestDto;
import SN.BANK.payment.dto.PaymentResponseDto;
import SN.BANK.account.repository.AccountRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor

public class PaymentService {

    private final ExchangeRateService exchangeRateService;
    private final AccountRepository accountRepository;
    private final PaymentListRepository paymentListRepository;

    @Transactional(rollbackFor = Exception.class)
    public PaymentResponseDto makePayment(PaymentRequestDto request) {

        // 출금 계좌 확인
        Account withdrawAccount = accountRepository.findById(request.getWithdrawId())
                .orElseThrow(() -> new CustomException(ErrorCode.NOT_FOUND_ACCOUNT));

        // 계좌 비밀번호 확인
        if (!withdrawAccount.getPassword().equals(request.getPassword())) {
            throw new CustomException(ErrorCode.INVALID_PASSWORD);
        }
        // 잔액 확인
        if (withdrawAccount.getMoney().compareTo(request.getAmount()) < 0) {
            throw new CustomException(ErrorCode.INSUFFICIENT_BALANCE);
        }
        // 입금 계좌 확인
        Account depositAccount = accountRepository.findById(request.getDepositId())
                .orElseThrow(() -> new CustomException(ErrorCode.NOT_FOUND_ACCOUNT));
        // 환율 가져오기
         BigDecimal exchangeRate = exchangeRateService.getExchangeRate(withdrawAccount.getCurrency(), depositAccount.getCurrency());
        // 출금 처리
        withdrawAccount.withdraw((request.getAmount().multiply(exchangeRate)));
        // 입금 처리
        depositAccount.deposit((request.getAmount()));
        // 결제내역 생성
        PaymentList payment = PaymentList.builder()
                .paidAt(LocalDateTime.now())
                .paymentTag(PaymentTag.결제)
                .withdrawId(withdrawAccount.getId())
                .depositId(depositAccount.getId())
                .amount(request.getAmount()) //입금계좌 기준 금액
                .exchangeRate(exchangeRate)
                .currency(depositAccount.getCurrency())
                .paymentStatus(PaymentStatus.결제완료)
                .build();

        paymentListRepository.save(payment);

        return new PaymentResponseDto(payment.getId());
    }
    @Transactional(rollbackFor = Exception.class)
    public void refundPayment(Long paymentId) {
        // 결제 내역 조회
        PaymentList paymentList = paymentListRepository.findById(paymentId)
                .orElseThrow(() -> new CustomException(ErrorCode.NOT_FOUND_ACCOUNT));

        // 이미 결제 취소된 상태인지 확인
        if (paymentList.getPaymentStatus() == PaymentStatus.결제취소) {
            throw new CustomException(ErrorCode.PAYMENT_ALREADY_CANCELLED);
        }

        // 출금 계좌와 입금 계좌 조회
        Account withdrawAccount = accountRepository.findById(paymentList.getWithdrawId())
                .orElseThrow(() -> new CustomException(ErrorCode.NOT_FOUND_ACCOUNT));
        Account depositAccount = accountRepository.findById(paymentList.getDepositId())
                .orElseThrow(() -> new CustomException(ErrorCode.NOT_FOUND_ACCOUNT));

        // 잔액 확인
        if (depositAccount.getMoney().compareTo(paymentList.getAmount()) < 0 ) {
            throw new CustomException(ErrorCode.INSUFFICIENT_BALANCE);
        }
        // 계좌 업데이트
        depositAccount.withdraw(paymentList.getAmount()); // 입금 계좌에서 원래 금액 차감
        withdrawAccount.deposit( (paymentList.getAmount().multiply(paymentList.getExchangeRate()))); // 출금 계좌에 환불 금액 추가

        // 결제 상태 변경
        paymentList.setPaymentStatus(PaymentStatus.결제취소);

    }


    public List<PaymentListResponseDto> getUserPaymentHistory(Long userId) {
        if(userId==null){
            throw new CustomException(ErrorCode.NOT_FOUND_USER);
        }
        // 유저의 계좌 ID 목록 조회
        List<Long> userAccountIds = accountRepository.findAllByUser_Id(userId)
                .stream()
                .map(Account::getId)
                .collect(Collectors.toList());

        // 해당 계좌들과 관련된 결제 내역 조회
        return paymentListRepository.findAllByWithdrawIdIn(userAccountIds).stream()
                .map(PaymentListResponseDto::of)
                .collect(Collectors.toList());
    }
}


