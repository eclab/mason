package sim.app.serengeti;
import sim.engine.*;
import sim.field.continuous.*;
import sim.util.*;
import ec.util.*;
import ec.gp.*;
import ec.gp.koza.*;

public class Lion implements Steppable
    {
    private static final long serialVersionUID = 1;

    public Double2D last = new Double2D(0,0);
    public int number;

/*
    public Double2D dir()
    	{
    	return serengeti.simstate.field.getObjectLocation(this).subtract(last);
    	}
*/

    public void step(SimState simstate)
        {        
       	Serengeti s = (Serengeti)simstate;
       	s.currentLion = this;
       	SerengetiData data = (SerengetiData)(s.problem.input);
        GPIndividual ind = ((GPIndividual)s.problem._ind);
		
		if (ind == null) ind = SerengetiWithUI.ind;
		
		ind.trees[number].child.eval(
			s.problem.state,
			s.problem.tnum,
			s.problem.input,
			s.problem.stack,
			ind,
			s.problem);
			
		double len = Math.sqrt(data.x * data.x + data.y * data.y);
		if (len != 0)
			{
			data.x *= (s.lionJump / len);
			data.y *= (s.lionJump / len);
			}

        Double2D loc = s.field.getObjectLocation(this);
        double dx = s.field.stx(loc.x + data.x);
        double dy = s.field.sty(loc.y + data.y);
        
		s.field.setObjectLocation(this, new Double2D(dx, dy));
        }
 
    }
