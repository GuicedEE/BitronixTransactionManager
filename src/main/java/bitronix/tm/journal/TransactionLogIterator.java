package bitronix.tm.journal;

import bitronix.tm.TransactionManagerServices;

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
				try
				{
					tlog = tlc.readLog(skipCrcCheck);
					if (tlog == null)
					{
						break;
					}
				}
				catch (CorruptedTransactionLogException ex)
				{
					if (TransactionManagerServices.getConfiguration()
					                              .isSkipCorruptedLogs())
					{
						log.log(Level.SEVERE, "skipping corrupted log", ex);
						continue;
					}
					throw ex;
				}
			}
			catch (IOException e)
			{
				throw new RuntimeException(e);
			}
		}

		return tlog != null;
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
