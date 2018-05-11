package migration.util;

import sim.util.Int2D;
import java.awt.Polygon;

/**
 * Miscellaneous static methods.
 * 
 * @author Eric 'Siggy' Scott
 */
public class Misc
{
    private Misc()
    { // Prevent instantiation of static class
        throw new AssertionError();
    }
    
    public static Polygon EquilateralTriangle(double sideLength)
    {
        assert(sideLength > 0);
        double height = Math.sqrt(sideLength*sideLength + Math.pow(sideLength/2, 2));
        int[] xCoords = new int[] { 0, (int) (sideLength/2), (int) (-sideLength/2) };
        int[] yCoords = new int[] { (int) (-height/2), (int) (height/2), (int) (height/2) };
        return new Polygon(xCoords, yCoords, 3);
    }
    
    public static double getEuclideanDistance(Int2D p1, Int2D p2)
    {
        assert(p1 != null);
        assert(p2 != null);
        
        int dx = p2.x - p1.x;
        int dy = p2.y - p1.y;
        return (Math.sqrt(dx*dx + dy*dy));
    }
    
    public static double sigmoid(double x, double beta, double offset)
    {
        return 1.0/(1+Math.exp(-beta * (x - offset)));
    }
    
    public enum HexDirection { N, NE, SE, S, SW, NW };
}