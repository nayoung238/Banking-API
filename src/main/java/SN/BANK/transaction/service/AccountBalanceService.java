package SN.BANK.transaction.service;

import SN.BANK.account.entity.Account;
import SN.BANK.account.repository.AccountRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
public class AccountBalanceService {

    private final AccountRepository accountRepository;

    public void updateAccountBalances(Account senderAccount, Account receiverAccount, BigDecimal amount) {
        senderAccount.changeMoney(senderAccount.getMoney().subtract(amount));
        receiverAccount.changeMoney(receiverAccount.getMoney().add(amount));
        accountRepository.save(senderAccount);
        accountRepository.save(receiverAccount);
    }
}
