package sim.app.pendulum;

import sim.engine.*;
import sim.field.continuous.*;
import ec.util.*;
import sim.physics2D.util.*;
import sim.physics2D.constraint.*;
import sim.physics2D.integrator.ODEEulerSolver;
import sim.util.Double2D;

import java.awt.*;

import sim.physics2D.*;

public class PendulumSim extends SimState
    {
    public double xMin = 0;
    public double xMax = 100;
    public double yMin = 0;
    public double yMax = 100;
        
    public Continuous2D fieldEnvironment;
                        
    public PendulumSim(long seed)
        {
        this(seed, 200, 200);
        }
        
    public PendulumSim(long seed, int width, int height)
        {
        super(seed);
        xMax = width; 
        yMax = height;
        createGrids();
        }
        
    boolean useEuler = false;
    public boolean getUseEuler() { return useEuler; }
    public void setUseEuler(boolean val) { useEuler = val; }
                
    void createGrids()
        {       
        fieldEnvironment = new Continuous2D(25, (xMax - xMin), (yMax - yMin));
        }
                
    // Resets and starts a simulation
    public void start()
        {
        super.start();  // clear out the schedule
        createGrids();

        PhysicsEngine2D objPE = new PhysicsEngine2D();
        if (useEuler)
            objPE.setODESolver(new ODEEulerSolver());
                
        Anchor anchor = new Anchor(new Double2D(100, 50), 5);
        fieldEnvironment.setObjectLocation(anchor, new sim.util.Double2D(100, 50));
        objPE.register(anchor);
                
        Pendulum pend = new Pendulum(new Double2D(80, 50), new Double2D(0, 0), 20, 5, Color.red);
        fieldEnvironment.setObjectLocation(pend, new sim.util.Double2D(80, 50));
        schedule.scheduleRepeating(pend);
        objPE.register(pend);
                
        PinJoint pj = new PinJoint(new Double2D(100, 50), anchor, pend);
        objPE.register(pj);
                
        // schedule the physics engine
        schedule.scheduleRepeating(objPE);
        }
                        
    public static void main(String[] args)
        {
        doLoop(PendulumSim.class, args);
        System.exit(0);
        }    
    }
