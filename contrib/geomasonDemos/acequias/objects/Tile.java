package objects;

/**  <b>Tile</b> the patches of landscape which make up the environment.
 * 
 * Tiles have certain attributes, such as their elevation and their current landuse allocation
 * 
 * @author Sarah Wise and Andrew Crooks
 */
public class Tile {
	
	// --- ATTRIBUTES ---
	int x, y; // location in the grid
	double elevation = 0;
	boolean road = false;
	int hydrologicalFeature = -1;
	int tractMembership = -1;
	int landuse = -1;
	int county = -1;
	int acequia = -1; // the acequia with which this tile is associated
	
	
	int cropType = 0; // EXTENSION: only one crop right now, but there could be many
	double hydrationLevel = -1;

	/**
	 * Constructor
	 * @param x - x coordinate
	 * @param y - y coordinate
	 */
	public Tile(int x, int y){
		this.x = x;
		this.y = y;
	}
	
	/** Convert the tile to be used for urbanized purposes */
	public void urbanize(){ landuse = 22; } // developed with low intensity
	
	//
	// --- ACCESSORS ---
	//
	
	public int getX(){ return x; }
	public int getY(){ return y; }
	public int getAcequia(){ return acequia; }
	public int getCounty(){ return county; }
	public double getElevation() { return elevation; }
	public double getHydration(){ return hydrationLevel; }
	public int getHydrologicalFeature(){ return hydrologicalFeature; }
	public int getLanduse(){ return landuse; }
	public boolean getRoad(){ return road; }
	public int getTract(){ return tractMembership; }
	
	public void setAcequia(int acequiaNum){ acequia = acequiaNum; }
	public void setCounty(int countyNum){ county = countyNum; }
	public void setElevation(double level) { elevation = level; }
	public void setHydration(double level){ hydrationLevel = level; }
	public void setHydrologicalFeature(int feature){ hydrologicalFeature = feature; }
	public void setLanduse(int type){ landuse = type; }
	public void setRoad(boolean road){ this.road = road ; }
	public void setTract(int tract){ tractMembership = tract; }
	
}