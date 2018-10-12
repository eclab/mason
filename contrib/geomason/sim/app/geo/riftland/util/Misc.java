package sim.app.geo.riftland.util;

import ec.util.MersenneTwisterFast;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import sim.app.geo.riftland.Parameters;
import sim.app.geo.riftland.parcel.GrazableArea;
import sim.util.Bag;
import sim.util.Int2D;

/**
 * Miscellaneous static utility methods.
 * 
 * @author Eric 'Siggy' Scott
 */
public class Misc
{
    private Misc()
    { // Prevent instantiation of static class.
        throw new AssertionError();
    }
    
    /**
     * Converts a string of the form "x,y", where x and y are integers, to an
     * AWT Point.
     *
     * @param pointString containing "x,y"
     * @return Point(x,y)
     * @throws IllegalArgumentException
     * @throws NumberFormatException
     */
    public static java.awt.Point stringToPoint(final String pointString) throws NumberFormatException, IllegalArgumentException
    {
        String[] coordinates = pointString.split(",");
        if (coordinates.length != 2)
            throw new IllegalArgumentException("Input to Misc.stringToPoint() must be two numbers separated by a comma.");

        int x = Integer.parseInt(coordinates[0].trim());
        int y = Integer.parseInt(coordinates[1].trim());

        return new java.awt.Point(x, y);
    }
    
    /**
     * Returns the command line argument for the corresponding keyword
     *
     * Assumes that the string following the keyword is an argument; i.e., all
     * command line keywords have a corresponding argument and are therefore not
     * toggles.
     *
     * E.g., for command line argument of "-foo bar" this will return "bar"
     *
     * XXX probably could eventually go for more traditional command line
     * parser; but this will do for our purposes for now.
     *
     * @param key keyword for which we're looking for corresponding parameter
     * @param args the command line arguments
     * @return argument for given command line keyword, or null if not found
     */
    public static String argumentForKey(final String key, final String[] args)
    {
        for (int x = 0; x < args.length - 1; x++)  // key can't be the last string
            if (args[x].equalsIgnoreCase(key))
                return args[x + 1];
        return null;
    }

    /**
     * Ensure that if the user specified a directory containing the location of
     * all the data files that the path correctly ends with a directory
     * separator. <P> Used in ctor to ensure we have a proper path string so
     * that we can naively prepend the data path to all file name strings. If
     * the user did not specify a file path, then an empty string is returned
     * instead.
     *
     * @param dataPath is string denoting directory where data files reside; may
     * be null if user did not specify a 'filepath' command line argument
     * @return dataPath terminated with the path separator character
     */
    public static String normalizeDataPath(final String dataPath)
    {
        // This covers the case where there is no 'filepath' argument specified
        // on the command line.

        if (dataPath == null)
            return "";

        // Essentially return an empty string if the user didn't specify a
        // data path
        if (dataPath.isEmpty())
            return dataPath;

        // Already ends with a proper path separator, so we're good.
        if (dataPath.trim().endsWith("/"))
            return dataPath;

        return dataPath + "/";
    }
    
    /** Returns true if the Iterable contains only objects of class type. */
    public static boolean containsOnlyType(Iterable it, Class type)
    {
        for (Object o : it)
        {
            if (!(type.isInstance(o)))
                return false;
        }
        return true;
    }
    
    /** Partition of bag of objects into n bags, where the 1st bag holds
     * the 1st objects.size()/n objects, etc. If n is greater than the number
     * of objects, the bags at indices greater than objects.size()-1 will be
     * empty. */
    public static List<Bag> partition(final Bag objects, final int n)
    {
        assert(n > 0);
        assert(objects != null);
        final List<Bag> bags = new ArrayList<Bag>(n) {{
          for (int i = 0; i < n; i++)
              add(new Bag());
        }};
        for(int i = 0; i < objects.size(); i++)
        {
            int objectsPerBag = (int)Math.ceil(objects.size()/(double)n);
            int bagIndex = i/objectsPerBag;
            bags.get(bagIndex).add(objects.get(i));
        }
        return bags;
    }
    
    /** Euclidean distance between to integral vectors. */
    public static double getEuclideanDistance(Int2D p1, Int2D p2)
    {
        assert(p1 != null);
        assert(p2 != null);
        
        int dx= p2.x - p1.x;
        int dy = p2.y - p1.y;
        return Math.sqrt(dx*dx + dy*dy);
    }
    
    /** Get a random subset of the GrazableAreas surrounding a location,
     * where the probability of a point being included is inversely proportional
     * to the distance from the location.
     */
    public static List<GrazableArea> sampleNearbyGrazableAreas(GrazableArea location, Parameters params, MersenneTwisterFast random)
    {
        Bag nearby = location.getNearestGrazableAreas();
        List<GrazableArea> sample = new ArrayList<GrazableArea>();
        assert (nearby.numObjs > 0);
//        sample.add(location);	// always add the current location
        for (Object o : nearby)
        {
            if (!shouldSkip((GrazableArea)o, params, random, location.getCoordinate()))
                sample.add((GrazableArea)o);
        }
        return sample;
    }
    
    /** Returns the average percentage of GrazableAreas that we expect the method
     *  sampleNearbyGrazableAreas to sample from the parcels near location.  This
     *  is a mathematical expression that was manually derived from the probability
     *  distribution encoded by shouldSkip().
     */
    public static double percentExpectedGrazableAreasSamples(GrazableArea location)
    {
        return Math.PI / 12; // i.e., about 26.18% of nearby parcels are sampled (if they are all GrazableAreas)
    }

    /**
     * Stochastically pick whether the given GrazableArea should be considered based
     * on its distance. Nearer parcels will be more likely to be considered.
     * @param a The grazable area in consideration
     * @return true if the parcel should be considered, false if it should be skipped
     */
    private static boolean shouldSkip(GrazableArea a, Parameters params, MersenneTwisterFast random, Int2D location) {
        double distance = CachedDistance.distance(location, a.getCoordinate());
        // idea here is to pay more attention to close parcels and less to far ones.

        if (distance > (random.nextDouble() * params.herding.getVisionAndMovementRange())){ 
           return true;
        }

        return false;
    }
    
    /** Builds a color map for the given minimum and maximum values
    *
    * Does a log ramping of colors to highlight lower population area structure
    *
    * @param min smallest legal value 
    * @param max largest legal value
    * @return color map suitable for SimpleColorMap ctor
    */
   public static Color[] buildLogColorMap(int min, int max)
   {
       Color[] colors = new Color[max];
       int curColor = 0;

       double maxValue = Math.log(max);

       try
       {
           for (int i = 1; i <= max; i++)
           {
               curColor = (int) (255.0 * Math.log((double) i) / maxValue);
               colors[i - 1] = new Color(curColor, curColor, curColor);
           }
       } catch (Exception e)
       {
           System.err.println(e);
       }

       return colors;
   }
}
