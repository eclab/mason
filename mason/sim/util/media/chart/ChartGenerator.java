/*
  Copyright 2006 by Sean Luke
  Licensed under the Academic Free License version 3.0
  See the file "LICENSE" for more information
*/

package sim.util.media.chart;

import java.awt.*;
import java.awt.geom.*;
import java.util.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.border.*;
import javax.swing.event.*;
import java.io.*;

// From MASON (cs.gmu.edu/~eclab/projects/mason/)
import sim.util.gui.LabelledList;
import sim.util.gui.NumberTextField;
import sim.util.gui.PropertyField;

// From JFreeChart (jfreechart.org)
import org.jfree.data.xy.*;
import org.jfree.chart.*;
import org.jfree.chart.axis.*;
import org.jfree.chart.event.*;
import org.jfree.chart.plot.*;
import org.jfree.data.general.*;
import org.jfree.chart.renderer.xy.*;
import org.jfree.data.general.*;
import org.jfree.chart.title.*;

// from iText (www.lowagie.com/iText/)
import com.lowagie.text.*;
import com.lowagie.text.pdf.*;

/**
   ChartGenerator is a JPanel which displays a chart using the JFreeChart library.  The class is abstract:
   you'll need to use a concrete subclass to build a specific kind of chart.
   The facility allows multiple time series to be displayed at one time, to be exported to PDF,
   and to be dynamically added and removed.
   
   <p>Subclasses need to override at least four methods: getSeriesDataset(), update(), removeSeries(index), and buildChart().
   Note that ChartGenerator has no standard API for <i>adding</i> a series to the chart, nor any standard way to modify
   this series once it has been added.  This is because JFreeChart has non-standard, non-consistent APIs for different
   kinds of charts.  You will need to implement these on a per-chart basis as you see fit.
   
   <p>ChartGenerator displays three regions:
   
   <p><ul>
   <li>The <tt>chart</tt> proper, stored in a <tt>chartPanel</tt>.  This panel is in turn stored in a JScrollPane.
   <li>The <tt>globalAttributes</tt>, a collection of Components on the top-left which control global features
   of the chart (its title, axis labels, etc.)
   <li>The <tt>seriesAttributes</tt>, a scrollable collection of Components on the bottom-left which control features
   of each separate series in the chart.  Each seriesAttribute is associated in turn with a Stoppable (stored in
   the list <tt>stoppables</tt>) which will have its <tt>stop()</tt> method called when the series is deleted from
   the chart.
   </ul>
                
*/

