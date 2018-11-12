/*
 * Copyright (C) 2006-2013 Bitronix Software (http://www.bitronix.be)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * GedMarc - 20181108 - Update to use internals rather
 */
package bitronix.tm.utils;

import bitronix.tm.internal.BitronixSystemException;

import javax.crypto.NoSuchPaddingException;
import javax.transaction.SystemException;
import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;

/**
 * <p>Simple crypto helper that uses symetric keys to crypt and decrypt resources passwords.</p>
 *
 * @author Ludovic Orban
 */
public class CryptoEngine
{
	private static final String CRYPTO_PASSWORD = "B1tr0n!+";

	private CryptoEngine()
	{
		//Nothing needed
	}

	/**
	 * Decrypt using the given cipher the given base64-encoded, crypted data.
	 *
	 * @param data
	 * 		the base64-encoded data to decrypt.
	 *
	 * @return decrypted data.
	 *
	 * @throws InvalidKeyException
	 * 		if the given key material is shorter than 8 bytes.
	 * @throws NoSuchAlgorithmException
	 * 		if a secret-key factory for the specified algorithm is not available in the
	 * 		default provider package or any of the other provider packages that were searched.
	 * @throws NoSuchPaddingException
	 * 		if transformation contains a padding scheme that is not available.
	 * @throws InvalidKeySpecException
	 * 		if the given key specification is inappropriate for this secret-key factory to
	 * 		produce a secret key.
	 * @throws IOException
	 * 		if an I/O error occurs.
	 */
	public static String decrypt(String data) throws SystemException
	{
		try
		{
			return new EncryptService(CRYPTO_PASSWORD, false).decrypt(data);
		}
		catch (Exception e)
		{
			throw new BitronixSystemException("Unable to decrypt", e);
		}
	}

	/**
	 * Crypt the given data using the given cipher.
	 * The crypted result is base64-encoded before it is returned.
	 *
	 * @param data
	 * 		the data to crypt.
	 *
	 * @return crypted, base64-encoded data.
	 *
	 * @throws InvalidKeyException
	 * 		if the given key material is shorter than 8 bytes.
	 * @throws NoSuchAlgorithmException
	 * 		if a secret-key factory for the specified algorithm is not available in the
	 * 		default provider package or any of the other provider packages that were searched.
	 * @throws NoSuchPaddingException
	 * 		if transformation contains a padding scheme that is not available.
	 * @throws InvalidKeySpecException
	 * 		if the given key specification is inappropriate for this secret-key factory to
	 * 		produce a secret key.
	 * @throws IOException
	 * 		if an I/O error occurs.
	 */
	public static String crypt(String data) throws SystemException
	{
		try
		{
			return new EncryptService(CRYPTO_PASSWORD, true).encrypt(data);
		}
		catch (Exception e)
		{
			throw new BitronixSystemException("Unable to decrypt", e);
		}
	}
}
