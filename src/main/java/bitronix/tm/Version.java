package bitronix.tm;

import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.jar.Manifest;

public class Version
{

	public static String getVersion()
	{
		URLClassLoader cl = (URLClassLoader) Version.class.getClassLoader();
		try
		{
			URL url = cl.findResource("META-INF/MANIFEST.MF");
			Manifest manifest = new Manifest(url.openStream());
			return manifest.getMainAttributes()
			               .getValue("Implementation-Version");
		}
		catch (IOException E)
		{
			// handle
		}
		return "Unknown";
	}
}
