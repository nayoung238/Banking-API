package SN.BANK.cc;

import SN.BANK.account.entity.Account;
import SN.BANK.account.enums.Currency;
import SN.BANK.account.repository.AccountRepository;
import SN.BANK.transfer.dto.request.TransferRequestDto;
import SN.BANK.transfer.service.TransferService;
import SN.BANK.users.entity.Users;
import SN.BANK.users.repository.UsersRepository;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.junit.jupiter.api.Assertions.assertEquals;

//@SpringBootTest
//@ActiveProfiles("test")
class ConcurrentControlTest {

//    @Autowired
//    private AccountRepository accountRepository;
//
//    @Autowired
//    private UsersRepository usersRepository;
//
//    @Autowired
//    private TransferService transferService;
//
//    Users users1;
//    Users users2;
//    Account sender;
//    Account receiver;
//
//    @BeforeEach
//    void setUp() {
//        users1 = Users.builder()
//                .name("테스터1")
//                .loginId("test1234")
//                .password("test1234")
//                .build();
//
//        users2 = Users.builder()
//                .name("테스터2")
//                .loginId("test4321")
//                .password("test4321")
//                .build();
//
//        users1 = usersRepository.save(users1);
//        users2 = usersRepository.save(users2);
//
//        sender = Account.builder()
//                .users(users1)
//                .password("1234")
//                .accountNumber("11111111111111")
//                .money(BigDecimal.valueOf(1000000000.00))
//                .accountName("test account1")
//                .createdAt(LocalDateTime.now())
//                .currency(Currency.KRW)
//                .build();
//
//        receiver = Account.builder()
//                .users(users2)
//                .password("4321")
//                .accountNumber("22222222222222")
//                .money(BigDecimal.valueOf(0.00))
//                .accountName("test account2")
//                .createdAt(LocalDateTime.now())
//                .currency(Currency.KRW)
//                .build();
//
//        sender = accountRepository.save(sender);
//        receiver = accountRepository.save(receiver);
//    }
//
//
//    @AfterEach
//    void clear() {
//        accountRepository.deleteAll();
//        usersRepository.deleteAll();
//    }
//
//    @Test
//    @DisplayName("동시성 제어 테스트")
//    void testConcurrentTransfer() throws InterruptedException {
//        // 동시 실행할 스레드 개수
//        int threadCount = 100;
//        BigDecimal transferAmount = BigDecimal.valueOf(1000.00);
//
//        // 스레드 풀 생성
//        ExecutorService executorService = Executors.newFixedThreadPool(threadCount);
//        CountDownLatch latch = new CountDownLatch(threadCount);
//
//        TransferRequestDto txRequest = new TransferRequestDto("1234", sender.getId(),
//                receiver.getId(), transferAmount);
//
//        for (int i = 0; i < threadCount; i++) {
//            executorService.execute(() -> {
//                try {
//                    transferService.createTransfer(
//                            users1.getId(), // 송신 계좌 ID
//                            txRequest
//                    );
//                } catch (Exception e) {
//                    e.printStackTrace();
//                    System.out.println("예외: " + e.getMessage());
//                } finally {
//                    latch.countDown();
//                }
//            });
//        }
//
//        latch.await(); // 모든 스레드가 작업을 완료할 때까지 대기
//        executorService.shutdown();
//
//        // 결과 검증
//        Account updatedSenderAccount = accountRepository.findById(sender.getId()).orElseThrow();
//        Account updatedReceiverAccount = accountRepository.findById(receiver.getId()).orElseThrow();
//
//        BigDecimal expectedSenderBalance = sender.getMoney().subtract(transferAmount.multiply(BigDecimal.valueOf(threadCount)))
//                .setScale(2, RoundingMode.HALF_UP);
//        BigDecimal expectedReceiverBalance = receiver.getMoney().add(transferAmount.multiply(BigDecimal.valueOf(threadCount)))
//                .setScale(2, RoundingMode.HALF_UP);
//
//        assertEquals(expectedSenderBalance, updatedSenderAccount.getMoney(), "송신 계좌 잔액 검증");
//        assertEquals(expectedReceiverBalance, updatedReceiverAccount.getMoney(), "수신 계좌 잔액 검증");
//    }

//    @Test
//    @DisplayName("레디슨 동시성 제어 테스트 ")
//    public void testRedissonLock() throws InterruptedException {
//        // 동시 실행할 스레드 개수
//        int threadCount = 100;
//        BigDecimal transferAmount = BigDecimal.valueOf(1000.00);
//
//        // 스레드 풀 생성
//        ExecutorService executorService = Executors.newFixedThreadPool(threadCount);
//        CountDownLatch latch = new CountDownLatch(threadCount);
//
//        TransactionRequest txRequest = new TransactionRequest("1234", sender.getId(),
//                receiver.getId(), transferAmount);
//
//        for (int i = 0; i < threadCount; i++) {
//            executorService.execute(() -> {
//                try {
//                    redissonService.createTransactionWithRedisson(user1.getId(),txRequest);
//                } catch (Exception e) {
//                    e.printStackTrace();
//                    System.out.println("예외: " + e.getMessage());
//                } finally {
//                    latch.countDown();
//                }
//            });
//        }
//
//        latch.await(); // 모든 스레드가 작업을 완료할 때까지 대기
//        executorService.shutdown();
//
//        // 결과 검증
//        Account updatedSenderAccount = accountRepository.findById(sender.getId()).orElseThrow();
//        Account updatedReceiverAccount = accountRepository.findById(receiver.getId()).orElseThrow();
//
//        BigDecimal expectedSenderBalance = sender.getMoney().subtract(transferAmount.multiply(BigDecimal.valueOf(threadCount)))
//                .setScale(2, RoundingMode.HALF_UP);
//        BigDecimal expectedReceiverBalance = receiver.getMoney().add(transferAmount.multiply(BigDecimal.valueOf(threadCount)))
//                .setScale(2, RoundingMode.HALF_UP);
//
//        assertEquals(expectedSenderBalance, updatedSenderAccount.getMoney(), "송신 계좌 잔액 검증");
//        assertEquals(expectedReceiverBalance, updatedReceiverAccount.getMoney(), "수신 계좌 잔액 검증");
//    }
}
