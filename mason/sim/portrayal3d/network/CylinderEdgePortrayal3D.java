/*
 * Created on Nov 18, 2006
 *
 * To change the template for this generated file go to
 * Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */
package sim.portrayal3d.network;

import java.awt.Color;
import javax.media.j3d.*;

import sim.portrayal3d.simple.PrimitivePortrayal3D;

import com.sun.j3d.utils.geometry.*;

/**
 * @author Gabriel Balan
 *
 */
public class CylinderEdgePortrayal3D extends PrimitiveEdgePortrayal3D
    {
    public CylinderEdgePortrayal3D()
        {
        super(new Cylinder(0.5f, 2f));
        }
        
    public CylinderEdgePortrayal3D(float cylinderRadius)
        {
        super(new Cylinder(cylinderRadius, 2));
        }

    public CylinderEdgePortrayal3D(Color labelColor)
        {
        super(new Cylinder(), labelColor);
        }

    protected void init(Node edgeModel)
        {
        super.init(edgeModel);
        Cylinder c = (Cylinder)edgeModel;       
        PrimitivePortrayal3D.setShape3DFlags(c.getShape(Cylinder.BODY));
        PrimitivePortrayal3D.setShape3DFlags(c.getShape(Cylinder.TOP));
        PrimitivePortrayal3D.setShape3DFlags(c.getShape(Cylinder.BOTTOM));
        }
    //top, botton, body.
    protected int numShapes(){return 3;}
    }
