
package sim.app.pendulum;

import java.awt.*;
import sim.physics2D.physicalObject.*;
import sim.physics2D.util.*;
import sim.util.Double2D;

public class Anchor extends StationaryObject2D 
    {
    public double radius;
    public Anchor(Double2D pos, double radius)
        {
        this.setPose(pos, new Angle(0));
        this.setShape(new sim.physics2D.shape.Circle(radius, Color.gray));
        this.setCoefficientOfRestitution(1);
        } 
    }
