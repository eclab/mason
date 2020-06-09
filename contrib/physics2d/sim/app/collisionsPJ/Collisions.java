package sim.app.collisionsPJ;

import sim.engine.*;
import sim.field.continuous.*;
import ec.util.*;
import sim.physics2D.util.*;
import java.awt.*;
import sim.physics2D.constraint.*;
import sim.physics2D.*;
import sim.util.Double2D;

public class Collisions extends SimState
    {
    public double xMin = 0;
    public double xMax = 200;
    public double yMin = 0;
    public double yMax = 200;
    int wallPos = 10;
        
    public Continuous2D fieldEnvironment;
                        
    public Collisions(long seed)
        {
        this(seed, 200, 200);
        }
        
    public Collisions(long seed, int width, int height)
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
        
    // Resets and starts a simulation
    public void start()
        {
        super.start();  // clear out the schedule
        createGrids();

        PhysicsEngine2D objPE = new PhysicsEngine2D();
                
        Double2D pos;
        Double2D vel;
                
        Wall wall;
        // WALLS
        // HORIZ
        pos = new Double2D(100,wallPos);
        wall = new Wall(pos, 200 - wallPos * 2, 6);
        fieldEnvironment.setObjectLocation(wall, new sim.util.Double2D(pos.x, pos.y));
        objPE.register(wall);
 
        pos = new Double2D(100,200 - wallPos);
        wall = new Wall(pos, 200 - wallPos * 2, 6);
        fieldEnvironment.setObjectLocation(wall, new sim.util.Double2D(pos.x, pos.y));
        objPE.register(wall);
                
        // VERT
        pos = new Double2D(wallPos,100);
        wall = new Wall(pos, 6, 200 - wallPos * 2);
        fieldEnvironment.setObjectLocation(wall, new sim.util.Double2D(pos.x, pos.y));
        objPE.register(wall);
        
        pos = new Double2D(200 - wallPos,100);
        wall = new Wall(pos, 6, 200 - wallPos * 2);
        fieldEnvironment.setObjectLocation(wall, new sim.util.Double2D(pos.x, pos.y));
        objPE.register(wall);
                
        pos = new Double2D(100, 100);
        vel = new Double2D(1, 1);
        MobilePoly rec = new MobilePoly(pos, vel, 10, 20, Color.red);
        fieldEnvironment.setObjectLocation(rec, new sim.util.Double2D(pos.x, pos.y));
        objPE.register(rec);
        schedule.scheduleRepeating(rec);
                
        pos = new Double2D(50, 50);
        vel = new Double2D(.5, .5);
        MobilePoly rec2 = new MobilePoly(pos, vel, 20, 10, Color.red);
        fieldEnvironment.setObjectLocation(rec2, new sim.util.Double2D(pos.x, pos.y));
        objPE.register(rec2);
        schedule.scheduleRepeating(rec2);
                
        pos = new Double2D(90, 50);
        vel = new Double2D(.5, .5);
        MobilePoly rec3 = new MobilePoly(pos, vel, 20, 10, Color.red);
        fieldEnvironment.setObjectLocation(rec3, new sim.util.Double2D(pos.x, pos.y));
        objPE.register(rec3);
        schedule.scheduleRepeating(rec3);
                
        PinJoint pj = new PinJoint(new Double2D(70, 50), rec2, rec3);
        objPE.register(pj);
                
        pos = new Double2D(70, 50);
        vel = new Double2D(.5, .5);
        JointDisplay jd = new JointDisplay(pos, vel, 3);
        fieldEnvironment.setObjectLocation(jd, new sim.util.Double2D(pos.x, pos.y));
        schedule.scheduleRepeating(jd);
        objPE.register(jd);
                
        PinJoint pj2 = new PinJoint(new Double2D(70, 50), rec2, jd);
        objPE.register(pj2);
                
        // schedule the physics engine
        schedule.scheduleRepeating(objPE);
        }
                        
    public static void main(String[] args)
        {
        doLoop(Collisions.class, args);
        System.exit(0);
        }    
    }
