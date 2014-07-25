/*
  Copyright 2006 by Sean Luke and George Mason University
  Licensed under the Academic Free License version 3.0
  See the file "LICENSE" for more information
*/

package sim.portrayal.inspector;
import java.awt.*;
import java.awt.event.*;
import sim.util.*;
import sim.display.*;
import sim.engine.*;
import javax.swing.*;
import sim.util.gui.*;
import sim.util.media.chart.*;
import org.jfree.data.xy.*;
import org.jfree.data.general.*;

/** An abstract superclass for property inspectors which use sim.util.ChartGenerator to produce charts.
    Contains a number of utility methods that these property inspector often have in commmon.  Each ChartingPropertyInspector
    works with a single data series on a single chart.  Additionally, each ChartingPropertyInspector has access to the
    global attributes common to the ChartGenerator.
        
    <p>To construct a ChartingPropertyInspector subclass, you need to override the methods validChartGenerator(generator),
    which indicates if the generator is one you know how to work with; createNewGenerator(), which produces a new
    chart generator when you need to create one from scratch, and updateSeries(...), which revises the series to possibly
    reflect new data.
                
    <p>Additionally, you may wish to override includeAggregationMethodAttributes() to indicate whether or not the 
    ChartingPropertyInspector should include an aggregation method in its global attributes.
    If so, the aggregation method will be stored in globalAttributes.aggregationMethod
    and will be one of AGGREGATIONMETHOD_CURRENT (don't aggregate), AGGREGATIONMETHOD_MIN (use the minimum of
    the aggregated data over time), AGGREGATIONMETHOD_MAX (use the maximum), or AGGREGATIONMETHOD_MEAN (use
    the mean).   The aggregation interval -- how much time you should wait for before dumping the aggregated
    results into the time series -- will be stored in globalAttriutes.interval. 
        
    <p>The ChartingPropertyInspector maintains a Bag of global charts presently on-screen.  This isn't a static variable, but
    rather is stored in GUIState.storage under the key chartKey ("sim.portrayal.inspector.ChartingPropertyInspector")
*/

