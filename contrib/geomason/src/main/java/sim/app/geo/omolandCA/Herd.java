package sim.app.geo.omolandCA;

import sim.engine.SimState;
import sim.engine.Steppable;
import sim.util.Bag;

/**
 *
 * @author gmu
 */
public class Herd implements Steppable {

	// herd grass or farm residue intake
	// http://www.fao.org/wairdocs/ILRI/x5485E/x5485e05.htm
	// average 1 cattle daily DM intake = 5Kg/day = 35kg/week
	// grass/ha/year = 52ton/hectare/year = 1ton/ha/week (for simipliciy) =
	// 1000kg/ha // too much
	//
	// cattle can live without food for max 60 days,
	// water - only 1 week
	// grass grow on average 1 week.
	// Zebu growthrate per day = 300(g/day) = 2100g/week = 2.1kg/week - intake -
	// 3kg/day - 21/week
	// 1 tlu = 250 kg livestock
	private double herdSizeTLU;
	private double herdFood; // initally herd will have 3-9 week food -
	// private double hungerLevel;

	private Household owner;
	// Person cowboy;
	private Parcel herdLocation; // agent add four-eight parcel and feed livestock
	public static final int ORDERING = 4;
	private Parcel visionParcel;
	private final double HERDMAXIMUMSIZE = 140.0; // Max size is 200 catle = 140 tlu CSA
													// http://www.csa.gov.et/images/general/news/livs_2014_2015
	private final double HERDHUNGERTHRESHOLD = 35.0; // --> about 10% of max food storage -- if less than this herd
														// start dieing
	// some paramters

	public Herd(final Household h) {
		this.setHerdOwner(h);

	}

	public void setHerdSizeTLU(final double size) {
		herdSizeTLU = size;
	}

	public double getHerdSizeTLU() {
		return herdSizeTLU;
	}

	public void setHerdFood(final double food) {
		herdFood = food;
	}

	public double getHerdFood() {
		return herdFood;
	}

	public void setHerdLocation(final Parcel loc) {
		herdLocation = loc;
	}

	public Parcel getHerdLocation() {
		return herdLocation;
	}

	public void setPotentialVisionParcel(final Parcel p) {
		visionParcel = p;
	}

	public Parcel getPotentialVisionParcel() {
		return visionParcel;
	}

	public void setHerdOwner(final Household h) {
		owner = h;
	}

	public Household getHerdOwner() {
		return owner;
	}

	public void eatGrass(final Landscape ls) {
		// average amount of dry matter a herd can eat in a week??
		// if there is too much grass, only eat until the level
		// proportion of grass - too much grass will be eaten too much
		// imagine
		// calcHungerlevel();
		double foodLevel = 0;
		if (this.getHerdSizeTLU() <= 0) {
			return;
		}

		if (this.getHerdSizeTLU() <= 1.0) {
			foodLevel = this.getHerdFood() / (ls.params.herdingParam.getHerdMaxFoodStored());
		} else {
			foodLevel = this.getHerdFood() / ((this.getHerdSizeTLU() * ls.params.herdingParam.getHerdMaxFoodStored()));
		}

		if (foodLevel >= 0.9) {
			return;
		}

		final double maxIntake = ls.params.herdingParam.getHerdMaxDailyDMIntake() * this.getHerdSizeTLU(); // maximum
																											// grass the
																											// herd can
																											// eat

		final Bag adjacentParcels = new Bag();
		adjacentParcels.clear();

		ls.getNearestNeighborGrazingAreas(getHerdLocation().getX(), getHerdLocation().getY(),
				HerderActivity.MINIMUMVISIONFEED, adjacentParcels);
		// assert adjacentParcels.numObjs > 0 : "total hunger: " +
		// adjacentParcels.numObjs;
		adjacentParcels.shuffle(ls.random);

		for (final Object o : adjacentParcels) {

			final Parcel grassParcel = (Parcel) o;

			if (grassParcel.getLanduseType() == ls.RIVER || grassParcel.getLanduseType() == ls.LAKE) {
				continue; // not parcel
			}

			if (grassParcel.getLanduseType() == ls.FARMLAND) {
				continue;
			}

			if (grassParcel.getGrass() <= ls.params.vegetationParam.getMinVegetation()) {
				continue; // no vegetation
			}

			// you are full,
			double totalHunger = 0;

			if (this.getHerdFood() + maxIntake >= ls.params.herdingParam.getHerdMaxFoodStored()
					* this.getHerdSizeTLU()) {
				totalHunger = (ls.params.herdingParam.getHerdMaxFoodStored() * this.getHerdSizeTLU())
						- this.getHerdFood();
			} else {

				totalHunger = maxIntake;
			}

			if (totalHunger < 0) {
				totalHunger = 0;
			}

			assert totalHunger >= 0 : "total hunger: " + totalHunger;

			final double vegEaten = grassParcel.eatGrass(totalHunger, ls);

			double tempV = this.getHerdFood() + vegEaten;
			if (tempV > ls.params.herdingParam.getHerdMaxFoodStored() * this.getHerdSizeTLU()) {
				tempV = ls.params.herdingParam.getHerdMaxFoodStored() * this.getHerdSizeTLU();
			}
			this.setHerdFood(tempV);

		}

	}

