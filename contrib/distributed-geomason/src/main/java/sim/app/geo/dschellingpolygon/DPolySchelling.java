//so I think in this version, Person is the agent that corresponds to a Polygon region?

package sim.app.geo.dschellingpolygon;

import java.net.URL;

import java.util.ArrayList;

import com.vividsolutions.jts.geom.Coordinate;


import sim.app.geo.schellingpolygon.data.SchellingPolygonData;
import sim.engine.DSimState;
import sim.field.geo.DGeomVectorField;
import sim.field.geo.GeomVectorField;
import sim.io.geo.ShapeFileImporter;
import sim.util.Bag;
import sim.util.Double2D;

public class DPolySchelling extends DSimState{
	

	
	private static final long serialVersionUID = 1L;
	
	public static final int width = 100;
	public static final int height = 100;
	public static final int aoi = 1;// TODO what value???
	
	
    // storing the data
    public DGeomVectorField world;
    ArrayList<DPolygon> polys = new ArrayList<DPolygon>();
    ArrayList<DPerson> people = new ArrayList<DPerson>();
    // used by PolySchellingWithUI to keep track of the percent of unhappy Persons
    int totalReds = 0;
    int totalBlues = 0;
	public static final int discretization = 1; //what should this be?

    
    
    public DPolySchelling(long seed) {
		super(seed, width, height, aoi, false);
		world = new DGeomVectorField<DPolygon>(discretization, this);;
	}




	@Override
	protected void startRoot() {
        try // to import the data from the shapefile
        {
            URL wardsFile = SchellingPolygonData.class.getResource("1991_wards_disolved_Project.shp");
            URL wardsDB  = SchellingPolygonData.class.getResource("1991_wards_disolved_Project.dbf");

            ShapeFileImporter.read(wardsFile, wardsDB, world.getStorage().getGeomVectorField(), DPolygon.class);

        } catch (Exception ex)
        {
            System.out.println("Error opening shapefile!" + ex);
            System.exit(-1);
        }
        
        // copy over the geometries into a list of Polygons
        Bag ps = world.getStorage().getGeomVectorField().getGeometries();
        polys.addAll(ps);
        world.getStorage().getGeomVectorField().clear(); //we need to do this because we will split up the polygons per partition!

        ArrayList<DPolygon> poly_list = new ArrayList<DPolygon>();
        // process the polygons for neighbor and Person info
        for (int i = 0; i < polys.size(); i++)
        {
            DPolygon p1 = polys.get(i);
            p1.init();
            
            poly_list.add(p1);
        }
		sendRootInfoToAll("poly_list", poly_list);
	}

    /** Import the data and then set up the simulation */
    public void start()
    {
        super.start();
        
		ArrayList<DPolygon> poly_list = (ArrayList<DPolygon>) getRootInfo("poly_list");

		for (Object p : poly_list)
		{
			DPolygon a = (DPolygon) p;
	        Double2D coord = new Double2D(a.getMasonGeometry().geometry.getCoordinate().x, a.getMasonGeometry().geometry.getCoordinate().y);

	        
	        
	 
	        
			if (partition.getLocalBounds().contains(coord)) {
				
				world.addAgent(coord, a, 0, 0, 1);
			    polys.add(a);
			    
			}
		}
		
        // process the polygons for neighbor and Person info
        for (int i = 0; i < polys.size(); i++)
        {
            DPolygon p1 = polys.get(i);
			
            for (int j = i + 1; j < polys.size(); j++)
            {
                DPolygon p2 = polys.get(j);
                if (p1.getMasonGeometry().geometry.touches(p2.getMasonGeometry().geometry))
                {
                    p1.neighbors.add(p2);
                    p2.neighbors.add(p1);
                }
            }

            if (p1.soc == null) // no agent is initialized in this location
            {
                continue;
            } else if (p1.soc.equals("RED"))
            { // a red Person is initialized here
                DPerson p = new DPerson("RED");
                p.updateLocation(p1);
                totalReds++;
                people.add(p);
            } else if (p1.soc.equals("BLUE"))
            { // a blue Person is initialized here
                DPerson p = new DPerson("BLUE");
                p.updateLocation(p1);
                totalBlues++;
                people.add(p);
            }
		}

        //poly instead
        int i = 0;
        for (DPolygon p : polys)
        {
            schedule.scheduleRepeating(i, p, polys.size());
            i++;
        }


    }


	public static void main(final String[] args)
	{
		doLoopDistributed(DPolySchelling.class, args);
		System.exit(0);
	}

}
