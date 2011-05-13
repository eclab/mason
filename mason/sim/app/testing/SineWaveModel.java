package sim.app.testing;

import sim.engine.SimState;
import sim.engine.Steppable;
import sim.util.Interval;

public class SineWaveModel extends SimState
{
	public double value = 0;
	public double getValue() {	return value; }
	public void setValue(double val) { value = val; }
	
	public double frequency = 0.2;
	public double getFrequency() {	return frequency; }
	public void setFrequency(double val) { frequency = val; }
	public Object domFrequency() { return new Interval(0.0, 1.0); }
	
	public double amplitude = 1.0;
	public double getAmplitude() {	return amplitude; }
	public void setAmplitude(double val) { amplitude = val; }
	public Object domAmplitude() { return new Interval(0.0, 2.0); }

	public SineWaveModel(long seed) {
		super(seed);
	}

	@Override
	public void start() {
		super.start();
		
		schedule.scheduleRepeating(new Steppable() {
			public void step(SimState state) {
				value = Math.sin(state.schedule.getTime() * frequency) * amplitude;								
			}});
	}	

	public static void main(String[] args) {
        doLoop(SineWaveModel.class, args);
        System.exit(0);
	}

}
