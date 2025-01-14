package SN.BANK.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
public class Users {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToMany(mappedBy = "id")
    private List<Account> accounts;

    private String name;
    private String loginId;
    private String password;

    @Builder
    public Users(String name, String loginId,String password){
        this.name = name;
        this.loginId = loginId;
        this.password = password;
    }
}
