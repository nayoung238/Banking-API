package banking.user.dto.response;

import banking.user.entity.User;
import lombok.Builder;

@Builder
public record UserPublicInfoDto(

	Long id,
	String name
) {

	public static UserPublicInfoDto of(User user) {
		return UserPublicInfoDto.builder()
			.id(user.getId())
			.name(user.getName())
			.build();
	}
}
