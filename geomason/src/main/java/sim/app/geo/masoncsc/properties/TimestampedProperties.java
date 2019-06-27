package sim.app.geo.masoncsc.properties;

import java.io.IOException;
import java.io.StringReader;
import java.util.Properties;

public class TimestampedProperties {


	public long time;
	public long getTime() { return time; }
	public void setTime(long val) { time = val; }
	
	public Properties properties = new Properties();
	

	public TimestampedProperties(long time) {
		super();
		this.time = time;
	}
	
	public TimestampedProperties() {
		super();
	}

	static public TimestampedProperties parse(String s) throws IOException {
		TimestampedProperties props = new TimestampedProperties();
		props.properties.load(new StringReader(s));
		if (props.properties.size() == 0)
			return null;
		props.time = Long.parseLong(props.properties.getProperty("time"));
		props.properties.remove("time"); // remove it from the properties since it's held in a local variable
		
		return props;
	}
	

	@Override
	public boolean equals(Object obj) {
		TimestampedProperties other = (TimestampedProperties)obj;
		if (time != other.time)
			return false;
		
		return properties.equals(other.properties);
	}
	
	@Override
	public String toString() {
		return String.format("Time: %d, properties: %s", time, properties.toString());
	}
	
	
	
}
