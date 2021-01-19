package sim.app.geo.omolandCA;

import java.awt.BasicStroke;
import java.awt.Canvas;
import java.awt.Color;
/**
 *
 * @author gmu
 */
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.geom.Line2D;
//import javax.swing.*;

public class Legend extends Canvas {

	public void paint(final Graphics legend) {

//        legend.setColor(Color.red);
//        legend.drawRect(10, 10, 260, 660);
//

		final Graphics2D leg = (Graphics2D) legend;
		leg.scale(0.7, 0.7);
		final Line2D line = new Line2D.Double(20, 60, 70, 75);
		leg.setColor(Color.lightGray);
		leg.setStroke(new BasicStroke(3));
		leg.draw(line);

		// river

		leg.setColor(Color.blue);
		leg.drawLine(20, 85, 70, 100);

		// adminstrative boundary
		leg.setColor(new Color(176, 48, 96));
		leg.drawLine(20, 105, 70, 120);

		// agent
		leg.setColor(Color.BLACK);
		leg.fillOval(20, 180, 25, 25);
		// leg.fillOval(20, 160, 20, 20

		leg.setColor(Color.BLUE); // livestock
		leg.fillOval(20, 300, 25, 25);
//

		// landscape - boundaries
		leg.setColor(new Color(0, 225, 0));
		leg.fillRect(20, 380, 30, 30);

		leg.setColor(new Color(255, 153, 0));
		leg.fillRect(20, 420, 30, 30);

		leg.setColor(Color.MAGENTA);
		leg.fillRect(20, 460, 30, 30);

		leg.setColor(new Color(0, 0, 255));
		leg.fillRect(20, 500, 30, 30);

		// crops

		leg.setColor(new Color(255, 0, 0));
		leg.fillRect(20, 590, 30, 30);

		leg.setColor(new Color(40, 128, 255));
		leg.fillRect(20, 630, 30, 30);

		leg.setColor(new Color(128, 0, 0));
		leg.fillRect(20, 670, 30, 30);

		leg.setColor(new Color(0, 205, 205));
		leg.fillRect(20, 710, 30, 30);

		// Graphics2D fontL = (Graphics2D)legend;
		final Font f = new Font("Serif", Font.BOLD, 26);
		leg.setFont(f);

		leg.setColor(Color.black);

		leg.drawString("LEGEND", 60, 40);

		final Font f2 = new Font("Serif", Font.BOLD, 24);
		leg.setFont(f2);

		leg.setColor(Color.black);

		leg.drawString("Agents", 20, 160);

		leg.drawString("Livestock", 20, 290);

		leg.drawString("Landuse", 20, 370);

		leg.drawString("Crop", 20, 580);

		final Font f3 = new Font("Serif", Font.PLAIN, 24);
		leg.setFont(f3);

		leg.setColor(Color.black);

		leg.drawString("Road", 90, 80);
		legend.drawString("River", 90, 105);
		legend.drawString("Woreda Boundary", 90, 130);

		leg.drawString("Household", 70, 200);

		leg.drawString("Livestock", 70, 320);

		leg.drawString("Grassland", 70, 400);
		leg.drawString("Small-scale Farmland", 70, 440);
		leg.drawString("Large-scale Farmland", 70, 480);
		leg.drawString("Lake", 70, 520);

		leg.drawString("Maize", 70, 615);
		leg.drawString("Sorghum", 70, 655);
		leg.drawString("Wheat", 70, 695);
		leg.drawString("Teff", 70, 735);

	}

}
