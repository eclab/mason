/*
 * Bee foraging simulation. Copyright by Joerg Hoehne.
 * For suggestions or questions email me at hoehne@thinktel.de
 */

package foragingBee;

import java.awt.Color;
import java.util.Random;

import javax.vecmath.Point3d;
import javax.vecmath.Tuple3d;
import javax.vecmath.Vector3d;

import masonGlue.ForagingHoneyBeeSimulation;
import sim.engine.Steppable;
import sim.portrayal.SimplePortrayal2D;
import utils.Filter;
import utils.Geometric;

/**
 * An abstract class for a moving object. Used by several subclasses that are
 * locating and moving objects in space. Every agent has an orientation (given
 * in degrees) and a velocity (given as a vector but heading in the same
 * direction as the orientation).
 * <p>
 * Copyright 2009 Joerg Hoehne
 * 
 * @author hoehne (<a href="mailto:hoehne@thinktel.de">J&ouml;rg H&ouml;hne</a>)
 * 
 */
public abstract class AbstractMovingAgent extends SimplePortrayal2D implements
		Steppable, IMovingAgent {
	/**
	 * The random number generator for all agents.
	 */
	static public Random r = new Random();

	/**
	 * The simulation the agent is dealing with.
	 */
	private ForagingHoneyBeeSimulation simulation;

	/**
	 * The current location of the agent.
	 */
	private Point3d location = new Point3d();

	/**
	 * The current speed of the agent.
	 */
	private Vector3d velocity = new Vector3d();

	/**
	 * The orientation of the agent.
	 */
	private double orientation = 0;

	/**
	 * The color of the moving object.
	 */
	private Color color;

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
			Point3d location, Vector3d velocity, double size, Color color) {
		this.simulation = simulation;
		setLocation(location);
		setVelocityVector(velocity);
		setColor(color);
		setSize(size);
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
	 * Return the agents color.
	 * 
	 * @return The color of the agent.
	 */
	public Color getColor() {
		return color;
	}

	/**
	 * Set the color of the agent.
	 * 
	 * @param theColor
	 *            The color of the agent.
	 */
	public void setColor(Color theColor) {
		color = theColor;
	}

	/**
	 * Return the current orientation in degrees.
	 * 
	 * @return The orientation in degrees.
	 */
	public double getOrientation() {
		return orientation;
	}

	/**
	 * Set the orientation in degrees.
	 * 
	 * @param orientation
	 *            The orientation in degrees.
	 */
	public void setOrientation(double orientation) {
		turnTo(orientation);
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
	 * update by calling {@link #updateLocation()}.
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
		this.location.set(x, y, z);
		updateLocation();
	}

	/**
	 * Set the location of this agent. Setting the location will cause a visual
	 * update by calling {@link #updateLocation()}.
	 * 
	 * @param location
	 *            The location of this agent.
	 */
	public void setLocation(Tuple3d location) {
		this.location.set(location);
		updateLocation();
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
		orientation = Math.toDegrees(Geometric.angle(velocity));
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
	 * Compute the orientation from agent1 to agent2 in radians.
	 * 
	 * @param agent1
	 *            The first agent the orientation is computed from.
	 * @param agent2
	 *            The second agent the orientation is computed to.
	 * @return The orientation from agent1 to agent2 in radians.
	 */
	static final double orientation(IMovingAgent agent1, IMovingAgent agent2) {
		return Geometric.angle(agent1.getLocation(), agent2.getLocation());
	}

	/**
	 * Return the distance to an other agent.
	 * 
	 * @param agent
	 *            The other agent
	 * @return The distance.
	 */
	public final double distance(IMovingAgent agent) {
		return location.distance(agent.getLocation());
	}

	/**
	 * Return the squared distance to an other agent. This method saves some
	 * execution time by not applying the square root so a squared distance is
	 * returned.
	 * 
	 * @param agent
	 * @return The squared distance.
	 */
	public final double distanceSquared(IMovingAgent agent) {
		return location.distanceSquared(agent.getLocation());
	}

	/**
	 * Move the agent in the direction given by the provided vector.
	 * 
	 * @param direction
	 *            The direction and speed as a vector.
	 */
	public final void forward(Vector3d direction) {
		location.add(direction);
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
	 * Return the angle between this agent and the given one. This is the
	 * absolute angle between both points represented by the agents. No
	 * orientation is attended.
	 * 
	 * @param agent
	 *            The other agent.
	 * @return The angle in radians.
	 */
	public double angle(IMovingAgent agent) {
		return Geometric.angle(this.location, agent.getLocation());
	}

	/**
	 * Return the angle between this agent and the given location. This is the
	 * absolute angle between both points represented by the agent and the
	 * point. No orientation is attended.
	 * 
	 * @param location
	 *            The location the angle will computed to.
	 * @return The angle in radians.
	 */
	public double angle(Tuple3d location) {
		return Geometric.angle(this.location, location);
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
		double angle = Geometric.angle(this.location, p);
		turnTo(Math.toDegrees(angle));
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
	 * Turn the agent by the given angle in degrees.
	 * 
	 * @param angle The angle in degrees the agent will be turned.
	 */
	public final void turnBy(double angle) {
		angle = Geometric.clampAngleDegree(angle);
		orientation += angle;
		orientation = Geometric.clampAngleDegree(orientation);
		Geometric.rotateTo(velocity, Math.toRadians(orientation));
	}

	/**
	 * Turn the agent to the given angle given in degrees.
	 * 
	 * @param angle The angle in degrees the agent will head to.
	 */
	public final void turnTo(double angle) {
		angle = Geometric.clampAngleDegree(angle);
		orientation = Geometric.clampAngleDegree(angle);
		Geometric.rotateTo(velocity, Math.toRadians(orientation));
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
}
