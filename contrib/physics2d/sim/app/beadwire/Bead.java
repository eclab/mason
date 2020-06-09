package sim.app.beadwire;

import java.awt.*;
import sim.engine.*;
import sim.physics2D.physicalObject.*;
import sim.physics2D.forceGenerator.*;
import sim.physics2D.util.*;
import sim.util.Double2D;

public class Bead extends MobileObject2D implements Steppable, ForceGenerator
    {
    public double radius;
    public boolean applyConstraints;
        
    public Bead(Double2D pos, Double2D vel, double radius) 
        {
        // vary the mass with the size
        this.setShape(new sim.physics2D.shape.Circle(radius, Color.red), Math.PI * radius * radius);
        this.setPose(pos, new Angle(0));
        this.radius = radius;
                
        this.setCoefficientOfFriction(0);
        this.setCoefficientOfRestitution(1);
        this.applyConstraints = true;
        }
 
    public void step(SimState state)
        {
        BeadWire simBeadWire = (BeadWire)state;
        Double2D position = this.getPosition();
        simBeadWire.fieldEnvironment.setObjectLocation(this, new sim.util.Double2D(position.x, position.y));
        }
        
    public void addForce()
        {
        // Add gravity force
        this.addForce(new Double2D(0, 1));
                
        // Add constraint forces
        if (this.getVelocity().length() > 0 && this.applyConstraints)
            {
            double lambda = (this.getForceAccumulator().x - this.getForceAccumulator().y)/(this.getVelocity().y + this.getVelocity().x);
            this.addForce(new Double2D(-lambda * this.getVelocity().y, lambda * this.getVelocity().x));
            }
        }
    }
