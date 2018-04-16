package riftland.dataCollectors;

import masoncsc.datawatcher.*;
import masoncsc.util.Pair;
import riftland.Parameters;
import riftland.World;
import sim.engine.SimState;
import sim.engine.Steppable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;


/**
 * It collects all information (number of population / displaced / farmers / herders / laboreres)
 * about each ethnicity group in the given subarea.
 *
 * @author Habib Karbasian
 * @author Eric Siggy
 */
public class DataCollector implements Steppable
{
    final Parameters params;
    final World state;
    FileDataWriter fileDW;
    DataWatcher masterDataWatcher;
    List<DataWatcher> dataWatchers = new ArrayList<DataWatcher>();
    HashMap<String, String> outputHeader = new HashMap<String, String>();

    public DataCollector(final World state)
    {
        assert(state != null);
        this.state = state;
        this.params = state.getParams();
        fileDW = new FileDataWriter();

        initHeaders();
        setupOutputData();
    }

    public void start()
    {
            fileDW.InitFileDataWriter(params.system.getOutputFilename(), masterDataWatcher);
    }

    private void initHeaders()
    {
        outputHeader.put("Non-Displaced Population", "TotalPop");
        outputHeader.put("Displaced Population", "NumDisplaced");
        outputHeader.put("Farmers Population", "NumFarmers");
        outputHeader.put("Herders Population", "NumHerders");
        outputHeader.put("Laborers Population", "NumLaborers");
        outputHeader.put("Population Change Rate", "PopChange");
    }

    /** Create anonymous DataWatchers for each output and add them to the
     * dataWatchers list. */
    private void setupOutputData()
    {
        dataWatchers = new ArrayList<DataWatcher>();

        //NonDisplacedPopulation
        dataWatchers.add(new PairDataWatcher<Long, Long>() {

            @Override
            protected void updateDataPoint()
            {
                final long currentStep = ((World)state).schedule.getSteps();
                final long nonDisplacedPopulation = ((World)state).getPopulation().getNonDisplacedPopulation();

                dataPoint = new Pair<Long, Long>(currentStep, nonDisplacedPopulation);
            }

            @Override
            public String getCSVHeader()
            {
                return outputHeader.get("Non-Displaced Population");
            }
        });

        //DisplacedPopulation
        dataWatchers.add(new PairDataWatcher<Long, Long>() {

            @Override
            protected void updateDataPoint()
            {
                final long currentStep = ((World)state).schedule.getSteps();
                final long displacedPopulation = ((World)state).getPopulation().getCurrentPopulationDisplaced();

                dataPoint = new Pair<Long, Long>(currentStep, displacedPopulation);
            }

            @Override
            public String getCSVHeader()
            {
                return outputHeader.get("Displaced Population");
            }
        });

        //FarmersPopulation
        dataWatchers.add(new PairDataWatcher<Long, Long>() {

            @Override
            protected void updateDataPoint()
            {
                final long currentStep = ((World)state).schedule.getSteps();
                final long farmersPopulation = ((World)state).getPopulation().getCurrentPopulationFarmers();

                dataPoint = new Pair<Long, Long>(currentStep, farmersPopulation);
            }

            @Override
            public String getCSVHeader()
            {
                return outputHeader.get("Farmers Population");
            }
        });

        //HerdersPopulation
        dataWatchers.add(new PairDataWatcher<Long, Long>() {

            @Override
            protected void updateDataPoint()
            {
                final long currentStep = ((World)state).schedule.getSteps();
                final long herdersPopulation = ((World)state).getPopulation().getCurrentPopulationHerders();

                dataPoint = new Pair<Long, Long>(currentStep, herdersPopulation);
            }

            @Override
            public String getCSVHeader()
            {
                return outputHeader.get("Herders Population");
            }
        });

        //LaborersPopulation
        dataWatchers.add(new PairDataWatcher<Long, Long>() {

            @Override
            protected void updateDataPoint()
            {
                final long currentStep = ((World)state).schedule.getSteps();
                final long laborersPopulation = ((World)state).getPopulation().getCurrentPopulationLaborers();

                dataPoint = new Pair<Long, Long>(currentStep, laborersPopulation);
            }

            @Override
            public String getCSVHeader()
            {
                return outputHeader.get("Laborers Population");
            }
        });

        //PopulationChangeRate
        dataWatchers.add(new PairDataWatcher<Long, Long>() {

            @Override
            protected void updateDataPoint()
            {
                final long currentStep = ((World)state).schedule.getSteps();
                final long populationChangeRate = (long)((World)state).getPopulation().getCurrentPopulationChangeRate();

                dataPoint = new Pair<Long, Long>(currentStep, populationChangeRate);
            }

            @Override
            public String getCSVHeader()
            {
                return outputHeader.get("Population Change Rate");
            }
        });

        //MasterDataWatcher collects all data from other outputs
        masterDataWatcher = new ListDataWatcher<String>() {
            { addListener(fileDW); }

            @Override
            protected void updateDataPoint() {
                dataList = new ArrayList<String>();
                dataList.add(String.valueOf(((World)state).schedule.getSteps()));
                for (DataWatcher dw : dataWatchers)
                {
                    dataList.add(String.valueOf(((Pair)dw.getDataPoint()).getRight()));
                }
            }

            @Override
            public String getCSVHeader() {
                String header = "Step";
                for (DataWatcher dw : dataWatchers)
                {
                    header = header + ("," + dw.getCSVHeader());
                }
                return header + "\n";
            }
        };

    }

    public void clearAll()
    {
        fileDW.close();
    }
    
    @Override
    public void step(SimState state)
    {
        for (DataWatcher dw : dataWatchers)
        {
            dw.update();
        }
        masterDataWatcher.update();
    }
}

