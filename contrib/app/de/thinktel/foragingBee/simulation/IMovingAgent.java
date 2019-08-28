/*
 * Bee foraging simulation. Copyright by Joerg Hoehne.
 * For suggestions or questions email me at hoehne@thinktel.de
 */

package de.thinktel.foragingBee.simulation;

import javax.vecmath.Point3d;
import javax.vecmath.Tuple3d;
import javax.vecmath.Vector3d;

/**
 * An interface for setting and retrieving information about a moving agent.
 * <p>
 * Copyright 2009 Joerg Hoehne
 * 
 * @author hoehne (<a href="mailto:hoehne@thinktel.de">J&ouml;rg H&ouml;hne</a>)
 * 
 */
public interface IMovingAgent {
	/**
	 * Return the distance to an other agent.
	 * 
	 * @param agent
	 *            The other agent.
	 * @return The distance.
	 */
	double distance(IMovingAgent agent);

	/**
	 * Return the distance to an other point.
	 * 
	 * @param p
	 *            The point the distance is calculated to.
	 * @return The distance.
	 */
	double distance(Point3d p);

	/**
	 * Return the squared distance to an other agent. This method saves some
	 * execution time by not applying the square root so a squared distance is
	 * returned.
	 * 
	 * @param agent
	 *            The other agent.
	 * @return The squared distance.
	 */
	double distanceSquared(IMovingAgent agent);

	/**
	 * Return the squared distance to the given point. This method saves some
	 * execution time by not applying the square root so a squared distance is
	 * returned.
	 * 
	 * @param p
	 *            The point.
	 * @return The squared distance.
	 */
	double distanceSquared(Point3d p);

	/**
	 * Calculate the vector from this agent to the provided one.
	 * 
	 * @param agent
	 *            The other the computed vector will head to.
	 * @return The vector heading to the other agent.
	 */
	Vector3d vectorTo(IMovingAgent agent);

	/**
	 * Return the reference to the agent's location.
	 * 
	 * @return The reference to the agents location as {@link Point3d}.
	 */
	public Point3d getLocation();

	/**
	 * Set the location of this agent.
	 * 
	 * @param x
	 *            The x value on the x-axis.
	 * @param y
	 *            The y value on the y-axis.
	 * @param z
	 *            The z value on the z-axis.
	 */
	public void setLocation(double x, double y, double z);

	/**
	 * Set the location of this agent.
	 * 
	 * @param location
	 *            The location of this agent.
	 */
	public void setLocation(Tuple3d location);

	/**
	 * Return the radius of the surrounding sphere of the agent. The sphere is
	 * similar to a bounding box but a sphere.
	 * 
	 * @return The sphere the agent fills in.
	 */
	public double getSphereRadius();

	/**
	 * Returns the vector with the velocity.
	 * 
	 * @return The current velocity vector of the agent.
	 */
	public Vector3d getVelocityVector();

	/**
	 * Set the velocity vector to a speed meaning setting the vector to a given
	 * length.
	 * 
	 * @param speed
	 */
	public void setVelocity(double speed);

	/**
	 * Set the velocity vector.
	 * 
	 * @param x
	 *            The velocity value for the x-axis.
	 * @param y
	 *            The velocity value for the y-axis.
	 * @param z
	 *            The velocity value for the z-axis.
	 */
	public void setVelocityVector(double x, double y, double z);

	/**
	 * Copies the field of the input vector in to current velocity vector.
	 * 
	 * @param v
	 *            The new velocity vector.
	 */
	public void setVelocityVector(Vector3d v);
}