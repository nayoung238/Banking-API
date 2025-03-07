package banking.transfer.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;

@Builder
@Schema(description = "거래 상세내역 조회 요청 DTO")
public record TransferDetailsRequest (

    @NotNull
    @Schema(description = "계좌 DB PK", example = "1")
    Long accountId,

    @NotNull
    @Schema(description = "이체 DB PK", example = "1")
    Long transferId
) { }
