package bitronix.tm.utils;

import org.junit.Test;

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
		catch (Exception e)
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
		catch (Exception e)
		{
			e.printStackTrace();
		}

	}
}
