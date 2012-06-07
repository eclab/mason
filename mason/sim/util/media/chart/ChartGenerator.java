/*
  Copyright 2006 by Sean Luke and George Mason University
  Licensed under the Academic Free License version 3.0
  See the file "LICENSE" for more information
*/

package sim.util.media.chart;

import java.awt.*;
import java.awt.geom.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.border.*;
import javax.swing.event.*;
import java.io.*;

import java.awt.image.*;

// From MASON (cs.gmu.edu/~eclab/projects/mason/)
import sim.util.gui.*;
import sim.util.gui.Utilities;
import sim.util.*;
import sim.display.*;
import sim.util.media.*;

// From JFreeChart (jfreechart.org)
import org.jfree.chart.*;
import org.jfree.chart.axis.*;
import org.jfree.chart.event.*;
import org.jfree.chart.plot.*;
import org.jfree.data.general.*;
import org.jfree.chart.title.*;
import org.jfree.data.xy.*;

// from iText (www.lowagie.com/iText/)
import com.lowagie.text.*;
import com.lowagie.text.pdf.*;

/*  // looks like we'll have to move to these soon
    import com.itextpdf.text.*;
    import com.itextpdf.text.pdf.*;
*/

/**
   ChartGenerator is a JPanel which displays a chart using the JFreeChart library.  The class is abstract:
   you'll need to use a concrete subclass to build a specific kind of chart.
   The facility allows multiple time series to be displayed at one time, to be exported to PDF,
   and to be dynamically added and removed.
   
   <p>Subclasses only really need to override one method: buildChart(), which creates the chart and the chartPanel and loads
   them into the chartHolder, then finally sets the series dataset.  Subclasses will also find it convenient to override
   update() to update the chart specially each time it's repainted, removeSeries(index) to remove a series, or moveSeries(index, boolean)
   to change the order of series.  In a subclass's constructor it may also modify the global attributes to make ones special
   to its kind of chart as well.  Finally, note that ChartGenerator has no standard API for <i>adding</i> a series to the chart, nor any standard way to modify
   this series once it has been added.  This is because JFreeChart has non-standard, non-consistent APIs for different
   kinds of charts.  You will need to implement these on a per-chart basis as you see fit.
   
   <p>ChartGenerator displays three regions:
   
   <p><ul>
   <li>The <tt>chart</tt> proper, stored in a <tt>chartPanel</tt>.  This panel is in turn stored in a JScrollPane.
   <li>The <tt>globalAttributes</tt>, a collection of Components on the top-left which control global features
   of the chart (its title, axis labels, etc.)
   <li>The <tt>seriesAttributes</tt>, a scrollable collection of Components on the bottom-left which control features
   of each separate series in the chart.
   </ul>
                
*/

