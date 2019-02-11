package sim.util;

import java.util.*;
import java.util.stream.IntStream;
import java.util.stream.Collectors;

import mpi.*;

import sim.field.DNonUniformPartition;

public class GraphColoring {
	DNonUniformPartition p;
	public int myColor, numColors;

	public GraphColoring(DNonUniformPartition p) {
		this.p = p;
	}

	public void color() {
		int[] c = greedyColor();
		this.myColor = c[p.getPid()];
		this.numColors = Arrays.stream(c).boxed().collect(Collectors.toSet()).size();
	}

	public int[] greedyColor() {
		int[] color = IntStream.range(0, p.np).toArray();

		int[][] neighbors = IntStream.range(0, p.np)
		                    .mapToObj(i -> p.getNeighborIds(i))
		                    .toArray(size -> new int[size][]);
		int maxDegree = Arrays.stream(neighbors)
		                .mapToInt(x -> x.length)
		                .max().getAsInt();

		for (int i = maxDegree + 1; i < p.np; i++) {
			Set<Integer> s = IntStream.range(0, maxDegree + 1)
			                 .boxed()
			                 .collect(Collectors.toSet());
			Set<Integer> es = Arrays.stream(neighbors[i])
			                  .map(x -> color[x])
			                  .boxed()
			                  .collect(Collectors.toSet());
			s.removeAll(es);
			if (!s.isEmpty())
				color[i] = s.stream().mapToInt(x -> x)
				           .min().getAsInt();
		}

		return color;
	}

	public static void main(String[] args) throws MPIException {
		MPI.Init(args);

		DNonUniformPartition p = DNonUniformPartition.getPartitionScheme(new int[] {72, 72}, false, new int[] {1, 1});

		p.initUniformly(null);
		GraphColoring gc = new GraphColoring(p);

		gc.color();

		System.out.println(String.format("[%d][%d] %d", p.pid, gc.numColors, gc.myColor));

		MPI.Finalize();
	}
}