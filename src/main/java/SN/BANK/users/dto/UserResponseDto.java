package SN.BANK.users.dto;

import SN.BANK.users.entity.Users;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

@Schema(description = "회원정보 응답 DTO")
@Builder
public record UserResponseDto (

    @Schema(description = "유저 DB PK", example = "1")
    Long userId,

    @Schema(description = "이름", example = "홍길동")
    String name,

    @Schema(description = "로그인 ID",example = "abc123")
    String loginId
) {
    public static UserResponseDto of(Users user){
        return UserResponseDto.builder()
            .userId(user.getId())
            .name(user.getName())
            .loginId(user.getLoginId())
            .build();
    }
}