package sim.app.dvirus;

//import java.awt.Color;

//import java.awt.Graphics2D;

import sim.engine.SimState;
//import sim.portrayal.DrawInfo2D;
import sim.util.Bag;
import sim.util.Double2D;

public class DGood extends DAgent{
    private static final long serialVersionUID = 1;

    protected boolean greedy = false;
    public final boolean getIsGreedy() { return greedy; }
    public final void setIsGreedy( final boolean b ) { greedy = b; }

    public DGood( String id, Double2D location ) 
        {
        super( id, location );
        try
            {
            intID = Integer.parseInt( id.substring(4) ); // "Good"
            }
        catch( Exception e )
            {
            throw new RuntimeException(e);
            }
        }

    Double2D desiredLocation = null;
    Double2D suggestedLocation = null;
    int steps = 0;

    public void step( final SimState state )
        {
        DVirusInfectionDemo hb = (DVirusInfectionDemo)state;

        desiredLocation = null;
        double distance2DesiredLocation = 1e30;

        //Bag mysteriousObjects = new Bag(hb.environment.getNeighborsWithinDistance( agentLocation, 50.0 * DVirusInfectionDemo.HEALING_DISTANCE ));
        Bag mysteriousObjects = new Bag(hb.environment.getNeighborsWithinDistance( agentLocation, Math.min(50.0 * DVirusInfectionDemo.HEALING_DISTANCE, (double)DVirusInfectionDemo.AOI )));

        if( mysteriousObjects != null )
            {
            for( int i = 0 ; i < mysteriousObjects.numObjs ; i++ )
                {
                if( mysteriousObjects.objs[i] != null &&
                    mysteriousObjects.objs[i] != this )
                    {
                    // if agent is not human, wasted time....
                    if( ! (((DAgent)mysteriousObjects.objs[i]) instanceof DHuman ))
                        continue;
                    DHuman ta = (DHuman)(mysteriousObjects.objs[i]);
                    // if agent is already healthy, wasted time....
                    if( !ta.isInfected() )
                        continue;
                    if( hb.withinHealingDistance( this, agentLocation, ta, ta.agentLocation ) )
                        ta.setInfected( false );
                    else
                        {
                        if( getIsGreedy() )
                            {
                            double tmpDist = distanceSquared( agentLocation, ta.agentLocation );
                            if( tmpDist <  distance2DesiredLocation )
                                {
                                desiredLocation = ta.agentLocation;
                                distance2DesiredLocation = tmpDist;
                                }
                            }
                        }
                    }
                }
            }

        steps--;
        if( desiredLocation == null || !getIsGreedy() )
            {
            if(  steps <= 0 )
                {
                suggestedLocation = new Double2D((state.random.nextDouble()-0.5)*((DVirusInfectionDemo.WIDTH)/5-DVirusInfectionDemo.DIAMETER) +
                    //VirusInfectionDemo.XMIN
                    agentLocation.x 
                    //+VirusInfectionDemo.DIAMETER/2
                    ,
                    (state.random.nextDouble()-0.5)*((DVirusInfectionDemo.HEIGHT)/5-DVirusInfectionDemo.DIAMETER) +
                    agentLocation.y
                    //VirusInfectionDemo.YMIN
                    //+VirusInfectionDemo.DIAMETER/2
                    );
                steps = 100;
                }
            desiredLocation = suggestedLocation;
            }

        double dx = desiredLocation.x - agentLocation.x;
        double dy = desiredLocation.y - agentLocation.y;

            {
            double temp = 0.5 * /*Strict*/Math.sqrt(dx*dx+dy*dy);
            if( temp < 1 )
                {
                steps = 0;
                }
            else
                {
                dx /= temp;
                dy /= temp;
                }
            }

        if( ! hb.acceptablePosition( this, new Double2D(agentLocation.x + dx, agentLocation.y + dy) ) )
            {
            steps = 0;
            }
        else
            {
            agentLocation = new Double2D(agentLocation.x + dx, agentLocation.y + dy);
            //hb.environment.setObjectLocation(this,agentLocation);
            hb.environment.moveAgent(agentLocation, this);

            }
        }
/*
    protected Color goodColor = new Color(0,0,0);
    protected Color goodMarkColor = new Color(255,0,0);
    public final void draw(Object object, Graphics2D graphics, DrawInfo2D info)
        {
        double diamx = info.draw.width*DVirusInfectionDemo.DIAMETER;
        double diamy = info.draw.height*DVirusInfectionDemo.DIAMETER;

        graphics.setColor( goodColor );            
        graphics.fillOval((int)(info.draw.x-diamx/2),(int)(info.draw.y-diamy/2),(int)(diamx),(int)(diamy));
        graphics.setColor( goodMarkColor );
        graphics.fillRect((int)(info.draw.x-diamx/3),(int)(info.draw.y-diamy/16),(int)(diamx/1.5),(int)(diamy/8));
        graphics.fillRect((int)(info.draw.x-diamx/16),(int)(info.draw.y-diamy/3),(int)(diamx/8),(int)(diamy/1.5));
        }
 */   
    public String getType() { return "Good"; }

}
