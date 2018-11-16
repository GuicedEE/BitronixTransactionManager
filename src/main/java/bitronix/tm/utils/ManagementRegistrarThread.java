package bitronix.tm.utils;

import java.util.logging.Level;

import static bitronix.tm.utils.ManagementRegistrar.*;

public class ManagementRegistrarThread
		extends Thread
{
	private static final java.util.logging.Logger log = java.util.logging.Logger.getLogger(ManagementRegistrarThread.class.toString());

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
				log.log(Level.FINEST, "an unexpected error occurred in JMX asynchronous registration code", ex);
				return;
			}
			catch (Exception ex)
			{
				log.log(Level.SEVERE, "an unexpected error occurred in JMX asynchronous registration code", ex);
			}
		}
	}
}
