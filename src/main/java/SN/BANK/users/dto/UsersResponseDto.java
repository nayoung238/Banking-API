package SN.BANK.users.dto;

import SN.BANK.users.entity.Users;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Schema(description = "회원정보 응답Dto")
@Getter
@NoArgsConstructor
public class UsersResponseDto {
    @Schema(description = "유저의 데이터베이스 아이디값",example = "1")
    private Long id;
    @Schema(description = "이름", example = "홍길동")
    private String name;
    @Schema(description = "로그인 id",example = "abc123")
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