package sim.app.geo.cityMigration;

import java.io.BufferedReader;
import java.io.FileReader;
import java.util.StringTokenizer;

import sim.engine.SimState;
import sim.engine.Steppable;

public class DisplacementLogReader
{
	private static final long serialVersionUID = 1L;
	private String filename;
	private BufferedReader reader;
	
	public DisplacementLogReader(String filename) {
		this.filename = filename;
		openFile();

		// Discard the headers
		try {
			reader.readLine();
		}
		catch (Exception e) { e.printStackTrace(); }
	}
	
	public boolean openFile() {
		try {
			reader = new BufferedReader(new FileReader(filename));
			return true;
		}
		catch (Exception e) { e.printStackTrace(); }
		return false;
	}
	
	public void closeFile() {
		try {
			reader.close();
		}
		catch (Exception e) { e.printStackTrace(); }
	}
	
	public DisplacementEvent getNextEvent() {
		try {
			String line = reader.readLine();
			if (line == null)
				return null;
			
			StringTokenizer st = new StringTokenizer(line, ",");
			
			long step = Long.parseLong(st.nextToken());
			int parcelX = Integer.parseInt(st.nextToken());
			int parcelY = Integer.parseInt(st.nextToken());
			int groupSize = Integer.parseInt(st.nextToken());
			int culture = Integer.parseInt(st.nextToken());
			int citizenship = Integer.parseInt(st.nextToken());
		
			return new DisplacementEvent(step, parcelX, parcelY, groupSize, culture, citizenship);
		}
		catch (Exception e) { e.printStackTrace(); return null;}
		
	}

}
