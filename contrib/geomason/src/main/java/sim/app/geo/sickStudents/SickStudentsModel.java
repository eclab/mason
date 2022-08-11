/**
 ** SickStudentsModel.java
 **
 ** Copyright 2011 by Joseph Harrison, Andrew Crooks, Mark Coletti, Cristina Metgher
 ** George Mason University.
 **
 ** Licensed under the Academic Free License version 3.0
 **
 ** See the file "LICENSE" for more information
 **
 ** $Id$
 **/
package sim.app.geo.sickStudents;

import sim.app.geo.sickStudents.data.SickStudentsData;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;

import sim.engine.SimState;
import sim.engine.Steppable;
import sim.field.geo.GeomVectorField;
import sim.io.geo.ShapeFileImporter;
import sim.util.Bag;
import sim.util.Interval;
import sim.util.geo.MasonGeometry;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.Point;

@SuppressWarnings("serial")
public class SickStudentsModel extends SimState {
    public int numHouseholds = 10000;

    public int getNumHouseholds() {
        return numHouseholds;
        }

    public void setNumHouseholds(final int val) {
        numHouseholds = val;
        reInitHouseholds = true;
        }

    public boolean reInitHouseholds = true;

    public boolean getReInitHouseholds() {
        return reInitHouseholds;
        }

    public void setReInitHouseholds(final boolean val) {
        reInitHouseholds = val;
        }

    public double childrenPerHouseholdAve = 1.45;

    public double getChildrenPerHouseholdAve() {
        return childrenPerHouseholdAve;
        }

    public void setChildrenPerHouseholdAve(final double val) {
        childrenPerHouseholdAve = val;
        }

    public double childrenPerHouseholdStdev = 0.5;

    public double getChildrenPerHouseholdStdev() {
        return childrenPerHouseholdStdev;
        }

    public void setChildrenPerHouseholdStdev(final double val) {
        childrenPerHouseholdStdev = val;
        }

    public int numInitialInfections = 10;

    public int getNumInitialInfections() {
        return numInitialInfections;
        }

    public void setNumInitialInfections(final int val) {
        numInitialInfections = val;
        }

    public int diseaseDuration = 10; // days

    public int getDiseaseDuration() {
        return diseaseDuration;
        }

    public void setDiseaseDuration(final int val) {
        diseaseDuration = val;
        }

    public int incubationPeriod = 2; // days

    public int getIncubationPeriod() {
        return incubationPeriod;
        }

    public void setIncubationPeriod(final int val) {
        incubationPeriod = val;
        }

    public double diseaseTransmissionProb = 0.3;

    public double getDiseaseTransmissionProb() {
        return diseaseTransmissionProb;
        }

    public void setDiseaseTransmissionProb(final double val) {
        diseaseTransmissionProb = val;
        }

    public Object domDiseaseTransmissionProb() {
        return new Interval(0.0, 1.0);
        }

    public boolean closeSchoolUponOutbreak = false;

    public boolean getCloseSchoolUponOutbreak() {
        return closeSchoolUponOutbreak;
        }

    public void setCloseSchoolUponOutbreak(final boolean val) {
        closeSchoolUponOutbreak = val;
        }

    public boolean closeAllSchoolsUponOutbreak = false;

    public boolean getCloseAllSchoolsUponOutbreak() {
        return closeAllSchoolsUponOutbreak;
        }

    public void setCloseAllSchoolsUponOutbreak(final boolean val) {
        closeAllSchoolsUponOutbreak = val;
        }

    /**
     * The proportion of students at a school who must get sick before all schools
     * get closed.
     */
    public double outbreakThreshold = 0.20;

    public double getOutbreakThreshold() {
        return outbreakThreshold;
        }

    public void setOutbreakThreshold(final double val) {
        outbreakThreshold = val;
        }

    public Object domOutbreakThreshold() {
        return new Interval(0.0, 1.0);
        }

