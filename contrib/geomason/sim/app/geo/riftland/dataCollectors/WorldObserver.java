/*
 * WorldObserver.java
 *
 * $Id: WorldObserver.java 2028 2013-09-04 18:44:30Z escott8 $
 */

package riftland.dataCollectors;

import riftland.World;
import riftland.conflict.Mediator;
import cityMigration.DisplacementEvent;
import net.sf.csv4j.CSVWriter;
import sim.engine.SimState;
import sim.engine.Steppable;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

/** Collects simulation data and writes it out to a file.
 *
 * It's a Steppable because we need to embed it in the simulation to
 * access data as its running.  So we create a WorldObserver and schedule
 * it to run with each step either first or last to collect data.
 *
 * This version of WorldObserver will emit data to a CSV file.  The CSV
 * filename will be in the form YYMMDDHHMMSSriftland.csv.
 *
 * TODO: may want to explore using alternative SortedMap<> interface for
 *       the CSVWriter; it may be more intuitive to use that to add new
 *       headers and data; besides using String arrays is error prone since
 *       adding new fields means making sure field header and data are
 *       in sync manually whereas using the SortedMap<> would handle all
 *       that automatically.
 *
 * @author mcoletti and jbassett
 */
public final class WorldObserver implements Steppable
{

//    private BufferedWriter dataFileBuffer_; // output file buffer for dataCSVFile_
//    private CSVWriter dataCSVFile_; // CSV file that contains run data

    private BufferedWriter displacementFileBuffer_;
    private CSVWriter displacementCSVFile_;
    private World world;
    // current simulation step
    //
    // incremented in WorldObservor.step()
    private int step = 0;

//    // NOTE: These are not currently hooked up, i.e. they are ignored.
//    private boolean trackNumHerders = true;
//    private boolean trackNumAnimals = true;
//    private boolean trackNumConflicts = true;
//    private boolean trackHHConflicts = true;
//    private boolean trackHFConflicts = true;
//    
//    public void setup(World world)
//    {
//    	trackNumHerders = world.returnBooleanParameter("TrackCurrentNumHerders", trackNumHerders);
//    	trackNumAnimals = world.returnBooleanParameter("TrackCurrentNumAnimals", trackNumAnimals);
//    	trackNumConflicts = world.returnBooleanParameter("TrackNumConflicts", trackNumConflicts);
//    	trackHHConflicts = world.returnBooleanParameter("TrackHHConflicts", trackHHConflicts);
//    	trackHFConflicts = world.returnBooleanParameter("TrackHFConflicts", trackHFConflicts);
//    }

    WorldObserver(World world)
    {
        this.world = world;
//    	setup(world);
    	//<GCB>: you may want to adjust the number of columns based on these flags.
    	// both in createLogFile, and step

        startLogFile();
    }

    WorldObserver()
    {
        startLogFile();
    }


    private void startLogFile()
    {
        // Create a CSV file to capture data for this run.
        try
        {
            createLogFile();

            // First line of file contains field names
//            String [] header = new String [] {"Job","Step","Culture","TotalPop","Farmers","Herders","Laborers","Displaced"
//            };
//
//            dataCSVFile_.writeLine(header);

            String [] dis_header = new String [] {"Timestamp","parcelX","parcelY","GroupSize","Culture","Citizenship" };

            displacementCSVFile_.writeLine(dis_header);

        }
        catch (IOException ex)
        {
            Logger.getLogger(World.class.getName()).log(Level.SEVERE, null, ex);
        }
    }


