package sim.util.mantissa.linalg;

import java.io.Serializable;

/** This class implements upper triangular matrices of linear algebra.

    <p>This file is from the "Mantissa" Java software package found at
    <a href="http://www.spaceroots.org/software/mantissa/index.html">
    http://www.spaceroots.org/software/mantissa/index.html</a>.  The license is included
    at the end of the source file.

    * @version $Id: UpperTriangularMatrix.java,v 1.1 2007-05-30 14:01:30 feijai Exp $
    * @author L. Maisonobe

    */

public class UpperTriangularMatrix
    extends SquareMatrix
    implements Serializable, Cloneable {
    /** Simple constructor.
     * This constructor builds a upper triangular matrix of specified order, all
     * elements being zeros.
     * @param order order of the matrix
     */
    public UpperTriangularMatrix(int order) {
        super(order);
        }

    /** Simple constructor.
     * Build a matrix with specified elements.
     * @param order order of the matrix
     * @param data table of the matrix elements (stored row after row)
     */
    public UpperTriangularMatrix(int order, double[] data) {
        super(order, data);
        }

    /** Copy constructor.
     * @param u upper triangular matrix to copy
     */
    public UpperTriangularMatrix(UpperTriangularMatrix u) {
        super(u);
        }

    public Matrix duplicate() {
        return new UpperTriangularMatrix(this);
        }

    public void setElement(int i, int j, double value) {
        if (i > j) {
            throw new ArrayIndexOutOfBoundsException("cannot set elements"
                + " below diagonal of a"
                + " upper triangular matrix");
            }
        super.setElement(i, j, value);
        }

    /** Add a matrix to the instance.
     * This method adds a matrix to the instance. It does modify the instance.
     * @param u upper triangular matrix to add
     * @exception IllegalArgumentException if there is a dimension mismatch
     */
    public void selfAdd(UpperTriangularMatrix u) {

        // validity check
        if ((rows != u.rows) || (columns != u.columns)) {
            throw new IllegalArgumentException("cannot add a "
                + u.rows + 'x' + u.columns
                + " matrix to a "
                + rows + 'x' + columns
                + " matrix");
            }

        // addition loop
        for (int i = 0; i < rows; ++i) {
            for (int index = i * (columns + 1); index < (i + 1) * columns; ++index) {
                data[index] += u.data[index];
                }
            }

        }

    /** Substract a matrix from the instance.
     * This method substract a matrix from the instance. It does modify the instance.
     * @param u upper triangular matrix to substract
     * @exception IllegalArgumentException if there is a dimension mismatch
     */
    public void selfSub(UpperTriangularMatrix u) {

        // validity check
        if ((rows != u.rows) || (columns != u.columns)) {
            throw new IllegalArgumentException("cannot substract a "
                + u.rows + 'x' + u.columns
                + " matrix from a "
                + rows + 'x' + columns
                + " matrix");
            }

        // substraction loop
        for (int i = 0; i < rows; ++i) {
            for (int index = i * (columns + 1); index < (i + 1) * columns; ++index) {
                data[index] -= u.data[index];
                }
            }

        }

    public double getDeterminant(double epsilon) {
        double determinant = data[0];
        for (int index = columns + 1; index < columns * columns; index += columns + 1) {
            determinant *= data[index];
            }
        return determinant;
        }

    public Matrix solve(Matrix b, double epsilon)
        throws SingularMatrixException {
        // validity check
        if (b.getRows() != rows) {
            throw new IllegalArgumentException("dimension mismatch");
            }

        // prepare the data storage
        int bRows  = b.getRows();
        int bCols  = b.getColumns();

        double[] resultData = new double[bRows * bCols];
        int resultIndex     = bRows * bCols - 1;
        int lowerElements   = 0;
        int upperElements   = 0;
        int minJ            = columns;
        int maxJ            = 0;

        // solve the linear system
        for (int i = rows - 1; i >= 0; --i) {
            double diag = data[i * (columns + 1)];
            if (Math.abs(diag) < epsilon) {
                throw new SingularMatrixException();
                }
            double inv = 1.0 / diag;

            NonNullRange range = b.getRangeForRow(i);
            minJ = Math.min(minJ, range.begin);
            maxJ = Math.max(maxJ, range.end);

            int j = bCols - 1;
            while (j >= maxJ) {
                resultData[resultIndex] = 0.0;
                --resultIndex;
                --j;
                }

            // compute the possibly non null elements
            int bIndex = i * bCols + maxJ - 1;
            while (j >= minJ) {

                // compute the current element
                int index1 = (i + 1) * columns - 1;
                int index2 = (bRows - 1) * bCols + j;
                double value = b.data[bIndex];
                while (index1 >= i * (columns + 1)) {
                    value -= data[index1] * resultData[index2];
                    --index1;
                    index2 -= bCols;
                    }
                value *= inv;
                resultData[resultIndex] = value;

                // count the affected upper and lower elements
                // (in order to deduce the shape of the resulting matrix)
                if (j < i) {
                    ++lowerElements;
                    } else if (i < j) {
                    ++upperElements;
                    }

                --bIndex;
                --resultIndex;
                --j;

                }

            while (j >= 0) {
                resultData[resultIndex] = 0.0;
                --resultIndex;
                --j;
                }

            }

        return MatrixFactory.buildMatrix(bRows, bCols, resultData,
            lowerElements, upperElements);

        }

    public NonNullRange getRangeForRow(int i) {
        return new NonNullRange (i, columns);
        }

    public NonNullRange getRangeForColumn(int j) {
        return new NonNullRange (0, j + 1);
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
