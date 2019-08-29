package sim.io.geo;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.net.URL;

public class DataObjectImporter {
	public static BufferedInputStream open(final URL url) throws IllegalArgumentException, RuntimeException, IOException {
		if (url == null)
			throw new IllegalArgumentException("url is null; file is probably not found");
		return new BufferedInputStream(url.openStream());
	}
}
