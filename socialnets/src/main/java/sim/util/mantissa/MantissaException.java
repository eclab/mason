package sim.util.mantissa;

/** This class is the base class for all specific exceptions thrown by
 * the mantissa classes.

 * <p>When the mantissa classes throw exceptions that are specific to
 * the package, these exceptions are always subclasses of
 * MantissaException. When exceptions that are already covered by the
 * standard java API should be thrown, like
 * ArrayIndexOutOfBoundsException or IllegalArgumentException, these
 * standard exceptions are thrown rather than the mantissa specific
 * ones.</p>

 <p>This file is from the "Mantissa" Java software package found at
 <a href="http://www.spaceroots.org/software/mantissa/index.html">
 http://www.spaceroots.org/software/mantissa/index.html</a>.  The license is included
 at the end of the source file.

 * @version $Id: MantissaException.java,v 1.1 2007-05-30 14:01:29 feijai Exp $
 * @author L. Maisonobe

 */

public class MantissaException
    extends Exception {

    /** Simple constructor.
     * Build an exception with a default message
     */
    public MantissaException() {
        super("mantissa exception");
        }

    /** Simple constructor.
     * Build an exception with the specified message
     */
    public MantissaException(String message) {
        super(message);
        }

    /** Simple constructor.
     * Build an exception from a cause
     * @param cause cause of this exception
     */
    public MantissaException(Throwable cause) {
        super(cause);
        }

    /** Simple constructor.
     * Build an exception from a message and a cause
     * @param message exception message
     * @param cause cause of this exception
     */
    public MantissaException(String message, Throwable cause) {
        super(message, cause);
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
