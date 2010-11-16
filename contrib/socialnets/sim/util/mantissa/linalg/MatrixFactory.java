package sim.util.mantissa.linalg;

/** This class is a factory for the linear algebra package.

 * <p>This class is devoted to building the right type of matrix
 * according to the structure of the non null elements.</p>

 * <p>This is a utility class, no instance of this class should be
 * built, so the constructor is explicitly made private.</p>

 <p>This file is from the "Mantissa" Java software package found at
 <a href="http://www.spaceroots.org/software/mantissa/index.html">
 http://www.spaceroots.org/software/mantissa/index.html</a>.  The license is included
 at the end of the source file.

 * @version $Id: MatrixFactory.java,v 1.1 2007-05-30 14:01:30 feijai Exp $
 * @author L. Maisonobe

 */

public class MatrixFactory {
    /** Simple constructor.
     * Since the class is a utility class with only static methods, the
     * constructor is made private to prevent creating instances of this
     * class.
     */
    private MatrixFactory() {
        }

    /** Build a matrix of the right subtype.
     * Build the right subtype of matrix according to the structure of
     * the non null elements of the instance. Note that the information
     * provided does not allow to build instances of the {@link
     * SymetricalMatrix} class. When the data corresponding to a
     * symetrical matrix is given, this method can only build an
     * instance of the {@link GeneralSquareMatrix} class.
     * @param rows number of row of the matrix
     * @param columns number of columns of the matrix
     * @param data table of the matrix elements (stored row after row)
     * @param lowerElements number of non null elements in the lower triangle
     * @param upperElements number of non null elements in the upper triangle
     * @return a matrix containing the instance
     */
    public static Matrix buildMatrix(int rows, int columns, double[] data,
        int lowerElements, int upperElements) {
        if (rows == columns) {
            if (lowerElements == 0 && upperElements == 0) {
                return new DiagonalMatrix(rows, data);
                } else if (lowerElements == 0) {
                return new UpperTriangularMatrix(rows, data);
                } else if (upperElements == 0) {
                return new LowerTriangularMatrix(rows, data);
                } else {
                return new GeneralSquareMatrix(rows, data);
                }
            } else {
            return new GeneralMatrix(rows, columns, data);
            }
        }

    /** Build a matrix of the right subtype.
     * Build the right subtype of matrix according to the dimensions.
     * @param rows number of row of the matrix
     * @param columns number of columns of the matrix
     * @param data table of the matrix elements (stored row after row)
     * @return a matrix containing the instance
     */
    public static Matrix buildMatrix(int rows, int columns, double[] data) {
        if (rows == columns) {
            return new GeneralSquareMatrix(rows, data);
            } else {
            return new GeneralMatrix(rows, columns, data);
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
