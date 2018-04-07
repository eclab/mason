package refugee;
import java.io.*;
import java.text.NumberFormat;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import net.sf.csv4j.CSVReader;
import refugee.refugeeData.RefugeeData;

public class Stats {

	public static void main(String[] args) {
		HashMap<String, Double> actualpop = new HashMap<String, Double>();
		String actual_file = "parameter_sweeps/actual.csv";
		readInActual(actual_file, actualpop);
		
		String compareFile = "parameter_sweeps/new_pop-risk/output_arrive";
		compareToFile(compareFile, actualpop);
		
	}
	
	private static void readInActual(String actual_file, HashMap<String, Double> actualpop){
		try {
			// buffer reader for age distribution data
			CSVReader csvReader = new CSVReader(new InputStreamReader(RefugeeData.class.getResourceAsStream(actual_file)));
			List<String> line = csvReader.readLine();
			while (!line.isEmpty()) {
				// read in the county ids
				String name = line.get(0);
				// relevant info is from 5 - 21
				double percOfPop = NumberFormat.getNumberInstance(java.util.Locale.US).parse(line.get(2)).doubleValue();
				// System.out.println("sum = " + sum);
				// System.out.println();

				// now add it to the hashmap
				actualpop.put(name,  percOfPop);
				line = csvReader.readLine();
			}
			
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (java.text.ParseException e) {
			e.printStackTrace();
		}
	}
	
	private static void compareToFile(String filename, HashMap<String, Double> actualpop){
		try {
			String filenameout = filename + "_compare.csv";
			File outfile = new File(filenameout);
			CSVReader csvReader = new CSVReader(new InputStreamReader(RefugeeData.class.getResourceAsStream(filename + ".csv")));
			FileWriter fw = new FileWriter(outfile, true);
			BufferedWriter bw = new BufferedWriter(fw);
			PrintWriter writer = new PrintWriter(bw);
			List<String> names = csvReader.readLine(); 
			names.remove(0);//header
			List<String> values = csvReader.readLine();

			List<Double> newvalues;
			
			while (!values.isEmpty()){
			newvalues = new ArrayList<Double>(values.size() - 1);
			//System.out.println("Size: " + newvalues.size());
			int trialNumber = NumberFormat.getNumberInstance(java.util.Locale.US).parse(values.get(0)).intValue();
			values.remove(0);
			System.out.println(values);
			System.out.println(values.size());
			//double sum = 0;
			for (int i = 0; i < values.size() - 1; i++){
				
				double value = (double) NumberFormat.getNumberInstance(java.util.Locale.US).parse(values.get(i)).intValue();
				System.out.println(i + ": " + value);
				//sum += value;
				newvalues.add(i, value);
			}
			double min = Collections.min(newvalues);
			double max = Collections.max(newvalues);
			double difference = 0;
			for (int i = 0; i < newvalues.size(); i++){
				double percent = (newvalues.get(i) - min)/(max - min);
				double actualPercent = actualpop.get(names.get(i));
				System.out.println(names.get(i));
				System.out.println(actualPercent + ", " + percent);
				difference += Math.abs(percent - actualPercent);
			}
			writer.println(trialNumber + "," + difference);
			values = csvReader.readLine();
			}
			writer.close();
			
			
		} catch (IOException e) {
			e.printStackTrace();
		} catch (ParseException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}
	}

