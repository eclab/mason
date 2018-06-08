package optimization.util ;

/**
 * This class is a generic filter implementation,
 * this class could be extended to implement other
 * kernel models like Gaussian, Laplacian etc. -- khaled
 */

import environment.Map ;

public class Filter
{
    public static boolean isDebug = false ;
    /**
     * Dimension of the kernel, 
     * and it is always an odd interger.
     */
    protected int rows = 0 ; // height
    protected int cols = 0 ; // width

    /** This is the kernel */
    protected double[][] kernel = null ;
    protected double[] kernel_ = null ;

    public Filter (int rows, int cols)
    {
        this.rows = rows ;
        this.cols = cols ;
        this.kernel = new double[this.rows][this.cols] ;
        this.kernel_ = new double[this.rows * this.cols];
    }

    // Getters
    public int getWidth() { return this.cols ; }
    public int getHeight() { return this.rows ; }
    public double getValue(int i, int j) { return this.kernel[i][j] ;} 

    /**
     * Builds up a kernel
     */
    protected void buildKernel(){;}

    /**
     * Applies the kernel to the input data double[][] matrix, 
     * and returns the result, the post re-normalization is necessary
     * since after the convolution the data values are increased.
     */
    public double[][] applyFilter(double[][] terrain)
    {
        // get the original value range of the terrain
        double[] oldRange = this.getValueRange(terrain);
        // do the convolution
        double[][] result = this.applyConvolution(terrain);
        // renormalize the terrain
        result = this.normalize(result, oldRange);
        return result ;
    }
    
    /**
     * This is like above, but it does the filtering for N times; 
     * this function will be faster if you need to apply filter for
     * n consecutive times.
     */
    public double[][] applyFilter(double[][] terrain, int pass)
    {
        // get the original value range of the terrain
        double[] oldRange = this.getValueRange(terrain);
        // do the convolution
        if(isDebug) System.err.println("Filter.applyFilter() : " + 0 + ".");
        double[][] result = this.applyConvolution(terrain);
        for(int i = 1 ; i < pass ; i++)
        {
            if(isDebug) System.err.println("Filter.applyFilter() : " + i + ".");
            result = this.applyConvolution(result);
        }
        // renormalize the terrain
        result = this.normalize(result, oldRange);
        return result ;
    }
            
    /**
     * A simple 2D convolution, but it does not
     * consider any constraint.
     */
    protected double[][] applyConvolution(double[][] terrain)
    {
        // main convolution procedure
        int kCenterX = this.cols / 2;
        int kCenterY = this.rows / 2;
        int w = terrain.length;
        int h = terrain[0].length;
        double[][] result = new double[w][h];
        for (int i = 0 ; i < terrain.length; i++) {
            System.arraycopy(terrain[i], 0, result[i], 0, terrain[0].length);
        }
        for(int i = 0 ; i < w ; ++i)        // rows
            for(int j = 0 ; j < h ; ++j)    // columns
            {
                double sum = 0 ;        // init to 0 before sum
                for(int m = 0 ; m < this.rows ; ++m)    // kernel rows
                {
                    int mm = this.rows - 1 - m;    // row index of flipped kernel
                    for(int n = 0 ; n < this.cols ; ++n)    // kernel columns
                    {
                        int nn = this.cols - 1 - n;    // column index of flipped kernel
                        // index of input signal, used for checking boundary
                        int ii = i + (m - kCenterY);
                        int jj = j + (n - kCenterX);
                        // ignore input samples which are out of bound
                        double kVal = this.kernel[mm][nn] ;
                        if( ii >= 0 && ii < w && jj >= 0 && jj < h && kVal != 0.0)
                            result[i][j] += result[ii][jj] * kVal ;
                    }
                }
            }
        return result ;
    }

    /**
     * This function is same as before, but it only applies the filter
     * on a specific region of the map, like 'only in canada' etc.
     */
    public static double[][] applyFilterWithMapConstraint(double[][] terrain, 
                int[][] nationGrid, Filter filter, int countryCode, 
                int starti, int endi, int startj, int endj)
    {
        // get the original value range of the terrain
        double[] oldRange = getValueRangeWithMapConstraint(terrain, 
                    nationGrid, countryCode,
                    starti, endi, startj, endj);
        // do the convolution
        double[][] result = applyConvolutionWithMapConstraint(terrain, filter, 
                            nationGrid, countryCode,
                            starti, endi, startj, endj);
        // renormalize the terrain
        result = normalizeWithMapConstraint(result, nationGrid, countryCode, oldRange, 
                            starti, endi, startj, endj);
        return result ;
    }

    /**
     * Same as above, but it does the convolution for n times,
     * if you need to do the filtering for n times and want it
     * to be faster, then use it.
     */
    public static double[][] applyFilterWithMapConstraint(double[][] terrain, Filter filter,
                int[][] nationGrid, int countryCode, int pass, 
                int starti, int endi, int startj, int endj)
    {
        // get the original value range of the terrain
        double oldRange[] = getValueRangeWithMapConstraint(terrain, nationGrid, countryCode, 
                                starti, endi, startj, endj);
        // do the convolution
        if(isDebug) System.err.println("Filter.applyFilterWithMapConstraint() : " + 0 + ".");
        double[][] result = applyConvolutionWithMapConstraint(terrain, filter, 
                                nationGrid, countryCode, 
                                starti, endi, startj, endj);
        for(int i = 1 ; i < pass ; i++)
        {
            if(isDebug) System.err.println("Filter.applyFilterWithMapCosntraint() : " + i + ".");
            result = applyConvolutionWithMapConstraint(result, filter, 
                                nationGrid, countryCode, 
                                starti, endi, startj, endj);
        }
        // renormalize the terrain
        result = normalizeWithMapConstraint(result, nationGrid, countryCode, oldRange, 
                            starti, endi, startj, endj);
        return result ;
    }

