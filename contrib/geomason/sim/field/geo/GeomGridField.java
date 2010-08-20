package sim.field.geo;

import com.vividsolutions.jts.geom.Envelope;
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

        // TODO update pixelWidth and pixelHeight iff MBR set
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




}
