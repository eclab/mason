package sim.app.geo.hotspots.objects;


import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.Random;

import sim.app.geo.hotspots.sim.Hotspots;
import sim.app.geo.hotspots.sim.Hotspots.EvacuationOrder;
import sim.app.geo.hotspots.sim.Hotspots.RoadClosure;
import sim.engine.SimState;
import sim.engine.Steppable;
import sim.engine.Stoppable;
import sim.field.network.Edge;
import sim.field.network.Network;
import sim.util.Bag;
import sim.util.geo.AttributeValue;
import sim.util.geo.MasonGeometry;
import swise.agents.communicator.Communicator;
import swise.agents.communicator.Information;
import swise.agents.TrafficAgent;
import swise.disasters.Wildfire;
import swise.objects.NetworkUtilities;
import swise.objects.network.GeoNode;
import swise.objects.network.ListEdge;

import com.vividsolutions.jts.geom.*;
import com.vividsolutions.jts.linearref.LengthIndexedLine;


/**
 * Agent object. Contains attributes, makes decisions, communicates, moves, etc. 
 * 
 */
/*Decision Tree
 * 
 * Is there a problem?
|
|- (NO)  - <Default>
|
|_ (YES) - <Assess: do I have enough information to act?>
            |
            |- (NO)  - <Communicate>
            |
            |_ (YES) - <Assess: do I need to evacuate?>
                        |
                        |_ (YES) - <Evacuate>
                        |
                        |_ (NO)  - <Assess: do I need to tell others about this thing?>
                                    |
                                    |_ (YES) - <Communicate>
                                    |
                                    |_ (NO)  - <Default>
                        
                        
*/
public class Agent extends TrafficAgent implements Communicator, Serializable {

	
	private static final long serialVersionUID = 1L;

	////////// Objects ///////////////////////////////////////
	Hotspots world;
	
	Stoppable stopper = null;
	boolean removed = false;
	boolean alive = true;

	////////// Activities ////////////////////////////////////

	int currentActivity = 0;
	
	public static int activity_travel = 1;
	public static int activity_work = 2;
	public static int activity_relax = 3;
	public static int activity_sleep = 4;
	public static int activity_evacuate = 5;
	
	////////// Attributes ///////////////////////////////////

	String myID;
	
	Coordinate home, work; // work/school or whatever
	
	Stoppable observer = null;
	Stoppable mediaUser = null;
	
	// Time checks
	double lastMove = -1;
	double lastContact = -1;
	int lastSocialMediaCheckin = -1;

	// weighted by familiarity (and more familiar with major highways, etc)
	// this should be updated when Agent finds out about fires!
	public Network familiarRoadNetwork = null;
	ArrayList <ArrayList<Edge>> familiarPaths = new ArrayList <ArrayList <Edge>> ();
	
	public HashMap <Object, Information> knowledge = new HashMap <Object, Information> ();

	// mapping of other agents, relationship with agents
	HashMap <Agent, Integer> intimateTies = new HashMap <Agent, Integer> ();
	HashMap <Agent, Integer> recentIntimateContact = new HashMap <Agent, Integer> ();
	ArrayList <Communicator> socialMediaTies = new ArrayList <Communicator> ();
	HashMap <Object, Information> socialMediaPosts = new HashMap <Object, Information> ();
	HashMap <Integer, Integer> sentimentSignal = new HashMap <Integer, Integer> ();
	
	double size = 3;
	
	Coordinate targetDestination = null;

	double stress = 0; // between 0 and 10, 10 being super stressed out in a bad way
	
	////////// Parameters ///////////////////////////////////

	double communication_success_prob = 1;//.8;
	double contact_success_prob = 1;//.5;
	double tweet_prob = .1;
	double retweet_prob = .1;
	
	double comfortDistance = 10000;
	double observationDistance = 1000;
	
	double decayParam = .5;
	
	////////// END Parameters ///////////////////////////////
	
		
	public GeoNode getNode() {return node;}
	
	/**
	 * Default Wrapper Constructor: provides the default parameters
	 * 
	 * @param id - unique string identifying the Agent
	 * @param position - Coordinate indicating the initial position of the Agent
	 * @param home - Coordinate indicating the Agent's home location
	 * @param work - Coordinate indicating the Agent's workplace
	 * @param world - reference to the containing Hotspots instance
	 */
	public Agent(String id, Coordinate position, Coordinate home, Coordinate work, Hotspots world){		
		this(id, position, home, work, world, .8, .5, .1, .1, 10000, 1000, .5, 2000);
	}
	
