/*
 * Bee foraging simulation. Copyright by Joerg Hoehne.
 * For suggestions or questions email me at hoehne@thinktel.de
 */

package utils;

import javax.vecmath.Point3d;
import javax.vecmath.*;

import foragingBee.IMovingAgent;

/**
 * A class with some static utility methods.
 * <p>
 * Copyright 2009 Joerg Hoehne
 * 
 * @author hoehne (<a href="mailto:hoehne@thinktel.de">J&ouml;rg H&ouml;hne</a>)
 * 
 */
public class Geometric {

	/**
	 * Compute the angle between the origin and the given point. This method
	 * calls {@link #angle(Tuple3d, Tuple3d)}.
	 * 
	 * @param a
	 *            First point.
	 * @return The angle in radians.
	 */
	public static double angle(Tuple3d a) {
		return angle(new Point3d(), a);
	}

	/**
	 * Compute the angle between the first and second point.
	 * 
	 * @param a
	 *            First point.
	 * @param b
	 *            Second point.
	 * @return The angle in radians.
	 */
	public static double angle(Tuple3d a, Tuple3d b) {
		double dx = b.x - a.x;
		double dy = b.y - a.y;
		double angle = 0.0d;

		if (dx == 0.0) {
			if (dy == 0.0)
				angle = 0.0;
			else if (dy > 0.0)
				angle = Math.PI / 2.0;
			else
				angle = (Math.PI * 3.0) / 2.0;
		} else if (dy == 0.0) {
			if (dx > 0.0)
				angle = 0.0;
			else
				angle = Math.PI;
		} else {
			if (dx < 0.0)
				angle = Math.atan(dy / dx) + Math.PI;
			else if (dy < 0.0)
				angle = Math.atan(dy / dx) + (2 * Math.PI);
			else
				angle = Math.atan(dy / dx);
		}

		return angle;
		// return (angle * 180) / Math.PI;
	}

	/**
	 * Clamp the given angle from 0 to 360 degrees. Please be aware the result
	 * may be inaccurate due to the usage of the double type.
	 * 
	 * @param angle
	 *            The angle to be clamped.
	 * @return The clamped angle.
	 */
	public static final double clampAngleDegree(double angle) {
		if (angle >= 360.0)
			angle %= 360.0d;
		if (angle < 0.0) {
			angle %= 360.0d;
			if (angle < 0.0)
				angle += 360;
		}
		return angle;
	}

	/**
	 * Return the normalized vector. Leave it unchanged if the vector is of
	 * length 0.
	 * 
	 * @param v
	 *            The vector to be normalized.
	 */
	public static final void normalize(Vector3d v) {
		if ((v.x == 0.0) && (v.y == 0.0) && (v.z == 0.0))
			return;

		normalize(v, v.length());
	}

	/**
	 * Return the normalized vector. Leave it unchanged if the vector is of
	 * length 0. The vector will be scaled to the given length.
	 * 
	 * @param v
	 *            The vector to be normalized and set to the given length.
	 * @param length
	 *            The pre-computed length of the vector.
	 */
	public static final void normalize(Vector3d v, double length) {
		if ((v.x == 0.0) && (v.y == 0.0) && (v.z == 0.0))
			return;

		v.scale(1 / length);
	}

	/**
	 * Rotate the give vector of type {@link Vector3d} by the given angle. Only
	 * the x and y value of the vector are modified.
	 * 
	 * @param v
	 *            The vector to rotate.
	 * @param angle
	 *            The angle in radians to rotate the vector.
	 */
	public static void rotateBy(Vector3d v, double angle) {
		double sina = Math.sin(angle);
		double cosa = Math.cos(angle);
		double x = v.x;
		double y = v.y;

		v.x = x * cosa - y * sina;
		v.y = x * sina + y * cosa;
	}

	/**
	 * Rotate a vector to a given angle in radians.
	 * 
	 * @param v
	 *            The vector to rotate.
	 * @param angle
	 *            The angle in radians.
	 */
	public static void rotateTo(Vector3d v, double angle) {
		double length = v.length();
		v.x = length * Math.cos(angle);
		v.y = length * Math.sin(angle);
	}
}
