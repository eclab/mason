package sim.app.physicsTutorial3;

import java.awt.*;
import sim.engine.*;
import sim.physics2D.physicalObject.*;
import sim.physics2D.util.*;
import sim.util.Double2D;

public class Effector extends MobileObject2D implements Steppable
    {
    // public double radius;
    public Effector(Double2D pos, Double2D vel, double radius, Paint paint)
        {
        this.setVelocity(vel);
        this.setPose(pos, new Angle(0));
        this.setShape(new sim.physics2D.shape.Circle(radius, paint), radius * radius * Math.PI);
        this.setCoefficientOfFriction(0);
        this.setCoefficientOfRestitution(1);
        }
 
    public void step(SimState state)
        {
        Double2D position = this.getPosition();
        PhysicsTutorial3 simPhysicsTutorial3 = (PhysicsTutorial3)state;
        simPhysicsTutorial3.fieldEnvironment.setObjectLocation(this, new sim.util.Double2D(position.x, position.y));
        }
    }