    /**
     * This function is same as before, but only does the convolution
     * on a specific region of the map, like canada.
     */    
    public static double[][] applyConvolutionWithMapConstraint(double[][] terrain, Filter filter, 
                    int[][] nationGrid, int countryCode, 
                    int starti, int endi, int startj, int endj)
    {
        // main convolution procedure
        int kCenterX = filter.cols / 2;
        int kCenterY = filter.rows / 2;
        int w = terrain.length;
        int h = terrain[0].length;
        double[][] result = new double[w][h];
        for (int i = 0 ; i < terrain.length; i++) {
            System.arraycopy(terrain[i], 0, result[i], 0, terrain[0].length);
        }
        for(int i = starti ; i < endi ; ++i)        // rows
            for(int j = startj ; j < endj ; ++j)    // columns
            {
                double sum = 0 ;        // init to 0 before sum
                for(int m = 0 ; m < filter.rows ; ++m)    // kernel rows
                {
                    int mm = filter.rows - 1 - m;    // row index of flipped kernel
                    for(int n = 0 ; n < filter.cols ; ++n)    // kernel columns
                    {
                        int nn = filter.cols - 1 - n;    // column index of flipped kernel
                        // index of input signal, used for checking boundary
                        int ii = i + (m - kCenterY);
                        int jj = j + (n - kCenterX);
                        // ignore input samples which are out of bound
                        // and only inside the specified country code
                        double kVal = filter.kernel[mm][nn];
                        if( ii >= 0 && ii < w && jj >= 0 && jj < h &&
                                kVal != 0.0 &&
                                 nationGrid[i][j] == countryCode &&
                                 nationGrid[ii][jj] == countryCode &&
                                result[i][j] != Map.MISSING &&
                                result[ii][jj] != Map.MISSING)
                            result[i][j] += result[ii][jj] * kVal ;
                    }
                }
            }
        return result ;
    }

    /**
     * Same as before, but uses a faster algorithm.
     */
    public double[][] applyFastFilter(double[][] terrain)
    {
        // get the original value range of the terrain
        double[] oldRange = this.getValueRange(terrain);
        // do the convolution
        double[][] result = this.applyFastConvolution(terrain);
        // renormalize the terrain
        result = this.normalize(result, oldRange);
        return result ;
    }

    /**
     * Same as before but uses a faster algorithm.
     */
    public double[][] applyFastFilter(double[][] terrain, int pass)
    {
        // get the original value range of the terrain
        double[] oldRange = this.getValueRange(terrain);
        // do the convolution
        if(isDebug) System.err.println("Filter.applyFilter() : " + 0 + ".");
        double[][] result = this.applyFastConvolution(terrain);
        for(int i = 1 ; i < pass ; i++)
        {
            if(isDebug) System.err.println("Filter.applyFilter() : " + i + ".");
            result = this.applyFastConvolution(result);
        }
        // renormalize the terrain
        result = this.normalize(result, oldRange);
        return result ;
    }

