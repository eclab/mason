package CDI.src.movement;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.StringTokenizer;

import sim.field.grid.IntGrid2D;
import sim.util.Double2D;
import ec.util.MersenneTwisterFast;
import CDI.src.environment.Cell;
import CDI.src.environment.Map;
import java.io.FileInputStream;

public class PeopleSprinkler {
	/**
	 * This class has the same functionality of the PeopleSprinkler in phase2
	 * But instead of sprinkle all people in the region, this sprinkle people
	 * incrementally
	 * 
	 * @author Ermo Wei
	 */


	public void initializeHouseholds(int numHouseholdsToSprinkle, Map map,
			double socialWeight, double socialWeightSpread,
			double desirabilityExp, int householdSize, MersenneTwisterFast rand) {

		// preprocessing to create initial numHouseholds

		HashMap<Integer, ArrayList<Cell>> popMap = new HashMap<Integer, ArrayList<Cell>>();
		HashMap<Integer, Double[]> scoreMap = new HashMap<Integer, Double[]>();
		HashMap<Integer, Integer> indexMap = new HashMap<Integer, Integer>();

		IntGrid2D scoreIndexGrid = new IntGrid2D(Map.GRID_WIDTH,
				Map.GRID_HEIGHT, -1);

		// divide canadaCell into different region for people sprinkle
		ArrayList<Cell> list;
		for (Cell c : map.canadaCells) {
			// group the cells of each cultural group
			c.setHouseholds(0); // clear the numHouseholds for initialization
			int code = map.initPopRegionGrid.get(c.x, c.y);
			if (code != -9999) {
				if (popMap.containsKey(code)) {
					popMap.get(code).add(c);
				} else {
					list = new ArrayList<Cell>();
					list.add(c);
					popMap.put(code, list);
				}
			}
		}

		BufferedReader reader;
		StringTokenizer tokenizer;

		ArrayList<Region> regionList = new ArrayList<Region>();
		int totalHousehold = 0;
		// populate map with initial numHouseholds
		try {
//			reader = new BufferedReader(new InputStreamReader(getClass()
//					.getClassLoader().getResourceAsStream(
//							map.parameters.initialzationPopulationFile)));
			reader = new BufferedReader(new InputStreamReader (new FileInputStream(
							map.parameters.initialzationPopulationFile)));
			reader.readLine();

			int regionCode, pop1911, regionHousehold;
			while (reader.ready()) {
				tokenizer = new StringTokenizer(reader.readLine(), ",");

				// skip some field for numHouseholds
				for (int i = 0; i < 5; ++i)
					tokenizer.nextToken();
				pop1911 = (int) Double.parseDouble(tokenizer.nextToken());

				// skip some field for region code
				String rc = null;
				while (tokenizer.hasMoreTokens()) {
					rc = tokenizer.nextToken();
				}
				regionCode = (int) Double.parseDouble(rc);

				regionHousehold = pop1911 / householdSize; // to get Households

				ArrayList<Cell> cellOfInterest = popMap.get(regionCode);
				if (cellOfInterest != null) {
					totalHousehold += regionHousehold;
					regionList.add(new Region(regionCode,
							cellOfInterest.size(), regionHousehold));
				} else {
					System.out.println(regionCode + " is null");
				}
				// System.out.println("next line");
			}
		} catch (Exception e) {
			e.printStackTrace();
		}

		for (Region r : regionList) {
			ArrayList<Cell> l = popMap.get(r.regionCode);
			if (l != null) {
				scoreMap.put(r.regionCode, new Double[l.size()]);
			} else {
				scoreMap.put(r.regionCode, new Double[0]);
			}
			indexMap.put(r.regionCode, 0);
		}

		// Sort the regionList, based on the region size, ascending order
		Collections.sort(regionList);

		Double2D minMax = calcInitDesirabilityBounds(map);
		double desMin = minMax.x;
		double desMax = minMax.y;
		double desRange = desMax - desMin;

		for (Cell c : map.canadaCells) {
			double des = map.initDesGrid.field[c.x][c.y];
			double value = Math.pow((des - desMin) / desRange, desirabilityExp);
			int regionCode = map.initPopRegionGrid.field[c.x][c.y];
			Integer i = indexMap.get(regionCode);
			if (i != null) {
				int index = i;
				scoreMap.get(regionCode)[index] = value;
				scoreIndexGrid.field[c.x][c.y] = index;
				indexMap.put(regionCode, index + 1);
			}
		}

		// schedule all the house hold until we run out of it
		while (totalHousehold > 0) {
			for (int i = 0; i < regionList.size(); ++i) {
				Region region = regionList.get(i);
				// if we still have household haven't been sprinkled, then we
				// sprinkle it
				if (region.remainingHouseholds > 0) {

					ArrayList<Cell> cellOfInterest = popMap
							.get(region.regionCode);
					if (numHouseholdsToSprinkle > region.remainingHouseholds)
						numHouseholdsToSprinkle = region.remainingHouseholds;
					if (cellOfInterest != null) {
						this.sprinklePeople(numHouseholdsToSprinkle, socialWeight,
								socialWeightSpread, cellOfInterest,
								region.regionCode, scoreIndexGrid,
								map.initPopRegionGrid, scoreMap, rand);
					} else {
						System.out.println("cell of interest is null");
					}
					// reduce the remainPopulation and totalHousehold
					totalHousehold -= numHouseholdsToSprinkle;
					region.remainingHouseholds -= numHouseholdsToSprinkle;
				}
				// test code, if the region have no people, we remove it from
				// the arraylist
				else {
					// System.out.println("region size "+ regionList.size());
					// regionList.remove(region);
				}

			}
			// System.out.println(totalHousehold);
		}

		System.out.println("end of sprinkling people");

	}