    /**
     * The following are to allow the tracking of SIR counts from the main
     * simulation panel. The movie-making tool works better there than through the
     * custom chart.
     */
    int sCount = 0, iCount = 0, rCount = 0;

    /**
     * Member variables.
     */
    public int width = 800;
    public int height = 800;
    // where all the county geometry lives
    public GeomVectorField elementarySchoolZones = new GeomVectorField(width, height);
    public GeomVectorField middleSchoolZones = new GeomVectorField(width, height);
    public GeomVectorField highSchoolZones = new GeomVectorField(width, height);
    public GeomVectorField elementarySchools = new GeomVectorField(width, height);
    public GeomVectorField middleSchools = new GeomVectorField(width, height);
    public GeomVectorField highSchools = new GeomVectorField(width, height);
    public GeomVectorField householdsField = new GeomVectorField(width, height);
    ArrayList<Household> households = new ArrayList<Household>();
    ArrayList<School> schools = new ArrayList<School>();
    ArrayList<Student> students = new ArrayList<Student>();
    HashMap<Integer, School> schoolMap = new HashMap<Integer, School>();
    HashMap<MasonGeometry, School> catchmentSchoolMap = new HashMap<MasonGeometry, School>();

    /**************************************************************************
     * Constructors start here
     *
     *************************************************************************/
    public SickStudentsModel(final long seed) {
        super(seed);

        readData();
        createSchools();
        }

    private void readData() {
        try {
            // read the data
            ShapeFileImporter.read(SickStudentsData.class.getResource("ES_ATTENDANCE_AREAS.shp"),
                SickStudentsData.class.getResource("ES_ATTENDANCE_AREAS.dbf"), elementarySchoolZones);
            ShapeFileImporter.read(SickStudentsData.class.getResource("MS_ATTENDANCE_AREAS.shp"),
                SickStudentsData.class.getResource("MS_ATTENDANCE_AREAS.dbf"), middleSchoolZones);
            ShapeFileImporter.read(SickStudentsData.class.getResource("HS_ATTENDANCE_AREAS.shp"),
                SickStudentsData.class.getResource("HS_ATTENDANCE_AREAS.dbf"), highSchoolZones);
            ShapeFileImporter.read(SickStudentsData.class.getResource("ElementarySchools.shp"),
                SickStudentsData.class.getResource("ElementarySchools.dbf"), elementarySchools);
            ShapeFileImporter.read(SickStudentsData.class.getResource("MiddleSchools.shp"),
                SickStudentsData.class.getResource("MiddleSchools.dbf"), middleSchools);
            ShapeFileImporter.read(SickStudentsData.class.getResource("HighSchools.shp"),
                SickStudentsData.class.getResource("HighSchools.dbf"), highSchools);

            // Make all the bounding rectangles match one another
            final Envelope MBR = elementarySchoolZones.getMBR();
            MBR.expandToInclude(middleSchoolZones.getMBR());
            MBR.expandToInclude(highSchoolZones.getMBR());
            MBR.expandToInclude(elementarySchools.getMBR());
            MBR.expandToInclude(middleSchools.getMBR());
            MBR.expandToInclude(highSchools.getMBR());

            elementarySchoolZones.setMBR(MBR);
            middleSchoolZones.setMBR(MBR);
            highSchoolZones.setMBR(MBR);
            elementarySchools.setMBR(MBR);
            middleSchools.setMBR(MBR);
            highSchools.setMBR(MBR);
            } catch (final Exception ex) {
            System.out.println("Error opening shapefile!" + ex);
            System.exit(-1);
            }
        }

    private void createSchoolsFromData(final GeomVectorField schoolField) {

        final Bag geoms = schoolField.getGeometries();
        for (int i = 0; i < geoms.numObjs; i++) {
            final MasonGeometry mg = (MasonGeometry) geoms.get(i);

            final Integer num = mg.getIntegerAttribute("SCHOOL_NUM");
            final String name = mg.getStringAttribute("SCHOOL_NAM");
            final String type = mg.getStringAttribute("SCHOOL_TYP");

            if (num != null) {
                final School s = new School(this, name, type);
                schoolMap.put(num, s);
                schools.add(s);
                }
            }
        }

