/*
  Copyright 2006 by Sean Luke and George Mason University
  Licensed under the Academic Free License version 3.0
  See the file "LICENSE" for more information
*/

package sim.app.wcss.tutorial14;
import sim.engine.*;
import sim.util.*;
import sim.field.continuous.*;
import sim.field.network.*;

public class Students extends SimState
    {
    private static final long serialVersionUID = 1;

    public Continuous2D yard = new Continuous2D(1.0,100,100);
    public Continuous3D agitatedYard = new Continuous3D(1.0, 100, 100, 100);
    
    public double TEMPERING_CUT_DOWN = 0.99;
    public double TEMPERING_INITIAL_RANDOM_MULTIPLIER = 10.0;
    public boolean tempering = true;
    public boolean isTempering() { return tempering; }
    public void setTempering(boolean val) { tempering = val; }
        
    public int numStudents = 50;

    double forceToSchoolMultiplier = 0.01;
    double randomMultiplier = 0.1;

    public int getNumStudents() { return numStudents; }
    public void setNumStudents(int val) { if (val > 0) numStudents = val; }

    public double getForceToSchoolMultiplier() { return forceToSchoolMultiplier; }
    public void setForceToSchoolMultiplier(double val) { if (forceToSchoolMultiplier >= 0.0) forceToSchoolMultiplier = val; }

    public double getRandomMultiplier() { return randomMultiplier; }
    public void setRandomMultiplier(double val) { if (randomMultiplier >= 0.0) randomMultiplier = val; }
    public Object domRandomMultiplier() { return new sim.util.Interval(0.0, 100.0); }

    public double[] getAgitationDistribution()
        {
        Bag students = buddies.getAllNodes();
        double[] distro = new double[students.numObjs];
        int len = students.size();
        for(int i = 0; i < len; i++)
            distro[i] = ((Student)(students.get(i))).getAgitation();
        return distro;
        }

    public Network buddies = new Network(false);

    public Students(long seed)
        {
        super(seed);
        }
                
    public void load3DStudents()
        {
        Bag students = buddies.getAllNodes();
        for(int i = 0; i < students.size(); i++)
            {
            Student student = (Student)(students.get(i));
            Double2D loc = (Double2D)(yard.getObjectLocation(student));
            // we multiply by 5 in order to scale the agitation roughly with the student dispersion
            // in the other two dimensions
            agitatedYard.setObjectLocation(student, new Double3D(loc, student.getAgitation() * 5.0));
            }
        }

    public void start()
        {
        super.start();
                
        // add the tempering agent
        if (tempering)
            {
            randomMultiplier = TEMPERING_INITIAL_RANDOM_MULTIPLIER;
            schedule.scheduleRepeating(schedule.EPOCH, 1, new Steppable() 
                { public void step(SimState state) { if (tempering) randomMultiplier *= TEMPERING_CUT_DOWN; } });
            }
                
        // clear the yard
        yard.clear();

        // clear the buddies
        buddies.clear();
        
        agitatedYard.clear();
                
        // add some students to the yard
        for(int i = 0; i < numStudents; i++)
            {
            Student student = new Student();
            yard.setObjectLocation(student, 
                new Double2D(yard.getWidth() * 0.5 + random.nextDouble() - 0.5,
                    yard.getHeight() * 0.5 + random.nextDouble() - 0.5));

            buddies.addNode(student);
            schedule.scheduleRepeating(student);
            Steppable steppable = new Steppable() 
                {
                public void step(SimState state) { load3DStudents(); }
                };
            schedule.scheduleRepeating(schedule.EPOCH, 2, steppable);
            }
        
        // define like/dislike relationships
        Bag students = buddies.getAllNodes();
        for(int i = 0; i < students.size(); i++)
            {
            Object student = students.get(i);
            
            // who does he like?
            Object studentB = null;
            do
                {
                studentB = students.get(random.nextInt(students.numObjs));
                } while (student == studentB);
            double buddiness = random.nextDouble();
            buddies.addEdge(student, studentB, new Double(buddiness));

            // who does he dislike?
            do
                {
                studentB = students.get(random.nextInt(students.numObjs));
                } while (student == studentB);
            buddiness = random.nextDouble();
            buddies.addEdge(student, studentB, new Double( -buddiness));
            }
                        
        load3DStudents();
        }
        
    public static void main(String[] args)
        {
        doLoop(Students.class, args);
        System.exit(0);
        }    
    }
