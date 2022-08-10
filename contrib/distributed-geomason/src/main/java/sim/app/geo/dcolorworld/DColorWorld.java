package sim.app.geo.dcolorworld;

import sim.app.geo.colorworld.CountingGeomWrapper;
//import sim.app.geo.colorworld.data.ColorWorldData;

import sim.app.geo.dcolorworld.data.DColorWorldData;


import java.io.FileNotFoundException;
import java.net.URL;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Point;

import sim.engine.SimState;
import sim.field.geo.DGeomVectorField;
import sim.field.geo.GeomVectorField;
import sim.io.geo.ShapeFileImporter;
import sim.util.Bag;
import sim.util.Double2D;
import sim.util.geo.MasonGeometry;


import sim.engine.DSimState;

public class DColorWorld extends DSimState{
	
    private static final long serialVersionUID = -2568637684893865458L;


	public static final int WIDTH = 300; 
	public static final int HEIGHT = 300; 
	public static final int discretization = 6;
	public static final int aoi = 1;// TODO what value???


	// number of agents in the simulation
    public static int NUM_AGENTS = 20;

    // where all the county geometry lives
    //we will send this to all partitions
    public GeomVectorField county = new GeomVectorField(WIDTH, HEIGHT);

    // where all the agents live.  We use a GeomVectorField since we want to determine how 
    // many agents are inside each district.  The most efficient way to do this is via 
    // the GeomVectorField's spatial indexing.  
    //public  GeomVectorField agents = new GeomVectorField(WIDTH, HEIGHT);
    public final DGeomVectorField<DAgent> agents;
;
    

    // getters and setters for inspectors
    public int getNumAgents() { return NUM_AGENTS; }
    public void setNumAgents(int a) { if (a > 0) NUM_AGENTS = a; }



    public DColorWorld(long seed)
    {
		super(seed, WIDTH, HEIGHT, aoi, false);
		

        // this line allows us to replace the standard MasonGeometry with our
        // own subclass of MasonGeometry; see CountingGeomWrapper.java for more info.
        // Note: this line MUST occur prior to ingesting the data
        URL politicalBoundaries = DColorWorldData.class.getResource("pol.shp");
        URL politicalDB = DColorWorldData.class.getResource("pol.dbf");

        Bag empty = new Bag();
        try
        {
            ShapeFileImporter.read(politicalBoundaries, politicalDB, county, empty, CountingGeomWrapper.class);
        } catch (Exception ex)
        {
            Logger.getLogger(DColorWorld.class.getName()).log(Level.SEVERE, null, ex);
        }

        // we use either the ConvexHull or Union to determine if the agents are within
        // Fairfax county or not
        county.computeConvexHull();
        county.computeUnion();
        
		agents = new DGeomVectorField<DAgent>(discretization, this, county.getMBR());

        

    }
    
	@Override
	protected void startRoot() {
		
		ArrayList<DAgent> out_agents = new ArrayList<DAgent>();


        for (int i = 0; i < NUM_AGENTS; i++)
            {
                // pick a random political region to plop the agent in
                Bag allRegions = county.getGeometries();

                if (allRegions.isEmpty())
                    {
                        // Something went wrong.  We *should* have regions.
                        throw new RuntimeException("No regions found.");
                    }
                MasonGeometry region = ((MasonGeometry)allRegions.objs[random.nextInt(allRegions.numObjs)]);
           
                // give each agent a random direction to initially move in
                DAgent a = new DAgent(random.nextInt(8));

                // set each agent in the center of corresponding region
                a.setLocation(region.getGeometry().getCentroid());
                
                
                out_agents.add(a);

                // place the agents in the GeomVectorField
                //agents.getStorage().getGeomVectorField().addGeometry(new MasonGeometry(a.getGeometry()));
                //this will be done when added

                // add the new agent the schedule
                //schedule.scheduleRepeating(a);
            }      
		sendRootInfoToAll("out_agents", out_agents);
	}
	
	@Override
	public void start()
	{
		super.start();
        agents.getStorage().getGeomVectorField().clear(); // remove any agents from previous runs

        agents.getStorage().globalEnvelope = agents.getStorage().getGeomVectorField().getMBR(); //this is hacky, figure out better way

		ArrayList<DAgent> out_agents = (ArrayList<DAgent>) getRootInfo("out_agents");
		for (Object p : out_agents) {
			DAgent a = (DAgent) p;
			
			Point pt = (Point)a.getMasonGeometry().getGeometry();
			
			Coordinate c = new Coordinate(pt.getX(), pt.getY());
			
			Double2D partSpacePoint = agents.convertJTSToPartitionSpace(c);
			
			if (partition.getLocalBounds().contains(partSpacePoint)); /////
				agents.addAgent(partSpacePoint, a, 0, 0, 1);
		}
		
		
		
		
	}
	
	public static void main(final String[] args)
	{
		doLoopDistributed(DColorWorld.class, args);
		System.exit(0);
	}

}
