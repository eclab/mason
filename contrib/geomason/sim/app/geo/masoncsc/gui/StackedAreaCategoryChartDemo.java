package masoncsc.gui;


import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.EventQueue;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Random;

import javax.swing.JButton;
import javax.swing.JCheckBox;
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
import org.jfree.data.xy.TableXYDataset;
import org.jfree.ui.ApplicationFrame;
import org.jfree.ui.RefineryUtilities;

public class StackedAreaCategoryChartDemo extends ApplicationFrame {
	private static final long serialVersionUID = 1L;
	
	private static final String TITLE = "Dynamic Series";
    private static final String START = "Start";
    private static final String STOP = "Stop";
    private static final float MINMAX = 100;
    private static final int COUNT = 50;	// moving window size, not currently used
    private static final int FAST = 1;
    private static final int MEDIUM = FAST * 5;
    private static final int SLOW = FAST * 20;
    private static final Random random = new Random();
    private Timer timer;
    
    private String[] seriesNames = new String[] { "One", "Two", "Three" };    

	int step = 0;
	double time = 0.0;
	int timingInterval = 100;
	int cullingInterval = 1000;
	long timestamp;
	boolean autoCull = true;
	boolean useCull2 = false;
	int cullMethod = 0;

    final CategoryTableXYDatasetAlt dataset = new CategoryTableXYDatasetAlt();
    
    JFreeChart chart;

