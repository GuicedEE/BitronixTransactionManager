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
package bitronix.tm.journal;

import bitronix.tm.internal.LogDebugCheck;
import bitronix.tm.utils.Decoder;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;

/**
 * Used to control a log file's header.
 * <p>The physical data is read when this object is created then cached. Calling setter methods sets the header field
 * then moves the file pointer back to the previous location.</p>
 *
 * @author Ludovic Orban
 */
public class TransactionLogHeader
{

	/**
	 * State of the log file when it has been closed properly.
	 */
	public static final byte CLEAN_LOG_STATE = 0;
	/**
	 * State of the log file when it hasn't been closed properly or it is still open.
	 */
	public static final byte UNCLEAN_LOG_STATE = -1;
	/**
	 * Position of the format ID in the header (see {@link bitronix.tm.BitronixXid#FORMAT_ID}).
	 */
	static final long FORMAT_ID_HEADER = 0;
	/**
	 * Position of the timestamp in the header.
	 */
	static final long TIMESTAMP_HEADER = FORMAT_ID_HEADER + 4;
	/**
	 * Position of the log file state in the header.
	 */
	static final long STATE_HEADER = TIMESTAMP_HEADER + 8;
	/**
	 * Position of the current log position in the header.
	 */
	static final long CURRENT_POSITION_HEADER = STATE_HEADER + 1;
	/**
	 * Total length of the header.
	 */
	static final long HEADER_LENGTH = CURRENT_POSITION_HEADER + 8;
	private static final java.util.logging.Logger log = java.util.logging.Logger.getLogger(TransactionLogHeader.class.toString());
	private final FileChannel fc;
	private final long maxFileLength;

	private volatile int formatId;
	private volatile long timestamp;
	private volatile byte state;
	private volatile long position;

	/**
	 * TransactionLogHeader are used to control headers of the specified RandomAccessFile.
	 *
	 * @param fc
	 * 		the file channel to read from.
	 * @param maxFileLength
	 * 		the max file length.
	 *
	 * @throws IOException
	 * 		if an I/O error occurs.
	 */
	public TransactionLogHeader(FileChannel fc, long maxFileLength) throws IOException
	{
		this.fc = fc;
		this.maxFileLength = maxFileLength;

		fc.position(FORMAT_ID_HEADER);
		ByteBuffer buf = ByteBuffer.allocate(4 + 8 + 1 + 8);
		while (buf.hasRemaining())
		{
			this.fc.read(buf);
		}
		buf.flip();
		formatId = buf.getInt();
		timestamp = buf.getLong();
		state = buf.get();
		position = buf.getLong();
		fc.position(position);

		if (LogDebugCheck.isDebugEnabled())
		{
			log.finer("read header " + this);
		}
	}

	/**
	 * Get TIMESTAMP_HEADER.
	 *
	 * @return the TIMESTAMP_HEADER value.
	 *
	 * @see #TIMESTAMP_HEADER
	 */
	public long getTimestamp()
	{
		return timestamp;
	}

	/**
	 * Set TIMESTAMP_HEADER.
	 *
	 * @param timestamp
	 * 		the TIMESTAMP_HEADER value.
	 *
	 * @throws IOException
	 * 		if an I/O error occurs.
	 * @see #TIMESTAMP_HEADER
	 */
	public void setTimestamp(long timestamp) throws IOException
	{
		ByteBuffer buf = ByteBuffer.allocate(8);
		buf.putLong(timestamp);
		buf.flip();
		while (buf.hasRemaining())
		{
			fc.write(buf, TIMESTAMP_HEADER + buf.position());
		}
		this.timestamp = timestamp;
	}

	/**
	 * Get STATE_HEADER.
	 *
	 * @return the STATE_HEADER value.
	 *
	 * @see #STATE_HEADER
	 */
	public byte getState()
	{
		return state;
	}

	/**
	 * Set STATE_HEADER.
	 *
	 * @param state
	 * 		the STATE_HEADER value.
	 *
	 * @throws IOException
	 * 		if an I/O error occurs.
	 * @see #STATE_HEADER
	 */
	public void setState(byte state) throws IOException
	{
		ByteBuffer buf = ByteBuffer.allocate(1);
		buf.put(state);
		buf.flip();
		while (buf.hasRemaining())
		{
			fc.write(buf, STATE_HEADER + buf.position());
		}
		this.state = state;
	}

	/**
	 * Get CURRENT_POSITION_HEADER.
	 *
	 * @return the CURRENT_POSITION_HEADER value.
	 *
	 * @see #CURRENT_POSITION_HEADER
	 */
	public long getPosition()
	{
		return position;
	}

	/**
	 * Set CURRENT_POSITION_HEADER.
	 *
	 * @param position
	 * 		the CURRENT_POSITION_HEADER value.
	 *
	 * @throws IOException
	 * 		if an I/O error occurs.
	 * @see #CURRENT_POSITION_HEADER
	 */
	public void setPosition(long position) throws IOException
	{
		if (position < HEADER_LENGTH)
		{
			throw new IOException("invalid position " + position + " (too low)");
		}
		if (position > maxFileLength)
		{
			throw new IOException("invalid position " + position + " (too high)");
		}

		ByteBuffer buf = ByteBuffer.allocate(8);
		buf.putLong(position);
		buf.flip();
		while (buf.hasRemaining())
		{
			fc.write(buf, CURRENT_POSITION_HEADER + buf.position());
		}

		this.position = position;
		fc.position(position);
	}

	/**
	 * Rewind CURRENT_POSITION_HEADER back to the beginning of the file.
	 *
	 * @throws IOException
	 * 		if an I/O error occurs.
	 * @see #setPosition
	 */
	void rewind() throws IOException
	{
		setPosition(HEADER_LENGTH);
	}

	/**
	 * Create human-readable String representation.
	 *
	 * @return a human-readable String representing this object's state.
	 */
	@Override
	public String toString()
	{
		return "a Bitronix TransactionLogHeader with timestamp=" + timestamp +
		       ", state=" + Decoder.decodeHeaderState(state) +
		       ", position=" + position;
	}

}
