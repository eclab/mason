package sim.app.geo.masoncsc.submodel;

import java.util.HashSet;

import sim.engine.Schedule;
import sim.engine.SimState;
import sim.engine.Steppable;
import sim.util.Bag;


public class MetaSchedule extends Schedule
    {
    private static final long serialVersionUID = 1;

    Bag states = new Bag();

    /** Creates a Schedule. */
    public MetaSchedule() { }
    
    public MetaSchedule(SimState[] states, int[] orderings)
        {
        if (states.length != orderings.length)
            throw new RuntimeException("Error: arrays must be the same length.");
                
        for(int i = 0; i < states.length; i++)
            addSimState(states[i], orderings[i]);
        }
    
    public MetaSchedule(SimState[] states)
        {
        for(int i = 0; i < states.length; i++)
            addSimState(states[i], i);
        }
    
    public double getTime()
        {
        synchronized(lock)
            {
            double minTime = AFTER_SIMULATION;
            for(int i = 0 ; i < states.size(); i++)
                {
                double scheduleStateTime = ((ScheduleStep)(states.get(i))).getScheduleTime();
                if (scheduleStateTime < minTime)
                    minTime = scheduleStateTime;
                }
            return minTime;
            }
        }

//    protected void pushToAfterSimulation()
//        {
//        synchronized(lock)
//            {
//            super.pushToAfterSimulation();
//            for(int i = 0 ; i < states.size(); i++)
//                ((ScheduleStep)(states.get(i))).scheduleState.schedule.pushToAfterSimulation();
//            }
//        }

    public void clear()
        {
        synchronized(lock)
            {
            super.clear();
            for(int i = 0 ; i < states.size(); i++)
                ((ScheduleStep)(states.get(i))).scheduleState.schedule.clear();
            }
        }

    public void reset()
        {
        synchronized(lock)
            {
            super.reset();
            for(int i = 0 ; i < states.size(); i++)
                ((ScheduleStep)(states.get(i))).scheduleState.schedule.reset();
            }
        }
    
    public boolean scheduleComplete()
        {
        synchronized(lock)
            {
            for(int i = 0 ; i < states.size(); i++)
                if (((ScheduleStep)(states.get(i))).scheduleState.schedule.scheduleComplete())
                    return true;
            return false;
            }
        }


    Bag subStates = new Bag();
    boolean inStep = false;
    
    public synchronized boolean step(final SimState state)
        {
        if (inStep)  // check for reentrant calls and deny
            {
            throw new RuntimeException("Schedule.step() is not reentrant, yet is being called recursively.");
            }
            
        inStep = true;
        
        ScheduleStep best = null;
        double bestTime = 0;
        int bestOrdering = 0;
        
        synchronized(lock)
            {
            if (states.size() == 0) // oops
                { inStep = false; return false; }
            subStates.clear();
                
            // grab the minimum time
            for(int i = 0; i < states.size(); i++)
                {
                ScheduleStep candidate = (ScheduleStep)(states.get(i));
                int candidateOrdering = candidate.getScheduleOrdering();
                double candidateTime = candidate.getScheduleTime();
                if (best == null ||
                        candidateTime < bestTime ||
                        (candidateTime == bestTime && candidateOrdering < bestOrdering))
                    {
                    best = candidate;
                    bestTime = candidateTime;
                    bestOrdering = candidateOrdering; 
                    }
                }
                
            // grab all minimum guys time-wise
            for(int i = 0; i < states.size(); i++)
                {
                ScheduleStep candidate = (ScheduleStep)(states.get(i));
                int candidateOrdering = candidate.getScheduleOrdering();
                double candidateTime = candidate.getScheduleTime();
                if (candidateTime == bestTime)
                    subStates.add(candidate);
                }
                
            if (subStates.size() > 1)
                {
                // next shuffle
                subStates.shuffle(state.random);
                
                // last sort
                subStates.sort();
                }
            }
            
        // step outside of the lock so SOMEBODY can reinsert himself if he so pleases
        for(int i = 0; i < subStates.size(); i++)
            ((ScheduleStep)subStates.get(i)).step(state);
            
        // reacquire lock and increment steps
        synchronized(lock) { steps++; }
        inStep = false;
        return true;
        }

    HashSet<SimState> statesSet = new HashSet<SimState>();
    public boolean addSimState(SimState state, int ordering)
        {
        synchronized(lock)
            {
            if (statesSet.contains(state)) return false;
            statesSet.add(state);
            ScheduleStep step = new ScheduleStep(state, ordering);
            states.add(step);
            }
        return true;
        }
        
    public boolean removeSimState(SimState state)
        {
        synchronized(lock)
            {
            if (!statesSet.contains(state)) return false;
            statesSet.remove(state);
            for(int i = 0; i < states.size(); i++)
                if (((ScheduleStep)(states.get(i))).getScheduleState() == state)
                    {
                    states.remove(i);
                    return true;
                    }
            return false;
            }
        }

    class ScheduleStep implements Steppable, Comparable
        {
        SimState scheduleState;
        int scheduleOrdering;
        
        public SimState getScheduleState() { return scheduleState; }
        public int getScheduleOrdering() { return scheduleOrdering; }
        public double getScheduleTime() { return scheduleState.schedule.getTime(); }
        
        public int compareTo(Object obj)
            {
            ScheduleStep s1 = this;
            ScheduleStep s2 = (ScheduleStep)obj;
            double t1 = s1.getScheduleTime();
            double t2 = s2.getScheduleTime();
            if (t1 < t2) return -1;
            if (t1 > t2) return 1;
            
            // times are equal, so we break ties with ordering
            int ord1 = s1.getScheduleOrdering();
            int ord2 = s2.getScheduleOrdering();
            if (ord1 < ord2) return -1;
            if (ord1 > ord2) return 1;
            
            // everything is equal
            return 0;
            }

        public ScheduleStep(SimState state, int ordering)
            {
            scheduleState = state;
            scheduleOrdering = ordering;
            }
            
        public synchronized void step(final SimState state)
            {
            scheduleState.schedule.step(scheduleState);
            }
            
        public String toString() { return "ScheduleStep[" + scheduleState + ", " + scheduleOrdering + "]"; }
        }
    }