    private void createSchools() {
        schools.clear();

        createSchoolsFromData(elementarySchools);
        createSchoolsFromData(middleSchools);
        createSchoolsFromData(highSchools);

        // removeUnusedSchools(); // no longer necessary since removing the unused
        // schools from the files
        }

    private void countCatchments(final GeomVectorField catchments) {
        final Bag geoms = catchments.getGeometries();
        for (int i = 0; i < geoms.numObjs; i++) {
            final MasonGeometry mg = (MasonGeometry) geoms.get(i);

            final Integer num = mg.getIntegerAttribute("SCHOOL_NUM");

            if (num != null) {
                final School s = schoolMap.get(num);
                if (s != null) {
                    s.catchmentCount++;
                    } else {
                    System.out.format("School %s not found.\n", num);
                    }
                }
            }
        }

    private void printSchools() {
        Collections.sort(schools, new Comparator<School>() {
            @Override
            public int compare(final School arg0, final School arg1) {
                return arg0.name.compareTo(arg1.name);
                }
            });
        for (final School s : schools) {
            System.out.format("Name: %s, Count: %d\n", s.name, s.catchmentCount);
            }
        }

    private Coordinate getRandomCoordinate(final Envelope bounds) {
        final double x = random.nextDouble() * bounds.getWidth() + bounds.getMinX();
        final double y = random.nextDouble() * bounds.getHeight() + bounds.getMinY();
        return new Coordinate(x, y);
        }

    private void createHouseholds() {
        // create households
        households.clear();
        householdsField.clear();
        householdsField.setMBR(highSchoolZones.getMBR());

        final Envelope bounds = highSchoolZones.getMBR();

        // this is needed because
        final Geometry exemplar = ((MasonGeometry) highSchoolZones.getGeometries().get(0)).getGeometry();

        for (int i = 0; i < numHouseholds; i++) {
            final Household h = new Household(this);
            households.add(h);
            Coordinate coord;
            do {
                coord = getRandomCoordinate(bounds);
                } while (!highSchoolZones.isCovered(coord));

            final Point pt = GeometryFactory.createPointFromInternalCoord(coord, exemplar);
            h.location = pt;
            householdsField.addGeometry(new MasonGeometry(pt));
            }
        }

    private School findAppropriateSchool(final int age, final Point location) {
        GeomVectorField zones;
        if (age < 11) {
            zones = elementarySchoolZones;
            } else if (age < 15) {
            zones = middleSchoolZones;
            } else {
            zones = highSchoolZones;
            }

        final Bag catchment = zones.getContainingObjects(location);

        if (catchment.numObjs != 1) {
            System.out.format("Error: school search (age: %d, location: %s) found %d catchments.\n", age, location,
                catchment.numObjs);
            for (int i = 0; i < catchment.numObjs; i++) {
                System.out.format("    Catchment %d: %s\n", i,
                    ((MasonGeometry) catchment.get(i)).getAttribute("SCHID_3"));
                }
            return null;
            }

        final MasonGeometry mg = (MasonGeometry) catchment.get(0);
        final Integer num = mg.getIntegerAttribute("SCHOOL_NUM");

        return schoolMap.get(num);
        }

    private void createStudents() {
        final double mu = StatsTools.calcLognormalMu(childrenPerHouseholdAve, childrenPerHouseholdStdev);
        final double sigma = StatsTools.calcLognormalSigma(childrenPerHouseholdAve, childrenPerHouseholdStdev);

        students.clear();
        for (final Household h : households) {
            // pick a random (non-zero) number of children from a lognormal distribution
            int numKids;
            do {
                numKids = (int) Math.round(StatsTools.normalToLognormal(mu, sigma, random.nextGaussian()));
                } while (numKids == 0);

            for (int j = 0; j < numKids; j++) {
                final int age = 5 + random.nextInt(14); // [5,18]
                final Student s = new Student(this, age);

                // pick a school for this child
                final School school = findAppropriateSchool(age, h.location);
                if (school != null)
                    school.students.add(s);
                else
                    System.out.println("School is null.");
                students.add(s);
                h.students.add(s);
                }
            }
        }

