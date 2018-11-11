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

import java.util.Date;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Asbtract superclass of all timed tasks.
 *
 * @author Ludovic Orban
 */
public abstract class Task
		implements Comparable<Task>
{

	private static final AtomicInteger UNIQUE_ID_SOURCE = new AtomicInteger();

	private final Date executionTime;
	private final TaskScheduler taskScheduler;
	private final int uniqueId;

	/**
	 * Constructor Task creates a new Task instance.
	 *
	 * @param executionTime
	 * 		of type Date
	 * @param scheduler
	 * 		of type TaskScheduler
	 */
	protected Task(Date executionTime, TaskScheduler scheduler)
	{
		this.executionTime = executionTime;
		this.taskScheduler = scheduler;
		this.uniqueId = UNIQUE_ID_SOURCE.getAndIncrement();
	}

	/**
	 * Method getExecutionTime returns the executionTime of this Task object.
	 *
	 * @return the executionTime (type Date) of this Task object.
	 */
	public Date getExecutionTime()
	{
		return executionTime;
	}

	/**
	 * Method getTaskScheduler returns the taskScheduler of this Task object.
	 *
	 * @return the taskScheduler (type TaskScheduler) of this Task object.
	 */
	protected TaskScheduler getTaskScheduler()
	{
		return taskScheduler;
	}

	/**
	 * Method compareTo ...
	 *
	 * @param otherTask
	 * 		of type Task
	 *
	 * @return int
	 */
	/*
	 * Compare by timestamp.  In the event of a duplicate timestamp, objects uniqueIds are compared so that
	 * one task (it doesn't matter which - they both have identical schedule times) will be deemed greater than the
	 * other
	 */
	@Override
	public int compareTo(Task otherTask)
	{
		int compareResult = this.executionTime.compareTo(otherTask.executionTime);

		if (compareResult == 0)
		{
			compareResult = Integer.valueOf(uniqueId)
			                       .compareTo(otherTask.getUniqueId());
		}
		return compareResult;
	}

	/**
	 * Method getUniqueId returns the uniqueId of this Task object.
	 *
	 * @return the uniqueId (type int) of this Task object.
	 */
	int getUniqueId()
	{
		return uniqueId;
	}

	/**
	 * Method getObject returns the object of this Task object.
	 *
	 * @return the object (type Object) of this Task object.
	 */
	public abstract Object getObject();

	/**
	 * Method execute ...
	 *
	 * @throws TaskException
	 * 		when
	 */
	public abstract void execute() throws TaskException;
}
