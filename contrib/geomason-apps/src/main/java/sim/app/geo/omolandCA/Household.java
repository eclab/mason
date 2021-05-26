package sim.app.geo.omolandCA;

import sim.engine.SimState;
import sim.engine.Steppable;
/**
 *
 * @author gmu
 */
import sim.util.Bag;
import sim.util.Valuable;

public class Household extends HouseholdData implements Steppable, Valuable, java.io.Serializable {

	// protected Stoppable stopper;
	public static final int ORDERING = 3;
	private int currentTime = 0;
	private Landscape ls;
	private int landPrepDate;
	private int currentOnset;
	private int currentAmount;
	private double onsetProb;
	private double amountProb;
	private int vision;
//    private Parcel scoutParcel;
	// private int season;
	private int cropTypeOne;

	int totalDaysConsumption = 10; //
	HerderActivity herderAct;
	FarmerActivity farmerAct;
	double maxLaborSizeForAdp = 7.0;
	HerdingAdaptation herdAdpt;
	FarmingAdaptation farmAdpt;
//    MersenneTwisterFast random;
	// int nextPredctionDate = 1;
	int predDate = 12;

	int hhNewForm = 890;
	int adjLabor = 30;
	double maxConThresholdExpense = 7; // max days to store
	private final int[] juleanCalanderRegularMonth = { 31, 28, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31 };

	public Household() {
		super();

		ls = null;
		landPrepDate = 30;
		herdAdpt = new HerdingAdaptation();
		farmAdpt = new FarmingAdaptation();
		herderAct = new HerderActivity();
		farmerAct = new FarmerActivity();

	}
	// <editor-fold defaultstate="collapsed" desc="setter and getter ">

	public void setCurrentOnset(final int onset) {
		currentOnset = onset;
	}

	public int getCurrentOnset() {
		return currentOnset;
	}

	public void setCurrentAmount(final int amount) {
		currentAmount = amount;
	}

	public int getCurrentAmount() {
		return currentAmount;
	}

	public void setLandPrepDate(final int d) {
		landPrepDate = d;
	}

	public int getLandPrepDate() {
		return landPrepDate;
	}

	private void setOnsetProbability(final double onset) {
		onsetProb = onset;
	}

	private double getOnsetProbability() {
		return onsetProb;
	}

	private void setAmountProbability(final double onset) {
		amountProb = onset;
	}

	private double getAmountProbability() {
		return amountProb;
	}

	public void setCropTypeOne(final int c) {
		cropTypeOne = c;
	}

	public int getCropTypeOne() {
		return cropTypeOne;
	}

	public void setHerdingVision(final int v) {
		vision = v;
	}

	public int getHerdingVision() {
		return vision;
	}

// </editor-fold>
	private boolean isFarmingPossible() {
		boolean isfarming = false;

		if (this.getFarmLands().numObjs > 0) {
			isfarming = true;

		} else {
			isfarming = false;
		}
		return isfarming;
	}

	private boolean isHerdingPossible() {
		if (this.getHerd() != null && this.getHerd().getHerdSizeTLU() > 0) {
			return true;
		} else {
			return false;
		}
	}

	public void consume() {

		final double currentExpenditure = this.getStoredCapital() - totalConsumptionAdultEquivalentUnit();

		if (currentExpenditure <= 0) {
			this.setStoredCapital(0);
		} else {
			this.setStoredCapital(currentExpenditure);
			assert currentExpenditure >= 0 : "totalStoredIncome" + currentExpenditure;
		}

	}

	private double totalConsumptionAdultEquivalentUnit() {

		double totCons = 0;
		double totalWealth = 0;
		if (this.getWealth() <= 0) {
			totalWealth = 0;
		} else {
			totalWealth = this.getWealth();
		}

		if (this.getFamilyMembers().numObjs <= 0) {
			totCons = 0;
		} else {
			final double k = ls.params.householdParam.getMimimumExpenditurePerPerson() * maxConThresholdExpense
					* this.getFamilyMembers().numObjs;
			final double cons = 1.0 / (1.0 + Math.exp(-(totalWealth - k) / (totalWealth + k))); // maximum 5 time the
																								// average consumption
			double r = 0.5 + (cons * totalWealth / k);
			if (r < 0.5) {
				r = 0.5;
			}
			if (r > 3) {
				r = 3;
			}
			totCons = ls.params.householdParam.getMimimumExpenditurePerPerson() * r * this.getFamilyMembers().numObjs;
		}

		return totCons; // convert to month

	}

