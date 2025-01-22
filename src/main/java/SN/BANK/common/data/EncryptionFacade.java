package SN.BANK.common.data;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.security.SecureRandom;
import java.util.Base64;

@Component
@RequiredArgsConstructor
@Slf4j
public class EncryptionFacade {

	@Value("${data.secret-key}")
	private String SECRET_KEY;

	public String encrypt(String plainText) {
		try {
			// Generate random IV
			byte[] iv = new byte[16];
			SecureRandom random = new SecureRandom();
			random.nextBytes(iv);

			// Initialize cipher
			Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
			SecretKeySpec keySpec = new SecretKeySpec(Base64.getDecoder().decode(SECRET_KEY), "AES");
			IvParameterSpec ivParams = new IvParameterSpec(iv);
			cipher.init(Cipher.ENCRYPT_MODE, keySpec, ivParams);

			// Encrypt data
			byte[] encryptedData = cipher.doFinal(plainText.getBytes());

			// Combine IV and encrypted data
			byte[] combined = new byte[iv.length + encryptedData.length];
			System.arraycopy(iv, 0, combined, 0, iv.length);
			System.arraycopy(encryptedData, 0, combined, iv.length, encryptedData.length);

			return Base64.getEncoder().encodeToString(combined);
		} catch (Exception e) {
			log.error(e.getMessage());
			throw new RuntimeException("암호화 실패");
		}
	}
}