public abstract class ChartingPropertyInspector extends PropertyInspector
    {   
    /** The ChartGenerator used by this ChartingPropertyInspector */
    protected ChartGenerator generator;
    public ChartGenerator getGenerator() { return generator; }
    double lastTime  = Schedule.BEFORE_SIMULATION;
    SeriesAttributes seriesAttributes;

    /** Called when the inspector is being asked to use an existing ChartGenerator.  Should return true if the
        ChartGenerator is compatable with this inspector. */
    protected abstract boolean validChartGenerator(ChartGenerator generator);

    /** Called when the inspector is being asked to create a new ChartGenerator from scratch. */
    protected abstract ChartGenerator createNewGenerator();

    /** Called from updateInspector() to inform the ChartingPropertyInspector that it may want to update its data series
        to reflect new data at this time.  The value lastTime indicates the previous timestep when this method was
        called.  It's possible that time == lastTime, that is, the method is called multiple times and nothing has
        changed. */
    protected abstract void updateSeries(double time, double lastTime);

    /** Returns true if a widget should be inserted into the series attributes that allows the user to specify
        an aggregation method.  If so, the aggregation method will be stored in globalAttributes.aggregationMethod
        and will be one of AGGREGATIONMETHOD_CURRENT (don't aggregate), AGGREGATIONMETHOD_MIN (use the minimum of
        the aggregated data over time), AGGREGATIONMETHOD_MAX (use the maximum), or AGGREGATIONMETHOD_MEAN (use
        the mean).   The aggregation interval -- how much time you should wait for before dumping the aggregated
        results into the time series -- will be stored in globalAttriutes.interval. */
    protected boolean includeAggregationMethodAttributes() { return true; }

    /** Returns the SeriesAttributes used in the ChartGenerator for the series defined by this inspector.  */
    public SeriesAttributes getSeriesAttributes() { return seriesAttributes; }

    /** Produces a ChartingPropertyInspector which tracks property number index from the given properties list,
        stored in the provided parent frame, and applied in the given simulation.  This constructor will give the
        user a chance to cancel the construction, in which case validInspector will be set to false, and generator
        may be set to null.  In that case, assume that the inspector will be deleted immediately after.  */
    public ChartingPropertyInspector(Properties properties, int index, Frame parent, final GUIState simulation)
        {
        super(properties,index,parent,simulation);
        generator = chartToUse( properties.getName(index), parent, simulation );
        setValidInspector(generator!=null);

        if (isValidInspector())
            {
            globalAttributes = findGlobalAttributes();  // so we share timer information.  If null, we're in trouble.
            // make sure that when the window is closed, the stopper is stopped
            WindowListener wl = new WindowListener()
                {
                public void windowActivated(WindowEvent e) {}
                public void windowClosed(WindowEvent e) { if (stopper!=null) stopper.stop(); }
                public void windowClosing(WindowEvent e) {  }
                public void windowDeactivated(WindowEvent e) {}
                public void windowDeiconified(WindowEvent e) {}
                public void windowIconified(WindowEvent e) {}
                public void windowOpened(WindowEvent e) {}
                };
            generator.getFrame().addWindowListener(wl);
            }
        }

    /**
     * This constructor allows one to set the chart generator programmatically (i.e. no GUI).
     * If the <code>generator</code> parameter is null, a new chart is used.
     * If the <code>generator</code> is not valid for this inspector, an exception is thrown.
     */
    public ChartingPropertyInspector(Properties properties, int index, final GUIState simulation, ChartGenerator generator)
        {
        //the parent is ignored in PropertyInspector anyway, so I just sent a null
        super(properties,index,null,simulation);
        
        if(generator!=null)
            {
            if(!validChartGenerator(generator))
                throw new RuntimeException("Invalid generator: "+generator);
            this.generator = generator;
            }
        else
            this.generator = createNewChart(simulation);

        // make sure that when the window is closed, the stopper is stopped
        WindowListener wl = new WindowListener()
            {
            public void windowActivated(WindowEvent e) {}
            public void windowClosed(WindowEvent e) {}
            public void windowClosing(WindowEvent e) { if (stopper!=null) stopper.stop(); }
            public void windowDeactivated(WindowEvent e) {}
            public void windowDeiconified(WindowEvent e) {}
            public void windowIconified(WindowEvent e) {}
            public void windowOpened(WindowEvent e) {}
            };
        this.generator.getFrame().addWindowListener(wl);

        globalAttributes = findGlobalAttributes();  // so we share timer information.  If null, we're in trouble.
        setValidInspector(this.generator!=null); //this should always be true.
        }
    
    /** Used to find the global attributes that another inspector has set so I can share it. */
    GlobalAttributes findGlobalAttributes()
        {
        if (generator == null) return null;  // got a problem
        int len = generator.getNumGlobalAttributes();
        for(int i = 0; i < len ; i ++)
            {
            // Global Attributes are members of DisclosurePanels
            if ((generator.getGlobalAttribute(i) instanceof DisclosurePanel))
                {
                DisclosurePanel pan = (DisclosurePanel)(generator.getGlobalAttribute(i));
                if (pan.getDisclosedComponent() instanceof GlobalAttributes)
                    return (GlobalAttributes) (pan.getDisclosedComponent());
                }
            }
        return null;
        }
        
    public final static String chartKey = "sim.portrayal.inspector.ChartingPropertyInspector";

    /** Returns the global charts Bag which holds all charts on-screen for this simulation instance. */
    protected Bag getCharts(GUIState simulation)
        {
        Bag c = (Bag)(simulation.storage.get(chartKey));
        if (c == null)
            {
            c = new Bag();
            simulation.storage.put(chartKey, c);
            }
        return c;
        }
                
    /** Used to find the global attributes that another inspector has set so I can share it. */
    ChartGenerator chartToUse( final String sName, Frame parent, final GUIState simulation )
        {
        Bag charts = new Bag(getCharts(simulation));            // make a copy so I can reduce it

        // reduce the charts to ones I can use
        for(int i = 0; i < charts.numObjs; i++)
            {
            ChartGenerator g = (ChartGenerator)(charts.objs[i]);
            if (!validChartGenerator(g))  // I can't use this chart
                { charts.remove(g); i--; 
            System.err.println(g);
                }
            }

        if( charts.numObjs == 0 )
            return createNewChart(simulation);

        // init the dialog panel
        JPanel p = new JPanel();
        p.setLayout(new BorderLayout());

        String[] chartNames = new String[ charts.numObjs + 1 ];

        chartNames[0] = "[Create a New Chart]";
        for( int i = 0 ; i < charts.numObjs ; i++ )
            chartNames[i+1] = ((ChartGenerator)(charts.objs[i])).getTitle();

        // add widgets
        JPanel panel2 = new JPanel();
        panel2.setLayout(new BorderLayout());
        panel2.setBorder(new javax.swing.border.TitledBorder("Plot on Chart..."));
        JComboBox encoding = new JComboBox(chartNames);
        panel2.add(encoding, BorderLayout.CENTER);
        p.add(panel2, BorderLayout.SOUTH);
                
        // ask
        if(JOptionPane.showConfirmDialog(parent, p,"Create a New Chart...",
                JOptionPane.OK_CANCEL_OPTION) != JOptionPane.OK_OPTION)
            return null;

        if( encoding.getSelectedIndex() == 0 )
            return createNewChart(simulation);
        else
            return (ChartGenerator)(charts.objs[encoding.getSelectedIndex()-1]);
        }
        
                                        
    protected static final int AGGREGATIONMETHOD_CURRENT = 0;
    protected static final int AGGREGATIONMETHOD_MAX = 1;
    protected static final int AGGREGATIONMETHOD_MIN = 2;
    protected static final int AGGREGATIONMETHOD_MEAN = 3;
        
    protected static final int REDRAW_ALWAYS = 0;
    protected static final int REDRAW_TENTH_SEC = 1;
    protected static final int REDRAW_HALF_SEC = 2;
    protected static final int REDRAW_ONE_SEC = 3;
    protected static final int REDRAW_TWO_SECS = 4;
    protected static final int REDRAW_FIVE_SECS = 5;
    protected static final int REDRAW_TEN_SECS = 6;
    protected static final int REDRAW_DONT = 7;
        
    /* The Global Attributes panel (the top-left panel) of this ChartingPropertyInspector.  Note that this
       panel is shared with other inspectors using the same chart. */
    GlobalAttributes globalAttributes;
    public GlobalAttributes getGlobalAttributes() { return globalAttributes; }

    /** The Global Attributes panel (the top-left panel) of ChartingPropertyInspectors. */
    protected class GlobalAttributes extends JPanel
        {
        public long interval = 1;
        public int aggregationMethod = AGGREGATIONMETHOD_CURRENT;
        public int redraw = REDRAW_HALF_SEC;
        String title = "";

        public GlobalAttributes()
            {
            setLayout(new BorderLayout());
                        
            title = includeAggregationMethodAttributes() ? "Add Data..." : "Redraw";
            LabelledList list = new LabelledList(title);
            add(list,BorderLayout.CENTER);
                        
            if (includeAggregationMethodAttributes())
                {
                NumberTextField stepsField = new NumberTextField(1,true)
                    {
                    public double newValue(double value)
                        {
                        value = (long)value;
                        if (value <= 0) return currentValue;
                        else 
                            {
                            interval = (long)value;
                            return value;
                            }
                        }
                    };
                                                                                        
                list.addLabelled("Every",stepsField);
                list.addLabelled("",new JLabel("...Timesteps"));

                String[] optionsLabel = { "Current", "Maximum", "Minimum", "Mean" };
                final JComboBox optionsBox = new JComboBox(optionsLabel);
                optionsBox.setSelectedIndex(aggregationMethod);
                optionsBox.addActionListener(
                    new ActionListener()
                        {
                        public void actionPerformed(ActionEvent e)
                            {
                            aggregationMethod = optionsBox.getSelectedIndex();
                            }
                        });
                list.addLabelled("Using", optionsBox);
                }
                        
            String[] optionsLabel2 = new String[]{ "When Adding Data", "Every 0.1 Seconds", "Every 0.5 Seconds", 
                                                   "Every Second", "Every 2 Seconds", "Every 5 Seconds", "Every 10 Seconds", "Never" };
            final JComboBox optionsBox2 = new JComboBox(optionsLabel2);
            optionsBox2.setSelectedIndex(redraw);
            optionsBox2.addActionListener(
                new ActionListener()
                    {
                    public void actionPerformed(ActionEvent e)
                        {
                        redraw = optionsBox2.getSelectedIndex();
                        generator.update(ChartGenerator.FORCE_KEY, false);  // keep up-to-date
                        }
                    });
            if (includeAggregationMethodAttributes())
                list.addLabelled("Redraw", optionsBox2);
            else list.add(optionsBox2);
            }
        }
        
    JFrame chartFrame = null;
        
    ChartGenerator createNewChart( final GUIState simulation)
        {
        generator = createNewGenerator();
        globalAttributes = new GlobalAttributes();
        DisclosurePanel pan = new DisclosurePanel(globalAttributes.title, globalAttributes);
        generator.addGlobalAttribute(pan);  // it'll be added last
                
        getCharts(simulation).add( generator );                 // put me in the global charts list
        chartFrame = generator.createFrame(true);
        chartFrame.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);  //  by default it's HIDE_ON_CLOSE

        WindowListener wl = new WindowListener()
            {
            public void windowActivated(WindowEvent e) {}
            public void windowClosed(WindowEvent e) {}
            public void windowClosing(WindowEvent e) { generator.quit(); }
            public void windowDeactivated(WindowEvent e) {}
            public void windowDeiconified(WindowEvent e) {}
            public void windowIconified(WindowEvent e) {}
            public void windowOpened(WindowEvent e) {}
            };
        chartFrame.addWindowListener(wl);
        chartFrame.setVisible(true);
        return generator;
        }

    // Utility method.  Returns a filename guaranteed to end with the given ending.
    static String ensureFileEndsWith(String filename, String ending)
        {
        // do we end with the string?
        if (filename.regionMatches(false,filename.length()-ending.length(),ending,0,ending.length()))
            return filename;
        else return filename + ending;
        }

    boolean updatedOnceAlready = false;  // did we update at the simulation start?
        
    /** Override this method if your charting property inspector can be updated and drawn
        with useful data even if the simulation is at an invalid time, such as BEFORE_SIMULATION
        or AFTER_SIMULATION.  If you have a time series or some other kind of inspector which
        doesn't get any data until EPOCH, then override this method to return FALSE.  Otherwise
        if your inspector always replaces all of its data (for example, a histogram), then
        you can leave the method as it is: the default returns TRUE. */
    protected boolean isAlwaysUpdateable() { return true; }
        
    public void updateInspector()
        {
        double time = simulation.state.schedule.getTime();
        // we should only update if we're at a new time that we've not seen yet, or if
        // we're at the start of inspection and haven't done an update yet.  It's possible
        // for this second condition to be true while the first one is false: if we're at
        // simulation start, then lastTime == Schedule.BEFORE_SIMULATION == time, but we'd
        // still want to update at least one time.
        if (((time >= Schedule.EPOCH && time < Schedule.AFTER_SIMULATION) || isAlwaysUpdateable()) &&
            (lastTime < time || !updatedOnceAlready))  // bug fix 
            {              
            updatedOnceAlready = true;

            // update the data
            updateSeries(time, lastTime);
            lastTime = time;
                
            // now determine when to redraw
            switch(globalAttributes.redraw) 
                {
                case REDRAW_ALWAYS:  // do it now
                    generator.update(simulation.state.schedule.getSteps(), true);
                    break;
                case REDRAW_TENTH_SEC:
                    generator.updateChartWithin(simulation.state.schedule.getSteps(), 100);
                    break;
                case REDRAW_HALF_SEC:
                    generator.updateChartWithin(simulation.state.schedule.getSteps(), 500);
                    break;
                case REDRAW_ONE_SEC:
                    generator.updateChartWithin(simulation.state.schedule.getSteps(), 1000);
                    break;
                case REDRAW_TWO_SECS:
                    generator.updateChartWithin(simulation.state.schedule.getSteps(), 2000);
                    break;
                case REDRAW_FIVE_SECS:
                    generator.updateChartWithin(simulation.state.schedule.getSteps(), 5000);
                    break;
                case REDRAW_TEN_SECS:
                    generator.updateChartWithin(simulation.state.schedule.getSteps(), 10000);
                    break;
                case REDRAW_DONT:  // do nothing
                    break;
                default:
                    throw new RuntimeException("Unknown redraw time specified.");
                }
            }
        }
    
    public boolean shouldCreateFrame()
        {
        return false;
        }

    public Stoppable reviseStopper(Stoppable stopper)
        {
        final Stoppable newStopper = super.reviseStopper(stopper);
        return new Stoppable()
            {
            public void stop()
                {
                if (newStopper!=null) newStopper.stop();  // wraps the stopper
                // give the movie a chance to write out                         
                generator.stopMovie();
                }
            };
        }
                
    public void disposeFrame()
        {
        if (chartFrame != null)
            chartFrame.dispose();
        chartFrame = null;
        }
    }
