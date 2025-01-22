package SN.BANK.notification.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
public class FCMToken
{
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long userId;
    private String token;

    private LocalDateTime createdAt;

    @Builder
    public FCMToken(Long userId, String token){
        this.userId = userId;
        this.token = token;
        this.createdAt = LocalDateTime.now();
    }
}
