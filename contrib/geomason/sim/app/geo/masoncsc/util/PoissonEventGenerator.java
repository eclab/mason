package masoncsc.util;

import ec.util.MersenneTwisterFast;
import sim.engine.SimState;
import sim.engine.Steppable;
import sim.util.distribution.Poisson;

/**
 * Event generator for which the number of events per step is drawn from the Poisson 
 * distribution. To use this class, override eventGenerated(), possibly by
 * declaring an anonymous class.
 * <p>
 * To use this class, add it to the schedule with scheduleRepeating.
 * You can disable the event generator by setting eventsPerStep to 0.
 * <p>
 * Example:
 * <pre><tt>
 * PoissonEventGenerator gen = new PoissonEventGenerator(0.1, random) {
 * 	public void eventGenerated() {
 * 		// do your event here
 *	}
 * };
 * </tt></pre>
 * 
 * @author Joey Harrison
 *
 */
public class PoissonEventGenerator implements Steppable
{	
	private static final long serialVersionUID = 1L;

	/**
	 * Average number of events per step, also known as lambda.
	 */
	private double eventsPerStep = 1.0;	
	public double getEventsPerStep() { return eventsPerStep; }
	public void setEventsPerStep(double val) { eventsPerStep = val;	}
	
	/** Poisson number generator. */
	public Poisson poissonDist; 
	
	/**
	 * Construct a poisson event generator. 
	 * 
	 * @param eventsPerStep Average number of events per timestep.
	 * @param random Random number generator to be used by the Poisson generator.
	 */
	public PoissonEventGenerator(double eventsPerStep, MersenneTwisterFast random) {
		this.eventsPerStep = eventsPerStep;
		poissonDist = new Poisson(eventsPerStep, random); 
	}	
	
//	/**
//	 * This calculates the continuous time between events. Not used in this class,
//	 * but would be useful in an event generator that generates events on non-integer
//	 * times.
//	 * @return
//	 */
//	public double calcTimeBetweenEvents() {
//		return -Math.log(1 - random.nextDouble()) / eventsPerStep;
//	}

	/**
	 * If it's time to generate an event (i.e. timeUntilNextEvent == 0),
	 * generate an event and continue generating events as long as we keep
	 * drawing timeUntilNextEvent == 0 from the Poisson distribution. 
	 * 
	 * When step is called for the first time (i.e. step 0), it initializes 
	 * timeUntilNextEvent.
	 */
	@Override
	public void step(SimState state) {
		if (eventsPerStep <= 0)
			return;
		
		int numEvents = poissonDist.nextInt(eventsPerStep);
		for (int i = 0; i < numEvents; i++)
			eventGenerated();
	}

	/** Override this to handle events. */
	public void eventGenerated() {}

}
