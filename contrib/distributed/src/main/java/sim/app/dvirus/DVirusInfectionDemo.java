package sim.app.dvirus;
import java.util.ArrayList;



import sim.app.virus.Agent;
import sim.app.virus.Evil;
import sim.app.virus.Good;
import sim.app.virus.Human;
import sim.engine.DSimState;
import sim.field.continuous.DContinuous2D;
import sim.util.Bag;
import sim.util.Double2D;
import sim.util.Timing;

public class DVirusInfectionDemo extends DSimState{
	
    public DContinuous2D environment = null;

	
    private static final long serialVersionUID = 1;

    //Distributed Mason starts at 0,0, so use a width instead of min/max
    public static final double XMIN = 0;
    //public static final double XMAX = 800;
    
    public static final int WIDTH = 800;
    
    public static final double YMIN = 0;
    //public static final double YMAX = 600;
    
    public static final int HEIGHT = 600;


    public static final double DIAMETER = 8;

    public static final double HEALING_DISTANCE = 20;
    public static final double HEALING_DISTANCE_SQUARED = HEALING_DISTANCE * HEALING_DISTANCE;
    public static final double INFECTION_DISTANCE = 20;
    public static final double INFECTION_DISTANCE_SQUARED = INFECTION_DISTANCE * INFECTION_DISTANCE;
    
    public static final int NUM_HUMANS = 100;
    public static final int NUM_GOODS = 4;
    public static final int NUM_EVILS = 4;
    
    public static final int AOI = 20;


    /** Creates a VirusInfectionDemo simulation with the given random number seed. */
    public DVirusInfectionDemo(long seed)
        {
        super(seed, WIDTH, HEIGHT, AOI, false);
        
	    environment = new DContinuous2D(25, this);

        }
    
    boolean conflict( final DAgent agent1, final Double2D a, final DAgent agent2, final Double2D b )
    {
    	if( ( ( a.x > b.x && a.x < b.x+DIAMETER ) ||
            ( a.x+DIAMETER > b.x && a.x+DIAMETER < b.x+DIAMETER ) ) &&
            ( ( a.y > b.y && a.y < b.y+DIAMETER ) ||
            ( a.y+DIAMETER > b.y && a.y+DIAMETER < b.y+DIAMETER ) ) )
    		{
    		return true;
    		}
    	return false;
    }
    
    public boolean withinInfectionDistance( final DAgent agent1, final Double2D a, final DAgent agent2, final Double2D b )
    {
    	return ( (a.x-b.x)*(a.x-b.x)+(a.y-b.y)*(a.y-b.y) <= INFECTION_DISTANCE_SQUARED );
    }

    public boolean withinHealingDistance( final DAgent agent1, final Double2D a, final DAgent agent2, final Double2D b )
    {
    	return ( (a.x-b.x)*(a.x-b.x)+(a.y-b.y)*(a.y-b.y) <= HEALING_DISTANCE_SQUARED );
    }
    
    //this had to be changed to not use Continuous2D initially! This is because being done in root and not correct position
    boolean acceptablePositionInitial( final DAgent agent, final Double2D location , ArrayList<DAgent> agents)
    {
    	if( location.x < DIAMETER/2 || location.x > (WIDTH)/*environment.getXSize()*/-DIAMETER/2 ||
			location.y < DIAMETER/2 || location.y > (HEIGHT)/*environment.getYSize()*/-DIAMETER/2 ) {
    		return false;
    	}
	
    
    	//Bag mysteriousObjects = new Bag(environment.getNeighborsWithinDistance( location, 2*DIAMETER ));
    	Bag mysteriousObjects = new Bag(agents);


    	if( mysteriousObjects != null )
    	{
    		for( int i = 0 ; i < mysteriousObjects.numObjs ; i++ )
        	{
    			if( mysteriousObjects.objs[i] != null && mysteriousObjects.objs[i] != agent )
    			{
    				DAgent ta = (DAgent)(mysteriousObjects.objs[i]);
    				if( conflict( agent, location, ta, ta.agentLocation ) )
    					return false;
    			}
        	}
    	}
    	
    	
    
	return true;
    }
    
    
    
    
    boolean acceptablePosition( final DAgent agent, final Double2D location )
    {
    	if( location.x < DIAMETER/2 || location.x > (WIDTH)/*environment.getXSize()*/-DIAMETER/2 ||
			location.y < DIAMETER/2 || location.y > (HEIGHT)/*environment.getYSize()*/-DIAMETER/2 ) {
    		return false;
    	}
	
    	Bag mysteriousObjects = new Bag(environment.getNeighborsWithinDistance( location, 2*DIAMETER ));
	
    	if( mysteriousObjects != null )
    	{
    		for( int i = 0 ; i < mysteriousObjects.numObjs ; i++ )
        	{
    			if( mysteriousObjects.objs[i] != null && mysteriousObjects.objs[i] != agent )
    			{
    				DAgent ta = (DAgent)(mysteriousObjects.objs[i]);
    				if( conflict( agent, location, ta, environment.getObjectLocationLocal(ta) ) )
    					return false;
    			}
        	}
    	}
	return true;
    }

	@Override
	protected void startRoot()
	{
		ArrayList<DAgent> agents = new ArrayList<DAgent>();
	    for(int x=0;x<NUM_HUMANS+NUM_GOODS+NUM_EVILS;x++)
        {
	    	Double2D loc = null;
	    	DAgent agent = null;
	    	int times = 0;
	    	do
            {
            loc = new Double2D( random.nextDouble()*(WIDTH-DIAMETER)+XMIN+DIAMETER/2,
                random.nextDouble()*(HEIGHT-DIAMETER)+YMIN+DIAMETER/2 );
            if( x < NUM_HUMANS )
                agent = new DHuman( "Human"+x, loc );
            else if( x < NUM_HUMANS+NUM_GOODS )
                agent = new DGood( "Good"+(x-NUM_HUMANS), loc );
            else
                agent = new DEvil( "Evil"+(x-NUM_HUMANS-NUM_GOODS), loc );
            times++;
            if( times == 1000 )
                {
                // can't place agents, oh well
                break;
                }
            } while( !acceptablePositionInitial( agent, loc , agents) );
	    	
	    	agents.add(agent);
        }

		sendRootInfoToAll("agents", agents);
	}

	@Override
	public void start()
	{
		// TODO Auto-generated method stub
		super.start(); // do not forget this line
	    
		ArrayList<DAgent> agents = (ArrayList<DAgent>) getRootInfo("agents");
		System.out.println(agents.size());


		for (DAgent a : agents)
		{
			if (getPartition().getLocalBounds().contains(a.agentLocation)) {
				environment.addAgent(a.agentLocation, a, 0, 0, 1);
			}
		}

	}
	
	
	public static void main(final String[] args)
	{
		Timing.setWindow(20);
		doLoopDistributed(DVirusInfectionDemo.class, args);
		System.exit(0);
	}

}
