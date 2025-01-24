package SN.BANK.transaction.dto.response;

import SN.BANK.transaction.entity.TransactionEntity;
import SN.BANK.transaction.enums.TransactionType;
import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@NoArgsConstructor
@Schema(description = "거래 상세 내역 응답Dto")
public class TransactionFindDetailResponse {
    @Schema(description = "거래 타입", example = "DEPOSIT")
    private TransactionType transactionType;
    @Schema(description = "거래일", example = "1985.06.11 15:20")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy.MM.dd HH:mm")
    private LocalDateTime transactedAt;
    @Schema(description = "거래 상대 이름", example = "홍길동")
    private String othersName;
    @Schema(description = "거래 상대 계좌 번호", example = "579221480232581")
    private String othersAccountNumber;
    @Schema(description = "거래액", example = "200000")
    private BigDecimal amount;
    @Schema(description = "거래 후 잔액", example = "8000")
    private BigDecimal balance;
    @Schema(description = "거래 메모", example = "치킨값 n빵")
    private String description;

    @Builder
    public TransactionFindDetailResponse(TransactionEntity tx, String othersName, String othersAccountNumber) {
        this.transactionType = tx.getType();
        this.transactedAt = tx.getTransactedAt();
        this.othersName = othersName;
        this.othersAccountNumber = othersAccountNumber;
        this.amount = tx.getAmount();
        this.balance = tx.getBalance();
        this.description = tx.getDescription();
    }
}
