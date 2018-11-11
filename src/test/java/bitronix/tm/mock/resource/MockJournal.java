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
package bitronix.tm.mock.resource;

import bitronix.tm.journal.Journal;
import bitronix.tm.journal.JournalRecord;
import bitronix.tm.journal.TransactionLogRecord;
import bitronix.tm.mock.events.EventRecorder;
import bitronix.tm.mock.events.JournalLogEvent;
import bitronix.tm.utils.Uid;

import javax.transaction.Status;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

/**
 * @author Ludovic Orban
 */
public class MockJournal
		implements Journal
{

	private Map<Uid, JournalRecord> danglingRecords;

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
	public void log(int status, Uid gtrid, Set<String> uniqueNames)
	{
		TransactionLogRecord record = new TransactionLogRecord(status, gtrid, uniqueNames);
		if (status == Status.STATUS_COMMITTING)
		{
			danglingRecords.put(gtrid, record);
		}
		if (status == Status.STATUS_COMMITTED)
		{
			danglingRecords.remove(gtrid);
		}
		getEventRecorder().addEvent(new JournalLogEvent(this, status, gtrid, uniqueNames));
	}

	/**
	 * Method getEventRecorder returns the eventRecorder of this MockJournal object.
	 *
	 * @return the eventRecorder (type EventRecorder) of this MockJournal object.
	 */
	private EventRecorder getEventRecorder()
	{
		return EventRecorder.getEventRecorder(this);
	}

	/**
	 * Open the journal. Integrity should be checked and an exception should be thrown in case the journal is corrupt.
	 *
	 * @throws java.io.IOException
	 * 		if an I/O error occurs.
	 */
	@Override
	public void open()
	{
		danglingRecords = new HashMap<>();
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
		danglingRecords = null;
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
		return danglingRecords;
	}

	/**
	 * Method readRecords ...
	 *
	 * @param includeInvalid
	 * 		of type boolean
	 *
	 * @return Iterator<JournalRecord>
	 */
	public Iterator<JournalRecord> readRecords(boolean includeInvalid)
	{
		return danglingRecords.values()
		                      .iterator();
	}

	/**
	 * Shutdown the service and free all held resources.
	 */
	@Override
	public void shutdown()
	{
	}
}