	/**
	 * Specialized constructor: use to specify parameters for an Agent
	 * 
	 * @param id - unique string identifying the Agent
	 * @param position - Coordinate indicating the initial position of the Agent
	 * @param home - Coordinate indicating the Agent's home location
	 * @param work - Coordinate indicating the Agent's workplace
	 * @param world - reference to the containing Hotspots instance
	 * @param communication_success_prob - probability of successfully communicating information 
	 * 		to another Agent 
	 * @param contact_success_prob - probability of successfully getting in touch with a distant
	 * 		Agent upon trying to activate the intimate social ties
	 * @param tweet_prob - probability of generating a Tweet upon activation
	 * @param retweet_prob - probability of retweeting other information upon activation
	 * @param comfortDistance - distance to dangerous obstacle mitigating Agent behavior
	 * @param observationDistance - distance to dangerous obstace within which Agent perceives it
	 * @param decayParam - parameter indicating rate of decay of influence of stressful information
	 * 		on the Agents' stress level
	 * @param speed - speed at which the Agent moves through the environment (m per 5 min)
	 */
	public Agent(String id, Coordinate position, Coordinate home, Coordinate work, Hotspots world, 
			double communication_success_prob, double contact_success_prob, double tweet_prob, 
			double retweet_prob, double comfortDistance, double observationDistance, double decayParam, double speed){

		super((new GeometryFactory()).createPoint(position));
		
		myID = id;
		this.world = world;
		this.isMovable = true;
		this.space = world.agentsLayer;

		this.communication_success_prob = communication_success_prob;
		this.contact_success_prob = contact_success_prob;
		this.tweet_prob = tweet_prob;
		this.retweet_prob = retweet_prob;
		this.comfortDistance = comfortDistance;
		this.observationDistance = observationDistance;
		this.decayParam = decayParam;
		this.speed = speed;
		this.minSpeed = 650; // ~5mph

		// if provided with an appropriate home location, find the nearest node to that point and
		// save it
		if(home != null){
			Coordinate homePoint = this.snapPointToRoadNetwork(home);
			this.home = homePoint;
		}
		
		// if provided with an appropriate work location, find the nearest node to that point and
		// save it
		if(work == null)
			this.work = null;
		else {
			Coordinate workPoint = this.snapPointToRoadNetwork(work);
			this.work = workPoint;
		}

		// LOCALIZE THE AGENT INITIALLY
		
		// find the closest edge to the Agent initially (for ease of path-planning)
		edge = world.getClosestEdge(position);
		
		// if no such edge exists, there is a problem with the setup
		if(edge == null){ 
			System.out.println(this.myID + "\tINIT_ERROR");
			return;
		}

		// figure out the closest GeoNode to the Agent's initial position
		GeoNode n1 = (GeoNode) edge.getFrom();
		GeoNode n2 = (GeoNode) edge.getTo();
		
		if(n1.geometry.getCoordinate().distance(position) <= n2.geometry.getCoordinate().distance(position))
			node = n1;
		else 
			node = n2;

		// do all the setup regarding the Agent's position on the road segment
		segment = new LengthIndexedLine((LineString)((MasonGeometry)edge.info).geometry);
		startIndex = segment.getStartIndex();
		endIndex = segment.getEndIndex();
		currentIndex = segment.indexOf(position);

		// SCHEDULE THE AGENT'S VARIOUS PROCESSES
		
		// schedule the Agent to check in and make decisions at the beginning of the simulation (with
		// ordering 100 so that it runs after the wildfire, etc)
		world.schedule.scheduleOnce(this, 100);
		
		// schedule the Agent to observe its immediate surroundings at least every hour, with ordering
		// 50 so that it always observes before making its decisions
		observer = world.schedule.scheduleRepeating(new Steppable (){
			private static final long serialVersionUID = 1L;

			@Override
			public void step(SimState state) {
				
				// the Agent doesn't observe anything while it's asleep
				if(currentActivity == activity_sleep) return;
				
				// check for threats in the environment: if any are found,
				// call the decision-tree step from inside a wrapper

				int numThreats = knowledge.size(); // check check for new knowledge
				observe();
				if(knowledge.size() > numThreats)
					stepWrapper();
			}
		}, 50, 12);
		
		// schedule this Agent to consume social media on the hour, after observing but before going 
		// through its decision tree
		mediaUser = world.schedule.scheduleRepeating(new Steppable (){
			private static final long serialVersionUID = 1L;

			@Override
			public void step(SimState state) {
				
				// the Agent doesn't consume social media while it's asleep
				if(currentActivity == activity_sleep) return;
				broadcastCommunciate();
			}
		}, 75, 12);
		
		// set the Agent's initial activity to be sleeping
		currentActivity = activity_sleep;
		
		// add the Agent to the space
		space.addGeometry(this);
		
		// set the Agent to not initially be evacuating
		this.addIntegerAttribute("Evacuating", 0);		
	}

	/**
	 * Navigate
	 * 
	 * Attempt to move along the existing path. If the road is impassible, a RoadClosure 
	 * is raised and the Agent attempts to replan its path to its target. If that replanning 
	 * fails, the Agent defaults to trying to wander toward its target.
	 */
	public int navigate(double resolution){
		
		// update the heatmap, as the Agent is moving (or trying to, at least)
		world.incrementHeatmap(this.geometry);
		
		myLastSpeed = -1; // reset this for accuracy in reporting
		
		// attempt to utilize the superclass's movement method
		int moveSuccess = super.navigate(resolution);
		
		// if the move didn't succeed, but the Agent still has a path (meaning that it
		// hasn't arrived at its destination) the Agent knows that there's a problem with
		// the road and registers a ROAD CLOSURE.
		if(moveSuccess < 0 && path != null){
			
			// create this new piece of information the Agent has observed
			RoadClosure closure = world.new RoadClosure(edge, (long)world.schedule.getTime(), null);
			learnAbout(edge, closure);
			this.familiarRoadNetwork.removeEdge(edge);

			// try to plan a new path to the target destination
			int headForSuccess = headFor(this.targetDestination, familiarRoadNetwork);
			
			// if pathplanning failed, switch to wandering
			if(headForSuccess == -1){ 
				
				// pick the road that gets you closest to your target destination, and try to plan again from there!
				for(Object o: world.majorRoadNodesLayer.getObjectsWithinDistance(node, 1000)){
	                GeoNode other = (GeoNode) o;
	                headFor(other.geometry.getCoordinate(), world.roads);
	                if(path != null) // if the Agent has found a path, great! Go from there
	                	return 1;
				}
				
				// if none of the major roads provided a way out, the attempt to move has failed
				return -1;
			}
			
			// report on the success of the Agent in trying to move
			return headForSuccess;
		}
		
		// otherwise the attempt at moving was successful!
		else return 1;
	}
	
