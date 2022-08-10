package sim.app.geo.dwaterworld;

import java.util.ArrayList;

import sim.engine.DObject;


public class DBasin extends DObject{
	
	  int loc_x, loc_y; // location in the grid
	    ArrayList<DRaindrop> drops = new ArrayList<DRaindrop>();
	    double baseheight = 0; // the pure elevation of the tile
	    double cumulativeHeight = 0; // the combined elevation and height from water of the tile
	    double raindropFactor = 1;

	    // initialize a Basin
	    public DBasin(int x, int y)
	    {
	        loc_x = x;
	        loc_y = y;
	    }

	    // initialize a Basin at a certain altitude
	    public DBasin(int x, int y, int h)
	    {
	        loc_x = x;
	        loc_y = y;
	        baseheight = h;
	        cumulativeHeight = baseheight;
	    }

	    //
	    // MANAGING RAINDROPS AND HEIGHTS
	    //

	    // add a Raindrop and update the height accordingly
	    public void addDrop(DRaindrop r)
	    {
	        drops.add(r);
	        cumulativeHeight += raindropFactor;
	    }

	    // add some Raindrops and update the height accordingly
	    public void addDrops(ArrayList<DRaindrop> rs)
	    {
	        drops.addAll(rs);
	        cumulativeHeight += rs.size() * raindropFactor;
	    }

	    // remove a Raindrop and update the height accordingly
	    public void removeDrop(DRaindrop r)
	    {
	        drops.remove(r);
	        cumulativeHeight -= raindropFactor;
	    }

	    // remove some Raindrops and update the height accordingly
	    public void removeDrops(ArrayList<DRaindrop> rs)
	    {
	        drops.removeAll(rs);
	        cumulativeHeight -= rs.size() * raindropFactor;
	    }
	

}
