/* 
 * Copyright 2011 by Mark Coletti, Keith Sullivan, Sean Luke, and
 * George Mason University Mason University Licensed under the Academic
 * Free License version 3.0
 *
 * See the file "LICENSE" for more information
 * 
 * $Id$
 */
package sim.app.geo.touchingworld;

import com.vividsolutions.jts.geom.Polygon;
import com.vividsolutions.jts.io.ParseException;
import com.vividsolutions.jts.io.WKTReader;
import java.util.logging.Level;
import java.util.logging.Logger;
import sim.engine.SimState;
import sim.field.geo.GeomVectorField;
import sim.util.geo.MasonGeometry;

/** This demonstrates use of touching geometries.
 *
 */
public class TouchingWorld extends SimState
{
    private static final long serialVersionUID = 7508584126243256514L;

    public static final int WIDTH = 300; 
    public static final int HEIGHT = 300; 
    
	// where all the shapes geometry lives
    public GeomVectorField shapes = new GeomVectorField(WIDTH, HEIGHT);

    // currently selected shape
    public GeomVectorField selectedShape = new GeomVectorField(WIDTH, HEIGHT);

    // responsible for changing selected shape
    public Mover mover = new Mover();

	

    public TouchingWorld(long seed)
    {
        super(seed);
    }

    
    
	public void start()
    {
        super.start();

        createWorld();

        // Bump out the main viewport a bit for aesthetics
        this.shapes.getMBR().expandBy(5.0);

		// ensure both GeomFields cover same area
		selectedShape.setMBR(shapes.getMBR());

        // Randomly select a district as "current"
        selectShape((MasonGeometry) shapes.getGeometries().objs[random.nextInt(shapes.getGeometries().size())]);

        schedule.scheduleRepeating(mover);
    }
    
    public static void main(String[] args)
    {
        doLoop(TouchingWorld.class, args);
        System.exit(0);
    }

    private
    void createWorld()
    {
        try
        {
            WKTReader rdr = new WKTReader();
            Polygon polygon = null;
            
            polygon = (Polygon) (rdr.read("POLYGON ((0 20, 10 30, 10 20, 0 20))"));
            this.shapes.addGeometry(new MasonGeometry(polygon));

            polygon = (Polygon) (rdr.read("POLYGON ((10 10, 10 30, 15 30, 15 10, 10 10))"));
            this.shapes.addGeometry(new MasonGeometry(polygon));

            polygon = (Polygon) (rdr.read("POLYGON ((15 10, 15 15, 30 15, 30 10, 15 10))"));
            this.shapes.addGeometry(new MasonGeometry(polygon));

            polygon = (Polygon) (rdr.read("POLYGON ((15 10, 30 10, 25 5, 15 10))"));
            this.shapes.addGeometry(new MasonGeometry(polygon));

            polygon = (Polygon) (rdr.read("POLYGON ((25 15, 25 25, 30 15, 25 15))"));
            this.shapes.addGeometry(new MasonGeometry(polygon));

            polygon = (Polygon) (rdr.read("POLYGON ((30 15, 25 25, 30 25, 30 15))"));
            this.shapes.addGeometry(new MasonGeometry(polygon));
        }
        catch (ParseException ex)
        {
            Logger.getLogger(TouchingWorld.class.getName()).log(Level.SEVERE, null, ex);
        }

    }

    public void selectShape(MasonGeometry shape)
    {
        selectedShape.clear();

        // selecteShape.clear() will have zeroed the MBR, so we need to resync
        // selectedShape with the MBR holding all the blue shapes.  Otherwise
        // the selected shape will take up the entire viewport.
        selectedShape.setMBR(shapes.getMBR());
        
        selectedShape.addGeometry(shape);
    }

}