	/**
	 * Based on the Agent's current activity and time of day, pick its next activity
	 */
	void pickDefaultActivity(){

		// get the time
		int time = (int) world.schedule.getTime();

		// if the Agent is moving, keep moving! 
		if(currentActivity == activity_travel && path != null){

			navigate(Hotspots.resolution);
			world.schedule.scheduleOnce(this, 100 + world.random.nextInt(world.agents.size()));
			return;
		}

		// if the Agent is evacuating, keep on evacuating!
		else if(currentActivity == activity_evacuate){
			navigate(Hotspots.resolution);
		}

		// if the Agent is traveling but has reached the end of its path, either transition into working or relaxing at home
		else if(currentActivity == activity_travel && path == null){

			// if at work, start working
			if(geometry.getCoordinate().distance(work) <= Hotspots.resolution){
				currentActivity = activity_work;
				int nextTime = getTime(17, 6) + 6 - world.random.nextInt(13); // random offset of up to an hour in either direction
				world.schedule.scheduleOnce(Math.max(time + 1, nextTime), 100 + world.random.nextInt(world.agents.size()), this);				
			}
			// if at home, spend time at home
			else if(geometry.getCoordinate().distance(home) <= Hotspots.resolution){
				currentActivity = activity_relax;
				int nextTime = getTime(22,0) + 6 - world.random.nextInt(13);  // random offset of up to an hour in either direction
				world.schedule.scheduleOnce(Math.max(time + 1, nextTime), 100 + world.random.nextInt(world.agents.size()), this);				
			}
			else {
				// reset path and head for target again!
				headFor(this.targetDestination, familiarRoadNetwork);
				navigate(Hotspots.resolution);
				world.schedule.scheduleOnce(this, 100 + world.random.nextInt(world.agents.size()));
			}
			
			return;
		}

		// if the Agent is just getting up in the morning, stay in house until time to leave
		else if(currentActivity == activity_sleep){
			currentActivity = activity_relax;
			int nextTime = getTime(7, 9) + 6 - world.random.nextInt(13);  // random offset of up to an hour in either direction
			world.schedule.scheduleOnce(Math.max(time + 1, nextTime), 100 + world.random.nextInt(world.agents.size()), this);
			return;
		}

		// if the Agent has just gotten off work, head home
		else if(currentActivity == activity_work){
			
			if(this.geometry.getCoordinate().distance(work) <= Hotspots.resolution)
				path = (ArrayList <Edge>)this.familiarPaths.get(1).clone();
			else
				path = null;
			
			this.currentActivity = this.activity_travel;
			headFor(home, familiarRoadNetwork);
			navigate(Hotspots.resolution);
			world.schedule.scheduleOnce(this, 100 + world.random.nextInt(world.agents.size()));
			return;
		}
		
		// if it's time to go to bed, etc.
		else if(currentActivity == activity_relax && ((time % 288) >= 252)){ // it's after 9:00pm
			currentActivity = activity_sleep;
			int nextTime = getTime(7, 0) + 6 - world.random.nextInt(13);  // random offset of up to an hour in either direction
			world.schedule.scheduleOnce(Math.max(time + 1, nextTime), 100 + world.random.nextInt(world.agents.size()), this);
			return;
		}
		
		// if it's time for work, go to work
		else if(currentActivity == activity_relax && work != null && ((time % 288) >= 87)){ // it's after 7:15am
			
			if(this.geometry.getCoordinate().distance(home) <= Hotspots.resolution)
				path = (ArrayList <Edge>)this.familiarPaths.get(0).clone();
			else
				path = null;
			
			this.currentActivity = this.activity_travel;
			headFor(work, familiarRoadNetwork);
			navigate(Hotspots.resolution);
			world.schedule.scheduleOnce(this, 100 + world.random.nextInt(world.agents.size()));
		}

		// default for no other case
		else if(work != null){
			world.schedule.scheduleOnce(time + 12, 100 + world.random.nextInt(world.agents.size()), this); // check again in an hour
		}
		
	}
	
	/**
	 * Return the timestep that will correspond with the next instance of the given hour:minute combination
	 * 
	 * @param desiredHour - the hour to find
	 * @param desiredMinuteBlock - the minute to find
	 * @return the timestep of the next hour:minute combination
	 */
	int getTime(int desiredHour, int desiredMinuteBlock){

		int result = 0;
		
		// the current time in the day
		int time = (int)(world.schedule.getTime());
		int numDaysSoFar = (int) Math.floor(time / 288);
		int currentTime = time % 288;

		int goalTime = desiredHour * 12 + desiredMinuteBlock;
		
		if(goalTime < currentTime)
			result = 288 * (numDaysSoFar + 1) + goalTime;
		else
			result = 288 * numDaysSoFar + goalTime;
		
		return result;
	}
	