    /**
     * This code is stolen "as-is" from 
     *     http://www.songho.ca/dsp/convolution/convolution.html#separable_convolution
     * As the gaussian kernel is symmetric and separable, this could be convolved with
     * the input data by using quarter size of the kernel, so kernel traversal is reduced
     * by 3, thus this code is 3 times faster than the naive approach.
     */
    protected double[][] applyFastConvolution(double[][] in_)
    {
        double[] in = this.vectorize(in_);
        double[] out = new double[in_.length * in_[0].length];

        int dataSizeX = in_[0].length ;
        int dataSizeY = in_.length ;

        // find center position of kernel (half of kernel size)
        int kernelSizeX = this.cols ;
        int kernelSizeY = this.rows ;
        int kCenterX = kernelSizeX >> 1;
        int kCenterY = kernelSizeY >> 1;
        int kSize = kernelSizeX * kernelSizeY;              // total kernel size

        // allocate memeory for multi-cursor
        int[] inPtr = new int[kSize] ;

        // set initial position of multi-cursor, NOTE: it is swapped instead of kernel
        int ptr = 0 + (dataSizeX * kCenterY + kCenterX); // the first cursor is shifted (kCenterX, kCenterY)
        for(int m = 0, t = 0 ; m < kernelSizeY ; ++m)
        {
            for(int n = 0 ; n < kernelSizeX ; ++n, ++t)
                inPtr[t] = ptr - n;
            ptr -= dataSizeX;
        }

        int rowEnd = dataSizeY - kCenterY;                  // bottom row partition divider
        int colEnd = dataSizeX - kCenterX;                  // right column partition divider

        // convolve rows from index=0 to index=kCenterY-1
        double sum = 0.0 ;
        int t = 0 ;
        int x = 0 ;
        int y = kCenterY;
        int oIndex = 0 ;
        for(int i = 0 ; i < kCenterY ; ++i)
        {
            // partition #1 ***********************************
            x = kCenterX;
            for(int j = 0 ; j < kCenterX ; ++j)                 // column from index=0 to index=kCenterX-1
            {
                sum = 0 ;
                t = 0 ;
                for(int m = 0 ; m <= y ; ++m)
                {
                    for(int n=0 ; n <= x ; ++n)
                    {
                        if(this.kernel_[t] != 0.0)
                            sum += in[inPtr[t]] * this.kernel_[t];
                        ++t;
                    }
                    t += (kernelSizeX - x - 1);         // jump to next row
                }

                // store output
                out[oIndex] = Math.abs(sum) + 0.5 ;
                ++oIndex ;
                ++x;
                for(int k = 0 ; k < kSize ; ++k) ++inPtr[k];    // move all cursors to next
            }

            // partition #2 ***********************************
            for(int j = kCenterX ; j < colEnd ; ++j)            // column from index=kCenterX to index=(dataSizeX-kCenterX-1)
            {
                sum = 0.0 ;
                t = 0 ;
                for(int m = 0 ; m <= y ; ++m)
                {
                    for(int n = 0 ; n < kernelSizeX ; ++n)
                    {
                        if(this.kernel_[t] != 0.0)
                            sum += in[inPtr[t]] * this.kernel_[t];
                        ++t;
                    }
                }

                // store output
                out[oIndex] = Math.abs(sum) + 0.5 ;
                ++oIndex;
                ++x;
                for(int k = 0 ; k < kSize ; ++k) ++inPtr[k];    // move all cursors to next
            }

            // partition #3 ***********************************
            x = 1;
            for(int j = colEnd ; j < dataSizeX ; ++j)           // column from index=(dataSizeX-kCenter) to index=(dataSizeX-1)
            {
                sum = 0;
                t = x;
                for(int m = 0 ; m <= y ; ++m)
                {
                    for(int n = x ; n < kernelSizeX ; ++n)
                    {
                        if(this.kernel_[t] != 0.0)
                            sum += in[inPtr[t]] * this.kernel_[t];
                        ++t;
                    }
                    t += x;                             // jump to next row
                }

                // store output
                out[oIndex] = Math.abs(sum) + 0.5 ;
                ++oIndex ;
                ++x;
                for(int k = 0 ; k < kSize ; ++k) ++inPtr[k];    // move all cursors to next
            }
            ++y;                                        // add one more row to convolve for next run
        }

        // convolve rows from index=kCenterY to index=(dataSizeY-kCenterY-1)
        for(int i = kCenterY ; i < rowEnd ; ++i)               // number of rows
        {
            // partition #4 ***********************************
            x = kCenterX;
            for(int j = 0 ; j < kCenterX ; ++j)                 // column from index=0 to index=kCenterX-1
            {
                sum = 0;
                t = 0;
                for(int m = 0 ; m < kernelSizeY ; ++m)
                {
                    for(int n = 0 ; n <= x ; ++n)
                    {
                        if(this.kernel_[t] != 0.0)
                            sum += in[inPtr[t]] * this.kernel_[t];
                        ++t;
                    }
                    t += (kernelSizeX - x - 1);
                }

                // store output
                out[oIndex] = Math.abs(sum) + 0.5 ;
                ++oIndex;
                ++x;
                for(int k = 0 ; k < kSize ; ++k) ++inPtr[k];    // move all cursors to next
            }

            // partition #5 ***********************************
            for(int j = kCenterX ; j < colEnd ; ++j)          // column from index=kCenterX to index=(dataSizeX-kCenterX-1)
            {
                sum = 0;
                t = 0;
                for(int m = 0 ; m < kernelSizeY ; ++m)
                {
                    for(int n = 0 ; n < kernelSizeX ; ++n)
                    {
                        if(this.kernel_[t] != 0.0)
                            sum += in[inPtr[t]] * this.kernel_[t];
                        ++inPtr[t]; // in this partition, all cursors are used to convolve. moving cursors to next is safe here
                        ++t;
                    }
                }

                // store output
                out[oIndex] = Math.abs(sum) + 0.5;
                ++oIndex ;
                ++x;
            }

            // partition #6 ***********************************
            x = 1;
            for(int j = colEnd ; j < dataSizeX ; ++j)           // column from index=(dataSizeX-kCenter) to index=(dataSizeX-1)
            {
                sum = 0;
                t = x;
                for(int m = 0 ; m < kernelSizeY ; ++m)
                {
                    for(int n = x ; n < kernelSizeX ; ++n)
                    {
                        if(this.kernel_[t] != 0.0)
                            sum += in[inPtr[t]] * this.kernel_[t];
                        ++t;
                    }
                    t += x;
                }

                // store output
                out[oIndex] = Math.abs(sum) + 0.5;
                ++oIndex ;
                ++x;
                for(int k = 0 ; k < kSize ; ++k) ++inPtr[k];    // move all cursors to next
            }
        }

        // convolve rows from index=(dataSizeY-kCenterY) to index=(dataSizeY-1)
        y = 1;
        for(int i = rowEnd ; i < dataSizeY ; ++i)               // number of rows
        {
            // partition #7 ***********************************
            x = kCenterX;
            for(int j = 0 ; j < kCenterX ; ++j)                 // column from index=0 to index=kCenterX-1
            {
                sum = 0;
                t = kernelSizeX * y;
                for(int m = y ; m < kernelSizeY ; ++m)
                {
                    for(int n = 0 ; n <= x ; ++n)
                    {
                        if(this.kernel_[t] != 0.0)
                            sum += in[inPtr[t]] * this.kernel_[t];
                        ++t;
                    }
                    t += (kernelSizeX - x - 1);
                }

                // store output
                out[oIndex] = Math.abs(sum) + 0.5;
                ++oIndex;
                ++x;
                for(int k = 0 ; k < kSize ; ++k) ++inPtr[k];    // move all cursors to next
            }

            // partition #8 ***********************************
            for(int j = kCenterX ; j < colEnd ; ++j)            // column from index=kCenterX to index=(dataSizeX-kCenterX-1)
            {
                sum = 0;
                t = kernelSizeX * y;
                for(int m = y ; m < kernelSizeY ; ++m)
                {
                    for(int n = 0 ; n < kernelSizeX ; ++n)
                    {
                        if(this.kernel_[t] != 0.0)
                            sum += in[inPtr[t]] * this.kernel_[t];
                        ++t;
                    }
                }

                // store output
                out[oIndex] = Math.abs(sum) + 0.5;
                ++oIndex ;
                ++x;
                for(int k = 0 ; k < kSize ; ++k) ++inPtr[k];
            }

            // partition #9 ***********************************
            x = 1;
            for(int j = colEnd ; j < dataSizeX ; ++j)           // column from index=(dataSizeX-kCenter) to index=(dataSizeX-1)
            {
                sum = 0;
                t = kernelSizeX * y + x;
                for(int m = y ; m < kernelSizeY; ++m)
                {
                    for(int n = x ; n < kernelSizeX ; ++n)
                    {
                        if(this.kernel_[t] != 0.0)
                            sum += in[inPtr[t]] * this.kernel_[t];
                        ++t;
                    }
                    t += x;
                }

                // store output
                out[oIndex] = Math.abs(sum) + 0.5;
                ++oIndex ;
                ++x;
                for(int k = 0 ; k < kSize ; ++k) ++inPtr[k];    // move all cursors to next
            }
            ++y;                                        // the starting row index is increased
        }
        return this.matricize(out, dataSizeY, dataSizeX);
    }

