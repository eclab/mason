package sim.field;

import java.io.*;
import java.util.*;

public class MigratedObject implements Serializable {
	Object obj;
	int dst;

	public MigratedObject(final Object obj, final int dst) {
		this.obj = obj;
		this.dst = dst;
	}
}