package CDI.src.movement.data;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

import CDI.src.movement.Household;
import CDI.src.movement.NorthLandsMovement;
import CDI.src.movement.data.BarChartFactor.Factor;

public class DataCollector {

    private final NorthLandsMovement model;
    private FileWriter writer = null;
    private FileWriter householdWriter = null;

    private double currentTime;
    private int iteration;
    
    private boolean columnLabels=false;

    // first plot, time series
    private int urbanPop;
    private int ruralPop;

    // second plot, time series
    private int urbanToRural; // indicate the urban population to Rural area
    private int urbanToUrban;
    private int ruralToUrban;
    private int ruralToRural;

    private int bufferUrbanToRural;   // indicate the urban population to Rural area
    private int bufferUrbanToUrban;
    private int bufferRuralToUrban;
    private int bufferRuralToRural;

    
    private int urbanToRuralMove; // indicate the urban population to Rural area
    private int urbanToUrbanMove;
    private int ruralToUrbanMove;
    private int ruralToRuralMove;

    private int bufferUrbanToRuralMove;   // indicate the urban population to Rural area
    private int bufferUrbanToUrbanMove;
    private int bufferRuralToUrbanMove;
    private int bufferRuralToRuralMove;
    
    
    // third plot
    private double totalUrbanDistance;
    private double totalRuralDistance;

    private double bufferTotalUrbanDistance;
    private double bufferTotalRuralDistance;

    // fourth plot
    private int cellToUrban;
    private int cellToRural;

    private int bufferCellToUrban;
    private int bufferCellToRural;
    
    private double wealthGini;
    private double ruralWealthGini;
    private double urbanWealthGini;

    private double ruralGiniCoeff;
    private double urbanGiniCoeff;

    private double ruralSatisMean;
    private double urbanSatisMean;

    private double ruralSatisStdev;
    private double urbanSatisStdev;

    private double ruralSatisKurtosis;
    private double urbanSatisKurtosis;

    private double federalRevenues;
    private double federalAssets;
    private double totalRevenues;
    private double totalAssets;
    private double federalExpenses;
    
    private double infrastructureExpenses;
    
    private int trappedRural;
    private int trappedUrban;
    private int bufferTrappedRural;
    private int bufferTrappedUrban;

    private final String pathPrefix;
    private final String householdPrefix;
    
    private double totalUrbanWealth;
    private double totalRuralWealth;
    private double bufferUrbanWealth;
    private double bufferRuralWealth;
    
    public double getBufferUrbanWealth() { return bufferUrbanWealth; }
  	public void   setBufferUrbanWealth(double bufferUrbanWealth) { this.bufferUrbanWealth = bufferUrbanWealth;}

  	public double getBufferRuralWealth() { return bufferRuralWealth; }
  	public void   setBufferRuralWealth(double bufferRuralWealth) { this.bufferRuralWealth = bufferRuralWealth; }

  	public double getTotalUrbanWealth() { return totalUrbanWealth; }
  	public double getTotalRuralWealth() { return totalRuralWealth; }

    public TimeSeriesDataStore<Integer> urbanPopSeries = new TimeSeriesDataStore<Integer>(
            "Urban population");
    public TimeSeriesDataStore<Integer> ruralPopSeries = new TimeSeriesDataStore<Integer>(
            "Rural population");

    // indicate the urban population to Rural area
    public TimeSeriesDataStore<Integer> urbanToRuralSeries = new TimeSeriesDataStore<Integer>(
            "#Urban households become rural households");
    public TimeSeriesDataStore<Integer> urbanToUrbanSeries = new TimeSeriesDataStore<Integer>(
            "#Urban households stay urban households");
    
	public TimeSeriesDataStore<Integer> ruralToUrbanSeries = new TimeSeriesDataStore<Integer>(
            "#Rural households become urban households");
    public TimeSeriesDataStore<Integer> ruralToRuralSeries = new TimeSeriesDataStore<Integer>(
            "#Rural households stay rural households");
    
