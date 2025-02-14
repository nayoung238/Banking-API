package banking.common;

import banking.account.dto.request.AccountCreationRequestDto;
import banking.account.dto.response.AccountResponseDto;
import banking.account.repository.AccountRepository;
import banking.account.service.AccountService;
import banking.fixture.dto.AccountCreationRequestDtoFixture;
import banking.fixture.dto.UserCreationRequestDtoFixture;
import banking.payment.repository.PaymentRepository;
import banking.transfer.repository.TransferRepository;
import banking.user.dto.request.UserCreationRequestDto;
import banking.user.dto.response.UserResponseDto;
import banking.user.repository.UserRepository;
import banking.user.service.UserService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class TransferIntegrationTestBase {

	@Autowired
	UserService userService;

	@Autowired
	UserRepository userRepository;

	@Autowired
	AccountService accountService;

	@Autowired
	AccountRepository accountRepository;

	@Autowired
	TransferRepository transferRepository;

	@Autowired
	PaymentRepository paymentRepository;

	public UserResponseDto senderUser;
	public UserResponseDto receiverUser;

	public AccountResponseDto senderKrwAccount;
	public String senderKrwAccountPassword;

	public AccountResponseDto receiverKrwAccount;
	public String receiverKrwAccountPassword;

	@BeforeEach
	void setUp() {
		UserCreationRequestDto sendUserRequest = UserCreationRequestDtoFixture.USER_CREATION_REQUEST_DTO_FIXTURE_1.createUserCreationRequestDto();
		senderUser = userService.register(sendUserRequest);

		UserCreationRequestDto receiverUserRequest = UserCreationRequestDtoFixture.USER_CREATION_REQUEST_DTO_FIXTURE_2.createUserCreationRequestDto();
		receiverUser = userService.register(receiverUserRequest);

		AccountCreationRequestDto senderAKrwAccountRequest = AccountCreationRequestDtoFixture.ACCOUNT_FIXTURE_KRW_1.createAccountCreationRequestDto();
		senderKrwAccount = accountService.createAccount(senderUser.userId(), senderAKrwAccountRequest);
		senderKrwAccountPassword = senderAKrwAccountRequest.password();

		AccountCreationRequestDto receiverKrwAccountRequest = AccountCreationRequestDtoFixture.ACCOUNT_FIXTURE_KRW_2.createAccountCreationRequestDto();
		receiverKrwAccount = accountService.createAccount(receiverUser.userId(), receiverKrwAccountRequest);
		receiverKrwAccountPassword = receiverKrwAccountRequest.password();
	}

	@AfterEach
	void tearDown() {
		transferRepository.deleteAll();
		paymentRepository.deleteAll();
		accountRepository.deleteAll();
		userRepository.deleteAll();
	}
}
