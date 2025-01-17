package SN.BANK.transaction.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class TransactionFindDetailRequest {

    @NotNull
    private Long accountId;
    @NotNull
    private Long transactionId;

    @Builder
    public TransactionFindDetailRequest(Long accountId, Long transactionId) {
        this.accountId = accountId;
        this.transactionId = transactionId;
    }
}
