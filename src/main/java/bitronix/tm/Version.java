package bitronix.tm;

import java.io.IOException;
import java.net.URL;
import java.util.jar.Manifest;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Version
{
	/**
	 * Constructor Version creates a new Version instance.
	 */
	private Version()
	{
		//No config
	}

	/**
	 * Method getVersion returns the version of this Version object.
	 *
	 * @return the version (type String) of this Version object.
	 */
	public static String getVersion()
	{
		ClassLoader cl = Version.class.getClassLoader();
		try
		{
			URL url = cl.getResource("META-INF/MANIFEST.MF");
			if (url != null)
			{
				Manifest manifest = new Manifest(url.openStream());
				return manifest.getMainAttributes()
				               .getValue("Implementation-Version");
			}
			else
			{
				return "Manifest File Not Found";
			}
		}
		catch (IOException E)
		{
			Logger.getLogger("BTMVersion")
			      .log(Level.WARNING, "Unable to read MANIFEST in META-INF", E);
		}
		return "Unknown";
	}
}
