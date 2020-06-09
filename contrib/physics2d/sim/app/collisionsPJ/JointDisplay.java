package sim.app.collisionsPJ;

import java.awt.*;
import sim.engine.*;
import sim.physics2D.physicalObject.*;
import sim.physics2D.util.*;
import sim.util.Double2D;

public class JointDisplay extends MobileObject2D implements Steppable
    {
    public double radius;
    public int count;
    public JointDisplay(Double2D pos, Double2D vel, double radius)
        {
        // vary the mass with the size
        this.setPose(pos, new Angle(0));
        this.setVelocity(vel);

        this.setShape(new sim.physics2D.shape.Circle(radius, Color.blue), Math.PI * radius * radius);
        this.radius = radius;
                
        this.setCoefficientOfFriction(0);
        this.setCoefficientOfRestitution(1);
                
        count = 0;
        }
 
    public void step(SimState state)
        {
        Double2D position = this.getPosition();
        Collisions simCollisions = (Collisions)state;
        this.addTorque(.1);
        simCollisions.fieldEnvironment.setObjectLocation(this, new sim.util.Double2D(position.x, position.y));
        }
    }
