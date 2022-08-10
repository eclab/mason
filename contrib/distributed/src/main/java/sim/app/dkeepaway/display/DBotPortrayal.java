package sim.app.dkeepaway.display;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.geom.Ellipse2D;

import sim.app.dkeepaway.DBot;
import sim.app.dkeepaway.DEntity;
import sim.portrayal.DrawInfo2D;
import sim.portrayal.SimplePortrayal2D;

public class DBotPortrayal extends SimplePortrayal2D{
	

		
		
		DBot portrayedBot;
		



	    
	    public DBotPortrayal(DBot d) {
	    	this.portrayedBot = d;
	    }
	    
	    
	    public final void draw(Object object, Graphics2D graphics, DrawInfo2D info)
	        {
	        double diamx = info.draw.width;
	        double diamy = info.draw.height;
	    
	                
	        graphics.setColor( portrayedBot.c );


	       	 
	             
	        
	        graphics.fillOval((int)(info.draw.x-diamx/2),(int)(info.draw.y-diamy/2),(int)(diamx),(int)(diamy));
	        }
	    
	    public boolean hitObject(Object object, DrawInfo2D info)
	    {
	    double diamx = info.draw.width * 10.0;
	    double diamy = info.draw.height * 10.0;
	    

	    Ellipse2D.Double ellipse = new Ellipse2D.Double( (int)(info.draw.x-diamx/2),(int)(info.draw.y-diamy/2),(int)(diamx),(int)(diamy) );
	    return ( ellipse.intersects( info.clip.x, info.clip.y, info.clip.width, info.clip.height ) );
	    }

	


}
