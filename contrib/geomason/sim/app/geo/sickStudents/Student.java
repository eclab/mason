/**
 ** Student.java
 **
 ** Copyright 2011 by Joseph Harrison, Mark Coletti, Cristina Metgher, Andrew Crooks
 ** George Mason University.
 **
 ** Licensed under the Academic Free License version 3.0
 **
 ** See the file "LICENSE" for more information
 **
 ** $Id$
 **/
package sickStudents;

import sim.engine.SimState;
import sim.engine.Steppable;



@SuppressWarnings("serial")
public class Student implements Steppable
{

    private SickStudentsModel model;
    public Status status = Status.SUSCEPTIBLE;
    public int timeSinceInfected = 0;
    public boolean homebound = false;
    public int age;



    public Student(SickStudentsModel model, int age)
    {
        this.model = model;
        this.age = age;
    }



    public void infect()
    {
        status = Status.INFECTED;
        this.timeSinceInfected = 0;
    }



    public void infect(int timeSinceInfected)
    {
        status = Status.INFECTED;
        this.timeSinceInfected = timeSinceInfected;
    }



    public void expose()
    {
        if (status != Status.SUSCEPTIBLE)
        {
            return;
        }

        double val = model.random.nextDouble();
        if (val <= model.diseaseTransmissionProb)
        {
            infect();
        }
    }



    @Override
    public void step(SimState state)
    {
        switch (status)
        {
            case SUSCEPTIBLE:
                break;		// do nothing

            case INFECTED:
                timeSinceInfected++;
                if (timeSinceInfected >= model.incubationPeriod)
                {
                    homebound = true;
                }
                if (timeSinceInfected > model.diseaseDuration)
                {
                    status = Status.RECOVERED;
                }
                break;

            case RECOVERED:
                homebound = false;
                timeSinceInfected++;	// just for record-keeping
                break;
        }
    }

}
