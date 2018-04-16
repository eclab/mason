package riftland.util;

import java.util.List;
import sim.portrayal.grid.ObjectGridPortrayal2D;
import sim.util.Int2D;

/**
 * An immutable, discrete Voronoi diagram -- a data structure for caching the
 * nearest object (a.k.a. "generator") to any point in a 2D grid.
 * 
 * @author Eric 'Siggy' Scott
 */
public interface DiscreteVoronoi
{
    public int getWidth();

    public int getHeight();
    
    /** @return A defensive copy of the closest generator to the given query coordinates. */
    public Int2D getNearestPoint(int x, int y) throws IllegalArgumentException;
    
    /** @return The generators that the Voronoi diagram was originally constructed arouns. */
    public List<Int2D> getObjects();
    
    /** @return A MASON portrayal attached to a defensive copy of the underlying grid. */
    public ObjectGridPortrayal2D getPortrayal();
    
    public boolean repOK();
}
