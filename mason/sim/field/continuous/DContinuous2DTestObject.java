package sim.field.continuous;

import java.io.*;

import sim.util.*;
import sim.engine.*;

public class DContinuous2DTestObject implements Steppable, Serializable {
	public int id;
	public Double2D loc;


	public DContinuous2DTestObject(int id, Double2D loc) {
		this.id = id;
		this.loc = loc;
	}

	public String toString() {
		return String.format("Object %d at location [%g, %g]", id, loc.x, loc.y);
	}

	 public void step(SimState state) {
	 	return;
	 }
}