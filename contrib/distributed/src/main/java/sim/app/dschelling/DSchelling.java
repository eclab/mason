package sim.app.dschelling;

import java.util.ArrayList;

import sim.app.dflockers.DFlocker;
import sim.app.dflockers.DFlockers;
import sim.app.dheatbugs.DHeatBug;
import sim.app.dheatbugs.DHeatBugs;
import sim.app.schelling.Agent;
import sim.app.schelling.Schelling;
import sim.engine.DSimState;
import sim.field.grid.DDenseGrid2D;
import sim.field.grid.DDoubleGrid2D;
import sim.field.grid.DIntGrid2D;
import sim.field.grid.IntGrid2D;
import sim.util.Bag;
import sim.util.Double2D;
import sim.util.Int2D;
import sim.util.Interval;

public class DSchelling extends DSimState
{

    private static final long serialVersionUID = 1;

    public int gridHeight;
    public int gridWidth;
    public int neighborhood = 1;
    public int threshold = 3;
    public double redProbability = 0.3;
    public double blueProbability = 0.3;
    public double emptyProbability = 0.3;
    public double unavailableProbability = 0.1;

    // we presume that no one relies on these DURING a simulation
    public int getGridHeight()
    {
    	return gridHeight;
    }
    
    public void setGridHeight(int val)
    {
    	if (val > 0) gridHeight = val;
    }
    
    public int getGridWidth()
    {
    	return gridWidth;
	}
    
    public void setGridWidth(int val)
    {
    	if (val > 0) gridWidth = val;
	}
    
    public int getNeighborhood()
    {
    	return neighborhood;
    }
    
    public void setNeighborhood(int val)
    {
    	if (val > 0) neighborhood = val;
	}
    
    public int getThreshold()
    {
    	return threshold;
	}
    
    public void setThreshold(int val)
    {
    	if (val >= 0) threshold = val;
	}

    // some cutsie-pie probability sliders.  More work than necessary, but it was fun.
    public Object domRedProbability()
    {
    	return new Interval(0.0,1.0);
	}
    
    public double getRedProbability()
    {
    	return redProbability;
	}
    
    public void setRedProbability(double val) 
        { 
        if (val >= 0 && val <= 1.0)
            {
            redProbability = val;
            }
        }
    
    public Object domBlueProbability()
    {
    	return new Interval(0.0,1.0);
	}
    
    public double getBlueProbability()
    {
    	return blueProbability;
	}
    
    public void setBlueProbability(double val) 
        { 
        if (val >= 0 && val <= 1.0)
            {
            blueProbability = val;
            }
        }

    public Object domEmptyProbability()
    {
    	return new Interval(0.0,1.0);
	}
    
    public double getEmptyProbability()
    {
    	return emptyProbability;
	}
    
    public void setEmptyProbability(double val) 
        { 
        if (val >= 0 && val <= 1.0)
            {
            emptyProbability = val;
            }
        }

    public Object domUnavailableProbability()
    {
    	return new Interval(0.0,1.0);
	}
    
    public double getUnavailableProbability()
    {
    	return unavailableProbability;
	}
    
    public void setUnavailableProbability(double val) 
        { 
        if (val >= 0 && val <= 1.0)
            {
            unavailableProbability = val;
            double total = redProbability + blueProbability + emptyProbability; 
            if (total==0.0) total = 1.0;
            redProbability *= (1.0 - unavailableProbability)/total;
            blueProbability *= (1.0 - unavailableProbability)/total;
            emptyProbability *= (1.0 - unavailableProbability)/total;
            }
        }

    public DIntGrid2D neighbors;
    public Bag emptySpaces = new Bag();
    public static final  int EMPTY = 0;
    public static final int UNAVAILABLE = 1;
    public static final int RED = 2;
    public static final int BLUE = 3;


    /** Creates a Schelling simulation with the given random number seed. */
    public DSchelling(long seed)
        {
        this(seed, 100, 100);
        }
        
    public DSchelling(long seed, int width, int height)
        {
		super(seed, width, height, 10); //what should aoi be?
		gridWidth = width;
		gridHeight = height;
        createGrids();
        }
    


    protected void createGrids()
        {
        emptySpaces.clear();
        //neighbors = new IntGrid2D(gridWidth, gridHeight,0);
        neighbors = new DIntGrid2D(this);
        for(int x=0;x<gridWidth;x++)
        {
            for(int y=0;y<gridHeight;y++)
                {
                double d = random.nextDouble();
                if (d < redProbability) neighbors.set(new Int2D(x, y), RED);  //g[x][y] = RED;
                else if (d < redProbability + blueProbability) neighbors.set(new Int2D(x, y), BLUE); //g[x][y] = BLUE;
                else if (d < redProbability + blueProbability + emptyProbability) 
                    {
                	// g[x][y] = EMPTY; 
                	// emptySpaces.add(new Int2D(x,y));
                	
                	neighbors.set(new Int2D(x, y), EMPTY);
                	emptySpaces.add(new Int2D(x,y));
                	
                	
                    }
                    
                else neighbors.set(new Int2D(x, y), UNAVAILABLE); //g[x][y] = UNAVAILABLE;
                }
        }
        }
    

    
    /** Resets and starts a simulation */
    public void start()
        {
    	
    	System.out.println("hello");
    	
        super.start();  // clear out the schedule
        
        // make new grids
        createGrids();
        System.out.println("partition "+this.getPID()+" bounds "+gridWidth+" "+gridHeight);
        for(int x=0;x<gridWidth;x++)
            for(int y=0;y<gridHeight;y++)
                {
                schedule.scheduleRepeating(new Agent(x,y));
                }
        }
    

    
	public static void main(final String[] args)
	{
		doLoopDistributed(Schelling.class, args);
		System.exit(0);
	}

}
