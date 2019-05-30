package sim.app.geo.masoncsc.gui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.EventQueue;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.Random;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JPanel;
import javax.swing.Timer;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.DateAxis;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.SeriesRenderingOrder;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.StackedXYAreaRenderer;
import org.jfree.data.time.SimpleTimePeriod;
import org.jfree.data.time.TimePeriod;
import org.jfree.data.time.TimeTableXYDataset;
import org.jfree.ui.ApplicationFrame;
import org.jfree.ui.RefineryUtilities;

public class StackedAreaTimeSeriesChartDemo extends ApplicationFrame
{
	private static final long serialVersionUID = 1L;

	private static final String TITLE = "Dynamic Series";
	private static final String START = "Start";
	private static final String STOP = "Stop";
	private static final float MINMAX = 100;
	private static final int COUNT = 600;
	private static final int FAST = 10;
	private static final int SLOW = FAST * 5;
	private static final Random random = new Random();
	private Timer timer;
	private static final String SERIES1 = "Positive";
	private static final String SERIES2 = "Negative";

	// Added by JFH:
	long step = 0; // current step
	public GregorianCalendar date = new GregorianCalendar(1970, 0, 1);

	public StackedAreaTimeSeriesChartDemo(final String title) {
		super(title);
		final TimeTableXYDataset dataset = new TimeTableXYDataset();
		JFreeChart chart = createAreaChart(dataset);

		final JButton run = new JButton(STOP);
		run.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				String cmd = e.getActionCommand();
				if (STOP.equals(cmd)) {
					timer.stop();
					run.setText(START);
				}
				else {
					timer.start();
					run.setText(STOP);
				}
			}
		});

		final JComboBox<String> combo = new JComboBox<String>();
		combo.addItem("Fast");
		combo.addItem("Slow");
		combo.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				if ("Fast".equals(combo.getSelectedItem())) {
					timer.setDelay(FAST);
				}
				else {
					timer.setDelay(SLOW);
				}
			}
		});

		this.add(new ChartPanel(chart), BorderLayout.CENTER);
		JPanel btnPanel = new JPanel(new FlowLayout());
		btnPanel.add(run);
		btnPanel.add(combo);
		this.add(btnPanel, BorderLayout.SOUTH);

		timer = new Timer(FAST, new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				long timeInMS = date.getTimeInMillis();
				final SimpleTimePeriod timePeriod = new SimpleTimePeriod(timeInMS, timeInMS);
				step++;
				date.add(Calendar.MONTH, 1);

				dataset.add(timePeriod, 100 + randomValue(), SERIES1);
				dataset.add(timePeriod, 100 + randomValue(), SERIES2);

				// cull
				if (dataset.getItemCount() > COUNT) {
					TimePeriod firstItemTime = dataset.getTimePeriod(0);
					dataset.remove(firstItemTime, SERIES1);
					dataset.remove(firstItemTime, SERIES2);
				}
			}
		});
	}

	private float randomValue() {
		float randValue = (float) (random.nextGaussian() * MINMAX / 3);
		return randValue < 0 ? -randValue : randValue;
	}

	private JFreeChart createAreaChart(final TimeTableXYDataset dataset) {
		final JFreeChart chart = ChartFactory.createStackedXYAreaChart("Live Sentiment Chart", "Time", "Sentiments", dataset, PlotOrientation.VERTICAL, true, true, false);

		final StackedXYAreaRenderer render = new StackedXYAreaRenderer();
		render.setSeriesPaint(0, Color.RED);
		render.setSeriesPaint(1, Color.GREEN);

		// commented out by JFH
		// DateAxis domainAxis = new DateAxis();
		// domainAxis.setAutoRange(true);
		// domainAxis.setDateFormatOverride(new SimpleDateFormat("HH:mm:ss"));
		// domainAxis.setTickUnit(new DateTickUnit(DateTickUnitType.SECOND, 1));

		DateAxis domainAxis = new DateAxis();
//		domainAxis.setLowerBound(date.getTimeInMillis());
		domainAxis.setLowerMargin(0.01);
		domainAxis.setAutoRange(true);
		domainAxis.setDateFormatOverride(new SimpleDateFormat("MMM YYYY"));
//		domainAxis.setTickUnit(new DateTickUnit(DateTickUnitType.MONTH, 12));
		domainAxis.setVerticalTickLabels(true);

		XYPlot plot = (XYPlot) chart.getPlot();
		plot.setRenderer(render);
		plot.setDomainAxis(domainAxis);
		plot.setSeriesRenderingOrder(SeriesRenderingOrder.FORWARD);
		plot.setForegroundAlpha(0.5f);

		NumberAxis rangeAxis = (NumberAxis) plot.getRangeAxis();
		rangeAxis.setNumberFormatOverride(new DecimalFormat("#,###.#"));
		rangeAxis.setAutoRange(true);

		return chart;
	}

	public void start() {
		timer.start();
	}

	public static void main(final String[] args) {
		EventQueue.invokeLater(new Runnable() {

			@Override
			public void run() {
				StackedAreaTimeSeriesChartDemo demo = new StackedAreaTimeSeriesChartDemo(TITLE);
				demo.pack();
				RefineryUtilities.centerFrameOnScreen(demo);
				demo.setVisible(true);
				demo.start();
			}
		});
	}
}
