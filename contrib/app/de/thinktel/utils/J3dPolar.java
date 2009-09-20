/**
 * 
 */
package de.thinktel.utils;

import javax.vecmath.Point3d;
import javax.vecmath.Tuple3d;

/**
 * This class computes spheric coordinates for the Java3D cartesian coordinate
 * system. Java3Ds cartesian coordinate system differs from the mathematical by
 * rotation of the axes. In Java3D the axis pointing
 * <ul>
 * <li>to the right of the screen is the positive x-axis,</li>
 * <li>to to the top of the screen is the positive y-axis and</li>
 * <li>out of the screen towards the viewer is the positive z-axis.</li>
 * </ul>
 * In the mathematical geometric definition the
 * <ul>
 * <li>to the right of the screen is the positive y-axis,</li>
 * <li>to to the top of the screen is the positive z-axis and</li>
 * <li>out of the screen towards the viewer is the positive x-axis.</li>
 * <p>
 * Copyright 2009 Joerg Hoehne
 * 
 * @author hoehne (<a href="mailto:hoehne@thinktel.de">J&ouml;rg H&ouml;hne</a>)
 * 
 */
public class J3dPolar {
	public double radius;
	/**
	 * The angle in the xy-pane in radians.
	 */
	public double azimuth;
	/**
	 * The angle from the xy-pane in radians.
	 */
	public double elevation;

	public J3dPolar() {
		this(0, 0, 0);
	}

	public J3dPolar(J3dPolar src) {
		this(src.radius, src.azimuth, src.elevation);
	}

	public J3dPolar(double radius, double azimuth, double elevation) {
		this.radius = radius;
		this.azimuth = azimuth;
		this.elevation = elevation;
	}

	/**
	 * Return the radius of the spherical coordinates.
	 * 
	 * @return the radius
	 */
	public final double getRadius() {
		return radius;
	}

	/**
	 * Set radius of the spherical coordinates.
	 * 
	 * @param radius
	 *            the radius to set
	 */
	public final void setRadius(double radius) {
		this.radius = radius;
	}

	/**
	 * Return the azimuth of the spherical coordinates.
	 * 
	 * @return the azimuth
	 */
	public final double getAzimuth() {
		return azimuth;
	}

	/**
	 * Set azimuth of the spherical coordinates.
	 * 
	 * @param azimuth
	 *            the azimuth to set
	 */
	public final void setAzimuth(double azimuth) {
		this.azimuth = azimuth;
	}

	/**
	 * Return the elevation of the spherical coordinates.
	 * 
	 * @return the elevation
	 */
	public final double getElevation() {
		return elevation;
	}

	/**
	 * Set elevation of the spherical coordinates.
	 * 
	 * @param elevation
	 *            the elevation to set
	 */
	public final void setElevation(double elevation) {
		this.elevation = elevation;
	}

	/**
	 * Set the spheric coordinates according to a point given by a Java3D
	 * cartesian coordinate system.
	 * 
	 * @param t
	 *            The point in the Java3D cartesian coordinate system.
	 */
	public final void set(Tuple3d t) {
		set(t.x, t.y, t.z);
	}

	/**
	 * Set the spheric coordinates according to a point given by a Java3D
	 * cartesian coordinate system.
	 * 
	 * @param x
	 *            The point on the x-axis of the Java3D cartesian coordinate
	 *            system.
	 * @param y
	 *            The point on the y-axis of the Java3D cartesian coordinate
	 *            system.
	 * @param z
	 *            The point on the z-axis of the Java3D cartesian coordinate
	 *            system.
	 */
	public void set(double x, double y, double z) {
		azimuth = 0;
		elevation = 0;
		radius = Math.sqrt(x * x + y * y + z * z);
		if (radius > 0) {
			azimuth = Math.atan2(y, x);
			elevation = Math.asin(z / radius);
		}
	}

	/**
	 * Create a {@link J3dPolar} object defining the same point in spheric
	 * coordinates given by the Java3D cartesian coordinate system. This method
	 * calls {@link #createFrom(double, double, double)}.
	 * 
	 * @param p
	 *            The point in Java3D cartesian coordinate system.
	 * @return The {@link J3dPolar} object.
	 */
	public static J3dPolar createFrom(Tuple3d p) {
		return createFrom(p.x, p.y, p.z);
	}

	/**
	 * Create a {@link J3dPolar} object defining a spheric vector from a source
	 * location to a destination location given by the Java3D cartesian
	 * coordinate system. This method calls
	 * {@link #createFrom(double, double, double)}.
	 * 
	 * @param p1
	 *            The point of the source in Java3D cartesian coordinate system.
	 * @param p2
	 *            The point of the destination in Java3D cartesian coordinate
	 *            system.
	 * @return The {@link J3dPolar} object with the spheric vector from source
	 *         to destination.
	 */
	public static J3dPolar createFrom(Tuple3d p1, Tuple3d p2) {
		return createFrom(p2.x - p1.x, p2.y - p1.y, p2.z - p1.z);
	}

	/**
	 * Create a {@link J3dPolar} object defining the same point in spheric
	 * coordinates given by the Java3D cartesian coordinate system.
	 * 
	 * @param x
	 *            The point on the x-axis of the Java3D cartesian coordinate
	 *            system.
	 * @param y
	 *            The point on the y-axis of the Java3D cartesian coordinate
	 *            system.
	 * @param z
	 *            The point on the z-axis of the Java3D cartesian coordinate
	 *            system.
	 * @return The {@link J3dPolar} object.
	 */
	public static J3dPolar createFrom(double x, double y, double z) {
		// create an empty direction
		J3dPolar p = new J3dPolar();
		// set the internal values according to the point
		p.set(x, y, z);

		// return the object
		return p;
	}

	/**
	 * Compute the point defined by the spheric coordinates of this object.
	 * 
	 * @return The point in Java3D cartesian coordinate system.
	 */
	public Point3d toCartesian() {
		// the factor that will reduce the length of the radius in the xy pane
		// by the projection of the elevated radius in z-axis
		double cosElevation = Math.cos(elevation);
		double x = radius * Math.cos(azimuth) * cosElevation;
		double y = radius * Math.sin(azimuth) * cosElevation;
		double z = radius * Math.sin(elevation);

		return new Point3d(x, y, z);
	}
}