	public boolean hasIrrigableLand() {
		int count = 0;
		for (final Object obj : this.getFarmLands()) {
			final Parcel farm = (Parcel) obj;
			if (farm.getIsIrrigatedFarm() == true) {
				count++;
			}
		}

		if (count > 0) {
			return true;
		} else {
			return false;
		}

	}

	// wealth is both livestock and monitary-
	public void updateWealth() {

		double farmIncome = 0;
		double herdIncome = 0;

		double tluC = 0;

		updateHerdIncome();

		if (this.getIncomeFarming() >= 0) {
			farmIncome = this.getIncomeFarming();
		}
		if (this.getIncomeHerding() >= 0) {
			herdIncome = this.getIncomeHerding();
		}

		if (this.getHerd() != null && this.getHerd().getHerdSizeTLU() > 0) {
			tluC = this.getHerd().getHerdSizeTLU() * ls.params.herdingParam.getAverageHerdPrice();

		}

		if (tluC <= 0) {
			tluC = 0;
		}

		final double totalStoredIncome = this.getStoredCapital() + farmIncome + herdIncome;

		this.setStoredCapital(totalStoredIncome);
		this.setIncomeHerding(0);
		this.setIncomeFarming(0);
		this.setIncomeLabor(0);

		// NO RELIEF SUPPORT
		final double totWealth = totalStoredIncome + tluC;
		if (this.getFamilyMembers().numObjs > 0) {
			this.setWealth(totWealth);
		} else {
			this.setWealth(0);
		}
		consume();

	}

	public void updateHerdIncome() {
		// simple assumption - no milk consideration- since TLU is a combination of
		// sheep, goat and cattle)
		// price set at the paramter is based on TLU
		double totalHerdInc = 0;

		if (this.getHerd() != null && this.getHerd().getHerdSizeTLU() > 0) {
			double herdSell = 0;
			if (this.getStoredCapital() <= ls.params.householdParam.getMimimumExpenditurePerPerson()
					* this.getFamilyMembers().numObjs * totalDaysConsumption) {
				if (this.getHerd().getHerdSizeTLU() <= 0.2) {
					herdSell = this.getHerd().getHerdSizeTLU();
				} else if (this.getHerd().getHerdSizeTLU() > 0.2 && this.getHerd().getHerdSizeTLU() < 3.0) {
					herdSell = 0.2;
				} else {
					herdSell = 0.2 + 1.3 * ls.random.nextDouble();
				}
			}

			totalHerdInc = this.getIncomeHerding() + (herdSell * ls.params.herdingParam.getAverageHerdPrice());

			final double remain = this.getHerd().getHerdSizeTLU() - herdSell;
			if (remain > 0) {
				this.getHerd().setHerdSizeTLU(remain);
			} else {
				this.getHerd().setHerdSizeTLU(0);
			}

		}
		this.setIncomeHerding(totalHerdInc);

	}

	public void determineSeasonClimateSituation(final Climate climate) {

		// if prediction date- agent predict
		// based on prediction decide which adaptation measure they want to apply
		// make land preparation date
		// after prepration,
		// track when the moisure is > MER
		// real onset is used to update their memory
		// there is penality if

		if (currentTime % 12 == 0) {
			this.percivedOnsetProbability();
			this.percievedAmountProbability();
			this.adaptationIntention();
			this.allocateLabor();
			this.implementClimateChangeAdaptation(climate);
			this.updateClimateChangeAdaptationExp(ls.climate);

		}

		if (currentTime % 12 == climate.START_OF_SEASONTWO) {
			this.allocateLabor();
			this.implementClimateChangeAdaptation(climate);

		}

		// labor

		// prediction - intiution for all
		// adaptive farmer - moisture factor flactuate- use high yield
		// adaptive herder - use labor more often -> increase productivity - destock and
		// restock

	}

