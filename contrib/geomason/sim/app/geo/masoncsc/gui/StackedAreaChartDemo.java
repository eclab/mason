package sim.app.geo.masoncsc.gui;


import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.EventQueue;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Random;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JPanel;
import javax.swing.Timer;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.SeriesRenderingOrder;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.StackedXYAreaRenderer;
import org.jfree.data.xy.DefaultTableXYDataset;
import org.jfree.data.xy.XYSeries;
import org.jfree.ui.ApplicationFrame;
import org.jfree.ui.RefineryUtilities;

public class StackedAreaChartDemo extends ApplicationFrame {
	private static final long serialVersionUID = 1L;
	
	private static final String TITLE = "Dynamic Series";
    private static final String START = "Start";
    private static final String STOP = "Stop";
    private static final float MINMAX = 100;
    private static final int COUNT = 50;
    private static final int FAST = 100;
    private static final int SLOW = FAST * 5;
    private static final Random random = new Random();
    private Timer timer;
    private static final String SERIES1 = "Positive";
    private static final String SERIES2 = "Negative";
    

	int step = 0;
	double time = 0.0;
    XYSeries series1 = new XYSeries("One", false, false);
    XYSeries series2 = new XYSeries("Two", false, false);     
    XYSeries series3 = new XYSeries("Three", false, false);
    
    JFreeChart chart;

    public StackedAreaChartDemo(final String title) {
        super(title);   

        final DefaultTableXYDataset dataset = new DefaultTableXYDataset();
//        final DefaultTableXYDataset dataset = new DefaultTableXYDataset() {
//
//        	double largestX = Double.NEGATIVE_INFINITY;
//			@Override
//			public void seriesChanged(SeriesChangeEvent event) {        
//				if (this.propagateEvents) {
//	            double x = ((XYSeries)event.getSource()).getMaxX();
//	            if (x > largestX) {
//	               largestX = x;
//	               updateXPoints();
//	            }
//	            fireDatasetChanged();
//	        }
//			}
//        	
//        };
        chart = createAreaChart(dataset);
        
          
        dataset.addSeries(series1);
        dataset.addSeries(series2);
        dataset.addSeries(series3);

        final JButton run = new JButton(STOP);
        run.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String cmd = e.getActionCommand();
                if (STOP.equals(cmd)) {
                    timer.stop();
                    run.setText(START);
                } else {
                    timer.start();
                    run.setText(STOP);
                }
            }
        });
        
        final JButton btnStep = new JButton("Step");
        btnStep.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
            	addData();
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
                } else {
                    timer.setDelay(SLOW);
                }
            }
        });

        this.add(new ChartPanel(chart), BorderLayout.CENTER);
        JPanel btnPanel = new JPanel(new FlowLayout());
        btnPanel.add(run);
        btnPanel.add(btnStep);
        btnPanel.add(combo);
        this.add(btnPanel, BorderLayout.SOUTH);
        
        timer = new Timer(FAST, new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
            	addData();
            }
        });
    }
    
    private void addData() {
//    	System.out.format("Step %d: addData\n", step);

    	series1.addOrUpdate(time, 100 + randomValue());
    	if ((step % 5) != 0)
    		series2.addOrUpdate(time, 100 + randomValue());
    	if ((step % 3) == 0)
    		series3.addOrUpdate(time, 100 + randomValue());
        time += 1;
        step++;
    }

    private float randomValue() {
        float randValue = (float) (random.nextGaussian() * MINMAX / 3);
        return randValue < 0 ? -randValue : randValue;
    }

    private JFreeChart createAreaChart(final DefaultTableXYDataset dataset) {
        final JFreeChart chart = ChartFactory.createStackedXYAreaChart(
                "Live Sentiment Chart", "Time", "Sentiments", dataset, PlotOrientation.VERTICAL, true, true, false);

        final StackedXYAreaRenderer render = new StackedXYAreaRenderer();
        render.setSeriesPaint(0, Color.RED);
        render.setSeriesPaint(1, Color.GREEN);
        render.setSeriesPaint(2, Color.BLUE);

		// commented out by JFH
//        DateAxis domainAxis = new DateAxis();
//        domainAxis.setAutoRange(true);
//        domainAxis.setDateFormatOverride(new SimpleDateFormat("HH:mm:ss"));
//        domainAxis.setTickUnit(new DateTickUnit(DateTickUnitType.SECOND, 1));

        XYPlot plot = (XYPlot) chart.getPlot();
        plot.setRenderer(render);
//        plot.setDomainAxis(domainAxis);
        plot.setSeriesRenderingOrder(SeriesRenderingOrder.FORWARD);
        plot.setForegroundAlpha(0.5f);

        NumberAxis rangeAxis = (NumberAxis) plot.getRangeAxis();
//        rangeAxis.setNumberFormatOverride(new DecimalFormat("#,###.#"));
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
            	StackedAreaChartDemo demo = new StackedAreaChartDemo(TITLE);
                demo.pack();
                RefineryUtilities.centerFrameOnScreen(demo);
                demo.setVisible(true);
                demo.start();
            }
        });
    }
}
