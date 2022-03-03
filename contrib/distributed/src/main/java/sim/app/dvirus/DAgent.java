package sim.app.dvirus;

import java.awt.geom.Ellipse2D;


import sim.engine.DSteppable;
import sim.portrayal.DrawInfo2D;
import sim.util.Double2D;

public abstract class DAgent extends DSteppable{
	
    private static final long serialVersionUID = 1;

    public String id;

    public Double2D agentLocation; 

    public int intID = -1;

    public DAgent( String id, Double2D location )
        {
        this.id = id;
        this.agentLocation = location;
        }

    double distanceSquared( final Double2D loc1, Double2D loc2 )
        {
        return( (loc1.x-loc2.x)*(loc1.x-loc2.x)+(loc1.y-loc2.y)*(loc1.y-loc2.y) );
        }

    // Returns "Human", "Evil", or "Good"
    public abstract String getType();  

    
    //will this cause problems?
    public boolean hitObject(Object object, DrawInfo2D info)
        {
        double diamx = info.draw.width*DVirusInfectionDemo.DIAMETER * 10.0;
        double diamy = info.draw.height*DVirusInfectionDemo.DIAMETER * 10.0;
        

        Ellipse2D.Double ellipse = new Ellipse2D.Double( (int)(info.draw.x-diamx/2),(int)(info.draw.y-diamy/2),(int)(diamx),(int)(diamy) );
        return ( ellipse.intersects( info.clip.x, info.clip.y, info.clip.width, info.clip.height ) );
        }
        

}
