/*
 * Bee foraging simulation. Copyright by Joerg Hoehne.
 * For suggestions or questions email me at hoehne@thinktel.de
 */

package foragingBee;

import java.awt.Color;
import java.awt.Graphics2D;

import javax.vecmath.Point3d;
import javax.vecmath.Vector3d;

import masonGlue.ForagingHoneyBeeSimulation;
import sim.engine.SimState;
import sim.portrayal.DrawInfo2D;
import utils.Filter;
import utils.Geometric;

/**
 * This class defines the behaviour of the bee inside this simulation. The bee
 * is modeled as a state based automaton that performs certain action according
 * to its state.<br>
 * The class implements the basic methods {@link #iterate()} that will perform
 * the state specific code during execution of the object.
 * <p>
 * Copyright 2009 Joerg Hoehne
 * 
 * @author hoehne (<a href="mailto:hoehne@thinktel.de">J&ouml;rg H&ouml;hne</a>)
 * 
 */
public class Bee extends AbstractMovingAgent {
	/**
	 * The states the bee can cycle through.
	 */
	public enum State {
		initialState, inHiveWithoutInfo, inHiveWithInfo, leaveHive, danceFollowing, dancing, foraging, searching, returnWithInfo, returnWithInfoAndLoad, unloadQueue, scouting, returnWithoutInfo, dead, terminated
	}

	/**
	 * A constant used for computing a mask for checking if to avoid the current
	 * target.
	 */
	static final int valueCurrentTarget = (1 << 0);
	/**
	 * A constant used for computing a mask for checking if to avoid obstacles.
	 */
	static final int valueObstacle = (1 << 1);
	/**
	 * A constant used for computing a mask for checking if to avoid other bees.
	 */
	static final int valueBee = (1 << 2);
	/**
	 * A constant used for computing a mask for checking if to avoid the hive.
	 */
	static final int valueHive = (1 << 3);
	/**
	 * A constant used for computing a mask for checking if to avoid the
	 * entrance of the hive.
	 */
	static final int valueEntrance = (1 << 4);
	/**
	 * A constant used for computing a mask for checking if to avoid food
	 * sources.
	 */
	static final int valueFoodSource = (1 << 5);

	/**
	 * The amount of nectar a bee needs for one step in the outside in [µl].
	 */
	static private final double nectarForOneStep = 1 / 80;

	/**
	 * The maximum load the bee can carry in [µl].
	 */
	static private final double nectarStomach = 50;

	/**
	 * The conversion factor from nectar to honey. Approximately 2.5 units of
	 * nectar will result in one unit honey.
	 */
	static private final double honeyShrinkFactor = .4;

	/**
	 * The conversion factor from honey to nectar. One unit of honey will result
	 * in approximately. 2.5 units of nectar.
	 */
	static private final double honeyExpandFactor = 1 / honeyShrinkFactor;

	/**
	 * This constant defines the amount of honey a bee needs for return.
	 */
	static private final double nectarForReturn = ((ForagingHoneyBeeSimulation.WIDTH
			+ ForagingHoneyBeeSimulation.HEIGHT + ForagingHoneyBeeSimulation.LENGTH) / 3 - ForagingHoneyBeeSimulation.HIVE_DIAMETER)
			* nectarForOneStep;

	/**
	 * The hive where the bee belongs to.
	 */
	Hive hive;

	/**
	 * The source of food the bee remembers, either by information or by the
	 * last foraging trip.
	 */
	FoodSource foodSource;

	/**
	 * The concentration of nectar for the last remembered source.
	 */
	double sourceConcentration;

	/**
	 * The location of the last source.
	 */
	Point3d sourceLocation;

	/**
	 * The measured distance from the source to the hives entrance in steps.
	 */
	int sourceDistance;

	/**
	 * The calculated direction from the hives entrance to the source.
	 */
	double sourceDirection;

	/**
	 * The current nectar load retrieved from a source.
	 */
	double nectarLoad;

	/**
	 * The computed quality of the last source.
	 */
	double sourceQuality;

	/**
	 * The costs for doing foraging (the last trip).
	 */
	double foragingCosts;

