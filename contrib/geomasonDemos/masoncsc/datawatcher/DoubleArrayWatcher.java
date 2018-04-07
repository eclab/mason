package masoncsc.datawatcher;

/**
 * 
 * @author Joey Harrison
 * @author Eric 'Siggy' Scott
 *
 */
abstract public class DoubleArrayWatcher extends DataWatcher<double[]>
{
	protected double[] data;

	@Override
	public double[] getDataPoint() {
		return data;
	}

	@Override
	public String dataToCSV() {
		if (data.length < 1)
			return "";
		
		StringBuilder sb = new StringBuilder(Double.toString(data[0]));
		for (int i = 1; i < data.length; i++)
			sb.append(", ").append(data[i]);				
		
		return sb.toString();
	}
}