    public TimeSeriesDataStore<Integer> urbanToRuralMoveSeries = new TimeSeriesDataStore<Integer>(
            "#Urban to rural moves");
    public TimeSeriesDataStore<Integer> urbanToUrbanMoveSeries = new TimeSeriesDataStore<Integer>(
            "#Urban to urban moves");
    
	public TimeSeriesDataStore<Integer> ruralToUrbanMoveSeries = new TimeSeriesDataStore<Integer>(
            "#Rural to urban moves");
    public TimeSeriesDataStore<Integer> ruralToRuralMoveSeries = new TimeSeriesDataStore<Integer>(
            "#Rural to rural moves");

    public TimeSeriesDataStore<Double> totalUrbanDistanceSeries = new TimeSeriesDataStore<Double>(
            "Total distance moved by urban households");
    public TimeSeriesDataStore<Double> totalRuralDistanceSeries = new TimeSeriesDataStore<Double>(
            "Total distance moved by rural households");

    public TimeSeriesDataStore<Integer> cellToUrbanSeries = new TimeSeriesDataStore<Integer>(
            "#Rural cells become urban cells");
    public TimeSeriesDataStore<Integer> cellToRuralSeries = new TimeSeriesDataStore<Integer>(
            "#Urban cells become rural cells");
    
    public TimeSeriesDataStore<Double> wealthGiniSeries = new TimeSeriesDataStore<Double>(
            "Wealth Gini coefficient");

    public TimeSeriesDataStore<Double> urbanWealthGiniSeries = new TimeSeriesDataStore<Double>(
            "Urban Wealth Gini coefficient");
    
    public TimeSeriesDataStore<Double> ruralWealthGiniSeries = new TimeSeriesDataStore<Double>(
            "Rural Wealth Gini coefficient");

    public TimeSeriesDataStore<Double> ruralGiniCoeffSeries = new TimeSeriesDataStore<Double>(
            "Rural Satisfaction Gini coefficient");

    public TimeSeriesDataStore<Double> urbanGiniCoeffSeries = new TimeSeriesDataStore<Double>(
            "Urban Satisfaction Gini coefficient");

    public TimeSeriesDataStore<Double> ruralSatisMeanSeries = new TimeSeriesDataStore<Double>(
            "Rural Satisfaction Mean");

    public TimeSeriesDataStore<Double> urbanSatisMeanSeries = new TimeSeriesDataStore<Double>(
            "Urban Satisfaction Mean");

    public TimeSeriesDataStore<Double> ruralSatisStdevSeries = new TimeSeriesDataStore<Double>(
            "Rural Satisfaction Std. Dev.");

    public TimeSeriesDataStore<Double> urbanSatisStdevSeries = new TimeSeriesDataStore<Double>(
            "Urban Satisfaction Std. Dev.");

    public TimeSeriesDataStore<Double> ruralSatisKurtosisSeries = new TimeSeriesDataStore<Double>(
            "Rural Satisfaction Kurtosis");

    public TimeSeriesDataStore<Double> urbanSatisKurtosisSeries = new TimeSeriesDataStore<Double>(
            "Urban Satisfaction Kurtosis");

    public TimeSeriesDataStore<Double> federalRevenueSeries = new TimeSeriesDataStore<Double>(
            "Federal Government Revenues");

    public TimeSeriesDataStore<Double> federalAssetsSeries = new TimeSeriesDataStore<Double>(
            "Federal Government Assets");
    
    public TimeSeriesDataStore<Double> totalRevenueSeries = new TimeSeriesDataStore<Double>(
            "Total Federal and Local Government Revenues");

    public TimeSeriesDataStore<Double> totalAssetsSeries = new TimeSeriesDataStore<Double>(
            "Total Federal and Local Government Assets");

    public TimeSeriesDataStore<Double> federalExpensesSeries = new TimeSeriesDataStore<Double>(
            "Federal Expenses");
    
    public TimeSeriesDataStore<Integer> trappedRuralSeries= new TimeSeriesDataStore<Integer>(
            "Trapped Rural Households");
    
    public TimeSeriesDataStore<Integer> trappedUrbanSeries= new TimeSeriesDataStore<Integer>(
            "Trapped Urban Households");
    
