package sim.util.matrix;

public class DenseMatrix extends Matrix
    {
    public double[][] vals;
        
    public DenseMatrix(int m, int n)
        {
        this.m = m;
        this.n = n;
        this.vals = new double[m][n];
        }
        
    public DenseMatrix(double[][] vals)
        {
        this.vals = vals;
        this.m = vals.length;
        this.n = vals[0].length;
        }
        
    public Vector times(Vector B)
        {
        double thism = this.m;
        double thisn = this.n;
        double[] result = new double[this.m];
        double[] bvals = B.vals;
        double[][] thisvals = this.vals;
                
        if (thisn != B.m)
            throw new RuntimeException("Matrix dimensions don't agree");
                
        for (int i = 0; i < thism; i++)
            {
            double[] thisrow = thisvals[i];
            double res = 0;
            for (int j = 0; j < thisn; j++)
                res += thisrow[j] * bvals[j];
            result[i] = res;
            }
        return new Vector(result);
        }
        
    public String toString()
        {
        double[][] vals = this.vals;
                
        String result = "";
        for (int i = 0; i < this.m; i++)
            {
            for (int j = 0; j < this.n; j++)
                result += (" " + vals[i][j]);
            result += "\n";
            }
        return result;
        }
        
    ///////////////////////////////////////////////////////////////////
    // Got (and slightly adapted all the rest of the code code 
    // from JAMA - http://math.nist.gov/javanumerics/jama/ 
    ///////////////////////////////////////////////////////////////////
    public DenseMatrix solve (DenseMatrix B) 
        {
        if (this.m != this.n)
            throw new RuntimeException("Attempting to solve non-square matrix");
                
        return (new LUDecomposition(this)).solve(B);
        }
        
    public DenseMatrix times(DenseMatrix other)
        {
        int thism = this.m;
        int thisn = this.n;
        int otherm = other.m;
        int othern = other.n;
        double[][] thisvals = this.vals;
        double[][] othervals = other.vals;
                
        if (otherm != thisn)
            throw new IllegalArgumentException("Matrix inner dimensions must agree.");

        DenseMatrix X = new DenseMatrix(thism,othern);          
        double[][] C = X.vals;
        double[] Bcolj = new double[n];
        for (int j = 0; j < othern; j++) 
            {
            for (int k = 0; k < thisn; k++) 
                Bcolj[k] = othervals[k][j];
            for (int i = 0; i < m; i++) 
                {
                double[] Arowi = thisvals[i];
                double s = 0;
                for (int k = 0; k < thisn; k++)
                    s += Arowi[k]*Bcolj[k];
                C[i][j] = s;
                }
            }
        return X;
        }
        
    public DenseMatrix getSubMatrix (int[] r, int j0, int j1) 
        {
        DenseMatrix X = new DenseMatrix(r.length,j1-j0+1);
        double[][] B = X.vals;
        try 
            {
            for (int i = 0; i < r.length; i++) 
                {
                for (int j = j0; j <= j1; j++) 
                    B[i][j-j0] = vals[r[i]][j];
                }
            } 
        catch(ArrayIndexOutOfBoundsException e) 
            {
            throw new ArrayIndexOutOfBoundsException("Submatrix indices");
            }
        return X;
        }

    public Vector times(Vector B, Vector C)
        {
        double thism = this.m;
        double thisn = this.n;
        double[] cvals = C.vals;
        double[] bvals = B.vals;
        double[][] thisvals = this.vals;
                
        if (thisn != B.m)
            throw new RuntimeException("Matrix dimensions don't agree");
                
        if (thism != C.m)
            throw new RuntimeException("Result vector wrong size");
                
        for (int i = 0; i < thism; i++)
            {
            double[] thisrow = thisvals[i];
            double res = 0;
            for (int j = 0; j < thisn; j++)
                res += thisrow[j] * bvals[j];
            cvals[i] = res;
            }
        return C;
        }
        
    public Vector transposeTimes(Vector B)
        {
        return transposeTimes(B, new Vector(this.m));
        }
        
    public Vector transposeTimes(Vector B, Vector C)
        {
        double thism = this.m;
        double thisn = this.n;
        double[] result = C.vals;
        double[] bvals = B.vals;
        double[][] thisvals = this.vals;
                
        if (thism != B.m)
            throw new RuntimeException("Matrix dimensions don't agree");
                
        for (int i = 0; i < thisn; i++)
            {
            double res = 0;
            for (int j = 0; j < thism; j++)
                res += vals[j][i] * bvals[j];
            result[i] = res;
            }
        return C;
        }
        
    public DiagonalMatrix getDiagonalMatrix()
        {
        if (this.m != this.n)
            throw new RuntimeException("Matrix is not square");
                
        DiagonalMatrix result = new DiagonalMatrix(this.m);
        for (int i = 0; i < this.m; i++)
            result.vals[i] = this.vals[i][i];
        return result;
        }
        
    public void setSubMatrix (int i0, int i1, int j0, int j1, DenseMatrix X) 
        {
        try 
            {
            for (int i = i0; i <= i1; i++) 
                {
                for (int j = j0; j <= j1; j++)
                    vals[i][j] = X.vals[i-i0][j-j0];
                }
            } 
        catch(ArrayIndexOutOfBoundsException e) 
            {
            throw new ArrayIndexOutOfBoundsException("Submatrix indices");
            }
        }
        
    public DenseMatrix transpose() 
        {
        double[][] thisvals = this.vals;
        int thism = m;
        int thisn = n;
                
        DenseMatrix X = new DenseMatrix(thisn,thism);
        double[][] C = X.vals;
        for (int i = 0; i < thism; i++) 
            {
            double[] row = thisvals[i];
            for (int j = 0; j < thisn; j++)
                C[j][i] = row[j];
            }
        return X;
        }
        
    public DenseMatrix minus(DenseMatrix other)
        {
        int thism = m;
        int thisn = n;
                
        if (thism != other.m && thisn != other.n)
            throw new RuntimeException("Matrix dimensions don't agreee");
                
        double[][] thisvals = this.vals;
        double[][] othervals = other.vals;
                
        DenseMatrix X = new DenseMatrix(thism,thisn);
        double[][] C = X.vals;
        for (int i = 0; i < thism; i++) 
            {
            double[] thisrow = thisvals[i];
            double[] otherrow = othervals[i];
            double[] resultrow = C[i];
                        
            for (int j = 0; j < thisn; j++)
                resultrow[j] = thisrow[j] - otherrow[j];
            }
        return X;
        }
        
    public DenseMatrix plus(DenseMatrix other)
        {
        int thism = m;
        int thisn = n;
                
        if (thism != other.m && thisn != other.n)
            throw new RuntimeException("Matrix dimensions don't agreee");
                
        double[][] thisvals = this.vals;
        double[][] othervals = other.vals;
                
        DenseMatrix X = new DenseMatrix(thism,thisn);
        double[][] C = X.vals;
        for (int i = 0; i < thism; i++) 
            {
            double[] thisrow = thisvals[i];
            double[] otherrow = othervals[i];
            double[] resultrow = C[i];
                        
            for (int j = 0; j < thisn; j++)
                resultrow[j] = thisrow[j] + otherrow[j];
            }
        return X;
        }
        
    public DenseMatrix times(double value)
        {
        int thism = m;
        int thisn = n;
                
        double[][] thisvals = this.vals;
                
        DenseMatrix X = new DenseMatrix(thism,thisn);
        double[][] C = X.vals;
        for (int i = 0; i < thism; i++) 
            {
            double[] thisrow = thisvals[i];
            double[] resultrow = C[i];
                        
            for (int j = 0; j < thisn; j++)
                resultrow[j] = thisrow[j] * value;
            }
        return X;
        }
        
    private class LUDecomposition implements java.io.Serializable 
        {

        /* ------------------------
           Class variables
           * ------------------------ */

        /** Array for internal storage of decomposition.
            @serial internal array storage.
        */
        private double[][] LU;
        
        /** Row and column dimensions, and pivot sign.
            @serial column dimension.
            @serial row dimension.
            @serial pivot sign.
        */
        private int m, n, pivsign; 

        /** Internal storage of pivot vector.
            @serial pivot vector.
        */
        private int[] piv;

        /* ------------------------
           Constructor
           * ------------------------ */

        /** LU Decomposition
            @param  A   Rectangular matrix
            @return     Structure to access L, U and piv.
        */
        public LUDecomposition(DenseMatrix A) 
            {
            // Use a "left-looking", dot-product, Crout/Doolittle algorithm.
            LU = A.vals;
            m = A.m;
            n = A.n;
            piv = new int[m];
            for (int i = 0; i < m; i++)
                piv[i] = i;
                        
            pivsign = 1;
            double[] LUrowi;
            double[] LUcolj = new double[m];

            // Outer loop.
            for (int j = 0; j < n; j++) 
                {
                // Make a copy of the j-th column to localize references.
                for (int i = 0; i < m; i++) 
                    LUcolj[i] = LU[i][j];
                        
                // Apply previous transformations.
                for (int i = 0; i < m; i++)
                    {
                    LUrowi = LU[i];
                    // Most of the time is spent in the following dot product.

                    int kmax = Math.min(i,j);
                    double s = 0.0;
                    for (int k = 0; k < kmax; k++)
                        s += LUrowi[k]*LUcolj[k];
                    LUrowi[j] = LUcolj[i] -= s;
                    }
                   
                // Find pivot and exchange if necessary.
                int p = j;
                for (int i = j+1; i < m; i++) 
                    {
                    if (Math.abs(LUcolj[i]) > Math.abs(LUcolj[p]))
                        p = i;
                    }
                                
                if (p != j) 
                    {
                    for (int k = 0; k < n; k++)
                        {
                        double t = LU[p][k]; LU[p][k] = LU[j][k]; LU[j][k] = t;
                        }
                    int k = piv[p]; piv[p] = piv[j]; piv[j] = k;
                    pivsign = -pivsign;
                    }
                                
                // Compute multipliers.
                                     
                if (j < m & LU[j][j] != 0.0) 
                    {
                    for (int i = j+1; i < m; i++)
                        LU[i][j] /= LU[j][j];
                    }
                }
            }

        /* ------------------------
           Public Methods
           * ------------------------ */
                
        /** Is the matrix nonsingular?
            @return     true if U, and hence A, is nonsingular.
        */
        public boolean isNonsingular() 
            {
            for (int j = 0; j < n; j++) 
                {
                if (LU[j][j] == 0)
                    return false;
                }
            return true;
            }
                
        /** Return lower triangular factor
            @return     L
        */
        public DenseMatrix getL() 
            {
            DenseMatrix X = new DenseMatrix(m,n);
            double[][] L = X.vals;
            for (int i = 0; i < m; i++) 
                {
                for (int j = 0; j < n; j++) 
                    {
                    if (i > j)
                        L[i][j] = LU[i][j];
                    else if (i == j)
                        L[i][j] = 1.0;
                    else
                        L[i][j] = 0.0;
                    }
                }
            return X;
            }
                
        /** Return upper triangular factor
            @return     U
        */
        public DenseMatrix getU() 
            {
            DenseMatrix X = new DenseMatrix(n,n);
            double[][] U = X.vals;
            for (int i = 0; i < n; i++) 
                {
                for (int j = 0; j < n; j++) 
                    {
                    if (i <= j)
                        U[i][j] = LU[i][j];
                    else
                        U[i][j] = 0.0;
                    }
                }
            return X;
            }
                
        /** Return pivot permutation vector
            @return     piv
        */
        public int[] getPivot()
            {
            int[] p = new int[m];
            for (int i = 0; i < m; i++) {
                p[i] = piv[i];
                }
            return p;
            }
                
        /** Return pivot permutation vector as a one-dimensional double array
            @return     (double) piv
        */
        public double[] getDoublePivot() 
            {
            double[] vals = new double[m];
            for (int i = 0; i < m; i++)
                vals[i] = (double) piv[i];
            return vals;
            }
                
        /** Determinant
            @return     det(A)
            @exception  IllegalArgumentException  Matrix must be square
        */
        public double det() 
            {
            if (m != n)
                throw new IllegalArgumentException("Matrix must be square.");
            double d = (double) pivsign;
            for (int j = 0; j < n; j++)
                d *= LU[j][j];
            return d;
            }
                
        /** Solve A*X = B
            @param  B   A Matrix with as many rows as A and any number of columns.
            @return     X so that L*U*X = B(piv,:)
            @exception  IllegalArgumentException Matrix row dimensions must agree.
            @exception  RuntimeException  Matrix is singular.
        */
        public DenseMatrix solve(DenseMatrix B) 
            {
            if (B.m != m)
                throw new IllegalArgumentException("Matrix row dimensions must agree.");
            if (!this.isNonsingular())
                throw new RuntimeException("Matrix is singular.");
                        
            // Copy right hand side with pivoting
            int nx = B.n;
            DenseMatrix Xmat = B.getSubMatrix(piv,0,nx-1);
            double[][] X = Xmat.vals;
                        
            // Solve L*Y = B(piv,:)
            for (int k = 0; k < n; k++) 
                {
                for (int i = k+1; i < n; i++) 
                    {
                    for (int j = 0; j < nx; j++)
                        X[i][j] -= X[k][j]*LU[i][k];
                    }
                }
            // Solve U*X = Y;
            for (int k = n-1; k >= 0; k--) 
                {
                for (int j = 0; j < nx; j++)
                    X[k][j] /= LU[k][k];
                for (int i = 0; i < k; i++) 
                    {
                    for (int j = 0; j < nx; j++)
                        X[i][j] -= X[k][j]*LU[i][k];
                    }
                }
            return Xmat;
            }
        }
    }
