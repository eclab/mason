package sim.app.serengeti;
import sim.engine.*;
import sim.field.continuous.*;
import sim.util.*;
import ec.util.*;

public class Gazelle implements Steppable, sim.portrayal.Oriented2D
    {
    private static final long serialVersionUID = 1;

    MutableDouble2D dir = new MutableDouble2D(0,0);
    public Continuous2D field;
    
    void updateDead(SimState state)
        {
        if (dead) return;
        
        final Serengeti s = (Serengeti)state;
        Double2D loc = s.field.getObjectLocation(this);

        for(int i = 0; i < s.lions.size(); i++)
            {
            Double2D pos = s.field.getObjectLocation(s.lions.get(i));
            Double2D v = s.field.tv(pos, loc);
            double len = v.lengthSq();
            if (len <= 1.0) { dead = true; return; }
            }
        }
    
    public double orientation2D()
        {
        if (dir.x == 0 && dir.y == 0) return 0;
        return Math.atan2(dir.y, dir.x);
        }
    
    boolean dead = false;
    public boolean isDead()
        {
        return dead;
        }

	public void setDead(boolean val)
		{
		dead = val;
		}

    public void step(SimState state)
        {        
        updateDead(state);

        final Serengeti s = (Serengeti)state;
        Double2D loc = s.field.getObjectLocation(this);
        double jump = s.gazelleJump;
        
        MutableDouble2D dir = new MutableDouble2D(0,0);
        double max = Math.sqrt((s.field.width / 2) * (s.field.width / 2) + (s.field.height / 2) * (s.field.height / 2));
        for(int i = 0; i < s.lions.size(); i++)
            {
            Double2D pos = s.field.getObjectLocation(s.lions.get(i));
            Double2D v = s.field.tv(pos, loc);
            double len = v.length();
            double scale = (max - len) / len;
            dir.addIn(v.x * scale, v.y * scale);
            }
        dir.negate();
        if (dir.length() > 0)
            dir.resize(s.gazelleJump);
        else 
        	{
        	//System.err.println("Gazelle Not Moving");
        	}
           
        s.field.setObjectLocation(this, new Double2D(s.field.stx(loc.x + dir.x), 
                s.field.sty(loc.y + dir.y)));
        }
 
    }
