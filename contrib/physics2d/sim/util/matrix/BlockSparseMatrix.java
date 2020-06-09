
package sim.util.matrix;

import sim.util.matrix.Block;


public class BlockSparseMatrix extends Matrix
    {
    private sim.util.Bag blocks;
        
    public BlockSparseMatrix(int m, int n)
        {
        this.m = m;
        this.n = n;
        this.blocks = new sim.util.Bag();
        }
        
    public void setBlock(int rowoffset, int coloffset, double[][] vals)
        {
        Block block = new Block(vals.length, vals[0].length, rowoffset, coloffset, vals);
        this.blocks.add(block);
        }
        
    public Vector times(Vector other)
        {
        return times(other, new Vector(this.m));
        }
        
    public Vector times(Vector other, Vector result)
        {
        result.clear();
                
        int thism = this.m;
        int thisn = this.n;
        int otherm = other.m;
        int resultm = result.m;
                
        int numObjs = blocks.numObjs;
        Object[] blockobjs = blocks.objs;
                
        double[] othervals = other.vals;
        double[] resultvals = result.vals;
                
        for (int i = 0; i < numObjs; i++)
            {
            Block block = (Block)blockobjs[i];
            int rowoffset = block.rowoffset;
            int coloffset = block.coloffset;
            double[][] vals = block.vals;
                        
            int blockm = block.m;
            int blockn = block.n;
                        
            for (int j = 0; j < blockm; j++)
                {
                int absRow = rowoffset + j;
                double curVectorVal = resultvals[absRow];
                double[] row = vals[j];
                for (int k = 0; k < blockn; k++)
                    curVectorVal += (row[k] * othervals[k + coloffset]);
                resultvals[absRow] = curVectorVal;
                }
            }
        return result;
        }
        
    public Vector transposeTimes(Vector other)
        {
        return transposeTimes(other, new Vector(this.n));
        }
        
    public Vector transposeTimes(Vector other, Vector C)
        {
        C.clear();
        double[] othervals = other.vals;
        double[] result = C.vals;
                
        int numObjs = blocks.numObjs;
        Object[] blockobjs = blocks.objs;
                
        for (int k = 0; k < numObjs; k++)
            {
            Block block = (Block)blockobjs[k];
                        
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
        double[][] vals = new double[this.m][this.n];
                
        for (int k = 0; k < blocks.numObjs; k++)
            {
            Block block = (Block)blocks.objs[k];
            for (int i = 0; i < block.m; i++)
                for (int j = 0; j < block.n; j++)
                    vals[i + block.rowoffset][j + block.coloffset] = block.vals[i][j];
            }
                
        String result = "";
        for (int i = 0; i < this.m; i++)
            {
            for (int j = 0; j < this.n; j++)
                result += (" " + vals[i][j]);
            result += "\n";
            }
        return result;
        }
        
    public DenseMatrix getDenseMatrix()
        {
        DenseMatrix denseMat = new DenseMatrix(this.m, this.n);
        for (int k = 0; k < blocks.numObjs; k++)
            {
            Block block = (Block)blocks.objs[k];
            denseMat.setSubMatrix(block.rowoffset, block.rowoffset + block.m - 1, block.coloffset, block.coloffset + block.n - 1, new DenseMatrix(block.vals));
            }
        return denseMat;
        }
    }

