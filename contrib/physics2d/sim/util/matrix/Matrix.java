
package sim.util.matrix;

public abstract class Matrix 
    {
    public int m;
    public int n;
    public abstract Vector times(Vector B);
    public abstract Vector times(Vector B, Vector C);
    public abstract Vector transposeTimes(Vector B);
    public abstract Vector transposeTimes(Vector B, Vector C);
    public abstract DiagonalMatrix getDiagonalMatrix();

    public static Vector solveBiConjugateGradient(BorderedDiagonalIdentityMatrix A, Vector b, Vector x, int maxit, double stop_tol, boolean useILU)
        {
        double b_norm = Math.sqrt(b.dot(b));
        if (b_norm == 0)
            {
            x = b.copyInto(x);
            return x;
            }
                
        Vector r = new Vector(b.m);
        Vector r_tilde = new Vector(b.m);
                
        Vector z = new Vector(b.m);
        Vector z_tilde = new Vector(b.m);
                
        Vector p = new Vector(b.m);
        Vector p_tilde = new Vector(b.m);
                
        Vector q = new Vector(b.m);
        Vector q_tilde = new Vector(b.m);
                
        double rho = 0;
        double rho_prev = 0;
        double beta;
        double alpha;
                
        Vector tmp = new Vector(b.m);

        r = b.minus(A.times(x, tmp), r);
        r_tilde = b.minus(A.transposeTimes(x, tmp), r_tilde);
                
        int i = 1;
        while (i <= maxit)
            {
            double[] pivots = A.getPivots();
                        
            if (useILU)
                {
                z = A.DILUSolve(r, pivots);
                z_tilde = A.DILUTransposeSolve(r_tilde, pivots);
                }
            else
                {
                r.copyInto(z);
                r_tilde.copyInto(z_tilde);
                }
                        
            rho_prev = rho;
            rho = z.dot(r_tilde);
                        
            if (rho == 0)
                {
                throw new RuntimeException("BiConjugate Gradient failed to converge - rho is 0");
                }
                        
            if (i == 1)
                {
                z.copyInto(p);
                z_tilde.copyInto(p_tilde);
                }
            else
                {
                beta = rho / rho_prev;
                p = z.plus(p.times(beta, tmp), p);                      
                p_tilde = z_tilde.plus(p_tilde.times(beta, tmp), p_tilde);
                }
            q = A.times(p, q);
            q_tilde = A.transposeTimes(p_tilde, q_tilde);
                        
            if (p_tilde.dot(q) == 0)
                throw new RuntimeException("P dot Q is 0");
                        
            alpha = rho/(p_tilde.dot(q));
            x = x.plus(p.times(alpha, tmp), x);
            r = r.minus(q.times(alpha, tmp), r);
            r_tilde = r_tilde.minus(q_tilde.times(alpha, tmp), r_tilde);
                        
            double r_norm = Math.sqrt(r.dot(r));
            if (r_norm <= stop_tol * b_norm)
                break;
                        
            i++;
            }
                
        if (i > maxit)
            throw new RuntimeException("BiConjugate Gradient failed to converge - max iterations exceeded");
                
        return x;
        }
        
    public static Vector solveBiConjugateGradient(Matrix J, DiagonalMatrix W, DiagonalMatrix M, sim.util.matrix.Vector b, sim.util.matrix.Vector x, int maxit, double stop_tol)
        {
        double b_norm = Math.sqrt(b.dot(b));
        if (b_norm == 0)
            {
            x = b.copyInto(x);
            return x;
            }
                
        Vector r = new Vector(b.m);
        Vector r_tilde = new Vector(b.m);
                
        Vector z = new Vector(b.m);
        Vector z_tilde = new Vector(b.m);
                
        Vector p = new Vector(b.m);
        Vector p_tilde = new Vector(b.m);
                
        Vector q = new Vector(b.m);
        Vector q_tilde = new Vector(b.m);
                
        double rho = 0;
        double rho_prev = 0;
        double beta;
        double alpha;
                
        Vector tmpJ = new Vector(J.m);
        Vector tmpJt = new Vector(J.n);
        Vector tmpW = new Vector(W.m);
        Vector tmpb = new Vector(b.m);
        Vector tmpx = new Vector(x.m);

        r = b.minus(J.times(W.times(J.transposeTimes(x, tmpJt), tmpW), tmpJ), r);
        r.copyInto(r_tilde);
                
        int i = 1;
        while (i <= maxit)
            {
            M.solve(r, z);
            M.solve(r_tilde, z_tilde);
                        
            rho_prev = rho;
            rho = z.dot(r_tilde);
                        
            if (rho == 0)
                throw new RuntimeException("BiConjugate Gradient failed to converge - rho is 0");
                        
            if (i == 1)
                {
                z.copyInto(p);
                z_tilde.copyInto(p_tilde);
                }
            else
                {
                beta = rho / rho_prev;
                p = z.plus(p.times(beta, tmpb), p);                     
                p_tilde = z_tilde.plus(p_tilde.times(beta, tmpb), p_tilde);
                }
            q = J.times(W.times(J.transposeTimes(p, tmpJt), tmpW), q);
            q_tilde = J.times(W.times(J.transposeTimes(p_tilde, tmpJt), tmpW), q_tilde);
                        
            alpha = rho/(p_tilde.dot(q));
            x = x.plus(p.times(alpha, tmpx), x);
            r = r.minus(q.times(alpha, tmpx), r);
            r_tilde = r_tilde.minus(q_tilde.times(alpha, tmpx), r_tilde);
                        
            double r_norm = Math.sqrt(r.dot(r));
            if (r_norm <= stop_tol * b_norm)
                break;
                        
            i++;
            }
                
        if (i > maxit)
            throw new RuntimeException("BiConjugate Gradient failed to converge - max iterations exceeded");
                
        return x;
        }
    }
