package banking.users.dto.response;

import banking.users.entity.Users;
import lombok.Builder;

@Builder
public record UserPublicInfoDto(

	Long id,
	String name
) {

	public static UserPublicInfoDto of(Users user) {
		return UserPublicInfoDto.builder()
			.id(user.getId())
			.name(user.getName())
			.build();
	}
}
