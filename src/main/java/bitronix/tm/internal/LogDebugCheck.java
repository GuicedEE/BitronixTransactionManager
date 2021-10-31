package bitronix.tm.internal;

import java.util.logging.*;

/**
 * Checks if debug log is enabled..;/
 */
public interface LogDebugCheck
{
	/**
	 * Method isDebugEnabled returns the debugEnabled of this LogDebugCheck object.
	 *
	 * @return the debugEnabled (type boolean) of this LogDebugCheck object.
	 */
	static boolean isDebugEnabled()
	{
		Logger log = LogManager.getLogManager()
		                       .getLogger("");
		if (log == null)
		{
			return false;
		}
		if (log.getLevel() == null)
		{
			return false;
		}
		
		return LogManager.getLogManager()
		                 .getLogger("")
		                 .getLevel()
		                 .intValue() >= Level.FINER.intValue();
	}
}
