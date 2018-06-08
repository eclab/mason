package CDI.src.phases.util;

import CDI.src.optimization.util.Filter;
import CDI.src.environment.Cell;
import CDI.src.environment.Map;
import sim.field.grid.Grid2D;
import sim.util.*;

/**
 * 
 * @author Ermo Wei
 * To do the smooth on the grid with an gaussian kernel
 * For the convolution, if there is a missing value for the kernel
 * we simply ignore it and normalize the remain distribution in the kernel to 1
 */
public class GaussianFilter {
	private double sigma = 0.0;
	
	
	/**
     * Dimension of the kernel, 
     * and it is always an odd interger.
     */
    private int rows = 0 ; // height
    private int cols = 0 ; // width
    private int kCenterX = 0;
    private int kCenterY = 0;

    /** This is the kernel */
    private double[][] kernel = null ;


	public GaussianFilter(int rows, int cols, double sigma)
	{
		this.rows = rows;
		this.cols = cols;
		this.kernel = new double[this.rows][this.cols];
		this.kCenterX = rows/2;
		this.kCenterY = cols/2;
		this.sigma = sigma ;
		this.buildKernel();
	}


    // Getters
    public int getWidth() { return this.cols ; }
    public int getHeight() { return this.rows ; }
    public double getValue(int i, int j) { return this.kernel[i][j] ;} 
	
	
	/**
	 * Builds up a Gaussian kernel
	 */
	protected void buildKernel()
	{
		double rowMean = this.rows/2;
		double colMean = this.cols/2;
		double sum = 0.0 ;
		for (int i = 0 ; i < this.rows ; i++)
			for (int j = 0 ; j < this.cols ; j++)
			{
				double val = Math.exp(-0.5 * (Math.pow((i - rowMean)/this.sigma, 2.0) 
							+ Math.pow((j - colMean)/this.sigma, 2.0))) / 
								(2 * Math.PI * this.sigma * this.sigma);
				sum += val ;
				this.kernel[i][j] = val ;
			}

		for(int i = 0 ; i < this.rows ; i++)
		{
			for(int j = 0 ; j < this.cols ; j++)
		
			{
				double val = this.kernel[i][j];
				val = val/sum ;
				this.kernel[i][j]  = val;
			}
		}
		
	}
	
	/**
	 * Smoothed grid with convolution
	 * @param grid
	 * @return the smoothed grid
	 */
	public double[][] applyFilter(double[][] grid, Map map)
    {
		return applyFilter(grid,1,map);
    }
	
	
    public double[][] applyFilter(double[][] grid, int pass, Map map)
    {
    	double [][] originalGrid = grid;
    	
        // do the convolution
        double[][] result = null;
        for(int i = 0 ; i < pass ; ++i)
        {
            result = this.applyConvolution(grid, map);
            grid = result;
        }
        
        result = adjustGrid(originalGrid,result,map);
        
        return result ;
    }

    /**
     * After the smooth, the total numHouseholds may differ from the original numHouseholds
 so we time each cell an coefficient
     * @param originalGrid
     * @param result
     * @return the adjusted Grid
     */
    private double[][] adjustGrid(double[][] originalGrid, double[][] result,Map map) {
    	double sumSmoothed = 0,sumPop = 0;
    	int w = originalGrid.length;
    	int l = originalGrid[0].length;
		for(int i = 0;i<w;++i)
		{
			for(int j = 0;j<l;++j)
			{
				if(isCanadaCell(map.cellGrid.field[i][j],map))
					sumPop += originalGrid[i][j];
			}
		}
		w = result.length;
    	l = result[0].length;
    	for(int i = 0;i<w;++i)
		{
			for(int j = 0;j<l;++j)
			{
				if(isCanadaCell(map.cellGrid.field[i][j],map))
					sumSmoothed += result[i][j];
			}
		}
    	double coefficient = sumPop/sumSmoothed;
    	for(int i = 0;i<w;++i)
		{
			for(int j = 0;j<l;++j)
			{
				if(isCanadaCell(map.cellGrid.field[i][j],map))
					result[i][j] *= coefficient;
			}
		}
    	return result;
	}


