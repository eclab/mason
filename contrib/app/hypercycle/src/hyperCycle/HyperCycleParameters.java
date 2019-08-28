/*
 Hypercycle simulation. Copyright by Jšrg Hšhne.
 For suggestions or questions email me at hoehne@thinktel.de
 */
package hyperCycle;

import java.awt.Color;
import java.util.Random;

/**
 * This class just provides the main parameters for the simulation.
 * 
 * @author hoehne
 * 
 */
public class HyperCycleParameters {

	final static Color colorBlack = Color.black;
	final static Color colorRed = Color.red;
	final static Color colorGreen = new Color(0x00, 0xc0, 0x00);
	final static Color colorLightGreen = new Color(0x60, 0xc0, 0x60);
	final static Color colorBlue = new Color(0x00, 0x00, 0xc0);
	final static Color colorLightBlue = new Color(0x60, 0x60, 0xc0);
	final static Color colorPurple = new Color(0xc0, 0x00, 0xc0);
	final static Color colorLightPurple = new Color(0xc0, 0x60, 0xc0);
	final static Color colorGrey = new Color(0x60, 0x60, 0x60);
	final static Color colorLightGrey = new Color(0xc0, 0xc0, 0xc0);
	final static Color colorYellow = new Color(0x60, 0x60, 0x00);

	public static Color colorTable[] = { colorBlack, colorRed, colorGreen,
			colorLightGreen, colorBlue, colorLightBlue, colorPurple,
			colorLightPurple, colorGrey, colorLightGrey, colorYellow };

	private int gridWidth = 256;
	private int gridHeight = 256;

	public Random r = new Random();

	/**
	 * The number of states used in the model. State 0 is always the background
	 * = empty, state 1 is always the parasite.
	 */
	public int states = 11;

	/**
	 * The rate an empty cell (state 0) stays empty.
	 */
	public int aEmpty = 11;

	/**
	 * The self replication rate for every state.
	 */
	public int replication[] = { 0, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1 };

	/**
	 * The probability a molecules of the state i will decay to state 0
	 */
	public float decays[] = { 0.0f, 0.2f, 0.2f, 0.2f, 0.2f, 0.2f, 0.2f, 0.2f,
			0.2f, 0.2f, 0.2f };

	/*
	 * this initialization is for demonstration purposes. This table will be
	 * computed by the settings of the sliders. This initialization demonstrates
	 * how the table will be setup for 11 states with a parasite state and the
	 * support-factor-parasite of 2.0.
	 */
	public int rSupport[][] = { { 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0 }, /*
																	 * state 0
																	 * (black)
																	 * will be
																	 * supported
																	 * by no one
																	 */
	{ 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 200 }, /*
											 * state 1 (parasite) will be
											 * supported by state 10 with factor
											 * 200
											 */
	{ 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 100 }, /*
											 * state 2 will be supported by
											 * state 10 with factor 100
											 */
	{ 0, 0, 100, 0, 0, 0, 0, 0, 0, 0, 0 }, /*
											 * state 3 will be supported by
											 * state 2 with factor 100
											 */
	{ 0, 0, 0, 100, 0, 0, 0, 0, 0, 0, 0 }, /*
											 * state 4 will be supported by
											 * state 3 with factor 100
											 */
	{ 0, 0, 0, 0, 100, 0, 0, 0, 0, 0, 0 }, /*
											 * state 5 will be supported by
											 * state 4 with factor 100
											 */
	{ 0, 0, 0, 0, 0, 100, 0, 0, 0, 0, 0 }, /*
											 * state 6 will be supported by
											 * state 5 with factor 100
											 */
	{ 0, 0, 0, 0, 0, 0, 100, 0, 0, 0, 0 }, /*
											 * state 7 will be supported by
											 * state 6 with factor 100
											 */
	{ 0, 0, 0, 0, 0, 0, 0, 100, 0, 0, 0 }, /*
											 * state 8 will be supported by
											 * state 7 with factor 100
											 */
	{ 0, 0, 0, 0, 0, 0, 0, 0, 100, 0, 0 }, /*
											 * state 9 will be supported by
											 * state 8 with factor 100
											 */
	{ 0, 0, 0, 0, 0, 0, 0, 0, 0, 100, 0 } /*
										 * state 10 will be supported by state 9
										 * with factor 100
										 */
	};

	// =================== Getters and Setters for reflection =================
	public int getWidth() {
		return gridWidth;
	}

	public void setWidth(int val) {
		if (val > 0)
			this.gridWidth = val;
	}

	public int getHeight() {
		return gridHeight;
	}

	public void setHeight(int val) {
		if (val > 0)
			this.gridHeight = val;
	}

	/**
	 * @return the decays
	 */
	public final float[] getDecays() {
		return decays;
	}

	/**
	 * @param decays
	 *            the decays to set
	 */
	public final void setDecays(float[] decays) {
		this.decays = decays;
	}

	/**
	 * @return the rSupport
	 */
	public final int[][] getRSupport() {
		return rSupport;
	}

	/**
	 * @param support
	 *            the rSupport to set
	 */
	public final void setRSupport(int[][] support) {
		this.rSupport = support;
	}
}
