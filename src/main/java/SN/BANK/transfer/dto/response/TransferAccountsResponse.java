package SN.BANK.transfer.dto.response;

import SN.BANK.account.entity.Account;
import lombok.Builder;
import lombok.Getter;

@Getter
public class TransferAccountsResponse {

    private Account senderAccount;
    private Account receiverAccount;

    @Builder
    public TransferAccountsResponse(Account senderAccount, Account receiverAccount) {
        this.senderAccount = senderAccount;
        this.receiverAccount = receiverAccount;
    }

}
