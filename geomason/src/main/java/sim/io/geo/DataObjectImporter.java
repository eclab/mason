package sim.io.geo;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

public class DataObjectImporter {
	public static InputStream open(final URL url) throws IllegalArgumentException, RuntimeException, IOException {
		if (url == null)
			throw new IllegalArgumentException("url is null; file is probably not found");
		final InputStream urlStream = new BufferedInputStream(url.openStream());
		if (urlStream == null)
			throw new RuntimeException("Cannot load URL " + url);
		return urlStream;
	}
}
