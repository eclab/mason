
package sim.util.matrix;

public class DiagonalMatrix extends Matrix
    {
    public double[] vals;
        
    public DiagonalMatrix(double[] vals)
        {
        this.vals = vals;
        this.m = vals.length;
        this.n = this.m;
        }
        
    public DiagonalMatrix(int m)
        {
        this.m = m;
        this.n = this.m;
        this.vals = new double[m];
        }
        
    public Vector times(Vector other)
        {
        return times(other, new Vector(this.m));
        }
        
    public Vector times(Vector other, Vector C)
        {
        C.clear();
                
        double[] result = C.vals;
        double[] vals = this.vals;
        double[] othervals = other.vals;
        int m = this.m;
                
        for (int i = 0; i < m; i++)
            result[i] = vals[i] * othervals[i];
                        
        return C;
        }
        
    public DiagonalMatrix getDiagonalMatrix()
        {
        return this;
        }
        
    public Vector transposeTimes(Vector other, Vector C)
        {
        // transposing doesn't do anything to a diagonal matrix
        return times(other, C);
        }
        
    public Vector transposeTimes(Vector other)
        {
        // transposing doesn't do anything to a diagonal matrix
        return this.times(other);
        }
        
    public String toString()
        {
        String result = "";
        for (int i = 0; i < this.m; i++)
            {
            for (int j = 0; j < this.m; j++)
                if (i == j)
                    result = result + " " + vals[i];
                else
                    result += " 0";
            result += "\n";
            }
        return result;
        }

    public Vector solve(Vector b)
        {
        return solve(b, new Vector(b.m));
        }
        
    public Vector solve(Vector b, Vector x)
        {
        double[] result = x.vals;
        double thism = this.m;
        double[] thisvals = this.vals;
        double[] bvals = b.vals;
                
        for (int i = 0; i < thism; i++)
            {
            if (thisvals[i] == 0)
                result[i] = bvals[i];
            else
                result[i] = bvals[i] / thisvals[i];
            }
        return x;
        }
        
    public DiagonalMatrix(DenseMatrix denseMat)
        {
        this.vals = new double[denseMat.m];
        this.m = denseMat.m;
        for (int i = 0; i < this.m; i++)
            this.vals[i] = denseMat.vals[i][i];
        }
        
    public DenseMatrix getDenseMatrix()
        {
        DenseMatrix denseMat = new DenseMatrix(this.m, this.m);
        for (int i = 0; i < this.m; i++)
            denseMat.vals[i][i] = this.vals[i];
        return denseMat;
        }
    }
