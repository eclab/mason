package sim.app.dkeepaway;

import java.awt.Color;
import java.rmi.RemoteException;
import java.util.ArrayList;

import sim.app.dflockers.DFlocker;
import sim.app.dflockers.DFlockers;
import sim.engine.DSimState;
import sim.field.continuous.Continuous2D;
import sim.field.continuous.DContinuous2D;
import sim.util.Double2D;
import sim.util.Timing;

public class DKeepaway extends DSimState{

    private static final long serialVersionUID = 1;

    public DContinuous2D<DEntity> fieldEnvironment;

    /** @todo handle realocation of grids when these two are changed */
    public double xMin = 0;
    public double xMax = 100;
    public double yMin = 0;
    public double yMax = 100;
    

    /** Creates a Keepaway simulation with the given random number seed. */
    public DKeepaway(long seed)
        {
        this(seed, 100, 100, 100);
        }
        
    public DKeepaway(long seed, int width, int height, int aoi)
        {
        super(seed, width, height, aoi, false);
        xMax = width; yMax = height;
        createGrids();
        }

    void createGrids()
        {       
        fieldEnvironment = new DContinuous2D<DEntity>(25, this);
        }
    
    
    @Override
    protected void startRoot()
        {
        

        ArrayList<DEntity> entities = new ArrayList<DEntity>();

        DBot b;
        double x,y;
        
        // bot 1-1  
        x = random.nextDouble()*xMax;
        y = random.nextDouble()*yMax;
        b = new DBot(x, y, Color.red);
        b.cap = 0.65;
        entities.add(b);
        
        // bot 2-1   
        x = random.nextDouble()*xMax;
        y = random.nextDouble()*yMax;
        b = new DBot(x, y, Color.blue);
        b.cap = 0.5;
        entities.add(b);
        
        
        // bot 2-2  
        x = random.nextDouble()*xMax;
        y = random.nextDouble()*yMax;
        b = new DBot(x, y, Color.blue);
        b.cap = 0.5;
        entities.add(b);
      
        // bot 2-3  
        x = random.nextDouble()*xMax;
        y = random.nextDouble()*yMax;
        b = new DBot(x, y, Color.blue);
        b.cap = 0.5;
        entities.add(b);
      
        // ball
        DBall ba;
        x = random.nextDouble()*xMax;
        y = random.nextDouble()*yMax;
        ba = new DBall(x, y);
        entities.add(ba);
        try {
            registerDistinguishedObject(ba);
            } catch (RemoteException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            }
     
        

        sendRootInfoToAll("entities", entities);
        }    
    
    
    /** Resets and starts a simulation */
    public void start()
        {
        super.start();  // clear out the schedule

        ArrayList<DFlocker> entities = (ArrayList<DFlocker>) getRootInfo("entities");

        for (Object p : entities)
            {
            DEntity a = (DEntity) p;
            Double2D temploc = new Double2D(a.loc.getX(), a.loc.getY());
            if (getPartition().getLocalBounds().contains(temploc)) {
                fieldEnvironment.addAgent(temploc, a, 0, 0, 1);
                }
                        

            }
        
  
        }

    public static void main(String[] args)
        {
        Timing.setWindow(20);
        doLoopDistributed(DKeepaway.class, args);
        System.exit(0);
        }    

        
    }
