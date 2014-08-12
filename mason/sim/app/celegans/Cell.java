/*
  Copyright 2006 by Sean Luke and George Mason University
  Licensed under the Academic Free License version 3.0
  See the file "LICENSE" for more information
*/

package sim.app.celegans;

import java.util.ArrayList;
import java.util.HashMap;
import java.lang.System;
import sim.util.*;
import sim.engine.*;
import sim.field.continuous.*;

/*
  Cell.java
  Sean Luke
  University of Maryland, Washington D.C., and
  Sony Computer Science Laboratory, Tokyo
  seanl@cs.umd.edu
*/

/**
   Cell contains the basic information about a particular cell as loaded from the
   <a href="http://probe.nalusda.gov:8300/cgi-bin/browse/acedb">ACeDB database on
   C. elegans.</a>  The ACeDB database has a lot of bugs, so some of
   this data has been hacked; you have been warned.  Cell info is not based on any
   fancy modelling techniques of cell kinematics, dynamics, or even more reasonable
   interpolation methods like spline interpolation.  All it is is a faithful
   reproduction of the read data, with simple linear interpolation between spatial
   data points, using simple spheres (in the Java3D version) or points representing
   supposedly the "center" (or nucleus?) of each cell.

   <p>Hack highlights:

   <ul>
   <li>
   A number of cells don't have any location information.  Most notably, many
   of the early cells (like P0', the zygote) have no location info at all.  This
   data is hand-hacked for them, and badly too!

   <li>
   Some cells have no parent.  Now we all know that the only cell for which this
   can truly be the case is P0'.  I imagine there's some data inconsistency in
   the database.  If Cell.java comes across one of these cells, it includes its
   information but never displays it (what would you do with it?)

   <li>
   Many cells have different lineage and official names.  Hence the two names
   for each.

   <li>
   A few cells split into four cells rather than into two, or at least that's
   what the database says.  So the daughters[] array isn't *always* of size 2.

   <li>
   Volume information is unknown except for the initial split from P0' to AB and
   P1', where it's hand-hacked to give AB 60% of the volume.  Other than that,
   all splits assume equal volume sharing between children.

   <li>
   Many cells are POSTEMBRYONIC, meaning that they don't appear until after
   gastrulation has begun.  Unfortunately, no cell is given any spatial information
   after gastrulation and later cell development, so we don't know where these
   postembryonic cells are supposed to be located in space.  Further, no information
   is given as to when they are born; only their lineage.  So I've set it up so
   that times -1 through 499 are the embryo's "preembryonic period", and times from
   500 through 999 are the embryo's "postembryonic period".  Postembryonic cells are
   born one tick after their parent is born, or at time 500, whichever is later.  
   The position of postembryonic cells (they've got to be displayed *somewhere*)
   is based on simple cubic splitting of their parent's space and volume, so you'll
   see clumps of cells together as a result.  We had to do *something*!

   <li>
   The database indicates that some cells are fated to die.  Unfortunately, the
   database doesn't include this as a fate, but as a remark (ergh).  Furthermore,
   the data doesn't say WHEN the cell dies.  We assume here that such a cell dies
   as soon as it's born.

   <li>
   The database has two different data reconstructions.  We use the N2-EMB-1
   reconstruction.  Furthermore, the birthdays in the reconstruction don't jibe with
   the embryo division time of parents.  ARGH!!!  We assume the birthdays are
   correct, falling back on the embryo division time only when the birthdays aren't
   available.

   <li>
   New cells are assumed to start at their parent's last known position.  This is
   a suboptimal decision.

   <li>
   Some cells have dual origins.  This means that some cells X and Y might, depending
   on various conditions, assume different roles (and be given different names).  Thus
   X might "become" cell A and Y might "become" cell B, or X might become B and Y 
   might become A.  There are only a few cells which can trade lineage roles like this.
   The way we handle this in the visualizer is by assuming that X, Y, A, and B are
   different cells (they aren't of course), then picking a parentage (X is A's "parent",
   Y is B's parent, say) arbitrarily.  Of course, X isn't *really* A's parent, as it
   X and A are the same cell (or in other animals, X and B are the same cell).  But it
   simplifies the visualization issues.

   <li>
   Remarks are included, but not journals.  We don't have journal data unfortunately. 

   <li>
   Fates, cell groups, and expression patterns are included as far as we have
   information.  The information is VERY VERY sparse, missing a lot of key facts;
   hopefully people will fill in more details on each cell.  Cell death is 
   considered a "fate" even though it's listed in the remarks.

   <li>
   When cells split they are assumed to divide volume evenly among their children.
   We assume this because we have no other data to tell us otherwise; the one
   exception is the famous example where P0' splits to form AB and P1' (AB gets
   60% of the volume, right?).  We've hand-hacked this one exception.

   </ul>

   Cells have a lot of data, both raw and processed.  See the Cell.java code directly for comments on what this data all means.
*/




