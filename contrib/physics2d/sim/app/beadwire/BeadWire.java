package sim.app.beadwire;

import sim.engine.*;
import sim.field.continuous.*;
import ec.util.*;
import sim.physics2D.PhysicsEngine2D;
import sim.physics2D.util.*;
import sim.util.Double2D;

public class BeadWire extends SimState
    {
    public double xMin = 0;
    public double xMax = 100;
    public double yMin = 0;
    public double yMax = 100;
        
    public Continuous2D fieldEnvironment;
                        
    public BeadWire(long seed)
        {
        this(seed, 200, 200);
        }
        
    public BeadWire(long seed, int width, int height)
        {
        super(seed);
        xMax = width; 
        yMax = height;
        createGrids();
        }
                
    void createGrids()
        {       
        fieldEnvironment = new Continuous2D(25, (xMax - xMin), (yMax - yMin));
        }
        
    boolean applyConstraints = true;
    public boolean getApplyConstraints() { return applyConstraints; }
    public void setApplyConstraints(boolean val) { applyConstraints = val; }
                
    // Resets and starts a simulation
    public void start()
        {
        super.start();  // clear out the schedule
        createGrids();
                
        PhysicsEngine2D objPE = new PhysicsEngine2D();

        Wire wire = new Wire();
        fieldEnvironment.setObjectLocation(wire, new sim.util.Double2D(0, 0));
                
        Bead bead = new Bead(new Double2D(50, 50), new Double2D(0, 0), 3);
        bead.applyConstraints = applyConstraints;
        fieldEnvironment.setObjectLocation(bead, new sim.util.Double2D(50, 50));
        schedule.scheduleRepeating(bead);
        objPE.register(bead);
                
        schedule.scheduleRepeating(objPE);
        }
                        
    public static void main(String[] args)
        {
        doLoop(BeadWire.class, args);
        System.exit(0);
        }    
    }