	private void implementActivity() {
		if (this.getHerd() != null && this.getHerd().getHerdSizeTLU() > 0) {
			implementHerdingActivity();
		}

		if (this.getFarmLands().numObjs != 0) {
			implementFarmingActivity();
		}

	}

	private void implementFarmingActivity() {

		farmerAct.implementFarmingActivity(this, this.getLandPrepDate(), this.getCropTypeOne(), currentTime, ls);

	}

	private void implementHerdingActivity() {

		herderAct.implementHerdActivity(this, currentTime, ls);

	}

	// based on climate risk - bast adp measure - amount resource , farmland -
	// allocate resource
	// based on the adaptation - allocate resource
	// based on adaptive mechanism - determien resource allocation- labor, land,
	// crop
	// based on input - implement acitivity
	/**
	 * ***********#################################*************************
	 */
	// farmer prcedure - predict climate -
	// choose best adaptive mechanism
	// implement that adaptive mechanism
//
	public void implementClimateChangeAdaptation(final Climate climate) {

		if (this.isHerdingPossible() == true) {

			if (this.getCurrenAdaptationMechanism() == ls.WITH_ADAPTATION) {
				herdingWithAdaptation(herdAdpt, climate);
			} else {
				herdingWithNoAdaptation(herdAdpt, climate);
			}

		}

		if (this.isFarmingPossible() == true) {
			// this.assignFarmActivityManager();

			if (this.getCurrenAdaptationMechanism() == ls.WITH_ADAPTATION) {
				if (this.isExpansionPossible() == true && ls.getRandom().nextDouble() < 0.2) {
					final FarmLandManager fm = new FarmLandManager();
					final Parcel p = fm.encroachRandomNeighbor(this.getLocation(), 1, ls);
					if (p != null) {
						p.setOwner(this);
						p.setLanduseType(ls.FARMLAND);
						p.setIsOccupied(true);
						this.addFarmLands(p);
						final double stored = this.getStoredCapital();
						this.setStoredCapital(stored - ls.params.farmingParam.getAverageInitialFarmingCost());
					}

				}

				farmingWithAdaptation(farmAdpt, climate);
			} else {
				farmingWithNoAdaptation(farmAdpt, climate);
			}

		}

	}

	public void farmingWithAdaptation(final FarmingAdaptation farmAdp, final Climate climate) {

		farmAdp.farmAdaptation(this.getCurrentOnset(), this.getCurrentAmount(), this, this.getLocation(), climate, ls);
		final int landPre = climate.determineSeasonLandPreparation(this.currentSeason(currentTime),
				this.getCurrentOnset(), this.getLocation().xLoc, this.getLocation().yLoc);
		this.setLandPrepDate(landPre);

	}

	public void farmingWithNoAdaptation(final FarmingAdaptation farmAdp, final Climate climate) {
		farmAdp.farmAdaptation(ls.ONTIME, ls.NORMAL, this, this.getLocation(), climate, ls);
		final int landPre = climate.determineSeasonLandPreparation(this.currentSeason(currentTime),
				this.getCurrentOnset(), this.getLocation().xLoc, this.getLocation().yLoc);
		this.setLandPrepDate(landPre);

	}

	public void herdingWithAdaptation(final HerdingAdaptation herdAdp, final Climate climate) {
		herdAdp.herdAdaptation(this, this.getCurrentOnset(), this.getCurrentAmount(), ls);
		this.setHerdingVision(ls.params.herdingParam.getHerderScoutingRange());

	}

	public void herdingWithNoAdaptation(final HerdingAdaptation herdAdp, final Climate climate) {
		herdAdp.herdNoAdaptation(this, ls);
		this.setHerdingVision(ls.params.herdingParam.getHerderMinVisionRange());

	}