	/**
     * This function is same as before, but only does the convolution
     * on a specific region of the map, like canada.
     */    
    private double[][] applyConvolution(double[][] terrain, Map map)
    {
    	int w = terrain.length;
        int h = terrain[0].length;
        int dist = this.rows/2;
        double[][] result = new double[w][h];
        for (int i = 0 ; i < terrain.length; i++) {
            System.arraycopy(terrain[i], 0, result[i], 0, terrain[0].length);
        }
        
        IntBag xBag = new IntBag(),yBag = new IntBag();
    	for(Cell cell:map.canadaCells)
    	{
    		double sumWeight = 0;
    		// get the neighborhood
    		map.cellGrid.getRadialLocations(cell.x, cell.y, dist, Grid2D.BOUNDED, true, xBag, yBag);
    		int length = xBag.numObjs;
    		// go through all the neigborhood cell, check if there are all validate cell
    		for(int i = 0;i<length;++i)
    		{
    			int x = xBag.get(i);
    			int y = yBag.get(i);
    			//System.out.println("cell is "+cell.x+","+cell.y+"x is "+x+"y is "+y);
    			if(isCanadaCell(map.cellGrid.field[x][y],map))
    			{
    				sumWeight += relativeKernelWeight(cell.x,cell.y,x,y);
    			}
    		}
    		// convolute the cell with the sumWeight
    		double sum = 0;
    		for(int i = 0;i<length;++i)
    		{
    			int x = xBag.get(i);
    			int y = yBag.get(i);
    			if(isCanadaCell(map.cellGrid.field[x][y], map))
    			{
    				sum += (terrain[x][y]*relativeKernelWeight(cell.x,cell.y,x,y)/sumWeight);
    			}
    		}
    		result[cell.x][cell.y] = sum;
    		
    		
    	}
        return result ;
    }
    
    private boolean isCanadaCell(Object object, Map map)
    {
    	if(object==null)
    		return false;
    	Cell cell = (Cell)object;
    	return map.indexMap.containsKey(cell);
    }
    
    
    private double relativeKernelWeight(int centerX, int centerY, int x, int y)
    {
    	int relativeX = x - centerX, relativeY = y - centerY;
    	return kernel[kCenterX+relativeX][kCenterY+relativeY];
    }
    
