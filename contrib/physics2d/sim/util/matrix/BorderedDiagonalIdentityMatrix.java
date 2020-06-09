
package sim.util.matrix;

import sim.util.matrix.Block;
import sim.util.Bag;

public class BorderedDiagonalIdentityMatrix extends Matrix
    {
    public double[] vals;
    public int borderRows;
    private Bag blocks;
        
    public Bag getBlocks()
        {
        return blocks;
        }
        
    Bag intersectionBlocks;
    Bag lowerBlocks;
    Bag upperBlocks;
        
    Bag[] upperBlockRows;
    Bag[] lowerBlockCols;
    Bag[] lowerBlockRows;
    Bag[] intersectionBlockRows;
        
    public BorderedDiagonalIdentityMatrix(int m, int borderRows)
        {
        this.m = m;
        this.n = m;
        this.vals = new double[m];
        this.borderRows = borderRows;
        this.blocks = new Bag();
                
        lowerBlocks = new Bag();
        upperBlocks = new Bag();
        intersectionBlocks = new Bag();
                
        upperBlockRows = new Bag[this.m];
        lowerBlockCols = new Bag[this.n];
        lowerBlockRows = new Bag[this.m];
        intersectionBlockRows = new Bag[this.m];
        }
        
    public void setBlock(int rowoffset, int coloffset, double[][] vals)
        {
        if (rowoffset < m - borderRows && coloffset < m - borderRows)
            throw new RuntimeException("Can't put a block in the diagonal part of the matrix");
                
        Block block = new Block(vals.length, vals[0].length, rowoffset, coloffset, vals);
        this.blocks.add(block);
                
        if (rowoffset >= m - borderRows && coloffset >= m - borderRows)
            {
            intersectionBlocks.add(block);
            for (int i = block.rowoffset; i < block.rowoffset + block.m; i++)
                {
                if (intersectionBlockRows[i] == null)
                    intersectionBlockRows[i] = new Bag();
                intersectionBlockRows[i].add(block);
                }
            }
        else if (rowoffset < m - borderRows)
            {
            upperBlocks.add(block);
            for (int i = block.rowoffset; i < block.rowoffset + block.m; i++)
                {
                if (upperBlockRows[i] == null)
                    upperBlockRows[i] = new Bag();
                upperBlockRows[i].add(block);
                }
            }
        else
            {
            lowerBlocks.add(block);
            for (int j = block.coloffset; j < block.coloffset + block.n; j++)
                {
                if (lowerBlockCols[j] == null)
                    lowerBlockCols[j] = new Bag();
                lowerBlockCols[j].add(block);
                }
            for (int i = block.rowoffset; i < block.rowoffset + block.m; i++)
                {
                if (lowerBlockRows[i] == null)
                    lowerBlockRows[i] = new Bag();
                lowerBlockRows[i].add(block);
                }
            }
        }
        
    public Vector times(Vector other)
        {
        return times(other, new Vector(this.m));
        }
        
    public Vector times(Vector other, Vector C)
        {
        C.clear();
        if (this.m != other.m)
            throw new RuntimeException("Inner dimensions must agree");
                
        double[] othervals = other.vals;
        double[] result = C.vals;
                
        // First perform the identity part of the multiplication
        for (int i = 0; i < m - borderRows; i++)
            result[i] = othervals[i];
                
        for (int k = 0; k < blocks.numObjs; k++)
            {
            Block block = (Block)blocks.objs[k];
            int rowoffset = block.rowoffset;
            int coloffset = block.coloffset;
            double[][] vals = block.vals;
                        
            int blockm = block.m;
            int blockn = block.n;
                        
            for (int i = 0; i < blockm; i++)
                {
                int absRow = rowoffset + i;
                double curVectorVal = result[absRow];
                for (int j = 0; j < blockn; j++)
                    curVectorVal += (vals[i][j] * othervals[j + coloffset]);
                result[absRow] = curVectorVal;
                }
            }
        return C;
        }
        
    public Vector transposeTimes(Vector other)
        {
        return transposeTimes(other, new Vector(this.m));
        }
        
    public Vector transposeTimes(Vector other, Vector C)
        {
        C.clear();
        if (this.m != other.m)
            throw new RuntimeException("Inner dimensions must agree");
                
        double[] othervals = other.vals;
        double[] result = C.vals;
                
        // First perform the identity part of the multiplication
        for (int i = 0; i < m - borderRows; i++)
            result[i] = othervals[i];
                
        for (int k = 0; k < blocks.numObjs; k++)
            {
            Block block = (Block)blocks.objs[k];
                        
            // these are reversed
            int rowoffset = block.coloffset;
            int coloffset = block.rowoffset;
            int vectorOffset = block.rowoffset;
                        
            double[][] vals = block.vals;
                        
            int blockm = block.n;
            int blockn = block.m;
                        
            // loop down the rows of the transpsed matrix
            for (int i = 0; i < blockm; i++)
                {
                int absRow = rowoffset + i;
                double curVectorVal = result[absRow];
                                
                // loop over the columns
                for (int j = 0; j < blockn; j++)
                    {
                    // reverse the access to the block vals;
                    curVectorVal += vals[j][i] * othervals[j + vectorOffset];
                    }
                result[absRow] = curVectorVal;
                }
            }
        return C;
        }
        
    public DiagonalMatrix getDiagonalMatrix()
        {
        DiagonalMatrix diag = new DiagonalMatrix(this.m);
        for (int i = 0; i < m - borderRows; i++)
            diag.vals[i] = 1;
                
        for (int k = 0; k < blocks.numObjs; k++)
            {
            Block block = (Block)blocks.objs[k];
            int rowstart = 0;
            int colstart = 0;
            int absstart = -1;
                        
            if (block.rowoffset >= block.coloffset && block.rowoffset <= block.coloffset + block.n - 1)
                {
                rowstart = 0;
                colstart = block.rowoffset - block.coloffset;
                absstart = block.rowoffset;
                                
                int i = 0;
                while (i < block.m && i < block.n - colstart)
                    {
                    diag.vals[absstart + i] = block.vals[rowstart + i][colstart + i];
                    i++;
                    }
                }
            else if (block.coloffset >= block.rowoffset && block.coloffset <= block.rowoffset + block.m - 1)
                {
                rowstart = block.coloffset - block.rowoffset;
                colstart = 0;
                absstart = block.coloffset;
                                
                int i = 0;
                while (i < block.m - rowstart && i < block.n)
                    {
                    diag.vals[absstart + i] = block.vals[rowstart + i][colstart + i];
                    i++;
                    }
                }       
            }
        return diag;
        }
        
    public String toString()
        {
        double[][] vals = new double[this.m][this.m];
                
        for (int k = 0; k < blocks.numObjs; k++)
            {
            Block block = (Block)blocks.objs[k];
            for (int i = 0; i < block.m; i++)
                for (int j = 0; j < block.n; j++)
                    vals[i + block.rowoffset][j + block.coloffset] = block.vals[i][j];
            }
                
        for (int i = 0; i < this.m - this.borderRows; i++)
            vals[i][i] = 1;
                
        String result = "";
        for (int i = 0; i < this.m; i++)
            {
            for (int j = 0; j < this.m; j++)
                result += (" " + vals[i][j]);
            result += "\n";
            }
        return result;
        }
        
    public DenseMatrix getDenseMatrix()
        {
        double[][] vals = new double[this.m][this.m];
                
        for (int k = 0; k < blocks.numObjs; k++)
            {
            Block block = (Block)blocks.objs[k];
            for (int i = 0; i < block.m; i++)
                for (int j = 0; j < block.n; j++)
                    vals[i + block.rowoffset][j + block.coloffset] = block.vals[i][j];
            }
                
        for (int i = 0; i < this.m - this.borderRows; i++)
            vals[i][i] = 1;
                
        return new DenseMatrix(vals);
        }
        
    public double[] getPivots()
        {
        int m = this.m;
        double[] pivots = getDiagonalMatrix().vals;
                
        for (int i = 0; i < this.m; i++)
            {
            if (pivots[i] <= 0)
                pivots[i] = 1;
            else
                pivots[i] = 1/pivots[i];
                        
            // Get all the upper blocks with a row in this row
            Bag matchingUpperBlocks = upperBlockRows[i];
                        
            // Get all the lower blocks with a column in this row
            Bag matchingLowerBlocks = lowerBlockCols[i];
                        
            if (matchingLowerBlocks != null && matchingUpperBlocks != null)
                {
                // TODO: this would be a lot more efficient if the blocks were
                // sorted by starting row or col number
                                
                // Loop through the upper blocks and lower blocks to find
                // matches between the upper blocks columns and the lower blocks
                // rows
                for (int blockup = 0; blockup < matchingUpperBlocks.numObjs; blockup++)
                    {
                    Block upper = (Block)matchingUpperBlocks.objs[blockup];
                    int upperRowNum = i - upper.rowoffset;
                                        
                    for (int blockdown = 0; blockdown < matchingLowerBlocks.numObjs; blockdown++)
                        {
                        Block lower = (Block)matchingLowerBlocks.objs[blockdown];
                                                
                        // See if this block has columns numbers matching
                        // the upper blocks row numbers
                        int minCol = Math.max(lower.rowoffset, upper.coloffset);
                        int maxCol = Math.min(lower.rowoffset + lower.m, upper.coloffset + upper.n);
                                                
                        if (minCol < maxCol)
                            {
                            int lowerColNum = i - lower.coloffset;
                                                        
                            for (int j = minCol; j < maxCol; j++)
                                {
                                int upperColNum = j - upper.coloffset;
                                int lowerRowNum = j - lower.rowoffset;
                                                                
                                if (upper.vals[upperRowNum][upperColNum] != 0 && lower.vals[lowerRowNum][lowerColNum] != 0)
                                    pivots[j] = pivots[j] - lower.vals[lowerRowNum][lowerColNum] * pivots[i] * upper.vals[upperRowNum][upperColNum];
                                }
                            }       
                        }
                    }
                }
            }
        
        return pivots;
        }
        
    public Vector DILUSolve(Vector x, double[] pivots)
        {
        Vector y = new Vector(x.m);
        for (int i = 0; i < x.m; i++)
            {
            Bag matchingLowerBlocks = lowerBlockRows[i];
            Bag matchingIntersectionBlocks = intersectionBlockRows[i];
                        
            double sum = 0;
                        
            if (matchingLowerBlocks != null)
                {
                for (int blockdown = 0; blockdown < matchingLowerBlocks.numObjs; blockdown++)
                    {
                    Block lower = (Block)matchingLowerBlocks.objs[blockdown];
                    int lowerRowNum = i - lower.rowoffset;
                                        
                    for (int jRel = 0; jRel < lower.n; jRel++)
                        sum = sum + lower.vals[lowerRowNum][jRel] * y.vals[jRel + lower.coloffset];
                    }
                }
                        
            if (matchingIntersectionBlocks != null)
                {
                for (int blockdown = 0; blockdown < matchingIntersectionBlocks.numObjs; blockdown++)
                    {
                    Block lower = (Block)matchingIntersectionBlocks.objs[blockdown];
                    int lowerRowNum = i - lower.rowoffset;
                                        
                    for (int jRel = 0; jRel < lower.n; jRel++)
                        {
                        if (jRel + lower.coloffset < i)
                            sum = sum + lower.vals[lowerRowNum][jRel] * y.vals[jRel + lower.coloffset];
                        }
                    }
                }
            y.vals[i] = pivots[i] * (x.vals[i] - sum);
            }
                
        for (int i = x.m - 1; i >= 0; i--)
            {
            Bag matchingUpperBlocks = upperBlockRows[i];
            double sum = 0;
                        
            if (matchingUpperBlocks != null)
                {
                for (int blockup = 0; blockup < matchingUpperBlocks.numObjs; blockup++)
                    {
                    Block upper = (Block)matchingUpperBlocks.objs[blockup];
                    int upperRowNum = i - upper.rowoffset;
                                        
                    for (int jRel = 0; jRel < upper.n; jRel++)
                        sum = sum + upper.vals[upperRowNum][jRel] * y.vals[jRel + upper.coloffset];
                    }
                }
            y.vals[i] = y.vals[i] - pivots[i] * sum;
            }
        
        return y;
        }
        
    public Vector DILUTransposeSolve(Vector x, double[] pivots)
        {
        Vector x_temp = new Vector(x.m);
        x.copyInto(x_temp);
        Vector y = new Vector(x.m);
        Vector z = new Vector(x.m);
                
        for (int i = 0; i < x_temp.m; i++)
            {
            z.vals[i] = x_temp.vals[i];
            double tmp = pivots[i] * z.vals[i];
                        
            Bag matchingUpperBlocks = upperBlockRows[i];
            if (matchingUpperBlocks != null)
                {
                for (int blockup = 0; blockup < matchingUpperBlocks.numObjs; blockup++)
                    {
                    Block upper = (Block)matchingUpperBlocks.objs[blockup];
                    int upperRowNum = i - upper.rowoffset;
                                        
                    for (int jRel = 0; jRel < upper.n; jRel++)
                        x_temp.vals[jRel + upper.coloffset] = x_temp.vals[jRel + upper.coloffset] - tmp * upper.vals[upperRowNum][jRel];
                    }
                }
            }
                
        for (int i = x_temp.m - 1; i >= 0; i--)
            {
            y.vals[i] = pivots[i] * z.vals[i];
                        
            Bag matchingLowerBlocks = lowerBlockRows[i];
            Bag matchingIntersectionBlocks = intersectionBlockRows[i];
                        
            if (matchingLowerBlocks != null)
                {
                for (int blockdown = 0; blockdown < matchingLowerBlocks.numObjs; blockdown++)
                    {
                    Block lower = (Block)matchingLowerBlocks.objs[blockdown];
                    int lowerRowNum = i - lower.rowoffset;
                                        
                    for (int jRel = 0; jRel < lower.n; jRel++)
                        z.vals[jRel + lower.coloffset] = z.vals[jRel + lower.coloffset] - lower.vals[lowerRowNum][jRel] * y.vals[i];
                    }
                }
                        
            if (matchingIntersectionBlocks != null)
                {
                for (int blockdown = 0; blockdown < matchingIntersectionBlocks.numObjs; blockdown++)
                    {
                    Block lower = (Block)matchingIntersectionBlocks.objs[blockdown];
                    int lowerRowNum = i - lower.rowoffset;
                                        
                    for (int jRel = 0; jRel < lower.n; jRel++)
                        {
                        if (jRel + lower.coloffset < i)
                            z.vals[jRel + lower.coloffset] = z.vals[jRel + lower.coloffset] - lower.vals[lowerRowNum][jRel] * y.vals[i];
                        }
                    }
                }
            }
        return y;
        }
    }
