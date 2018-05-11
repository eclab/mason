package CDI.src.environment ;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
//import java.io.FileInputStream;
//import java.io.FileWriter;
import java.io.BufferedReader;
//import java.io.BufferedWriter;
//import java.io.InputStream;
//import java.io.InputStreamReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.StringTokenizer;
import java.util.Vector;
import java.net.URL;

import org.spiderland.Psh.intStack;


public class GrowthRateTable
{
    class TableEntry
    {
        public double begin;
        public double end;
        public double urbanGrowth;
        public double ruralGrowth;
        
        TableEntry(double begin, double end, double urbanGrowth, double ruralGrowth)
        {
            this.begin = begin;
            this.end = end;
            this.urbanGrowth = urbanGrowth;
            this.ruralGrowth = ruralGrowth;
        }
        
        @Override
        public String toString() {
        	return this.begin+","+this.end+","+this.urbanGrowth+","+this.ruralGrowth;
        }
    }
    
    private Vector<TableEntry> table;
    // map the year to its seasonal growthRate
    private HashMap<Double, Double> urbanGrowRateMap;
    private HashMap<Double, Double> ruralGrowRateMap;

    public GrowthRateTable(String tableFilename)
    {
        //table = hardcodeTable();
        table = loadTableFromFile(tableFilename);
        urbanGrowRateMap = new HashMap<Double, Double>();
        ruralGrowRateMap = new HashMap<Double, Double>();
        preComupte(table);
    }


    private void preComupte(Vector<TableEntry> table) {
		for(int i = 0;i<table.size();++i)
		{
			TableEntry entry = table.get(i);
			int j = 0;
			while(j+entry.begin<entry.end)
			{
				urbanGrowRateMap.put(j+entry.begin, toGrowRate(entry.urbanGrowth/100, 4));
				ruralGrowRateMap.put(j+entry.begin, toGrowRate(entry.ruralGrowth/100, 4));
				j++;
			}			
		}
	}


	private double toGrowRate(double growth, double base) {
		double ans = Math.pow(1+growth, 1/base)-1;
		return ans;
	}


	public double getUrbanGrowthRateForDate(double date, double defaultGrowthRate)
    {
    	double year = Math.floor(date);
    	if(urbanGrowRateMap.containsKey(year))
    		return urbanGrowRateMap.get(year);
    	return defaultGrowthRate;
    }

    
    public double getRuralGrowthRateForDate(double date, double defaultGrowthRate)
    {
    	double year = Math.floor(date);
    	if(ruralGrowRateMap.containsKey(year))
    		return ruralGrowRateMap.get(year);
    	return defaultGrowthRate;
    }


/*
    public double getGrowthRateForDate(double date, double defaultGrowthRate)
    {
        return getUrbanGrowthRateForDate(date, defaultGrowthRate);
    }
*/

    
    public Vector<TableEntry> hardcodeTable()
    {
        Vector<TableEntry> table = new Vector<TableEntry>();
        
        table.add(new TableEntry(1911, 1921, 2.19, 2.19));
        table.add(new TableEntry(1921, 1931, 1.80, 1.80));
        table.add(new TableEntry(1931, 1941, 1.08, 1.08));
        table.add(new TableEntry(1941, 1951, 1.86, 1.86));
        table.add(new TableEntry(1951, 1956, 3.56, 3.56));
        table.add(new TableEntry(1956, 1961, 2.68, 2.68));
        table.add(new TableEntry(1961, 1966, 1.94, 1.94));
        table.add(new TableEntry(1966, 1971, 1.55, 1.55));
        table.add(new TableEntry(1971, 1976, 1.74, 1.74));
        table.add(new TableEntry(1976, 1981, 1.16, 1.16));
        table.add(new TableEntry(1981, 1986, 1.03, 1.03));
        table.add(new TableEntry(1986, 1991, 1.47, 1.47));
        table.add(new TableEntry(1991, 1996, 1.12, 1.12));
        table.add(new TableEntry(1996, 2001, 0.95, 0.95));

        return table;
    }
    

    /**
     * Load a table
     */
    private Vector<TableEntry> loadTableFromFile(String filename)
    {
    	
    	
    	
        FileReader fr = null;
        BufferedReader reader = null;
        StringTokenizer tokenizer;
        String line;
        Vector<TableEntry> table = new Vector<TableEntry>();
        
        if(filename==null)
    		return table;
        
        
        double begin, end, urbanGrowth, ruralGrowth;

        try
        {
            //InputStream is = getClass().getClassLoader().getResourceAsStream(filename);
        	
        	File file = new File(filename);
        	FileInputStream is = new FileInputStream(file);
        	InputStreamReader isr = new InputStreamReader(is);
            reader = new BufferedReader(isr);

            while ((line = reader.readLine()) != null)
            {
                if (line.startsWith("#"))   // ignore comments
                    continue;
                
                //System.out.println(line);
                tokenizer = new StringTokenizer(line);
                begin = Double.parseDouble(tokenizer.nextToken());
                end = Double.parseDouble(tokenizer.nextToken());
                urbanGrowth = Double.parseDouble(tokenizer.nextToken());
                ruralGrowth = Double.parseDouble(tokenizer.nextToken());
                TableEntry entry = new TableEntry(begin, end, urbanGrowth, ruralGrowth);
                table.add(entry);
                
            }
        }
        catch(FileNotFoundException ex)
        {
            System.err.println("Could not open growth rate file: " + filename);
            Thread.dumpStack();
            System.exit(0);
        }
        catch(IOException ex)
        {
            System.err.println("Error reading growth rate file: " + filename);
            Thread.dumpStack();
            //System.exit(0);
        }
        finally
        {
            try
            {
                if(reader != null)
                    reader.close();
                if(fr != null)
                    fr.close();
            }
            catch (IOException ex)
            {
                // Just ignore these
            }
        }

/*
        if(grid instanceof DoubleGrid2D) // if the grid is double
        {
            DoubleGrid2D terrain = (DoubleGrid2D)grid ;
            for(int i = 0; i < terrain.field[0].length; i++)
            {
                tokenizer = new StringTokenizer(reader.readLine());
                for(int j = 0; j < terrain.field.length; j++)
                    terrain.field[j][i] = 
                        Double.parseDouble(tokenizer.nextToken());
            }
        }
        else if(grid instanceof IntGrid2D) // if integer
        {
            IntGrid2D terrain = (IntGrid2D)grid ;
            for(int i = 0; i < terrain.field[0].length; i++)
            {
                tokenizer = new StringTokenizer(reader.readLine());
                for(int j = 0; j < terrain.field.length; j++)
                    terrain.field[j][i] = 
                        (int)Double.parseDouble(tokenizer.nextToken());
            }
        }
*/
        
        return table;
    }
}