    /** Append simulation stats to the data file.
     *
     * @param state
     */
    public
    void step(SimState state)
    {
        // Commented out since the simulation variables are in flux. MAC, 5/25/2011.
        
        World world = (World)state;
        Mediator mediator = world.getMediator();

        // write current step and number of conflicts to CSV file
        String job = Long.toString(state.job());
        
/* These variables have all been deleted.  Nothing we were logging to CSV exists anymore! -- Siggy
        int cult0 = world.trackedCulture[0]; //199;
        int cult1 = world.trackedCulture[1]; // 37;
        int cult2 = world.trackedCulture[2]; // 86;
        String culture0      = Integer.toString(cult0);
        String totalPop0     = Integer.toString((int) world.getCurrentPopulationCultureID(cult0));
        String numFarmers0   = Integer.toString((int) world.getPopForActivityIDCultureID(0,cult0));
        String numHerders0   = Integer.toString((int) world.getPopForActivityIDCultureID(1,cult0));
        String numLaborers0  = Integer.toString((int) world.getPopForActivityIDCultureID(2,cult0));
        String numDisplaced0 = Integer.toString((int) world.getPopForActivityIDCultureID(3,cult0));

        String culture1      = Integer.toString(cult1);
        String totalPop1     = Integer.toString((int) world.getCurrentPopulationCultureID(cult1));
        String numFarmers1   = Integer.toString((int) world.getPopForActivityIDCultureID(0,cult1));
        String numHerders1   = Integer.toString((int) world.getPopForActivityIDCultureID(1,cult1));
        String numLaborers1  = Integer.toString((int) world.getPopForActivityIDCultureID(2,cult1));
        String numDisplaced1 = Integer.toString((int) world.getPopForActivityIDCultureID(3,cult1));
        String cultureInfo = world.getCultureInfo();
        
        String culture2      = Integer.toString(cult2);
        String totalPop2     = Integer.toString((int) world.getCurrentPopulationCultureID(cult2));
        String numFarmers2   = Integer.toString((int) world.getPopForActivityIDCultureID(0,cult2));
        String numHerders2   = Integer.toString((int) world.getPopForActivityIDCultureID(1,cult2));
        String numLaborers2  = Integer.toString((int) world.getPopForActivityIDCultureID(2,cult2));
        String numDisplaced2 = Integer.toString((int) world.getPopForActivityIDCultureID(3,cult2));
        String endmarker =    Integer.toString(0);
        
        String [] data = new String [] {job, Integer.toString(this.step), 
            culture0,totalPop0,numFarmers0,numHerders0,numLaborers0,numDisplaced0,
            culture1,totalPop1,numFarmers1,numHerders1,numLaborers1,numDisplaced1,
            culture2,totalPop2,numFarmers2,numHerders2,numLaborers2,numDisplaced2,cultureInfo,
            endmarker};
            */
//        String[] data = null;
        
        try
        {
//            this.dataCSVFile_.writeLine(data);

            for (DisplacementEvent event : world.getPopulation().getDisplacementEvents())
            {
                this.displacementFileBuffer_.write(event.toString() + "\n");
                //System.out.println(event.toString() + "\n");
            }
            world.getPopulation().getDisplacementEvents().clear();
        }
        catch (IOException ex)
        {
            Logger.getLogger(WorldObserver.class.getName()).log(Level.SEVERE, null, ex);
        }

//        System.out.println("AvgHunger "+world.getCurrentAverageHerderHunger()+"\tAvgThrirst "+world.getCurrentAverageHerderThirst());

//        //Detect zombie herding.  If there is at least 1 herder, then the smallest herd must be strictly positive!
//        assert(world.getCurrentNumHerders() ==0 || world.getMinHerdSize()>0);
        
//        World world = (World) state;
        
        this.step++;
    }


    // closes the file
    //
    // called by sim state finish()
    void finish()
    {
        try
        {
//            this.dataFileBuffer_.close();
            this.displacementFileBuffer_.close();
        }
        catch (IOException ex)
        {
            Logger.getLogger(WorldObserver.class.getName()).log(Level.SEVERE, null, ex);
        }
    }


    // creates the log file associated with current run
    //
    // Will write out statistics for the current run into a CSV file with
    // a name formatted as YYMMDDHHSSriftland.csv.
    private void createLogFile() throws IOException
    {
        long now = System.currentTimeMillis();

//        String filename = String.format("%ty%tm%td%tH%tM%tS", now, now, now, now, now, now)
//            + "riftland.csv";
//
//        this.dataFileBuffer_ = new BufferedWriter(new FileWriter(filename));
//        this.dataCSVFile_ = new CSVWriter(dataFileBuffer_);

//        String displacementfilename = String.format("%ty%tm%td%tH%tM%tS", now, now, now, now, now, now)
//                + "displacementInfo.csv";
        String displacementfilename = world.getParams().system.getDisplacedOutputFilename();

        this.displacementFileBuffer_ = new BufferedWriter(new FileWriter(displacementfilename));
        this.displacementCSVFile_ = new CSVWriter(displacementFileBuffer_);
    }

   /**
    * Save the current state of the object.  This automatically gets called
    * by the Java serialization mechanism during checkpointing.
    * <p>
    * While this routine is normally not necessary, we need it here because
    * the CSVWriter class is not serializable.
    *
    * @param  out  the output stream for the checkpoint file
    */
    private void writeObject(java.io.ObjectOutputStream out)
        throws IOException
    {
        out.writeInt(step);

//        out.writeBoolean(trackNumHerders);
//        out.writeBoolean(trackNumAnimals);
//        out.writeBoolean(trackHHConflicts);
//        out.writeBoolean(trackHFConflicts);
    }


   /**
    * Restore the state of the object from a file.  This automatically gets
    * called by the Java serialization mechanism when a checkpoint is loaded.
    * <p>
    * While this routine is normally not necessary, we need it here because
    * 1) the CSVWriter class is not serializable, and
    * 2) a new output file should be opened.
    *
    * @param  in  the input stream for the checkpoint file
    */
    private void readObject(java.io.ObjectInputStream in)
        throws IOException, ClassNotFoundException
    {
        step = in.readInt();

//        trackNumHerders = in.readBoolean();
//        trackNumAnimals = in.readBoolean();
//        trackHHConflicts = in.readBoolean();
//        trackHFConflicts = in.readBoolean();

        startLogFile();
    }
}
