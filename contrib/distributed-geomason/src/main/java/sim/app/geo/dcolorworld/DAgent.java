package sim.app.geo.dcolorworld;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.geom.util.AffineTransformation;

import sim.app.geo.colorworld.ColorWorld;
import sim.engine.SimState;
import sim.field.geo.GeomVectorField;
import sim.util.Double2D;
import sim.util.geo.DGeomSteppable;
import sim.util.geo.MasonGeometry;



public class DAgent extends DGeomSteppable{
	
    private static final long serialVersionUID = -5318720825474063385L;
    
    static private GeometryFactory fact = new GeometryFactory();

    
	// possible directions of movement
    final int N  = 0; 
    final int NW = 1; 
    final int W  = 2;
    final int SW = 3;
    final int S  = 4;
    final int SE = 5; 
    final int E  = 6; 
    final int NE = 7;

    // Current direction the agent is moving
    int direction;
    
    // agent's position
    //Point location = null;
    //Use mg instead (MasonGeometry)

    // How much to move the agent by in each step()
    double moveRate = 100.0;
     
    public DAgent(int d)
    {
        direction = d;
    }
            
    public void setLocation(Point p) { 
		MasonGeometry agentGeometry = new MasonGeometry(fact.createPoint(new Coordinate(p.getX(), p.getY())));
        mg = agentGeometry; 
        }


	@Override
	public void step(SimState state) {
		// TODO Auto-generated method stub
		
        // try to move the agent, keeping the agent inside its political region
        
    	DColorWorld cState = (DColorWorld)state; 
        GeomVectorField world = cState.county;
       // Coordinate coord = (Coordinate) location.getCoordinate().clone();
        Coordinate coord = (Coordinate) ((Point)this.getMasonGeometry().getGeometry()).getCoordinate().clone();
        AffineTransformation translate = null;

        switch (direction)
            {
            case N : // move up
                translate = AffineTransformation.translationInstance(0.0, moveRate);
                coord.y += moveRate;
                break;
            case S : // move down
                translate = AffineTransformation.translationInstance(0.0, -moveRate);
                coord.y -= moveRate;
                break;
            case E : // move right
                translate = AffineTransformation.translationInstance(moveRate, 0.0);
                coord.x += moveRate;
                break;
            case W : // move left
                translate = AffineTransformation.translationInstance(-moveRate, 0.0);
                coord.x -= moveRate;
                break;
            case NW : // move upper left
                translate = AffineTransformation.translationInstance(-moveRate,moveRate);
                coord.x -= moveRate;
                coord.y += moveRate; 
                break;
            case NE : // move upper right
                translate = AffineTransformation.translationInstance( moveRate, moveRate );
                coord.x += moveRate;
                coord.y += moveRate;
                break;
            case SW : // move lower left
                translate = AffineTransformation.translationInstance(-moveRate, -moveRate);
                coord.x -= moveRate;
                coord.y -= moveRate;
                break;
            case SE : // move lower right
                translate = AffineTransformation.translationInstance( moveRate, -moveRate);
                coord.x += moveRate;
                coord.y -= moveRate;
                break;
            }

        // is the new position still within the county?
        if (world.isInsideUnion(coord))  { 
        	//cState.county.updateTree(location, translate); 
        	((Point) this.getMasonGeometry().getGeometry()).apply(translate);
        	
        	Point pt = (Point) this.getMasonGeometry().getGeometry();
        	
			Coordinate c = new Coordinate(pt.getX(), pt.getY());
			
			Double2D partSpacePoint = cState.agents.convertJTSToPartitionSpace(c); //Need to moveAgent using partitionSpace
        	
    		cState.agents.moveAgent(partSpacePoint, this);

        }
        else // try randomly moving in different direction if trying to stray
            direction = state.random.nextInt(8);
		
	}

}
