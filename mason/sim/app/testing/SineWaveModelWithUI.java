package sim.app.testing;

import java.awt.BorderLayout;
import java.awt.Color;

import javax.swing.JFrame;

import org.jfree.data.xy.XYSeries;

import sim.display.Controller;
import sim.display.GUIState;
import sim.engine.SimState;
import sim.engine.Steppable;
import sim.util.media.chart.ChartGenerator;
import sim.util.media.chart.TimeSeriesChartGenerator;

public class SineWaveModelWithUI extends GUIState
{
	public TimeSeriesChartGenerator chart;
	public JFrame chartFrame;
	public XYSeries sineWaveSeries = new XYSeries("Sine Wave");	
    
    double lastTime = 0, time = 0;
    
	public SineWaveModelWithUI() {
		super(new SineWaveModel(System.currentTimeMillis()));
	}

	public SineWaveModelWithUI(SimState state) {
		super(state);
	}

	public static String getName() {
		return "Sine Wave";
	}

	public Object getSimulationInspectedObject() {
		return state;
	}

	public void start() {
		super.start();
		setupPortrayals();

		sineWaveSeries.clear();
	}

	public void load(SimState state) {
		super.load(state);
		setupPortrayals();
	}

	@SuppressWarnings("serial")
	public void setupPortrayals() {
		this.scheduleRepeatingImmediatelyAfter(new Steppable() {			
			@Override
			public void step(SimState state) {
				sineWaveSeries.add(state.schedule.getTime(), ((SineWaveModel)state).value);
				
				// without this update the video contains only one frame,
				// with the update there are many frames, but terrible flicker
				//chart.update(state.schedule.getSteps(), true);
			}			
		});

		chart.repaint();
	}

	public void init(final Controller c) {
		super.init(c);
		
		c.registerFrame(createGrievanceFrame());
		chartFrame.setVisible(true);
	}
	
	public JFrame createGrievanceFrame() {

		sineWaveSeries = new XYSeries("Sine Wave");
		chart = new TimeSeriesChartGenerator();
		chart.setTitle("Sine Wave");
		chart.setDomainAxisLabel("Time");
		chart.setRangeAxisLabel("Amplitude");
		chart.addSeries(sineWaveSeries, null).setStrokeColor(Color.black);
		
		chartFrame = chart.createFrame(this);
		chartFrame.getContentPane().setLayout(new BorderLayout());
		chartFrame.getContentPane().add(chart, BorderLayout.CENTER);
		chartFrame.pack();
	
		return chartFrame;
	}

	public void quit() {
		super.quit();
		
		if (chartFrame != null)	chartFrame.dispose();
		chartFrame = null;
	}
	
	public static void main(String[] args) {
		new SineWaveModelWithUI().createController();
	}

}
