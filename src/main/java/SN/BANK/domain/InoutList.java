package SN.BANK.domain;

import SN.BANK.domain.enums.InoutTag;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@NoArgsConstructor
@Getter
public class InoutList {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    private InoutTag inoutTag;

    private Long amount;

    private Long balance;

    private Long accountId;
}