	//
	public void move(final Parcel new_parcel) {

		final Parcel oldLocation = getHerdLocation();
		oldLocation.getHerds().remove(this);
		setHerdLocation(new_parcel);
		getHerdLocation().getHerds().add(this);

	}

	// need energy to stay alive-
	public void metabolize(final Landscape ls) {

		if (this.getHerdSizeTLU() <= 0) {

			return;
		}
		final double cons = this.getHerdSizeTLU() * ls.params.herdingParam.getHerdDailyConsumptionRate();
		double metaboliztion = 0;

		if (this.getHerdFood() > cons) {
			metaboliztion = this.getHerdFood() - cons;
			setHerdFood(metaboliztion);

		} else {
			setHerdFood(0);

		}

	}

	public void adjustHerdSize(final Landscape ls) {

		double tluSize = 0;
		final double adaptWeight = 0.3;
		final double laborWeight = 0.7;
		final double laborAndAdContribution = 0.05;
		// when there is enough food, birth willl happen. f it is not enough food, death
		// will happen

		if (this.getHerdSizeTLU() <= 0) {
			return;
		}

		final double laborDeman = this.getHerdSizeTLU() / ls.params.herdingParam.getProportionLaborToTLU();
		final double laborSupply = Math.pow(getHerdOwner().getLaborAllocatedHerding(), 1.27);

		final double laborFac = (((laborSupply - laborDeman) / (laborSupply + laborDeman)) + 1.0) / 2.0;// -1 to 1 --
																										// but changed
																										// to 0 to 1
		// adaptation impact
		double adpt = 0.0;
		if (this.getHerdOwner().getCurrenAdaptationMechanism() == ls.WITH_ADAPTATION) { // experience has more value
			adpt = this.getHerdOwner().getClimateCAdaptationExperience();
		}

		final double lb = 1.0 + laborAndAdContribution * (adaptWeight * adpt + laborWeight * laborFac);
		final double foodFc = this.getHerdFood() / this.getHerdSizeTLU(); // change to indivizual tlu food amount
		final double growthFactor = (foodFc * lb - HERDHUNGERTHRESHOLD) / ls.params.herdingParam.getHerdMaxFoodStored();
		final double growthRateprob = growthFactor * this.getHerdSizeTLU() * ls.params.herdingParam.getHerdGrowthRate();
		tluSize = this.getHerdSizeTLU() + growthRateprob;
		// tluSize = this.getHerdSizeTLU() + 10- 20 *ls.getRandom().nextDouble() ;

		if (tluSize < 0) {
			tluSize = 0;
		}

		if (tluSize > HERDMAXIMUMSIZE) {
			final double inc = this.getHerdOwner().getIncomeHerding()
					+ 0.1 * ls.params.herdingParam.getAverageHerdPrice() * (tluSize - HERDMAXIMUMSIZE)
							* ls.getRandom().nextDouble();
			this.getHerdOwner().setIncomeHerding(inc);
			tluSize = HERDMAXIMUMSIZE;
		}

		this.setHerdSizeTLU(tluSize);

	}

	public void step(final SimState state) {
		final Landscape ls = (Landscape) state;
		eatGrass(ls);
		metabolize(ls);
		adjustHerdSize(ls);

	}
}