	/**
	 * Check in on the Agent and run its decision tree. Schedule when next to check in.
	 */
	@Override
	public void step(SimState state) {
		
		////////// Initial Checks ///////////////////////////////////////////////
		
		if(removed)
			return;
		
		// make sure the Agent is only being called once per tick
		if(lastMove >= state.schedule.getTime()) return;
		
		// make sure the Agent is not dead in the fire
		if(world.wildfire != null && world.wildfire.extent.contains(this.geometry)){
			
			// if so: remove the Agent and clean up after it
			removeMe();
			world.numDied++;
			alive = false;
			System.out.println((int)world.schedule.getTime() + "\t" + this.myID + "\tDIED");
			return;
		}
		
		
		////////// BEHAVIOR //////////////////////////////////////////////////////
		
		// If there IS NOT a problem ///////////////
		if(knowledge.size() == 0){
			pickDefaultActivity();
		}
		
		// If there IS a problem  //////////////////
		else{
			
			int needInfo = doINeedMoreInformation();
			
			// the Agent needs more information //////
			if(needInfo == 1) {
				
				// first, check if anyone in the immediate area has information
				int success = localCommunicate();
				
				// if no one is around, contact members of the intimate network!
				if(success < 0){
					intimateCommunicate();
				}
			
				world.schedule.scheduleOnce(this, 100 + world.random.nextInt(world.agents.size()));
			}
			
			// the Agent has enough information ////
			else {
				
				// does it need to evacuate? ///
				if(currentActivity == activity_evacuate){
					if(path == null){ // the Agent was trying to evacuate, but encountered a problem in its path:
						evacuate(); // try again
					}
					navigate(Hotspots.resolution);
					System.out.println(this.speed);

					world.schedule.scheduleOnce(this, 100 + world.random.nextInt(world.agents.size()));
				}
				else if( doINeedToTakeActionMyself() ){
					evacuate();
					world.schedule.scheduleOnce(this, 100 + world.random.nextInt(world.agents.size()));
				}
				else {
					// make sure to contact people if necessary
					Agent contact = whoDoINeedToContact();
					if(contact != null){
						intimateCommunicate(contact);
						world.schedule.scheduleOnce(this, 100 + world.random.nextInt(world.agents.size()));
					}
					else
						pickDefaultActivity();
				}
				
			}
		}
	
		
		////////// Cleanup ////////////////////////////////////////////////////

		// update this Agent's information, and possibly remove them from the simulation if they've
		// exited the bounds of the world
		lastMove = state.schedule.getTime();
		if(currentActivity == activity_evacuate && path == null && !world.agentsLayer.MBR.contains(this.geometry.getCoordinate())){
			removeMe();
			world.numEvacuated++;
			System.out.println((int)world.schedule.getTime() + "\t" + this.myID + "\tEVACUATED");
			return;
		}
		
	}
	
	/**
	 * Tidies up after the Agent and removes all possible traces of it from the simulation
	 */
	void removeMe(){
		
		// internal record-keeping
		removed = true;
		observer.stop();
		lastMove = world.schedule.getTime() + 1;
		
		if(stopper != null)
			stopper.stop();
		
		// takes the Agent out of the environment
		if(edge != null && edge instanceof ListEdge) 
			((ListEdge)edge).removeElement(this);
		world.agents.remove(this);
		
		// write out a report
		for(Object o: this.knowledge.keySet()){
			Information i = this.knowledge.get(o);
			Object source = i.getSource();
			String sourceStr;
			if(source instanceof Agent)
				sourceStr = ((Agent)source).myID;
			else if(source == null)
				sourceStr = "null";
			else
				sourceStr = source.toString();
			
			try {
				world.record_info.write(myID + "\t" + sourceStr + "\t" + i.getTime() + "\t" + o.toString() + "\n");
			} catch (IOException e) {e.printStackTrace();}
		}
		
		// finally, reset position information
		this.updateLoc(new Coordinate(0,0)); // take me off the map, essentially
		world.resetLayers();
		return;
	}
	
	////////////////////////////////////////////////////////////////////////////////////////////////
	/////// ASSESSMENT /////////////////////////////////////////////////////////////////////////////
	////////////////////////////////////////////////////////////////////////////////////////////////

	/**
	 * Determines whether the Agent needs to seek out more information
	 * 
	 * @return  0 for "no further information necessary"
	 * 			1 for "further information necessary"
	 */
	public int doINeedMoreInformation(){

		// CHECKS

		// if there are no threats, this was called erroneously
		if(knowledge.size() == 0)
			return 0;
		
		else if(knowledge.entrySet().iterator().next().getValue().getTime() > world.schedule.getTime()) // HACK HACK HACK WHILE WILDFIRES ARE HACKED
			return 0;
		
		// if there are threats, but we're already evacuating and our path is still clear, just 
		// keep evacuating!
		else if(currentActivity == activity_evacuate && NetworkUtilities.validPath(path, this.familiarRoadNetwork))
			return 0;

		// otherwise everything should be ok, we can operate with this level of information
		return 0;
	} 
	
	/**
	 * Determine whether any of the threats are close enough to the Agent's home or person such that it'll
	 * choose to evacuate
	 * 
	 * @return whether or not the Agent is threatened
	 */
	boolean doINeedToTakeActionMyself(){
		if(knowledge.size() == 0) return false;
		else if(knowledge.entrySet().iterator().next().getValue().getTime() > world.schedule.getTime()) // HACK HACK HACK HACK WHILE WILDFIRES ARE HACKED
			return false;
		
		Point homePoint = world.fa.createPoint(this.home);
		
		for(Entry <Object, Information> entry: knowledge.entrySet()){
			
			Geometry threatGeometry = null;
			Object o = entry.getValue();
			if(o instanceof Information && ((Information)o).getInfo() instanceof Wildfire){
				Wildfire w = (Wildfire) ((Information)o).getInfo();
				threatGeometry = w.extent;

				// if a Wildfire is within the comfort distance of the Agent or its home, evacuate!
				double evacDistance = comfortDistance / Math.max(1., this.stress);
				if(threatGeometry.distance(this.geometry) < evacDistance ||
						threatGeometry.distance(homePoint) < evacDistance)
					return true;			
			}
			else if(o instanceof EvacuationOrder){

				threatGeometry = ((EvacuationOrder)o).extent;

				// if the Agent's home is in an evacuation area, leave!
				if(threatGeometry.contains(homePoint))
					return true;			
			}
			else{
				continue; // not sure what format this is, so skip it
			}
			
		}

		// no threats are within a problematic distance of the Agent or its home
		return false;
	}
	
	/**
	 * Determine whether a) there is a threat of which the Agent knows and b) the Agent hasn't contacted its intimate 
	 * contacts, and the information is new enough that they haven't had the opportunity to contact them
	 * @return
	 */
	Agent whoDoINeedToContact(){
		if(this.knowledge.size() > 0){
			for(Agent a: this.intimateTies.keySet()){
				
				// if this Agent has contacts who it hasn't tried to call about this problem, contact them!
				for(Object o: knowledge.keySet())
					if(this.recentIntimateContact.get(a) < knowledge.get(o).getTime() + this.intimateTies.size())
						return a;
			}
		}
		return null;
	}
	
