package bitronix.tm.utils;

import org.junit.Test;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import java.io.UnsupportedEncodingException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;

public class EncryptServiceTest
{

	/**
	 * Method encrypt ...
	 */
	@Test
	public void encrypt()
	{
		try
		{
			String encrypted = new EncryptService("password", true).encrypt("This is a classified message!");
			System.out.println(encrypted);
		}
		catch (UnsupportedEncodingException e)
		{
			e.printStackTrace();
		}
		catch (BadPaddingException e)
		{
			e.printStackTrace();
		}
		catch (IllegalBlockSizeException e)
		{
			e.printStackTrace();
		}
		catch (InvalidKeySpecException e)
		{
			e.printStackTrace();
		}
		catch (NoSuchPaddingException e)
		{
			e.printStackTrace();
		}
		catch (NoSuchAlgorithmException e)
		{
			e.printStackTrace();
		}
		catch (InvalidKeyException e)
		{
			e.printStackTrace();
		}
	}

	/**
	 * Method decrypt ...
	 */
	@Test
	public void decrypt()
	{
		try
		{
			String decrypted = new EncryptService("password", false).decrypt("qlW1WyuCiMAiJgHyZti+b16qYzPseRgdfWB2wToJw9g=");
			System.out.println(decrypted);
		}
		catch (UnsupportedEncodingException e)
		{
			e.printStackTrace();
		}
		catch (BadPaddingException e)
		{
			e.printStackTrace();
		}
		catch (IllegalBlockSizeException e)
		{
			e.printStackTrace();
		}
		catch (InvalidKeySpecException e)
		{
			e.printStackTrace();
		}
		catch (NoSuchPaddingException e)
		{
			e.printStackTrace();
		}
		catch (NoSuchAlgorithmException e)
		{
			e.printStackTrace();
		}
		catch (InvalidKeyException e)
		{
			e.printStackTrace();
		}
	}
}
