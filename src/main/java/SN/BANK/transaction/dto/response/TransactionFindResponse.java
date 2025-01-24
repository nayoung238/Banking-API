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
@Schema(description = "거래 조회 응답Dto")
public class TransactionFindResponse {
    @Schema(description = "거래의 데이터베이스 id 값", example = "1")
    private Long transactionId;
    @Schema(description = "계좌 주인 이름", example = "홍길동")
    private String othersName;
    @Schema(description = "계좌 이름", example = "청년저축통장")
    private String accountNumber;
    @Schema(description = "거래 타입", example = "DEPOSIT")
    private TransactionType transactionType;
    @Schema(description = "거래일", example = "1985.06.11 15:20")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy.MM.dd HH:mm")
    private LocalDateTime transactedAt;
    @Schema(description = "거래액", example = "200000")
    private BigDecimal amount; // 송금액
    @Schema(description = "거래 후 잔액", example = "8000")
    private BigDecimal balance; // 이체(입금) 후 잔액

    @Builder
    public TransactionFindResponse(TransactionEntity tx, String othersName, String accountNumber) {
        this.transactionId = tx.getId();
        this.othersName = othersName;
        this.accountNumber = accountNumber;
        this.transactionType = tx.getType();
        this.transactedAt = tx.getTransactedAt();
        this.amount = tx.getAmount();
        this.balance = tx.getBalance();
    }
}