    /**
     * Initialize the population. The process goes as follows:
     *
     * point p = random location in overall bounding box if p is not inside one of
     * the catchment regions, repeat previous step populate household with random
     * number of children for each child in household if child is elementary school
     * age, find elementary school catchment if child is middle school age, find
     * middle school catchment if child is high school age, find high school
     * catchment lookup school associated with catchment assign child to school
     */
    private void init() {
        // reopen the schools
        for (final School s : schools)
            s.closed = false;

        if (reInitHouseholds) {
            for (final School s : schools)
                s.students.clear();
            createHouseholds();
            createStudents();
            } else // at least reset the students
            {
            for (final Student s : students) {
                s.status = Status.SUSCEPTIBLE;
                s.homebound = false;
                s.timeSinceInfected = 0;
                }
            }
        }

    private void beginInfection() {
        if (numInitialInfections > students.size()) {
            System.err.format("ERROR: Cannot infect %d students since there are only %d in the population.",
                numInitialInfections, students.size());
            return;
            }

        // let's infect some kids!
        Student s;
        for (int j = 0; j < numInitialInfections; j++) {
            do {
                s = students.get(random.nextInt(students.size()));
                } while (s.status != Status.SUSCEPTIBLE);
            s.infect();
            }
        }

    @Override
    public void start() {
        super.start();
        init();

        for (final Student s : students)
            schedule.scheduleRepeating(s, 0, 1.0);

        for (final School s : schools)
            schedule.scheduleRepeating(s, 1, 1.0);

        for (final Household h : households)
            schedule.scheduleRepeating(h, 2, 1.0);

        // at step 20, begin the infection
        schedule.scheduleOnce(20, 3, new Steppable() {
            public void step(final SimState state) {
                beginInfection();
                }
            });

        // containment strategy: if any school has an outbreak, close all the schools
        schedule.scheduleRepeating(0, 4, new Steppable() {
            public void step(final SimState state) {
                if (closeSchoolUponOutbreak || closeAllSchoolsUponOutbreak)
                    for (final School s : schools)
                        if (s.getProportionOfHomeboundStudents() > outbreakThreshold) { // if any school has an outbreak
                            s.closed = true;
                            if (closeAllSchoolsUponOutbreak) {
                                for (final School s2 : schools) // close all the schools
                                    s2.closed = true;
                                break;
                                }
                            }
                }
            });

        // check for end condition and terminate
        schedule.scheduleRepeating(0, 5, new Steppable() {
            public void step(final SimState state) {
                sCount = 0;
                iCount = 0;
                rCount = 0;
                for (final Student s : students) {
                    switch (s.status) {
                    case SUSCEPTIBLE:
                        sCount++;
                        break;
                    case INFECTED:
                        iCount++;
                        break;
                    case RECOVERED:
                        rCount++;
                        break;
                        }
                    }

                if ((iCount == 0) && (rCount > 0)) {
                    // System.out.format("Step %d. Total people infected: %d out of: %d\n",
                    // state.schedule.getSteps(), rCount, students.size());
                    System.out.format("%d, %d, %d, %d, %d, %f, %b, %b, %f, %d, %d\n",
                        schedule.getSteps(),
                        numHouseholds,
                        numInitialInfections,
                        diseaseDuration,
                        incubationPeriod,
                        diseaseTransmissionProb,
                        closeSchoolUponOutbreak,
                        closeAllSchoolsUponOutbreak,
                        outbreakThreshold,
                        rCount,
                        students.size());
                    state.kill();
                    }
                }
            });
        }

    public static void main(final String[] args) {
        doLoop(SickStudentsModel.class, args);
        System.exit(0);
        }

    }
