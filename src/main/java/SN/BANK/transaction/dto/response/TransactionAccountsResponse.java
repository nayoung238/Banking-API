package SN.BANK.transaction.dto.response;

import SN.BANK.account.entity.Account;
import lombok.Builder;
import lombok.Getter;

@Getter
public class TransactionAccountsResponse {

    private Account senderAccount;
    private Account receiverAccount;

    @Builder
    public TransactionAccountsResponse(Account senderAccount, Account receiverAccount) {
        this.senderAccount = senderAccount;
        this.receiverAccount = receiverAccount;
    }

}