    /**
     * Same as before, but uses the faster version 
     * of the convolution operator.
     */
    public static double[][] applyFastFilterWithMapConstraint(double[][] terrain, Filter filter,
                int[][] nationGrid, int countryCode, int pass, 
                int starti, int endi, int startj, int endj)
    {
        // get the original value range of the terrain
        double oldRange[] = getValueRangeWithMapConstraint(terrain, nationGrid, countryCode, 
                                starti, endi, startj, endj);
        // do the convolution
        if(isDebug) System.err.println("Filter.applyFastFilterWithMapConstraint() : " + 0 + ".");
        double[][] result = applyFastConvolutionWithMapConstraint(terrain, filter, nationGrid, 
                        countryCode, starti, endi, startj, endj);
        
        /*
        double sumSmoothed = 0;
        int w = result.length,l = result[0].length;
		for(int i = 0;i<w;++i)
		{
			for(int j = 0;j<l;++j)
			{
				if(result[i][j]!=Map.MISSING&&nationGrid[i][j]==Map.CANADA_CODE)
					sumSmoothed += result[i][j];
			}
		}
		System.out.println("first pass: "+ sumSmoothed);
        */
        for(int i = 1 ; i < pass ; i++)
        {
            if(isDebug) System.err.println("Filter.applyFastFilterWithMapCosntraint() : " + i + ".");
            result = applyFastConvolutionWithMapConstraint(result, filter, 
                                nationGrid, countryCode, 
                                starti, endi, startj, endj);
            /*
            sumSmoothed = 0;
            w = result.length;
            l = result[0].length;
    		for(int j = 0;j<w;++j)
    		{
    			for(int k = 0;k<l;++k)
    			{
    				if(result[j][k]!=Map.MISSING&&nationGrid[j][k]==Map.CANADA_CODE)
    					sumSmoothed += result[j][k];
    			}
    		}
    		System.out.println((i+1) + " pass: "+ sumSmoothed);
            */
            
            
        }
        // renormalize the terrain
        
        result = normalizeWithMapConstraint(result, nationGrid, 
                            countryCode, oldRange, 
                            starti, endi, startj, endj);
        
        /*
        sumSmoothed = 0;
        w = result.length;
        l = result[0].length;
		for(int i = 0;i<w;++i)
		{
			for(int j = 0;j<l;++j)
			{
				if(result[i][j]!=Map.MISSING&&nationGrid[i][j]==Map.CANADA_CODE)
					sumSmoothed += result[i][j];
			}
		}
		System.out.println("after normalization pass: "+ sumSmoothed);
        */
        
        
        
        return result ;
    }
    
