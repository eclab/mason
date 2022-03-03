package sim.app.dvirus.display;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.geom.Ellipse2D;

import sim.app.dvirus.DAgent;
import sim.app.dvirus.DEvil;
import sim.app.dvirus.DGood;
import sim.app.dvirus.DHuman;
import sim.app.dvirus.DVirusInfectionDemo;
import sim.app.virus.VirusInfectionDemo;
import sim.portrayal.DrawInfo2D;
import sim.portrayal.SimplePortrayal2D;

public class DAgentPortrayal extends SimplePortrayal2D{
	
	
	DAgent portrayedAgent;
	
    protected Color humanColor = new Color(192,128,128);
    protected Color infectedColor = new Color(128,255,128);
    protected Color goodColor = new Color(0,0,0);
    protected Color evilColor = new Color(255,0,0);

    
    public DAgentPortrayal(DAgent d) {
    	this.portrayedAgent = d;
    }
    
    
    public final void draw(Object object, Graphics2D graphics, DrawInfo2D info)
        {
        double diamx = info.draw.width*VirusInfectionDemo.DIAMETER;
        double diamy = info.draw.height*VirusInfectionDemo.DIAMETER;
    
        
        if (this.portrayedAgent.getClass() == DHuman.class) {
            if (((DHuman)portrayedAgent).isInfected())
                graphics.setColor( infectedColor );
            else graphics.setColor ( humanColor ); 
        }
        
        else if (this.portrayedAgent.getClass() == DGood.class) {
        	 graphics.setColor( goodColor );
        }
        
        else if (this.portrayedAgent.getClass() == DEvil.class) {
       	 graphics.setColor(evilColor );
        }
       	 
       	 else {
           	 graphics.setColor(humanColor );
       	 }

       	 
             
        
        graphics.fillOval((int)(info.draw.x-diamx/2),(int)(info.draw.y-diamy/2),(int)(diamx),(int)(diamy));
        }
    
    public boolean hitObject(Object object, DrawInfo2D info)
    {
    double diamx = info.draw.width*DVirusInfectionDemo.DIAMETER * 10.0;
    double diamy = info.draw.height*DVirusInfectionDemo.DIAMETER * 10.0;
    

    Ellipse2D.Double ellipse = new Ellipse2D.Double( (int)(info.draw.x-diamx/2),(int)(info.draw.y-diamy/2),(int)(diamx),(int)(diamy) );
    return ( ellipse.intersects( info.clip.x, info.clip.y, info.clip.width, info.clip.height ) );
    }

}
