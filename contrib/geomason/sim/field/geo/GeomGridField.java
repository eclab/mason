/*
 * Copyright 2011 by Mark Coletti, Keith Sullivan, Sean Luke, and
 * George Mason University Mason University Licensed under the Academic
 * Free License version 3.0
 * 
 * See the file "LICENSE" for more information
 *
 * $Id$
*/
package sim.field.geo;

import com.vividsolutions.jts.geom.*;
import sim.field.grid.DoubleGrid2D;
import sim.field.grid.Grid2D;
import sim.field.grid.IntGrid2D;

/** A georeferenced area represented by a grid
 *
 * The associated GeomField.MBR defines the area the grid maps to.
 *
 */
public class GeomGridField extends GeomField
{
    private static final long serialVersionUID = 5804948960128647172L;


    /**
     * Used to determine the GeomGridField storage type.
     */
    public enum GridDataType
    {
        INTEGER, DOUBLE
    }


    /**
     *
     * @return the data type of the grid cells; null if there is no assigned grid
     */
    public GridDataType getGridDataType()
    {
        if (getGrid() instanceof IntGrid2D)
        {
            return GridDataType.INTEGER;
        }
        else if (getGrid() instanceof DoubleGrid2D)
        {
            return GridDataType.DOUBLE;
        }

        return null;
    }


	/** Grid container
     * 
     * Allows for the user to use an arbitrary grid of integers, doubles, or objects.
     *
     * XXX Maybe consider Abstract2DGrid instead? But that would eliminate possibly
     * using Sparse2DGrid.
     * 
     */
    private Grid2D grid = null;

    public GeomGridField()
    {
        super();
    }


    public GeomGridField(Grid2D wrappedGrid)
    {
        super();
        
        setGrid(wrappedGrid);
    }

    /** width of grid point in projection coordinate system
     *
     * @see GeomGridField.setGrid()
     * @see GeomGridField.setMBR()
     */
    private double pixelWidth = 0.0;

    /** height of grid point in projection coordinate system
     *
     * @see GeomGridField.setGrid()
     * @see GeomGridField.setMBR()
     */
    private double pixelHeight = 0.0;

    /** Height of pixels in units of the underlying coordinate reference system */
    public double getPixelHeight()
    {
        return pixelHeight;
    }

    /** Set heigh of pixels in units of the underlying coordinate reference system */
    public void setPixelHeight(double pixelHeight)
    {
        this.pixelHeight = pixelHeight;
    }

    /** Width of pixels in units of the underlying coordinate reference system */
    public double getPixelWidth()
    {
        return pixelWidth;
    }

    /** Set pixel width in units of underlying coordinate reference system */
    public void setPixelWidth(double pixelWidth)
    {
        this.pixelWidth = pixelWidth;
    }
   

    public final Grid2D getGrid()
    {
        return grid;
    }

    
    public final void setGrid(Grid2D newGrid)
    {
        grid = newGrid;

        setPixelWidth(grid.getWidth());
        setPixelHeight(grid.getHeight());
    }

    /**
     * Returns width of grid in pixels.
     * <p>
     * By contrast, super.getWidth() returns width in spatial reference context.
     *
     * @return Returns width in grid points
     *
     * @throws NullPointerException if grid not assigned
     */
    public final int getGridWidth()
    {
        return grid.getWidth();
    }


    /**
     * By contrast, super.getHeight() returns width in spatial reference context.
     *
     * @return height of grid in pixels
     *
     * @throws NullPointerException if grid not assigned
     */
    public final int getGridHeight()
    {
        return grid.getHeight();
    }



    @Override
    public void clear()
    {
        super.clear();

        this.grid = null;
    }
    

    @Override
    public void setMBR(Envelope MBR)
    {
        super.setMBR(MBR);

        // update pixelWidth and pixelHeight iff grid is set
        if (grid != null)
        {
            setPixelWidth(MBR.getWidth() / getGridWidth());
            setPixelHeight(MBR.getHeight() / getGridHeight());
        }
    }



    /**
     * @return true if (x,y) within grid boundaries
     */
    public boolean isInGrid(int x, int y)
    {
        if ( x < 0 || y < 0 || x >= getGridWidth() || y >= getGridHeight() )
        {
            return false;
        }
        return true;
    }

    
    /**
     * @param p point
     * @return x grid coordinate for cell 'p' is in
     */
    public int toXCoord(final Point p)
    {
        return (int) Math.floor((p.getX() - getMBR().getMinX()) / getPixelWidth());
    }

