package sim.app.geo.dschellingpolygon;

import java.util.ArrayList;

import sim.engine.DSimState;
import sim.engine.DSteppable;
import sim.engine.SimState;


public class DPerson{


	    String color;
	    double preference;
	    DPolygon region;
	    double personalThreshold = .5;
	    int numMoves = 0;



	    /**
	     * Constructor function
	     * @param c
	     */
	    public DPerson(String c)
	    {
	        color = c;
	    }



	    /**
	     * Moves the Person to the given Polygon
	     * @param p - the Polygon to which the Person should move
	     */
	    public void updateLocation(DPolygon p)
	    {

	        // leave old tile, if previously was on a tile
	        if (region != null)
	        {
	            region.residents.remove(this);
	            region.soc = "UNOCCUPIED";
	        }

	        // go to new tile
	        region = p;
	        region.residents.add(this);
	        region.soc = color;

	        numMoves++; // increment the number of times moved
	    }



	    /**
	     *
	     * @param poly the proposed location
	     * @return whether the given Polygon is an acceptable location for the Person,
	     * based on the Person's personalThreshold
	     */
	    boolean acceptable(DPolygon poly)
	    {

	        // decide if Person is unhappy with surroundings
	        double unlike = 0, total = 0.;
	        for (DPolygon p : poly.neighbors)
	        {

	            if (p.soc.equals("UNOCCUPIED")) // empty spaces don't count
	            {
	                continue;
	            }
	            if (!p.soc.equals(color)) // is this neighbor an unlike neighbor?
	            {
	                unlike++;
	            }
	            total++; // total count of neighbors
	        }
	        double percentUnlike = unlike / Math.max(total, 1); // don't divide by 0!

	        // if unhappy, return false
	        if (percentUnlike >= personalThreshold)
	        {
	            return false;
	        } else // if happy, return true
	        {
	            return true;
	        }
	    }



	    /**
	     * @param ps the list of Polygons open to the Person
	     * @return the closest available Polygon that meets the Person's needs, if such
	     * a Polygon exists. If no such Polygon exists, returns null.
	     */
	    DPolygon bestMove(ArrayList<DPolygon> ps)
	    {

	        DPolygon result = null;
	        double bestDist = Double.MAX_VALUE;

	        // go through all polygons and determine the best move to make
	        for (DPolygon p : ps)
	        {

	            if (!p.soc.equals("UNOCCUPIED"))
	            {
	                continue; // not available
	            } else if (p.getMasonGeometry().geometry.getCentroid().distance(
	                region.getMasonGeometry().geometry.getCentroid()) >= bestDist) // distance between centroids
	            //else if( p.geometry.distance( region.geometry ) >= bestDist)
	            // distance between region borders
	            {
	                continue; // we already have a better option
	            } else if (!acceptable(p))
	            {
	                continue; // not an acceptable neighborhood
	            } else
	            { // otherwise it's an acceptable region and the closest region yet
	                result = p;
	                bestDist = p.getMasonGeometry().geometry.distance(region.getMasonGeometry().geometry);
	            }
	        }

	        return result;
	    }



	    /**
	     * Determines whether the Person's current location is acceptable. If not, attempts
	     * to move the Person to a better location.
	     */
	    public void moveToBetterLocation(SimState state)
	    {

	        if (!acceptable(region))
	        { // the current location is unacceptable

//	            System.out.println("unacceptable!");

	            // try to find and move to a better location
	            DPolygon potentialNew = bestMove(((DPolySchelling) state).polys);

	            if (potentialNew != null) // a better location was found
	            {
	                updateLocation(potentialNew);
	            } else // no better location was found. Stay in place.
	            {
//	                System.out.println("...but immobile");
	            }
	        }

	    }

	}