    public DataCollector(String prefix, String householdPrefix, NorthLandsMovement model) 
    {
        this.model = model;
        this.pathPrefix = prefix;
        this.householdPrefix = householdPrefix;
    }

    private void resetFile() 
    {
        if (this.writer != null) 
        {
            // first close the old file
            try { this.writer.close(); } 
            catch (IOException e) { e.printStackTrace(); }
        }

		// generate a new file name
        DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss");
        // get current date time with Date()
        Date date = new Date();
        String suffix = dateFormat.format(date);

        try 
        {
            String p = "./" + this.pathPrefix + "-" + suffix + ".csv";
            System.out.println(p);
            File file = new File(p);

            if (!file.exists()) { file.createNewFile(); }
            
            writer = new FileWriter(file);
        } 
        catch (FileNotFoundException e) { System.err.println("Could not open file: " + e.getMessage()); } 
        catch (IOException e) { e.printStackTrace(); }
        // write header
        try 
        {
            writer.append("Time,UrbanPop,RuralPop,TotalUrbanWealth,TotalRuralWealth,");
            writer.append("federalRevenues,federalAssets,totalRevenues,totalAssets,federalExpenses,infrastructureExpenses,");
            
            writer.append("urbanToRural,urbanToUrban,ruralToUrban,ruralToRural,");
            writer.append("bufferUrbanToRural,bufferUrbanToUrban,bufferRuralToUrban,bufferRuralToRural,");
            writer.append("urbanToRuralMove,urbanToUrbanMove,ruralToUrbanMove,ruralToRuralMove,");
            writer.append("bufferUrbanToRuralMove,bufferUrbanToUrbanMove,bufferRuralToUrbanMove,bufferRuralToRuralMove,");

            writer.append("totalUrbanDistance,totalRuralDistance,bufferTotalUrbanDistance,bufferTotalRuralDistance,");

    // fourth plot
            writer.append("cellToUrban,cellToRural,bufferCellToUrban,bufferCellToRural,");
            writer.append("wealthGini,ruralWealthGini,urbanWealthGini,ruralGiniCoeff,urbanGiniCoeff,");
            writer.append("ruralSatisMean,urbanSatisMean,ruralSatisStdev,urbanSatisStdev,ruralSatisKurtosis,urbanSatisKurtosis,");  

            writer.append("\n");
        } 
        catch (IOException e) { System.err.println("IOException: " + e.getMessage()); }        
    }

