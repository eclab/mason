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

import sim.util.gui.*;
import sim.util.gui.Utilities;
import sim.util.*;
import sim.display.*;
import sim.util.media.*;

// From JFreeChart
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
    public void setChartPanel(ScrollableChartPanel chartPanel)
        {
        chartHolder.getViewport().setView(chartPanel);
        this.chartPanel = chartPanel;
        }
    
    /** A holder for global attributes components */
    protected Box globalAttributes = Box.createVerticalBox();
    /** A holder for series attributes components */
    protected Box seriesAttributes = Box.createVerticalBox();
        
    /** The chart */
    protected JFreeChart chart;
    /** The panel which holds and draws the chart */
    protected ScrollableChartPanel chartPanel;
    /** The JScrollPane which holds the ChartPanel */
    private JScrollPane chartHolder = new JScrollPane();
        
    JFrame frame;
    /** Returns the JFrame which stores the whole chart.  Set in createFrame(), else null. */
    public JFrame getFrame() { return frame; }
        
    /** The global attributes chart title field. */
    PropertyField titleField;
    
    NumberTextField scaleField;
    NumberTextField proportionField;
    JCheckBox fixBox;

    JButton movieButton = new JButton("Create Movie");
    BufferedImage buffer;
        
    public abstract Dataset getSeriesDataset();
    public abstract void setSeriesDataset(Dataset obj);

    protected void update() { }

    /** Override this to construct the appropriate kind of chart.  This is the first thing called from the constructor; so certain
        of your instance variables may not have been set yet and you may need to set them yourself.  You'll need to set the dataset. */
    protected abstract void buildChart();
    
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
            {
            seriesAttributes.add(c[i]);
            }
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
        if ((index > 0 && up) || (index < getSeriesCount() - 1 && !up))  // it's not the first or the last given the move
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



    /** Starts a Quicktime movie on the given ChartGenerator.  The size of the movie frame will be the size of
        the chart at the time this method is called.  This method ought to be called from the main event loop.
        Most of the default movie formats provided will result in a gigantic movie, which you can
        re-encode using something smarter (like the Animation or Sorenson codecs) to put to a reasonable size.
        On the Mac, Quicktime Pro will do this quite elegantly. */
    public void startMovie()
        {
        // can't start a movie if we're in an applet
        if (SimApplet.isApplet())
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
        


    public abstract int getSeriesCount();
    

    /** Deletes all series from the chart. */
    public void removeAllSeries()
        {
        for(int x = getSeriesCount()-1 ; x>=0 ; x--)
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

    /** This is set to a string indicating that the chart is invalid.  When the title
        is set in the chart, this title will be used instead. */
    protected String invalidChartTitle = null;
    protected String validChartTitle = "";
        
    /** Sets the invalid chart title if any.  If null,
        clears the invalid chart title and displays the
        actual chart title. */
    public void setInvalidChartTitle(String title)
        {
        invalidChartTitle = title;
        setTitle(validChartTitle);
        }
        
    /** Sets the title of the chart (and the window frame). 
        If there is an invalidChartTitle set, this is used
        instead and the specified title is held in storage
        to be used later.  */
    public void setTitle(String title)
        {
        validChartTitle = title;

        if (invalidChartTitle != null)
            title = invalidChartTitle;
                
        chart.setTitle(title);
        chart.titleChanged(new TitleChangeEvent(new org.jfree.chart.title.TextTitle(title)));
        if (frame!=null) frame.setTitle(title);
        titleField.setValue(title);
        }

    /** Returns the title of the chart */
    public String getTitle()
        {
        return validChartTitle;
        }
    
    //static int uniqueNameKey = 0;
    //protected static String makeUniqueString(String name) { return name + (uniqueNameKey++); }
                     
    /** Returns the underlying chart. **/
    public JFreeChart getChart()
        {
        return chart;
        }

    protected void buildGlobalAttributes(LabelledList list) { }

    /** Generates a new ChartGenerator with a blank chart.  Before anything else, buildChart() is called.  */
    public ChartGenerator()
        {
        // create the chart
        buildChart();
        chart.getPlot().setBackgroundPaint(Color.WHITE);
        chart.setAntiAlias(true);

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


        buildGlobalAttributes(list);



        final JCheckBox legendCheck = new JCheckBox();
        ItemListener il = new ItemListener()
            {
            public void itemStateChanged(ItemEvent e)
                {
                if (e.getStateChange() == ItemEvent.SELECTED)
                    {
                    LegendTitle title = new LegendTitle(chart.getPlot());
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
        legendCheck.setSelected(true);

/*
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
*/

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
                
        // Add scale and proportion fields
        Box header = Box.createHorizontalBox();

        final double MAXIMUM_SCALE = 8;
        
        fixBox = new JCheckBox("Fill");
        fixBox.addActionListener(new ActionListener()
            {
            public void actionPerformed(ActionEvent e)
                {
                setFixed(fixBox.isSelected());
                }
            });
        header.add(fixBox);
        fixBox.setSelected(true);
        
        // add the scale field
        scaleField = new NumberTextField("  Scale: ", 1.0, true)
            {
            public double newValue(double newValue)
                {
                if (newValue <= 0.0) newValue = currentValue;
                if (newValue > MAXIMUM_SCALE) newValue = currentValue;
                scale = newValue;
                resizeChart();
                return newValue;
                }
            };
        scaleField.setToolTipText("Zoom in and out");
        scaleField.setBorder(BorderFactory.createEmptyBorder(0,0,0,2));
        scaleField.setEnabled(false);
        scaleField.setText("");
        header.add(scaleField);
       
        // add the proportion field
        proportionField = new NumberTextField("  Proportion: ", 1.5, true)
            {
            public double newValue(double newValue)
                {
                if (newValue <= 0.0) newValue = currentValue;
                proportion = newValue;
                resizeChart();
                return newValue;
                }
            };
        proportionField.setToolTipText("Change the chart proportions (ratio of width to height)");
        proportionField.setBorder(BorderFactory.createEmptyBorder(0,0,0,2));
        header.add(proportionField);


        chartHolder.setMinimumSize(new Dimension(0,0));
        chartHolder.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        chartHolder.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);        
        chartHolder.getViewport().setBackground(Color.gray);
        JPanel p2 = new JPanel();
        p2.setLayout(new BorderLayout());
        p2.add(chartHolder, BorderLayout.CENTER);
        p2.add(header, BorderLayout.NORTH);
        split.setRightComponent(p2);
        setLayout(new BorderLayout());
        add(split,BorderLayout.CENTER);
        
        // set the default to be white, which looks good when printed
        chart.setBackgroundPaint(Color.WHITE);

        // JFreeChart has a hillariously broken way of handling font scaling.
        // It allows fonts to scale independently in X and Y.  We hack a workaround here.
        chartPanel.setMinimumDrawHeight((int)DEFAULT_CHART_HEIGHT);
        chartPanel.setMaximumDrawHeight((int)DEFAULT_CHART_HEIGHT);
        chartPanel.setMinimumDrawWidth((int)(DEFAULT_CHART_HEIGHT * proportion));
        chartPanel.setMaximumDrawWidth((int)(DEFAULT_CHART_HEIGHT * proportion));
        chartPanel.setPreferredSize(new java.awt.Dimension((int)(DEFAULT_CHART_HEIGHT * DEFAULT_CHART_PROPORTION), (int)(DEFAULT_CHART_HEIGHT)));
        }

    public boolean isFixed()
        {
        return fixBox.isSelected();
        }
                
    public void setFixed(boolean value)
        {
        fixBox.setSelected(value);
        chartHolder.setHorizontalScrollBarPolicy(
            value? ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER: ScrollPaneConstants.HORIZONTAL_SCROLLBAR_ALWAYS);
        scaleField.setEnabled(!value);
        if (value) scaleField.setText("");
        else 
            {
            double val = scaleField.getValue();
            if (val == (int) val)
                scaleField.setText("" + (int)val);
            else scaleField.setText("" + val);
            }
        resizeChart();
        }
                


    public double DEFAULT_CHART_HEIGHT = 480;
    public double DEFAULT_CHART_PROPORTION = 1.5;
    
    double scale = 1.0;
    double proportion = 1.5;
    
    public double getScale() { return scale; }
    public double getProportion() { return proportion; }
    public void setScale(double val) { scale = val; scaleField.setValue(val); resizeChart(); }
    public void setProportion(double val) { proportion = val; proportionField.setValue(val); resizeChart(); }
    
    
    void resizeChart()
        {
        double w = DEFAULT_CHART_HEIGHT * scale * proportion;
        double h = DEFAULT_CHART_HEIGHT * scale;
        Dimension d = new java.awt.Dimension((int)(w), (int)(h));

        chartPanel.setSize(new java.awt.Dimension(d));
        chartPanel.setPreferredSize(chartPanel.getSize());
        
        // JFreeChart has a hillariously broken way of handling font scaling.
        // It allows fonts to scale independently in X and Y.  We hack a workaround
        // here.
        chartPanel.setMinimumDrawHeight((int)DEFAULT_CHART_HEIGHT);
        chartPanel.setMaximumDrawHeight((int)DEFAULT_CHART_HEIGHT);
        chartPanel.setMinimumDrawWidth((int)(DEFAULT_CHART_HEIGHT * proportion));
        chartPanel.setMaximumDrawWidth((int)(DEFAULT_CHART_HEIGHT * proportion));
        
        chartPanel.repaint();
        }
    
    /** Returns a JFrame suitable or housing the ChartGenerator.  This frame largely calls chart.quit() when
        the JFrame is being closed.  By default the JFrame will HIDE itself (not DISPOSE itself) when closed.  */
    public JFrame createFrame( )
        {
        return createFrame(false);
        }

    /** Returns a JFrame suitable or housing the ChartGenerator.  This frame largely calls chart.quit() when
        the JFrame is being closed.  By default the JFrame will HIDE itself (not DISPOSE itself) when closed. 
        If inspector == true, the frame will have the look of an inspector */
    public JFrame createFrame( boolean inspector )
        {
        frame = new JFrame()
            {
            public void dispose()
                {
                quit();
                super.dispose();
                }
            };
        if (inspector)
            frame.getRootPane().putClientProperty("Window.style", "small");  // on the Mac

        frame.setDefaultCloseOperation(WindowConstants.HIDE_ON_CLOSE);
        frame.getContentPane().setLayout(new BorderLayout());
        frame.getContentPane().add(this,BorderLayout.CENTER);
        frame.setResizable(true);
        frame.pack();
        frame.setTitle(chart.getTitle().getText());
        return frame;
        }


        
    /** @deprecated use createFrame() */
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
                    Class.forName("ch.randelshofer.quaqua.Quaqua14ColorChooserUI", true, Thread.currentThread().getContextClassLoader()).getName());
            }
        catch (Exception e) { }
        }
    
    /** Add a legend to the chart unless the chart already has one. **/
    public void addLegend() 
        {
        if (chart.getLegend() != null)  // don't do anything if there already is one
            return;

        LegendTitle title = new LegendTitle(chart.getPlot());
        title.setLegendItemGraphicPadding(new org.jfree.ui.RectangleInsets(0,8,0,4));
        chart.addLegend(title);
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
    
    static int DEFAULT_UNIT_FRACTION = 20;
    static int DEFAULT_BLOCK_FRACTION = 2;
    
    class ScrollableChartPanel extends ChartPanel implements Scrollable
        {
        public ScrollableChartPanel(JFreeChart chart, boolean useBuffer)
            {
            super(chart, useBuffer);
            }
        
        public Dimension getPreferredSize()
            {
            Dimension size = super.getPreferredSize();
            int viewportWidth = chartHolder.getViewport().getWidth();
            if (viewportWidth == 0)  // uh oh, not set up yet
                return size;

            // adjust height
            if (isFixed())
                size.height = (int)(size.height / (double) size.width * viewportWidth);
            return size;
            }
                
        public Dimension getPreferredScrollableViewportSize()
            {
            return getPreferredSize();
            }
            
        public int getScrollableUnitIncrement(java.awt.Rectangle visibleRect, int orientation, int direction)
            { 
            return (int)((orientation == SwingConstants.HORIZONTAL) ?
                ( visibleRect.getWidth() / DEFAULT_UNIT_FRACTION ) :
                ( visibleRect.getHeight() / DEFAULT_UNIT_FRACTION ));
            }
            
        public int getScrollableBlockIncrement(java.awt.Rectangle visibleRect, int orientation, int direction)
            { 
            return (int)((orientation == SwingConstants.HORIZONTAL) ?
                ( visibleRect.getWidth() / DEFAULT_BLOCK_FRACTION ) :
                ( visibleRect.getHeight() / DEFAULT_BLOCK_FRACTION ));
            }
            
        public boolean getScrollableTracksViewportHeight() { return false; }
        public boolean getScrollableTracksViewportWidth() { return isFixed(); }
        public Dimension getMaximumSize() { return getPreferredSize(); }
        public Dimension getMinimumSize() { return getPreferredSize(); }
        
        public void setSize(Dimension d)
            {
            super.setSize(d);
            }
        }
        
        
        
    public ScrollableChartPanel buildChartPanel(JFreeChart chart)
        {
        return new ScrollableChartPanel(chart, true); 
        }


    // This ridiculous class exists so we can create Strings (of sorts) which are completely
    // uncomparable and have a total sort order regardless of their values.  Otherwise
    // (this is true) MultiplePiePlot won't allow multiple PiePlots with the same name.
    public static class UniqueString implements java.lang.Comparable
        {
        String string;
        
        public UniqueString(Object obj)
            {
            string = "" + obj;
            }
        
        public boolean equals(Object obj)
            {
            return obj == this;
            }
                
        public int compareTo(Object obj)
            {
            if (obj == this) return 0;
            if (obj == null) throw new NullPointerException();
            if (!(obj instanceof UniqueString)) return -1;
            UniqueString us = (UniqueString)obj; 
            if (us.string.equals(string))  // gotcha.  Gotta differentiate
                {
                if (System.identityHashCode(this) > System.identityHashCode(us))
                    return 1; 
                else return -1;
                }
            else return us.string.compareTo(string);
            }
                
        public String toString() { return string; }
        }    }

        
