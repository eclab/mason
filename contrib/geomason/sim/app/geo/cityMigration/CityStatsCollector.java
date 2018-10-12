package sim.app.geo.cityMigration;

import java.util.ArrayList;
import java.util.List;

import masoncsc.datawatcher.DataWatcher;
import masoncsc.datawatcher.FileDataWriter;
import masoncsc.datawatcher.ListDataWatcher;
import riftland.PopulationCenter;
import sim.engine.SimState;
import sim.engine.Steppable;

public class CityStatsCollector implements Steppable
{
	private static final long serialVersionUID = 1L;
	FileDataWriter fileDW;
	DataWatcher<List<Integer>> dataWatcher;
	String filename;
	
	public CityStatsCollector(final CityMigrationModel model, String filename) {
		this.filename = filename;
		fileDW = new FileDataWriter();
		
		dataWatcher = new ListDataWatcher<Integer>() {
			{ addListener(fileDW); }

			@Override
			public String getCSVHeader() {
				StringBuilder sb = new StringBuilder("Step");
				for (PopulationCenter pc : model.populationCenters.values())
					sb.append(",").append(pc.getName());
				
				return sb.toString();
			}

			@Override
			protected void updateDataPoint() {
				dataList.clear();
				dataList.add((int)model.schedule.getSteps());
				for (PopulationCenter pc : model.populationCenters.values())
					dataList.add(pc.getNumRefugees());
				
			}
		};
	}
	
	public void start() {
		fileDW.InitFileDataWriter(filename, dataWatcher);
	}

	@Override
	public void step(SimState state) {
		dataWatcher.update();
	}
	
	public void close() {
		fileDW.close();
	}

}
