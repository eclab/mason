/*
  Copyright � 1999 CERN - European Organization for Nuclear Research.
  Permission to use, copy, modify, distribute and sell this software and its documentation for any purpose 
  is hereby granted without fee, provided that the above copyright notice appear in all copies and 
  that both that copyright notice and this permission notice appear in supporting documentation. 
  CERN makes no representations about the suitability of this software for any purpose. 
  It is provided "as is" without expressed or implied warranty.
*/
package sim.util.distribution;
import ec.util.MersenneTwisterFast;

/**
 * Mean-square BreitWigner distribution; See the <A HREF="http://www.cern.ch/RD11/rkb/AN16pp/node23.html#SECTION000230000000000000000"> math definition</A>.
 * <p>
 * Instance methods operate on a user supplied uniform random number generator; they are unsynchronized.
 * <dt>
 * Static methods operate on a default uniform random number generator; they are synchronized. 
 * <p>
 * <b>Implementation:</b> This is a port of <A HREF="http://wwwinfo.cern.ch/asd/lhc++/clhep/manual/RefGuide/Random/RandBreitWigner.html">RandBreitWigner</A> used in <A HREF="http://wwwinfo.cern.ch/asd/lhc++/clhep">CLHEP 1.4.0</A> (C++).
 *
 * @author wolfgang.hoschek@cern.ch
 * @version 1.0, 09/24/99
 */
public class BreitWignerMeanSquare extends BreitWigner {
    private static final long serialVersionUID = 1;

    protected Uniform uniform; // helper
        
/**
 * Constructs a mean-squared BreitWigner distribution.
 * @param cut </tt>cut==Double.NEGATIVE_INFINITY</tt> indicates "don't cut".
 */
    public BreitWignerMeanSquare(double mean, double gamma, double cut, MersenneTwisterFast randomGenerator) {
        super(mean,gamma,cut,randomGenerator);
        this.uniform = new Uniform(randomGenerator);
        }
/*
 * Returns a deep copy of the receiver; the copy will produce identical sequences.
 * After this call has returned, the copy and the receiver have equal but separate state.
 *
 * @return a copy of the receiver.
 */
    /*
      public Object clone() {
      BreitWignerMeanSquare copy = (BreitWignerMeanSquare) super.clone();
      if (this.uniform != null) copy.uniform = new Uniform(copy.randomGenerator);
      return copy;
      }
    */
/**
 * Returns a mean-squared random number from the distribution; bypasses the internal state.
 * @param cut </tt>cut==Double.NEGATIVE_INFINITY</tt> indicates "don't cut".
 */
    public double nextDouble(double mean,double gamma,double cut) {
        if (gamma == 0.0) return mean;
        if (cut==Double.NEGATIVE_INFINITY) { // don't cut
            double val = Math.atan(-mean/gamma);
            double rval = this.uniform.nextDoubleFromTo(val, Math.PI/2.0);
            double displ = gamma*Math.tan(rval);
            return Math.sqrt(mean*mean + mean*displ);
            }
        else {
            double tmp = Math.max(0.0,mean-cut);
            double lower = Math.atan( (tmp*tmp-mean*mean)/(mean*gamma) );
            double upper = Math.atan( ((mean+cut)*(mean+cut)-mean*mean)/(mean*gamma) );
            double rval = this.uniform.nextDoubleFromTo(lower, upper);

            double displ = gamma*Math.tan(rval);
            return Math.sqrt(Math.max(0.0, mean*mean + mean*displ));
            }
        }
    }
