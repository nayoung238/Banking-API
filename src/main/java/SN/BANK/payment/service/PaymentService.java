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
import SN.BANK.payment.tempRepository.TempAccountRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PaymentService {

    private final ExchangeRateService exchangeRateService;
    private final TempAccountRepository tempAccountRepository;
    private final PaymentListRepository paymentListRepository;

    @Transactional
    public PaymentResponseDto makePayment(PaymentRequestDto request) {

        // 출금 계좌 확인
        Account withdrawAccount = tempAccountRepository.findById(request.getWithdrawId())
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
        Account depositAccount = tempAccountRepository.findById(request.getDepositId())
                .orElseThrow(() -> new CustomException(ErrorCode.NOT_FOUND_ACCOUNT));
        // 환율 계산
         BigDecimal exchangeRate = exchangeRateService.getExchangeRate(withdrawAccount.getCurrency(), depositAccount.getCurrency());
        // 출금 처리
        withdrawAccount.withdraw((request.getAmount().multiply(exchangeRate)).setScale(0, RoundingMode.DOWN)); //원화는 소수점 버리고 출금
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

    public void refundPayment(Long paymentId) {
        // 결제 내역 조회
        PaymentList paymentList = paymentListRepository.findById(paymentId)
                .orElseThrow(() -> new CustomException(ErrorCode.NOT_FOUND_ACCOUNT));

        // 출금 계좌와 입금 계좌 조회
        Account withdrawAccount = tempAccountRepository.findById(paymentList.getWithdrawId())
                .orElseThrow(() -> new CustomException(ErrorCode.NOT_FOUND_ACCOUNT));
        Account depositAccount = tempAccountRepository.findById(paymentList.getDepositId())
                .orElseThrow(() -> new CustomException(ErrorCode.NOT_FOUND_ACCOUNT));

        // 잔액 확인
        if (depositAccount.getMoney().compareTo(paymentList.getAmount()) < 0 ) {
            throw new CustomException(ErrorCode.INSUFFICIENT_BALANCE);
        }
        // 계좌 업데이트
        depositAccount.withdraw(paymentList.getAmount()); // 입금 계좌에서 원래 금액 차감
        withdrawAccount.deposit( (paymentList.getAmount().multiply(paymentList.getExchangeRate()).setScale(0,RoundingMode.DOWN))); // 출금 계좌에 환불 금액 추가

        // 결제 상태 변경
        paymentList.setPaymentStatus(PaymentStatus.결제취소);

    }


    public List<PaymentListResponseDto> getUserPaymentHistory(Long userId) {
        if(userId==null){
            throw new CustomException(ErrorCode.NOT_FOUND_USER);
        }
        // 유저의 계좌 ID 목록 조회
        List<Long> userAccountIds = tempAccountRepository.findAllByUser_Id(userId)
                .stream()
                .map(Account::getId)
                .collect(Collectors.toList());

        // 해당 계좌들과 관련된 결제 내역 조회
        return paymentListRepository.findAllByWithdrawIdIn(userAccountIds).stream()
                .map(PaymentListResponseDto::of)
                .collect(Collectors.toList());
    }
}