    public void updateFile() 
    {
        try 
        {
            StringBuffer buffer = new StringBuffer();

            // for Experiment H1-1 and H4
            buffer.append(Double.toString(currentTime));              buffer.append(',');
            buffer.append(Integer.toString(urbanPop));                buffer.append(',');
            buffer.append(Integer.toString(ruralPop));                buffer.append(',');
            buffer.append(Double.toString(totalUrbanWealth));         buffer.append(',');
            buffer.append(Double.toString(totalRuralWealth));         buffer.append(',');
            buffer.append(Double.toString(federalRevenues));          buffer.append(',');
            buffer.append(Double.toString(federalAssets));            buffer.append(',');
            buffer.append(Double.toString(totalRevenues));            buffer.append(',');
            buffer.append(Double.toString(totalAssets));              buffer.append(',');
            buffer.append(Double.toString(federalExpenses));          buffer.append(',');
            buffer.append(Double.toString(infrastructureExpenses));   buffer.append(',');
            
// second plot
            buffer.append(Integer.toString(urbanToRural));            buffer.append(',');
            buffer.append(Integer.toString(urbanToUrban));            buffer.append(',');
            buffer.append(Integer.toString(ruralToUrban));            buffer.append(',');
            buffer.append(Integer.toString(ruralToRural));            buffer.append(',');
            
            buffer.append(Integer.toString(bufferUrbanToRural));      buffer.append(',');
            buffer.append(Integer.toString(bufferUrbanToUrban));      buffer.append(',');
            buffer.append(Integer.toString(bufferRuralToUrban));      buffer.append(',');
            buffer.append(Integer.toString(bufferRuralToRural));      buffer.append(',');
            
            buffer.append(Integer.toString(urbanToRuralMove));        buffer.append(',');
            buffer.append(Integer.toString(urbanToUrbanMove));        buffer.append(',');
            buffer.append(Integer.toString(ruralToUrbanMove));        buffer.append(',');
            buffer.append(Integer.toString(ruralToRuralMove));        buffer.append(',');
            
            buffer.append(Integer.toString(bufferUrbanToRuralMove));  buffer.append(',');
            buffer.append(Integer.toString(bufferUrbanToUrbanMove));  buffer.append(',');
            buffer.append(Integer.toString(bufferRuralToUrbanMove));  buffer.append(',');
            buffer.append(Integer.toString(bufferRuralToRuralMove));  buffer.append(',');
            
// third plot  
            buffer.append(Double.toString(totalUrbanDistance));       buffer.append(',');
            buffer.append(Double.toString(totalRuralDistance));       buffer.append(',');
            buffer.append(Double.toString(bufferTotalUrbanDistance)); buffer.append(',');
            buffer.append(Double.toString(bufferTotalRuralDistance)); buffer.append(',');

// fourth plot
            buffer.append(Integer.toString(cellToUrban));             buffer.append(',');
            buffer.append(Integer.toString(cellToRural));             buffer.append(',');
            buffer.append(Integer.toString(bufferCellToUrban));       buffer.append(',');
            buffer.append(Integer.toString(bufferCellToRural));       buffer.append(',');
            buffer.append(Double.toString(wealthGini));               buffer.append(',');
            buffer.append(Double.toString(ruralWealthGini));          buffer.append(',');
            buffer.append(Double.toString(urbanWealthGini));          buffer.append(',');
            buffer.append(Double.toString(ruralGiniCoeff));           buffer.append(',');
            buffer.append(Double.toString(urbanGiniCoeff));           buffer.append(',');
            buffer.append(Double.toString(ruralSatisMean));           buffer.append(',');
            buffer.append(Double.toString(urbanSatisMean));           buffer.append(',');
            buffer.append(Double.toString(ruralSatisStdev));          buffer.append(',');
            buffer.append(Double.toString(urbanSatisStdev));          buffer.append(',');
            buffer.append(Double.toString(ruralSatisKurtosis));       buffer.append(',');
            buffer.append(Double.toString(urbanSatisKurtosis));       buffer.append(',');
            
            buffer.append('\n');
            
            writer.append(buffer.toString());
            writer.flush();
        } 
        catch (IOException e) { System.err.println("IOException: " + e.getMessage()); }
    }

    public void close() 
    {
        try { writer.close(); } 
        catch (IOException e) { System.err.println("IOException: " + e.getMessage()); }
    }
    
    private void resetHouseholdFile() 
    {
        if (this.householdWriter != null) 
        {   // first close the old file
            try { this.householdWriter.close(); } 
            catch (IOException e) { e.printStackTrace(); }
        }

		// generate a new file name
        DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss");
        // get current date time with Date()
        Date date = new Date();
        String suffix = dateFormat.format(date);

        try 
        {
            String p = "./" + this.householdPrefix + "-" + suffix + ".csv";
            System.out.println(p);
            File file = new File(p);

            if (!file.exists()) { file.createNewFile(); }
            householdWriter = new FileWriter(file);
        } 
        catch (FileNotFoundException e) { System.err.println("Could not open file: " + e.getMessage()); } 
        catch (IOException e) { e.printStackTrace(); }
    }

    public void updateHouseholdFile(Household h) {
        try 
        {	
            StringBuffer buffer = new StringBuffer();
            
            if (!columnLabels) 
            {	
            	buffer.append("Time");
                buffer.append(',');

                buffer.append("X_coordinate");
                buffer.append(',');
                
                buffer.append("Y_coordinate");
                buffer.append(',');
                
                buffer.append("Wealth");
                buffer.append(',');
                
                buffer.append("Major_cell_factor");
                buffer.append('\n');
                
                columnLabels=true;	
            }
            
            buffer.append(Double.toString(currentTime));
            buffer.append(',');

            buffer.append(Double.toString(h.getCurrentCell().x));
            buffer.append(',');
            
            buffer.append(Double.toString(h.getCurrentCell().y));
            buffer.append(',');
            
            buffer.append(Double.toString(h.wealth));
            buffer.append(',');
            
            if (h.typeFlag==0) 
            {
            	Factor factor = h.getCurrentCell().urbanMajorFactor;
            	buffer.append(factor.toString());
            }
            else 
                if (h.typeFlag==1) 
                {
                    Factor factor = h.getCurrentCell().ruralMajorFactor;
                    buffer.append(factor.toString());
                }
            
            buffer.append('\n');
          
            householdWriter.append(buffer.toString());
            householdWriter.flush();
        } 
        catch (IOException e) { System.err.println("IOException: " + e.getMessage()); }
    }