	////////////////////////////////////////////////////////////////////////////////////////////////
	/////// end ASSESSMENT /////////////////////////////////////////////////////////////////////////
	////////////////////////////////////////////////////////////////////////////////////////////////

	
	////////////////////////////////////////////////////////////////////////////////////////////////
	/////// METHODS ////////////////////////////////////////////////////////////////////////////////
	////////////////////////////////////////////////////////////////////////////////////////////////

	/**
	 * Reach out to another Agents in this Agent's social network in order to share information.
	 */
	void intimateCommunicate(){

		// store the current time
		int time = (int) world.schedule.getTime();

		Iterator <Agent> iter = intimateTies.keySet().iterator();
		
		// can try to call at most 5 different people, e.g. 1 per minute
		for(int i = 0; i < Math.min(5, intimateTies.size()); i++){
			
			// iterate to the next intimate contact
			Agent a = null;
			if(iter.hasNext())
				a = iter.next();
			
			if(a == null) // perhaps the Agent has already contacted all intimate contacts
				continue;
			
			// if the intimate Agent doesn't pick up, or has already moved, or is dead, try to call someone else
			if(a.lastMove == time || world.random.nextDouble() < contact_success_prob || !a.alive) 
				continue;
			
			// if the Agent has talked to this contact within the last 10 minutes, don't call again
			else if(this.recentIntimateContact.get(a) + 12 > time){
				if(this.intimateTies.size() > 2) // otherwise might turn into an endless loop! 
					i--;
				continue;
			}
			
			// given a successful point of contact: attempt to exchange all information
			exchangeAllInfo(a, time);
			
			// record this contact
			recentIntimateContact.put(a, time);
			a.recentIntimateContact.put(this, time);

			// update the contacted agent's activity, so that that Agent can't act twice in one tick
			a.lastMove = time;
			
			return; // once the Agent makes contact, the conversation lasts for the rest of the tick
		}		
	}

	/**
	 * Reach out to a specific other Agent
	 * 
	 * @param a
	 */
	void intimateCommunicate(Agent a){

		// store the current time
		int time = (int) world.schedule.getTime();

		if (a == null)
			return;

		// if the Agent doesn't pick up, or has already moved, try to call someone else
		if (a.lastMove == time || world.random.nextDouble() < contact_success_prob)
			return;

		// given a successful point of contact: attempt to exchange all information
		exchangeAllInfo(a, time);

		// record this contact
		recentIntimateContact.put(a, time);
		a.recentIntimateContact.put(this, time);
		
		// update the contacted agent's activity, so that that Agent can't act twice in one tick
		a.lastMove = time;
	}
	
	/**
	 * Exchange all information in the Agent's set of knowledge with another Agent's knowledge set
	 * 
	 * @param a - the Agent with which the exchange is occuring
	 * @param time - the current time
	 */
	void exchangeAllInfo(Agent a, int time){

		// tell the other Agent about everything this Agent knows, with a certain probability of successfully communicating
		for(Object o: knowledge.keySet()){
			
			if(!a.knowledge.containsKey(o) && world.random.nextDouble() < communication_success_prob){
				// successful communication!
				
				if(o instanceof ListEdge)
					a.learnAbout(o, world.new RoadClosure(o, time, this));
				else
					a.learnAbout(o, new Information(o, time, this, knowledge.get(o).getValence()));
			}
		}
		
		// the other Agent tells this Agent about everything it knows, with a certain probability of successfully communicating
		for(Object o: a.knowledge.keySet()){

			if(!knowledge.containsKey(o)  && world.random.nextDouble() < communication_success_prob){
				// successful communication!
				if(o instanceof ListEdge)
					learnAbout(o, world.new RoadClosure(o, time, a));
				else
					learnAbout(o, new Information(o, time, a, a.knowledge.get(o).getValence()));
			}
		}
		
	}
	
	/**
	 * Add new information to the Agent's store of knowledge, and factor it into the Agent's stress level
	 */
	@Override
	public void learnAbout(Object o, Information info){
		knowledge.put(o, info);
		this.updateToStressLevel(info.getValence());
		
		if(info instanceof EvacuationOrder && !knowledge.containsKey(world.wildfire))
			learnAbout(world.wildfire, new Information(world.wildfire, info.getTime(), info.getSource(), 10));
	}
	
	/**
	 * Exchange information with other Agents in the same location
	 * 
	 * @return -1 if there is no one around with whom to communicate, 1 otherwise
	 */
	int localCommunicate(){

		// talk to everyone in the same place
		Bag immediateNeighbors = world.agentsLayer.getObjectsWithinDistance(this, Hotspots.resolution);
		
		if(immediateNeighbors.size() <= 1) // don't talk to self
			return -1; // no communication took place
		
		// store the current time
		int time = (int) world.schedule.getTime();

		// go through all neighbors and try to talk to them
		for(Object n: immediateNeighbors){
			if(n == this) continue;
			Agent a = (Agent) n;
			
			// given a successful point of contact: attempt to exchange all information
			exchangeAllInfo(a, time);

			// if this Agent is an intimate tie, no need to try to call them later
			if(intimateTies.containsKey(a)){
				recentIntimateContact.put(a, time);
				a.recentIntimateContact.put(this, time);
			}
		}		
		
		return 1; // successful communication!
	}

	/**
	 * Factor a new piece of information with a set amount of stress into the Agent's stress level

	 * @param valence - the degree to which the new information is stressful
	 */
	void updateToStressLevel(int valence){
		
		int time = (int)world.schedule.getTime();
		if(!sentimentSignal.containsKey(time)) // if no other information this tick, save it
			sentimentSignal.put(time, valence);
		else if(valence > sentimentSignal.get(time)) // else, if this is the most stressful info this tick, save it
			sentimentSignal.put(time, valence);
	}

