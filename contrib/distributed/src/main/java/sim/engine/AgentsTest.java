package sim.engine;

public class AgentsTest
{
	private static final long serialVersionUID = 1L;

	public static void main(String[] args)
	{
		
		String str = "10 11 12 41 21 2";
		String[] strs = str.split(" ");
		System.out.println(strs[0]);
		System.out.println(strs[1]);
		System.out.println(strs[2]);
		
		
		
		
//		
//
//		final Int2D[] splitPoints = new Int2D[] {
//				new Int2D(50, 50),
//				new Int2D(25, 25),
//				new Int2D(75, 75),
//				new Int2D(60, 90),
//				new Int2D(10, 10)
//		};
//
//		System.out.println(new ArrayList<>(Arrays.asList(splitPoints)));

//		int[] size = { 100, 100 };
//		int[] aoi = { -1, -0 };
//		
//		
//		
//		ArrayList<IntRect2D> allBounds = new ArrayList<>();
//
//		// init with nulls
//		for (int i = 0; i < 4; i++)
//			allBounds.add(null);
//		System.out.println(allBounds);
//		for (int i = 0; i < 4; i++)
//			allBounds.set(i, new IntRect2D(1, 2));
//		System.out.println(allBounds);

//		IntHyperRect rect = new IntHyperRect(size);
//		System.out.println(rect);
//		System.out.println(rect.resize(aoi));

//		System.out.println(Long.MAX_VALUE);
//
//		System.out.println((int) (Math.floor(6.69)));
//
//		int[] dsize = { 2, 3 };
//		Arrays.stream(dsize).reduce(1, (x, y) -> x * y);
//
//		System.out.println(Arrays.stream(dsize).reduce(1, (x, y) -> x * y));

//		int numDelims = 10, k = 10;
//
//		for (int i = 0; i < 2; i++) {
//			int stride = (int) Math.pow(numDelims - 1, 1 - i);
//			int idx = k / stride % (numDelims - 1);
//			System.out.println(stride + ", " + idx);
//
//			stride = (i == 0) ? numDelims - 1 : 1;
//			idx = k / stride % (numDelims - 1);
//			System.out.println(stride + ", " + idx);
//		}

//		for (final Int2D p : IntPointGenerator.getLayer(2, 1)) // 2 == number of dimensions (width, height)
//		{
//			System.out.println(p);
//		}

//		Int2D ul = new Int2D(0, 0);
//		Int2D br = new Int2D(2, 3);
//
//		for (final Int2D p : IntPointGenerator.getBlock(ul, br))
//			System.out.println(p);
//
//		System.out.println("----------");
//		for (int x = ul.x; x < br.x; x++)
//			for (int y = ul.y; y < br.y; y++)
//				System.out.println(new Int2D(x, y));

//		DSimState.setMultiThreaded(true);
//		DSimState.setMultiThreaded(true);

//		DHeatBug heatBug = new DHeatBug(0, 0, 0, 0, 0);
//		System.out.println(heatBug);
//		heatBug = new DHeatBug(0, 0, 0, 0, 0);
//		System.out.println(heatBug);
//		heatBug = new DHeatBug(0, 0, 0, 0, 0);
//		System.out.println(heatBug);
//		heatBug = new DHeatBug(0, 0, 0, 0, 0);
//		System.out.println(heatBug);
//		heatBug = new DHeatBug(0, 0, 0, 0, 0);
//		System.out.println(heatBug);
//		heatBug = new DHeatBug(0, 0, 0, 0, 0);
//		System.out.println(heatBug);
//		heatBug = new DHeatBug(0, 0, 0, 0, 0);
//		System.out.println(heatBug);
	}

}