    public StackedAreaCategoryChartDemo(final String title) {
        super(title);   

        chart = createAreaChart(dataset);
        timestamp = System.nanoTime();

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
        
        final JButton btnCull = new JButton("Cull");
        btnCull.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
            	cullData();
			}
		});
        
        final JCheckBox chkAutoCull = new JCheckBox("Auto Cull", true);
        chkAutoCull.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				autoCull = chkAutoCull.isSelected();
			}
		});
        
        final JCheckBox chkUseCull2 = new JCheckBox("Use Cull2", false);
        chkUseCull2.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				useCull2 = chkUseCull2.isSelected();
			}
		});
        
        final JButton btnClear = new JButton("Clear");
        btnClear.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
            	clearData();
			}
		});
        

        final JComboBox<String> cmbCullMethod = new JComboBox<String>();
        cmbCullMethod.addItem("cullData1");
        cmbCullMethod.addItem("cullData2");
        cmbCullMethod.addItem("cullData3");
        cmbCullMethod.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				cullMethod = cmbCullMethod.getSelectedIndex();
			}
		});
        

        final JComboBox<String> combo = new JComboBox<String>();
        combo.addItem("Fast");
        combo.addItem("Medium");
        combo.addItem("Slow");
        combo.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if ("Fast".equals(combo.getSelectedItem())) {
                    timer.setDelay(FAST);
                } 
                else if ("Medium".equals(combo.getSelectedItem())) {
                    timer.setDelay(MEDIUM);
                }
                else {
                    timer.setDelay(SLOW);
                }
            }
        });

        this.add(new ChartPanel(chart), BorderLayout.CENTER);
        JPanel btnPanel = new JPanel(new FlowLayout());
        btnPanel.add(run);
        btnPanel.add(btnStep);
        btnPanel.add(btnCull);
        btnPanel.add(chkAutoCull);
        btnPanel.add(cmbCullMethod);
        btnPanel.add(btnClear);
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
    	for (int i = 0; i < seriesNames.length; i++)
    		dataset.add(time, 100 + randomValue(), seriesNames[i]);
    	
        time += 1;	// for trying non-integer time values
        step++;
        
        if (autoCull && dataset.getItemCount() >= cullingInterval) {
        	cullData();
        }
        
        if (step % timingInterval == 0) {
//        	long prevTimestamp = timestamp;
        	timestamp = System.nanoTime();
        	
//        	System.out.format("%.3f ms per step, %d values\n", 
//        			(1.0e-7*(timestamp - prevTimestamp))/timingInterval, dataset.getItemCount());
        }
    }
    
    /**
     * Cull the data by looping backwards and removing every second point.
     */
    private void cullData() { 
		long start = System.nanoTime();

		switch (cullMethod) {
		case 0: cullData1(); break;
		case 1: cullData2(); break;
		case 2: cullData3(); break;
		}
		
		long elapsed = System.nanoTime() - start;
		System.out.format("Culling%d: %.3f ms\n", cullMethod+1, 1.0e-7 * elapsed);
    }
    
    /**
     * Cull the data by looping backwards and removing every second point.
     */
    private void cullData1() {    
    	int numValues = dataset.getItemCount();	
    	// loop backward, removing every second x-value. Don't remove the first or last.
    	for (int i = numValues-2; i > 0; i -= 2) {
    		Number x = dataset.getX(0, i);

        	for (int j = 0; j < seriesNames.length; j++)
        		dataset.remove(x, seriesNames[j], false);
    	}
   	
    	dataset.setNotify(true);
    }
    
    /**
     * Cull the data by looping backwards and removing every second point.
     */
    private void cullData3() {    
    	int numValues = dataset.getItemCount();	
    	// loop backward, removing every second x-value. Don't remove the first or last.
    	for (int i = numValues-2; i > 0; i -= 2) {
    		dataset.removeItem(i); // this function is exposed in CategoryTableXYDatasetAlt
    	}
   	
    	dataset.setNotify(true);
    }
    
    /**
     * Cull the data by gathering the keepers, clearing the dataset, and refilling it from scratch.
     */
    private void cullData2() {
    	int numValues = dataset.getItemCount();
    	// loop by series first, then through the points
    	ArrayList<ArrayList<Number>> xKeepers = new ArrayList<ArrayList<Number>>();
    	ArrayList<ArrayList<Number>> yKeepers = new ArrayList<ArrayList<Number>>();
    	for (int j = 0; j < seriesNames.length; j++) {
    		ArrayList<Number> xs = new ArrayList<Number>();
    		ArrayList<Number> ys = new ArrayList<Number>();
    		for (int i = 0; i < numValues-1; i += 2) {
    			xs.add(dataset.getX(j, i));
    			ys.add(dataset.getY(j, i));
    		}

    		// add the last value
    		if (numValues > 1) {
				xs.add(dataset.getX(j, numValues-1));
				ys.add(dataset.getY(j, numValues-1));
    		}
    		
    		xKeepers.add(xs);
    		yKeepers.add(ys);
    	}
    	
    	dataset.clear();

    	for (int j = 0; j < seriesNames.length; j++) {
    		ArrayList<Number> xs = xKeepers.get(j);
    		ArrayList<Number> ys = yKeepers.get(j);
    		for (int i = 0; i < xs.size(); i++) {
    			dataset.add(xs.get(i), ys.get(i), seriesNames[j], false);
    		}
    	}
    	
    	dataset.setNotify(true);
    }
    
    private void clearData() {
    	dataset.clear();
    }

    private float randomValue() {
        float randValue = (float) (random.nextGaussian() * MINMAX / 3);
        return randValue < 0 ? -randValue : randValue;
    }

    private JFreeChart createAreaChart(final TableXYDataset dataset) {
        final JFreeChart chart = ChartFactory.createStackedXYAreaChart(
                "Live Sentiment Chart", "Time", "Sentiments", dataset, PlotOrientation.VERTICAL, true, true, false);

        final StackedXYAreaRenderer render = new StackedXYAreaRenderer();
        render.setSeriesPaint(0, Color.RED);
        render.setSeriesPaint(1, Color.GREEN);
        render.setSeriesPaint(2, Color.BLUE);

        XYPlot plot = (XYPlot) chart.getPlot();
        plot.setRenderer(render);
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
            	StackedAreaCategoryChartDemo demo = new StackedAreaCategoryChartDemo(TITLE);
                demo.pack();
                RefineryUtilities.centerFrameOnScreen(demo);
                demo.setVisible(true);
                demo.start();
            }
        });
    }
}