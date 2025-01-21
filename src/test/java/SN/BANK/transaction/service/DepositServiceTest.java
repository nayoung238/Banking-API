package SN.BANK.transaction.service;

import SN.BANK.account.entity.Account;
import SN.BANK.account.enums.Currency;
import SN.BANK.account.service.AccountService;
import SN.BANK.transaction.dto.request.TransactionRequest;
import SN.BANK.transaction.dto.response.TransactionResponse;
import SN.BANK.transaction.repository.TransactionRepository;
import SN.BANK.users.entity.Users;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(SpringExtension.class)
public class DepositServiceTest {

    @Mock
    AccountService accountService;

    @Mock
    TransactionRecoverService recoverService;

    @Mock
    TransactionRepository transactionRepository;

    @InjectMocks
    DepositService depositService;

    Users sender;
    Users receiver;
    Account senderAccount;
    Account receiverAccount;

    @BeforeEach
    void setUp() {

        sender = Users.builder()
                .name("테스터1")
                .loginId("test1234")
                .password("test1234")
                .build();

        receiver = Users.builder()
                .name("테스터2")
                .loginId("test4321")
                .password("test4321")
                .build();

        senderAccount = Account.builder()
                .id(1L)
                .user(sender)
                .password("1234")
                .accountNumber("11111111111111")
                .money(BigDecimal.valueOf(10000))
                .accountName("test account1")
                .createdAt(LocalDateTime.now())
                .currency(Currency.KRW)
                .build();

        receiverAccount = Account.builder()
                .id(2L)
                .user(receiver)
                .password("1234")
                .accountNumber("22222222222222")
                .money(BigDecimal.valueOf(10000))
                .accountName("test account2")
                .createdAt(LocalDateTime.now())
                .currency(Currency.KRW)
                .build();
    }

    @Test
    @DisplayName("정상적으로 돈을 수취하고 거래 내역을 생성한다.")
    void receiveSuccessTest() {

        // given
        BigDecimal amount = BigDecimal.valueOf(1000.00);
        BigDecimal expectedBalance = receiverAccount.getMoney().add(amount);

        TransactionRequest txRequest = TransactionRequest.builder()
                .accountPassword("1234")
                .senderAccountId(senderAccount.getId())
                .receiverAccountId(receiverAccount.getId())
                .amount(amount)
                .build();

        when(accountService.getAccountWithLock(receiverAccount.getId())).thenReturn(receiverAccount);

        // when
        TransactionResponse response = depositService.receiveFrom(txRequest, senderAccount,
                BigDecimal.ONE, amount);

        // then
        assertNotNull(response);
        assertEquals(expectedBalance, receiverAccount.getMoney());
        assertEquals(sender.getName(), response.getSenderName());
        assertEquals(receiver.getName(), response.getReceiverName());
    }

}
