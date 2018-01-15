package sim.engine;

import java.io.*;
import java.util.*;

import sim.util.*;
import ec.util.*;

public class DistributedAgentQueueTestAgent implements Steppable, Serializable {
	public int loc_x, loc_y, id;
	public final int step_x, step_y;
	public final int w, h;

	public void step( final SimState state ) {
		DistributedAgentQueueTest sim = (DistributedAgentQueueTest)state;

		loc_x = (loc_x + step_x) % w;
		loc_y = (loc_y + step_y) % h;

		try {
			sim.queue.setPos(this, loc_x, loc_y);
		} catch (Exception e) {
			e.printStackTrace();
			System.exit(-1);
		}
	}

	public DistributedAgentQueueTestAgent(int id, int x, int y, int sx, int sy, int w, int h) {
		this.id = id;
		loc_x = x;
		loc_y = y;
		step_x = sx;
		step_y = sy;
		this.w = w;
		this.h = h;
	}

	public String toString() {
		return String.format("Agent %d [%d %d]", id, loc_x, loc_y);
	}
}