    /**
     * Same as above but does the convolution on a selected area of the input
     * data -- like only on canada cell, and within a bounding box etc.
     */
    public static double[][] applyFastConvolutionWithMapConstraint(double[][] in_, Filter filter, 
                    int[][] nationGrid, int countryCode, 
                    int starti, int endi, int startj, int endj)
    {
        // do some acrobatics to maintain the map bounding box
        // we will do the convolution only on this region of the map.
        // the below simply cuts out the bounding-box portion from 
        // the input map and the nationGrid, this is tricky !!
        int colChunkWidth = endj - startj ;
        int rowChunkHeight = endi - starti ;
        
        // -- bound the input map and convert it to vector
        double[] in = new double[rowChunkHeight * colChunkWidth];
        for(int row = starti, col = startj, k = 0 ; row < endi ; row++, k += colChunkWidth)
            System.arraycopy(in_[row], col, in, k, colChunkWidth);
        // -- output map has the same dimension as the input map
        double[] out = new double[in.length];
        // -- bound the nationGrid[][] and convert it to vector
        int[] nationCode = new int[rowChunkHeight * colChunkWidth];
        for(int row = starti, col = startj, k = 0 ; row < endi ; row++, k += colChunkWidth)
            System.arraycopy(nationGrid[row], col, nationCode, k, colChunkWidth);

        int dataSizeX = colChunkWidth ;
        int dataSizeY = rowChunkHeight ;

        // find center position of kernel (half of kernel size)
        int kernelSizeX = filter.cols ;
        int kernelSizeY = filter.rows ;
        int kCenterX = kernelSizeX >> 1;
        int kCenterY = kernelSizeY >> 1;
        int kSize = kernelSizeX * kernelSizeY;              // total kernel size

        // allocate memeory for multi-cursor
        int[] inPtr = new int[kSize] ;

        // set initial position of multi-cursor, NOTE: it is swapped instead of kernel
        int ptr = 0 + (dataSizeX * kCenterY + kCenterX); // the first cursor is shifted (kCenterX, kCenterY)
        for(int m = 0, t = 0 ; m < kernelSizeY ; ++m)
        {
            for(int n = 0 ; n < kernelSizeX ; ++n, ++t)
                inPtr[t] = ptr - n;
            ptr -= dataSizeX;
        }

        int rowEnd = dataSizeY - kCenterY;                  // bottom row partition divider
        int colEnd = dataSizeX - kCenterX;                  // right column partition divider

        // convolve rows from index=0 to index=kCenterY-1
        double sum = 0.0 ;
        int t = 0 ;
        int x = 0 ;
        int y = kCenterY;
        int oIndex = 0 ;
        for(int i = 0 ; i < kCenterY ; ++i)
        {
            // partition #1 ***********************************
            x = kCenterX;
            for(int j = 0 ; j < kCenterX ; ++j)                 // column from index=0 to index=kCenterX-1
            {
                sum = 0 ;
                t = 0 ;
                for(int m = 0 ; m <= y ; ++m)
                {
                    for(int n=0 ; n <= x ; ++n)
                    {
                        double inVal = in[inPtr[t]] ;
                        double kVal = filter.kernel_[t] ;
                        int nVal = nationCode[inPtr[t]] ;
                        if( nVal == countryCode && kVal != 0.0 && inVal != Map.MISSING)
                            sum += in[inPtr[t]] * filter.kernel_[t];
                        ++t;
                    }
                    t += (kernelSizeX - x - 1);         // jump to next row
                }

                // store output
                if(nationCode[oIndex] == countryCode && in[oIndex] != Map.MISSING)
                    out[oIndex] = Math.abs(sum) + 0.5 ;
                else
                    out[oIndex] = in[oIndex] ;
                ++oIndex ;
                ++x;
                for(int k = 0 ; k < kSize ; ++k) ++inPtr[k];    // move all cursors to next
            }

            // partition #2 ***********************************
            for(int j = kCenterX ; j < colEnd ; ++j)            // column from index=kCenterX to index=(dataSizeX-kCenterX-1)
            {
                sum = 0.0 ;
                t = 0 ;
                for(int m = 0 ; m <= y ; ++m)
                {
                    for(int n = 0 ; n < kernelSizeX ; ++n)
                    {
                        double inVal = in[inPtr[t]] ;
                        double kVal = filter.kernel_[t] ;
                        int nVal = nationCode[inPtr[t]] ;
                        if( nVal == countryCode && kVal != 0.0 && inVal != Map.MISSING)
                            sum += in[inPtr[t]] * filter.kernel_[t];
                        ++t;
                    }
                }

                // store output
                if(nationCode[oIndex] == countryCode && in[oIndex] != Map.MISSING)
                    out[oIndex] = Math.abs(sum) + 0.5 ;
                else
                    out[oIndex] = in[oIndex] ;
                ++oIndex;
                ++x;
                for(int k = 0 ; k < kSize ; ++k) ++inPtr[k];    // move all cursors to next
            }

            // partition #3 ***********************************
            x = 1;
            for(int j = colEnd ; j < dataSizeX ; ++j)           // column from index=(dataSizeX-kCenter) to index=(dataSizeX-1)
            {
                sum = 0;
                t = x;
                for(int m = 0 ; m <= y ; ++m)
                {
                    for(int n = x ; n < kernelSizeX ; ++n)
                    {
                        double inVal = in[inPtr[t]] ;
                        double kVal = filter.kernel_[t] ;
                        int nVal = nationCode[inPtr[t]] ;
                        if( nVal == countryCode && kVal != 0.0 && inVal != Map.MISSING)
                            sum += in[inPtr[t]] * filter.kernel_[t];
                        ++t;
                    }
                    t += x;                             // jump to next row
                }

                // store output
                if(nationCode[oIndex] == countryCode && in[oIndex] != Map.MISSING)
                    out[oIndex] = Math.abs(sum) + 0.5 ;
                else
                    out[oIndex] = in[oIndex] ;
                ++oIndex ;
                ++x;
                for(int k = 0 ; k < kSize ; ++k) ++inPtr[k];    // move all cursors to next
            }
            ++y;                                        // add one more row to convolve for next run
        }

        // convolve rows from index=kCenterY to index=(dataSizeY-kCenterY-1)
        for(int i = kCenterY ; i < rowEnd ; ++i)               // number of rows
        {
            // partition #4 ***********************************
            x = kCenterX;
            for(int j = 0 ; j < kCenterX ; ++j)                 // column from index=0 to index=kCenterX-1
            {
                sum = 0;
                t = 0;
                for(int m = 0 ; m < kernelSizeY ; ++m)
                {
                    for(int n = 0 ; n <= x ; ++n)
                    {
                        double inVal = in[inPtr[t]] ;
                        double kVal = filter.kernel_[t] ;
                        int nVal = nationCode[inPtr[t]] ;
                        if( nVal == countryCode && kVal != 0.0 && inVal != Map.MISSING)
                            sum += in[inPtr[t]] * filter.kernel_[t];
                        ++t;
                    }
                    t += (kernelSizeX - x - 1);
                }

                // store output
                if(nationCode[oIndex] == countryCode && in[oIndex] != Map.MISSING)
                    out[oIndex] = Math.abs(sum) + 0.5 ;
                else
                    out[oIndex] = in[oIndex] ;
                ++oIndex;
                ++x;
                for(int k = 0 ; k < kSize ; ++k) ++inPtr[k];    // move all cursors to next
            }

            // partition #5 ***********************************
            for(int j = kCenterX ; j < colEnd ; ++j)          // column from index=kCenterX to index=(dataSizeX-kCenterX-1)
            {
                sum = 0;
                t = 0;
                for(int m = 0 ; m < kernelSizeY ; ++m)
                {
                    for(int n = 0 ; n < kernelSizeX ; ++n)
                    {
                        double inVal = in[inPtr[t]] ;
                        double kVal = filter.kernel_[t] ;
                        int nVal = nationCode[inPtr[t]] ;
                        if( nVal == countryCode && kVal != 0.0 && inVal != Map.MISSING)
                            sum += in[inPtr[t]] * filter.kernel_[t];
                        ++inPtr[t]; // in this partition, all cursors are used to convolve. moving cursors to next is safe here
                        ++t;
                    }
                }

                // store output
                if(nationCode[oIndex] == countryCode && in[oIndex] != Map.MISSING)
                    out[oIndex] = Math.abs(sum) + 0.5 ;
                else
                    out[oIndex] = in[oIndex] ;
                ++oIndex ;
                ++x;
            }

            // partition #6 ***********************************
            x = 1;
            for(int j = colEnd ; j < dataSizeX ; ++j)           // column from index=(dataSizeX-kCenter) to index=(dataSizeX-1)
            {
                sum = 0;
                t = x;
                for(int m = 0 ; m < kernelSizeY ; ++m)
                {
                    for(int n = x ; n < kernelSizeX ; ++n)
                    {
                        double inVal = in[inPtr[t]] ;
                        double kVal = filter.kernel_[t] ;
                        int nVal = nationCode[inPtr[t]] ;
                        if( nVal == countryCode && kVal != 0.0 && inVal != Map.MISSING)
                            sum += in[inPtr[t]] * filter.kernel_[t];
                        ++t;
                    }
                    t += x;
                }

                // store output
                if(nationCode[oIndex] == countryCode && in[oIndex] != Map.MISSING)
                    out[oIndex] = Math.abs(sum) + 0.5 ;
                else
                    out[oIndex] = in[oIndex] ;
                ++oIndex ;
                ++x;
                for(int k = 0 ; k < kSize ; ++k) ++inPtr[k];    // move all cursors to next
            }
        }

        // convolve rows from index=(dataSizeY-kCenterY) to index=(dataSizeY-1)
        y = 1;
        for(int i = rowEnd ; i < dataSizeY ; ++i)               // number of rows
        {
            // partition #7 ***********************************
            x = kCenterX;
            for(int j = 0 ; j < kCenterX ; ++j)                 // column from index=0 to index=kCenterX-1
            {
                sum = 0;
                t = kernelSizeX * y;
                for(int m = y ; m < kernelSizeY ; ++m)
                {
                    for(int n = 0 ; n <= x ; ++n)
                    {
                        double inVal = in[inPtr[t]] ;
                        double kVal = filter.kernel_[t] ;
                        int nVal = nationCode[inPtr[t]] ;
                        if( nVal == countryCode && kVal != 0.0 && inVal != Map.MISSING)
                            sum += in[inPtr[t]] * filter.kernel_[t];
                        ++t;
                    }
                    t += (kernelSizeX - x - 1);
                }

                // store output
                if(nationCode[oIndex] == countryCode && in[oIndex] != Map.MISSING)
                    out[oIndex] = Math.abs(sum) + 0.5 ;
                else
                    out[oIndex] = in[oIndex] ;
                ++oIndex;
                ++x;
                for(int k = 0 ; k < kSize ; ++k) ++inPtr[k];    // move all cursors to next
            }

            // partition #8 ***********************************
            for(int j = kCenterX ; j < colEnd ; ++j)            // column from index=kCenterX to index=(dataSizeX-kCenterX-1)
            {
                sum = 0;
                t = kernelSizeX * y;
                for(int m = y ; m < kernelSizeY ; ++m)
                {
                    for(int n = 0 ; n < kernelSizeX ; ++n)
                    {
                        double inVal = in[inPtr[t]] ;
                        double kVal = filter.kernel_[t] ;
                        int nVal = nationCode[inPtr[t]] ;
                        if( nVal == countryCode && kVal != 0.0 && inVal != Map.MISSING)
                            sum += in[inPtr[t]] * filter.kernel_[t];
                        ++t;
                    }
                }

                // store output
                if(nationCode[oIndex] == countryCode && in[oIndex] != Map.MISSING)
                    out[oIndex] = Math.abs(sum) + 0.5 ;
                else
                    out[oIndex] = in[oIndex] ;
                ++oIndex ;
                ++x;
                for(int k = 0 ; k < kSize ; ++k) ++inPtr[k];
            }

            // partition #9 ***********************************
            x = 1;
            for(int j = colEnd ; j < dataSizeX ; ++j)           // column from index=(dataSizeX-kCenter) to index=(dataSizeX-1)
            {
                sum = 0;
                t = kernelSizeX * y + x;
                for(int m = y ; m < kernelSizeY; ++m)
                {
                    for(int n = x ; n < kernelSizeX ; ++n)
                    {
                        double inVal = in[inPtr[t]] ;
                        double kVal = filter.kernel_[t] ;
                        int nVal = nationCode[inPtr[t]] ;
                        if( nVal == countryCode && kVal != 0.0 && inVal != Map.MISSING)
                            sum += in[inPtr[t]] * filter.kernel_[t];
                        ++t;
                    }
                    t += x;
                }

                // store output
                if(nationCode[oIndex] == countryCode && in[oIndex] != Map.MISSING)
                    out[oIndex] = Math.abs(sum) + 0.5 ;
                else
                    out[oIndex] = in[oIndex] ;
                ++oIndex ;
                ++x;
                for(int k = 0 ; k < kSize ; ++k) ++inPtr[k];    // move all cursors to next
            }
            ++y;                                        // the starting row index is increased
        }
        // now this is the tricky part --
        // -- make an array result[][] that is same dimension as original map
        // -- and copy all the contents of in_[][] to result[][].
        double[][] result = new double[in_.length][in_[0].length];
        for (int i = 0 ; i < in_.length; i++) {
            System.arraycopy(in_[i], 0, result[i], 0, in_[0].length);
        }
        // -- now stich the out[][] map into the result[][].
        for(int k = 0, row = starti, col = startj ; row < endi ; row++, k += colChunkWidth)
            System.arraycopy(out, k, result[row], col, colChunkWidth);
        // and return
        return result;
    }
    