	public void updateClimateChangeAdaptationExp(final Climate climate) {

		final Bag adjacentParcels = new Bag();
		adjacentParcels.clear();
		ls.getNearestNeighborAreas(this.getLocation().getX(), this.getLocation().getY(), 3, adjacentParcels);

		double neighExp = 0;
		int totHH = 0;
		if (adjacentParcels.numObjs <= 1) {
			neighExp = 0;
		} else {
			for (final Object o : adjacentParcels) {
				final Parcel p = (Parcel) o;
				if (p.getOwner() == null) {
					continue;
				}

				if (p.getOwner().getClimateCAdaptationExperience() > this.getClimateCAdaptationExperience()) {
					neighExp = neighExp + p.getOwner().getClimateCAdaptationExperience();

				}
				totHH = totHH + 1;

			}

		}

		double aveExp = 0;
		if (totHH == 0 || neighExp == 0) {
			aveExp = 0;
		} else {
			aveExp = (neighExp / (1.0 * totHH)) * ls.params.householdParam.getAdaptiveAgentLearningRate()
					* this.getLearningRate();
		}

		double adpt;

		if (this.getCurrenAdaptationMechanism() == ls.WITH_ADAPTATION) {
			adpt = ls.params.householdParam.getAdaptiveAgentLearningRate();
		} else {
			// inginuity - even if not adpt but agent may get some know-how about cliamte
			// change
			if (ls.getRandom().nextDouble() < 0.01) {
				adpt = ls.params.householdParam.getAdaptiveAgentLearningRate() * this.getLearningRate()
						* ls.getRandom().nextDouble();
			} else {
				adpt = 0;
			}

		}

		double currentAdpt = this.getClimateCAdaptationExperience() + (aveExp * adpt)
				+ 0.001 * ls.getRandom().nextDouble();

		if (currentAdpt < 0) {
			currentAdpt = 0;
		}

		if (currentAdpt > 1) {
			currentAdpt = 1;
		}

		this.setClimateCAdaptationExperience(currentAdpt);

	}

	/**
	 * ***********#################################*************************
	 */
	private void percivedOnsetProbability() {
		final double[] early = { 0, 0, 0, 0 };
		final double[] ontime = { 0, 0, 0, 0 };
		final double[] late = { 0, 0, 0, 0 };

		final double[] preOnset = { 0, 0, 0 };

		final int cSeason = Climate.MAINRAINSEASON_ID;
		for (int i = 0; i < preOnset.length + 1; i++) {
			final int onset = this.getRainfallOnsetMemory()[i][cSeason];
			if (onset == ls.EARLY) {
				early[i] = 1.0;

			} else {
				early[i] = 0;
			}

			if (onset == ls.ONTIME) {
				ontime[i] = 1.0;

			} else {
				ontime[i] = 0;
			}

			if (onset == ls.LATE) {
				late[i] = 1.0;

			} else {
				late[i] = 0;
			}

		}

		preOnset[0] = ls.wOnsetYear1 * early[0] + ls.wOnsetYear2 * early[1] + ls.wOnsetYear3 * early[2]
				+ ls.getRandom().nextDouble() * early[3];
		preOnset[1] = ls.wOnsetYear1 * ontime[0] + ls.wOnsetYear2 * ontime[1] + ls.wOnsetYear3 * ontime[2]
				+ ls.getRandom().nextDouble() * ontime[3];
		preOnset[2] = ls.wOnsetYear1 * late[0] + ls.wOnsetYear2 * late[1] + ls.wOnsetYear3 * late[2]
				+ ls.getRandom().nextDouble() * late[3];

		int currOnset = 0;
		double maxOnset;
		for (int i = 0; i < preOnset.length; i++) {
			maxOnset = preOnset[currOnset];
			if (preOnset[i] >= maxOnset) {
				currOnset = i;
			}

		}

		this.setCurrentOnset(currOnset + 1);
		this.setOnsetProbability(preOnset[currOnset]);

	}

