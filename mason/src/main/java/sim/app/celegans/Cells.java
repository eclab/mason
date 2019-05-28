/*
  Copyright 2006 by Sean Luke and George Mason University
  Licensed under the Academic Free License version 3.0
  See the file "LICENSE" for more information
*/

package sim.app.celegans;

import java.util.*;
import java.io.*;
import java.util.zip.*;
import sim.util.*;

/** Cells is the database of cell information in the program.  It loads and parses
    cells from the data file, creating five database items: a cell dictionary (looked
    up by the cell's official name), a group dictionary, an expression pattern
    dictionary, a cell fate dictionary, and a ArrayList of lineage roots.  Ordinarily,
    P0' would be the only root in a lineage tree, but the data is flawed and in fact
    several cells don't have parents and get put in as roots.  Hence the need for
    a vector rather than a single root.

    Most of Cells' items are public; there's little real encapsulation here, in the
    name of getting the job done.
*/

public class Cells extends Object
    {
    private static final long serialVersionUID = 1;

    public HashMap cell_dictionary;
    //    public HashMap group_dictionary;
    //    public HashMap pattern_dictionary;
    //    public HashMap fate_dictionary;
    public ArrayList roots;     // Cells with no parent. There should only be one, but what the heck.
    int num_processed_cells;
        
    public Cell P0;

    public Cells()
        {
        cell_dictionary = new HashMap(300);
        //      group_dictionary = new HashMap();
        //      pattern_dictionary = new HashMap();
        //      fate_dictionary = new HashMap();
        roots = new ArrayList();
        num_processed_cells=0;
        try 
            {
            Reader r = new InputStreamReader(new GZIPInputStream(Cells.class.getResourceAsStream("cells.ace4.gz")));
            readCells(r);
            r.close();
            postProcess();
            }
        catch (IOException e) { throw new RuntimeException(e); }
        }


    /** PostProcesses the cells after their basic information has been loaded and
        parsed from the input file.  A <i>lot</i> of derived information is formed
        during postProcess(), as you can see from the method code. */

    public void postProcess()
        {
        Iterator cells;
        
        /* Assign Types; report everyone who doesn't have a parent (there should only be one: P0) */
        
        System.out.println("-----Assigning Cell Types, and Parents to equivalent-origin cells");
        cells= cell_dictionary.values().iterator();
        while(cells.hasNext())
            {
            Cell cell=(Cell)cells.next();

            if (cell.official_name.equals("P0"))
                P0 = cell;

            if (cell.parent==null && !cell.official_name.equals("P0") && cell.num_equivalence_origin==0)
                System.out.println("Whoa!  This ain't right: " +cell.official_name+ "Has no parent.");
            if (cell.parent==null && cell.num_equivalence_origin==0) roots.add(cell);

            if (cell.num_equivalence_origin!=0)
                {
                cell.type=Cell.cell_type_postembryonic_dual_origin;
                cell.parent=cell.equivalence_origin[0]; // determining equivalence junk
                if (cell.parent.equivalence_fate[0]!=cell) // determining equivalence junk
                    cell.parent.equivalence_fate[0].parent=cell.equivalence_origin[1];
                else cell.parent.equivalence_fate[1].parent=cell.equivalence_origin[1];
                }
            else if (cell.official_name.equals("P0") || 
                cell.official_name.equals("P1'") ||
                cell.official_name.equals("P2'") ||
                cell.official_name.equals("AB") ||
                cell.official_name.equals("P3'") ||
                cell.official_name.equals("P4'"))
                cell.type=Cell.cell_type_preembryonic_unknown_position;
            else if (cell.official_name.equals("Z3") ||
                cell.official_name.equals("Z2"))
                cell.type=Cell.cell_type_postembryonic_unknown_position;
            else if (cell.birthday<Cell.post_embryonic_birthday) /* no time born, probably no reconstruction */
                cell.type=Cell.cell_type_postembryonic;
            else cell.type=Cell.cell_type_preembryonic;
            }

        /* First, make certain that every cell has a birthday and a death day */
        
        System.out.println("-----Assigning Birthdays and Deaths");
        cells=roots.iterator();
        while(cells.hasNext())
            {
            Cell cell=(Cell)cells.next();
            cell.postProcessBirthday(true);
            }
        cells=roots.iterator();
        while(cells.hasNext())
            {
            Cell cell=(Cell)cells.next();
            cell.postProcessDeathDay(true);
            }
        
        
        /* Next, I need to figure my locations in space */

        System.out.println("-----Assigning Locations");
        cells=roots.iterator();
        while(cells.hasNext())
            {
            Cell cell=(Cell)cells.next();
            cell.postProcessLocation(true);
            }

        /* Next we move the locations around the origin.  I believe the locations are
           located between 0 and 60 X, 0 and 40 Y, and 0 and 40 Z.  So we move them
           by (-30,-20,-20) to center them *roughly* about the origin. */

        cells=roots.iterator();
        while(cells.hasNext())
            {
            Cell cell=(Cell)cells.next();
            cell.modifyLocations(-30.0,-20.0,-20.0);
            }

        /* Give Everyone a Volume. */
        cells=roots.iterator();
        while(cells.hasNext())
            {
            Cell cell=(Cell) cells.next();
            cell.setVolume(1.0f);
            }
        
        /* And we're done! */

        System.out.println("-----Finished PostProcessing");

        }


    /** Loads the cells from the input data.  After this method is called, you should
        call postProcess() to finish the data setup */


    public void readCells(Reader r)
        {
        Cell currentCell=null;
        ArrayList v=new ArrayList();
        num_processed_cells=0;
        Scanner d = new Scanner(r);
        System.out.println("-----Loading 2237 Cells....");
        System.out.println();

        try
            {
            while(readCellLine(d,v)!=-1)
                currentCell=processCellLine(currentCell,v);
            }
        catch (IOException e)
            {
            System.out.println(e.getMessage());
            }
        System.out.println(); System.out.println("-----Phew! Finally finished loading...");
        }
    

    /** readCellLine() is used by readCells() to read a cell line from the input stream. */

    int readCellLine(Scanner i, ArrayList v) throws IOException
        {
        v.clear();
        if (!i.hasNextLine()) return -1;
        String s=i.nextLine();
        // if (s==null) return -1;
        int pos=0;
        int newpos;
        int size=s.length();
        while(true)
            {
            newpos=s.indexOf('|',pos);
            if (newpos<0 || newpos>=size)  // Bad Index, nothing left
                break;
            if (newpos==pos) v.add("");
            else v.add(s.substring(pos,newpos));
            pos=newpos+1;
            }
        if (pos!=size)
            {
            // Grab last one if any
            v.add(s.substring(pos,size));  // is that right?
            }
        return 1;
        }


    /** processCellLine is used by readCells() to process a single cell line from the input stream.  As you can tell, this is a big method, doing a lot of processing.*/

    public Cell processCellLine(Cell current, ArrayList v) throws NumberFormatException
        {
        if (v==null) return current;
        if (v.size()==0) return current;
        String title=(String)v.get(0);
        if (title.equals("Cell"))
            {
            current=fetchCell((String)v.get(2));
            if (num_processed_cells%100 == 0)
                System.out.println((num_processed_cells+1) + ": " + current.official_name);
            num_processed_cells++;
            }

        else if (current==null) return current;
        else
            {
            // here we go!
            if (title.equals("Parent"))
                { 
                current.parent=fetchCell((String)v.get(1));
                if (current.parent.num_children>=2) 
                    /* We've got a problem.
                       Extend the array for this
                       exceptional situation */
                    {
                    Cell tmp[] = 
                        new Cell[current.parent.num_children+1];
                    /* Yeah, yeah, it's a linear increase,
                       which is O(n^2) in the worst case,
                       but this is an exceptional situation
                       hopefully. */
                    System.arraycopy(current.parent.daughters,0,tmp,0,current.parent.num_children);
                    current.parent.daughters=tmp;
                    System.out.print(current.parent.official_name + " has more than 2 children: ");
                    for(int zz=0;zz<current.parent.num_children;zz++)
                        System.out.print(current.parent.daughters[zz].official_name + ", ");
                    System.out.println("and " + current.official_name);
                    }
                current.parent.daughters[current.parent.num_children++]=current;
                }
            else if (title.equals("Daughter"))   
                {
                /* Not interested.  We assume this is done by Parent */
                }
            else if (title.equals("Lineage_name"))
                {
                current.lineage_name=(String)v.get(1);
                }
            else if (title.equals("Embryo_division_time"))
                {
                current.embryo_division_time=Double.valueOf((String)v.get(1)).doubleValue();
                }
            else if (title.equals("Reconstruction"))
                {
                /* Two items we're interested in: Birth and Timepoint */
                if (v.get(2).equals("Birth"))
                    {
                    current.time_born=Double.valueOf((String)v.get(3)).doubleValue();
                    }
                else if (v.get(2).equals("Timepoint"))
                    {
                    current.
                        pushLocation(Double.valueOf((String)v.get(5)).doubleValue(),
                            Double.valueOf((String)v.get(6)).doubleValue(),
                            Double.valueOf((String)v.get(7)).doubleValue(),
                            Double.valueOf((String)v.get(3)).doubleValue());
                    }
                }
            else if (title.equals("Neurodata"))
                {
                /* For the moment, we ignore Receive and Receive_Joint,
                   assume Send and Send_Joint are equivalent,
                   and include gap junctions.
                   
                   Furthermore, for lack of knowledge, we're assuming that the N2U and JSH
                   statements are in fact different surveys (this could be TOTALLY wrong);
                   we're going with N2U---looks more interesting.

                   Also, we assume the last value in the neurodata line is the *number*
                   of synapse connections.  Again, this could be totally wrong.
                */
                if (v.get(3).equals("N2U"))  // what we're going with
                    {
                    if (v.get(2).equals("Send") || v.get(2).equals("Send_joint"))
                        {
                        Synapse s= new Synapse();
                        Cell to=fetchCell((String)v.get(1));
                        s.to=to;
                        s.from=current;
                        s.type=Synapse.type_chemical;
                        s.number= Integer.valueOf((String)v.get(4)).intValue();
                        current.synapses.add(s);
                        to.synapses.add(s);
                        }
                    else if (v.get(2).equals("Gap_junction"))
                        {
                        Synapse s= new Synapse();
                        Cell to=fetchCell((String)v.get(1));
                        s.to=to;
                        s.from=current;
                        s.type=Synapse.type_gap;
                        s.number= Integer.valueOf((String)v.get(4)).intValue();
                        current.synapses.add(s);
                        to.synapses.add(s);
                        }
                    /* I've tried to code Cell.java to ignore duplicate gap junctions. */
                    }
                }
            else if (title.equals("Equivalence_origin"))
                {
                Cell equiv=fetchCell((String)v.get(1));
                current.equivalence_origin[current.num_equivalence_origin++]=equiv;
                equiv.equivalence_fate[equiv.num_equivalence_fate++]=current;
                }
            else if (title.equals("Equivalence_fate"))
                {
                /* Don't care. Done with equivalence origin. */
                }
            else if (title.equals("Cell_group"))
                {
                current.cellGroup = fetchGroup((String)v.get(1));
                }
            else if (title.equals("Expr_pattern"))
                {
                current.expressionPattern=fetchPattern((String)v.get(1));
                }
            else if (title.equals("Fate"))
                {
                current.fate = fetchFate((String)v.get(1));
                }
            else if (title.equals("Remark"))
                {
                if (current.remark.equals("")) current.remark = (String)(v.get(1));
                else current.remark = current.remark + "; " + (String)(v.get(1));
                }
            else
                {
                /* Nothing for the while */
                }
            }
        return current;
        }
    
   






    /** Fetches a cell in the cell dictionary by a given key, creating a new cell if the key doesn't exist yet. */

    public Cell fetchCell(String key)
        {
        // is cell already in dictionary?
        if (cell_dictionary.containsKey(key))
            return (Cell)cell_dictionary.get(key);
        else
            {
            // make a new cell and add it to dictionary
            Cell c=new Cell();
            c.official_name=key;
            cell_dictionary.put(key,c);
            return c;
            }
        }


    /** Fetches a group in the group dictionary by a given key, creating a new group if the key doesn't exist yet. */

    public int fetchGroup(String key)
        {
        for(int i=0; i < Cell.cellGroups.length; i++)
            if (Cell.cellGroups[i].equalsIgnoreCase(key))
                return i;
        System.out.println("Unknown cell group: " + key);
        return 0;
        }


    /** Fetches an expression pattern in the pattern dictionary by a given key, creating a new pattern if the key doesn't exist yet. */

    public int fetchPattern(String key)
        {
        for(int i=0; i < Cell.expressionPatterns.length; i++)
            if (Cell.expressionPatterns[i].equalsIgnoreCase(key))
                return i;
        System.out.println("Unknown expression pattern: " + key);
        return 0;
        }

    /** Fetches a cell fate in the fate dictionary by a given key, creating a new fate if the key doesn't exist yet. */

    public int fetchFate(String key)
        {
        for(int i=0; i < Cell.fates.length; i++)
            if (Cell.fates[i].equalsIgnoreCase(key))
                return i;
        System.out.println("Unknown fate: " + key);
        return 0;
        }
    }
