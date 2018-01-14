package sim.engine;

import java.io.*;
import java.util.*;

public class AgentWithPosition implements Serializable {
	Steppable obj;
	int x;
	int y;

	public AgentWithPosition(Steppable a, int x, int y) {
		obj = a;
		this.x = x;
		this.y = y;
	}
}