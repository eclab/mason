/*
 * Created on Nov 18, 2006
 *
 * To change the template for this generated file go to
 * Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */
package sim.portrayal3d.network;

import java.awt.Color;
import javax.media.j3d.*;
import com.sun.j3d.utils.geometry.*;

/**
 * @author Gabriel Balan
 *
 */
public class CylinderEdgePortrayal3D extends GenericEdgePortrayal3D
    {
    static class PickableCylinder extends Cylinder
        {
        public PickableCylinder()
            {
            super(); 
            setAllPickableFlags();
            }
                
        public PickableCylinder(float radius)
            {
            super(radius, 2);
            setAllPickableFlags();
            }
                
        public PickableCylinder(float radius, Appearance ap)
            {
            super(radius, 2, ap);
            setAllPickableFlags();
            }
        
        public PickableCylinder(Appearance ap)
            {
            super(1, 2, ap);
            setAllPickableFlags();
            }

        public void setUserData(java.lang.Object userData)
            {
            getShape(Cylinder.BODY).setUserData(userData);
            getShape(Cylinder.TOP).setUserData(userData);
            getShape(Cylinder.BOTTOM).setUserData(userData);
            }
        public void setAllPickableFlags()
            {
            setPickableFlags(getShape(Cylinder.BODY));
            setPickableFlags(getShape(Cylinder.TOP));
            setPickableFlags(getShape(Cylinder.BOTTOM));
            }
               
        }
    public CylinderEdgePortrayal3D()
        {
        super(new PickableCylinder());
        }
        
    public CylinderEdgePortrayal3D(float cylinderRadius)
        {
        super(new PickableCylinder(cylinderRadius));
        }
        
    public CylinderEdgePortrayal3D(float cylinderRadius, Appearance ap)
        {
        super(new PickableCylinder(cylinderRadius, ap));
        }       


    public CylinderEdgePortrayal3D(Color labelColor)
        {
        super(new PickableCylinder(), labelColor);
        }

    public CylinderEdgePortrayal3D(Appearance edgeAppearance, Color labelColor)
        {
        super(new PickableCylinder(edgeAppearance), labelColor);
        }
    }
