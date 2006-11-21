/*
 * Created on Nov 18, 2006
 *
 * To change the template for this generated file go to
 * Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */
package sim.portrayal3d.network;

import java.awt.Color;
import javax.media.j3d.Appearance;
import javax.media.j3d.Shape3D;
import com.sun.j3d.utils.geometry.Cone;

/**
 * @author Gabriel Balan
 *
 */
public class ConeEdgePortrayal3D extends GenericEdgePortrayal3D
{
	static class MyPickableCone extends Cone
	{
		public MyPickableCone(){super();}
		
		public MyPickableCone(float radius)
		{
			super(radius, 2);
		}
		
		public MyPickableCone(float radius, Appearance ap)
		{
			super(radius, 2, ap);
		}
		public MyPickableCone(Appearance ap)
		{
			super(1, 2, ap);
		}

		public void setUserData(java.lang.Object userData)
		{
			super.setUserData(userData);
			setup(getShape(Cone.CAP), userData);
			setup(getShape(Cone.BODY), userData);

		}
		private void setup(Shape3D shape,java.lang.Object userData)
		{
			shape.setUserData(userData);
			setPickableFlags(shape);
		}
		
	}

	public ConeEdgePortrayal3D()
	{
		super(new MyPickableCone());
	}
	
	public ConeEdgePortrayal3D(float coneBaseRadius)
	{
		super(new MyPickableCone(coneBaseRadius));
	}
	
	public ConeEdgePortrayal3D(float coneBaseRadius, Appearance ap)
	{
		super(new MyPickableCone(coneBaseRadius, ap));
	}	


	public ConeEdgePortrayal3D(Color labelColor)
	{
		super(new MyPickableCone(), labelColor);
	}

	public ConeEdgePortrayal3D(Appearance edgeAppearance, Color labelColor)
	{
		super(new MyPickableCone(edgeAppearance), labelColor);
	}
}
