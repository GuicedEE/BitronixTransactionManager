package bitronix.tm.utils;


import javax.crypto.*;
import javax.crypto.spec.DESKeySpec;
import java.io.UnsupportedEncodingException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.util.Base64;

class EncryptService
{
	private static final String cipher = "DES";
	private static final String utf8 = "UTF8";

	private final SecretKey key;
	private Cipher ecipher;
	private Cipher dcipher;

	public EncryptService(String keyPass, boolean encrypt) throws InvalidKeySpecException, NoSuchAlgorithmException, InvalidKeyException, NoSuchPaddingException
	{
		DESKeySpec desKeySpec = new DESKeySpec(keyPass.getBytes());
		SecretKeyFactory keyFactory = SecretKeyFactory.getInstance(cipher);
		this.key = keyFactory.generateSecret(desKeySpec);
		if (encrypt)
		{
			ecipher = Cipher.getInstance(cipher);
		}
		else
		{
			dcipher = Cipher.getInstance(cipher);
		}
		if (encrypt)
		{
			ecipher.init(Cipher.ENCRYPT_MODE, key);
		}
		else
		{
			dcipher.init(Cipher.DECRYPT_MODE, key);
		}
	}

	public String encrypt(String message) throws UnsupportedEncodingException, BadPaddingException, IllegalBlockSizeException
	{
		byte[] utf8 = message.getBytes(EncryptService.utf8);
		byte[] enc = ecipher.doFinal(utf8);
		return Base64.getEncoder()
		             .encodeToString(enc);
	}

	public String decrypt(String message) throws BadPaddingException, IllegalBlockSizeException, UnsupportedEncodingException
	{
		byte[] dec = Base64.getDecoder()
		                   .decode(message.getBytes());
		byte[] utf8 = dcipher.doFinal(dec);
		return new String(utf8, EncryptService.utf8);
	}
}