	/**
	 * The current state of the bee.
	 */
	private State state;

	/**
	 * True if the bee is receptive to other bees, used to listen to other
	 * dancing bees.
	 */
	boolean receptive;

	/**
	 * A counter for the statistics how often a bee is successive dancing.
	 */
	int repeatedDance;

	/**
	 * The probability a bee will start dancing. This value also affects the
	 * duration of dancing. Every step the probability the value is reduced.
	 * Distinctive values represent certain states:
	 * <ul>
	 * <li>-1: no dancing</li>
	 * <li>[0,1[: no dancing, set this value to -1</li>
	 * <li>>=1: dancing</li>
	 * </ul>
	 */
	double dancingThreshold;

	/**
	 * The number of cycles the bee invests in dancing.
	 */
	int dancingTime;

	/**
	 * The number of successive trips to the same food source.
	 */
	int repeatedTrip;

	/**
	 * The constructor for a bee. A bee belongs to a hive and has a current
	 * position.
	 * 
	 * @param simulation
	 *            The simulation this bee belongs to.
	 * @param hive
	 *            The hive the bee belongs to.
	 * @param location
	 *            The current position of the bee.
	 */
	public Bee(ForagingHoneyBeeSimulation simulation, Hive hive,
			Point3d location) {
		super(simulation, location, new Vector3d(), 2, Color.black);

		// the hive (home) of this bee
		this.hive = hive;

		Vector3d v = new Vector3d(1 - r.nextDouble() * 2,
				1 - r.nextDouble() * 2, 0);
		if (v.length() != 0)
			v.scale(1 / v.length());
		else
			v.set(1.0d, 1.0d, 0);

		setVelocityVector(v);

		setColor(Color.red);
		setState(State.inHiveWithoutInfo);
	}

	/**
	 * Conversion from honey to nectar.
	 * 
	 * @param honey
	 *            The amount of honey to be converted.
	 * @return The converted nectar equivalent.
	 */
	static final double honeyToNectar(double honey) {
		return honey / honeyExpandFactor;
	}

	/**
	 * Conversion from nectar to honey.
	 * 
	 * @param nectar
	 *            The amount of nectar to be converted.
	 * @return The converted honey equivalent.
	 */
	static final double nectarToHoney(double nectar) {
		return nectar * honeyShrinkFactor;
	}

	/**
	 * Retrieve some nectar equivalent from the hive. If the request is greater
	 * than the stomach of the bee ({@link #nectarStomach}) then it is limited
	 * to this value. If the hive has less equivalent available only this amount
	 * is returned.
	 * 
	 * @param nectar
	 *            The amount of nectar requested.
	 * @return The amount of nectar equivalent the hive can provide
	 */
	private double requestNectarFromHive(double nectar) {
		nectar = Math.min(nectar, nectarStomach);
		// calculate the equivalent of honey
		double honey = nectarToHoney(nectar);
		// try to get the honey from the hive if available
		honey = hive.getHoney(honey);
		// recompute the nectar load
		nectar = honeyToNectar(honey);

		return nectar;
	}

	/**
	 * The overwritten method for the MASON interface for a single step for this
	 * object during simulation. This method calls {@link #iterate()}.
	 * 
	 * @param state
	 *            The current simulation.
	 */
	final public void step(SimState state) {
		iterate();
	}

	/**
	 * The drawing of the object, called by MASON. A bee is drawn as a rectangle
	 * to save execution (display) time. Drawing rectangles is much more faster
	 * than drawing circles.
	 * 
	 * @param object
	 *            The object itself.
	 * @param graphics
	 *            Where (output device) to draw.
	 * @param info
	 *            Where (location) to draw.
	 */
	public final void draw(Object object, Graphics2D graphics, DrawInfo2D info) {
		double diamx = info.draw.width * 2;
		double diamy = info.draw.height * 2;

		graphics.setColor(getColor());

		graphics.fillRect((int) (info.draw.x - diamx / 2),
				(int) (info.draw.y - diamy / 2), (int) (diamx), (int) (diamy));
		// graphics.fillOval((int) (info.draw.x - diamx / 2),
		// (int) (info.draw.y - diamy / 2), (int) (diamx), (int) (diamy));
	}

