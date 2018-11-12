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
package bitronix.tm.twopc.executor;

import bitronix.tm.internal.BitronixRuntimeException;

import java.util.concurrent.*;
import java.util.logging.Level;

/**
 * This implementation executes submitted jobs using a <code>java.util.concurrent</code> cached thread pool.
 *
 * @author Ludovic Orban
 */
public class AsyncExecutor
		implements Executor
{
	private static final java.util.logging.Logger log = java.util.logging.Logger.getLogger(AsyncExecutor.class.toString());
	private final ExecutorService executorService;


	/**
	 * Constructor AsyncExecutor creates a new AsyncExecutor instance.
	 */
	public AsyncExecutor()
	{
		executorService = Executors.newCachedThreadPool();
	}

	/**
	 * Submit a job to be executed by the thread pool.
	 *
	 * @param job
	 * 		the {@link Runnable} to execute.
	 *
	 * @return an object used to monitor the execution of the submitted {@link Runnable}.
	 */
	@Override
	public Object submit(Job job)
	{
		return executorService.submit(job);
	}

	/**
	 * Wait for the job represented by the future to terminate. The call to this method will block until the job
	 * finished its execution or the specified timeout elapsed.
	 *
	 * @param future
	 * 		the future representing the job as returned by {@link #submit}.
	 * @param timeout
	 * 		if the job did not finish during the specified timeout in milliseconds, this method returns anyway.
	 */
	@Override
	public void waitFor(Object future, long timeout)
	{
		Future<?> f = (Future<?>) future;

		try
		{
			f.get(timeout, TimeUnit.MILLISECONDS);
		}
		catch (InterruptedException ex)
		{
			throw new BitronixRuntimeException("job interrupted", ex);
		}
		catch (ExecutionException ex)
		{
			throw new BitronixRuntimeException("job execution exception", ex);
		}
		catch (TimeoutException ex)
		{
			// ok, just return
			log.log(Level.FINEST, "Just Return", ex);
		}
	}

	/**
	 * Check if the thread pool has terminated the execution of the job represented by a future.
	 *
	 * @param future
	 * 		the future representing the job as returned by {@link #submit}.
	 *
	 * @return true if the job is done, false otherwise.
	 */
	@Override
	public boolean isDone(Object future)
	{
		Future<?> f = (Future<?>) future;

		return f.isDone();
	}

	/**
	 * Shutdown the thead pool.
	 */
	@Override
	public void shutdown()
	{
		executorService.shutdownNow();
	}
}
