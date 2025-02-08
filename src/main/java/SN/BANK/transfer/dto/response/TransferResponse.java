package SN.BANK.transfer.dto.response;

import SN.BANK.transfer.entity.Transfer;
import SN.BANK.transfer.enums.TransferType;
import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@NoArgsConstructor
@Schema(description = "거래 응답 Dto")
public class TransferResponse {
    @Schema(description = "거래의 데이터베이스 id 값", example = "1")
    private Long transactionId;
    @Schema(description = "송금계좌의 데이터베이스 id 값", example = "1")
    private Long senderAccountId;
    @Schema(description = "입금계좌의 데이터베이스 id 값", example = "1")
    private Long receiverAccountId;
    @Schema(description = "송금자의 이름", example = "홍길동")
    private String senderName;
    @Schema(description = "입금자의 이름", example = "전우치")
    private String receiverName;
    @Schema(description = "거래 타입", example = "DEPOSIT")
    private TransferType transferType;
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy.MM.dd HH:mm")
    @Schema(description = "거래일", example = "1985.06.11 15:20")
    private LocalDateTime transactedAt;
    @Schema(description = "거래액", example = "200000")
    private BigDecimal amount; // 송금액
    @Schema(description = "거래 후 잔액", example = "8000")
    private BigDecimal balance; // 이체(입금) 후 잔액

    @Builder
    public TransferResponse(Transfer tx, String senderName, String receiverName) {
        this.transactionId = tx.getId();
        this.senderAccountId = tx.getSenderAccountId();
        this.receiverAccountId = tx.getReceiverAccountId();
        this.transferType = tx.getType();
        this.transactedAt = tx.getTransactedAt();
        this.amount = tx.getAmount();
        this.balance = tx.getBalance();
        this.senderName = senderName;
        this.receiverName = receiverName;
    }
}