	/**
	 * The state specific iteration of the bee. This method will call the code
	 * according to the current state.
	 */
	public void iterate() {
		State state = getState();

		switch (state) {
		case initialState:
			System.err.println("State " + state + " is not implemented.");
			System.exit(5);
			break;
		case inHiveWithoutInfo:
			doStateInHiveWithoutInfo();
			break;
		case inHiveWithInfo:
			doStateInHiveWithInfo();
			break;
		case leaveHive:
			doStateLeaveHive();
			break;
		case danceFollowing:
			System.err.println("State " + state + " is not implemented.");
			System.exit(5);
			break;
		case dancing:
			doStateDancing();
			break;
		case foraging:
			doStateForaging();
			break;
		case searching:
			doStateSearching();
			break;
		case returnWithInfo:
			System.err.println("State " + state + " is not implemented.");
			System.exit(5);
			break;
		case returnWithInfoAndLoad:
			doStateReturnWithInfoAndLoad();
			break;
		case unloadQueue:
			doStateUnloadQueue();
			break;
		case scouting:
			doStateScouting();
			break;
		case returnWithoutInfo:
			doStateReturnWithoutInfo();
			break;
		case dead:
			doStateDead();
			break;
		case terminated:
			doStateTerminated();
			break;
		default:
			System.err.println("iterate(): State " + state
					+ " is not implemented.");
			System.exit(5);
			break;
		}
	}

	/**
	 * The bee is in mode in hive without any information about a food source.
	 * With a given probability the bee starts scouting or otherwise does a step
	 * inside the hive by invoking method {@link #goInHive()} that will check if
	 * the bee has contact to a dancing bee.
	 */
	protected void doStateInHiveWithoutInfo() {
		setColor(Color.yellow);
		receptive = true;
		double colonyNectarNeed = getSimulation().colonyNectarNeed;
		double pStartScouting = getSimulation().pStartScouting;

		if ((pStartScouting * colonyNectarNeed) >= r.nextDouble()) {
			forgetSource(null, State.leaveHive);

			double nectar = nectarForReturn * 3 + r.nextInt(10);
			nectarLoad = requestNectarFromHive(nectar);

			repeatedTrip = 0;
			receptive = false;
			foragingCosts = 0;
		}

		goInHive();
	}

	/**
	 * The is in the state information about a food source and is inside the
	 * hive. The bee wanders around, may listen to other dancing bees, may
	 * switch into state dancing or starts foraging again.<br>
	 * This method differs from the original model by setting the bee to non
	 * receptive during dancing.
	 */
	protected void doStateInHiveWithInfo() {
		double pForgettingSource = getSimulation().pForgettingSource;
		double pForagingAgain = getSimulation().pForagingAgain;

		setColor(Color.yellow);
		receptive = true;
		double colonyNectarNeed = getSimulation().colonyNectarNeed;

		// check if the bee forgot the source
		double threshold_forgetting = pForgettingSource * (100 - sourceQuality);
		if (threshold_forgetting > r.nextDouble()) {
			forgetSource(Color.yellow, State.inHiveWithoutInfo);
			goInHive();
			return;
		}

		if ((dancingThreshold > -1)
				&& (dancingThreshold * colonyNectarNeed) > (r.nextDouble() * 10000)) {
			setColor(foodSource.getColor());
			setState(State.dancing);

			repeatedDance++;
			receptive = false;
			dancingTime = (int) Math.round(10 + dancingThreshold * .33);
			dancingThreshold *= .75;

			// if the bee is below the threshold
			if (dancingThreshold < 1) {
				dancingThreshold = -1;
				repeatedDance = 0;
				receptive = true;
			}

			goInHive();
			return;
		}

		if ((pForagingAgain * colonyNectarNeed) > r.nextDouble()) {
			// prepare for scouting
			// compute the energy needed for scouting
			double nectar = nectarForReturn + sourceDistance * nectarForOneStep
					+ r.nextDouble() * 5;
			nectarLoad = this.requestNectarFromHive(nectar);
			setState(State.leaveHive);

			goInHive();
			return;
		}

		goInHive();
	}