	private void sprinklePeople(int numberOfHouseholds, double socialWeight,
			double socialWeightSpread, ArrayList<Cell> cellOfInterest,
			int regionCode, IntGrid2D scoreIndexGrid,
			IntGrid2D initPopRegionGrid, HashMap<Integer, Double[]> scoreMap,
			MersenneTwisterFast rand) {

		Double[] scores = scoreMap.get(regionCode);
		double[] cumulProbs = new double[scores.length];
		double total = calcCumulativeProbs(scores, cumulProbs);
		for (int i = 0; i < numberOfHouseholds; ++i) {
			int index = chooseStochasticallyFromCumulativeProbs(cumulProbs,
					total, rand);

			Cell cell = cellOfInterest.get(index);
			cell.addHousehold();

			// add social weight to the correspond score
			scoreMap.get(regionCode)[index] += socialWeight;

			// add discounted socialWeight to adjacent cells
			// System.out.println(socialWeightSpread);
			if (socialWeightSpread != 0.0) {
				double adjacentSocialWeight = socialWeight * socialWeightSpread;
				increaseAdjacentScores(cell, adjacentSocialWeight,
						initPopRegionGrid, scoreMap, scoreIndexGrid);
			}
		}

	}

	private void increaseAdjacentScores(Cell cell, double adjacentSocialWeight,
			IntGrid2D initPopRegionGrid, HashMap<Integer, Double[]> scoreMap,
			IntGrid2D scoreIndexGrid) {
		// FIXME why some of the region are out side of canada cell?
		for (int y = cell.y - 1; y <= cell.y + 1; y++) {
			for (int x = cell.x - 1; x <= cell.x + 1; x++) {
				if (((y != cell.y) || (x != cell.x)) && (x >= 0)
						&& (x < Map.GRID_WIDTH) && (y >= 0)
						&& (y < Map.GRID_HEIGHT)) {
					int regionCode = initPopRegionGrid.field[x][y];
					int index = scoreIndexGrid.field[x][y];
					if (scoreMap.get(regionCode) != null && index >= 0) {
						scoreMap.get(regionCode)[index] += adjacentSocialWeight;
					}

				}
			}
		}

	}

	/**
	 * Calculate the cumulative probabilities for the given array probabilities.
	 * 
	 * @return the sum of the probabilities
	 */
	private double calcCumulativeProbs(Double[] probs, double[] cumulProbs) {
		double sum = 0;
		for (int j = 0; j < probs.length; j++) {
			sum += probs[j];
			cumulProbs[j] = sum;
		}

		return sum;
	}

	/**
	 * Calculate the cumulative probabilities for the given array probabilities.
	 * @return the sum of the probabilities
	 */
	public static double calcCumulativeProbs(double[] probs, double[] cumulProbs) {
		double sum = 0;
		for (int j = 0; j < probs.length; j++) {
			sum += probs[j];
			cumulProbs[j] = sum;
		}
		
		return sum;
	}
	
	
	/**
	 * Modified version of Utility.chooseStochastically that takes an array of
	 * cumulative probabilities rather than calculating them itself.
	 */
	public static int chooseStochasticallyFromCumulativeProbs(double [] cumulProbs, double total, MersenneTwisterFast rand) {
		int val = Arrays.binarySearch(cumulProbs, rand.nextDouble() * total);
		if (val < 0)
		{
			val = -(val + 1);		// Arrays.binarySearch(...) returns (-(insertion point) - 1) if the key isn't found
		}
		
		if (val == cumulProbs.length)
			System.out.format("Error: val:%d, total:%f\n", val, total);
		
		return val;
	}

	/**
	 * Calculate the bounds of the total desirability grid in the given map.
	 * 
	 * @return A Double2D where x holds the min and y holds the max.
	 */
	public Double2D calcInitDesirabilityBounds(Map map) {
		// calculate bounds of desirability map
		double min = Double.POSITIVE_INFINITY;
		double max = Double.NEGATIVE_INFINITY;
		for (Cell cell : map.canadaCells) {
			double des = cell.initDes;
			if ((des == 0) || Double.isInfinite(des))
				continue;
			if (des < min)
				min = des;
			if (des > max)
				max = des;
		}

		return new Double2D(min, max);
	}

}

class Region implements Comparable<Region> {
	public int regionCode;
	public int numberOfCells;
	public int numHouseholds;
	public int remainingHouseholds;

	public Region(int regionCode, int numberOfCells, int numHouseholds) {
		this.regionCode = regionCode;
		this.numberOfCells = numberOfCells;
		this.numHouseholds = numHouseholds;
		this.remainingHouseholds = numHouseholds;
	}

	/**
	 * sort the list based on the numberOfCells, smaller numberOfCells show up
	 * first
	 */
	@Override
	public int compareTo(Region o) {
		if (this.numberOfCells < o.numberOfCells)
			return -1;
		else if (this.numberOfCells == o.numberOfCells)
			return 0;
		else
			return 1;
	}

}
