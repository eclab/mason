package masoncsc.properties;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;

import sim.engine.SimState;
import sim.engine.Steppable;

public class DynamicPropertiesReader implements Steppable
{
	private static final long serialVersionUID = 1L;
	
	private ArrayList<TimestampedProperties> properties = new ArrayList<TimestampedProperties>();
	private TimestampedProperties nextProps = null;
	private int index = 0;

	/**
	 * Read a file containing multiple sets of properties.
	 * The file can be in one of two formats. This first format has multiple 
	 * comma-separated properties per line, as follows:
	 * 
	 * time=1, a=1
	 * time=2, a=2, b=3
	 * time=3, a=3, b=4, c=5
	 * 
	 * The second format has properties on individual lines, with blocks 
	 * separated by lines with 5 or more dashes, i.e. "-----":
	 * 
	 * time=1
	 * a=1
	 * -----
	 * time=2
	 * a=2
	 * b=3
	 * -----
	 * time=3
	 * a=3
	 * b=4
	 * c=5
	 * 
	 * The multi-line format can handle comments begun with #.
	 * 
	 * @param filename
	 */
	public boolean read(String filename) {
		properties.clear();
		nextProps = null;
		index = 0;
		
		try {
			// read all the lines of the file into a list of strings
			Path path = Paths.get(filename);
			List<String> lines = Files.readAllLines(path, StandardCharsets.UTF_8);
			
			// put it all in one string
			StringBuilder sb = new StringBuilder();
			for (String s : lines)
				sb.append(s).append("\n");
			String all = sb.toString();
			
//			System.out.format("All:\n|%s|\n", all);
			
			List<String> blocks;
			// check for multi-line entries separated by -----
//			if (all.matches("-{5,}")) {
			if (all.contains("-----")) {
//				System.out.println("Multiline!");
//				String[] array = all.split("-{5,}");
//				for (String s : array)
//					System.out.println("|" + s + "|");
				blocks = new ArrayList<String>(Arrays.asList(all.split("-{5,}")));
			}
			else {
//				System.out.println("Single line!");
				blocks = new ArrayList<String>();
				for (String s : lines)
					blocks.add(s.replace(',', '\n'));
			}
			
			// assume all properties are on a single line
			for (String s : blocks) {
//				System.out.format("Block: |%s|\n", s);
//				properties.add(TimestampedProperties.parse(s));
				TimestampedProperties tsp = TimestampedProperties.parse(s);
//				System.out.format("Reading entry %d, %s\n", properties.size(), tsp);
				if (tsp != null)
					properties.add(tsp);
			}
			
		}
		catch (Exception e) { 
			e.printStackTrace(); 
			return false; 
		}
		
		return true;
	}
	
	public void init() {
		index = 0;
	}
	
	/**
	 * If there are properties at timestep 0, we may want to use them 
	 * before initializing the population. This function allows that, 
	 * but is a bit of a hack.
	 * 
	 * TODO find a better solution
	 */
	public void checkForInitialProperties() {
		if (!properties.isEmpty()) {
			TimestampedProperties p = properties.get(0);
			if (p.time == 0)
				useProperties(p.properties);
		}			
	}
	
	

	@Override
	public void step(SimState state) {
		if (index > properties.size())
			return;
		
		if (nextProps == null) {
			nextProps = getNextProperties();
			if (nextProps == null)
				return;
		}
		
		if (state.schedule.getTime() == nextProps.time) {
			useProperties(nextProps.properties);
			nextProps = null;
		}
	}
	
	public TimestampedProperties getNextProperties() {
		if (index >= properties.size())
			return null;
		
		return properties.get(index++);
	}
	
	/**
	 * Override this to use the properties.
	 * @param properties
	 */
	public void useProperties(Properties properties) {
	}
	
	public String toString() {
		StringBuilder sb = new StringBuilder();
		
		for (TimestampedProperties tsp : properties) {
			sb.append("time: " + tsp.time + "\n");
			sb.append(tsp);
			sb.append("\n-------\n");
		}
		
		return sb.toString();
	}
	

	

	public static void main(String[] args) {
		DynamicPropertiesReader dpr = new DynamicPropertiesReader();
		dpr.read("test1.properties");
		
		System.out.println(dpr);

//		String s = "abc\n---\n123\n";
//		System.out.println(s);
//
//		// Regex tests
////		System.out.println(s.matches("(?s).*"));
////		System.out.println(s.matches("(?s).*-{3,}.*"));
//		System.out.println(s.matches("(.|\\n)*^(-{3,})(.|\\n)*"));
//		System.out.println(s.matches("(.|\\n)*^abc$(.|\\n)*"));
//		System.out.println(s.matches("(.|\\n)*123$(.|\\n)*"));
//		System.out.println(s.matches("(.|\\n)*^123(.|\\n)*"));
//		System.out.println(s.matches("(.|\\n)*"));
	}

}