	/**
	 * This method is invoked if the bee will leave the hive. First the bees
	 * heads to the center of the entrance. If the bee is inside the entrance's
	 * sphere the bee will head in the current direction until the sphere is
	 * left.<br>
	 * This method has to differ between the states startForaging and scouting
	 * according if the bee currently has stored information about a food
	 * source.
	 */
	protected void doStateLeaveHive() {
		HiveEntrance entrance = hive.getEntrance();

		// the bee is inside the entrance area so it has to forward until it
		// leaves the entrance area
		if (entrance.isInSphere(this)) {
			forward();
			return;
		}

		// if the bee is inside the hive but not in the entrance area then head
		// to the entrance
		if (hive.isInSphere(this)) {
			headTo(hive.getEntrance());
			forward();
			return;
		}

		// the bee is outside the hive and entrance area so it can start

		// if the bee has information about a food source so start foraging
		if (foodSource != null) {
			setColor(foodSource.getColor());
			repeatedDance = 0;

			// set to foraging mode
			setState(State.foraging);
		}

		// if the bee has no information about a food source so start scouting
		if (foodSource == null) {
			setColor(Color.green);
			setState(State.scouting);
		}
	}

	/**
	 * The bee is in the state dancing. After the dancing time elapsed the bee
	 * will switch into the state in hive with info.<br>
	 * The state dancing is marked just by the state. The bee shows no special
	 * behaviour in this simulation. Other bees recognize this state and copies
	 * information about the food source from this bee.
	 */
	protected void doStateDancing() {
		dancingTime--;
		if (dancingTime <= 0) {
			setState(State.inHiveWithInfo);
		}

		goInHive();
	}

	/**
	 * The bee is in the foraging mode that means it has information about a
	 * food source and tries to find it.<br>
	 * The bee heads to the given direction until the distance is reached. Then
	 * the bee switches to state searching. If the bee runs out of fuel (nectar)
	 * during foraging it will return by switching to state returnWithoutInfo.
	 */
	protected void doStateForaging() {
		turnTo(sourceDirection);
		doStepOutgoing(null);
		sourceDistance--;

		// if the bee is at the estimated position set a search distance and
		// switch to state searching
		if (sourceDistance <= 0) {
			int maxSearchSteps = getSimulation().maxSearchSteps;
			sourceDistance = maxSearchSteps - (r.nextInt(maxSearchSteps) / 10);
			setState(State.searching);
		}

		// if the bee runs out of nectar return
		if (nectarLoad <= nectarForReturn) {
			forgetSource(Color.white, State.returnWithoutInfo);
		}
	}

	/**
	 * The bee arrived at the estimated food source position and now starts
	 * searching for the food source. The bee performs a random walk for the set
	 * search distance (encoded in {@link #sourceDistance}). If the food source
	 * is not found the bee switch to state returnWithoutInfo. If the food
	 * source is found the bee fetches nectar and heads to the hive. If the bee
	 * runs out of nectar during searching it switches to returnWithoutInfo.
	 */
	protected void doStateSearching() {
		sourceDistance--;
		if (sourceDistance <= 0) {
			forgetSource(Color.white, State.returnWithoutInfo);
		}

		turnBy(sourceDistance - 2 * r.nextDouble() * sourceDistance);
		doStepOutgoing(null);

		// if a food source is found get the nectar and switch to return mode
		foundSource();

		// no nectar, return home and forget about the source
		if (nectarLoad <= nectarForReturn) {
			forgetSource(Color.white, State.returnWithoutInfo);
		}
	}

	/**
	 * The bee is in returning mode and has information about a food source and
	 * some nectar load. This method invokes method
	 * {@link #doCommonReturnToHive()} that distinguish between the states with
	 * information and without.
	 */
	protected void doStateReturnWithInfoAndLoad() {
		doCommonReturnToHive();
	}

