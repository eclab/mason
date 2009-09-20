/*
 * Bee foraging simulation. Copyright by Joerg Hoehne.
 * For suggestions or questions email me at hoehne@thinktel.de
 */

package de.thinktel.foragingBee.simulation;

import java.awt.Color;
import java.util.Random;

import javax.vecmath.Point3d;
import javax.vecmath.Tuple3d;
import javax.vecmath.Vector3d;

import sim.engine.Steppable;
import de.thinktel.foragingBee.masonGlue.ForagingHoneyBeeSimulation;
import de.thinktel.foragingBee.masonGlue.IAgentVisualization;
import de.thinktel.foragingBee.masonGlue.IIterationAgent;
import de.thinktel.utils.Filter;
import de.thinktel.utils.Geometric;
import de.thinktel.utils.J3dPolar;

/**
 * An abstract class for a moving object. Used by several subclasses that are
 * locating and moving objects in space. Every agent has an orientation (given
 * in radians) and a velocity (given as a vector but heading in the same
 * direction as the orientation).
 * <p>
 * Changes:
 * <ul>
 * <li>20090901: The agent is no more inherited from any MASON object (but still
 * supports the {@link Steppable} interface) and does not support directly
 * visualization. Instead the agent holds a visualization object {@link #visual}
 * that will be used by the simulation environment for displaying this agent.</li>
 * <li>20090901: Removed the color attribute because the color is part of the
 * visualization rather than the moving agent.</li>
 * <li>20090908: All angular methods are now working with radians.</li>
 * <li>20090918: Added the flag {@link #is3dMode} and its according methods.</li>
 * <li>20090920: Changed some methods to behave accordingly to the
 * {@link #is3dMode} flag.</li>
 * </ul>
 * <p>
 * Copyright 2009 Joerg Hoehne
 * 
 * @author hoehne (<a href="mailto:hoehne@thinktel.de">J&ouml;rg H&ouml;hne</a>)
 * 
 */