public class Cell extends Object implements Steppable
    {
    private static final long serialVersionUID = 1;

    /* Processed Information */
    public Cell parent;         // my parent in the cell lineage, else null

    public Cell getParent() { return parent; }

    public int expressionPattern;
    final static String[] expressionPatterns = new String[] { "", "Expr8", "Expr12", "Expr15", "Expr21", "Expr24", "Expr28", "Expr29", "Expr35", "Expr38", "Expr29", "Expr49", "Expr56", "Expr67", "Expr68" };
    public String getExpressionPattern() { return expressionPatterns[expressionPattern]; }

    public int cellGroup;
    final static String[] cellGroups = new String[] { "", "GLR", "e1", "e2", "gon_herm_anch", "gon_herm_dish_A", "gon_herm_dish_P", "gon_herm_dut", "gon_herm_prsh_A", "gon_herm_prsh_P", "gon_herm_spth_A", "gon_herm_spth_P", "gon_herm_sujn_A", "gon_herm_sujn_P", "gon_herm_vut", "hyp10", "hyp3", "hyp4", "hyp5", "hyp6", "hyp7", "hyp8/9", "int_emb", "int_post", "m2", "m4", "m6", "m7", "mu_bod", "rectal epithelium", "se_herm", "seam", "um1", "um2", "vm1", "vm2", "vulvaA", "vulvaB", "vulvaC", "vulvaD", "vulvaE", "vulvaF" };
    public String cellGroup() { return cellGroups[cellGroup]; }

    public int fate;
    final static String[] fates = new String[] { "", "Dies", "Muscle", "Hypodermis", "Intestine", "Neuron", "Pharynx" };
    public String getFate() { return fates[fate]; }
    boolean isNeuron() { return fate == 5; }

    public int type;            // what kind of cell am I?  Choose from:
    // no-info founding,embryonic,
    // postembryonic,postembryonic dual origin

    final static String[] types = new String[] { "Founding / Unknown", "Preembryonic", "Postembryonic", "Postembryonic Dual Origin", "Postembryonic Unknown" };
    public String getType() { return types[type]; }
        

    public Cell[] daughters;    // The cells that I split into.  Yes Virginia,
    // there are a few cases where it's not just 2.
    // Also, some cells have dual origins (AVFL for
    // example), meaning that they can have
    // originally been one of two cells.  For
    // example, depending on the situation, 
    // Either W.aaa or P1.aaa can "become" AVFL.
    // For these situations, we arbitrarily pick
    // one of the two to be the "parent" of the
    // cell.  Of course, no cell-splitting is
    // involved--it's just a convenience to
    // maintain a tree structure rather than
    // a graph.
        
    public Cell[] getDaughters() 
        { 
        return daughters;
        }

    public int num_children;    // The size of daughters[]
        
    public int getNumChildren() { return num_children; }

    public int birthday;        // The time I was born

    public int getBirthday() { return birthday; }

    public int death_day;       // The time I die, or am split.
    // Some cells are "fated" to die, though
    // this information is oddly only in the data's
    // remarks.  These cells have a death day
    // that's equal to their birthday.

    public int getDeathday() { return death_day; }

    public String official_name = "";// My "official" name

    public String getName() { return official_name; }

    public String lineage_name = ""; // My lineage name -- sometimes different

    public String getLineageName() { return lineage_name; }

    public String remark = "";  // remarks
    public String getRemark() { return remark; }

    public double location_x[];     // Locations (x, y, z, at time t)
    public double location_y[];
    public double location_z[];
    public double location_t[];

    public int location_size;       // The size of the location_foo[] arrays

    public ArrayList synapses;  // how many synapse connections I have

    public ArrayList getSynapses() { return synapses; }

    public float getRadius() { return radius; }

    public float radius;        // My cell radius, assuming I'm spherical
    // (yeah, yeah, big assumption).  My volume
    // is of course my radius cubed
    
    
    /* Data used for processing the above */
    public double split_radius_distance[];
    public Cell[] equivalence_origin;
    public Cell[] equivalence_fate;
    public int num_equivalence_fate;
    public int num_equivalence_origin;
    public double embryo_division_time;
    public double time_born;
    public int location_max;

    /* Constants */
    public static final int initial_location_size=4;
    public static final double initial_split_radius_distance=2.0;
    public static final int post_embryonic_birthday = 500;
    public static final int maximum_death_day=1000;
    public static final char cell_type_preembryonic_unknown_position = 0;
    public static final char cell_type_preembryonic = 1;
    public static final char cell_type_postembryonic = 2;
    public static final char cell_type_postembryonic_dual_origin = 3;
    public static final char cell_type_postembryonic_unknown_position = 4;


    /* Functions */

    public Cell()
        {
        radius=1.0f;
        synapses = new ArrayList(0);
        daughters=new Cell[2];
        equivalence_origin=new Cell[2];
        equivalence_fate=new Cell[2];
        embryo_division_time=-1.0;
        time_born=-1.0;
        num_children=0;
        location_max=0;
        split_radius_distance=new double[3];
        split_radius_distance[0]=split_radius_distance[1]=
            split_radius_distance[2]=initial_split_radius_distance;
        }


    /** Adds a Synapse, but only if one between the cells in question doesn't already exist. */

    public void addSynapse(Synapse s)
        {
        /* adds only if a synapse of that type and direction is not available yet. */
        for(int x=0;x<synapses.size();x++)
            {
            Synapse test=(Synapse)(synapses.get(x));
            if (s.from==test.from && s.to==test.to &&
                s.type==test.type && s.type==Synapse.type_chemical)
                /* chemical synapse conditions */
                {  return; }
            else if ((s.from==test.from && s.to==test.to &&  s.type==Synapse.type_gap) ||
                (s.from==test.to && s.to==test.from && s.type==Synapse.type_gap))
                /* gap junction conditions */
                {  return; }
            }
        /* At this point, we know that we don't have the synapse already included */
        synapses.add(s);
        }



    /** Sets the volume of the Cell.  This has been hacked so that AB and P1' get .6 and .4 respectively, all root cells get 1.0, and all other cells split the volume of their parent up among their siblings.
     */

    public void setVolume(float vol)
        {
        radius=(float)Math.pow(vol,0.333333333333f);
        
        // we hack it here to allow P0 to distribute to AB and P1' properly
        if (official_name.equalsIgnoreCase("P0"))
            {
            for(int x=0;x<num_children;x++)
                {
                // we assume that unless we have information to the contrary, volume
                // is split evenly
                
                if (daughters[x].official_name.equalsIgnoreCase("AB"))
                    daughters[x].setVolume(vol * .6f);
                else daughters[x].setVolume(vol * .4f);
                }
            }
        else
            {
            for(int x=0;x<num_children;x++)
                {
                // we assume that unless we have information to the contrary, volume
                // is split evenly
                
                daughters[x].setVolume(vol/(float)num_children);
                }
            }
        }


    
    /**  Returns the radius of the cell.  This isn't exactly computable from the
         volume-- there's some hacking going on.  
         Cells start out in their parent's position, but that means that when a cell
         dies and its children are formed it looks like the cell has "shrunk" suddenly.
         To make this look more lifelike, the rule we'll have for current cell
         volume is: 
         <ol>
         <li> if the timestamp is out of range, or I have only one location
         (my figured birth location), then my radius is radius.

         <li> otherwise, I start out with my parent's radius and slowly degrade
         to my true radius by the time of my first non-birth location
         timestamp.
         </ol>
         <p>...it's a hack, but it looks better.
    */

    public float getRadius(int timestamp)
        {
        if (timestamp >= birthday && timestamp < death_day && location_size > 1 && parent!=null)
            {
            if (location_t[1]<location_t[0]) { System.out.println("Uh oh"); return parent.radius; }
            if (location_t[1]==location_t[0]) { return parent.radius; }
            if (timestamp < location_t[1])
                return (float) (radius + (parent.radius - radius) * (1.0 - (timestamp-location_t[0])/(location_t[1]-location_t[0])));
            // assumes locations are sorted!
            }
        return radius;  // what the heck
        }


    /** returns true if cell is live at this time, and puts in xyz the coordinates of the cell.  This method assumes that the location array has been sorted already. */


    public boolean getLocation(int timestamp, double[] xyz)
        {
        if (timestamp >= birthday && timestamp < death_day)
            {
            if (location_size==0)  // shouldn't ever happen
                {
                System.out.println("No location information for cell" + official_name);
                return false;
                }
            else
                {
                if (location_t[0] > timestamp)  // hmmm...shouldn't ever happen
                    {                   
                    xyz[0]=location_x[0]; xyz[1]=location_y[0]; xyz[2]=location_z[0];
                    return true;
                    }
                for(int x=0;x<location_size;x++)
                    {
                    if (location_t[x]==timestamp)
                        {
                        /* right on the nose! */
                        xyz[0]=location_x[x]; xyz[1]=location_y[x]; xyz[2]=location_z[x];
                        return true;
                        }
                    else if (x==location_size-1)  /* last one */
                        {
                        xyz[0]=location_x[x]; xyz[1]=location_y[x]; xyz[2]=location_z[x];
                        return true;
                        }
                    else if (location_t[x]<timestamp && location_t[x+1]>timestamp)
                        {
                        /* interpolate */
                        double p = (timestamp-location_t[x]) / (location_t[x+1]-location_t[x]);
                        xyz[0]=location_x[x]*(1.0-p)+location_x[x+1]*p;
                        xyz[1]=location_y[x]*(1.0-p)+location_y[x+1]*p;
                        xyz[2]=location_z[x]*(1.0-p)+location_z[x+1]*p;
                        return true;
                        }
                    }
                }
            return true;  /* shouldn't ever reach here */
            }
        else return false;
        }


    /** Sorts the location array for this cell. */
    public void sortLocation()
        {
        // a simple insertion sort -- we don't have enough locations to justify
        // a quicksort's overhead.
        for(int x=0;x<location_size;x++)
            {
            for(int y=x+1;y<location_size;y++)
                {
                if (location_t[y] < location_t[x])
                    // swap
                    {
                    double swap;
                    swap=location_x[y]; location_x[y]=location_x[x]; location_x[x]=swap;
                    swap=location_y[y]; location_y[y]=location_y[x]; location_y[x]=swap;
                    swap=location_z[y]; location_z[y]=location_z[x]; location_z[x]=swap;
                    swap=location_t[y]; location_t[y]=location_t[x]; location_t[x]=swap;
                    }
                else if (location_t[y]==location_t[x])
                    System.out.println("Identical Times: " + x + " " + y + " in " + official_name);
                }
            }
        }


    /** Adds a new location x,y,z at time t to the location array. */

    public void pushLocation(double x, double y, double z, double t)
        {
        if (location_max==0)
            {
            // initialize lazily
            location_x=new double[initial_location_size];
            location_y=new double[initial_location_size];
            location_z=new double[initial_location_size];
            location_t=new double[initial_location_size];
            location_max=initial_location_size;
            location_size=0;
            }
        // load data
        location_x[location_size]=x;
        location_y[location_size]=y;
        location_z[location_size]=z;
        location_t[location_size]=t;
        location_size++;
        if (location_size==location_max)
            {
            // double location_max
            double[] new_location_x=new double[location_max*2];
            double[] new_location_y=new double[location_max*2];
            double[] new_location_z=new double[location_max*2];
            double[] new_location_t=new double[location_max*2];
            System.arraycopy(location_x,0,new_location_x,0,location_max);
            System.arraycopy(location_y,0,new_location_y,0,location_max);
            System.arraycopy(location_z,0,new_location_z,0,location_max);
            System.arraycopy(location_t,0,new_location_t,0,location_max);
            location_x=new_location_x;
            location_y=new_location_y;
            location_z=new_location_z;
            location_t=new_location_t;
            location_max*=2;
            }
        }



    /** PostProcesses the cell's birthday.
        The rule is:
        <ol>
        <li> if I'm root, my birthday is -1.
        <li> if I have a time_born, that's my birthday.
        <li> else if my parent has an embryo_division time, that's my birthday.
        <li> else if my parent's birthday is greater than or equal 
        to post_embryonic_birthday,my birthday is my parent's birthday plus 1.0.
        <li> else my birthday is the post_embryonic_birthday.
        </ol>

        <p>Then add 2 to every birthday. This should make it so that cells have preembryonic cells have absolute birthdays, and postembryonic cells have relative birthdays counting from the post_embryonic_birthday (which should be larger than any preembryonic birthday). Report any situations where parents have higher birthdays than children.
    */
 
    public void postProcessBirthday(boolean root)
        {
        if (root && time_born==-1) birthday=-1;
        else if (time_born!=-1) birthday=(int)time_born;
        else if (parent.embryo_division_time!=-1) birthday=(int)parent.embryo_division_time;
        else if (parent.birthday >= post_embryonic_birthday) birthday=parent.birthday+1;
        else birthday=post_embryonic_birthday;

        /* Perform on daughters */
        for(int z=0;z<num_children;z++) daughters[z].postProcessBirthday(false);

        // add 2 AFTERWARDS
        birthday += 2;
        }
    



    /** Tweak the locations so they're properly centered, by translating them by <x,y,z>. */

    public void modifyLocations(double x, double y, double z)
        {
        for(int a=0;a<location_size;a++)
            {
            location_x[a]+=x;
            location_y[a]+=y;
            location_z[a]+=z;
            }
        /* Perform on daughters */
        for(int j=0;j<num_children;j++) daughters[j].modifyLocations(x,y,z);
        }


    /** PostProcess the death day of the cell.  The rule is:
        <ol> 
        <li> if I have children, I die when they are born.
        <!-- <li> if I don't have children, and my fate is to die, I die the day after I'm born -->
        <li> Otherwise since I have no data on when cells die, if I have no children,
        I die at maximum_death_day
        </ol>
    */

    public void postProcessDeathDay(boolean root)
        {
        // determine fate
        /*
          boolean fatedToDie=false;
          for(int x=0;x<fate.size();x++)
          if (((Fate)(fate.get(x))).name.equalsIgnoreCase("dies"))
          fatedToDie=true;
        */
        
        if (num_children!=0) death_day=daughters[0].birthday;
        // else if (fatedToDie) death_day=birthday+1;
        else death_day=maximum_death_day;
        /* Perform on daughters */
        for(int z=0;z<num_children;z++) daughters[z].postProcessDeathDay(false);
        }
    

    /** Figure out my birth location, and add this as location #0.
        We only have spatial information
        on PREembryonic cells; however, many if not most neural cells are postembryonic.
        The rule is therefore:
      
        <ol>
        <li> if I'm P0, P1', AB, P2', P3', P4', Z3, or Z2, fix me specially.
        Namely, P0 is given a fixed position, and the others are given a final position
        which they achieve a third (what the heck) of the way into their lifespan
        between birth and death, plus an initial position which obeys rule #2 below.
      
        <li> Else if my parent has a location and I have a location, my first location is
        my parent`s last location.  A little ugly, but it does the job. If my birthday
        is actually *after* my first birth location, there may be nasty errors in the
        visualization. My split_radius_distance is initial_split_radius_distance

        <li> Else if my parent has no location but I have a location, my first location is
        as indicated in the data.

        <li> Else we need to do cubic splitting.  My split_radius_distance is 1/2 my parent's
        split_radius_distance.  Determine my split direction (report any cells who don't
        have a split direction). My location is my parent's location, moving in the split 
        direction by split_radius_distance.
        </ol>
        <P>
        Some cells have "many" children.  We'll ignore this issue.  Bleah!
    */


    public void postProcessLocation(boolean root)
        {

        /* Sort */
        sortLocation();

        double birth_location_x=0;
        double birth_location_y=0;
        double birth_location_z=0;

        // hand-hacked because we have no better data.  Condition 1
        if (official_name.equals("P0") ||
            official_name.equals("P1'") || 
            official_name.equals("P2'") ||
            official_name.equals("AB") ||
            official_name.equals("P3'") ||
            official_name.equals("P4'") ||
            official_name.equals("Z3") ||
            official_name.equals("Z2"))
            {
            if (official_name.equals("P0"))
                {
                birth_location_x=30;
                birth_location_y=17.95;
                birth_location_z=13.6;
                }
            else if (official_name.equals("P1'"))
                {
                birth_location_x=45;
                birth_location_y=17.95;
                birth_location_z=13.6;
                }
            else if (official_name.equals("P2'"))
                {
                birth_location_x=54;
                birth_location_y=31.3;
                birth_location_z=14.4;
                }
            else if (official_name.equals("AB"))
                {
                birth_location_x=22.05;
                birth_location_y=17.95;
                birth_location_z=13.6;
                }
            else if (official_name.equals("P3'"))
                {
                birth_location_x=55;
                birth_location_y=25;
                birth_location_z=9.6;
                }
            else if (official_name.equals("P4'"))
                {
                birth_location_x=53.5;
                birth_location_y=26;
                birth_location_z=6.5;
                }
            else if (official_name.equals("Z3"))
                {
                birth_location_x=53;
                birth_location_y=23;
                birth_location_z=6.5;
                }
            else if (official_name.equals("Z2"))
                {
                birth_location_x=53;
                birth_location_y=29;
                birth_location_z=6.5;
                }
            else // we can never get here
                {
                birth_location_x=0;
                birth_location_y=0;
                birth_location_z=0;
                }

            /* Add the birth location */
            pushLocation(birth_location_x, birth_location_y, birth_location_z, birthday+(death_day-birthday)/3);
            
            /* Sort Again */
            sortLocation();
            
            /* Keep on Going ... */
            }
        else if (root)
            {
            // Since I'm not P0', we'll assume I'm a rogue root.
            System.out.println("Whoa! " + official_name + "is root, but has no location!");
            // Assume location is (0,0,0)
            birth_location_x=0;
            birth_location_y=0;
            birth_location_z=0;
            }

        // okay, hopefully everyone who doesn't have a parent still has a birth position.

        if (root || parent==null)  // which should always be the same thing anyway...
            {
            // do nothing 
            }
        else if (location_size!=0 && parent.location_size!=0)  // condition 2   
            {
            // assumes the parent's locations have been sorted by now!
            birth_location_x=parent.location_x[parent.location_size-1];
            birth_location_y=parent.location_y[parent.location_size-1];
            birth_location_z=parent.location_z[parent.location_size-1];
            }
        else if (location_size!=0)  // condition 3
            {
            birth_location_x=location_x[0];
            birth_location_y=location_y[0];
            birth_location_z=location_z[0];
            }
        else                    // condition 4
            {                   
            birth_location_x=parent.location_x[parent.location_size-1];
            birth_location_y=parent.location_y[parent.location_size-1];
            birth_location_z=parent.location_z[parent.location_size-1];
            
            if (lineage_name.equals(""))
                {
                System.out.println("Whoa! " + official_name + "has no lineage name!");
                /* Assume the lineage name is the official name. */
                lineage_name=official_name;
                char sc=lineage_name.charAt(lineage_name.length()-1);
                if (sc=='d') { split_radius_distance[1]=parent.split_radius_distance[1]/2;
                    birth_location_y-=split_radius_distance[1]; }
                else if (sc=='v') { split_radius_distance[1]=parent.split_radius_distance[1]/2;
                    birth_location_y+=split_radius_distance[1];}
                else if (sc=='a') { split_radius_distance[0]=parent.split_radius_distance[0]/2;
                    birth_location_x-=split_radius_distance[0];}
                else if (sc=='p') { split_radius_distance[0]=parent.split_radius_distance[0]/2;
                    birth_location_x+=split_radius_distance[0];}
                else if (sc=='l') { split_radius_distance[2]=parent.split_radius_distance[2]/2;
                    birth_location_z-=split_radius_distance[2];}
                else if (sc=='r') { split_radius_distance[2]=parent.split_radius_distance[2]/2;
                    birth_location_z+=split_radius_distance[2];}
                else System.out.println("Whoa! Lineage name with no split characteristics: " 
                    +lineage_name+ " (" + official_name +")");
                }
/*            else if (parent==null)
              {
              // this shouldn't be able to happen, along with us being !root
              System.out.println("Whoa! " + official_name + "is root, but has no location -- I shouldn't be able to get here!");
              // Assume location is (0,0,0)
              birth_location_x=0;
              birth_location_y=0;
              birth_location_z=0;
              }
*/
            else// further modify by moving cell in split direction
                {
                char sc=lineage_name.charAt(lineage_name.length()-1);
                if (sc=='d') { split_radius_distance[1]=parent.split_radius_distance[1]/2;
                    birth_location_y-=split_radius_distance[1]; }
                else if (sc=='v') { split_radius_distance[1]=parent.split_radius_distance[1]/2;
                    birth_location_y+=split_radius_distance[1];}
                else if (sc=='a') { split_radius_distance[0]=parent.split_radius_distance[0]/2;
                    birth_location_x-=split_radius_distance[0];}
                else if (sc=='p') { split_radius_distance[0]=parent.split_radius_distance[0]/2;
                    birth_location_x+=split_radius_distance[0];}
                else if (sc=='l') { split_radius_distance[2]=parent.split_radius_distance[2]/2;
                    birth_location_z-=split_radius_distance[2];}
                else if (sc=='r') { split_radius_distance[2]=parent.split_radius_distance[2]/2;
                    birth_location_z+=split_radius_distance[2];}
                else System.out.println("Whoa! Lineage name with no split characteristics: " 
                    +lineage_name+ " (" + official_name +")");
                }
            }

        /* Add the birth location */
        pushLocation(birth_location_x, birth_location_y, birth_location_z, birthday);

        /* Sort Again */
        sortLocation();

        /* Perform on daughters */
        for(int z=0;z<num_children;z++) daughters[z].postProcessLocation(false);
        }

    
    /** Push into add_here the descendents of a given cell -- all of them. */

    public void descendentsOf(Cell cell, HashMap add_here)
        {
        /* add my name to the hashtable */
        add_here.put(official_name,this);
        for(int z=0;z<num_children;z++) daughters[z].descendentsOf(cell,add_here);
        }
        

        
        
    double[] loc_xyz = new double[3];
    public Stoppable stopper;
        
    public void step(SimState state)
        {
        Celegans celegans = ((Celegans)state);
        double time = state.schedule.getTime();
        if (time >= death_day && time < Schedule.AFTER_SIMULATION)  // time to split!  But hang around if the simulation just ended
            {
            stopper.stop();
            celegans.cells.remove(this);
            if (daughters != null)
                {
                for(int i = 0; i < num_children; i++)
                    {
                    daughters[i].stopper = state.schedule.scheduleRepeating(daughters[i]);
                    daughters[i].step(state);  // step 'em once to have them add themselves at the right location
                    if (daughters[i].isNeuron())
                        {
                        int size = daughters[i].synapses.size();
                        for (int j=0; j < size; j++)
                            {
                            Synapse s = (Synapse)(daughters[i].synapses.get(j));
                            if (celegans.neurons.exists(s.to) && celegans.neurons.exists(s.from))  // time to add!  Both neurons are now in the field
                                celegans.synapses.addEdge(s.to, s.from, s);
                            }
                        }
                    }
                }
            }
        else
            {
            getLocation((int)time, loc_xyz);  // ugh, cast to int
            celegans.cells.setObjectLocation(this, new Double3D(loc_xyz[0], loc_xyz[1], loc_xyz[2]));
            if (isNeuron())
                celegans.neurons.setObjectLocation(this, new Double3D(loc_xyz[0], loc_xyz[1], loc_xyz[2]));
            }
        }
                
    public String toString() { return "Cell " + official_name; }
    }