	/**
	 * Communicate using social media
	 */
	void broadcastCommunciate(){

		// assemble a list of all pieces of information which have been shared since the last time the Agent checked in
		HashMap <Information, Communicator> infoToPosterMapping = new HashMap <Information, Communicator>();
		for(Communicator c: socialMediaTies){
			ArrayList csPosts = c.getInformationSince(lastSocialMediaCheckin);
			for(Object o: csPosts){
				Information info = (Information) o;
				if(infoToPosterMapping.containsKey(info.getInfo())) continue;
				infoToPosterMapping.put(info, c);
			}
		}
		
		int time = (int) world.schedule.getTime();

		// randomly consider 30 posts of this set of posts
		ArrayList <Information> foundPosts = new ArrayList <Information> (infoToPosterMapping.keySet());
		for(int i = 0; i < Math.min(1000, foundPosts.size()); i++){ // TODO: TAKE BACK OUT
			Information post = foundPosts.remove(world.random.nextInt(foundPosts.size()));

			Object postInfo = post.getInfo();
			
			// have the Agent learned a thing?
			if(! (postInfo instanceof Long || knowledge.containsKey(postInfo)) ){
				Information info;
				if(post instanceof RoadClosure)
					info = world.new RoadClosure(postInfo, time, infoToPosterMapping.get(post));
				else if(post instanceof EvacuationOrder)
					info = world.new EvacuationOrder(postInfo, time, infoToPosterMapping.get(post));
				else
					info = new Information(postInfo, time, infoToPosterMapping.get(post), post.getValence());

				// the Agent has learned a thing - save it
				learnAbout(postInfo, info);
			}
			
			// should the Agent retweet this thing?
			if(world.random.nextDouble() < retweet_prob){
				if(post instanceof RoadClosure)
					socialMediaPosts.put(postInfo, world.new RoadClosure(postInfo, time, infoToPosterMapping.get(post)));
				else
					socialMediaPosts.put(postInfo, new Information(postInfo, time, infoToPosterMapping.get(post), post.getValence()));
			}			
		}

		// check to see if there's anything this Agent knows about which it has not previously posted about
		Object toPost = null;
		Iterator <Object> iter = knowledge.keySet().iterator();
		while(iter.hasNext() && toPost == null){
			Object o = iter.next();
			if(!socialMediaPosts.containsKey(o))
				toPost = o;
		}
		
		// if this Agent has something to tweet about, tweet it!
		if(toPost != null){
			Information info = knowledge.get(toPost);
			if(info instanceof RoadClosure)
				socialMediaPosts.put(info.getInfo(), world.new RoadClosure(info.getInfo(), time, info.getSource()));
			else
				socialMediaPosts.put(info.getInfo(), new Information(info.getInfo(), time, info.getSource(), info.getValence()));
		}
		
		// if nothing exists to tweet about, with some probability tweet a random number to
		// reflect white noise
		else if(toPost == null && world.random.nextDouble() < tweet_prob){
			long chatter = world.random.nextLong();
			socialMediaPosts.put(chatter, new Information(chatter, time, null, world.random.nextInt(5))); 
			// nothing more alarming than a road closure in random chatter!
		}
	
		// record that the Agent has checked into social media
		lastSocialMediaCheckin = time;
	}
	

	/**
	 * Set up a course to take the Agent to the given coordinates
	 * 
	 * @param place - the target destination
	 * @return 1 for success, -1 for a failure to find a path, -2 for failure based on the provided destination or current position
	 */
	int headFor(Coordinate place, Network roadNetwork) {

		// first, record from where the agent is starting
		startPoint = this.geometry.getCoordinate();
		goalPoint = null;

		// if the current node and the current edge don't match, there's a problem with the Agent's understanding of its
		// current position
		if(!(edge.getTo().equals(node) || edge.getFrom().equals(node))){
			System.out.println( (int)world.schedule.getTime() + "\t" + this.myID + "\tMOVE_ERROR_mismatch_between_current_edge_and_node");
			return -2;
		}

		// FINDING THE GOAL //////////////////

		// set up goal information
		targetDestination = this.snapPointToRoadNetwork(place);
		
		GeoNode destinationNode = world.getClosestGeoNode(targetDestination);//place);
		if(destinationNode == null){
			System.out.println((int)world.schedule.getTime() + "\t" + this.myID + "\tMOVE_ERROR_invalid_destination_node");
			return -2;
		}

		// be sure that if the target location is not a node but rather a point along an edge, that
		// point is recorded
		if(destinationNode.geometry.getCoordinate().distance(targetDestination) > Hotspots.resolution)
			goalPoint = targetDestination;
		else
			goalPoint = null;


		// FINDING A PATH /////////////////////

		if(path == null)
			path = pathfinder.astarPath(node, destinationNode, roadNetwork);

		// if it fails, give up
		if (path == null){
			return -1;
		}

		// CHECK FOR BEGINNING OF PATH ////////

		// we want to be sure that we're situated on the path *right now*, and that if the path
		// doesn't include the link we're on at this moment that we're both
		// 		a) on a link that connects to the startNode
		// 		b) pointed toward that startNode
		// Then, we want to clean up by getting rid of the edge on which we're already located

		// Make sure we're in the right place, and face the right direction
		if (edge.getTo().equals(node))
			direction = 1;
		else if (edge.getFrom().equals(node))
			direction = -1;
		else {
			System.out.println((int)world.schedule.getTime() + "\t" + this.myID + "MOVE_ERROR_mismatch_between_current_edge_and_node_2");
			return -2;
		}

		// reset stuff
		if(path.size() == 0 && targetDestination.distance(geometry.getCoordinate()) > world.resolution){
			path.add(edge);
			node = (GeoNode) edge.getOtherNode(node); // because it will look for the other side in the navigation!!! Tricky!!
		}

		// CHECK FOR END OF PATH //////////////

		// we want to be sure that if the goal point exists and the Agent isn't already on the edge 
		// that contains it, the edge that it's on is included in the path
		if (goalPoint != null) {// && path.size() > 0) {

			ListEdge myLastEdge = world.getClosestEdge(goalPoint);
			
			if(myLastEdge == null){
				System.out.println((int)world.schedule.getTime() + "\t" + this.myID + "\tMOVE_ERROR_goal_point_is_too_far_from_any_edge");
				return -2;
			}
			
			// make sure the point is on the last edge
			Edge lastEdge;
			if (path.size() > 0)
				lastEdge = path.get(0);
			else
				lastEdge = edge;

			Point goalPointGeometry = world.fa.createPoint(goalPoint);
			if(!lastEdge.equals(myLastEdge) && ((MasonGeometry)lastEdge.info).geometry.distance(goalPointGeometry) > Hotspots.resolution){
				if(lastEdge.getFrom().equals(myLastEdge.getFrom()) || lastEdge.getFrom().equals(myLastEdge.getTo()) 
						|| lastEdge.getTo().equals(myLastEdge.getFrom()) || lastEdge.getTo().equals(myLastEdge.getTo()))
					path.add(0, myLastEdge);
				else{
					System.out.println((int)world.schedule.getTime() + "\t" + this.myID + "\tMOVE_ERROR_goal_point_edge_is_not_included_in_the_path");
					return -2;
				}
			}
			
		}

		// set up the coordinates
		this.startIndex = segment.getStartIndex();
		this.endIndex = segment.getEndIndex();

		return 1;
	}


