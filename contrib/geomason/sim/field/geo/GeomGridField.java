package sim.field.geo;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LinearRing;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.geom.Polygon;
import sim.field.grid.Grid2D;

/** A georeferenced area represented by a grid
 *
 * The associated GeomField.MBR defines the area the grid maps to.
 *
 */
public class GeomGridField extends GeomField
{
    private static final long serialVersionUID = 5804948960128647172L;

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
        
        grid = wrappedGrid;
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
 
    public double getPixelHeight()
    {
        return pixelHeight;
    }

    public void setPixelHeight(double pixelHeight)
    {
        this.pixelHeight = pixelHeight;
    }

    public double getPixelWidth()
    {
        return pixelWidth;
    }

    public void setPixelWidth(double pixelWidth)
    {
        this.pixelWidth = pixelWidth;
    }
   

    public Grid2D getGrid()
    {
        return grid;
    }

    
    public void setGrid(Grid2D newGrid)
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
    public int getGridWidth()
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
    public int getGridHeight()
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

        // TODO update pixelWidth and pixelHeight iff grid is set
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

        Coordinate coordinate = new Coordinate();

        double x_orig = getMBR().getMinX();
        double y_orig = getMBR().getMinY(); // XXX getMaxY() instead?

        coordinate.x = x_orig + (x * getPixelWidth() + 0.5 * getPixelWidth());
        // XXX should invert y?
        coordinate.y = y_orig + (y * getPixelHeight() + 0.5 * getPixelHeight());
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
