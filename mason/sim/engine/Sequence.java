/*
  Copyright 2006 by Sean Luke and George Mason University
  Licensed under the Academic Free License version 3.0
  See the file "LICENSE" for more information
*/
package sim.engine;
import java.util.*;
import sim.util.*;



/**
 *
 * Sequence is Steppable which holds an array of Steppables.  When it is stepped,
 * Sequence steps each of its subsidiary Steppables in turn.
 *
 * <p>You provide Sequence
 * with a Collection of Steppables, or with an array of Steppables, via its constructor.
 * It then copies the Collection or array into its own internal array and uses that whenever
 * you step it.
 *
 * <p>You can also modify the Steppables after the fact, in one of three ways.  First, you
 * can provide a new Collection or array to replace the internal array it is presently using, via the
 * method replaceSteppables(...).  Second, you can provide a collection of Steppables to be
 * removed from the internal array, via the methods removeSteppable(...) or removeSteppables(...).
 * Third, you can provide a collection of Steppables to be added to the internal array, via 
 * the methods addSteppable(...) or addSteppables(...).  Sequence delays performing these actions 
 * until step(...) is called on it.  At which time it first replaces the Steppabes with those provided
 * by replaceSteppables(...), then removes any Steppables requested, then finally adds any Steppables
 * requested.  It then steps all the Steppables in the resulting internal array.
 *
 * <p>By default, after removing Steppables from the internal array, Sequence does not guarantee
 * that the remaining Steppables will still be in the same order.  It does this for speed.  If you
 * want to force them to be in the same order, you can call setEnsuresOrder(true).  Note that
 * even if the array has a consistent order internally, subclasses are free to ignore this: for
 * example, RandomSequence and ParallelSequence do not keep the order consistent.
 *
 * <p>Removing Steppables is costly: the Sequence has to hunt through its array to find the ones
 * you've asked to be removed, and that's O(n) per Steppable to remove.  If you are often removing
 * a fair number of Steppables (perhaps more than 5 at a time), Sequence provides a further option
 * which results in O(1) removal: using an internal Set.  The procedure is as follows: all the current
 * Steppables, or the ones to relace them, are maintained in a LinkedHashSet.  The Steppables to 
 * be removed are removed from the Set (O(1) per Steppable).  Steppables to be added are then added
 * to the Set.  Finally the Set is dumped to an array, which is then Stepped.
 * 
 * <p>To turn on this option, call setUsesSets(true).
 *
 * <p>This approach is dramatically faster than the default approach when a large number of Steppables
 * are in the Sequence and at least a moderate number (5 or greater typically) is removed at a time.
 * It has three disadvantages however.   First, it is slower when the number of Steppables is very
 * small, or when the number of Steppables removed is small (less than 5 perhaps).  Second, because
 * a Set is used, the Steppables in the Sequence must be unique: you cannot insert the same Steppable
 * multiple times in the array.  Third, using sets does not ensure order regardless of what you stated in 
 * setEnsuresOrder(...).
 *
 * @author Mark Coletti
 * @author Sean Luke
 * 
 */
 