	private void percievedAmountProbability() {
		final double[] below = { 0, 0, 0, 0 };
		final double[] normal = { 0, 0, 0, 0 };
		final double[] above = { 0, 0, 0, 0 };

		final double[] preAmount = { 0, 0, 0 };

		final int cSeason = Climate.MAINRAINSEASON_ID;

		for (int i = 0; i < preAmount.length + 1; i++) {
			final int amount = this.getRainfallAmountMemory()[i][cSeason];
			if (amount == ls.BELOW_NORMAL) {
				below[i] = 1.0;
			} else {
				below[i] = 0;
			}

			if (amount == ls.NORMAL) {
				normal[i] = 1.0;
			} else {
				normal[i] = 0;
			}

			if (amount == ls.ABOVE_NORMAL) {
				above[i] = 1.0;
			} else {
				above[i] = 0;
			}

		}

		preAmount[0] = ls.wAmountYear1 * below[0] + ls.wAmountYear2 * below[1] + ls.wAmountYear3 * below[2]
				+ ls.wAmountYear4 * below[3] + 0.01 * ls.getRandom().nextDouble();
		preAmount[1] = ls.wAmountYear1 * normal[0] + ls.wAmountYear2 * normal[1] + ls.wAmountYear3 * normal[2]
				+ ls.wAmountYear4 * normal[3] + 0.01 * ls.getRandom().nextDouble();
		preAmount[2] = ls.wAmountYear1 * above[0] + ls.wAmountYear2 * above[1] + ls.wAmountYear3 * above[2]
				+ ls.wAmountYear4 * above[3] + 0.01 * ls.getRandom().nextDouble();

		int currAmount = 0;
		double maxAmount = 0;
		for (int i = 0; i < preAmount.length; i++) {
			maxAmount = preAmount[currAmount];
			if (preAmount[i] >= maxAmount) {
				currAmount = i;
			}

		}

		this.setCurrentAmount(currAmount + 1);
		this.setAmountProbability(preAmount[currAmount]);

	}
	// measuring the risk for each livelihood helps agent to allocate resource to
	// less risky livelihood
	// any change from the normal may case potential damage on farming

	private double percievedSeverity() {

		// severity = seveityIndex *(alpha * onset + (1-alpha * amount)
		// perProbabilityWeightIndex

		final int amountIndex = this.getCurrentAmount() - 1;
		final int onsetIndex = this.getCurrentOnset() - 1;

		double severity;

		severity = this.getOnsetProbability() * this.getAmountProbability()
				* ls.PER_SEVERITY_INDEX[onsetIndex][amountIndex];
		if (severity > 1) {
			severity = 1;
		}
		if (severity < 0) {
			severity = 0;
		}

		return severity;
	}

	// private double compareFarmAdaptiveCap(int cropID,)
	// adaptation efficancy of each adaptive mechanism
	private double percivedAdatationEfficancy() {
		// personal characterstics + knowledge of the adaptio options
		// if best crop for current condition is the same as the

		// double ageHH = this.getMemoryHolder().

		int age = 1; // age of the hh head -- better to take the average age of potential worker (
						// age > 15)
		int laborForce = 0;
		int maxAge = 1;
		for (final Object o : this.getFamilyMembers()) {
			final Person v = (Person) o;

			if (v.getAge() >= ls.params.householdParam.getMinimumLaborAge()) {
				age = age + v.getAge();
				laborForce = laborForce + 1;
				if (v.getAge() > maxAge) {
					maxAge = v.getAge();
				}

			}
		}

		// increase the level of adaptation intention - either the agent is responsve to
		// amount or not
		// coefficent
		if (maxAge > 50) {
			maxAge = 50;
		}
		if (laborForce > 10) {
			laborForce = 10;
		}

		final double ageAv = (maxAge * 1.0) / (50.0);
		final double wAge = 1.0 - (0.12 / (0.12 + ageAv * ageAv * ageAv)); // age
		final double hhFactor = (laborForce * 1.0) / (maxLaborSizeForAdp * 1.0);
		final double wHHSize = 1.0 - (0.12 / (0.12 + (hhFactor * hhFactor * hhFactor)));
		final double wAccessTechno = Math.pow((1.0 - this.getLocation().getAccessToTechno()), 1.5);

		// no need to include education - but signigicant in ethiopian case
		// wAccessTechno used as proxy
		// distance from town??
		// adaptation experience
		double adpE = 0.4 * wAge + 0.3 * wHHSize + 0.3 * wAccessTechno + (0.2 - 0.3 * ls.getRandom().nextDouble());
		if (adpE > 1) {
			adpE = 1;
		}

		return adpE;

	}

