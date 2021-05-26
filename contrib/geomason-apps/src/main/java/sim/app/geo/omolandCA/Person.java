package sim.app.geo.omolandCA;

import ec.util.MersenneTwisterFast;

public class Person {

	// experience might affect adaptation
	// if villager work for some household and exposed to

	private int sex; // Male =1 ; Female = 0
	private int age; //
	// private int status; // 1 = head, 0= other
	private int familyID; // parent=1; child =2; member = 3;
	private int dateOfBirth = 1;

	private double income = 0;
	private final double wealth = 0; // total wealth

	// skill and preference
	private int educLevel; // 0 0r 1; // can be 0= no eduction 1= elementary 2=seconday 3 above seconday
	private double skill;
	public static final int DEPENDANT = 0;
	public static final int OFFFARM_WORKER_HH = 1;

	private int workingStatus; // 1-5

//    private Status role = null;
	Household family;
	boolean atHome = true;

	// protected Stoppable stopper;

	Parcel activityLocation; // holds where the villager currently located
	// for remitance - a town with different value will be assigned as location
	MersenneTwisterFast random = new MersenneTwisterFast();

//
//    public Person() {
//
//    }

	public Person(final int sexP, final int ageP, final Household hh) {
		this.setSex(sexP);
		this.setAge(ageP);

		this.setMyFamily(hh);
		// this.setDateOfBirth(2 + random.nextInt(360));

	}

	public void setAge(final int a) {
		age = a;
	}

	public int getAge() {
		return age;
	}

	public void setSex(final int s) {
		sex = s;
	}

	public int getSex() {
		return sex;
	}

	public void setEducationLevel(final int educ) {
		educLevel = educ;
	}

	public int getEducationLevel() {
		return educLevel;
	}

	public void setFamilyID(final int fid) {
		familyID = fid;
	}

	public int getFamilyID() {
		return familyID;
	}

	public void setMyFamily(final Household f) {
		family = f;
	}

	public Household getMyFamily() {
		return family;
	}

	public void setDateOfBirth(final int mb) {
		dateOfBirth = mb;
	}

	public int getDateOfBirth() {
		return dateOfBirth;
	}

	public void setActivityLocation(final Parcel loc) {
		activityLocation = loc;
	}

	public Parcel getActivityLocation() {
		return activityLocation;
	}

	public void setIncome(final double inc) {
		income = inc;
	}

	public double getIncome() {
		return income;
	}

	public void setSkill(final double skill) {
		this.skill = skill;
	}

	public double getSkill() {
		return skill;
	}

	public void setWorkingStatus(final int e) {
		workingStatus = e;
	}

	public int getWorkingStatus() {
		return workingStatus;
	}

}