    public void closeHousehold() 
    {
        try { householdWriter.close(); }
        catch (IOException e) { System.err.println("IOException: " + e.getMessage()); }
    }

    public void setIteration(int counter) { iteration = counter; }

    public void setTime(double time) { currentTime = time; }

    public void setRuralPop(int ruralResidence) { ruralPop = ruralResidence; }
    public void setUrbanPop(int urbanResidence) { urbanPop = urbanResidence; }
    
    public void setTotalUrbanWealth(double wealth) { totalUrbanWealth = wealth; } // household has wealth, which becomes urban or rural
    public void setTotalRuralWealth(double wealth) { totalRuralWealth = wealth; } // depending on where it is (was totalUrbanWealth)
    
    public void setWealthGini(double gini) { wealthGini = gini; }
    public void setUrbanWealthGini(double gini) { urbanWealthGini = gini; }
    public void setRuralWealthGini(double gini) { ruralWealthGini = gini; }

    public void setRuralGiniCoeff(double gini) { ruralGiniCoeff = gini; }
    public void setUrbanGiniCoeff(double gini) { urbanGiniCoeff = gini; }

    public void setRuralDesireMean(double val) { ruralSatisMean = val; }
    public void setUrbanDesireMean(double val) { urbanSatisMean = val; }

    public void setRuralDesireStdev(double val) { ruralSatisStdev = val; }
    public void setUrbanDesireStdev(double val) { urbanSatisStdev = val; }

    public void setRuralDesireKurtosis(double val) { ruralSatisKurtosis = val; }
    public void setUrbanDesireKurtosis(double val) { urbanSatisKurtosis = val; }

    public void setFederalRevenues(double val) { federalRevenues = val; }
    public void setFederalAssets(double val) { federalAssets = val; }
    
    public void setTotalRevenues(double val) { totalRevenues = val; }
    public void setTotalAssets(double val) { totalAssets = val; }
    public void setFederalExpenses(double val) { federalExpenses = val; }
    public void setInfrastructureExpenses(double val) { infrastructureExpenses = val; }
    public double getInfrastructureExpenses() { return infrastructureExpenses; }

    public void incrementUrbanWealth(double x) { bufferUrbanWealth += x; }
    public void incrementRuralWealth(double x) { bufferRuralWealth += x; }

    public void incrementTrappedRural() { bufferTrappedRural++; }
    public void incrementTrappedUrban() { bufferTrappedUrban++; }
    
    public void incrementUrbanToUrban() { bufferUrbanToUrban++; }
    public void incrementRuralToUrban() { bufferRuralToUrban++; }

    public void incrementRuralToRural() { bufferRuralToRural++; }
    public void incrementUrbanToRural() { bufferUrbanToRural++; }
    
    public void incrementUrbanToRuralMove() { bufferUrbanToRuralMove++; }
    public void incrementUrbanToUrbanMove() { bufferUrbanToUrbanMove++; }

    public void incrementRuralToUrbanMove() { bufferRuralToUrbanMove++; }
    public void incrementRuralToRuralMove() { bufferRuralToRuralMove++; }


    public void incrementCellToRural() { bufferCellToRural++; }
    public void incrementCellToUrban() { bufferCellToUrban++; }

    public void incrementUrbanDistance(double distance) { bufferTotalUrbanDistance += distance; }
    public void incrementRuralDistance(double distance) { bufferTotalRuralDistance += distance; }