public abstract class AbstractMovingAgent implements IIterationAgent,
		IVisualAgent {
	/**
	 * Generated serial version id.
	 */
	private static final long serialVersionUID = 7815716363922534915L;

	/**
	 * The random number generator for all agents.
	 */
	static public Random r = new Random();

	/**
	 * Determine if this agent is running in a 3D simulation environment. Some
	 * methods may behave differently according to this flag.
	 */
	private boolean is3dMode = false;

	/**
	 * The object that is used for visualizing this agent. The simulation
	 * environment is not known so no knowledge of the type is given.
	 */
	IAgentVisualization visual;

	/**
	 * Information to access the scheduler for this agent.
	 */
	Object schedulerInformation;

	/**
	 * The simulation the agent is dealing with.
	 */
	private ForagingHoneyBeeSimulation simulation;

	/**
	 * The current location of the agent.
	 */
	private Point3d location = new Point3d();

	/**
	 * The current speed vector of the agent.
	 */
	private Vector3d velocity = new Vector3d();

	/**
	 * This variable contains the heading. The heading itself contains the
	 * orientation (azimuth) that the angle in radians inside the xy-plane and
	 * the elevation that is the angle between the positive x-axis and the
	 * projection of the vector onto the xy-plane.
	 */
	private J3dPolar heading = new J3dPolar();

	/**
	 * The size of the agent.
	 */
	private double size;

	/**
	 * The radius of the object.
	 */
	private double radius;

	/**
	 * The constructor of the agent.
	 * 
	 * @param simulation
	 *            The simulation the agent resides in.
	 * @param is3dMode
	 *            Determine if the agent is running in a 3d simulation
	 *            environment (if it is set to true).
	 * @param location
	 *            The current location of the agent.
	 * @param velocity
	 *            The current velocity of the agent.
	 * @param size
	 *            The size (diameter) of the agent.
	 * @param color
	 *            The color of the agent.
	 */
	public AbstractMovingAgent(ForagingHoneyBeeSimulation simulation,
			boolean is3dMode, Point3d location, Vector3d velocity, double size,
			Color color) {
		setIs3dMode(is3dMode);
		/*
		 * Set the values which will not affect any other classes.
		 */
		setSize(size);
		/*
		 * store the simulation
		 */
		this.simulation = simulation;
		/*
		 * set the visualization object for this agent
		 */
		this.setVisualizationObject(this.simulation.createVisual(this));
		this.getVisualizationObject().setColor(color);
		/*
		 * The location and velocity may affect the visualization by calling
		 * updateLocation() so the visualization has to be initialized first.
		 */
		setLocation(location);
		setVelocityVector(velocity);
	}

	/**
	 * Get the simulation this agent resides in.
	 * 
	 * @return The reference to the current simulation.
	 */
	public final ForagingHoneyBeeSimulation getSimulation() {
		return simulation;
	}

	/**
	 * Return true if this agent is running in a 3d simulation environment
	 * (basically the agent thinks it is running in such an environment).
	 * 
	 * @return True, if the agent is running in a 3d simulation environment.
	 */
	public final boolean is3dMode() {
		return is3dMode;
	}

	/**
	 * Set the flag if the agent is running in a 3d simulation environment.
	 * 
	 * @param value
	 *            True, if the agent is running in a 3d simulation environment;
	 *            false otherwise.
	 */
	private final void setIs3dMode(boolean value) {
		is3dMode = value;
	}

	/**
	 * Return the sphere radius the agent fills in.
	 * 
	 * @return The radius of the sphere.
	 */
	public double getSphereRadius() {
		return radius;
	}

	/**
	 * Return the size of the agent.
	 * 
	 * @return The size (diameter) of the agent.
	 */
	public double getSize() {
		return this.size;
	}

	/**
	 * Set the size of the agent.
	 * 
	 * @param size
	 *            The size of the agent.
	 */
	public void setSize(double size) {
		this.size = size;
		this.radius = size / 2;
	}

	/**
	 * Get the current heading of the agent.
	 * 
	 * @return the heading
	 */
	public final J3dPolar getHeading() {
		return heading;
	}

	/**
	 * Set the current heading of the agent.
	 * 
	 * @param heading
	 *            the heading to set
	 */
	public final void setHeading(J3dPolar heading) {
		this.heading = heading;
	}

	/**
	 * Return the reference to the agent's location.
	 * 
	 * @return The agent's location reference.
	 */
	public Point3d getLocation() {
		return location;
	}

	/**
	 * Set the location of this agent. Setting the location will cause a visual
	 * update by calling {@link #updateLocation()}. If {@link #is3dMode} is
	 * false the z coordinate will be set to 0.
	 * 
	 * @param x
	 *            The x value on the x-axis.
	 * @param y
	 *            The y value on the y-axis.
	 * @param z
	 *            The z value on the z-axis.
	 * 
	 */
	public void setLocation(double x, double y, double z) {
		if (is3dMode)
			this.location.set(x, y, z);
		else {
			this.location.set(x, y, 0);
		}
		updateLocation();
	}

	/**
	 * Set the location of this agent by calling
	 * {@link #setLocation(double, double, double).}
	 * 
	 * @param location
	 *            The location of this agent.
	 */
	public void setLocation(Tuple3d location) {
		setLocation(location.x, location.y, location.z);
	}

	/**
	 * Returns the vector with the velocity.
	 * 
	 * @return The reference to the agent's current velocity vector.
	 */
	public final Vector3d getVelocityVector() {
		return velocity;
	}

	/**
	 * Set the velocity vector to a speed meaning setting the vector to a given
	 * length.
	 * 
	 * @param speed
	 */
	public final void setVelocity(double speed) {
		this.velocity.normalize();
		this.velocity.scale(speed);
		this.heading.set(velocity);
	}

	/**
	 * Set the velocity vector. Setting the vector will also cause a change in
	 * the current orientation according to the velocity vector settings.
	 * 
	 * @param x
	 *            The velocity value for the x-axis.
	 * @param y
	 *            The velocity value for the y-axis.
	 * @param z
	 *            The velocity value for the z-axis.
	 */
	public final void setVelocityVector(double x, double y, double z) {
		velocity.set(x, y, z);
		heading.set(velocity);
	}

	/**
	 * Copies the field of the input vector in to current velocity vector.
	 * 
	 * @param v
	 *            The new velocity vector.
	 */
	public final void setVelocityVector(Vector3d v) {
		setVelocityVector(v.x, v.y, v.z);
	}

	/**
	 * Compute the direction (including distance) from agent1 to agent2 in
	 * radians.
	 * 
	 * @param agent1
	 *            The first agent the orientation is computed from.
	 * @param agent2
	 *            The second agent the orientation is computed to.
	 * @return The direction from agent1 to agent2.
	 */
	static final J3dPolar directionTo(IMovingAgent agent1, IMovingAgent agent2) {
		return J3dPolar.createFrom(agent1.getLocation(), agent2.getLocation());
	}

	/**
	 * Return the distance to an other agent.
	 * 
	 * @param agent
	 *            The other agent.
	 * @return The distance.
	 */
	public final double distance(IMovingAgent agent) {
		return distance(agent.getLocation());
	}

	/**
	 * Return the distance to an other point.
	 * 
	 * @param p
	 *            The point the distance is calculated to.
	 * @return The distance.
	 */
	public final double distance(Point3d p) {
		return location.distance(p);
	}

	/**
	 * Return the squared distance to an other agent. This method saves some
	 * execution time by not applying the square root so a squared distance is
	 * returned. This method calls {@link #distanceSquared(Point3d)}.
	 * 
	 * @param agent
	 *            The other agent.
	 * @return The squared distance.
	 */
	public final double distanceSquared(IMovingAgent agent) {
		return distanceSquared(agent.getLocation());
	}

	/**
	 * Return the squared distance to the given point. This method saves some
	 * execution time by not applying the square root so a squared distance is
	 * returned.
	 * 
	 * @param p
	 *            The point.
	 * @return The squared distance.
	 */
	public final double distanceSquared(Point3d p) {
		return location.distanceSquared(p);
	}

	/**
	 * Move the agent in the direction given by the provided vector. If
	 * {@link #is3dMode} is false the z coordinate will be set to 0.
	 * 
	 * @param direction
	 *            The direction and speed as a vector.
	 */
	public final void forward(Vector3d direction) {
		location.add(direction);
		if (!is3dMode)
			location.z = 0.0d;
		updateLocation();
	}

	/**
	 * Move the agent in the current direction given by {@link #velocity}.
	 */
	public final void forward() {
		forward(velocity);
	}

	/**
	 * Return the objects within the distance of this object. Due to the
	 * dimension of an object the sphere of both objects might be included in
	 * the computation.
	 * 
	 * @param radius
	 *            The distance we are looking for.
	 * @param useMySphere
	 *            Use this agents sphere for reducing the distance between this
	 *            agent and others
	 * @param useTheirSpheres
	 *            Use the others agents spheres to reduce the distance
	 * @param includeMySelf
	 *            Shall the agent itself be included in the results?
	 * @return The objects meeting the criteria.
	 */
	public IMovingAgent[] getObjectsWithinMyDistance(double radius,
			boolean useMySphere, boolean useTheirSpheres, boolean includeMySelf) {

		return getObjectsWithinMyDistance(radius, useMySphere, useTheirSpheres,
				getSimulation().getMaxSphereRadius(), includeMySelf, null);
	}

	/**
	 * This method returns all objects that are within a radius of the this
	 * object. The computation is done in several stages and configurations.
	 * First the distance between objects (agents) is measured from center to
	 * center. The center is a point so it has no dimension.<br>
	 * The parameter radius controls the radius from the center of the current
	 * object (agent) where the center of the other objects (agents) have to lie
	 * within to be returned.<br>
	 * Every agent in the simulation has a sphere given by a radius. This radius
	 * can be used to compute if a center of an other objects lies inside the
	 * sphere. This behaviour is configured by the parameter useMySphere. The
	 * usage of the sphere of the other object (agent) instead of its center is
	 * controlled by useTheirSpheres.<br>
	 * The parameter maxSphere defines the maximum radius of all other agents
	 * used for computing the distance in which one object (agent) have to be in
	 * maximum to be returned.<br>
	 * 
	 * @param radius
	 *            The radius where to look for other objects (agents).
	 * @param useMySphere
	 *            Is the center or sphere of an other object inside by sphere.
	 * @param useTheirSpheres
	 *            Use the sphere of the other object instead of the center
	 *            point.
	 * @param maxSphere
	 *            The maximum sphere of all other objects (agents).
	 * @param includeMySelf
	 *            If the returned objects shall include this object (agent).
	 * @param type
	 *            If this type is not null only objects (agents) of the defined
	 *            type (or subclass) are returned.
	 * @return The objects meeting the criteria.
	 */
	public IMovingAgent[] getObjectsWithinMyDistance(double radius,
			boolean useMySphere, boolean useTheirSpheres, double maxSphere,
			boolean includeMySelf, Class<?> type) {
		double distanceCorrection = 0;
		double correction = 0;

		if (useMySphere) {
			distanceCorrection += this.getSphereRadius();
			correction = distanceCorrection;
		}

		if (useTheirSpheres)
			distanceCorrection += maxSphere;

		// get all objects which centers lies within a certain distance
		Object[] objects = simulation.getObjectsWithinDistance(this, radius
				+ distanceCorrection);

		if (type != null)
			objects = Filter.filter(objects, type);

		IMovingAgent agent;
		int i, k;
		for (i = 0, k = 0; i < objects.length; i++) {
			agent = (IMovingAgent) objects[i];

			// continue if the agent itself is included or the agent is not
			// myself
			if (includeMySelf || (agent != this)) {
				// get the distance from center to center
				double dist = this.distance(agent);
				// correct the distance according to the spheres
				dist -= correction;
				if (useTheirSpheres)
					dist -= agent.getSphereRadius();

				if (dist < radius) {
					objects[k] = agent;
					k++;
				}
			}
		}

		IMovingAgent[] neighbours = new IMovingAgent[k];
		System.arraycopy(objects, 0, neighbours, 0, k);

		return neighbours;
	}

	/**
	 * Head to the direction of the given agent.
	 * 
	 * @param agent
	 *            The agent this agent has to head to.
	 */
	public final void headTo(IMovingAgent agent) {
		headTo(agent.getLocation());
	}

	/**
	 * Head to the direction of the given point.
	 * 
	 * @param p
	 *            The point this agent has to head to.
	 */
	public final void headTo(Tuple3d p) {
		J3dPolar direction = J3dPolar.createFrom(this.getLocation(), p);
		turnTo(direction.azimuth, direction.elevation);
	}

	/**
	 * Compute if the location of an agent is inside the sphere of this agent.
	 * The outer sphere of the agent is not considered, only the center of the
	 * agent.
	 * 
	 * @param agent
	 *            The agent whose sphere will be tested.
	 * @return True, if this agent is inside the other agent's sphere.
	 */
	public final boolean isInSphere(IMovingAgent agent) {
		return isInSphere(agent, false);
	}

	/**
	 * Compute if the location of an agent if it is inside the sphere of this
	 * agent. The consideration of the other agents sphere is optional. If the
	 * sphere is used for computation false is returned if the sphere of the
	 * agent is (partially) out of this sphere.
	 * 
	 * @param agent
	 *            The agent whose sphere will be tested.
	 * @param useSphere
	 *            If true the sphere of this agent will also used to decide if
	 *            this agent's sphere intersects with the other agent's sphere.
	 * @return True, if this agent is inside the other agent's sphere.
	 */
	public final boolean isInSphere(IMovingAgent agent, boolean useSphere) {
		return (distanceToSphere(agent, true) <= this.getSphereRadius());
	}

	/**
	 * Compute the distance of the given agent to the outer sphere perimeter of
	 * this agent. The sphere perimeter of the agent also can be used otherwise
	 * the center.
	 * 
	 * @param agent
	 *            The other agent.
	 * @param useSphere
	 *            If true use the outer perimeter of the other agent.
	 * @return The distance to the other agent's sphere.
	 */
	public final double distanceToSphere(IMovingAgent agent, boolean useSphere) {
		double d = distance(agent);
		if (useSphere)
			d += agent.getSphereRadius();
		return d;
	}

	/**
	 * Turn the agent by the given angle in radians. This method will call
	 * {@link #turnTo(double, double)} so the checking for 2d/3d mode has to be
	 * done in {@link #turnTo(double, double)}.
	 * 
	 * @param dAzimuth
	 *            The angle in radians the agent will be turned.
	 * @param dElevation
	 *            The angle in radians the agent will be raised for elevation.
	 */
	public final void turnBy(double dAzimuth, double dElevation) {
		turnTo(heading.azimuth + dAzimuth, heading.elevation + dElevation);
	}

	/**
	 * Turn the agent to the given angle given in radians.
	 * 
	 * @param azimuth
	 *            The angle (azimuth) in radians the agent will head to.
	 * @param elevation
	 *            The angle in radians the agent will elevate from the xy-pane.
	 */
	public final void turnTo(double azimuth, double elevation) {
		if (!is3dMode)
			elevation = 0.0d;
		heading.azimuth = Geometric.clampAngleRadians(azimuth);
		heading.elevation = Geometric.clampAngleRadians(elevation);
		velocity.set(heading.toCartesian());
	}

	/**
	 * Update the location of the agent using the simulation framework.
	 */
	final private void updateLocation() {
		simulation.updateLocation(this);
	}

	/**
	 * Calculate the vector from this agent to the provided one.
	 * 
	 * @param agent
	 *            The other the computed vector will head to.
	 * @return The vector heading to the other agent.
	 */
	public final Vector3d vectorTo(IMovingAgent agent) {
		Vector3d v = new Vector3d();
		v.sub(agent.getLocation(), location);

		return v;
	}

	// ================== IVisualAgent ====================

	/**
	 * Return the object that will be used for visualizing the this agent. The
	 * type of the object is not known due to different simulation environments
	 * so an interface {@link IAgentVisualization} is returned.
	 * 
	 * @return The visualization object.
	 */
	public IAgentVisualization getVisualizationObject() {
		return visual;
	}

	/**
	 * Set the object that will be used for visualizing the this agent. The type
	 * of the object is not known due to different simulation environments so an
	 * interface {@link IAgentVisualization} is used.
	 * 
	 * @param visual
	 *            The object used for visualization.
	 */
	public void setVisualizationObject(IAgentVisualization visual) {
		this.visual = visual;
	}

	// ================== IIterationAgent ====================

	/**
	 * Return the information a simulation environment needs to access
	 * information to the scheduler regarding this agent.
	 * 
	 * @return The information for the scheduler.
	 */
	public Object getSchedulerInformation() {
		return schedulerInformation;
	}

	/**
	 * Set information a simulation environment needs to access information to
	 * the scheduler regarding this agent.
	 * 
	 * @param o
	 */
	public void setSchedulerInformation(Object o) {
		schedulerInformation = o;
	}

	// =======================================

	/**
	 * Method for easy setting of the color of this agent. This method calls the
	 * color setting method of the visualization object.
	 * 
	 * @param color
	 *            The new color of the agent.
	 */
	public final void setColor(Color color) {
		visual.setColor(color);
	}
}
