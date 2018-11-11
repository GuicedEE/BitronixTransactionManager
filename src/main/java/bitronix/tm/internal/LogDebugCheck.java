package bitronix.tm.internal;

import java.util.logging.Level;
import java.util.logging.Logger;

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
		return Logger.getLogger("")
		             .getLevel()
		             .intValue() >= Level.FINER.intValue();
	}
}
