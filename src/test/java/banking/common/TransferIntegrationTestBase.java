package banking.common;

import banking.account.dto.request.AccountCreationRequest;
import banking.account.dto.response.AccountDetailResponse;
import banking.account.repository.AccountRepository;
import banking.account.service.AccountService;
import banking.fixture.dto.AccountCreationRequestDtoFixture;
import banking.fixture.dto.UserCreationRequestDtoFixture;
import banking.payment.repository.PaymentRepository;
import banking.transfer.repository.TransferRepository;
import banking.user.dto.request.UserCreationRequest;
import banking.user.dto.response.UserResponse;
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

	public UserResponse senderUser;
	public UserResponse receiverUser;

	public AccountDetailResponse senderKrwAccount;
	public String senderKrwAccountPassword;

	public AccountDetailResponse receiverKrwAccount;
	public String receiverKrwAccountPassword;

	@BeforeEach
	void setUp() {
		UserCreationRequest sendUserRequest = UserCreationRequestDtoFixture.USER_CREATION_REQUEST_DTO_FIXTURE_1.createUserCreationRequestDto();
		senderUser = userService.register(sendUserRequest);

		UserCreationRequest receiverUserRequest = UserCreationRequestDtoFixture.USER_CREATION_REQUEST_DTO_FIXTURE_2.createUserCreationRequestDto();
		receiverUser = userService.register(receiverUserRequest);

		AccountCreationRequest senderAKrwAccountRequest = AccountCreationRequestDtoFixture.ACCOUNT_FIXTURE_KRW_1.createAccountCreationRequestDto();
		senderKrwAccount = accountService.createAccount(senderUser.userId(), senderAKrwAccountRequest);
		senderKrwAccountPassword = senderAKrwAccountRequest.password();

		AccountCreationRequest receiverKrwAccountRequest = AccountCreationRequestDtoFixture.ACCOUNT_FIXTURE_KRW_2.createAccountCreationRequestDto();
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
