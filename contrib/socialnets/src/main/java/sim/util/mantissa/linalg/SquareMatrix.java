package sim.util.mantissa.linalg;

import java.io.Serializable;

/** This class factor all services common to square matrices of linear algebra.

 * <p>This class is the base class of all square matrix
 * implementations. It extends the {@link Matrix} class with methods
 * specific to square matrices.</p>

 <p>This file is from the "Mantissa" Java software package found at
 <a href="http://www.spaceroots.org/software/mantissa/index.html">
 http://www.spaceroots.org/software/mantissa/index.html</a>.  The license is included
 at the end of the source file.

 * @version $Id: SquareMatrix.java,v 1.1 2007-05-30 14:01:30 feijai Exp $
 * @author L. Maisonobe

 */

public abstract class SquareMatrix
    extends Matrix
    implements Serializable, Cloneable {
    /** Simple constructor.
     * Build a matrix with null elements.
     * @param order order of the matrix
     */
    protected SquareMatrix(int order) {
        super(order, order);
        }

    /** Simple constructor.
     * Build a matrix with specified elements.
     * @param order order of the matrix
     * @param data table of the matrix elements (stored row after row)
     */
    protected SquareMatrix(int order, double[] data) {
        super(order, order, data);
        }

    /** Copy constructor.
     * @param m matrix to copy
     */
    protected SquareMatrix(SquareMatrix m) {
        super(m);
        }

    /** Get the determinant of the matrix.
     * @param epsilon threshold on matrix elements below which the
     * matrix is considered singular (this is used by the derived
     * classes that use a factorization to compute the determinant)
     * @return the determinant of the matrix
     */
    public abstract double getDeterminant(double epsilon);

    /** Invert the instance.
     * @param epsilon threshold on matrix elements below which the
     * matrix is considered singular
     * @return the inverse matrix of the instance
     * @exception SingularMatrixException if the matrix is singular
     */
    public SquareMatrix getInverse(double epsilon)
        throws SingularMatrixException {
        return (SquareMatrix) solve(new DiagonalMatrix (columns), epsilon);
        }


    /** Solve the <tt>A.X = B</tt> equation.
     * @param b second term of the equation
     * @param epsilon threshold on matrix elements below which the
     * matrix is considered singular
     * @return a matrix X such that <tt>A.X = B</tt>, where A is the instance
     * @exception SingularMatrixException if the matrix is singular
     */
    public abstract Matrix solve(Matrix b, double epsilon)
        throws SingularMatrixException;

    /** Solve the <tt>A.X = B</tt> equation.
     * @param b second term of the equation
     * @param epsilon threshold on matrix elements below which the
     * matrix is considered singular
     * @return a matrix X such that <tt>A.X = B</tt>, where A is the instance
     * @exception SingularMatrixException if the matrix is singular
     */
    public SquareMatrix solve(SquareMatrix b, double epsilon)
        throws SingularMatrixException {
        return (SquareMatrix) solve((Matrix) b, epsilon);
        }

    }



/**
   COPYRIGHT AND LICENSE

   Copyright (c) 2001-2005, Luc Maisonobe
   All rights reserved.

   Redistribution and use in source and binary forms,
   with or without modification, are permitted provided that
   the following conditions are met:

   Redistributions of source code must retain the above
   copyright notice, this list of conditions and the
   following disclaimer.
   Redistributions in binary form must reproduce the above
   copyright notice, this list of conditions and the
   following disclaimer in the documentation and/or other
   materials provided with the distribution.
   Neither the names of spaceroots.org, spaceroots.com nor
   the names of their contributors may be used to endorse or
   promote products derived from this software without
   specific prior written permission.

   THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND
   CONTRIBUTORS 'AS IS' AND ANY EXPRESS OR IMPLIED
   WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
   WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A
   PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE
   COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT,
   INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
   CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
   PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
   DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
   CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
   CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE
   OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
   SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH
   DAMAGE.

*/
