package SN.BANK.transfer.entity;

import SN.BANK.common.entity.BaseTimeEntity;
import SN.BANK.common.exception.CustomException;
import SN.BANK.common.exception.ErrorCode;
import SN.BANK.transfer.enums.TransferType;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Entity
@Getter
@Builder
@AllArgsConstructor(access = AccessLevel.PROTECTED)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Transfer extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long withdrawalAccountId;

    @Column(nullable = false)
    private Long depositAccountId;

    @Column(nullable = false)
    private String currency;

    @Column(nullable = false)
    private BigDecimal exchangeRate;

    @Column(nullable = false)
    @Builder.Default
    @OneToMany(mappedBy = "transfer", cascade = CascadeType.ALL, fetch = FetchType.EAGER)
    @MapKey(name = "type")
    private Map<TransferType, TransferDetails> transferDetails = new ConcurrentHashMap<>();

    public void addTransferDetails(TransferType type, TransferDetails transferDetails) {
        this.transferDetails.computeIfAbsent(type, key -> {
            if (this.transferDetails.containsKey(key)) {
                throw new CustomException(ErrorCode.DUPLICATE_TRANSFER_TYPE);
            }
            return transferDetails;
        });
    }
}