public abstract class ChartGenerator extends JPanel
    {
    /** A holder for global attributes components */
    protected Box globalAttributes = Box.createVerticalBox();
    /** A holder for series attributes components */
    protected Box seriesAttributes = Box.createVerticalBox();
        
    /** The chart */
    protected JFreeChart chart;
    /** The panel which holds and draws the chart */
    protected ChartPanel chartPanel;
    /** The JScrollPane which holdw the ChartPanel */
    protected JScrollPane chartHolder = new JScrollPane();
    
        
    JFrame frame;
    /** Returns the JFrame which stores the whole chart.  Set in createFrame(), else null. */
    public JFrame getFrame() { return frame; }
        
    /** The global attributes chart title field. */
    PropertyField titleField;
    /** The global attributes domain axis field. */
    PropertyField xLabel;
    /** The global attributes range axis field. */
    PropertyField yLabel;
        
    /** The global attributes logarithmic range axis check box. */
    JCheckBox yLog;
    /** The global attributes logarithmic domain axis check box. */
    JCheckBox xLog;
    
    JButton movieButton = new JButton("Create Movie");
    BufferedImage buffer;
        
    public void setXAxisLogScaled(boolean isLogScaled){xLog.setSelected(isLogScaled);}
    public boolean isXAxisLogScaled(){return xLog.isSelected();}
    public void setYAxisLogScaled(boolean isLogScaled){yLog.setSelected(isLogScaled);}
    public boolean isYAxisLogScaled(){return yLog.isSelected();}
        
    public XYDataset getSeriesDataset() { return ((XYPlot)(chart.getPlot())).getDataset(); }
    public void setSeriesDataset(XYDataset obj) { ((XYPlot)(chart.getPlot())).setDataset(obj); }

    BufferedImage getBufferedImage()
        {
        // make a buffer
        if (buffer == null || buffer.getWidth(null) != chartPanel.getWidth() || buffer.getHeight(null) != chartPanel.getHeight())
            {
            buffer = getGraphicsConfiguration().createCompatibleImage((int)chartPanel.getWidth(),(int)chartPanel.getHeight());
            }
                        
        // paint to the buffer
        Graphics2D g = (Graphics2D)(buffer.getGraphics());
        g.setColor(chartPanel.getBackground());
        g.fillRect(0,0,buffer.getWidth(null),buffer.getHeight(null));
        chartPanel.paintComponent(g);
        g.dispose();
        return buffer;
        }
        
    MovieMaker movieMaker = null;

    static final long INITIAL_KEY = -1;
    public static final long FORCE_KEY = -2;
    long oldKey = INITIAL_KEY;
        
    /** Key must be 0 or higher.  Will update only if the key passed in is different
        from the previously passed in key or if the key is FORCE_KEY.  
        If newData is true, then the chart will also be written out to a movie if appropriate. */
    public void update(long key, boolean newData)
        {
        if (key == oldKey && key != FORCE_KEY)  // we already did it
            return;
        else
            {
            oldKey = key;
            update();
                        
            if (newData)
                chart.getPlot().datasetChanged(new DatasetChangeEvent(chart.getPlot(), null));

            // now possibly write to the movie maker
            if (newData && movieMaker != null)
                {
                // add buffer to the movie maker
                movieMaker.add(getBufferedImage());
                }
            }
        }
                
    /** Override this to update the chart to reflect new data. */
    protected void update() { }
        
    void rebuildAttributeIndices()
        {
        SeriesAttributes[] c = getSeriesAttributes();
        for(int i = 0; i < c.length; i++)
            {
            SeriesAttributes csa = c[i];
            csa.setSeriesIndex(i);
            csa.rebuildGraphicsDefinitions();
            }
        revalidate();
        }
        
    protected SeriesAttributes getSeriesAttribute(int i)
        {
        return (SeriesAttributes)(seriesAttributes.getComponent(i));
        }
                
    public int getNumSeriesAttributes() { return seriesAttributes.getComponents().length; }

    protected SeriesAttributes[] getSeriesAttributes()
        {
        Component[] c = seriesAttributes.getComponents();
        SeriesAttributes[] sa = new SeriesAttributes[c.length];
        System.arraycopy(c, 0, sa, 0, c.length);
        return sa;
        }
                
    protected void setSeriesAttributes(SeriesAttributes[] c)
        {
        seriesAttributes.removeAll();
        for(int i = 0; i < c.length; i++)
            seriesAttributes.add(c[i]);
        }

    /** Override this to remove a series from the chart.  Be sure to call super(...) first. */
    public void removeSeries(int index)
        {
        // stop the inspector....
        SeriesAttributes[] c = getSeriesAttributes();
        SeriesChangeListener tmpObj = c[index].getStoppable();
        if (tmpObj != null)
            {
            tmpObj.seriesChanged(new SeriesChangeEvent(this));
            }
        
        // for good measure, set the index of the component to something crazy just in case a stoppable tries to continue pulsing it
        Component comp = seriesAttributes.getComponent(index);
        ((SeriesAttributes)comp).setSeriesIndex(-1);

        // remove the attribute and rebuild indices
        seriesAttributes.remove(index);
        rebuildAttributeIndices();
        revalidate();
        }
                
    
    /** Override this to move a series relative to other series.  Be sure to call super(...) first. */
    public void moveSeries(int index, boolean up)
        {
        if ((index > 0 && up) || (index < getSeriesDataset().getSeriesCount() - 1 && !up))  // it's not the first or the last given the move
            {
            SeriesAttributes[] c = getSeriesAttributes();
                        
            if (up)
                {
                SeriesAttributes s1 = c[index];
                SeriesAttributes s2 = c[index-1];
                c[index] = s2;
                c[index-1] = s1;
                }
            else
                {
                SeriesAttributes s1 = c[index];
                SeriesAttributes s2 = c[index+1];
                c[index] = s2;
                c[index+1] = s1;
                }
            setSeriesAttributes(c);
            rebuildAttributeIndices();
            revalidate();
            }
        else { } // ignore -- stupid user
        }
                
    /** Override this to construct the appropriate kind of chart.  This is the first thing called from the constructor; so certain
        of your instance variables may not have been set yet and you may need to set them yourself.  You'll need to set the dataset. */
    protected abstract void buildChart();
    






    /** Starts a Quicktime movie on the given ChartGenerator.  The size of the movie frame will be the size of
        the chart at the time this method is called.  This method ought to be called from the main event loop.
        Most of the default movie formats provided will result in a gigantic movie, which you can
        re-encode using something smarter (like the Animation or Sorenson codecs) to put to a reasonable size.
        On the Mac, Quicktime Pro will do this quite elegantly. */
    public void startMovie()
        {
        // can't start a movie if we're in an applet
        if (SimApplet.isApplet)
            {
            Object[] options = {"Oops"};
            JOptionPane.showOptionDialog(
                this, "You cannot create movies from an applet.",
                "MASON Applet Restriction",
                JOptionPane.OK_OPTION, JOptionPane.ERROR_MESSAGE,
                null, options, options[0]);
            return;
            }
                                        
        if (movieMaker != null) return;  // already running
        movieMaker = new MovieMaker(getFrame());

        if (!movieMaker.start(getBufferedImage()))
            movieMaker = null;  // failed
        else 
            {
            movieButton.setText("Stop Movie");
                        
            // emit an image
            update(FORCE_KEY, true);
            }
        }



    /** Stops a Quicktime movie and cleans up, flushing the remaining frames out to disk. 
        This method ought to be called from the main event loop. */
    public void stopMovie()
        {
        if (movieMaker == null) return;  // already stopped
        if (!movieMaker.stop())
            {
            Object[] options = {"Drat"};
            JOptionPane.showOptionDialog(
                this, "Your movie did not write to disk\ndue to a spurious JMF movie generation bug.",
                "JMF Movie Generation Bug",
                JOptionPane.OK_OPTION, JOptionPane.WARNING_MESSAGE,
                null, options, options[0]);
            }
        movieMaker = null;
        if (movieButton!=null)  // hasn't been destroyed yet
            {
            movieButton.setText("Create Movie");
            }
        }
        







    /** Deletes all series from the chart. */
    public void removeAllSeries()
        {
        for(int x = getSeriesDataset().getSeriesCount()-1 ; x>=0 ; x--)
            removeSeries(x);
        }
        
    /** Prepares the chart to be garbage collected.  If you override this, be sure to call super.quit() */
    public void quit()
        {
        if (movieMaker !=null) movieMaker.stop();
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

    /** @deprecated Use getNumGlobalAttributes */
    public int getGlobalAttributeCount() { return getNumGlobalAttributes(); }
        
    /** Returns the number of global attribute panels. */
    public int getNumGlobalAttributes()
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
                        
    /** @deprecated
        Sets the name of the Range Axis label -- usually this is the Y axis. */
    public void setRangeAxisLabel(String val) { setYAxisLabel(val); }
        
    /** Sets the name of the Y Axis label. */
    public void setYAxisLabel(String val)
        {
        XYPlot xyplot = (XYPlot)(chart.getPlot());
        xyplot.getRangeAxis().setLabel(val);
        xyplot.axisChanged(new AxisChangeEvent(xyplot.getRangeAxis()));
        yLabel.setValue(val);
        }
                
    /** @deprecated
        Returns the name of the Range Axis Label -- usually this is the Y axis. */
    public String getRangeAxisLabel() { return getYAxisLabel(); }
        
    /** Returns the name of the Y Axis label. */
    public String getYAxisLabel()
        {
        return ((XYPlot)(chart.getPlot())).getRangeAxis().getLabel();
        }
                
    /** @deprecated
        Sets the name of the Domain Axis label  -- usually this is the X axis. */
    public void setDomainAxisLabel(String val) { setXAxisLabel(val); }
        
    /** Sets the name of the X Axis label. */
    public void setXAxisLabel(String val)
        {
        XYPlot xyplot = (XYPlot)(chart.getPlot());
        xyplot.getDomainAxis().setLabel(val);
        xyplot.axisChanged(new AxisChangeEvent(xyplot.getDomainAxis()));
        xLabel.setValue(val);
        }
                
    /** @deprecated Returns the name of the Domain Axis label -- usually this is the X axis. */
    public String getDomainAxisLabel() { return getXAxisLabel(); } 

    /** Returns the name of the X Axis label. */
    public String getXAxisLabel()
        {
        return ((XYPlot)(chart.getPlot())).getDomainAxis().getLabel();
        }
    
    /** Returns the underlying chart. **/
    public JFreeChart getChart()
        {
        return chart;
        }

    /** Generates a new ChartGenerator with a blank chart.  Before anything else, buildChart() is called.  */
    public ChartGenerator()
        {
        // create the chart
        buildChart();
        chart.getPlot().setBackgroundPaint(Color.WHITE);
        ((XYPlot)(chart.getPlot())).setDomainGridlinesVisible(false);
        ((XYPlot)(chart.getPlot())).setRangeGridlinesVisible(false);
        ((XYPlot)(chart.getPlot())).setDomainGridlinePaint(new Color(200,200,200));
        ((XYPlot)(chart.getPlot())).setRangeGridlinePaint(new Color(200,200,200));


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

        LabelledList list = new LabelledList("Chart Properties");
        DisclosurePanel pan1 = new DisclosurePanel("Chart Properties", list);
        globalAttributes.add(pan1);
        
        JLabel j = new JLabel("Right-Click or Control-Click");
        j.setFont(j.getFont().deriveFont(10.0f).deriveFont(java.awt.Font.ITALIC));
        list.add(j);
        j = new JLabel("on Chart for More Options");
        j.setFont(j.getFont().deriveFont(10.0f).deriveFont(java.awt.Font.ITALIC));
        list.add(j);

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

        xLabel = new PropertyField()
            {
            public String newValue(String newValue)
                {
                setXAxisLabel(newValue);
                getChartPanel().repaint();
                return newValue;
                }
            };
        xLabel.setValue(getXAxisLabel());

        list.add(new JLabel("X Label"), xLabel);
        
        yLabel = new PropertyField()
            {
            public String newValue(String newValue)
                {
                setYAxisLabel(newValue);
                getChartPanel().repaint();
                return newValue;
                }
            };
        yLabel.setValue(getYAxisLabel());
        
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

        final JCheckBox xgridlines = new JCheckBox();
        xgridlines.setSelected(false);
        ItemListener il = new ItemListener()
            {
            public void itemStateChanged(ItemEvent e)
                {
                if (e.getStateChange() == ItemEvent.SELECTED)
                    {
                    chart.getXYPlot().setDomainGridlinesVisible(true);
                    }
                else
                    {
                    chart.getXYPlot().setDomainGridlinesVisible(false);
                    }
                }
            };
        xgridlines.addItemListener(il);
        list.add(new JLabel("X Grid Lines"), xgridlines);


        final JCheckBox ygridlines = new JCheckBox();
        ygridlines.setSelected(false);
        il = new ItemListener()
            {
            public void itemStateChanged(ItemEvent e)
                {
                if (e.getStateChange() == ItemEvent.SELECTED)
                    {
                    chart.getXYPlot().setRangeGridlinesVisible(true);
                    }
                else
                    {
                    chart.getXYPlot().setRangeGridlinesVisible(false);
                    }
                }
            };
        ygridlines.addItemListener(il);
        list.add(new JLabel("Y Grid Lines"), ygridlines);

        final JCheckBox legendCheck = new JCheckBox();
        legendCheck.setSelected(false);
        il = new ItemListener()
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
        pdfButtonPanel.setBorder(new javax.swing.border.TitledBorder("Chart Output"));
        DisclosurePanel pan2 = new DisclosurePanel("Chart Output", pdfButtonPanel);
                
        pdfButtonPanel.setLayout(new BorderLayout());
        Box pdfbox = new Box(BoxLayout.Y_AXIS);
        pdfButtonPanel.add(pdfbox,BorderLayout.WEST);

        JButton pdfButton = new JButton( "Save as PDF" );
        pdfbox.add(pdfButton);
        pdfButton.addActionListener(new ActionListener()
            {
            public void actionPerformed ( ActionEvent e )
                {
                FileDialog fd = new FileDialog(frame,"Choose PDF file...", FileDialog.SAVE);
                fd.setFile(chart.getTitle().getText() + ".pdf");
                fd.setVisible(true);
                String fileName = fd.getFile();
                if (fileName!=null)
                    {
                    Dimension dim = chartPanel.getPreferredSize();
                    PDFEncoder.generatePDF( chart, dim.width, dim.height, 
                        new File(fd.getDirectory(), Utilities.ensureFileEndsWith(fd.getFile(),".pdf")));
                    } 
                }
            });
        movieButton = new JButton( "Create a Movie" );
        pdfbox.add(movieButton);
        pdfbox.add(Box.createGlue());
        movieButton.addActionListener(new ActionListener()
            {
            public void actionPerformed ( ActionEvent e )
                {
                if (movieMaker == null) startMovie();
                else stopMovie();
                }
            });

        globalAttributes.add(pan2);
                
                
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
        the JFrame is being closed.  By default the JFrame will HIDE itself (not DISPOSE itself) when closed.  */
    public JFrame createFrame( )
        {
        frame = new JFrame()
            {
            public void dispose()
                {
                quit();
                super.dispose();
                }
            };
            
        frame.setDefaultCloseOperation(WindowConstants.HIDE_ON_CLOSE);
        frame.getContentPane().setLayout(new BorderLayout());
        frame.getContentPane().add(this,BorderLayout.CENTER);
        frame.setResizable(true);
        frame.pack();
        frame.setTitle(chart.getTitle().getText());
        return frame;
        }
        
    /** @deprecated, use createFrame() */
    public JFrame createFrame(Object simulation)
        {
        return createFrame();
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
    
    /** Add a legend to the chart unless the chart already has one. **/
    public void addLegend() 
        {
        if (chart.getLegend() != null)  // don't do anything if there already is one
            return;

        LegendTitle title = new LegendTitle(chart.getXYPlot());
        title.setLegendItemGraphicPadding(new org.jfree.ui.RectangleInsets(0,8,0,4));
        chart.addLegend(title);
        }

    /** @deprecated */
    public void setRangeAxisRange(double lower, double upper) { setYAxisRange(lower, upper); }

    public void setYAxisRange(double lower, double upper)
        {
        XYPlot xyplot = (XYPlot)(chart.getPlot());
        xyplot.getRangeAxis().setRange(lower, upper);
        }
                
    /** @deprecated */
    public void setDomainAxisRange(double lower, double upper) { setXAxisRange(lower, upper); }
        
    public void setXAxisRange(double lower, double upper)
        {
        XYPlot xyplot = (XYPlot)(chart.getPlot());
        xyplot.getDomainAxis().setRange(lower, upper);
        }


    Thread timer = null;
    /** Updates the inspector asynchronously sometime before the given milliseconds have transpired.  Once
        requested, further calls to request an update via this method will be ignored until the update occurs. */
    public void updateChartWithin(final long key, final long milliseconds)
        {
        if (timer == null)
            {
            timer= sim.util.gui.Utilities.doLater(milliseconds, new Runnable()
                {
                public void run()
                    {
                    update(key, true);  // keep up-to-date
                    // this is in the Swing thread, so it's okay
                    timer = null;
                    }
                });
            }
        }
        
    /** Posts a request to update the chart on the Swing event queue to happen next time repaints etc. happen. */
    public void updateChartLater(final long key)
        {
        repaint();  // make sure a repaint happens first  -- this is probably unnecessary

        javax.swing.SwingUtilities.invokeLater(new Runnable()
            {
            public void run()
                {
                update(key, true);
                }
            });
        }
    }

        
