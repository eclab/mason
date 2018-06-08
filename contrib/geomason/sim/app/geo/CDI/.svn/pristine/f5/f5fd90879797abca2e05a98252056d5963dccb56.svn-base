package optimization.util ;

/**
 * This class implements a simple Gaussian kernel
 * for 2D smoothing operation. -- khaled
 */

public class GaussianFilter extends Filter
{
	private double sigma = 0.0 ;

	public GaussianFilter(int rows, int cols, double sigma)
	{
		super(rows, cols);
		this.sigma = sigma ;
		this.buildKernel();
	}

	/**
	 * Builds up a Gaussian kernel
	 */
	protected void buildKernel()
	{
		double rowMean = super.rows/2;
		double colMean = super.cols/2;
		double sum = 0.0 ;
		for (int i = 0 ; i < super.rows ; i++)
			for (int j = 0 ; j < super.cols ; j++)
			{
				double val = Math.exp(-0.5 * (Math.pow((i - rowMean)/this.sigma, 2.0) 
							+ Math.pow((j - colMean)/this.sigma, 2.0))) / 
								(2 * Math.PI * this.sigma * this.sigma);
				sum += val ;
				super.kernel[i][j] = val ;
			}

		for(int i = 0 ; i < super.rows ; i++)
		{
			for(int j = 0 ; j < super.cols ; j++)
		
			{
				double val = super.kernel[i][j];
				val = val/sum ;
				super.kernel[i][j]  = val;
				//System.out.print(val+" ");
			}
			//System.out.println();
		}
		
		// also store the kernel into a row major order linear array
		super.kernel_ = super.vectorize(super.kernel);
	}

	/**
	 * A tester function
	 */
	public static void main(String args[]) throws Exception
	{
		Terrain t = new Terrain(100, 100);
		t.generateRandomTerrain(0, 500);
		t.save("orig-terrain.dat");
		
		GaussianFilter f = new GaussianFilter(13,13,1.0) ;
		// System.err.println("Filter:\n" + f.toString());

		double[][] result = null ;
		for(int i = 0 ; i < 10 ; i++)
		{
			System.err.print("GaussianFilter.main() : " 
					+ "Filtering pass: " + i + " :: ");
			result = t.getData();
			long startTime = System.currentTimeMillis();
				result = f.applyFastFilter(result) ;
				// result = f.applyFilter(result) ;
			long endTime = System.currentTimeMillis();
			t.setData(result);
			long elapsedTime = endTime - startTime ;
			System.err.print(elapsedTime/1000.0 + "sec.\n");
			System.err.println("GaussianFilter.main() : " 
					+ "Saving smooth-terrain-" + i + ".dat");
			t.save("smooth-terrain-" + i + ".dat");
		}

		int pass = 11 ;
		t.generateRandomTerrain(0, 100);
		long startTime = System.currentTimeMillis();
			result = f.applyFastFilter(t.getData(), pass) ;
			// result = f.applyFilter(t.getData(), pass) ;
		long endTime = System.currentTimeMillis();
		t.setData(result);
		long elapsedTime = endTime - startTime ;
		System.err.print(elapsedTime/1000.0 + "sec.\n");
		System.err.println("GaussianFilter.main() : " 
				+ "Saving smooth-terrain-" + (pass-1) + ".dat");
		t.save("smooth-terrain-" + (pass-1) + ".dat");
	}
}