	/**
	 * Process the bee being in the unload queue. This method is even invoked if
	 * the bee has no nectar load and no information about a food source so this
	 * case has to be handled.
	 */
	protected void doStateUnloadQueue() {
		double colonyNectarNeed = getSimulation().colonyNectarNeed;
		/**
		 * Compute the parameters for the bee according to the food source.
		 */
		// compute the costs for the last trip
		double costs = foragingCosts * 5.8;
		// compute the gain by the last trip
		double gain = ((nectarLoad - foragingCosts) * sourceConcentration / 1000) * 5.8;
		// compute the individuality parameter for dancing and dancing time
		double individuality = .8 + ((r.nextDouble() * 20) / 100);
		// compute the source quality for the last source
		sourceQuality = gain / (costs + 1);
		// compute the threshold for dancing
		dancingThreshold = (individuality * sourceQuality)
				+ (20 - colonyNectarNeed);
		dancingThreshold = Math.max(0, dancingThreshold);

		// compute the dancing time
		// dancingTime = (int) Math.round(individuality * source_quality * 2);

		/*
		 * Compute the amount of honey returned by this bee to the hive. 1) The
		 * conversion factor between nectar and honey is .4 2) The honey in the
		 * hive is stored in ml but the nectar load is given in µl. 3)
		 * concentration is represented in mMol/L so again factor 1000
		 */
		double honey = nectarToHoney(nectarLoad); // the nectar load from the
		// bee
		honey /= 1000; // ml to mMol/L
		honey *= sourceConcentration; // mMol
		hive.storeHoney(honey);
		// remove the nectar load from this bee
		nectarLoad = 0;

		/*
		 * Do the preparations for switching to the next state.
		 */
		// head to the center of the hive
		headTo(hive);
		// the bee is receptive for an other dancing bee
		receptive = true;

		// handle the case the bee has information about the food source
		if (foodSource != null) {
			// set the color according the color of the source
			setColor(foodSource.getColor());
			// switch into the new state
			setState(State.inHiveWithInfo);
		}

		// handle the case the bee has no information about a food source
		if (foodSource == null) {
			setState(State.inHiveWithoutInfo);
		}
	}

	/**
	 * The bee is in state scouting and and behaves as follows:
	 * <ul>
	 * <li>With a probability of p(.2) it will turn by an angle between -20 and
	 * 20 degrees.</li>
	 * <li>It makes one step forward in the current direction.</li>
	 * <li>It will sense if any food source is around.</li>
	 * </ul>
	 */
	protected void doStateScouting() {
		// probability p(.2)
		if (r.nextInt(5) == 0) {
			turnBy(r.nextInt(40) - 20);
		}
		// do a step
		doStepOutgoing(null);

		// if a food source is found get the nectar and switch to return mode
		foundSource();

		// return if not enough energy for more scouting
		if (nectarLoad <= nectarForReturn) {
			forgetSource(Color.white, State.returnWithoutInfo);
		}
	}

	/**
	 * The bee is in returning mode and has no information about a food source.
	 * This method invokes method {@link #doCommonReturnToHive()} that
	 * distinguish between the states with information and without.
	 */
	protected void doStateReturnWithoutInfo() {
		doCommonReturnToHive();
	}

	/**
	 * Prepare to let this bee die. Set it into the terminated step. This is
	 * necessary because the agent will be called once more before totally
	 * removed from simulation.
	 */
	protected void doStateDead() {
		setColor(Color.red);
		getSimulation().schedule.scheduleOnce(this);
		setState(State.terminated);
	}

	/**
	 * Remove the agent from the simulation.
	 */
	protected void doStateTerminated() {
		setColor(new Color(0xd0, 0x00, 0x00));
		getSimulation().removeAgent(this);
		System.err.println("Terminate called for Bee " + this);
	}

	/**
	 * This method leads the bee to the entrance of the hive. After reaching the
	 * entrance the bee will head to the unload queue even if no nectar is
	 * loaded.
	 */
	private void doCommonReturnToHive() {
		AbstractMovingAgent entrance = hive.getEntrance();
		doStepReturning(entrance);
		if (entrance.isInSphere(this)) {
			setState(State.unloadQueue);
		}
	}

