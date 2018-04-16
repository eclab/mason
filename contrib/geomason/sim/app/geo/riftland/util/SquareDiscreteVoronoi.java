package riftland.util;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import riftland.gui.AlternatingColorPortrayal2D;
import sim.field.grid.ObjectGrid2D;
import sim.portrayal.grid.ObjectGridPortrayal2D;
import sim.util.Int2D;

/**
 * An immutable, discrete Voronoi diagram computed with square distance (as
 * opposed to, say, Euclidean or Manhattan).
 * 
 * @author Eric 'Siggy' Scott
 */
public class SquareDiscreteVoronoi implements DiscreteVoronoi
{
    final int width;
    final int height;
    /** Each [y][x] location stores a reference to the point that is closest
     * to it. */
    final Int2D[][] grid;
    final List<Int2D> generators;
    
    public SquareDiscreteVoronoi(final int width, final int height, List<Int2D> generators) throws IllegalArgumentException
    {
        if (width <= 0)
            throw new IllegalArgumentException("DiscreteVoronoi: width was not positive.");
        if (height <= 0)
            throw new IllegalArgumentException("DiscreteVoronoi: height was not positive.");
        if (generators == null)
            throw new IllegalArgumentException("DiscreteVoronoi: List points was null.");
        if (generators.isEmpty())
            throw new IllegalArgumentException("DiscreteVoronoi: List points was empty.");

        this.width = width;
        this.height = height;
        grid = new Int2D[height][width];
        this.generators = generators;
        fillGrid(generators);
        assert(repOK());
    }

    // <editor-fold defaultstate="collapsed" desc="Accessors">
    @Override
    public int getWidth()
    {
        return width;
    }

    @Override
    public int getHeight()
    {
        return height;
    }
    
    /** @return A defensive copy of the closest point to the given query coordinates. */
    @Override
    public Int2D getNearestPoint(int x, int y)
    {
        if (x < 0)
            throw new IllegalArgumentException("DiscreteVoronoi.getNereastPoint(x,y): x is negative.");
        if (y < 0)
            throw new IllegalArgumentException("DiscreteVoronoi.getNereastPoint(x,y): y is negative.");
        if (x >= width)
            throw new IllegalArgumentException("DiscreteVoronoi.getNereastPoint(x,y): x was greater than width.");
        if (y >= height)
            throw new IllegalArgumentException("DiscreteVoronoi.getNereastPoint(x,y): x was greater than height.");
        
        Int2D nearest = grid[y][x];
        assert(repOK());
        return (Int2D)nearest;
    }

    @Override
    public List<Int2D> getObjects()
    {
        return generators;
    }
    // </editor-fold>
    
    // <editor-fold defaultstate="collapsed" desc="Algorithm">
    
    /** Compute the Voronoi diagram. This algorithm works by growing "marked"
     * areas in square layers simultaneously around each point by marking each
     * cell in the grid with the point it was grown from.  Thus a cell that has
     * the same square distance to two generators will be marked as closest to
     * the point that appears earlier in the generators list.
     * 
     * This is a more readable rewrite of the brute force algorithm available
     * at: http://yoshihitoyagi.com/projects/mesh/voronoi/discrete/index.html
     */
    private void fillGrid(List<Int2D> generators)
    {
        assert(generators != null);
        assert(!generators.isEmpty());
        Set<Int2D> completedPoints = new HashSet<Int2D>();
        for (int diameter = 0; completedPoints.size() < generators.size(); diameter++)
        {
            // Grow the next "layer" of the marked area around each point.
            for(Int2D p : generators)
            {
                if (completedPoints.contains(p))
                    continue;
                boolean stillGrowing = false;
                for (Int2D q : retrieveSquare(p.x, p.y, diameter))
                {
                    if (grid[q.y][q.x] == null) // If this point has not been visited
                    {
                        grid[q.y][q.x] = p; // Mark it as being closest to p
                        stillGrowing = true;
                    }
                }
                // If we're out of space, stop trying to grow this area.
                if (!stillGrowing)
                    completedPoints.add(p);
            }
        }
    }
    