	private double percieevedSelfEfficancy() {
		final double w = (this.getWealth() - 10000.0) / 30000.0;
		final double weatlhF = 1.0 / (1.0 + Math.exp(-3.0 * (w - 0.5)));
		final double percivedSelfE = 0.3 * weatlhF + 0.6 * this.getClimateCAdaptationExperience()
				+ (0.1 - 0.2 * ls.getRandom().nextDouble());

		return percivedSelfE;

	}

	// household- choose to adpat or not to adapt
	// if adapt - they choose appropraite adaptation - that give them good benefit
	private double percievedAdatationCost() {
		final double soilFert = this.getLocation().getSoilQuality(); // max soil fert = 1.0 >0.7 no need tp use
																		// ferilizer

		// cost - moisture cost, soil fertility cost , new land cost ( fxed param???)

		double labDemand = 0;
		double farmlaborDemand = 0;
		double herdLaborDemand = 0;
		if (this.getHerd() != null && this.getHerd().getHerdSizeTLU() > 0) {
			herdLaborDemand = Math
					.pow(this.getHerd().getHerdSizeTLU() / ls.params.herdingParam.getProportionLaborToTLU(), 0.8);
		}
		if (this.getFarmLands().numObjs > 0) {
			farmlaborDemand = (this.getFarmLands().numObjs * 0.5);
		}
		labDemand = farmlaborDemand + herdLaborDemand;

		// cost can be 20%-100%
		final double potentialCost = ((this.getFarmLands().numObjs * (1.0 - soilFert)
				* ls.params.farmingParam.getFarmInputCost())
				+ (ls.params.farmingParam.getIrrigationFarmCost() * ls.getRandom().nextDouble()))
				* (0.3 + 0.7 * ls.getRandom().nextDouble());
		final double potentialCostPer = (potentialCost)
				/ ((0.4 * this.getWealth() + 0.4 * ls.getRandom().nextDouble() * this.getWealth()) + 1.0);
		double costF = (1.0 - (0.12 / (0.12 + potentialCostPer * potentialCostPer * potentialCostPer)));

		if (costF > 1) {
			costF = 1;
		}

		if (costF < 0) {
			costF = 0;
		}
		// System.out.println("cost: " +costF);
		return costF;
	}

//    private double socialCostAdpt(int currentAdptation) {
//        // social network - how different your adaptation is ?
//        // get around your neighbors - their adaptation
//        // if you have experience of the same adaptation - social cost is minimum- you have already implelemnted it
//        // other wise to be a starter is difficult - have resitance value
//
//        return 0;
//    }
	private double riskAppraisal() {

		return (0.6 * percievedSeverity() + 0.4 * ls.getRandom().nextDouble());

	}

	private double adaptationAppraisal() {
		// weight for each parameter - necessary

		double adpAppr = 0.5 * percivedAdatationEfficancy() + 0.5 * percieevedSelfEfficancy();

		if (adpAppr >= 1.0) {
			adpAppr = 1.0;
		}
		if (adpAppr < 0) {
			adpAppr = 0;
		}

		return adpAppr;

	}

	private void adaptationIntention() {
//        int onset = this.getCurrentOnset();
//        int amount  = this.getCurrentAmount();

		final double r = ls.params.householdParam.getRiskElasticity() * (riskAppraisal()); // risk ;
		final double c = this.percievedAdatationCost() * ls.params.householdParam.getCostAdptElasticity();
		final double p = adaptationAppraisal() * (1.0 - ls.params.householdParam.getCognitiveBias());// proportional
																										// risk
																										// deduction

		final double adpIntention = (p - c) - (r);

		if (adpIntention >= ls.params.householdParam.getAdaptationIntentionThreshold()) {
			this.setCurrenAdaptationMechanism(ls.WITH_ADAPTATION);

		} else {
			this.setCurrenAdaptationMechanism(ls.NO_ADAPTATION);
		}

		// System.out.println("INT: " + r);
		// return adpIntention;

	}

