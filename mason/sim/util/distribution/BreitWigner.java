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
 * BreitWigner (aka Lorentz) distribution; See the <A HREF="http://www.cern.ch/RD11/rkb/AN16pp/node23.html#SECTION000230000000000000000"> math definition</A>.
 * A more general form of the Cauchy distribution.
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
public class BreitWigner extends AbstractContinousDistribution {
    private static final long serialVersionUID = 1;

    protected double mean;
    protected double gamma;
    protected double cut;

/**
 * Constructs a BreitWigner distribution.
 * @param cut </tt>cut==Double.NEGATIVE_INFINITY</tt> indicates "don't cut".
 */
    public BreitWigner(double mean, double gamma, double cut, MersenneTwisterFast randomGenerator) {
        setRandomGenerator(randomGenerator);
        setState(mean, gamma, cut);
        }
/**
 * Returns a random number from the distribution.
 */
    public double nextDouble() {
        return nextDouble(mean, gamma, cut);
        }
/**
 * Returns a random number from the distribution; bypasses the internal state.
 * @param cut </tt>cut==Double.NEGATIVE_INFINITY</tt> indicates "don't cut".
 */
    public double nextDouble(double mean,double gamma,double cut) {
        double val, rval, displ;

        if (gamma == 0.0) return mean;
        if (cut==Double.NEGATIVE_INFINITY) { // don't cut
            rval = 2.0*randomGenerator.nextDouble()-1.0;
            displ = 0.5*gamma*Math.tan(rval*(Math.PI/2.0));
            return mean + displ;
            }
        else {
            val = Math.atan(2.0*cut/gamma);
            rval = 2.0*randomGenerator.nextDouble()-1.0;
            displ = 0.5*gamma*Math.tan(rval*val);

            return mean + displ;
            }
        }
/**
 * Sets the mean, gamma and cut parameters.
 * @param cut </tt>cut==Double.NEGATIVE_INFINITY</tt> indicates "don't cut".
 */
    public void setState(double mean, double gamma, double cut) {
        this.mean = mean;
        this.gamma = gamma;
        this.cut = cut;
        }
/**
 * Returns a String representation of the receiver.
 */
    public String toString() {
        return this.getClass().getName()+"("+mean+","+gamma+","+cut+")";
        }
    }
