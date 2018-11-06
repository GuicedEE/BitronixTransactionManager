package bitronix.tm;

import java.io.IOException;
import java.net.URL;
import java.util.jar.Manifest;

public class Version
{

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
			// handle
		}
		return "Unknown";
	}
}