	public int currentSeason(final int cTime) {

		final int currentT = cTime % 12;

		if (currentT >= Climate.START_OF_SEASONONE && currentT < Climate.START_OF_SEASONTWO) {
			return Climate.MAINRAINSEASON_ID;
		} else {
			return Climate.SECONDRAINSEASON_ID;
		}
	}

	public void updateRainfallPatternMemory(final Climate climate) {

		if (currentTime % 12 == 6) { // before start of season two update seasonone

			final int year1 = this.getRainfallAmountMemory()[0][Climate.MAINRAINSEASON_ID];// last year
			final int year2 = this.getRainfallAmountMemory()[1][Climate.MAINRAINSEASON_ID];// last year

			this.getRainfallAmountMemory()[0][Climate.MAINRAINSEASON_ID] = climate.determineSeasonRainfallPattern(
					Climate.MAINRAINSEASON_ID, this.getLocation().getX(), this.getLocation().getY());
			this.getRainfallAmountMemory()[1][Climate.MAINRAINSEASON_ID] = year1;
			this.getRainfallAmountMemory()[2][Climate.MAINRAINSEASON_ID] = year2;
			this.getRainfallAmountMemory()[2][Climate.MAINRAINSEASON_ID] = 0 + ls.getRandom().nextInt(2);

		}

		if (currentTime % 12 == 11) { // before start of season one update seasontwo

			final int year1 = this.getRainfallAmountMemory()[0][Climate.SECONDRAINSEASON_ID];// last year
			final int year2 = this.getRainfallAmountMemory()[1][Climate.SECONDRAINSEASON_ID];// last year

			this.getRainfallAmountMemory()[0][Climate.SECONDRAINSEASON_ID] = climate.determineSeasonRainfallPattern(
					Climate.SECONDRAINSEASON_ID, this.getLocation().getX(), this.getLocation().getY());

			// this.getRainfallAmountMemory()[0][Climate.SECONDRAINSEASON_ID] =
			// climate.determineSeasonRainfallPattern(Climate.SECONDRAINSEASON_ID,
			// this.getLocation().getX(), this.getLocation().getY());
			this.getRainfallAmountMemory()[1][Climate.SECONDRAINSEASON_ID] = year1;
			this.getRainfallAmountMemory()[2][Climate.SECONDRAINSEASON_ID] = year2;
			this.getRainfallAmountMemory()[2][Climate.SECONDRAINSEASON_ID] = 0 + ls.getRandom().nextInt(2);

		}

	}

	public void updateRainfallOnsetMemory(final Climate climate) {
		if (currentTime % 12 == 4) {

			final int year1 = this.getRainfallOnsetMemory()[0][Climate.MAINRAINSEASON_ID];// last year
			final int year2 = this.getRainfallOnsetMemory()[1][Climate.MAINRAINSEASON_ID];// last year

			this.getRainfallOnsetMemory()[0][Climate.MAINRAINSEASON_ID] = climate.determineSeasonOnset(
					Climate.MAINRAINSEASON_ID, this.getLocation().getX(), this.getLocation().getY());

			this.getRainfallOnsetMemory()[1][Climate.MAINRAINSEASON_ID] = year1;
			this.getRainfallOnsetMemory()[2][Climate.MAINRAINSEASON_ID] = year2;
			this.getRainfallOnsetMemory()[2][Climate.MAINRAINSEASON_ID] = 0 + ls.getRandom().nextInt(2);

		}

		if (currentTime % 12 == 10) {

			final int year1 = this.getRainfallOnsetMemory()[0][Climate.SECONDRAINSEASON_ID];// last year
			final int year2 = this.getRainfallOnsetMemory()[1][Climate.SECONDRAINSEASON_ID];// last year

			this.getRainfallOnsetMemory()[0][Climate.SECONDRAINSEASON_ID] = climate.determineSeasonOnset(
					Climate.SECONDRAINSEASON_ID, this.getLocation().getX(), this.getLocation().getY());

			this.getRainfallOnsetMemory()[1][Climate.SECONDRAINSEASON_ID] = year1;
			this.getRainfallOnsetMemory()[2][Climate.SECONDRAINSEASON_ID] = year2;
			this.getRainfallOnsetMemory()[2][Climate.SECONDRAINSEASON_ID] = 0 + ls.getRandom().nextInt(2);
		}

	}

