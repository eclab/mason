package sim.util.mantissa.linalg;

import java.io.Serializable;

/** This class implements symetrical matrices of linear algebra.

    <p>This file is from the "Mantissa" Java software package found at
    <a href="http://www.spaceroots.org/software/mantissa/index.html">
    http://www.spaceroots.org/software/mantissa/index.html</a>.  The license is included
    at the end of the source file.

    * @version $Id: SymetricalMatrix.java,v 1.1 2007-05-30 14:01:30 feijai Exp $
    * @author L. Maisonobe

    */

public class SymetricalMatrix
    extends GeneralSquareMatrix
    implements Serializable, Cloneable {
    /** Simple constructor.
     * This constructor builds a symetrical matrix of specified order, all
     * elements beeing zeros.
     * @param order order of the matrix
     */
    public SymetricalMatrix(int order) {
        super(order);
        }

    /** Simple constructor.
     * Build a matrix with specified elements.
     * @param order order of the matrix
     * @param data table of the matrix elements (stored row after row)
     */
    public SymetricalMatrix(int order, double[] data) {
        super(order, data);
        }

    /** Copy constructor.
     * @param s square matrix to copy
     */
    public SymetricalMatrix(SymetricalMatrix s) {
        super(s);
        }

    /** Build the symetrical matrix resulting from the product w.A.At.
     * @param w multiplicative factor (weight)
     * @param a base vector used to compute the symetrical contribution
     */
    public SymetricalMatrix(double w, double[] a) {
        super(a.length, new double[a.length * a.length]);

        for (int i = 0; i < a.length; ++i) {
            int indexU = i * (columns + 1);
            int indexL = indexU;

            double factor = w * a[i];
            data[indexU] = factor * a[i];

            for (int j = i + 1; j < columns; ++j) {
                ++indexU;
                indexL += columns;
                data[indexU] = factor * a[j];
                data[indexL] = data[indexU];
                }
            }

        }

    public Matrix duplicate() {
        return new SymetricalMatrix(this);
        }

    /** Set a matrix element.
     * On symetrical matrices, setting separately elements outside of
     * the diagonal is forbidden, so this method throws an
     * ArrayIndexOutOfBoundsException in this case. The {@link
     * #setElementAndSymetricalElement} can be used to set both elements
     * simultaneously.
     * @param i row index, from 0 to rows - 1
     * @param j column index, from 0 to cols - 1
     * @param value value of the element
     * @exception ArrayIndexOutOfBoundsException if the indices are wrong
     * @see #setElementAndSymetricalElement
     * @see #getElement
     */
    public void setElement(int i, int j, double value) {
        if (i != j) {
            throw new ArrayIndexOutOfBoundsException("cannot separately set"
                + " elements out of diagonal"
                + " in a symetrical matrix");
            }
        super.setElement(i, j, value);
        }

    /** Set both a matrix element and its symetrical element.
     * @param i row index of first element (column index of second
     * element), from 0 to order - 1
     * @param j column index of first element (row index of second
     * element), from 0 to order - 1
     * @param value value of the elements
     * @exception ArrayIndexOutOfBoundsException if the indices are wrong
     * @see #setElement
     * @see #getElement
     */
    public void setElementAndSymetricalElement(int i, int j, double value) {
        super.setElement(i, j, value);
        if (i != j) {
            super.setElement(j, i, value);
            }
        }

    /** Add a matrix to the instance.
     * This method adds a matrix to the instance. It does modify the instance.
     * @param s symetrical matrix to add
     * @exception IllegalArgumentException if there is a dimension mismatch
     */
    public void selfAdd(SymetricalMatrix s) {

        // validity check
        if ((rows != s.rows) || (columns != s.columns)) {
            throw new IllegalArgumentException("cannot add a "
                + s.rows + 'x' + s.columns
                + " matrix to a "
                + rows + 'x' + columns
                + " matrix");
            }

        // addition loop
        for (int i = 0; i < rows; ++i) {
            int indexU = i * (columns + 1);
            int indexL = indexU;

            data[indexU] += s.data[indexU];

            for (int j = i + 1; j < columns; ++j) {
                ++indexU;
                indexL += columns;
                data[indexU] += s.data[indexU];
                data[indexL]  = data[indexU];
                }
            }

        }

    /** Substract a matrix from the instance.
     * This method substracts a matrix from the instance. It does modify the instance.
     * @param s symetrical matrix to substract
     * @exception IllegalArgumentException if there is a dimension mismatch
     */
    public void selfSub(SymetricalMatrix s) {

        // validity check
        if ((rows != s.rows) || (columns != s.columns)) {
            throw new IllegalArgumentException("cannot substract a "
                + s.rows + 'x' + s.columns
                + " matrix from a "
                + rows + 'x' + columns
                + " matrix");
            }

        // substraction loop
        for (int i = 0; i < rows; ++i) {
            int indexU = i * (columns + 1);
            int indexL = indexU;

            data[indexU] -= s.data[indexU];

            for (int j = i + 1; j < columns; ++j) {
                ++indexU;
                indexL += columns;
                data[indexU] -= s.data[indexU];
                data[indexL] = data[indexU];
                }
            }

        }

    /** Add the symetrical matrix resulting from the product w.A.At to the instance.
     * This method can be used to build progressively the matrices of
     * least square problems. The instance is modified.
     * @param w multiplicative factor (weight)
     * @param a base vector used to compute the symetrical contribution
     * @exception IllegalArgumentException if there is a dimension mismatch
     */
    public void selfAddWAAt(double w, double[] a) {
        if (rows != a.length) {
            throw new IllegalArgumentException("cannot add a "
                + a.length + 'x' + a.length
                + " matrix to a "
                + rows + 'x' + columns
                + " matrix");
            }

        for (int i = 0; i < rows; ++i) {
            int indexU = i * (columns + 1);
            int indexL = indexU;

            double factor   = w * a[i];
            data[indexU] += factor * a[i];

            for (int j = i + 1; j < columns; ++j) {
                ++indexU;
                indexL += columns;
                data[indexU] += factor * a[j];
                data[indexL]  = data[indexU];
                }
            }

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
