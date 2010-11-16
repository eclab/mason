package sim.util.mantissa.linalg;

import java.io.Serializable;

/** This class factor all services common to matrices.

 * <p>This class is the base class of all matrix implementations, it
 * is also the base class of the {@link SquareMatrix} class which adds
 * methods specific to square matrices.</p>

 * <p>This class both handles the storage of matrix elements and
 * implements the classical operations on matrices (addition,
 * substraction, multiplication, transposition). It relies on two
 * protected methods ({@link #getRangeForRow} and {@link
 * #getRangeForColumn}) to get tight loop bounds for matrices with
 * known structures full of zeros. These methods should be
 * implemented by derived classes to provide information about their
 * specific shape to the general algorithms implemented by this
 * abstract class.</p>

 <p>This file is from the "Mantissa" Java software package found at
 <a href="http://www.spaceroots.org/software/mantissa/index.html">
 http://www.spaceroots.org/software/mantissa/index.html</a>.  The license is included
 at the end of the source file.

 * @version $Id: Matrix.java,v 1.1 2007-05-30 14:01:30 feijai Exp $
 * @author L. Maisonobe

 */

public abstract class Matrix
    implements Serializable {
    /** Simple constructor.
     * Build a matrix with null elements.
     * @param rows number of rows of the matrix
     * @param columns number of columns of the matrix
     */
    protected Matrix(int rows, int columns) {
        // sanity check
        if (rows <= 0 || columns <= 0) {
            throw new IllegalArgumentException("cannot build a matrix"
                + " with negative or null dimension");
            }

        this.rows    = rows;
        this.columns = columns;
        data = new double[rows * columns];
        for (int i = 0; i < data.length; ++i) {
            data[i] = 0.0;
            }

        }

    /** Simple constructor.
     * Build a matrix with specified elements.
     * @param rows number of rows of the matrix
     * @param columns number of columns of the matrix
     * @param data table of the matrix elements (stored row after row)
     */
    public Matrix(int rows, int columns, double[] data) {
        // sanity check
        if (rows <= 0 || columns <= 0) {
            throw new IllegalArgumentException("cannot build a matrix"
                + " with negative or null dimension");
            }

        this.rows    = rows;
        this.columns = columns;
        this.data    = data;

        }

    /** Copy constructor.
     * @param m matrix to copy
     */
    protected Matrix(Matrix m) {
        rows    = m.rows;
        columns = m.columns;
        data    = new double[rows * columns];
        System.arraycopy(m.data, 0, data, 0, m.data.length);
        }

    /** Polymorphic copy operator.
     * This method build a new object of the same type of the
     * instance. It is somewhat similar to the {@link Object#clone}
     * method, except that it has public access, it doesn't throw any
     * specific exception and it returns a Matrix.
     *@see Object#clone
     */
    public abstract Matrix duplicate();

    /** Get the number of rows of the matrix.
     * @return number of rows
     * @see #getColumns
     */
    public int getRows() {
        return rows;
        }

    /** Get the number of columns of the matrix.
     * @return number of columns
     * @see #getRows
     */
    public int getColumns() {
        return columns;
        }

    /** Get a matrix element.
     * @param i row index, from 0 to rows - 1
     * @param j column index, from 0 to cols - 1
     * @return value of the element
     * @exception ArrayIndexOutOfBoundsException if the indices are wrong
     * @see #setElement
     */
    public double getElement(int i, int j) {
        if (i < 0 || i >= rows || j < 0 || j >= columns) {
            throw new IllegalArgumentException("cannot get element ("
                + i + ", " + j + ") from a "
                + rows + 'x' + columns
                + " matrix");
            }
        return data[i * columns + j];
        }

    /** Set a matrix element.
     * @param i row index, from 0 to rows - 1
     * @param j column index, from 0 to cols - 1
     * @param value value of the element
     * @exception ArrayIndexOutOfBoundsException if the indices are wrong
     * @see #getElement
     */
    public void setElement(int i, int j, double value) {
        if (i < 0 || i >= rows || j < 0 || j >= columns) {
            throw new IllegalArgumentException("cannot set element ("
                + i + ", " + j + ") in a "
                + rows + 'x' + columns
                + " matrix");
            }
        data[i * columns + j] = value;
        }

    /** Add a matrix to the instance.
     * This method adds a matrix to the instance. It returns a new
     * matrix and does not modify the instance.
     * @param m matrix to add
     * @return a new matrix containing the result
     * @exception IllegalArgumentException if there is a dimension mismatch
     */
    public Matrix add(Matrix m) {

        // validity check
        if ((rows != m.rows) || (columns != m.columns)) {
            throw new IllegalArgumentException("cannot add a "
                + m.rows + 'x' + m.columns
                + " matrix to a "
                + rows + 'x' + columns
                + " matrix");
            }

        double[] resultData    = new double[rows * columns];
        int      resultIndex   = 0;
        int      lowerElements = 0;
        int      upperElements = 0;

        // external loop through the rows
        for (int i = 0; i < rows; ++i) {
            // compute the indices of the internal loop
            NonNullRange r = NonNullRange.reunion(getRangeForRow(i),
                m.getRangeForRow(i));

            // assign the zeros before the non null range
            int j = 0;
            while (j < r.begin) {
                resultData[resultIndex] = 0.0;
                ++resultIndex;
                ++j;
                }

            // compute the possibly non null elements
            while (j < r.end) {

                // compute the current element
                resultData[resultIndex] = data[resultIndex] + m.data[resultIndex];

                // count the affected upper and lower elements
                // (in order to deduce the shape of the resulting matrix)
                if (j < i) {
                    ++lowerElements;
                    } else if (i < j) {
                    ++upperElements;
                    }

                ++resultIndex;
                ++j;

                }

            // assign the zeros after the non null range
            while (j < columns) {
                resultData[resultIndex++] = 0.0;
                ++resultIndex;
                ++j;
                }
            }

        return MatrixFactory.buildMatrix(rows, columns, resultData,
            lowerElements, upperElements);

        }

    /** Substract a matrix from the instance.
     * This method substracts a matrix from the instance. It returns a new
     * matrix and does not modify the instance.
     * @param m matrix to substract
     * @return a new matrix containing the result
     * @exception IllegalArgumentException if there is a dimension mismatch
     */
    public Matrix sub(Matrix m) {

        // validity check
        if ((rows != m.rows) || (columns != m.columns)) {
            throw new IllegalArgumentException("cannot substract a "
                + m.rows + 'x' + m.columns
                + " matrix from a "
                + rows + 'x' + columns
                + " matrix");
            }

        double[] resultData    = new double[rows * columns];
        int      resultIndex   = 0;
        int      lowerElements = 0;
        int      upperElements = 0;

        // external loop through the rows
        for (int i = 0; i < rows; ++i) {
            // compute the indices of the internal loop
            NonNullRange r = NonNullRange.reunion(getRangeForRow(i),
                m.getRangeForRow(i));

            // assign the zeros before the non null range
            int j = 0;
            while (j < r.begin) {
                resultData[resultIndex] = 0.0;
                ++resultIndex;
                ++j;
                }

            // compute the possibly non null elements
            while (j < r.end) {

                // compute the current element
                resultData[resultIndex] = data[resultIndex] - m.data[resultIndex];

                // count the affected upper and lower elements
                // (in order to deduce the shape of the resulting matrix)
                if (j < i) {
                    ++lowerElements;
                    } else if (i < j) {
                    ++upperElements;
                    }

                ++resultIndex;
                ++j;

                }

            // assign the zeros after the non null range
            while (j < columns) {
                resultData[resultIndex++] = 0.0;
                ++resultIndex;
                ++j;
                }
            }

        return MatrixFactory.buildMatrix(rows, columns, resultData,
            lowerElements, upperElements);

        }

    /** Multiply the instance by a matrix.
     * This method multiplies the instance by a matrix. It returns a new
     * matrix and does not modify the instance.
     * @param m matrix by which to multiply
     * @return a new matrix containing the result
     * @exception IllegalArgumentException if there is a dimension mismatch
     */
    public Matrix mul(Matrix m) {

        // validity check
        if (columns != m.rows) {
            throw new IllegalArgumentException("cannot multiply a "
                + rows + 'x' + columns
                + " matrix by a "
                + m.rows + 'x' + m.columns
                + " matrix");
            }

        double[] resultData = new double[rows * m.columns];
        int resultIndex     = 0;
        int lowerElements   = 0;
        int upperElements   = 0;

        for (int i = 0; i < rows; ++i) {
            for (int j = 0; j < m.columns; ++j) {
                double value = 0.0;

                // compute the tighter possible indices of the internal loop
                NonNullRange r = NonNullRange.intersection(getRangeForRow(i),
                    m.getRangeForColumn(j));

                if (r.begin < r.end) {
                    int k    = r.begin;
                    int idx  = i * columns + k;
                    int midx = k * m.columns + j;
                    while (k++ < r.end) {
                        value += data[idx++] * m.data[midx];
                        midx  += m.columns;
                        }

                    // count the affected upper and lower elements
                    // (in order to deduce the shape of the resulting matrix)
                    if (j < i) {
                        ++lowerElements;
                        } else if (i < j) {
                        ++upperElements;
                        }

                    }

                // store the element value
                resultData[resultIndex++] = value;

                }
            }

        return MatrixFactory.buildMatrix(rows, m.columns, resultData,
            lowerElements, upperElements);

        }

    /** Multiply the instance by a scalar.
     * This method multiplies the instance by a scalar. It returns a new
     * matrix and does not modify the instance.
     * @param a scalar by which to multiply
     * @return a new matrix containing the result
     * @see #selfMul(double)
     */
    public Matrix mul(double a) {
        Matrix copy = duplicate();
        copy.selfMul(a);
        return copy;
        }

    /** Multiply the instance by a scalar.
     * This method multiplies the instance by a scalar.
     * It does modify the instance.
     * @param a scalar by which to multiply
     * @see #mul(double)
     */
    public void selfMul(double a) {
        for (int i = 0; i < rows; ++i) {
            NonNullRange r = getRangeForRow(i);
            for (int j = r.begin, index = i * columns + r.begin; j < r.end; ++j) {
                data[index++] *= a;
                }
            }

        }

    /** Compute the transpose of the instance.
     * This method transposes the instance. It returns a new
     * matrix and does not modify the instance.
     * @return a new matrix containing the result
     */
    public Matrix getTranspose() {

        double[] resultData    = new double[columns * rows];
        int      resultIndex   = 0;
        int      upperElements = 0;
        int      lowerElements = 0;

        for (int i = 0; i < columns; ++i) {
            // compute the indices of the internal loop
            NonNullRange range = getRangeForColumn(i);

            int j     = 0;
            int index = i;

            // assign the zeros before the non null range
            while (j < range.begin) {
                resultData[resultIndex++] = 0.0;
                index += columns;
                ++j;
                }

            // compute the possibly non null elements
            while (j < range.end) {
                resultData[resultIndex] = data[index];

                // count the affected upper and lower elements
                // (in order to deduce the shape of the resulting matrix)
                if (j < i) {
                    ++lowerElements;
                    } else if (i < j) {
                    ++upperElements;
                    }

                index += columns;
                ++resultIndex;
                ++j;

                }

            // assign the zeros after the non null range
            while (j < rows) {
                resultData[resultIndex] = 0.0;
                ++resultIndex;
                ++j;
                }

            }

        return MatrixFactory.buildMatrix(columns, rows, resultData,
            lowerElements, upperElements);

        }

    /** Set a range to the non null part covered by a row.
     * @param i index of the row
     * @return range of non nul elements in the specified row
     * @see #getRangeForColumn
     */
    protected abstract NonNullRange getRangeForRow(int i);

    /** Set a range to the non null part covered by a column.
     * @param j index of the column
     * @return range of non nul elements in the specified column
     * @see #getRangeForRow
     */
    protected abstract NonNullRange getRangeForColumn(int j);

    public String toString() {
        String separator = System.getProperty("line.separator");

        StringBuffer buf = new StringBuffer();
        for (int index = 0; index < rows * columns; ++index) {
            if (index > 0) {
                if (index % columns == 0) {
                    buf.append(separator);
                    } else {
                    buf.append(' ');
                    }
                }
            buf.append(Double.toString(data[index]));
            }

        return buf.toString();

        }

    /** number of rows of the matrix. */
    protected final int rows;

    /** number of columns of the matrix. */
    protected final int columns;

    /** array of the matrix elements.
     * the elements are stored in a one dimensional array, row after row
     */
    protected final double[] data;

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
