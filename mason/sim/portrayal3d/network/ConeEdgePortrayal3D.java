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
public class ConeEdgePortrayal3D extends PrimitiveEdgePortrayal3D
    {
    public ConeEdgePortrayal3D()
        {
        super(new Cone(0.5f, 2f));
        }
        
    public ConeEdgePortrayal3D(float coneBaseRadius)
        {
        super(new Cone(coneBaseRadius, 2));
        }
        
    public ConeEdgePortrayal3D(Color labelColor)
        {
        super(new Cone(), labelColor);
        }
    protected void init(Node edgeModel)
        {
        super.init(edgeModel);
        Cone c = (Cone)edgeModel;       
        PrimitivePortrayal3D.setShape3DFlags(c.getShape(Cone.BODY));
        PrimitivePortrayal3D.setShape3DFlags(c.getShape(Cone.CAP));
        }

    //cap, body.
    protected int numShapes(){return 2;}
    }