    /**
     * Get the max and min value of the terrain,
     * but this does not consider any constraints.
     */
    protected double[] getValueRange(double[][] terrain)
    {
        double max = terrain[0][0] ;
        double min = terrain[0][0] ;
        int w = terrain.length ;
        int h = terrain[0].length;
        for(int i = 0 ; i < w ; i++)
            for(int j = 0 ; j < h ; j++)
            {
                if(terrain[i][j] >= max) max = terrain[i][j];
                if(terrain[i][j] <= min) min = terrain[i][j] ;
            }
        double[] range = new double[2];
               range[0] = min ;
        range[1] = max ;
        return range ;
    }

    /**
     * This is same like the function above, but only calculates
     * the range on the specified country, like canada.
     */
    public static double[] getValueRangeWithMapConstraint(double[][] terrain, 
                int[][] nationGrid, int countryCode,
                int starti, int endi, int startj, int endj)
    {
        double maxVal = -1.0;
        double minVal = -1.0 ;
        boolean isFirstHit = true ;
        int w = terrain.length;
        int h = terrain[0].length;
        for(int i = starti ; i < endi ; ++i)        // rows
            for(int j = startj ; j < endj ; ++j)    // columns
            {
                if(nationGrid[i][j] == countryCode && terrain[i][j] != Map.MISSING)
                {
                    if(isFirstHit)
                    {
                        minVal = maxVal = terrain[i][j] ;
                        isFirstHit = false ;            
                    }
                    else
                    {
                        if(terrain[i][j] <= minVal) minVal = terrain[i][j] ;
                        if(terrain[i][j] >= maxVal) maxVal = terrain[i][j] ;
                    }
                }
            }
        double[] range = new double[2];
               range[0] = minVal ;
        range[1] = maxVal ;
        return range ;
    }

