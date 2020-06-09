package sim.util.matrix;

public class Vector 
    {
    public double[] vals;
    public int m;
    private static double[] zeros = new double[0];
        
    public Vector(double[] vals)
        {
        this.vals = vals;
        this.m = vals.length;
        }
        
    public Vector(int m)
        {
        this.m = m;
        this.vals = new double[m];
        }
        
    public Vector minus(Vector other)
        {
        return minus(other, new Vector(other.m));
        }
        
    public Vector minus(Vector other, Vector C)
        {
        int otherm = other.m;
        //if (this.m != otherm)
        //      throw new RuntimeException("Dimensions don't agree");
                
        double[] thisvals = this.vals;
        double[] othervals = other.vals;
                
        double[] result = C.vals;
        for (int i = 0; i < otherm; i++)
            result[i] = thisvals[i] - othervals[i];
        return C;
        }
        
    public Vector plus(Vector other)
        {
        return plus(other, new Vector (other.m));
        }
        
    // C = A + B
    public Vector plus(Vector B, Vector C)
        {
        int thism = this.m;
        /*if (thism != B.m)
          throw new RuntimeException("Dimensions don't agree");
                
          if (thism != C.m)
          throw new RuntimeException("Result vector wrong size");
        */
        double[] thisvals = this.vals;
        double[] Cvals = C.vals;
        double[] Bvals = B.vals;
                
        for (int i = 0; i < thism; i++)
            Cvals[i] = thisvals[i] + Bvals[i];
        return C;
        }
        
    public Vector copy()
        {
        double[] result = new double[this.m];
        System.arraycopy(this.vals, 0, result, 0, this.vals.length);
        return new Vector(result);      
        }
        
    public Vector copyInto(Vector other)
        {
        System.arraycopy(this.vals, 0, other.vals, 0, this.vals.length);
        return other;
        }
        
    public double dot(Vector other)
        {
        double result = 0;
        int otherm = other.m;
        double[] thisvals = this.vals;
        double[] othervals = other.vals;
        for (int i = 0; i < otherm; i++)
            result += thisvals[i] * othervals[i];
        return result;
        }
        
    public Vector times(double other)
        {
        return times(other, new Vector(this.m));
        }
        
    public Vector times(double other, Vector C)
        {
        double[] result = C.vals;
        double[] vals = this.vals;
        int thism = this.m;
        for (int i = 0; i < thism; i++)
            result[i] = vals[i] * other;
        return C;
        }
        
    public String toString()
        {
        String result = "";
        for (int i = 0; i < m; i++)
            result += (vals[i] + "\n");
        return result;
        }
        
    public void clear()
        {
        if (zeros.length < this.m)
            zeros = new double[this.m];
        double[] thisvals = this.vals;
        System.arraycopy(zeros, 0, thisvals, 0, thisvals.length);
        }
        
    public Vector(DenseMatrix jam)
        {
        this.vals = new double[jam.m];
        this.m = jam.m;
        for (int i = 0; i < this.m; i++)
            this.vals[i] = jam.vals[i][0];
        }
         
    public DenseMatrix getDenseMatrix()
        {
        DenseMatrix jam = new DenseMatrix(this.m, 1);
        for (int i = 0; i < this.m; i++)
            jam.vals[i][0] = this.vals[i];
        return jam;
        }
    }
