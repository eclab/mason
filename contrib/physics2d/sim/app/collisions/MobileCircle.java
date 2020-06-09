package sim.app.collisions;

import java.awt.*;
import sim.engine.*;
import sim.physics2D.physicalObject.*;
import sim.physics2D.forceGenerator.ForceGenerator;
import sim.physics2D.util.*;
import sim.util.Double2D;

public class MobileCircle extends MobileObject2D implements Steppable, ForceGenerator
    {
    public double radius;
    public int count;
    public MobileCircle(Double2D pos, Double2D vel, double radius)
        {
        // vary the mass with the size
        this.setPose(pos, new Angle(0));
        this.setVelocity(vel);

        if (this.index > 0)
            this.setShape(new sim.physics2D.shape.Circle(radius, Color.red), Math.PI * radius * radius);
        else
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
        checkWallCollisions(simCollisions, position);
        this.addTorque(.1);
        simCollisions.fieldEnvironment.setObjectLocation(this, new sim.util.Double2D(position.x, position.y));
        }
        
    public void addForce()
        {
        }
        
    public boolean checkWallCollisions(Collisions simCollisions, Double2D newPos)
        {
        double dist = 0;
        Double2D velocity = this.getVelocity();
        Double2D position = this.getPosition();
                
        // check walls
        if(newPos.x > simCollisions.xMax)
            {
            position = new Double2D(simCollisions.xMax, position.y);
            velocity = new Double2D(-velocity.x, velocity.y);
            }
        if(newPos.y > simCollisions.yMax)
            {
            position = new Double2D(position.x, simCollisions.yMax);
            velocity = new Double2D(velocity.x, -velocity.y);
            }
        if(newPos.x < simCollisions.xMin)
            {
            position = new Double2D(simCollisions.xMin, position.y);
            velocity = new Double2D(-velocity.x, velocity.y);
            }
        if(newPos.y < simCollisions.yMin)
            {
            position = new Double2D(position.x, simCollisions.yMin);
            velocity = new Double2D(velocity.x, -velocity.y);
            }
                        
                        
        this.setVelocity(velocity);
        this.setPose(position, this.getOrientation());

        return true;
        }
    }
