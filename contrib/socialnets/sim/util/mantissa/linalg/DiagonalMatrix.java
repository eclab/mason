package sim.util.mantissa.linalg;

import java.io.Serializable;

/** This class implements diagonal matrices of linear algebra.

    <p>This file is from the "Mantissa" Java software package found at
    <a href="http://www.spaceroots.org/software/mantissa/index.html">
    http://www.spaceroots.org/software/mantissa/index.html</a>.  The license is included
    at the end of the source file.

    * @version $Id: DiagonalMatrix.java,v 1.1 2007-05-30 14:01:29 feijai Exp $
    * @author L. Maisonobe

    */

public class DiagonalMatrix
    extends SquareMatrix
    implements Serializable, Cloneable {
    /** Simple constructor.
     * This constructor builds a diagonal matrix of specified order, all
     * elements on the diagonal being ones (so this is an identity matrix).
     * @param order order of the matrix
     */
    public DiagonalMatrix(int order) {
        this(order, 1.0);
        }

    /** Simple constructor.
     * This constructor builds a diagonal matrix of specified order and
     * set all diagonal elements to the same value.
     * @param order order of the matrix
     * @param value value for the diagonal elements
     */
    public DiagonalMatrix(int order, double value) {
        super(order);
        for (int index = 0; index < order * order; index += order + 1) {
            data[index] = value;
            }
        }

    /** Simple constructor.
     * Build a matrix with specified elements.
     * @param order order of the matrix
     * @param data table of the matrix elements (stored row after row)
     */
    public DiagonalMatrix(int order, double[] data) {
        super(order, data);
        }

    /** Copy constructor.
     * @param d diagonal matrix to copy
     */
    public DiagonalMatrix(DiagonalMatrix d) {
        super(d);
        }

    public Matrix duplicate() {
        return new DiagonalMatrix(this);
        }

    public void setElement(int i, int j, double value) {
        if (i != j) {
            throw new ArrayIndexOutOfBoundsException("cannot set elements"
                + " out of diagonal in a"
                + " diagonal matrix");
            }
        super.setElement(i, j, value);
        }

    public double getDeterminant(double epsilon) {
        double determinant = data[0];
        for (int index = columns + 1; index < columns * columns; index += columns + 1) {
            determinant *= data[index];
            }
        return determinant;
        }

    public SquareMatrix getInverse(double epsilon)
        throws SingularMatrixException {

        DiagonalMatrix inv = new DiagonalMatrix (columns);

        for (int index = 0; index < columns * columns; index += columns + 1) {
            if (Math.abs(data[index]) < epsilon) {
                throw new SingularMatrixException();
                }
            inv.data[index] = 1.0 / data[index];
            }

        return inv;

        }

    public Matrix solve(Matrix b, double epsilon)
        throws SingularMatrixException {

        Matrix result = b.duplicate();

        for (int i = 0; i < columns; ++i) {
            double diag = data[i * (columns + 1)];
            if (Math.abs(diag) < epsilon) {
                throw new SingularMatrixException();
                }
            double inv = 1.0 / diag;

            NonNullRange range = result.getRangeForRow(i);
            for (int index = i * b.columns + range.begin;
                 index < i * b.columns + range.end;
                 ++index) {
                result.data[index] = inv * b.data[index];
                }
            }

        return result;

        }

    public NonNullRange getRangeForRow(int i) {
        return new NonNullRange(i, i + 1);
        }

    public NonNullRange getRangeForColumn(int j) {
        return new NonNullRange(j, j + 1);
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