    /**
     * Maps [min, max] to [a,b]
     */
    private static double normFunc(double x, double min, double max, double a, double b)
    {
        return (b - a) * (x - min)/(max - min) ;
    }

    /**
     * Renormalize the values after smoothing, 
     * but this does not consider any constraint.
     */
    protected double[][] normalize(double[][] terrain, double[] refRange)
    {
        double[] ownRange = this.getValueRange(terrain);
        int w = terrain.length;
        int h = terrain[0].length;
        double[][] result = new double[w][h] ;
        for (int i = 0 ; i < terrain.length; i++) {
            System.arraycopy(terrain[i], 0, result[i], 0, terrain[0].length);
        }
        for(int i = 0 ; i < w ; i++)
            for(int j = 0 ; j < h ; j++)
                result[i][j] = normFunc(result[i][j], ownRange[0], ownRange[1], 
                        refRange[0], refRange[1]);
        return result ;
    }

    /**
     * This function is same as above, but only normalizes a specific
     * region of the map, like canada.
     */
    public static double[][] normalizeWithMapConstraint(double[][] terrain, 
            int[][] nationGrid, int countryCode, 
            double[] refRange, 
            int starti, int endi, int startj, int endj)
    {
        double[] ownRange = getValueRangeWithMapConstraint(terrain, nationGrid, 
                                    countryCode, 
                                    starti, endi, startj, endj);
        int w = terrain.length;
        int h = terrain[0].length;
        double[][] result = new double[w][h];
        for (int i = 0 ; i < terrain.length; i++) {
            System.arraycopy(terrain[i], 0, result[i], 0, terrain[0].length);
        }
        for(int i = starti ; i < endi ; ++i)        // rows
            for(int j = startj ; j < endj ; ++j)    // columns
                if(nationGrid[i][j] == countryCode && terrain[i][j] != Map.MISSING)
                    result[i][j] = normFunc(result[i][j], ownRange[0], ownRange[1],
                            refRange[0], refRange[1]);
        return result ;
    }

