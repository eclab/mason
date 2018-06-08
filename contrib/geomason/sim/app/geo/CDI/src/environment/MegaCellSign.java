package CDI.src.environment;


import sim.engine.SimState;
import sim.engine.Steppable;
import sim.util.Double2D;


public class MegaCellSign implements Steppable, sim.portrayal.Orientable2D{
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	public Double2D loc = new Double2D(0,0);
	public double orientation = 0;
	public double magnitude = -1;
	
	public int id;
	public double x;
	public double y;
	
	public int peopleMovedIn = 0;
	public int peopleMovedOut = 0;
	public int netInOrOut = 0;
	
	public MegaCellSign(int id, Double2D loc) {
		this.id = id;
		this.loc = loc;
	}
	
	
	public int getId() {
		return this.id;
	}
	
	public double getOrientation() {
		return this.orientation2D();
	}
	
	@Override
	public double orientation2D() {
		return this.orientation;
	}
	
	@Override
	public void setOrientation2D(double val) {
		
	}
	
	
	public double getMagnitude() {
		return this.magnitude;
	}
	
	public int getNetMovement() {
		return this.netInOrOut;
	}
	
	

	@Override
	public void step(SimState state) {
		// here we need to update the orientation of the sign
		
		if (this.x == 0 && this.y == 0) 
			this.orientation = 0;
		else
			orientation = Math.atan2(this.y, this.x);
		
		// no body moved, we don't need to show the sign
		if(this.peopleMovedOut==0)
			this.magnitude = -1;
		else {
			double temp = Math.sqrt(this.x*this.x+this.y*this.y);
			this.magnitude = Math.log(this.peopleMovedOut * temp);
		}
		
		this.netInOrOut = this.peopleMovedIn - this.peopleMovedOut;
		
		this.x = 0;
		this.y = 0;
		
		this.peopleMovedOut = 0;
		this.peopleMovedIn = 0;
		
	}
	
	

	

	public void addDelta(double deltaX, double deltaY) {
		this.x += deltaX;
		this.y += deltaY;		
	}


	public void incrementMovedOutPeople() {
		this.peopleMovedOut++;
	}
	
	public void incrementMovedInPeople() {
		this.peopleMovedIn++;
	}

}
