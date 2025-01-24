package SN.BANK.transaction.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;

@Builder
@Schema(description = "거래 상세내역 조회 요청 Dto")
public record TransactionFindDetailRequest (
    @NotNull
    @Schema(description = "계좌의 데이터베이스 id 값", example = "1")
    Long accountId,

    @NotNull
    @Schema(description = "거래의 데이터베이스 id 값", example = "1")
    Long transactionId
) { }