	/**
	 * Do a step in the hive. The bee performs a random walk by invoking
	 * {@link #doStepWalking()}. With a certain probability the bee listen to an
	 * other bee that is dancing.
	 */
	private void goInHive() {
		double colonyNectarNeed = getSimulation().colonyNectarNeed;
		doStepWalking();
		// if this bee may listen to a dancing bee
		if ((receptive) && (colonyNectarNeed >= r.nextDouble()))
			listenToDancingBee();
	}

	/**
	 * Fly back to the hive. This method uses the parametrized method
	 * {@link #doStepFlying(IMovingAgent, boolean, boolean)}.
	 * 
	 * @param target
	 *            The location the bee is heading for, null keeps current course
	 */
	private void doStepReturning(IMovingAgent target) {
		doStepFlying(target, true, false);
		sourceDistance += (int) Math.round(getVelocityVector().length());
	}

	/**
	 * Fly away from the hive. This method uses the parametrized method
	 * {@link #doStepFlying(IMovingAgent, boolean, boolean)}.
	 * 
	 * @param target
	 *            The location the bee is heading for, null keeps current course
	 */
	private void doStepOutgoing(IMovingAgent target) {
		doStepFlying(target, true, true);
	}

	/**
	 * Do flying. During the flight is checked if the boundaries of the
	 * simulation are left. If so the bee is reverting the flying vector. This
	 * method takes the current target position in account when setting the new
	 * direction. If no target is given the current direction is kept and only
	 * the avoidance of objects is computed.
	 * 
	 * @param target
	 *            The agent the bee is heading for, null keeps current course
	 * @param checkBoundaries True if to check if the bee will fly inside the simulations boundaries.
	 * @param checkHive Check if to avoid the hive.
	 */
	private void doStepFlying(IMovingAgent target, boolean checkBoundaries,
			boolean checkHive) {
		// register the current load for this bee
		nectarLoad -= nectarForOneStep;

		if (target != null) {
			headTo(target);
		}

		// add the current costs for this step to the current foraging costs
		if (getState() != State.foraging)
			foragingCosts += nectarForOneStep;

		// check if bee is dead because it ran out of fuel
		if (nectarLoad < 0) {
			setState(State.dead);
			return;
		}

		// Make an u-turn if the bee is colliding with the hive or is outside
		// the simulation boundaries.
		if (checkHive & hive.isInSphere(this)
				|| (checkBoundaries & getSimulation().isOutside(this))) {
			turnBy(180 + (r.nextDouble() * 10) - 5);
			forward();
			return;
		}

		// the bee is not outside the boundaries or inside the hive

		/*
		 * avoid obstacles if necessary by adding an avoidance vector to the
		 * current moving vector and use this new vector for the next step
		 * forward
		 */
		if (getSimulation().avoidObstacles) {
			// avoid obstacles like other bees
			Vector3d av = computeAvoidance(target);
			// the avoidance is five times more important than the current
			// direction
			av.scale(5);
			// add the current direction to the avoidance and normalize it
			av.add(getVelocityVector());
			av.normalize();
			// one step forward
			forward(av);
		} else {
			// no avoidance, so move according to the stored vector
			forward();
		}
	}

