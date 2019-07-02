package sim.app.geo.riftland.dataCollectors;

import sim.app.geo.cityMigration.DisplacementEvent;
import sim.app.geo.masoncsc.datawatcher.DataWatcher;
import sim.app.geo.masoncsc.datawatcher.FileDataWriter;
import sim.app.geo.masoncsc.datawatcher.ListDataWatcher;
import sim.app.geo.riftland.Parameters;
import sim.app.geo.riftland.World;
import sim.engine.SimState;
import sim.engine.Steppable;
import java.util.ArrayList;

/**
 * It collects all information ("step", "parcelX", "parcelY", "GroupSize", "Culture", "Citizenship")
 * about every displacement event in the given subarea.
 *
 * @author Habib Karbasian
 */

public class DisplacementEventCollector implements Steppable
{
    final Parameters params;
    final World state;
    FileDataWriter fileDW;
    DataWatcher masterDataWatcher;
    String table = null;

    public DisplacementEventCollector(final World state)
    {
        assert(state != null);
        this.state = state;
        this.params = state.getParams();
        fileDW = new FileDataWriter();

        setupOutputData();
    }

    public void start()
    {
        fileDW.InitFileDataWriter(params.system.getDisplacedOutputFilename(), masterDataWatcher);
    }

    /** Create anonymous DataWatchers for each output and add them to the
     * dataWatchers list. */
    private void setupOutputData()
    {
        //MasterDataWatcher collects all data from other outputs
        masterDataWatcher = new ListDataWatcher<String>() {
            { addListener(fileDW); }

            @Override
            protected void updateDataPoint() {
                dataList = new ArrayList<String>();
                String data = collectAllData();
                if (data != null)
                    dataList.add(data);
            }

            @Override
            public String getCSVHeader() {
                String header = "Step";
                header = header + ",ParcelX,ParcelY,GroupSize,Culture,Citizenship";
                return header + "\n";
            }
        };
    }

    private String collectAllData()
    {
        table = null;

        for (DisplacementEvent event : state.getPopulation().getDisplacementEvents())
        {
            if (table != null)
                table += event.toString() + "\n";
            else
                table = event.toString() + "\n";
        }
        state.getPopulation().getDisplacementEvents().clear();

        if (table != null && table.length() > 0)
            table = table.substring(0, table.length() - 1);
        return table;
    }

    public void clearAll()
    {
        fileDW.close();
    }

    public void step(SimState state)
    {
        masterDataWatcher.update();
    }
}
