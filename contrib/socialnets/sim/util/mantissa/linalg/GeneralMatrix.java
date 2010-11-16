package sim.util.mantissa.linalg;

import java.io.Serializable;

/** This class represents matrices of the most general type.

 * <p>This class is the basic implementation of matrices to use when
 * nothing special is known about the structure of the matrix.</p>

 <p>This file is from the "Mantissa" Java software package found at
 <a href="http://www.spaceroots.org/software/mantissa/index.html">
 http://www.spaceroots.org/software/mantissa/index.html</a>.  The license is included
 at the end of the source file.

 * @version $Id: GeneralMatrix.java,v 1.1 2007-05-30 14:01:29 feijai Exp $
 * @author L. Maisonobe

 */

public class GeneralMatrix
    extends Matrix
    implements Serializable {
    /** Simple constructor.
     * Build a matrix with null elements.
     * @param rows number of rows of the matrix
     * @param columns number of columns of the matrix
     */
    public GeneralMatrix(int rows, int columns) {
        super(rows, columns);
        }

    /** Simple constructor.
     * Build a matrix with specified elements.
     * @param rows number of rows of the matrix
     * @param columns number of columns of the matrix
     * @param data table of the matrix elements (stored row after row)
     */
    public GeneralMatrix(int rows, int columns, double[] data) {
        super(rows, columns, data);
        }

    /** Copy constructor.
     * @param m matrix to copy
     */
    public GeneralMatrix(Matrix m) {
        super(m);
        }

    public Matrix duplicate() {
        return new GeneralMatrix(this);
        }

    /** Add a matrix to the instance.
     * This method adds a matrix to the instance. It does modify the instance.
     * @param m matrix to add
     * @exception IllegalArgumentException if there is a dimension mismatch
     */
    public void selfAdd(Matrix m) {

        // validity check
        if ((rows != m.rows) || (columns != m.columns)) {
            throw new IllegalArgumentException("cannot add a "
                + m.rows + 'x' + m.columns
                + " matrix to a "
                + rows + 'x' + columns
                + " matrix");
            }

        // addition loop
        for (int index = 0; index < rows * columns; ++index) {
            data[index] += m.data[index];
            }

        }

    /** Substract a matrix from the instance.
     * This method substracts a matrix from the instance. It does modify the instance.
     * @param m matrix to substract
     * @exception IllegalArgumentException if there is a dimension mismatch
     */
    public void selfSub(Matrix m) {

        // validity check
        if ((rows != m.rows) || (columns != m.columns)) {
            throw new IllegalArgumentException("cannot substract a "
                + m.rows + 'x' + m.columns
                + " matrix from a "
                + rows + 'x' + columns
                + " matrix");
            }

        // substraction loop
        for (int index = 0; index < rows * columns; ++index) {
            data[index] -= m.data[index];
            }

        }

    protected NonNullRange getRangeForRow(int i) {
        return new NonNullRange(0, columns);
        }

    protected NonNullRange getRangeForColumn(int j) {
        return new NonNullRange(0, rows);
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
