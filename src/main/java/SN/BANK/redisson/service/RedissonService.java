package SN.BANK.redisson.service;

import SN.BANK.account.entity.Account;
import SN.BANK.account.repository.AccountRepository;
import SN.BANK.common.exception.CustomException;
import SN.BANK.common.exception.ErrorCode;
import SN.BANK.payment.dto.request.PaymentRefundRequestDto;
import SN.BANK.payment.dto.request.PaymentRequestDto;
import SN.BANK.payment.entity.PaymentList;
import SN.BANK.payment.service.PaymentService;
import SN.BANK.transaction.dto.request.TransactionRequest;
import SN.BANK.transaction.dto.response.TransactionResponse;
import SN.BANK.transaction.service.TransactionService;
import lombok.RequiredArgsConstructor;
import org.redisson.RedissonMultiLock;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

//@Service
//@RequiredArgsConstructor
//public class RedissonService {
//    private final TransactionService transactionService;
//    private final RedissonClient redissonClient;
//    private final PaymentService paymentService;
//    private final AccountRepository accountRepository;
//
//    public TransactionResponse createTransactionWithRedisson(Long userId, TransactionRequest transactionRequest){
//        // 락 설정
//        // 여기선 2개의 락을 동시에 잡기에 계좌 id에 대한 락을 멀티락으로 한 번에 잠금
//        String firstLock = "accountLock: " + Math.max(transactionRequest.receiverAccountId(), transactionRequest.senderAccountId());
//        String secondLock = "accountLock: " + Math.min(transactionRequest.receiverAccountId(), transactionRequest.senderAccountId());
//        RLock lock1 = redissonClient.getLock(firstLock);
//        RLock lock2 = redissonClient.getLock(secondLock);
//
//        RedissonMultiLock multiLock = new RedissonMultiLock(lock1,lock2);
//
//        try{//락 획득 시도 tryLock(최대 대기 시간, 최대 락 점유 시간)
//            if(!multiLock.tryLock(30, 30, TimeUnit.SECONDS)){
//                throw new RuntimeException("락 획득 실패");
//            }
//            //작업 코드 들어갈 곳
//            return transactionService.createTransactionNonLock(userId,transactionRequest);
//        } catch (InterruptedException e) {
//            System.out.println(e);
//            throw new RuntimeException(e);
//        } finally {//락 해제
//            if(multiLock.isHeldByCurrentThread()){
//                multiLock.unlock();
//            }
//        }
//    }
//
//    public Long makePaymentWithRedisson(PaymentRequestDto request){
//        Account firstAccount = accountRepository.findByAccountNumber(request.withdrawAccountNumber()).orElseThrow(() -> new CustomException(ErrorCode.NOT_FOUND_ACCOUNT));
//        Account secondAccount = accountRepository.findByAccountNumber(request.depositAccountNumber()).orElseThrow(() -> new CustomException(ErrorCode.NOT_FOUND_ACCOUNT));
//        // 락 설정
//        String firstLock = "accountLock: " + Math.max(firstAccount.getId(), secondAccount.getId());
//        String secondLock = "accountLock: " + Math.min(firstAccount.getId(), secondAccount.getId());
//        RLock lock1 = redissonClient.getLock(firstLock);
//        RLock lock2 = redissonClient.getLock(secondLock);
//
//        RedissonMultiLock multiLock = new RedissonMultiLock(lock1,lock2);
//
//        try{//락 획득 시도 tryLock(최대 대기 시간, 최대 락 점유 시간)
//            if(!multiLock.tryLock(30, 30, TimeUnit.SECONDS)){
//                throw new RuntimeException("락 획득 실패");
//            }
//            //작업 코드 들어갈 곳
//            return paymentService.makePaymentNonLock(request);
//        } catch (InterruptedException e) {
//            System.out.println(e);
//            throw new RuntimeException(e);
//        } finally {//락 해제
//            if(multiLock.isHeldByCurrentThread()){
//                multiLock.unlock();
//            }
//        }
//    }
//
//    public void refundPaymentWithRedisson(PaymentRefundRequestDto request){
//        // 결제 내역 조회
//        PaymentList paymentList = paymentService.getPaymentById(request.paymentId());
//
//        // 출금 계좌
//        Account firstAccount = accountRepository.findByAccountNumber(paymentList.getWithdrawAccountNumber())
//                .orElseThrow(() -> new CustomException(ErrorCode.NOT_FOUND_ACCOUNT));
//        // 입금 계좌
//        Account secondAccount = accountRepository.findByAccountNumberWithLock(paymentList.getDepositAccountNumber())
//                .orElseThrow(() -> new CustomException(ErrorCode.NOT_FOUND_ACCOUNT));
//
//        // 락 설정
//        String firstLock = "accountLock: " + Math.max(firstAccount.getId(), secondAccount.getId());
//        String secondLock = "accountLock: " + Math.min(firstAccount.getId(), secondAccount.getId());
//        RLock lock1 = redissonClient.getLock(firstLock);
//        RLock lock2 = redissonClient.getLock(secondLock);
//
//        RedissonMultiLock multiLock = new RedissonMultiLock(lock1,lock2);
//
//        try{//락 획득 시도 tryLock(최대 대기 시간, 최대 락 점유 시간)
//            if(!multiLock.tryLock(30, 30, TimeUnit.SECONDS)){
//                throw new RuntimeException("락 획득 실패");
//            }
//            //작업 코드 들어갈 곳
//            paymentService.refundPayment(request);
//        } catch (InterruptedException e) {
//            System.out.println(e);
//            throw new RuntimeException(e);
//        } finally {//락 해제
//            if(multiLock.isHeldByCurrentThread()){
//                multiLock.unlock();
//            }
//        }
//    }
//}
