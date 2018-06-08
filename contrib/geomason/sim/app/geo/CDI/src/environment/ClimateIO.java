package CDI.src.environment;

import java.io.IOException;

import sim.field.grid.DoubleGrid2D;
import ucar.ma2.Index;
import ucar.ma2.InvalidRangeException;


public abstract class ClimateIO
{

    public abstract int getStepsPerYear();


    public abstract double getFirstTime();


    public abstract double getTimeForLayer(int layer);


    public abstract int getLastLayer();


    /**
     * Reads in the layer of weather data in the next time-step.
     * @throws IOException
     * @throws InvalidRangeException
     */
    public abstract int loadLayer(int timeIndex);


    /**
     * Make sure all angle fall between 0 and 360.
     */
    public static double adjustAngle(double angle)
    {
        double newAngle = angle;

        while (newAngle < 0.0)
            newAngle += 360.0;

        while (newAngle > 360.0)
            newAngle -= 360.0;

        return newAngle;
    }


    /**
     * Check to see if a number is within certain bounds.  If upperBound is
     * less than lowerBound, it is assumed that the area is toroidal, and the
     * space wraps.
     */
    public static boolean withinBounds(double val, double lowerBound,
                                       double upperBound)
    {
        boolean isWithin = false;
        if (upperBound < lowerBound)
            isWithin = val < upperBound || val >= lowerBound;
        else
            isWithin = val < upperBound && val >= lowerBound;

        return isWithin;
    }


    /**
     * Check to see if an angle is within certain bounds.  Bounds are
     * assumed to wrap (i.e. -180 degrees = 180 degrees).
     */
    public static boolean withinAngles(double angle, double lowerBound,
                                       double upperBound)
    {
        return withinBounds(adjustAngle(angle), adjustAngle(lowerBound),
                            adjustAngle(upperBound));
    }


    /**
     * Search for the bin in which the value belongs.  Bins are
     * described by an array of bounds, where each array values defines
     * the upper bound of the previous bin and the lower bound of the next
     * bin.  The index of the lower bound is returned.  If no bin is found,
     * -1 is returned.
     */
    int findBin(double val, double[] bounds)
    {
        //System.out.println("findBin("+val+")");
        for(int i = 0; i < bounds.length-1; i++)
            if(withinBounds(val, bounds[i], bounds[i+1]))
                return i;
        //return -1;
        return bounds.length-1;
    }


    void reprojectDoubleGrid2D(DoubleGrid2D fromGrid, DoubleGrid2D toGrid,
                               DoubleGrid2D latGrid, DoubleGrid2D lonGrid,
                               double[] latBounds, double[] lonBounds,
                               double minVal, double maxVal, double defaultVal)
    {
        // minVal = 0.0
        // maxVal = 400.0
        // defaultVal = 270.0   // 0 degrees C?

        int width = latGrid.getWidth();
        int height = latGrid.getHeight();
        assert(width == toGrid.getWidth());
        assert(height == toGrid.getHeight());

        double previousVal = defaultVal; 
        for (int xi = 0; xi < width; xi++)
        {
            for (int yi = 0; yi < height; yi++)
            {
                double lon = lonGrid.get(xi, yi);
                double lat = latGrid.get(xi, yi);
                int lonIndex = findBin(lon, lonBounds);
                int latIndex = findBin(lat, latBounds);
                //System.out.println("(lonInd, latInd) = (" + lonIndex + ", " + latIndex + ")");
                double dataVal = fromGrid.get(lonIndex, latIndex);

/*
                if (dataVal <= minVal)
                {
                    System.out.println(dataVal);
                }
*/

                if (dataVal > maxVal || dataVal <= minVal)
                {
                    //System.out.println(data);
                    dataVal = previousVal;
                }

                toGrid.set(xi, yi, dataVal);
                previousVal = dataVal;
            }
        }
    }
}