public abstract class ChartGenerator extends JPanel
    {
    /** A holder for global attributes components */
    protected Box globalAttributes = Box.createVerticalBox();
    /** A holder for series attributes components */
    protected Box seriesAttributes = Box.createVerticalBox();
        
    public SeriesAttributes getSeriesAttributes(int seriesIndex)
        {
        Component[] c = seriesAttributes.getComponents();
        return (SeriesAttributes)c[seriesIndex];
        }
    
    /** The chart */
    protected JFreeChart chart;
    /** The panel which holds and draws the chart */
    protected ChartPanel chartPanel;
    /** The JScrollPane which holdw the ChartPanel */
    protected JScrollPane chartHolder = new JScrollPane();
    /** The JFrame which stores the whole chart.  Set in createFrame(), else null. */
    protected JFrame frame;
    /** The global attributes chart title field. */
    protected PropertyField titleField;
    /** The global attributes domain axis field. */
    protected PropertyField xLabel;
    /** The global attributes range axis field. */
    protected  PropertyField yLabel;
    
    /** The global attributes logarithmic range axis check box. */
    protected JCheckBox yLog;
    /** The global attributes logarithmic domain axis check box. */
    protected JCheckBox xLog;
    
    public void setXAxisLogScaled(boolean isLogScaled){xLog.setSelected(isLogScaled);}
    public boolean isXAxisLogScaled(){return xLog.isSelected();}
    public void setYAxisLogScaled(boolean isLogScaled){yLog.setSelected(isLogScaled);}
    public boolean isYAxisLogScaled(){return yLog.isSelected();}
    
    /** Override this to return the JFreeChart data set used by your Chart.  For example, time series charts
        might return the XYSeriesCollection. */ 
    public abstract AbstractSeriesDataset getSeriesDataset();
        
    /** Override this to update the chart to reflect new data. */
    public abstract void update();
        
    /** Override this to remove a series from the chart. */
    public abstract void removeSeries(int index);
    
    /** Override this to move a series relative to other series. */
    public abstract void moveSeries(int index, boolean up); 
                
    /** Override this to construct the appropriate kind of chart.  This is the first thing called from the constructor; so certain
        of your instance variables may not have been set yet and you may need to set them yourself.  */
    protected abstract void buildChart();
    
    /** Deletes all series from the chart. */
    public void removeAllSeries()
        {
        for(int x = getSeriesDataset().getSeriesCount()-1 ; x>=0 ; x--)
            removeSeries(x);
        }
        
    /** Prepares the chart to be garbage collected.  If you override this, be sure to call super.quit() */
    public void quit()
        {
        removeAllSeries();
        }

    /** Returns the ChartPanel holding the chart. */
    public ChartPanel getChartPanel() { return chartPanel; }

    /** Adds a global attribute panel to the frame */
    public void addGlobalAttribute(Component component)
        {
        globalAttributes.add(component);
        }

    /** Returns the global attribute panel of the given index. */
    public Component getGlobalAttribute(int index)
        {
        // at present we have a PDF button and a chart global panel --
        // then the global seriesAttributes start
        return globalAttributes.getComponent(index+2);
        }

    /** Returns the number of global attribute panels. */
    public int getGlobalAttributeCount()
        {
        // at present we have a PDF button and a chart global panel --
        // then the global seriesAttributes start
        return globalAttributes.getComponentCount()-2;
        }

    /** Remooves the global attribute at the given index and returns it. */
    public Component removeGlobalAttribute(int index)
        {
        Component component = getGlobalAttribute(index);
        globalAttributes.remove(index);
        return component;
        }
                
    /** Sets the title of the chart (and the window frame). */
    public void setTitle(String title)
        {
        chart.setTitle(title);
        chart.titleChanged(new TitleChangeEvent(new org.jfree.chart.title.TextTitle(title)));
        if (frame!=null) frame.setTitle(title);
        titleField.setValue(title);
        }

    /** Returns the title of the chart */
    public String getTitle()
        {
        return chart.getTitle().getText();
        }
                
    /** Sets the name of the Range Axis label -- usually this is the Y axis. */
    public void setRangeAxisLabel(String val)
        {
        XYPlot xyplot = (XYPlot)(chart.getPlot());
        xyplot.getRangeAxis().setLabel(val);
        xyplot.axisChanged(new AxisChangeEvent(xyplot.getRangeAxis()));
        yLabel.setValue(val);
        }
                
    /** Returns the name of the Range Axis Label -- usually this is the Y axis. */
    public String getRangeAxisLabel()
        {
        return ((XYPlot)(chart.getPlot())).getRangeAxis().getLabel();
        }
                
    /** Sets the name of the Domain Axis label  -- usually this is the X axis. */
    public void setDomainAxisLabel(String val)
        {
        XYPlot xyplot = (XYPlot)(chart.getPlot());
        xyplot.getDomainAxis().setLabel(val);
        xyplot.axisChanged(new AxisChangeEvent(xyplot.getDomainAxis()));
        xLabel.setValue(val);
        }
                
    /** Returns the name of the Domain Axis label -- usually this is the X axis. */
    public String getDomainAxisLabel()
        {
        return ((XYPlot)(chart.getPlot())).getDomainAxis().getLabel();
        }

    /** Generates a new ChartGenerator with a blank chart.  Before anything else, buildChart() is called.  */
    public ChartGenerator()
        {
        // create the chart
        buildChart();

        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, true);
        split.setBorder(new EmptyBorder(0,0,0,0));
        JScrollPane scroll = new JScrollPane();
        JPanel b = new JPanel();
        b.setLayout(new BorderLayout());
        b.add(seriesAttributes, BorderLayout.NORTH);
        b.add(new JPanel(), BorderLayout.CENTER);
        scroll.getViewport().setView(b);
        scroll.setBackground(getBackground());
        scroll.getViewport().setBackground(getBackground());
        JPanel p = new JPanel();
        p.setLayout(new BorderLayout());

        LabelledList list = new LabelledList("Chart");
        globalAttributes.add(list);
        
        JLabel j = new JLabel("Right-Click or Control-Click");
        j.setFont(j.getFont().deriveFont(10.0f).deriveFont(java.awt.Font.ITALIC));
        list.add(j);
        j = new JLabel("on Chart for More Options");
        j.setFont(j.getFont().deriveFont(10.0f).deriveFont(java.awt.Font.ITALIC));
        list.add(j);


/*
  titleField = new JTextField();
  titleField.setText(chart.getTitle().getText());
  titleField.addKeyListener(new KeyListener()
  {
  public void keyReleased(KeyEvent keyEvent) {}
  public void keyTyped(KeyEvent keyEvent) {}
  public void keyPressed(KeyEvent keyEvent)
  {
  if (keyEvent.getKeyCode() == KeyEvent.VK_ENTER)
  {
  setTitle(titleField.getText());
  }
  else if (keyEvent.getKeyCode() == KeyEvent.VK_ESCAPE)
  titleField.setText(getTitle());
  }
  });
  titleField.addFocusListener(new FocusAdapter()
  {
  public void focusLost ( FocusEvent e )
  {
  setTitle(titleField.getText());
  }
  });
*/

        titleField = new PropertyField()
            {
            public String newValue(String newValue)
                {
                setTitle(newValue);
                getChartPanel().repaint();
                return newValue;
                }
            };
        titleField.setValue(chart.getTitle().getText());

        list.add(new JLabel("Title"), titleField);

/*
  xLabel = new JTextField();
  xLabel.setText(getDomainAxisLabel());
  xLabel.addKeyListener(new KeyListener()
  {
  public void keyReleased(KeyEvent keyEvent) {}
  public void keyTyped(KeyEvent keyEvent) {}
  public void keyPressed(KeyEvent keyEvent)
  {
  if (keyEvent.getKeyCode() == KeyEvent.VK_ENTER)
  {
  setDomainAxisLabel(xLabel.getText());
  }
  else if (keyEvent.getKeyCode() == KeyEvent.VK_ESCAPE)
  xLabel.setText(getDomainAxisLabel());
  }
  });
  xLabel.addFocusListener(new FocusAdapter()
  {
  public void focusLost ( FocusEvent e )
  {
  setDomainAxisLabel(xLabel.getText());
  }
  });
*/
        xLabel = new PropertyField()
            {
            public String newValue(String newValue)
                {
                setDomainAxisLabel(newValue);
                getChartPanel().repaint();
                return newValue;
                }
            };
        xLabel.setValue(getDomainAxisLabel());

        list.add(new JLabel("X Label"), xLabel);
        
/*
  yLabel = new JTextField();
  yLabel.setText(getRangeAxisLabel());
  yLabel.addKeyListener(new KeyListener()
  {
  public void keyReleased(KeyEvent keyEvent) {}
  public void keyTyped(KeyEvent keyEvent) {}
  public void keyPressed(KeyEvent keyEvent)
  {
  if (keyEvent.getKeyCode() == KeyEvent.VK_ENTER)
  {
  setRangeAxisLabel(yLabel.getText());
  }
  else if (keyEvent.getKeyCode() == KeyEvent.VK_ESCAPE)
  yLabel.setText(getRangeAxisLabel());
  }
  });
  yLabel.addFocusListener(new FocusAdapter()
  {
  public void focusLost ( FocusEvent e )
  {
  setRangeAxisLabel(yLabel.getText());
  }
  });
*/
        yLabel = new PropertyField()
            {
            public String newValue(String newValue)
                {
                setRangeAxisLabel(newValue);
                getChartPanel().repaint();
                return newValue;
                }
            };
        yLabel.setValue(getRangeAxisLabel());
        
        list.add(new JLabel("Y Label"), yLabel);
        
        xLog = new JCheckBox();
        xLog.addChangeListener(new ChangeListener(){
            public void stateChanged(ChangeEvent e)
                {
                if(xLog.isSelected())
                    {
                    LogarithmicAxis logAxis = new LogarithmicAxis(xLabel.getValue());
                    logAxis.setStrictValuesFlag(false);
                    chart.getXYPlot().setDomainAxis(logAxis);
                    }
                else
                    chart.getXYPlot().setDomainAxis(new NumberAxis(xLabel.getValue()));
                }
            });
        list.add(new JLabel("Log X axis"), xLog);
        

        yLog = new JCheckBox();
        yLog.addChangeListener(new ChangeListener(){
            public void stateChanged(ChangeEvent e)
                {
                if(yLog.isSelected())
                    {
                    LogarithmicAxis logAxis = new LogarithmicAxis(yLabel.getValue());
                    logAxis.setStrictValuesFlag(false);
                    chart.getXYPlot().setRangeAxis(logAxis);
                    }
                else
                    chart.getXYPlot().setRangeAxis(new NumberAxis(yLabel.getValue()));
                }
            });
        list.add(new JLabel("Log Y axis"), yLog);

        final JCheckBox legendCheck = new JCheckBox();
        legendCheck.setSelected(false);
        ItemListener il = new ItemListener()
            {
            public void itemStateChanged(ItemEvent e)
                {
                if (e.getStateChange() == ItemEvent.SELECTED)
                    {
                    LegendTitle title = new LegendTitle(chart.getXYPlot());
                    title.setLegendItemGraphicPadding(new org.jfree.ui.RectangleInsets(0,8,0,4));
                    chart.addLegend(title);
                    }
                else
                    {
                    chart.removeLegend();
                    }
                }
            };
        legendCheck.addItemListener(il);
        list.add(new JLabel("Legend"), legendCheck);

        final JCheckBox aliasCheck = new JCheckBox();
        aliasCheck.setSelected(chart.getAntiAlias());
        il = new ItemListener()
            {
            public void itemStateChanged(ItemEvent e)
                {
                chart.setAntiAlias( e.getStateChange() == ItemEvent.SELECTED );
                }
            };
        aliasCheck.addItemListener(il);
        list.add(new JLabel("Antialias"), aliasCheck);

        JPanel pdfButtonPanel = new JPanel();
        pdfButtonPanel.setLayout(new BorderLayout());
        JButton pdfButton = new JButton( "Save as PDF" );
        pdfButtonPanel.add(pdfButton,BorderLayout.WEST);
        pdfButton.addActionListener(new ActionListener()
            {
            public void actionPerformed ( ActionEvent e )
                {
                FileDialog fd = new FileDialog(frame,"Choose PDF file...", FileDialog.SAVE);
                fd.setFile(chart.getTitle().getText() + ".pdf");
                fd.setVisible(true);;
                String fileName = fd.getFile();
                if (fileName!=null)
                    {
                    Dimension dim = chartPanel.getPreferredSize();
                    generatePDF( chart, dim.width, dim.height, fd.getDirectory() + fileName );
                    } 
                }
            });
        globalAttributes.add(pdfButtonPanel);
                
        // we add into an outer box so we can later on add more global seriesAttributes
        // as the user instructs and still have glue be last
        Box outerAttributes = Box.createVerticalBox();
        outerAttributes.add(globalAttributes);
        outerAttributes.add(Box.createGlue());

        p.add(outerAttributes,BorderLayout.NORTH);
        p.add(scroll,BorderLayout.CENTER);
        p.setMinimumSize(new Dimension(0,0));
        p.setPreferredSize(new Dimension(200,0));
        split.setLeftComponent(p);
                
        chartHolder.setMinimumSize(new Dimension(0,0));
        split.setRightComponent(chartHolder);
        setLayout(new BorderLayout());
        add(split,BorderLayout.CENTER);
        
        // set the default to be white, which looks good when printed
        chart.setBackgroundPaint(Color.WHITE);
        }
    
    /** Returns a JFrame suitable or housing the ChartGenerator.  This frame largely calls chart.quit() when
        the JFrame is being closed. */
    public JFrame createFrame( final sim.display.GUIState state )
        {
        frame = new JFrame()
            {
            public void dispose()
                {
                quit();
                super.dispose();
                }
            };
            
        // these bugs are tickled by our constant redraw requests.
        frame.addComponentListener(new ComponentAdapter()
            {
            // Bug in MacOS X Java 1.3.1 requires that we force a repaint.
            public void componentResized (ComponentEvent e) 
                {
                // Utilities.doEnsuredRepaint(ChartGenerator.this);
                }
            });

        frame.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        frame.getContentPane().setLayout(new BorderLayout());
        frame.getContentPane().add(this,BorderLayout.CENTER);
        frame.setResizable(true);
        frame.pack();
        frame.setTitle(chart.getTitle().getText());
        return frame;
        }
    
    /* Generates PDF from the chart, saving ou to the given file.  width and height are the
       desired width and height of the chart in points. */
    void generatePDF( JFreeChart chart, int width, int height, String fileName )
        {
        try
            {
            Document document = new Document(new com.lowagie.text.Rectangle(width,height));
            PdfWriter writer = PdfWriter.getInstance(document, new FileOutputStream(fileName));
            document.addAuthor("MASON");
            document.open();
            PdfContentByte cb = writer.getDirectContent();
            PdfTemplate tp = cb.createTemplate(width, height); 
            Graphics2D g2 = tp.createGraphics(width, height, new DefaultFontMapper());
            Rectangle2D rectangle2D = new Rectangle2D.Double(0, 0, width, height); 
            chart.draw(g2, rectangle2D);
            g2.dispose();
            cb.addTemplate(tp, 0, 0);
            document.close();
            }
        catch( Exception e )
            {
            e.printStackTrace();
            }
        }
    
    static
        {
        // quaquaify
        try
            {
            String version = System.getProperty("java.version");
            // both of the following will generate exceptions if the class doesn't exist, so we're okay
            // trying to put them in the UIManager here
            if (version.startsWith("1.3"))
                {
                // broken
                //UIManager.put("ColorChooserUI", 
                //      Class.forName("ch.randelshofer.quaqua.Quaqua13ColorChooserUI").getName());
                }
            else // hope there's no one using 1.2! 
                UIManager.put("ColorChooserUI", 
                    Class.forName("ch.randelshofer.quaqua.Quaqua14ColorChooserUI").getName());
            }
        catch (Exception e) { }
        }

    }
