package CDI.src.environment;

import java.awt.Graphics2D;
import java.awt.Paint;

import sim.portrayal.DrawInfo2D;
import sim.portrayal.SimplePortrayal2D;
import sim.portrayal.simple.OrientedPortrayal2D;
import sim.util.gui.ColorMap;

public class VectorSignPortrayal2D extends WindSignPortrayal2D{

	private static final long serialVersionUID = 1L;
	
	public LevelMap map;
	public DoubleColorMap colorMap;
	
	public VectorSignPortrayal2D(SimplePortrayal2D child, int offset, double scale, LevelMap map, DoubleColorMap colorMap) {
		
		super(child);
		this.offset = offset;
		this.map = map;
		this.colorMap = colorMap;
		this.scale = scale;
	}
	
	
	public double getMagnitude(Object object, DrawInfo2D info)
    {
		if (object != null && object instanceof MegaCellSign)
			return ((MegaCellSign)object).getMagnitude();
		else
			return Double.NaN;
    }
	
	public double getNetMovement(Object object, DrawInfo2D info) {
		if (object != null && object instanceof MegaCellSign)
			return ((MegaCellSign)object).getNetMovement();
		else
			return Double.NaN;
	}
	
	
	
	@Override
	public void draw(Object object, Graphics2D graphics, DrawInfo2D info) {
		double magnitude = getMagnitude(object, info);
		this.level = this.map.getLevel(magnitude);
		this.paint = colorMap.getColor(getNetMovement(object, info));
				
		if(magnitude==-1) // no body move
			return;
		
		super.draw(object, graphics, info);
	}

}
