package sim.app.geo.haiti;



import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.geom.Rectangle2D;
import sim.portrayal.DrawInfo2D;
import sim.portrayal.simple.RectanglePortrayal2D;


class KnowledgePortrayal extends RectanglePortrayal2D
{
    private static final long serialVersionUID = 1L;

    // colormap for people in tile levels, which varies between 0 and 30
//	public static ColorMap PeopleColor = new sim.util.gui.SimpleColorMap(
//			0, 30, new Color(0, 0, 0, 0), new Color(255, 0, 0, 255));
    @Override
    public void draw(Object object, Graphics2D graphics, DrawInfo2D info)
    {
        if (object == null)
        {
            return;
        }

        Rectangle2D.Double draw = info.draw;
        final double width = draw.width * scale;
        final double height = draw.height * scale;

        final int x = (int) (draw.x - width / 2.0);
        final int y = (int) (draw.y - height / 2.0);
        final int w = (int) (width);
        final int h = (int) (height);

        if (((Agent) object).centerInfo > 0)
        {
            //	if( ((Agent)object).centerInfo.size() > 0){
            graphics.setColor(Color.red);
            graphics.fillRect(x, y, w, h);
        }
    }

}
