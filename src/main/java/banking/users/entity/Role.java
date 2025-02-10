package banking.users.entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum Role {

    ROLE_USER("USER"),
    ROLE_ADMIN("ADMIN");

    private final String description;
}
