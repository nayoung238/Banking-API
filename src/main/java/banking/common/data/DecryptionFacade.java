package banking.common.data;

import banking.common.exception.CustomException;
import banking.common.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.util.Base64;

@Component
@RequiredArgsConstructor
@Slf4j
public class DecryptionFacade {

	@Value("${data.secret-key}")
	private String SECRET_KEY;

	public String decrypt(String encryptedText) {
		try {
			byte[] combined = Base64.getDecoder().decode(encryptedText);

			// Extract IV
			byte[] iv = new byte[16];
			System.arraycopy(combined, 0, iv, 0, iv.length);

			// Extract encrypted data
			byte[] encryptedData = new byte[combined.length - iv.length];
			System.arraycopy(combined, iv.length, encryptedData, 0, encryptedData.length);

			// Initialize cipher for decryption
			Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
			SecretKeySpec keySpec = new SecretKeySpec(Base64.getDecoder().decode(SECRET_KEY), "AES");
			IvParameterSpec ivParams = new IvParameterSpec(iv);
			cipher.init(Cipher.DECRYPT_MODE, keySpec, ivParams);

			// Decrypt data
			return new String(cipher.doFinal(encryptedData));
		} catch (Exception e) {
			log.error(e.getMessage());
			throw new CustomException(ErrorCode.DECRYPTION_FAIL);
		}
	}
}
