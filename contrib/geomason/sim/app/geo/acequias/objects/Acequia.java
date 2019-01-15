package sim.app.geo.acequias.objects;

import java.util.ArrayList;

/** <b>Acequia</b> the social and phsycial system of water management
 * 
 * The Acequia holds information about where it flows and who is involved in maintaining
 * and using it. 
 * 
 * @author Sarah Wise and Andrew Crooks
 */
public class Acequia {
	int id = -1;
	int length = 0;
	int maxSizeEver = 0;
	ArrayList <Tile> ditch = new ArrayList <Tile> ();
	ArrayList <Parciante> members = new ArrayList <Parciante> ();
	
	/**
	 * Constructor
	 * @param id - an identifying number
	 * @param ditchTiles - the set of Tiles associated with this Acequia
	 */
	public Acequia(int id, ArrayList <Tile> ditchTiles){
		ditch = ditchTiles;
		length = ditchTiles.size();
		members = new ArrayList <Parciante> ();
	}
	
	/**
	 * Add a new Parciante to the membership
	 * @param p - the Parciante
	 */
	public void gainMember( Parciante p ){ 
		members.add( p );
		if( members.size() > maxSizeEver)
			maxSizeEver = members.size();
	}
	
	/** Lose a Parciante from the membership */
	public void loseMember( Parciante p ){ members.remove( p ); }
	
	/** return the number of Parciantes who are currently part of the Acequia */ 
	public int memberSize(){ return members.size(); }
	
	/** return the ratio of current size to maximum size throughout history - basically
	 * a measure of how much the acequia has shrunk since the 'glory days'
	 */
	public double proportionOfMaxMembers(){ return members.size() / ((double)maxSizeEver); }
}