    public int getUrbanToRural() { return urbanToRural; }
    public int getUrbanToUrban() { return urbanToUrban; }
    public int getRuralToUrban() { return ruralToUrban; }
    public int getRuralToRural() { return ruralToRural; }
    
    public int getUrbanToRuralMove() { return urbanToRuralMove; }
    public int getUrbanToUrbanMove() { return urbanToUrbanMove; }
    public int getRuralToUrbanMove() { return ruralToUrbanMove; }
    public int getRuralToRuralMove() { return ruralToRuralMove; }
    
    public double getTotalUrbanDistance() { return totalUrbanDistance; }
    public double getTotalRuralDistance() { return totalRuralDistance; }

    public int getCellToUrban() { return cellToUrban; }
    public int getCellToRural() { return cellToRural; }

    public int getTrappedRural() { return trappedRural; }
	public int getTrappedUrban() { return trappedUrban; }

    
    private void resetCounter() 
    {
        bufferUrbanToRural = 0;
        bufferUrbanToUrban = 0;
        bufferRuralToUrban = 0;
        bufferRuralToRural = 0;
        
        bufferUrbanToRuralMove = 0;
        bufferUrbanToUrbanMove = 0;
        bufferRuralToUrbanMove = 0;
        bufferRuralToRuralMove = 0;

        bufferTotalUrbanDistance = 0;
        bufferTotalRuralDistance = 0;

        bufferCellToUrban = 0;
        bufferCellToRural = 0;
        
        bufferTrappedRural = 0;
        bufferTrappedUrban = 0;
        
        bufferUrbanWealth = 0;
        bufferRuralWealth = 0;
    }

    public void initialize(double firstDate) 
    {
        this.setIteration(0);
        this.setTime(firstDate);
        this.setRuralPop(model.ruralResidence);
        this.setUrbanPop(model.urbanResidence);
    }

    // reset all staff
    public void resetAll(boolean recordData) 
    {
        iteration = -1;
        currentTime = 0.0;
        urbanPop = 0;
        ruralPop = 0;
        totalUrbanWealth = 0;
        totalRuralWealth = 0;

        urbanToRural = 0;
        urbanToUrban = 0;
        ruralToUrban = 0;
        ruralToRural = 0;
        
        urbanToRuralMove = 0;
        urbanToUrbanMove = 0;
        ruralToUrbanMove = 0;
        ruralToRuralMove = 0;

        totalUrbanDistance = 0;
        totalRuralDistance = 0;

        cellToUrban = 0;
        cellToRural = 0;
        
        trappedRural=0;
        trappedUrban=0;

        // reset the output file
        if (recordData) 
        {
            this.resetFile();
            this.resetHouseholdFile();
        }

        // reset some data counter
        this.resetCounter();

        urbanPopSeries.clear();
        ruralPopSeries.clear();
        urbanToRuralSeries.clear();
        urbanToUrbanSeries.clear();
        ruralToUrbanSeries.clear();
        ruralToRuralSeries.clear();
        urbanToRuralMoveSeries.clear();
        urbanToUrbanMoveSeries.clear();
        ruralToUrbanMoveSeries.clear();
        ruralToRuralMoveSeries.clear();
        totalUrbanDistanceSeries.clear();
        totalRuralDistanceSeries.clear();
        cellToUrbanSeries.clear();
        cellToRuralSeries.clear();
        wealthGiniSeries.clear();
        ruralWealthGiniSeries.clear();
        urbanWealthGiniSeries.clear();
        ruralGiniCoeffSeries.clear();
        urbanGiniCoeffSeries.clear();
        ruralSatisMeanSeries.clear();
        urbanSatisMeanSeries.clear();
        ruralSatisStdevSeries.clear();
        urbanSatisStdevSeries.clear();
        ruralSatisKurtosisSeries.clear();
        urbanSatisKurtosisSeries.clear();
        federalRevenueSeries.clear();
        federalAssetsSeries.clear();
        totalRevenueSeries.clear();
        totalAssetsSeries.clear();
        federalExpensesSeries.clear();
        trappedRuralSeries.clear();
        trappedUrbanSeries.clear();
    }