    public String toString()
    {
        String str = "" ;
        for(int i = 0 ; i < this.rows ; i++)
        {
            for(int j = 0 ; j < this.cols ; j++)
                str += String.format("%.8f ",this.kernel[i][j]);
            str += "\n" ;
        }        
        return str;
    }
	
	
	public static void main(String[] args)
	{
		//GaussianFilter filter = new GaussianFilter(7,7,0.84089642);
		//System.out.print(filter.toString());
	
		
		double[][] array0 = new double[5][5];
		double[][] array = new double[5][5];
		double[][] array2 = new double[5][5];
		double[][] array3 = new double[5][5];
		double[][] array4 = new double[5][5];
		double[][] array5 = new double[5][5];
		int index = 0;
		for(int i = 0;i<5;++i)
		{
			for(int j = 0;j<5;++j)
			{
				double a = Math.random();
				array0[i][j] = a;
				array[i][j] = a * Math.random();
				array2[i][j] = a - array[i][j];
				array3[i][j] = 0;
				array4[i][j] = 0;
				array5[i][j] = 0;
				index++;
			}
		}
			
		
		GaussianFilter filter = new GaussianFilter(3,3,1);
		System.out.print(filter.toString());
		System.out.println();
		// start from the inside
		/*
		for(int i = 1;i<4;++i)
		{
			for(int j = 1;j<4;++j)
			{
				double sum = 0;
				for(int ii = -1;ii<=1;++ii)
				{
					for(int jj=-1;jj<=1;++jj)
					{
						sum+=filter.relativeKernelWeight(i, j, i+ii, j+jj)*array[i+ii][j+jj];
					}
				}
				array2[i][j] = sum;
			}
		}*/
		
		for(int i = 0;i<5;++i)
		{
			for(int j = 0;j<5;++j)
			{
				double sumWeight = 0;
				for(int ii = -1;ii<=1;++ii)
				{
					for(int jj=-1;jj<=1;++jj)
					{
						int x = i+ii,y = j+jj;
						if((x<=4&&x>=0)&&(y<=4&&y>=0))
							sumWeight+=filter.relativeKernelWeight(i, j, x, y);
					}
				}
				double sum = 0;
				for(int ii = -1;ii<=1;++ii)
				{
					for(int jj=-1;jj<=1;++jj)
					{
						int x = i+ii,y = j+jj;
						if((x<=4&&x>=0)&&(y<=4&&y>=0))
						{
							sum+=filter.relativeKernelWeight(i, j, x, y)*array[x][y]/sumWeight;
						}
					}
				}
				array3[i][j] = sum;
			}
		}
		
		for(int i = 0;i<5;++i)
		{
			for(int j = 0;j<5;++j)
			{
				double sumWeight = 0;
				for(int ii = -1;ii<=1;++ii)
				{
					for(int jj=-1;jj<=1;++jj)
					{
						int x = i+ii,y = j+jj;
						if((x<=4&&x>=0)&&(y<=4&&y>=0))
							sumWeight+=filter.relativeKernelWeight(i, j, x, y);
					}
				}
				double sum = 0;
				for(int ii = -1;ii<=1;++ii)
				{
					for(int jj=-1;jj<=1;++jj)
					{
						int x = i+ii,y = j+jj;
						if((x<=4&&x>=0)&&(y<=4&&y>=0))
						{
							sum+=filter.relativeKernelWeight(i, j, x, y)*array2[x][y]/sumWeight;
						}
					}
				}
				array4[i][j] = sum;
			}
		}
		
		for(int i = 0;i<5;++i)
		{
			for(int j = 0;j<5;++j)
			{
				double sumWeight = 0;
				for(int ii = -1;ii<=1;++ii)
				{
					for(int jj=-1;jj<=1;++jj)
					{
						int x = i+ii,y = j+jj;
						if((x<=4&&x>=0)&&(y<=4&&y>=0))
							sumWeight+=filter.relativeKernelWeight(i, j, x, y);
					}
				}
				double sum = 0;
				for(int ii = -1;ii<=1;++ii)
				{
					for(int jj=-1;jj<=1;++jj)
					{
						int x = i+ii,y = j+jj;
						if((x<=4&&x>=0)&&(y<=4&&y>=0))
						{
							sum+=filter.relativeKernelWeight(i, j, x, y)*array0[x][y]/sumWeight;
						}
					}
				}
				array5[i][j] = sum;
			}
		}
		
		double sum1 = 0,sum2 = 0,sum3=0;;
		/*for(int i = 0;i<5;++i)
		{
			for(int j = 0;j<5;++j)
			{
				System.out.print(array3[i][j]+" ");
				sum1+=array3[i][j];
			}
			System.out.println();
		}
		System.out.println();
		for(int i = 0;i<5;++i)
		{
			for(int j = 0;j<5;++j)
			{
				System.out.print(array4[i][j]+" ");
				sum2+=array4[i][j];
			}
			System.out.println();
		}
		System.out.println();
		for(int i = 0;i<5;++i)
		{
			for(int j = 0;j<5;++j)
			{
				System.out.print(array5[i][j]+" ");
				sum3+=array5[i][j];
			}
			System.out.println();
		}*/
		
		for(int i = 0;i<5;++i)
		{
			for(int j = 0;j<5;++j)
			{
				System.out.print(((array3[i][j]+array4[i][j])-array5[i][j])+" ");
				sum2+=array4[i][j];
			}
			System.out.println();
		}
		
		System.out.println("sum1 is "+sum1+",sum2 is "+sum2+",sum3 is "+sum3);
	}
}