	/**
	 * Rank the different points of egress based on the qualities of the places accessible from them;
	 * in decreasing order of preference, check to see whether there is an accessible path to that
	 * point. If so, take it.
	 */
	void evacuate(){
		
		// update activity
		this.addIntegerAttribute("Evacuating", 1);	
		currentActivity = activity_evacuate;
		
		// ensure that the Agent will act in the next step
		//world.schedule.scheduleOnce(this);
		stopper = world.schedule.scheduleRepeating(this, 50, 1);

		// assemble the list of known information about threats
		ArrayList <Geometry> threats = new ArrayList <Geometry> ();
		for(Object o: knowledge.keySet()){
			if(o instanceof Wildfire) threats.add(((Wildfire)o).extent);
			else if(o instanceof Coordinate) threats.add(world.fa.createPoint((Coordinate)o));
		}
		
		// setting up holders for data
		LineString line = null;
		Coordinate here = this.geometry.getCoordinate();
		HashMap <GeoNode, Double> accessibleNodes = new HashMap <GeoNode, Double> ();
		
		// for each of the potential terminus points, determine whether reaching them would
		// bring the Agent in contact with any of the known threats
		for (Object o : world.terminus_points) {
			
			GeoNode potentialEscapeNode = (GeoNode) o;
			double dist = potentialEscapeNode.geometry.distance(this.geometry);
			Coordinate escape = potentialEscapeNode.geometry.getCoordinate();
			
			// create a line between the Agent and the potential escape point, and see
			// if it intersects any of the threat geometries
			line = world.fa.createLineString(new Coordinate[] { here, escape });
			boolean crosses = false;
			for (Geometry threat : threats)
				if (line.crosses(threat))
					crosses = true;
			
			// if no threats lie between the Agent and the endpoint, add the node for consideration
			if (!crosses) {
				accessibleNodes.put(potentialEscapeNode, dist);
			}
		}
		
		// if no acceptable terminus nodes exist, the Agent is stuck and must take secondary measures
		if(accessibleNodes.size() == 0){
			path = null; // TODO: secondary measures
			return; //nothing good here!
		}

		// otherwise, go through the potential nodes and pick the closest terminus point that is
		// accessible by the road network
		path = null;
		GeoNode goalNode = null;
		while(path == null && accessibleNodes.size() > 0){
			
			double minDist = Double.MAX_VALUE;
			for(GeoNode n: accessibleNodes.keySet())
				if(accessibleNodes.get(n) < minDist){
					minDist = accessibleNodes.get(n);
					goalNode = n;
				}
			
			accessibleNodes.remove(goalNode);
			
			headFor(goalNode.geometry.getCoordinate(), familiarRoadNetwork);
			if(path == null)
				headFor(goalNode.geometry.getCoordinate(), world.roads);
		}
		
		// 	TODO: "pick up kids at school, partner at home" if appropriate
		if(path != null){
			int success = navigate(Hotspots.resolution);
		}
		else {
			node = (GeoNode) edge.getOtherNode(node);
		}
	}
	
