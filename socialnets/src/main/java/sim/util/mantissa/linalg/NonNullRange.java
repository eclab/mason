package sim.util.mantissa.linalg;

import java.io.Serializable;

/** This class represents range of non null elements for rows or
 * columns of matrices.

 * <p>This class is used to reduce the computation loops by avoiding
 * using elements that are known to be zeros. For full matrices, the
 * range simply spans from 0 to the order of the matrix. For lower and
 * upper triangular matrices, its width will depend on the index of
 * the row or column considered. For diagonal matrices, the range is
 * reduced to one index.</p>

 * <p>The indices provided by the class correspond to the elements
 * that are non-null only according to the <emph>structure</emph> of
 * the matrix. The real value of the element is not
 * considered. Consider for example the following lower triangular
 * matrix :</p>

 * <pre>
 *   1 0 0 0
 *   2 8 0 0
 *   0 5 3 0
 *   3 2 4 4
 * </pre>

 * <p>The third rows begins with zero, but this is not a consequence
 * of the lower triangular structure, it is only a
 * coincidence. Therefore, the range (in row/columns count)
 * corresponding to third row will span from 0 to 2, not from 1 to 2.</p>

 <p>This file is from the "Mantissa" Java software package found at
 <a href="http://www.spaceroots.org/software/mantissa/index.html">
 http://www.spaceroots.org/software/mantissa/index.html</a>.  The license is included
 at the end of the source file.

 * @version $Id: NonNullRange.java,v 1.1 2007-05-30 14:01:30 feijai Exp $
 * @author L. Maisonobe

 */
class NonNullRange
    implements Serializable, Cloneable {

    /** Index in row/column count of the first non-null element. */
    public final int begin;

    /** Index in row/column count after the last non-null element. */
    public final int end;

    /** Simple constructor.
     * @param begin index in row/column count of the first non-null element
     * @param end index in row/column count after the last non-null element
     */
    public NonNullRange(int begin, int end)
        {
        this.begin = begin;
        this.end   = end;
        }

    /** Copy constructor.
     * @param range range to copy.
     */
    public NonNullRange(NonNullRange range) {
        begin = range.begin;
        end   = range.end;
        }

    /** Build the intersection of two ranges.
     * @param first first range to consider
     * @param second second range to consider
     */
    public static NonNullRange intersection(NonNullRange first,
        NonNullRange second) {
        return new NonNullRange(Math.max(first.begin, second.begin),
            Math.min(first.end, second.end));
        }

    /** Build the reunion of two ranges.
     * @param first first range to consider
     * @param second second range to consider
     */
    public static NonNullRange reunion(NonNullRange first,
        NonNullRange second) {
        return new NonNullRange(Math.min(first.begin, second.begin),
            Math.max(first.end, second.end));
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
