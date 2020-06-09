package sim.app.pendulum;

import java.awt.*;
import sim.engine.*;
import sim.physics2D.physicalObject.*;
import sim.physics2D.util.*;
import sim.physics2D.forceGenerator.*;
import sim.util.Double2D;

public class Pendulum extends MobileObject2D implements Steppable, ForceGenerator
    {
    // public double radius;
    public Pendulum(Double2D pos, Double2D vel, double width, double height, Paint paint)
        {
        this.setVelocity(vel);
        this.setPose(pos, new Angle(0));

        this.setShape(new sim.physics2D.shape.Rectangle(width, height, paint), width * height);

        this.setCoefficientOfFriction(0);
        this.setCoefficientOfStaticFriction(0);
        this.setCoefficientOfRestitution(1);
        }
 
    public void step(SimState state)
        {
        Double2D position = this.getPosition();
        PendulumSim simPendulumSim = (PendulumSim)state;
        simPendulumSim.fieldEnvironment.setObjectLocation(this, new sim.util.Double2D(position.x, position.y));
        }
        
    public void addForce()
        {
        this.addForce(new Double2D(0, 5));
        }
    }