public class Sequence implements Steppable
    {
    private static final long serialVersionUID = 1L;

    /** The internal Steppables to be stepped.  Only steps[0...size-1] are valid.
        This array will be populated after loadSteps() is called. */
    protected Steppable[] steps;
    
    /** The number of actual Steppables in the steps array. */
    protected int size;

    // Loaded up and used temporarily to remove and add elements from the steps[] array
    // If you're using steps.
    LinkedHashSet stepsHash = null;

    // Collection of Steppables to remove from steps array
    Bag toBeRemoved = new Bag();

    // Collection of Steppables to add to steps array
    Bag toBeAdded = new Bag();

    // Collection of Steppables to replace the steps array with
    Steppable[] toReplace = null;
    
    // True if the order is maintained when removing Stepables
    boolean ensuresOrder = false;
    
    public Sequence(Steppable[] steps)
        {
        this.steps = (Steppable[])(steps.clone());
        size = steps.length;
        }
        
    public Sequence(Collection collection)
        {
        steps = new Steppable[collection.size()];
        steps = (Steppable[])(collection.toArray(steps));
        }

    /** Returns whether the order among the remaining Steppables in the internal array is maintained after removing
        Steppables via removeSteppable() or removeSteppables().  Note that this value may be entirely ignored
        by subclasses for which maintaining order doesn't make sense (such as parallel or random sequences).  Also
        if you use sets (via setUsesSets(true)), then order is never ensured regardless. */
    public boolean getEnsuresOrder() { return ensuresOrder; }

    /** Sets whether the order among the remaining Steppables in the internal array is maintained after removing
        Steppables via removeSteppable() or removeSteppables().  Note that this value may be entirely ignored
        by subclasses for which maintaining order doesn't make sense (such as parallel or random sequences).  Also
        if you use sets (via setUsesSets(true)), then order is never ensured regardless. */
    public void setEnsuresOrder(boolean val) { ensuresOrder = val; }
    
    /** If your subclass does not respect order, override this method to return
        false, and Sequence will ignore the ensuresOrder result. */
    protected boolean canEnsureOrder() { return true; }

    /** Returns whether the Sequence uses a Set internally to manage the internal array.  
        This is faster, often much faster, for large numbers of removals (perhaps
        more than 5 or so), but requires that each Steppable in the internal array be unique.  */
    public boolean getUsesSets() { return stepsHash != null; }

    /** Sets whether the Sequence uses a Set internally to manage the internal array.  
        This is faster, often much faster, for large numbers of removals (perhaps
        more than 5 or so), but requires that each Steppable in the internal array be unique.  */
    public void setUsesSets(boolean val) 
        { 
        if (val && stepsHash == null) 
            {
            stepsHash = new LinkedHashSet();
            for(int i = 0; i < size; i++)
                if (!stepsHash.add(steps[i]))
                    throw new RuntimeException("This Sequence is set up to use Sets, but duplicate Steppables were added to the sequence, which is not permitted in this mode.");
            }
        else if (!val && stepsHash != null)
            {
            stepsHash = null; 
            }
        }

    // Internal version of loadSteps() which uses sets instead of scanning through the array directly
    void loadStepsSet()
        {
        boolean stepsHashChanged = false;
        
        // First, replace the steppables if called for
        if (toReplace != null)
            {
            stepsHashChanged = true;
            stepsHash.clear();
            for(int i = 0; i < toReplace.length; i++)
                if (!stepsHash.add(toReplace[i]))
                    throw new RuntimeException("This Sequence is set up to use Sets, but duplicate Steppables were added to the sequence, which is not permitted in this mode.");
            size = toReplace.length;
            toReplace = null;
            }
    
        // Remove steppables
        int toBeRemovedSize = this.toBeRemoved.size();
        if (toBeRemovedSize > 0)
            {
            stepsHashChanged = true;
            for(int i = 0; i < toBeRemovedSize; i++)
                {
                stepsHash.remove(toBeRemoved.get(i));
                }
            toBeRemoved.clear();
            }
        
        // add in new steppables
        int toBeAddedSize = this.toBeAdded.size();
        if (toBeAddedSize > 0)
            {
            stepsHashChanged = true;
            for(int i = 0; i < toBeAddedSize; i++)
                {
                if (!stepsHash.add(toBeAdded.get(i)))
                    // throw new RuntimeException("This Sequence is set up to use Sets, but duplicate Steppables were added to the sequence, which is not permitted in this mode.");
                    { } // do nohing
                }
            toBeAdded.clear();
            }

        // copy over set
        if (stepsHashChanged)
            {
            if (steps == null)
                steps = new Steppable[stepsHash.size()];
            steps = (Steppable[]) (stepsHash.toArray(steps));
            size = steps.length;
            }
        }
        

    /** Subclasses should call this method as more or less the first thing in their step(...) method.
        This method replaces, removes, and adds new Steppables to the internal array as directed by the
        user.  After calling this method, the Sequence is ready to have the Steppables in its internal
        array stepped. */
    protected void loadSteps()
        {
        if (stepsHash != null)
            {
            loadStepsSet();
            return;
            }
        
        // First, replace the steppables if called for
        if (toReplace != null)
            {
            steps = toReplace;
            size = steps.length;
            toReplace = null;
            }
    
        // Remove steppables
        int toBeRemovedSize = toBeRemoved.size();
        if (toBeRemovedSize > 0)
            {
            boolean ensuresOrder = this.ensuresOrder && canEnsureOrder(); 
            Steppable[] steps = this.steps;
            Bag toBeRemoved = this.toBeRemoved;
            int stepsSize = this.size;
            
            for (int s = stepsSize - 1; s >= 0; s--)
                {
                for (int r = 0; r < toBeRemovedSize; r++)
                    {
                    if (steps[s] == toBeRemoved.get(r))
                        {
                        if (s < stepsSize -1)  // I'm not already at top
                            {
                            // remove from steps, possibly nondestructively
                            if (ensuresOrder)
                                System.arraycopy(steps, s + 1, steps, s, stepsSize - s - 1);
                            else
                                steps[s] = steps[stepsSize - 1];
                            }
                        // else we don't bother moving me

                        steps[stepsSize - 1] = null;  // let top element GC
                        stepsSize--;

                        // remove from toBeRemoved, always destructively
                        toBeRemoved.remove(r);
                        toBeRemovedSize--;

                        break;  // all done
                        }
                    }

                if (toBeRemovedSize == 0)      // nothing left
                    {
                    break;
                    }
                }

            // finish up
            toBeRemoved.clear();
            this.size = stepsSize;
            }


        // add in new steppables
        int toBeAddedSize = this.toBeAdded.size();
        if (toBeAddedSize > 0)
            {
            // extend steppables
            Bag toBeAdded = this.toBeAdded;
            int stepsSize = this.size;
            int newLen = stepsSize + toBeAddedSize;
            if (newLen >= steps.length)
                {
                int newSize = steps.length * 2 + 1;
                if (newSize <= newLen) newSize = newLen;
                Steppable[] newSteppables = new Steppable[newSize];
                System.arraycopy(steps, 0, newSteppables, 0, steps.length);
                this.steps = newSteppables;
                steps = newSteppables;
                }
            
            // copy in new elements
            if (toBeAddedSize < 20)
                for(int i = 0; i < toBeAddedSize; i++)
                    steps[stepsSize + i] = (Steppable)(toBeAdded.get(i));
            else
                toBeAdded.copyIntoArray(0, steps, stepsSize, toBeAddedSize);


            // finish up
            toBeAdded.clear();            
            this.size = newLen;
            }
        }


    /** Requests that the provided Steppables replace the existing Steppables in the internal array prior to the next step() call. */
    public void replaceSteppables(Collection collection)
        {
        if (toReplace == null)
            toReplace = new Steppable[collection.size()];
        toReplace = (Steppable[])(collection.toArray(toReplace));
        }

    /** Requests that the provided Steppables replace the existing Steppables in the internal array prior to the next step() call. */
    public void replaceSteppables(Steppable[] steppables)
        {
        toReplace = (Steppable[])(steppables.clone());
        }

    /** Requests that the provided Steppable be added to the Sequence prior to the next step() call. */
    public void addSteppable(Steppable steppable)
        {
        toBeAdded.add(steppable);
        }

    /** Requests that the provided Steppables be added to the Sequence prior to the next step() call. */
    public void addSteppables(Steppable[] steppables)
        {
        toBeAdded.addAll(steppables);
        }

    /** Requests that the provided Steppables be added to the Sequence prior to the next step() call. */
    public void addSteppables(Collection steppables)
        {
        toBeAdded.addAll(steppables);
        }

    /** Requests that the provided Steppable be removed from the Sequence prior to the next step() call. */
    public void removeSteppable(Steppable steppable)
        {
        toBeRemoved.add(steppable);
        }

    /** Requests that the provided Steppables be removed from the Sequence prior to the next step() call. */
    public void removeSteppables(Steppable[] steppables)
        {
        toBeRemoved.addAll(steppables);
        }

    /** Requests that the provided Steppables be removed from the Sequence prior to the next step() call. */
    public void removeSteppables(Collection steppables)
        {
        toBeRemoved.addAll(steppables);
        }

    public void step(SimState state)
        {
        loadSteps();

        int stepsSize = this.size;
        Steppable[] steps = this.steps;
        
        for(int x=0;x<stepsSize;x++)
            {
            if (steps[x]!=null) 
                {
                steps[x].step(state);
                }
            }
        }

    }
