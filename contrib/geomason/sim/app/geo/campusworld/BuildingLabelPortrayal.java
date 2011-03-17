package sim.app.geo.campusworld;

import java.awt.Paint;
import java.util.ArrayList;
import java.util.Collections;

import com.vividsolutions.jts.geom.Geometry;

import sim.portrayal.DrawInfo2D;
import sim.portrayal.SimplePortrayal2D;
import sim.portrayal.simple.LabelledPortrayal2D;
import sim.util.geo.AttributeField;
import sim.util.geo.GeometryUtilities;
import sim.util.geo.MasonGeometry;

public class BuildingLabelPortrayal extends LabelledPortrayal2D
{
	private static final long serialVersionUID = 1L;
	
	public BuildingLabelPortrayal(SimplePortrayal2D child, Paint paint)
	{
		super(child, null, paint, true);
	}

	
	public String getLabel(Object object, DrawInfo2D info)
	{
		String label = "";
		if (object instanceof MasonGeometry)
		{
			MasonGeometry mg = (MasonGeometry) object;
			AttributeField key = new AttributeField("NAME");
			Geometry g = mg.getGeometry();
			ArrayList attrs = (ArrayList) g.getUserData();
			int index = Collections.binarySearch(attrs, key, GeometryUtilities.attrFieldCompartor);

			if (index >= 0)
				label = (String)((AttributeField)(attrs.get(index))).value;
		}
		return label;

	}
}
