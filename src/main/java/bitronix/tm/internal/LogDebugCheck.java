package bitronix.tm.internal;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Checks if debug log is enabled..;/
 */
public interface LogDebugCheck
{
	static boolean isDebugEnabled()
	{
		return Logger.getLogger("")
		             .getLevel()
		             .intValue() >= Level.FINER.intValue();
	}
}
