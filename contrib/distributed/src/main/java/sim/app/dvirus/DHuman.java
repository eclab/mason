package sim.app.dvirus;

import java.awt.Color;

import java.awt.Graphics2D;

import sim.engine.SimState;
import sim.portrayal.DrawInfo2D;
import sim.util.Double2D;

public class DHuman extends DAgent{
    private static final long serialVersionUID = 1;

    protected boolean infected = false;
    public final boolean isInfected() { return infected; }
    public final void setInfected( boolean b ) { infected = b; }

    public DHuman( String id, Double2D location ) 
        {
        super( id, location );
        try
            {
            intID = Integer.parseInt( id.substring(5) ); // "Human"
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

        steps--;
        if( desiredLocation == null || steps <= 0 )
            {
            desiredLocation = new Double2D((state.random.nextDouble()-0.5)*((DVirusInfectionDemo.WIDTH)/5-DVirusInfectionDemo.DIAMETER) +
                //VirusInfectionDemo.XMIN
                agentLocation.x 
                //+VirusInfectionDemo.DIAMETER/2
                ,
                (state.random.nextDouble()-0.5)*((DVirusInfectionDemo.HEIGHT)/5-DVirusInfectionDemo.DIAMETER) +
                agentLocation.y
                //VirusInfectionDemo.YMIN
                //+VirusInfectionDemo.DIAMETER/2
                );
            steps = 50+state.random.nextInt(50);
            }

        double dx = desiredLocation.x - agentLocation.x;
        double dy = desiredLocation.y - agentLocation.y;

            {
            double temp = /*Strict*/Math.sqrt(dx*dx+dy*dy);
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

        if( ! hb.acceptablePosition( this, new Double2D( agentLocation.x + dx, agentLocation.y + dy ) ) )
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

    protected Color humanColor = new Color(192,128,128);
    protected Color infectedColor = new Color(128,255,128);
    public final void draw(Object object, Graphics2D graphics, DrawInfo2D info)
        {
        double diamx = info.draw.width*DVirusInfectionDemo.DIAMETER * 10.0;
        double diamy = info.draw.height*DVirusInfectionDemo.DIAMETER * 10.0;
        
 
        
        if (isInfected())
            graphics.setColor( infectedColor );
        else graphics.setColor ( humanColor ); 
        graphics.fillOval((int)(info.draw.x-diamx/2),(int)(info.draw.y-diamy/2),(int)(diamx),(int)(diamy));
        }


    public String getType()
        {
        if( isInfected() )
            return "Infected Human";
        else
            return "Healthy Human";
        }

}