	/**
	 * Compute the avoidance of objects by:
	 * <ul>
	 * <li>Get all objects that might collide with or are near within 5 times of
	 * the sphere radius of this bee. Consider the sphereRadius of all objects.</li>
	 * <li>Get all objects in an angle of +/- 90 degrees of flight direction.</li>
	 * <li>The urge to avoid an object is the higher the closer the object is.</li>
	 * </ul>
	 */
	private Vector3d computeAvoidance(IMovingAgent currentTarget) {
		double sphereRadius = getSphereRadius();
		double observationRadius = sphereRadius * 5;
		double avoidRadius = sphereRadius * 5;

		boolean useMyRadius = true;
		boolean useTheirRadius = true;

		// get all agents excluding myself in the given distance
		IMovingAgent[] agents = this.getObjectsWithinMyDistance(
				observationRadius, useMyRadius, useTheirRadius, getSimulation()
						.getMaxSphereRadius(), false, null);

		// get all objects in a direction of +/- 90 degrees that are to be
		// avoided and compute an avoidance vector
		Vector3d avoidVector = new Vector3d();
		int i = 0;
		for (i = 0; i < agents.length; i++) {
			IMovingAgent agent = agents[i];

			// check first if the other has to be avoided
			if (checkToAvoid(agent, currentTarget)) {
				// the angle of this agent
				double angle = this.angle(agent);
				angle = Math.toDegrees(angle);
				// no set it in relation to agents orientation
				angle -= this.getOrientation();
				// if the objects are in the correct angle add the avoidance to
				// the avoidance vector; this agent looks from 90 degrees from
				// left to right
				if (!((angle < -90.0) | (angle > 90))) {
					double distance = distance(agent);
					if (useTheirRadius)
						distance -= agent.getSphereRadius();

					if (useMyRadius)
						distance -= sphereRadius;

					if (distance <= avoidRadius) {
						// the vector from the agent to the obstacle
						Vector3d v = vectorTo(agent);
						double len = v.length();

						if (len != 0.0) {
							// normalize and scale according to distance
							double s = 1 / len / (distance * distance);
							v.scale(s);
						}

						double turnAngle = 90 - 10 * r.nextGaussian();
						if (angle > 0.0)
							turnAngle = -turnAngle;
						Geometric.rotateBy(v, Math.toRadians(turnAngle));

						avoidVector.add(v);
					}
				}
			}
		}

		// normalize the vector
		Geometric.normalize(avoidVector);

		return avoidVector;
	}

	/**
	 * Check if an agent has to be avoided by this agent. The avoidance is
	 * computed according to the state and the current target.
	 * 
	 * @param agent
	 *            The agent to be checked if it has to be avoided.
	 * @param currentTarget
	 *            The current target of this agent.
	 * @return True, if the agent has to be avoided by this agent.
	 */
	private boolean checkToAvoid(IMovingAgent agent, IMovingAgent currentTarget) {
		State state = getState();
		int mask = 0;
		int value = 0;

		if (agent == currentTarget)
			value |= valueCurrentTarget;
		if (agent instanceof Obstacle)
			value |= valueObstacle;
		if (agent instanceof Bee)
			value |= valueBee;
		if (agent instanceof Hive)
			value |= valueHive;
		if (agent instanceof HiveEntrance)
			value |= valueEntrance;
		if (agent instanceof FoodSource)
			value |= valueFoodSource;

		switch (state) {
		case foraging:
		case searching:
			mask = valueObstacle | valueBee | valueHive | valueEntrance;
			break;
		case returnWithInfo:
		case returnWithInfoAndLoad:
			mask = valueObstacle | valueHive;
			// mask = valueObstacle | valueBee | valueHive;
			break;
		case scouting:
			mask = valueObstacle | valueBee | valueHive | valueEntrance;
			break;
		case returnWithoutInfo:
			mask = valueObstacle | valueHive;
			// mask = valueObstacle | valueBee | valueHive;
			break;
		case initialState:
		case leaveHive:
		case inHiveWithoutInfo:
		case inHiveWithInfo:
		case danceFollowing:
		case dancing:
		case unloadQueue:
		case dead:
		case terminated:
			System.err.println("checkToAvoid(): State " + state
					+ " is not expected for testing to avoid.");
			return true;

		default:
			System.err.println("checkToAvoid(): State " + state
					+ " is not implemented.");
			System.exit(-7);
			break;
		}

		// now compare if mask and value differs to 0 then return true
		return (value & mask) != 0;
	}

	/**
	 * Do a step inside the hive.
	 */
	private void doStepWalking() {
		if ((hive.distanceToSphere(this, true) + this.getSphereRadius()) >= hive
				.getSphereRadius()) {
			headTo(hive);
			forward();
		} else {
			turnBy(45 - r.nextDouble() * 90);
			forward();
		}
	}

