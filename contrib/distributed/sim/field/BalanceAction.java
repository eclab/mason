package sim.field;

import java.util.Arrays;

import sim.util.IntHyperRect;

public class BalanceAction implements java.io.Serializable {

	// Source and destination partition group, their outer boundary on the given dimension must align
	int[] src, dst;

	// Dimension, direction, and the offset of the adjustment
	int dim, dir, offset;

	public BalanceAction(int src, int dst, int dim, int dir, int offset) {
		this.src = new int[] {src};
		this.dst = new int[] {dst};
		this.dim = dim;
		this.dir = dir;
		this.offset = offset;
	}

	public BalanceAction(int src, int[] dst, int dim, int dir, int offset) {
		this.src = new int[] {src};
		this.dst = dst;
		this.dim = dim;
		this.dir = dir;
		this.offset = offset;
	}

	public BalanceAction(int[] src, int[] dst, int dim, int dir, int offset) {
		this.src = src;
		this.dst = dst;
		this.dim = dim;
		this.dir = dir;
		this.offset = offset;
	}

	public static BalanceAction idle() {
		return new BalanceAction(null, null, 0, 0, 0);
	}

	public void applyToPartition(DNonUniformPartition p) {
		if (offset == 0)
			return;

		IntHyperRect n;

		for (int pid : src) {
			n = p.getPartition(pid).resize(dim, dir, offset);
			p.updatePartition(n);
		}
		for (int pid : dst) {
			n = p.getPartition(pid).resize(dim, -dir, -offset);
			p.updatePartition(n);
		}
	}

	public String toString() {
		if (dir > 0 && offset > 0)
			return String.format("%s =(%d,%d)=> %s", Arrays.toString(src), dim, offset, Arrays.toString(dst));
		else if (dir > 0 && offset < 0)
			return String.format("%s <=(%d,%d)= %s", Arrays.toString(src), dim, offset, Arrays.toString(dst));
		else if (dir < 0 && offset > 0)
			return String.format("%s <=(%d,%d)= %s", Arrays.toString(dst), dim, offset, Arrays.toString(src));
		else if (dir < 0 && offset < 0)
			return String.format("%s =(%d,%d)=> %s", Arrays.toString(dst), dim, offset, Arrays.toString(src));

		return String.format("%s =(%d,%d)= %s ", Arrays.toString(src), dim, offset, Arrays.toString(dst));
	}
}