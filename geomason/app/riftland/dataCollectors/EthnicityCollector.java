package sim.app.geo.riftland.dataCollectors;

import sim.app.geo.masoncsc.datawatcher.DataWatcher;
import sim.app.geo.masoncsc.datawatcher.FileDataWriter;
import sim.app.geo.masoncsc.datawatcher.ListDataWatcher;
import sim.app.geo.riftland.Parameters;
import sim.app.geo.riftland.World;
import sim.app.geo.riftland.household.Household;
import sim.engine.SimState;
import sim.engine.Steppable;
import sim.util.Bag;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 * It collects all information (number of farmers / herders/ laborers/ displaced)
 * about each ethnicity group in the given subarea.
 *
 * @author Habib Karbasian
 */
public class EthnicityCollector implements Steppable
{
    private class EthnicityEntity
    {

        private int cultureID = 0;
        private int farmers = 0;
        private int herders = 0;
        private int laborers = 0;
        private int displaced = 0;
        private int total = 0;

        // <editor-fold defaultstate="collapsed" desc="Accessors">

        int getCultureID() {
            return cultureID;
        }

        void setCultureID(int cultureID) {
            this.cultureID = cultureID;
        }

        int getFarmers() {
            return farmers;
        }

        void setFarmers(int farmers) {
            this.farmers = farmers;
        }

        int getHerders() {
            return herders;
        }

        void setHerders(int herders) {
            this.herders = herders;
        }

        int getLaborers() {
            return laborers;
        }

        void setLaborers(int laborers) {
            this.laborers = laborers;
        }

        int getDisplaced() {
            return displaced;
        }

        void setDisplaced(int displaced) {
            this.displaced = displaced;
        }

        int getTotal() {
            return total;
        }

        void setTotal(int total) {
            this.total = total;
        }
        // </editor-fold>
    }

    final Parameters params;
    final World state;
    FileDataWriter fileDW;
    DataWatcher masterDataWatcher;

    public EthnicityCollector(final World state)
    {
        assert(state != null);
        this.state = state;
        this.params = state.getParams();
        fileDW = new FileDataWriter();

        setupOutputData();
    }

    public void start()
    {
        fileDW.InitFileDataWriter(params.system.getEthnicityOutputFilename(), masterDataWatcher);
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
                dataList.add(data);
            }

            @Override
            public String getCSVHeader() {
                String header = "Step";
                header = header + ",EthnicID,EthnicName,NumFarmers,NumHerders,NumLaborers,NumDisplaced";
                return header + "\n";
            }
        };
    }

    private String collectAllData()
    {
        HashMap<Integer, EthnicityEntity> ethnicityGroups = new HashMap<Integer, EthnicityEntity>();
        Bag householdBag = ((World)state).getPopulation().getHouseholds();
        HashMap displacedPeople = ((World)state).getPopulation().getDisplacedPeopleByEthnicity();
        String table = "";

        //Data Collection for farmers/herders/laborers
        for (int i = 0; i < householdBag.size(); i++)
        {
            Household thisHousehold = (Household) householdBag.objs[i];
            int culture = thisHousehold.getCulture();
            EthnicityEntity entity;

            if (!ethnicityGroups.containsKey(culture))
            {
                entity = new EthnicityEntity();

                entity.setCultureID(culture);
                entity.setFarmers(thisHousehold.getFarmingPopulation());
                entity.setHerders(thisHousehold.getHerdingPopulation());
                entity.setLaborers(thisHousehold.getLaboringPopulation());
            }
            else
            {
                entity = ethnicityGroups.get(culture);

                entity.setFarmers(entity.getFarmers() + thisHousehold.getFarmingPopulation());
                entity.setHerders(entity.getHerders() + thisHousehold.getHerdingPopulation());
                entity.setLaborers(entity.getLaborers() + thisHousehold.getLaboringPopulation());
            }
            ethnicityGroups.put(culture, entity);
        }

        //Data Collection for displaced
        for (Object o : displacedPeople.entrySet())
        {
            Map.Entry pairs = (Map.Entry) o;
            int displacedCulture = (Integer) pairs.getKey();
            EthnicityEntity entity;

            if (!ethnicityGroups.containsKey(displacedCulture))
                entity = new EthnicityEntity();
            else
                entity = ethnicityGroups.get(displacedCulture);

            entity.setCultureID(displacedCulture);
            entity.setDisplaced((Integer)displacedPeople.get(displacedCulture));
            ethnicityGroups.put(displacedCulture, entity);
        }

        //prepares all data collected in a string for printing
        for (Object o : ethnicityGroups.entrySet())
        {
            Map.Entry pairs = (Map.Entry) o;
            int ethnicityCulture = (Integer) pairs.getKey();
            EthnicityEntity entity = (EthnicityEntity) pairs.getValue();
            table += String.valueOf(((World)state).schedule.getSteps()) + "," +
                    entity.getCultureID() + "," +
                    ((World)state).getPopulation().getEthnicName(ethnicityCulture) + "," +
                    entity.getFarmers() + "," +
                    entity.getHerders() + "," +
                    entity.getLaborers() + "," +
                    entity.getDisplaced() + "\n";
        }
        if (table.length() > 0)
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

