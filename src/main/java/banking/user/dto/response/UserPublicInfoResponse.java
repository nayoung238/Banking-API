package banking.user.dto.response;

import banking.user.entity.User;
import lombok.Builder;

@Builder
public record UserPublicInfoResponse (

	Long userId,
	String name
) {

	public static UserPublicInfoResponse of(User user) {
		return UserPublicInfoResponse.builder()
			.userId(user.getId())
			.name(user.getName())
			.build();
	}
}
