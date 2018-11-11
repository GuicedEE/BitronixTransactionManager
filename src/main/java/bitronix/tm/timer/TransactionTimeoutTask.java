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
package bitronix.tm.timer;

import bitronix.tm.BitronixTransaction;
import bitronix.tm.internal.LogDebugCheck;

import javax.transaction.SystemException;
import java.util.Date;

/**
 * This task is used to mark a transaction as timed-out.
 *
 * @author Ludovic Orban
 */
public class TransactionTimeoutTask
		extends Task
{

	private final static java.util.logging.Logger log = java.util.logging.Logger.getLogger(TransactionTimeoutTask.class.toString());

	private final BitronixTransaction transaction;

	/**
	 * Constructor TransactionTimeoutTask creates a new TransactionTimeoutTask instance.
	 *
	 * @param transaction
	 * 		of type BitronixTransaction
	 * @param executionTime
	 * 		of type Date
	 * @param scheduler
	 * 		of type TaskScheduler
	 */
	public TransactionTimeoutTask(BitronixTransaction transaction, Date executionTime, TaskScheduler scheduler)
	{
		super(executionTime, scheduler);
		this.transaction = transaction;
	}

	/**
	 * Method getObject returns the object of this TransactionTimeoutTask object.
	 *
	 * @return the object (type Object) of this TransactionTimeoutTask object.
	 */
	@Override
	public Object getObject()
	{
		return transaction;
	}

	/**
	 * Method execute ...
	 *
	 * @throws TaskException
	 * 		when
	 */
	@Override
	public void execute() throws TaskException
	{
		try
		{
			if (LogDebugCheck.isDebugEnabled())
			{
				log.finer("marking " + transaction + " as timed out");
			}
			transaction.timeout();
		}
		catch (SystemException ex)
		{
			throw new TaskException("failed to timeout " + transaction, ex);
		}
	}

	/**
	 * Method toString ...
	 *
	 * @return String
	 */
	@Override
	public String toString()
	{
		return "a TransactionTimeoutTask on " + transaction + " scheduled for " + getExecutionTime();
	}

}
