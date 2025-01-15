package SN.BANK.users.dto;

import SN.BANK.users.entity.Users;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class UsersResponseDto {
    private Long id;
    private String name;
    private String loginId;

    @Builder
    private UsersResponseDto(Long id, String name, String  loginId){
        this.id = id;
        this.name = name;
        this.loginId = loginId;
    }

    public static UsersResponseDto of(Users users){
        return UsersResponseDto.builder().id(users.getId()).name(users.getName()).loginId(users.getLoginId()).build();
    }


}