    /** Get the generators lying on a square positioned d pixels away from (x,y), or
     * along the edge of the grid, whichever is closer. If (x,y) is far away
     * from the edge of the grid, the square will have a side length of 2r+1.*/
    private List<Int2D> retrieveSquare(final int x, final int y, final int r)
    {
        assert (x < width);
        assert (y < height);
        assert (x >= 0);
        assert (y >= 0);
        assert (r >= 0);
        assert (r < Math.max(width, height));
        if (r == 0)
            return new ArrayList<Int2D>(){{ add(new Int2D(x,y)); }};
        else
            return new ArrayList<Int2D>() {{
              addAll(retrieveSide(x, y, r, Direction.UP));  
              addAll(retrieveSide(x, y, r, Direction.RIGHT));  
              addAll(retrieveSide(x, y, r, Direction.DOWN));  
              addAll(retrieveSide(x, y, r, Direction.LEFT));  
            }};
    }
    
    /** Get the generators lying along a line segment 2*r pixels long positioned r
     * pixels away from (x,y), or along the edge of the grid, whichever is
     * closer.  Combining lists for each direction in a clockwise manner creates
     * a square.  The most clockwise point is left out of each line, because
     * that is where the next line would start in a square.
     * For instance, if (x,y) = (10,10), d = 3, and direction = UP,
     * then the list: [(7,13), (8, 13), (9,13), (10,13), (11,13), (12,13)]. The
     * point (13,13) is omitted, because that is where the line would
     * begin. for direction = RIGHT.*/
    private List<Int2D> retrieveSide(int x, int y, int r, Direction direction)
    {
        assert(x < width);
        assert(y < height);
        assert(x >= 0);
        assert(y >= 0);
        assert(r >= 0);
        List<Int2D> linePoints = new ArrayList(2*r);
        for(int i = -r; i < r; i++)
        {
            switch(direction)
            {
                case UP: 
                    linePoints.add(new Int2D(truncX(x + i), truncY(y + r)));
                    break;
                case DOWN:
                    linePoints.add(new Int2D(truncX(x - i), truncY(y - r)));
                    break;
                case LEFT:
                    linePoints.add(new Int2D(truncX(x - r), truncY(y + i)));
                    break;
                case RIGHT:
                    linePoints.add(new Int2D(truncX(x + r), truncY(y - i)));
                    break;
                default: assert(false);
            }
        }
        return linePoints;
    }
    
    /** Takes an x-coordinate that lies outside the grid and maps it to the
     * nearest edge of the grid. */
    private int truncX(int x)
    {
        if (x < 0)
            return 0;
        else if (x >= width)
            return width - 1;
        return x;
    }
    
    
    /** Takes a y-coordinate that lies outside the grid and maps it to the
     * nearest edge of the grid. */
    private int truncY(int y)
    {
        if (y < 0)
            return 0;
        else if (y >= height)
            return height - 1;
        return y;
    }
    
    private enum Direction { UP, DOWN, LEFT, RIGHT };
    // </editor-fold>
    
    /** Creates a portrayal that displays the whole Voronoi diagram. */
    @Override
    public ObjectGridPortrayal2D getPortrayal()
    {
        ObjectGridPortrayal2D portrayal = new ObjectGridPortrayal2D();
        portrayal.setField(getObjectGrid2D());
        portrayal.setPortrayalForAll(new AlternatingColorPortrayal2D());
        return portrayal;
    }
    
    /** Creates an ObjectGrid2D containing the Voronoi diagram, where each cell
     * holds exactly 1 reference to the nearest Point.*/
    private ObjectGrid2D getObjectGrid2D()
    {
        ObjectGrid2D objectGrid = new ObjectGrid2D(width, height);
        for (int x = 0; x < grid[0].length; x++)
        {
            for (int y = 0; y < grid.length; y++)
            {
                objectGrid.set(x, y, grid[y][x]);
            }
        }
        return objectGrid;
    }
    
    // <editor-fold defaultstate="collapsed" desc="RepOK">
    
    @Override
    final public boolean repOK()
    {
        return width > 0
                && height > 0
                && grid != null
                && generators != null
                && !generators.isEmpty()
                && gridFull();
    }
    
    private boolean gridFull()
    {
        for (Int2D[] c : grid)
        {
            for (Int2D r : c)
            {
                if (r == null)
                    return false;
            }
        }
        return true;
    }
    // </editor-fold>
}
