package CDI.src.environment;

import java.awt.Graphics2D;

import sim.portrayal.DrawInfo2D;
import sim.portrayal.SimplePortrayal2D;

public class HouseholdTrackerPortrayal2D extends SimplePortrayal2D {


	private static final long serialVersionUID = 1L;


	

	
	@Override
	public void draw(Object object, Graphics2D graphics, DrawInfo2D info) {
		
	}
	
	
	@Override
	public boolean hitObject(Object object, DrawInfo2D range) {
		return true;
	}
}
