package conflictdiamonds;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.geom.Line2D;
import java.awt.BasicStroke;
import java.awt.Canvas;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.geom.Line2D;

/**
 * This builds the legend
 * 
 * @author bpint
 */
public class Legend extends Canvas {
    
    public void paint(Graphics legend) {   
        Graphics2D leg = (Graphics2D)legend;
    	leg.scale(0.7, 0.7);
    	Line2D line = new Line2D.Double(20, 60, 70, 70);
    	leg.setColor(Color.lightGray);
    	leg.setStroke(new BasicStroke(3));
    	//leg.draw(line);

        // agent 
        leg.setColor(Color.blue);
        leg.fillOval(20, 90, 20, 20);
        
        leg.setColor(Color.red);
        leg.fillOval(20, 120, 20, 20);
        
        Font f = new Font("Serif", Font.BOLD, 24);       
        leg.setFont(f);   
        
        leg.setColor(Color.black);
        
        leg.drawString("LEGEND", 60, 40);        
        
        Font f2 = new Font("Serif", Font.BOLD, 18);       
        leg.setFont(f2);   
        
        leg.setColor(Color.black);
        
        leg.drawString("Agent's Status", 20, 80);
        
        Font f3 = new Font("Serif", Font.PLAIN, 20);       
        leg.setFont(f3);   
        
        leg.setColor(Color.black);
        
        leg.drawString("Miners", 70, 105);
        leg.drawString("Rebels", 70, 135);
    }
    
    
}
