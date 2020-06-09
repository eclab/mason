package sim.app.collisionsPJ;

import java.awt.*;
import sim.engine.*;
import sim.physics2D.physicalObject.*;
import sim.physics2D.util.*;
import sim.util.Double2D;

public class MobilePoly extends MobileObject2D implements Steppable
    {
    public JointDisplay jd;
    public Double2D jointPos;
        
    // public double radius;
    public MobilePoly(Double2D pos, Double2D vel, double width, double height, Paint paint)
        {
        this.setVelocity(vel);
        this.setPose(pos, new Angle(0));

        this.setShape(new sim.physics2D.shape.Rectangle(width, height, paint), width * height);

        this.setCoefficientOfFriction(0);
        this.setCoefficientOfRestitution(1);
        }
 
    public void step(SimState state)
        {
        Double2D position = this.getPosition();
        Collisions simCollisions = (Collisions)state;
        checkWallCollisions(simCollisions, position);
        simCollisions.fieldEnvironment.setObjectLocation(this, new sim.util.Double2D(position.x, position.y));
                
        if (jd != null && jointPos != null)
            {
            jd.setPose(jointPos.rotate(this.getOrientation().radians).add(this.getPosition()), new Angle(0));
            }
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