	public void removeFamilyMembers(final Person p) {

		// household head is dead, assign another household head
		if (this.getFamilyMembers().numObjs == 0) {
			return;
		} else {
			p.setMyFamily(null);
			this.getFamilyMembers().remove(p);

		}
	}

	private boolean isExpansionPossible() {

		if (this.getCurrenAdaptationMechanism() == ls.WITH_ADAPTATION && this.getStoredCapital() > 10000) {
			return true;
		} else {
			return false;
		}

	}

//
	private double laborFarm(final double size, final double supply) {

		final double maxHandling = Math.pow(supply, 1.1) * ls.params.farmingParam.getProportionLaborToAreaHa();

		final double demand = size / maxHandling;
		final double excess = supply - demand;
		double labor = 0;
		if (excess <= 0) {
			labor = supply * (0.5 + 0.3 * ls.getRandom().nextDouble()); // max 80% of labor
		} else {
			labor = demand * (0.5 + 0.3 * ls.getRandom().nextDouble())
					+ excess * (0.2 + 0.5 * ls.getRandom().nextDouble());
		}

		return labor;

	}

	private double laborLTU(final double size, final double supply) {
		final double maxHandling = Math.pow(supply, 1.2) * ls.params.herdingParam.getProportionLaborToTLU();
		final double demand = size / maxHandling;
		final double excess = supply - demand;
		double labor = 0;
		if (excess <= 0) {
			labor = supply * (0.5 + 0.3 * ls.getRandom().nextDouble()); // max 80% of labor
		} else {
			labor = demand * (0.5 + 0.3 * ls.getRandom().nextDouble())
					+ excess * (0.2 + 0.5 * ls.getRandom().nextDouble());
		}

		return labor;

	}

	private void allocateLabor() {

		double farmlaborFactor = 0;
		double herdlaborFactor = 0;
		final double potLabor = potentialLabor();

		if (potLabor <= 0) {
			herdlaborFactor = 0;
			farmlaborFactor = 0;
		} else {
			if (this.getHerd() != null && this.getHerd().getHerdSizeTLU() > 0) {
				herdlaborFactor = laborLTU(this.getHerd().getHerdSizeTLU(), potLabor);
				if (this.getFarmLands().numObjs > 0) {
					final double fPot = laborFarm(this.getFarmLands().numObjs, potLabor);
					if (fPot >= (potLabor - herdlaborFactor)) {
						farmlaborFactor = (potLabor - herdlaborFactor);
					} else {
						farmlaborFactor = fPot;
					}
				} else {
					farmlaborFactor = 0;
				}

			} else {
				herdlaborFactor = 0;
				if (this.getFarmLands().numObjs > 0) {
					farmlaborFactor = laborFarm(this.getFarmLands().numObjs, potLabor);
				} else {
					farmlaborFactor = 0;
				}

			}
		}

		this.setLaborAllocatedHerding(herdlaborFactor);
		this.setLaborAllocatedFarming(farmlaborFactor);

	}

	private double potentialLabor() {
		int potLabor = 0;
		for (final Object o : this.getFamilyMembers()) {
			final Person p = (Person) o;
			if (p.getAge() >= ls.params.householdParam.getMinimumLaborAge()) {
				potLabor = potLabor + 1;
			}

		}

		return (potLabor * 1.0);
	}

	public void step(final SimState state) {
		ls = (Landscape) state;
		currentTime = (int) ls.schedule.getSteps();

		determineSeasonClimateSituation(ls.climate);
		implementActivity();
		// updateLaborAllocation();
		updateRainfallOnsetMemory(ls.climate);
		updateRainfallPatternMemory(ls.climate);
		updateWealth();

	}

	public double doubleValue() {
		return 1.0;
	}

	private int calcOnsetMargin(final int onset) {
		int onsetMar = 0;
		final int lp = currentTime % 12;

		if (onset == ls.EARLY) {
			onsetMar = 0;

		} else if (onset == ls.ONTIME) {

			onsetMar = 0;

		} else {
			onsetMar = 2;
		}

		return lp + onsetMar;

	}

}
