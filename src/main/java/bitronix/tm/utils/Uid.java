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
 */
package bitronix.tm.utils;

import java.util.Arrays;

/**
 * <p>A constant UID byte array container optimized for use with hashed collections.</p>
 *
 * @author Ludovic Orban
 */
public final class Uid
{

	private static final char[] HEX = {'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B', 'C', 'D', 'E', 'F'};
	private final byte[] array;
	private final int hashCodeValue;
	private final String toStringValue;

	/**
	 * Constructor Uid creates a new Uid instance.
	 *
	 * @param array
	 * 		of type byte[]
	 */
	public Uid(byte[] array)
	{
		this.array = new byte[array.length];
		System.arraycopy(array, 0, this.array, 0, array.length);
		this.hashCodeValue = arrayHashCode(array);
		this.toStringValue = arrayToString(array);
	}

	/**
	 * Compute a UID byte array hashcode value.
	 *
	 * @param uid
	 * 		the byte array used for hashcode computation.
	 *
	 * @return a constant hash value for the specified uid.
	 */
	private static int arrayHashCode(byte[] uid)
	{
		int hash = 0;
		// Common fast but good hash with wide dispersion
		for (int i = uid.length - 1; i > 0; i--)
		{
			// rotate left and xor
			// (very fast in assembler, a bit clumsy in Java)
			hash <<= 1;

			if (hash < 0)
			{
				hash |= 1;
			}

			hash ^= uid[i];
		}
		return hash;
	}

	/**
	 * Decode a UID byte array into a (somewhat) human-readable hex string.
	 *
	 * @param uid
	 * 		the uid to decode.
	 *
	 * @return the resulting printable string.
	 */
	private static String arrayToString(byte[] uid)
	{
		char[] hexChars = new char[uid.length * 2];
		int c = 0;
		int v;
		for (int i = 0; i < uid.length; i++)
		{
			v = uid[i] & 0xFF;
			hexChars[c++] = HEX[v >> 4];
			hexChars[c++] = HEX[v & 0xF];
		}
		return new String(hexChars);
	}

	/**
	 * Method getArray returns the array of this Uid object.
	 *
	 * @return the array (type byte[]) of this Uid object.
	 */
	public byte[] getArray()
	{
		return array;
	}

	/**
	 * Method extractServerId ...
	 *
	 * @return byte[]
	 */
	public byte[] extractServerId()
	{
		int serverIdLength = array.length - 4 - 8; // - sequence - timestamp
		if (serverIdLength < 1)
		{
			return null;
		}

		byte[] result = new byte[serverIdLength];
		System.arraycopy(array, 0, result, 0, serverIdLength);
		return result;
	}

	/**
	 * Method extractTimestamp ...
	 *
	 * @return long
	 */
	public long extractTimestamp()
	{
		return Encoder.bytesToLong(array, array.length - 4 - 8); // - sequence - timestamp
	}

	/**
	 * Method extractSequence ...
	 *
	 * @return int
	 */
	public int extractSequence()
	{
		return Encoder.bytesToInt(array, array.length - 4); // - sequence
	}

	/**
	 * Method length ...
	 *
	 * @return int
	 */
	public int length()
	{
		return array.length;
	}

	/**
	 * Method hashCode ...
	 *
	 * @return int
	 */
	@Override
	public int hashCode()
	{
		return hashCodeValue;
	}

	/**
	 * Method equals ...
	 *
	 * @param obj
	 * 		of type Object
	 *
	 * @return boolean
	 */
	@Override
	public boolean equals(Object obj)
	{
		if (obj instanceof Uid)
		{
			Uid otherUid = (Uid) obj;

			// optimizes performance a bit
			if (hashCodeValue != otherUid.hashCodeValue)
			{
				return false;
			}

			return Arrays.equals(array, otherUid.array);
		}
		return false;
	}

	/**
	 * Method toString ...
	 *
	 * @return String
	 */
	@Override
	public String toString()
	{
		return toStringValue;
	}
}

