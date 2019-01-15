package sim.app.geo.masoncsc.util;

import ec.util.MersenneTwisterFast;
import sim.engine.SimState;
import sim.engine.Steppable;
import sim.engine.Stoppable;
import sim.util.distribution.Poisson;

/**
 * Event generator for which the time between events is drawn from the Poisson 
 * distribution. To use this class, override eventGenerated(), possibly by
 * declaring an anonymous class.
 * <p>
 * Instead of adding this to the scheduler manually, just call start() from
 * your SimState's start and it will schedule itself. You can disable this 
 * event generator by setting eventsPerStep to 0.
 * <p>
 * Example:
 * <pre><tt>
 * PoissonEventGenerator gen = new PoissonEventGenerator(0.1, random) {
 * 	public void eventGenerated() {
 * 		// do your event here
 *	}
 * };
 *
 * gen.start(this); // call this from your SimState's start function
 * </tt></pre>
 * 
 * @author Joey Harrison
 *
 */
public class PoissonEventGeneratorInverted implements Steppable
{	
	private static final long serialVersionUID = 1L;
	
	/** Average time between events (i.e. 1/eventsPerStep), often called "lambda".
	 * This can be set directly by calling setTimeBetweenEvents() or indirectly
	 * by calling setEventsPerStep().
	 */
	private double timeBetweenEvents = 1.0;	
	public double getTimeBetweenEvents() { return timeBetweenEvents; }
	/** 
	 * Set the average time between events. This automatically sets 
	 * eventsPerStep to 1/timeBetweenEvents.
	 */
	public void setTimeBetweenEvents(double val) {
		timeBetweenEvents = val;
		eventsPerStep = 1 / timeBetweenEvents;
	}

	/**
	 * Average number of events generated per step (i.e. 1/timeBetweenEvents).
	 * This can be set directly by calling setEventsPerStep() or indirectly
	 * by calling setTimeBetweenEvents().
	 */
	private double eventsPerStep = 1.0;
	public double getEventsPerStep() { return 1 / timeBetweenEvents; }	
	/**
	 * Set the average number of events per step. This automatically
	 * sets timeBetweenEvents to 1/eventsPerStep.
	 */
	public void setEventsPerStep(double val) {
		eventsPerStep = val;
		timeBetweenEvents = 1 / eventsPerStep;
	}	
	
	/** Poisson number generator. */
	private Poisson poissonDist;
	
	Stoppable stoppable = null;
		
	public PoissonEventGeneratorInverted(double eventsPerStep, MersenneTwisterFast random) {
		setEventsPerStep(eventsPerStep);
		poissonDist = new Poisson(timeBetweenEvents, random);
	}	

	/**
	 * Start the event generator. Call this from your SimState's start function.
	 */
	public void start(SimState state) {
//		if (eventsPerStep <= 0)
//			return;
//
//		int timeUntilNext = poissonDist.nextInt(timeBetweenEvents);
//		state.schedule.scheduleOnce(timeUntilNext, this);	// scheduleOnceIn(0, this) breaks if you call it at start
		
//		if (stoppable != null)
//			stoppable.stop();

//		if (eventsPerStep <= 0)
//			return;
		if (Double.isInfinite(timeBetweenEvents))
			return;
		
		double currentTime = Math.max(0, state.schedule.getTime());

		int timeUntilNext = poissonDist.nextInt(timeBetweenEvents);

		System.out.format("eventGen.start() time=%f\n", currentTime + timeUntilNext);
		state.schedule.scheduleOnce(currentTime + timeUntilNext, this);
	}
	
	/**
	 * Restart the event generator. Call this from anywhere as long as your simulation is running.
	 */
//	public void restart(SimState state) {
////		state.schedule.
//		if (eventsPerStep <= 0)
//			return;		
//
//		int timeUntilNext = poissonDist.nextInt(timeBetweenEvents);
//		state.schedule.scheduleOnce(timeUntilNext, this); // scheduleOnceIn(0, this) breaks if you call it at start
//	}

	/**
	 * Generate an event and continue generating events as long as we keep
	 * drawing timeUntilNext==0 from the Poisson distribution. When a non-zero
	 * is drawn, schedule the next event at that time.
	 */
	@Override
	public void step(SimState state) {
//		if (eventsPerStep <= 0)
//			return;
		if (Double.isInfinite(timeBetweenEvents))
			return;
		
		int timeUntilNext = 0;
		while (timeUntilNext == 0) {
			eventGenerated();
			timeUntilNext = poissonDist.nextInt(timeBetweenEvents);
		}				
		
		state.schedule.scheduleOnceIn(timeUntilNext, this);
	}
	
	//////////// Prototyping a new event generator that can handle rate changes on the fly

	int timeUntilNextEvent=0;
	public void step_new(SimState state) {
		if (--timeUntilNextEvent > 0)
			return;
		
		while (timeUntilNextEvent == 0) {
			eventGenerated();
			timeUntilNextEvent = poissonDist.nextInt(timeBetweenEvents);
		}
	}
	
	public void init() {
		timeUntilNextEvent = poissonDist.nextInt(timeBetweenEvents);
	}
	
	/** Override this to handle events. */
	public void eventGenerated() {}

}
