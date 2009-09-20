/*
 * Bee foraging simulation. Copyright by Joerg Hoehne.
 * For suggestions or questions email me at hoehne@thinktel.de
 */

package de.thinktel.utils;

import javax.vecmath.Tuple3d;
import javax.vecmath.Vector3d;

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
	 * A constant holding the value of -pi.
	 */
	public static final double MINUS_PI = -Math.PI;

	/**
	 * A constant holding the value of pi/2.
	 */
	public static final double PI_HALF = Math.PI / 2;

	/**
	 * A constant holding the value of pi/2.
	 */
	public static final double MINUS_PI_HALF = -PI_HALF;

	/**
	 * A constant holding the value of 2pi.
	 */
	public static final double PI2 = Math.PI * 2;

	/**
	 * A constant holding the value of -2pi.
	 */
	public static final double MINUS_PI2 = -PI2;

	/**
	 * Compute the angle between the origin and the given point. This method
	 * calls {@link #azimuth(Tuple3d, Tuple3d)}. FIXME this is still the 2d
	 * version
	 * 
	 * @param a
	 *            First point.
	 * @return The angle (azimuth) in radians.
	 */
	public static double azimuth(Tuple3d a) {
		return Math.atan2(a.y, a.x);
	}

	/**
	 * Compute the angle between the first and second point. FIXME this is still
	 * the 2d version
	 * 
	 * @param a
	 *            First point.
	 * @param b
	 *            Second point.
	 * @return The angle (azimuth) in radians.
	 */
	public static final double azimuth(Tuple3d a, Tuple3d b) {
		double dx = b.x - a.x;
		double dy = b.y - a.y;
		return Math.atan2(dy, dx);
	}

	public static J3dPolar toPolar(double x, double y, double z) {
		return J3dPolar.createFrom(x, y, z);
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
			angle += 360;
		}
		return angle;
	}

	/**
	 * Clamp the given angle from -PI to PI radians (-180 to 180 degrees).
	 * Please be aware the result may be inaccurate due to the usage of the
	 * double type.
	 * 
	 * @param angle
	 *            The angle to be clamped.
	 * @return The clamped angle.
	 */
	public static final double clampAngleRadians(double angle) {
		if (angle >= Math.PI) {
			angle %= PI2;
			if (angle > Math.PI)
				angle -= PI2;
		} else {
			if (angle < 0.0) {
				angle %= PI2;
				if (angle < MINUS_PI)
					angle += PI2;
			}
		}
		return angle;
	}

	/**
	 * Compute the distance between to points given by two {@link Tuple3d}
	 * arguments.
	 * 
	 * @param p1
	 *            The first point.
	 * @param p2
	 *            The second point.
	 * @return The distance.
	 */
	public static final double distance(Tuple3d p1, Tuple3d p2) {
		double dx = p1.x - p2.x;
		double dy = p1.y - p2.y;
		double dz = p1.z - p2.z;
		return Math.sqrt(dx * dx + dy * dy + dz * dz);
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
}
