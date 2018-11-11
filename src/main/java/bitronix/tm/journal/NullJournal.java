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

import bitronix.tm.utils.Uid;

import java.io.IOException;
import java.util.Collections;
import java.util.Map;
import java.util.Set;


/**
 * No-op journal. Do not use for anything else than testing as the transaction manager cannot guarantee
 * data integrity with this journal implementation.
 *
 * @author Ludovic Orban
 */
public class NullJournal
		implements Journal
{

	/**
	 * Constructor NullJournal creates a new NullJournal instance.
	 */
	public NullJournal()
	{
		//No content
	}

	/**
	 * Log a new transaction status to journal. Note that the journal will not check the flow of the transactions.
	 * If you call this method with erroneous data, it will be added to the journal as-is.
	 *
	 * @param status
	 * 		transaction status to log.
	 * @param gtrid
	 * 		GTRID of the transaction.
	 * @param uniqueNames
	 * 		unique names of the RecoverableXAResourceProducers participating in the transaction.
	 *
	 * @throws java.io.IOException
	 * 		if an I/O error occurs.
	 */
	@Override
	public void log(int status, Uid gtrid, Set<String> uniqueNames) throws IOException
	{
		//No content
	}

	/**
	 * Open the journal. Integrity should be checked and an exception should be thrown in case the journal is corrupt.
	 *
	 * @throws java.io.IOException
	 * 		if an I/O error occurs.
	 */
	@Override
	public void open() throws IOException
	{
		//No content
	}

	/**
	 * Close this journal and release all underlying resources.
	 *
	 * @throws java.io.IOException
	 * 		if an I/O error occurs.
	 */
	@Override
	public void close()
	{
		//No content
	}

	/**
	 * Force journal to synchronize with permanent storage.
	 *
	 * @throws java.io.IOException
	 * 		if an I/O error occurs.
	 */
	@Override
	public void force()
	{
		//No content
	}

	/**
	 * Collect all dangling records of the journal, ie: COMMITTING records with no corresponding COMMITTED record.
	 *
	 * @return a Map using Uid objects GTRID as key and implementations of {@link bitronix.tm.journal.JournalRecord} as value.
	 *
	 * @throws java.io.IOException
	 * 		if an I/O error occurs.
	 */
	@Override
	public Map<Uid, JournalRecord> collectDanglingRecords()
	{
		return Collections.emptyMap();
	}

	/**
	 * Shutdown the service and free all held resources.
	 */
	@Override
	public void shutdown()
	{
		//No content
	}

	/**
	 * Method toString ...
	 *
	 * @return String
	 */
	@Override
	public String toString()
	{
		return "a NullJournal";
	}
}
