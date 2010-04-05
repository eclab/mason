/*
 * CampusWorld.java
 *
 * $Id: CampusWorld.java,v 1.1 2010-04-05 17:27:19 mcoletti Exp $
 */
package sim.app.geo.campusworld;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.planargraph.Node;
import java.io.*;
import java.util.*; 

import java.util.logging.Level;
import java.util.logging.Logger;
import sim.engine.SimState;
import sim.field.geo.GeomField;
import sim.io.geo.*;
import sim.util.geo.Network;
import sim.util.*; 
import sim.util.geo.GeomWrapper;

/** Set up a GeoField with a number of points and a corresponding portrayal.
 *
 * @author mcoletti
 */
public class CampusWorld extends SimState
{
    public int numAgents = 1000;
//    public int numAgents = 1;

//	private static final String dataDirectory = "../../../../../data/";
    private static final String dataDirectory = "./data/";

    // where all the stream geometry lives
    public GeomField walkways = new GeomField();
    public GeomField roads = new GeomField();
    public GeomField buildings = new GeomField();

    // where all the agents live
    public GeomField agents = new GeomField();

    // Open simple Shape file of stream.
	//OGRImporter importer = new OGRImporter();
	//GeoToolsImporter importer = new GeoToolsImporter(); 
	ShapeFileImporter importer = new ShapeFileImporter();
    
	
	//OGRExporter exporter = new OGRExporter();
	ShapeFileExporter exporter = new ShapeFileExporter();
	//GeoToolsExporter exporter = new GeoToolsExporter(); 
	
    // Stores transportation network connections
    public Network network = new Network();
    public GeomField junctions = new GeomField(); // nodes for intersections

    public CampusWorld(long seed)
    {
        super(seed);

    }

    public 
    int getNumAgents()
    {
        return numAgents;
    }

    public
    void setNumAgents(int n)
    {
        numAgents = n;
    }




    /** add agents to the simulation
     */
    private void addAgents()
    {
        for (int i = 0; i < numAgents; i++)
        {
            Agent a = new Agent();
			agents.addGeometry(new GeomWrapper(a.getGeometry(), null));
            a.start(this);
            schedule.scheduleRepeating(a);
        }
    }
    
	public static boolean testing = false; 
	
	
	public void finish()
	{
		/*if (!testing) { 
			try { 
				exporter.write("testing", "ESRI Shapefile", buildings); 
			} catch (IOException e) { e.printStackTrace(); } 
		}  */
		
		agents.clear(); 
	}
	
    
    public void start()
    {
        super.start();
        try
        {
            // We want to save the MBR so that we can ensure that all GeomFields
            // cover identical area.
            System.out.println("reading buildings layer");
			Bag masked = new Bag(); 
			masked.add("NAME"); 
			//masked.add("FLOORS"); 
			masked.add("ADDR_NUM"); 
			
			//if (!testing)
				importer.ingest(dataDirectory + "bldg.shp", buildings, null);
			//else 
			//	importer.ingest("./testing.shp", buildings, null); 
			
			//exporter.write("testing", "ESRI Shapefile", buildings); 
			//System.exit(-1); 
			
			
            Envelope MBR = buildings.getMBR();

            System.out.println("reading roads layer");
            importer.ingest(dataDirectory + "roads.shp", roads, null);

            MBR.expandToInclude(roads.getMBR());

            System.out.println("reading walkways layer");
            importer.ingest(dataDirectory + "walk_ways.shp", walkways, null);

            MBR.expandToInclude(walkways.getMBR());

            System.out.println("Done reading data");

            // Now synchronize the MBR for all GeomFields to ensure they cover the same area
            buildings.setMBR(MBR);
            roads.setMBR(MBR);
            walkways.setMBR(MBR);

            network.createFromGeomField(walkways);

            addIntersectionNodes( network.nodeIterator(), junctions ) ;
            addAgents();
            agents.setMBR(MBR);
			 
        }
        catch (FileNotFoundException ex)
        {
            Logger.getLogger(CampusWorld.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    


    /** adds nodes corresponding to road intersections to GeomField
     *
     * @param nodeIterator Points to first node
     * @param intersections GeomField containing intersection geometry
     *
     * Nodes will belong to a planar graph populated from LineString network.
     */
    private void addIntersectionNodes(Iterator nodeIterator, GeomField intersections)
    {
        GeometryFactory fact = new GeometryFactory();
        Coordinate coord = null;
        Point point = null;
        int counter = 0;

        while (nodeIterator.hasNext())
        {
            Node node = (Node) nodeIterator.next();

            if ( counter % 10 == 0 )
                System.out.print(".");

            coord = node.getCoordinate();
            point = fact.createPoint(coord);

            junctions.addGeometry(new GeomWrapper(point, null));
            counter++;
        }
    }
	
    public static void main(String[] args)
    {
        doLoop(CampusWorld.class, args);
        System.exit(0);
    }
}