    /** Same as above but it takes an interval instead of bound */
    public static double[][] normalizeWithMapConstraint(double[][] terrain, 
            int[][] nationGrid, int countryCode, 
            double low, double high, 
            int starti, int endi, int startj, int endj)
    {
        double refRange = high - low ;
        double[] ownRange = getValueRangeWithMapConstraint(terrain, nationGrid, 
                            countryCode,
                            starti, endi, startj, endj);
        int w = terrain.length;
        int h = terrain[0].length;
        double[][] result = new double[w][h];
        for (int i = 0 ; i < terrain.length; i++) {
            System.arraycopy(terrain[i], 0, result[i], 0, terrain[0].length);
        }
        for(int i = starti ; i < endi ; ++i)        // rows
            for(int j = startj ; j < endj ; ++j)    // columns
                if(nationGrid[i][j] == countryCode && terrain[i][j] != Map.MISSING)
                    result[i][j] = normFunc(result[i][j], ownRange[0], ownRange[1],
                               low, high);    
        return result ;
    }

    /** Similar to normalize, but calculates zscore */
    public static double[][] zscoreWithMapConstraint(double[][] terrain, 
            int[][] nationGrid, int countryCode, 
            int starti, int endi, int startj, int endj)
    {
        // Calculate mean and standard deviation
        int n = 0;
        double sum = 0.0, sumOfSquares = 0.0;
        for(int i = starti ; i < endi ; ++i)        // rows
            for(int j = startj ; j < endj ; ++j)    // columns
                if(nationGrid[i][j] == countryCode && terrain[i][j] != Map.MISSING)
                {
                    sum += terrain[i][j];
                    sumOfSquares += terrain[i][j] * terrain[i][j];
                    n += 1;
                }
        double mean = sum / n;
        double sd = Math.sqrt(sumOfSquares/n - mean * mean);

        int w = terrain.length;
        int h = terrain[0].length;
        double[][] result = new double[w][h];
        for (int i = 0 ; i < terrain.length; i++) {
            System.arraycopy(terrain[i], 0, result[i], 0, terrain[0].length);
        }
        for(int i = starti ; i < endi ; ++i)        // rows
            for(int j = startj ; j < endj ; ++j)    // columns
                if(nationGrid[i][j] == countryCode && terrain[i][j] != Map.MISSING)
                    result[i][j] = (terrain[i][j] - mean) / sd;
        return result ;
    }

    /**
     * Converts a 2D array to 1D vector in row-major order.
     */
    protected double[] vectorize(double[][] matrix)
    {
        double[] vector = new double[matrix.length * matrix[0].length];
        for(int i = 0, j = 0 ; i < matrix.length ; i++, j += matrix[0].length)
            System.arraycopy(matrix[i], 0, vector, j, matrix[0].length);
        return vector ;
    }

    /** 
     * Opposite of the above.
     */
    protected double[][] matricize(double[] vector, int nrow, int ncol)
    {
        double[][] matrix = new double[nrow][ncol] ;
        for(int i = 0, j = 0 ; i < matrix.length ; i++, j += matrix[0].length)
            System.arraycopy(vector, j, matrix[i], 0, matrix[0].length);
        return matrix ;    
    }

    /**
     * Stringize the kernel
     */
    public String toString()
    {
        String str = "" ;
        for(int i = 0 ; i < this.rows ; i++)
        {
            for(int j = 0 ; j < this.cols ; j++)
                str += String.format("%.3f ",this.kernel[i][j]);
            str += "\n" ;
        }
        str += ":: [";

        for(int i = 0 ; i < this.kernel_.length ; i++)
            if( i == this.kernel_.length - 1)
                str += String.format("%.3f",this.kernel_[i]) ;
            else
                str += String.format("%.3f, ",this.kernel_[i]);
        str += "]\n";
        return str ;
    }
}
