package sim.field.geo;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.Point;
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


    /** Return a Point corresponding to center of grid cell
     *
     * @param x in pixel coordinates
     * @param y in pixel coordinates
     * @return Point for center of given grid cell
     */
    public Point toPoint(int x, int y)
    {
        assert x >= 0 && y >= 0 && x < getGridWidth() && y < getGridHeight() : "x: " + x + "y: " + y;

        if ( x < 0 || y < 0 || x >= getGridWidth() || y >= getGridHeight() )
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
    

    /** Used to create geometry as needed
     *
     * @see toPoint()
     */
    private static GeometryFactory geometryFactory = new GeometryFactory();

}