	/**
	 * Check the tile in which this Agent finds itself to see if there's something on fire near it
	 * 
	 * (Could easily be extended to check for other things as well)
	 */
	void observe(){
		if(removed == true) return;
		// make sure Agent is not caught in the fire
		else if (world.wildfire != null && world.wildfire.extent.contains(this.geometry)){
			removeMe();
			world.numDied++;
			alive = false;
			System.out.println((int)world.schedule.getTime() + "\t" + this.myID + "\tDIED");
			return;
		}

		// update stress
		int sentiment = 0;
		int time = (int)world.schedule.getTime();
		for(Integer i: sentimentSignal.keySet()){
			sentiment += Math.pow((time - i)/(double)sentimentSignal.get(i), decayParam);
		}
		this.stress = Math.min(Math.max(0,Math.log(sentiment)), 10);
		
		// look around
		if(world.wildfire == null) return; // a cheap way to avoid the cost of a lookup
		else if(knowledge.containsKey(world.wildfire)) return;
		
		// if there is indeed a wildfire within the observation distance, observe it!
		if(world.wildfire != null && this.geometry.distance(world.wildfire.extent) < observationDistance){

			// record observation of the wildfire
			learnAbout(world.wildfire, new Information(world.wildfire, (int) world.schedule.getTime(), null, 10));
			
			System.out.println((int)world.schedule.getTime() + "\t" + this.myID + "\tOBSERVED_THREAT");

			// update knowledge of roads: not going to take any roads that are within the wildfire!
			Bag b = world.networkEdgeLayer.getObjectsWithinDistance(world.wildfire.extent, world.resolution);

			// remove these firey roads from the set of considered paths
			for(Object o: b){
				MasonGeometry mg = (MasonGeometry) o;
				if(world.wildfire.extent.contains(mg.geometry)){
					Object badEdge = ((AttributeValue)(mg.getAttribute("ListEdge"))).getValue();
					familiarRoadNetwork.removeEdge((ListEdge) badEdge);
					learnAbout(badEdge, world.new RoadClosure(badEdge, time, null));
				}
			}
			
			// if Agent was traveling, make sure that Agent's path is still valid given new knowledge
			if(this.currentActivity == this.activity_travel){
				if(! NetworkUtilities.validPath(path, this.familiarRoadNetwork)){
					path = null;
					headFor(targetDestination, familiarRoadNetwork);
				}
					
			}
		}
	}
		
	////////////////////////////////////////////////////////////////////////////////////////////////
	/////// end METHODS ////////////////////////////////////////////////////////////////////////////
	////////////////////////////////////////////////////////////////////////////////////////////////

	
	////////////////////////////////////////////////////////////////////////////////////////////////
	/////// UTILITIES //////////////////////////////////////////////////////////////////////////////
	////////////////////////////////////////////////////////////////////////////////////////////////
	
	/**
	 * Determines the distance from the given location to the nearest threat of which 
	 * the Agent knows.
	 * @param location - the location which is being compared against possible threats
	 * @return the distance to the nearest threat
	 */
	double distanceToThreat(Geometry location){
		double mindist = Double.MAX_VALUE;
		for(Object o: knowledge.keySet()){
			double dist;
			if(o instanceof Wildfire)
				dist = ((Wildfire)o).extent.distance(location);
			else if(o instanceof Coordinate)
				dist = ((Coordinate)o).distance(location.getCoordinate());
			else // something has gone wrong
				dist = Double.MAX_VALUE;
			if(dist < mindist) mindist = dist;
		}
		return mindist;
	}
	
	/**
	 * Add a new intimate contact
	 * 
	 * @param contact
	 * @param weight
	 */
	public void addContact(Agent contact, int weight){
		intimateTies.put(contact, weight);
		recentIntimateContact.put(contact, (int) world.schedule.getTime());
	}

	/**
	 * Add a new social media contact
	 * 
	 * @param contact
	 */
	public void addSocialMediaContact(Communicator contact){
		socialMediaTies.add(contact);
	}
	
	/**
	 * Snap the coordinate to the nearest point on the road network
	 * 
	 * @param c - the point in question
	 * @return - nearest point on the road network
	 */
	public Coordinate snapPointToRoadNetwork(Coordinate c){
		ListEdge myEdge = null;
		double resolution = Hotspots.resolution;
		
		// if the network hasn't been properly set up, don't try to find something on it =\
		if(world.networkEdgeLayer.getGeometries().size() == 0) 
			return null;
		
		// while there's no edge, expand outward until the Agent finds one
		while(myEdge == null){
			myEdge = world.getClosestEdge(c, resolution);
			resolution *= 10;
		}
		
		// having found a line, find the index of the point on that line
		LengthIndexedLine closestLine = new LengthIndexedLine((LineString) (((MasonGeometry)myEdge.info).getGeometry()));
		double myIndex = closestLine.indexOf(c);
		return closestLine.extractPoint(myIndex);
	}
	
	/**
	 * Get all social media posts the Agent has generated since the given timestep
	 */
	@Override
	public ArrayList getInformationSince(double time) {
		ArrayList <Object> result = new ArrayList <Object> ();
		for(Object o: socialMediaPosts.keySet()){
			Information i = socialMediaPosts.get(o);
			if(i.getTime() >= time)
				result.add(i);
		}
		return result;
	}

	/**
	 * Comparator
	 */
	public boolean equals(Object o){
		if(!(o instanceof Agent)) return false;
		else 
			return ((Agent)o).myID.equals(myID);
	}
	
	/** HashCode */
	public int hashCode(){ return myID.hashCode(); }

	public String toString(){ return myID; }
	
	// GETTERS
	public Coordinate getHome(){ return home; }
	public Coordinate getWork(){ return work; }
	public int getActivity(){ return this.currentActivity; }
	public double getValence(){ return this.stress; }
	
	/**
	 * Generates default paths between home and work, to ease on computation during the actual run
	 */
	public void setupPaths(){
		if(work != null){
			GeoNode workNode = world.getClosestGeoNode(this.work);
			GeoNode homeNode = world.getClosestGeoNode(this.home);

			ArrayList <Edge> pathFromHomeToWork = pathfinder.astarPath(homeNode, workNode, world.roads);
			this.familiarPaths.add(pathFromHomeToWork);
			
			ArrayList <Edge> pathFromWorkToHome = pathfinder.astarPath(workNode, homeNode, world.roads);
			this.familiarPaths.add(pathFromWorkToHome);
		}

	}
	
	/**  Wrapper around step, so that it can be called from other functions */
	void stepWrapper(){ this.step(world); }

	////////////////////////////////////////////////////////////////////////////////////////////////
	/////// end UTILITIES //////////////////////////////////////////////////////////////////////////
	////////////////////////////////////////////////////////////////////////////////////////////////
 
}