    private void update() 
    {
        totalUrbanWealth = bufferUrbanWealth;
        totalRuralWealth = bufferRuralWealth;
        
        urbanToRural = bufferUrbanToRural;
        urbanToUrban = bufferUrbanToUrban;
        ruralToUrban = bufferRuralToUrban;
        ruralToRural = bufferRuralToRural;
        
        urbanToRuralMove = bufferUrbanToRuralMove;
        urbanToUrbanMove = bufferUrbanToUrbanMove;
        ruralToUrbanMove = bufferRuralToUrbanMove;
        ruralToRuralMove = bufferRuralToRuralMove;

        totalUrbanDistance = bufferTotalUrbanDistance;
        totalRuralDistance = bufferTotalRuralDistance;

        cellToUrban = bufferCellToUrban;
        cellToRural = bufferCellToRural;
        
        trappedRural = bufferTrappedRural;
        trappedUrban = bufferTrappedUrban;

        //double year = ((double)this.iteration)/4;
        double year = this.currentTime;

        urbanPopSeries.addDataPoint(year, this.urbanPop);
        ruralPopSeries.addDataPoint(year, this.ruralPop);
        urbanToRuralSeries.addDataPoint(year, this.urbanToRural);
        urbanToUrbanSeries.addDataPoint(year, this.urbanToUrban);
        ruralToUrbanSeries.addDataPoint(year, this.ruralToUrban);
        ruralToRuralSeries.addDataPoint(year, this.ruralToRural);
        urbanToRuralMoveSeries.addDataPoint(year, this.urbanToRuralMove);
        urbanToUrbanMoveSeries.addDataPoint(year, this.urbanToUrbanMove);
        ruralToUrbanMoveSeries.addDataPoint(year, this.ruralToUrbanMove);
        ruralToRuralMoveSeries.addDataPoint(year, this.ruralToRuralMove);
        totalUrbanDistanceSeries.addDataPoint(year,
                this.totalUrbanDistance);
        totalRuralDistanceSeries.addDataPoint(year,
                this.totalRuralDistance);
        cellToUrbanSeries.addDataPoint(year, this.cellToUrban);
        cellToRuralSeries.addDataPoint(year, this.cellToRural);
        wealthGiniSeries.addDataPoint(year, this.wealthGini);
        ruralWealthGiniSeries.addDataPoint(year, this.ruralWealthGini);
        urbanWealthGiniSeries.addDataPoint(year, this.urbanWealthGini);
        ruralGiniCoeffSeries.addDataPoint(year, this.ruralGiniCoeff);
        urbanGiniCoeffSeries.addDataPoint(year, this.urbanGiniCoeff);
        ruralSatisMeanSeries.addDataPoint(year, this.ruralSatisMean);
        urbanSatisMeanSeries.addDataPoint(year, this.urbanSatisMean);
        ruralSatisStdevSeries.addDataPoint(year, this.ruralSatisStdev);
        urbanSatisStdevSeries.addDataPoint(year, this.urbanSatisStdev);
        ruralSatisKurtosisSeries.addDataPoint(year, this.ruralSatisKurtosis);
        urbanSatisKurtosisSeries.addDataPoint(year, this.urbanSatisKurtosis);
        federalRevenueSeries.addDataPoint(year, this.federalRevenues);
        federalAssetsSeries.addDataPoint(year, this.federalAssets);
        totalRevenueSeries.addDataPoint(year, this.totalRevenues);
        totalAssetsSeries.addDataPoint(year, this.totalAssets);
        federalExpensesSeries.addDataPoint(year, this.federalExpenses);
        trappedRuralSeries.addDataPoint(year, this.trappedRural);
        trappedUrbanSeries.addDataPoint(year, this.trappedUrban);
    }

    // We probably don't need this, but I'll include it anyway, just in case.
    public void step(int counter) 
    {
        this.step(counter, counter / 4.0);
    }

    public void step(int counter, double time) 
    {
        // first store the data into data structures
        this.update();

        // grab some data from model
        this.setIteration(counter);
        this.setTime(time);
        this.setRuralPop(model.ruralResidence);
        this.setUrbanPop(model.urbanResidence);

        // reset some data counter
        this.resetCounter();
    }
}
