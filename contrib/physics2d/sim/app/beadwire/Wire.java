
package sim.app.beadwire;

import sim.engine.SimState;
import sim.portrayal.*;
import sim.physics2D.*;
import sim.engine.*;
import sim.physics2D.physicalObject.*;
import sim.util.Double2D;

import java.awt.*;

public class Wire extends SimplePortrayal2D implements Steppable 
    {
    private PhysicalObject2D po1;
    private PhysicalObject2D po2;
    private PhysicsState physicsState;
    private double springConstant;
        
    public Wire()
        {
        }

    public void draw(Object object, Graphics2D graphics, DrawInfo2D info)
        {
        graphics.setColor(Color.black);
        graphics.drawLine(0, 0, 800, 800);              
        }
        
    public void step(SimState state)
        {
        BeadWire simBeadWire = (BeadWire)state;
        simBeadWire.fieldEnvironment.setObjectLocation(this, new sim.util.Double2D(0,0));
        }
    }