    /**
     *
     * @param x Coordinate in base projection
     * @return x grid coordinate for cell 'x'
     */
    public int toXCoord(final double x)
    {
        return (int) Math.floor((x - getMBR().getMinX()) / getPixelWidth());
    }


    /**
     * @param p point
     * @return y grid coordinate for cell 'p' is in
     */
    public int toYCoord(final Point p)
    {
        // Note that we have to flip the y coordinate because the origin in
        // MASON is in the upper left corner.
        return (int) Math.floor((getMBR().getMaxY() - p.getY()) / getPixelHeight());
    }

    /**
     *
     * @param y coordinate in base projection
     * @return y grid coordinate for cell 'y' is in
     */
    public int toYCoord(final double y)
    {
        // Note that we have to flip the y coordinate because the origin in
        // MASON is in the upper left corner.
        return (int) Math.floor((getMBR().getMaxY() - y) / getPixelHeight());
    }



    /** Return a Point corresponding to center of grid cell
     *
     * @param x in pixel coordinates
     * @param y in pixel coordinates
     * @return Point for center of given grid cell
     */
    public Point toPoint(int x, int y)
    {
        assert x >= 0 && y >= 0 && x < getGridWidth() && y < getGridHeight() : "x: " + x + "y: " + y;

        if ( ! isInGrid(x,y) )
        {
            throw new IndexOutOfBoundsException();
        }

        // Invert the y axis to map from MASON coordinate system, which has
        // the origin in the upper left, to real world coordinate systems that
        // have the origin in the lower left.
        int inverted_y = getGridHeight() - y - 1;

        Coordinate coordinate = new Coordinate();

        double x_orig = getMBR().getMinX();
        double y_orig = getMBR().getMinY();

        coordinate.x = x_orig + (x * getPixelWidth() + 0.5 * getPixelWidth());
        coordinate.y = y_orig + (inverted_y * getPixelHeight() + 0.5 * getPixelHeight());
        coordinate.z = 0.0;

        Point point = geometryFactory.createPoint(coordinate);

        return point;
    }


    /** Return a Polygon corresponding to the grid cell perimeter
     *
     * @param x in pixel coordinates
     * @param y in pixel coordinates
     * @return Polygon representing grid cell area
     */
    public Polygon toPolygon(int x, int y)
    {
        assert x >= 0 && y >= 0 && x < getGridWidth() && y < getGridHeight() : "x: " + x + "y: " + y;

        if ( ! isInGrid(x,y) )
        {
            throw new IndexOutOfBoundsException();
        }

        Coordinate[] coordinates = new Coordinate[5];
        double x_orig = getMBR().getMinX();
        double y_orig = getMBR().getMinY(); // XXX getMaxY() instead?


        // Bottom left corner
        coordinates[0] = new Coordinate();
        coordinates[0].x = x_orig + (x * getPixelWidth());
        coordinates[0].y = y_orig + (y * getPixelHeight());
        coordinates[0].z = 0.0;

        // Upper left corner
        coordinates[1] = new Coordinate();
        coordinates[1].x = x_orig + (x * getPixelWidth());
        coordinates[1].y = y_orig + ((y + 1) * getPixelHeight());
        coordinates[1].z = 0.0;

        // Upper right corner
        coordinates[2] = new Coordinate();
        coordinates[2].x = x_orig + ((x + 1) * getPixelWidth());
        coordinates[2].y = y_orig + ((y + 1) * getPixelHeight());
        coordinates[2].z = 0.0;

        // Finally, lower right corner
        coordinates[3] = new Coordinate();
        coordinates[3].x = x_orig + ((x + 1) * getPixelWidth());
        coordinates[3].y = y_orig + (y * getPixelHeight());
        coordinates[3].z = 0.0;

        // Close the ring
        coordinates[4] = new Coordinate();
        coordinates[4].x = x_orig + (x * getPixelWidth());
        coordinates[4].y = y_orig + (y * getPixelHeight());
        coordinates[4].z = 0.0;

        
        LinearRing ring = geometryFactory.createLinearRing(coordinates);

        Polygon polygon = geometryFactory.createPolygon(ring, null); // null means no inner rings; i.e., no hollow parts

        return polygon;
    }

    

    /** Used to create geometry as needed
     *
     * @see toPoint()
     */
    private static GeometryFactory geometryFactory = new GeometryFactory();

}
