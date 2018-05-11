package optimization.util ;

/**
 * This class generates a terrain,
 * currently we have two algorithms -- 
 * a) Random terrain
 * b) Fractal Terrain using the Diamond Square algorithm.
 */

import java.util.* ;
import java.io.* ;

public class Terrain
{
	/** All terrain properties */
	private double[][] data ; // the terrain
	private int width ; // dimensions
	private int height ;
	private Random rng ; // an RNG

	/** Constructors */
	public Terrain(int width, int height)
	{
		this.width = width ;
		this.height = height ;
		this.data = new double[this.width][this.height] ;
		this.rng = new Random();
	}

	public Terrain(int width, int height, Random rng)
	{
		this.width = width ;
		this.height = height ;
		this.data = new double[this.width][this.height] ;
		this.rng = rng ;
	}

	public void setRandomSeed(Random rng)
	{
		this.rng = rng ;
	}
	
	/** Sets a value to [i,j] location */
	public void set(int x, int y, double val)
	{
		if(x < this.width && y < this.height)
			this.data[x][y] = val ;
	}

	/** Gets the dimension */
	public int getWidth() { return this.width ; }
	public int getHeight() { return this.height ; }

	/** Gets a value from [i,j] location */
	public double get(int x, int y)
	{
		if(x < this.width && y < this.height)
			return this.data[x][y] ;
		else
			return -1 ;
	}

	/** Get the reference of the whole terrain */
	public double[][] getData()
	{
		return this.data ;
	}

	/** Copy the whole terrain data */
	public double[][] copyData()
	{
		double[][] copy = new double[this.width][this.height] ;
		for(int i = 0 ; i < this.width ; i++)
			for(int j = 0 ; j < this.height ; j++)
				copy[i][j] = this.data[i][j] ;
		return copy ;
	}

	/** Sets the values of the whole terrain */
	public void setData(double[][] src)
	{
		for(int i = 0 ; i < this.width ; i++)
			for(int j = 0 ; j < this.height ; j++)
			{
				if(i < src.length && j < src[0].length)
					this.data[i][j] = src[i][j] ;
			}
	}

	/**
	 *  Generate a random terrain
	 */
	public void generateRandomTerrain(double high, double low)
	{
		for(int i = 0 ; i < this.width ; i++)
			for(int j = 0 ; j < this.height ; j++)
				this.data[i][j] = low + (this.rng.nextDouble() * (high - low));
	}

	/**
	 * Seed the terrain.
	 */
	private void seed(int x, int y, int size)
	{
		int s = size - 1;
		this.data[x][y] = this.rng.nextDouble();
		this.data[x][y + s] = this.rng.nextDouble();
		this.data[x + s][y] = this.rng.nextDouble();
		this.data[x + s][y + s] = this.rng.nextDouble();
	}

	/**
	 * Calculate displacement
	 */
	private double displacement(int i)
	{
		double val = this.rng.nextDouble();
		val = ((val * 2) - 1) / (1 << (i + 1));
		return val;
	}

	/**
	 * The Diamond-Square algorithm
	 */
	private void performDiamondSquare(int x, int y, int size, int i)
	{
		int halfSize = size / 2;
		int xHalf = x + halfSize;
		int yHalf = y + halfSize;
		int s = size - 1;
		double val;

		// diamond
		double x0y0 = get(x, y);
		double x0y1 = get(x, y + s);
		double x1y0 = get(x + s, y);
		double x1y1 = get(x + s, y + s);
		double x0Avg = (x0y0 + x0y1) / 2.0;
		double x1Avg = (x1y0 + x1y1) / 2.0;
		double avg = (x0Avg + x1Avg) / 2.0;
		double displacement = this.displacement(i);
		val = avg + displacement;
		if (val > 1 || val < 0)
			val = avg;
		set(xHalf, yHalf, val);

		// square
		double y0Avg = (x0y0 + x1y0) / 2.0;
		double y1Avg = (x0y1 + x1y1) / 2.0;
		this.data[xHalf][y] = y0Avg;
		this.data[xHalf][y + s] = y1Avg;
		this.data[x][yHalf] = x0Avg;
		this.data[x + s][yHalf] = x1Avg;

		if (halfSize > 1)
		{
			i++;
			int halfSizePlus = halfSize + 1;
			this.performDiamondSquare(x, y, halfSizePlus, i);
			this.performDiamondSquare(xHalf, y, halfSizePlus, i);
			this.performDiamondSquare(x, yHalf, halfSizePlus, i);
			this.performDiamondSquare(xHalf, yHalf, halfSizePlus, i);
		}
	}

	/**
	 * Generate a fractal terrain, using the diamond-square algorithm
	 */
	public void generateFractalTerrain()
	{
		int len = Math.min(this.width, this.height);
		seed(0, 0, len);
		performDiamondSquare(0, 0, len, 0);
	}

	/**
	 * Save the data in a file
	 */
	public void save(String fileName) throws Exception
	{
		File file = new File(fileName);
		BufferedWriter output = new BufferedWriter(new FileWriter(file, false));
		for(int i = 0 ; i < this.width ; i++)
		{
			for(int j = 0 ; j < this.height ; j++)
				output.write(i + "\t" + j + "\t" + this.data[i][j] + "\n");
		}
		output.close();
	}

	/** Saves the given data to a file */
	public static void save(String fileName, double[][] data) throws Exception
	{
		File file = new File(fileName);
		BufferedWriter output = new BufferedWriter(new FileWriter(file, false));
		for(int i = 0 ; i < data.length ; i++)
		{
			for(int j = 0 ; j < data[0].length ; j++)
				output.write(i + "\t" + j + "\t" + data[i][j] + "\n");
		}
		output.close();
	}

	/**
	 * Tester function
	 */
	public static void main(String args[]) throws Exception
	{
		Terrain tg = new Terrain(50, 50);
		tg.generateRandomTerrain(0, 500);
		// tg.generateFractalTerrain();
		tg.save("terrain.dat");
	}
}
