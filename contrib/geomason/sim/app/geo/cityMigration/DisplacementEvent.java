package cityMigration;

import riftland.household.Household;
import java.util.StringTokenizer;

public class DisplacementEvent
{
	double timestamp;
	int parcelX, parcelY;
	public int groupSize;
	public int culture;
	int citizenship;
	Household household;	// might be null if running in stand-alone mode
	
	public DisplacementEvent(double timestamp, Household household) {
		this.timestamp = timestamp;
		this.household = household;
		parcelX = household.getLocation().getX();
		parcelY = household.getLocation().getY();
		groupSize = household.getPopulation();
		culture = household.getCulture();
		citizenship = household.getCitizenship();
	}
	
	public DisplacementEvent(double timestamp, int parcelX, int parcelY, int groupSize, int culture, int citizenship, Household household) {
		this.timestamp = timestamp;
		this.parcelX = parcelX;
		this.parcelY = parcelY;
		this.groupSize = groupSize;
		this.culture = culture;
		this.citizenship = citizenship;
		this.household = household;
	}	
	
	/** This constructor is used in the city migration model when reading events from a file.
	 * That's why there's no household parameter. */
	public DisplacementEvent(double timestamp, int parcelX, int parcelY, int groupSize, int culture, int citizenship) {
		this(timestamp, parcelX, parcelY, groupSize, culture, citizenship, null);
	}
	
	/**
	 * Create a displacement event from a string. This occurs when events are read from a log file
	 * into the stand-alone migration model.
	 */
	public DisplacementEvent(String str) {
		StringTokenizer st = new StringTokenizer(str, ",");
		
		timestamp = Long.parseLong(st.nextToken());
		parcelX = Integer.parseInt(st.nextToken());
		parcelY = Integer.parseInt(st.nextToken());
		groupSize = Integer.parseInt(st.nextToken());
		culture = Integer.parseInt(st.nextToken());
		citizenship = Integer.parseInt(st.nextToken());
	}
	
	public String toString() {
		return String.format("%f, %d, %d, %d, %d, %d", timestamp, parcelX, parcelY, groupSize, culture, citizenship);
	}
}
