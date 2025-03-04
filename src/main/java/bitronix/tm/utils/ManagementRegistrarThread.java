package bitronix.tm.utils;

import java.util.logging.Level;

import static bitronix.tm.utils.ManagementRegistrar.*;

public class ManagementRegistrarThread
		extends Thread
{
	private static final org.apache.logging.log4j.Logger log = org.apache.logging.log4j.LogManager.getLogger(ManagementRegistrarThread.class);

	public ManagementRegistrarThread()
	{
		setName("bitronix-async-jmx-worker");
		setDaemon(true);
	}

	/**
	 * Method run ...
	 */
	@Override
	public void run()
	{
		while (!isInterrupted())
		{
			try
			{
				normalizeAndRunQueuedCommands();
				Thread.sleep(250); // sampling interval
			}
			catch (InterruptedException ex)
			{
				log.trace( "an unexpected error occurred in JMX asynchronous registration code", ex);
				return;
			}
			catch (Exception ex)
			{
				log.error( "an unexpected error occurred in JMX asynchronous registration code", ex);
			}
		}
	}
}
