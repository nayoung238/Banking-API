package SN.BANK.transaction.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class TransactionFindDetailRequest {

    @NotNull
    private Long accountId;
    @NotNull
    private Long transactionId;

}
