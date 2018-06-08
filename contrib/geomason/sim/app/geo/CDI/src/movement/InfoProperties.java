package CDI.src.movement;

public class InfoProperties {

	NorthLandsMovement model;
	NorthLandsMovementWithUI modelUI;
	
	
	public int getPopLowerBound() {
		return modelUI.householdHistogramLowerBound;
	}
	public void setPopLowerBound(int lower) {
		modelUI.householdHistogramLowerBound = lower;
	}
	public int getPopUpperBound() {
		return modelUI.householdHistogramUpperBound;
	}
	public void setPopUpperBound(int upper) {
		modelUI.householdHistogramUpperBound = upper;
	}
	
	
	public InfoProperties(NorthLandsMovementWithUI modelUI) {
		this.modelUI = modelUI;
		this.model = modelUI.getModel();
	}
	

	
//	public double[] getTemperatureDes()
//	{
//		return model.map.getTempDesData();
//	}
	
//	public double[] getTemperature()
//	{
//		return model.map.getTemperatureData();
//	}
	
	
	
	public int getRuralResidence() {
		return model.ruralResidence;
	}
	
	public String desRuralResidence()
	{
		return "Rural population";
	}

	
	public int getUrbanResidence() {
		return model.urbanResidence;
	}

	public String desUrbanResidence()
	{
		return "Urban population";
	}

	
	public int getTotalPopulation()
	{
		return model.urbanResidence+model.ruralResidence;
	}
	
	public String desTotalResidence()
	{
		return "Population in Canada";
	}
	
	public int getUrbanToRural()
	{
		return model.collector.getUrbanToRural();
	}
	
	public String desUrbanToRural()
	{
		return "How many urban people become rural people";
	}
	
	
	public int getRuralToRural()
	{
		return model.collector.getRuralToRural();
	}
	
	public String desRuralToRural()
	{
		return "How many rural people stay rural";
	}
	
	public int getUrbanToUrban()
	{
		return model.collector.getUrbanToUrban();
	}
	
	public String desUrbanToUrban()
	{
		return "How many urban people stay urban";
	}
	
	public int getRuralToUrban()
	{
		return model.collector.getRuralToUrban();
	}

	public String desRuralToUrban()
	{
		return "How many rural people become urban people";
	}
	
	public int getCellToUrban()
	{
		return model.collector.getCellToUrban();
	}
	
	public String desCellToUrban()
	{
		return "How many rural cells become urban cells";
	}
	
	public int getCellToRural()
	{
		return model.collector.getCellToRural();
	}
	
	public String desCellToRural()
	{
		return "How many urban cells become rural cells";
	}
	
	public double getUrbanDistance()
	{
		return model.collector.getTotalUrbanDistance();
	}
	
	public String desUrbanDistance()
	{
		return "Total distance move by urban people";
	}
	
	public double getRuralDistance()
	{
		return model.collector.getTotalRuralDistance();
	}
	
	public String desRuralDistance()
	{
		return "Total distance move by rural people";
	}
}
