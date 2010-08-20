/*
 * TouchingWorld.java
 *
 * $Id: TouchingWorld.java,v 1.3 2010-08-20 00:52:37 mcoletti Exp $
 */
package sim.app.geo.touchingworld;

import com.vividsolutions.jts.geom.Polygon;
import com.vividsolutions.jts.io.ParseException;
import com.vividsolutions.jts.io.WKTReader;
import java.util.logging.Level;
import java.util.logging.Logger;
import sim.engine.*; 
import sim.field.geo.*; 
import sim.util.geo.*; 

/** Set up a GeoField with a number of points and a corresponding portrayal.
 *
 * @author mcoletti
 */
public class TouchingWorld extends SimState
{
    private static final long serialVersionUID = 7508584126243256514L;

	// where all the shapes geometry lives
    public GeomVectorField shapes = new GeomVectorField();

    // currently selected shape
//    public GeomVectorField selectedShape = new GeomVectorField();
    public MasonGeometry selectedShape = null;

    // responsible for changing selected shape
    public Mover mover = new Mover();

	

    public TouchingWorld(long seed)
    {
        super(seed);
    }

    
    
    @Override
	public void start()
    {
        super.start();

        createWorld();

        this.shapes.getMBR().expandBy(5.0);

        // Randomly select a district as "current"
        this.selectedShape = (MasonGeometry) shapes.getGeometries().objs[random.nextInt(shapes.getGeometries().size())];

//        mover.setCurrentShape((Polygon) district.geometry);
//        selectedShape.addGeometry(district);
        
		// ensure both GeomFields cover same area
//		selectedShape.setMBR(shapes.getMBR());

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
        unSelectShape(this.selectedShape);
        
        this.selectedShape = shape;
    }

    public void unSelectShape(MasonGeometry shape)
    {
        this.selectedShape = null;
    }

}
