package SN.BANK.account.entity;

import SN.BANK.account.enums.Currency;
import SN.BANK.common.entity.BaseTimeEntity;
import SN.BANK.common.exception.CustomException;
import SN.BANK.common.exception.ErrorCode;
import SN.BANK.users.entity.Users;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;
import java.math.BigDecimal;

@Entity
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Builder
@EntityListeners(AuditingEntityListener.class)
@ToString
public class Account extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private Users user;

    @Column(nullable = false)
    private String password;

    @Column(nullable = false, unique = true)
    private String accountNumber;

    @Column(nullable = false)
    private BigDecimal money;

    private String accountName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Currency currency;

    public void decreaseMoney(BigDecimal amount) {
        if (this.money.compareTo(amount) < 0) {
            throw new CustomException(ErrorCode.INSUFFICIENT_BALANCE);
        }
        this.money = this.money.subtract(amount);
    }

    public void increaseMoney(BigDecimal amount) {
        this.money = this.money.add(amount);
    }

    public boolean isAccountOwner(Long userId) {
        return this.user != null && this.user.getId().equals(userId);
    }

    public boolean isGreaterThanBalance(BigDecimal amount) {
        return amount.compareTo(money) > 0;
    }

    public boolean isCorrectPassword(String password) {
        return this.password.equals(password);
    }

}
