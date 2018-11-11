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

import bitronix.tm.TransactionManagerServices;
import bitronix.tm.internal.LogDebugCheck;
import bitronix.tm.recovery.Recoverer;

import java.util.Date;

/**
 * This task is used to run the background recovery.
 *
 * @author Ludovic Orban
 */
public class RecoveryTask
		extends Task
{

	private final static java.util.logging.Logger log = java.util.logging.Logger.getLogger(RecoveryTask.class.toString());

	private final Recoverer recoverer;

	/**
	 * Constructor RecoveryTask creates a new RecoveryTask instance.
	 *
	 * @param recoverer
	 * 		of type Recoverer
	 * @param executionTime
	 * 		of type Date
	 * @param scheduler
	 * 		of type TaskScheduler
	 */
	public RecoveryTask(Recoverer recoverer, Date executionTime, TaskScheduler scheduler)
	{
		super(executionTime, scheduler);
		this.recoverer = recoverer;
	}

	/**
	 * Method getObject returns the object of this RecoveryTask object.
	 *
	 * @return the object (type Object) of this RecoveryTask object.
	 */
	@Override
	public Object getObject()
	{
		return recoverer;
	}

	/**
	 * Method execute ...
	 */
	@Override
	public void execute()
	{
		if (LogDebugCheck.isDebugEnabled())
		{
			log.finer("running recovery");
		}
		Thread recovery = new Thread(recoverer);
		recovery.setName("bitronix-recovery-thread");
		recovery.setDaemon(true);
		recovery.setPriority(Thread.NORM_PRIORITY - 1);
		recovery.start();

		Date nextExecutionDate = new Date(getExecutionTime().getTime() + (TransactionManagerServices.getConfiguration()
		                                                                                            .getBackgroundRecoveryIntervalSeconds() * 1000L));
		if (LogDebugCheck.isDebugEnabled())
		{
			log.finer("rescheduling recovery for " + nextExecutionDate);
		}
		getTaskScheduler().scheduleRecovery(recoverer, nextExecutionDate);
	}

	/**
	 * Method toString ...
	 *
	 * @return String
	 */
	@Override
	public String toString()
	{
		return "a RecoveryTask scheduled for " + getExecutionTime();
	}

}