	/**
	 * Is a {@link FoodSource} to be found by this bee? If so, return the found
	 * food source.
	 * 
	 * @return The found food source.
	 */
	private FoodSource foundSource() {

		// is the bee around a food source
		FoodSource fs = nearFoodSource();

		if (fs != null) {
			foodSource = fs;
			sourceConcentration = fs.getConcentration();
			sourceLocation = fs.getLocation();
			sourceDistance = 0;
			sourceDirection = Math
					.toDegrees(orientation(hive.getEntrance(), fs));
			nectarLoad = fs.getNectar(nectarStomach);
			setColor(fs.getColor());
			setState(State.returnWithInfoAndLoad);
		}

		return fs;
	}

	/**
	 * Return a food source a bee is near by. If more than one food source is in
	 * the perimeter of the bee one is randomly chosen.
	 * 
	 * @return One (if more) of the food source, nil otherwise.
	 */
	private FoodSource nearFoodSource() {
		// find all objects those outer perimeter is or closer than .5 units to
		// this bee
		Object[] objects = this.getObjectsWithinMyDistance(.5, true, true,
				false);

		// filter the food sources
		objects = Filter.filter(objects, FoodSource.class);

		// set the default return value
		FoodSource fs = null;
		// if food sources are around choose one randomly
		if (objects.length > 0) {
			fs = (FoodSource) objects[r.nextInt(objects.length)];
		}

		return fs;
	}

	/**
	 * Reset all information about the last or current food source; set the
	 * current color and the next state.
	 */
	private void forgetSource(Color newColor, State nextState) {
		sourceQuality = 0;
		dancingThreshold = -1;
		sourceDistance = 0;
		sourceConcentration = 0;
		repeatedTrip = 0;
		foodSource = null;
		foragingCosts = 0;

		if (newColor != null)
			setColor(newColor);
		setState(nextState);
	}

	/**
	 * Copy the information about a food source from the given bee into this
	 * bee.
	 * 
	 * @param src
	 */
	private void copySourceInformationFrom(Bee src) {
		copySourceInformation(this, src);
	}

	/**
	 * Copy the information about a food source from one bee to another.
	 * 
	 * @param dest
	 *            The destination {@link Bee}
	 * @param src
	 *            The source {@link Bee}
	 */
	private void copySourceInformation(Bee dest, Bee src) {
		dest.sourceQuality = src.sourceQuality;
		dest.sourceDistance = src.sourceDistance;
		dest.sourceDirection = src.sourceDirection;
		dest.setColor(src.getColor());
		dest.sourceConcentration = src.sourceConcentration;
		dest.foodSource = src.foodSource;
	}

	/**
	 * Listen to a dancing bee if one is around and be chosen to listen. Switch
	 * to foraging if listened to dancing bee.
	 */
	private void listenToDancingBee() {
		// get all agents excluding myself in the given distance
		IMovingAgent[] agents = this.getObjectsWithinMyDistance(1.0, true,
				true, this.getSphereRadius(), false, null);
		// remove all agents except bees
		agents = (IMovingAgent[]) Filter.filter(agents, Bee.class);

		if (agents.length > 0) {
			int index = r.nextInt(agents.length);
			Bee b = (Bee) agents[index];
			// check if the chosen bee is dancing
			if (b.getState() == Bee.State.dancing) {
				double comNoise = getSimulation().comNoise;
				// if the bee is dancing just copy the information about the
				// source into this bee and switch to foraging mode
				copySourceInformationFrom(b);
				sourceDistance += Math.round((r.nextGaussian() * comNoise)
						* sourceDistance);
				sourceDirection += (r.nextGaussian() * comNoise) * 360;
				sourceDirection = Geometric.clampAngleDegree(sourceDirection);
				receptive = false;
				double nectar = nectarForReturn + sourceDistance
						* nectarForOneStep + r.nextDouble() * 5;
				nectarLoad = requestNectarFromHive(nectar);

				repeatedTrip = 0;
				repeatedDance = 0;

				setState(State.leaveHive);
			}
		}
	}

	/**
	 * Set the current state. Setting the should be done by this method so no
	 * internal interference will occur when tampering with the state.
	 * 
	 * @return The current state.
	 */
	private State getState() {
		return this.state;
	}

	/**
	 * Set the current state. Setting the should be done by this method so no
	 * internal interference will occur when tampering with the state.
	 * 
	 * @param state
	 */
	private void setState(State state) {
		this.state = state;
	}
}
