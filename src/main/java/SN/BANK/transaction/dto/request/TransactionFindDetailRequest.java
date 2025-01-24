package SN.BANK.transaction.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@Schema(description = "거래 상세내역 조회 요청 Dto")
public class TransactionFindDetailRequest {

    @NotNull
    @Schema(description = "계좌의 데이터베이스 id 값", example = "1")
    private Long accountId;
    @NotNull
    @Schema(description = "거래의 데이터베이스 id 값", example = "1")
    private Long transactionId;

    @Builder
    public TransactionFindDetailRequest(Long accountId, Long transactionId) {
        this.accountId = accountId;
        this.transactionId = transactionId;
    }
}
