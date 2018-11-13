package bitronix.tm.journal;

import bitronix.tm.TransactionManagerServices;
import bitronix.tm.internal.BitronixRuntimeException;
import bitronix.tm.internal.BitronixSystemException;

import java.io.IOException;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.logging.Level;

/**
 * An iterator that goes over transaction logs
 */
public class TransactionLogIterator
		implements Iterator<TransactionLogRecord>
{
	private static final java.util.logging.Logger log = java.util.logging.Logger.getLogger(TransactionLogIterator.class.toString());

	private final TransactionLogCursor tlc;
	private final boolean skipCrcCheck;

	private TransactionLogRecord tlog;

	/**
	 * Constructor TransactionLogIterator creates a new TransactionLogIterator instance.
	 *
	 * @param tlc
	 * 		of type TransactionLogCursor
	 * @param skipCrcCheck
	 * 		of type boolean
	 */
	public TransactionLogIterator(TransactionLogCursor tlc, boolean skipCrcCheck)
	{
		this.tlc = tlc;
		this.skipCrcCheck = skipCrcCheck;
	}


	/**
	 * Method hasNext ...
	 *
	 * @return boolean
	 */
	@Override
	public boolean hasNext()
	{
		while (tlog == null)
		{
			try
			{
				processTlog();
			}
			catch (CorruptedTransactionLogException ctle)
			{
				log.log(Level.SEVERE, "Skipping Corrupted Log", ctle);
			}
			catch (BitronixSystemException bse)
			{
				break;
			}
			catch (IOException e)
			{
				throw new BitronixRuntimeException(e);
			}
		}

		return tlog != null;
	}

	private void processTlog() throws IOException, BitronixSystemException
	{
		try
		{
			tlog = tlc.readLog(skipCrcCheck);
			if (tlog == null)
			{
				throw new BitronixSystemException("Breaker");
			}
		}
		catch (CorruptedTransactionLogException ex)
		{
			if (TransactionManagerServices.getConfiguration()
			                              .isSkipCorruptedLogs())
			{
				throw ex;
			}
			throw ex;
		}
	}

	/**
	 * Method next ...
	 *
	 * @return TransactionLogRecord
	 */
	@Override
	public TransactionLogRecord next()
	{
		if (!hasNext())
		{
			throw new NoSuchElementException();
		}
		try
		{
			return tlog;
		}
		finally
		{
			tlog = null;
		}
	}

	/**
	 * Method remove ...
	 */
	@Override
	public void remove()
	{
		throw new UnsupportedOperationException();
	}
}
