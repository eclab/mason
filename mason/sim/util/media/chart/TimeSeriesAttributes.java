/*
  Copyright 2006 by Sean Luke
  Licensed under the Academic Free License version 3.0
  See the file "LICENSE" for more information
*/

package sim.util.media.chart;

import java.awt.*;
import javax.swing.*;
import javax.swing.border.*;
import java.awt.event.*;
import java.util.*;
import sim.util.gui.*;

// From JFreeChart (jfreechart.org)
import org.jfree.data.xy.*;
import org.jfree.chart.*;
import org.jfree.chart.event.*;
import org.jfree.chart.plot.*;
import org.jfree.data.general.*;
import org.jfree.chart.renderer.xy.*;
import org.jfree.data.general.*;

/** A SeriesAttributes used for user control pf time series created with TimeSeriesCharGenerator.
    This is done largely through the
    manipulation of XYSeries objects and features of the XYPlot class. */
        
public class TimeSeriesAttributes extends SeriesAttributes
    {
    /** A dash */
    static final float DASH = 6;
    /** A dot */
    static final float DOT = 1;
    /** A short space */            
    static final float SPACE = 3;
    /** A long space */
    static final float SKIP = DASH;
                
    /** Nine dash combinations that the user might find helpful. */
    static final float[][] dashes = 
        { 
        { DASH, 0.0f }, // --------
            { DASH * 2, SKIP }, 
            { DASH, SKIP } , // -  -  -  
            { DASH, SPACE } , // - - - -
            { DASH, SPACE, DASH, SPACE, DOT, SPACE },  // - - . - - . 
            { DASH, SPACE, DOT, SPACE, }, // - . - .
            { DASH, SPACE, DOT, SPACE, DOT, SPACE },  // - . . - . . 
            { DOT, SPACE }, // . . . .
            { DOT, SKIP }   // .  .  .  .  
        };
    
    /** How much we should stretch the dashes listed above.  1.0 is normal. */
    float stretch;
    /** Line thickness. */
    float thickness;
    /** Line dash pattern (one of the dashes above). */
    float[] dash;
    /** Line color. */
    Color strokeColor;
                
    /** The time series in question.  */
    public XYSeries series;
    public void setSeriesName(String val) { series.setKey(val); }
    public String getSeriesName() { return "" + series.getKey(); }
                
    /** Builds a TimeSeriesAttributes with the given generator, series, and index for the series. */
    public TimeSeriesAttributes(ChartGenerator generator, XYSeries series, int index)
        { 
        super(generator, "" + series.getKey(), index); this.series = series;
        }

    public void rebuildGraphicsDefinitions()
        {
        float[] newDash = new float[dash.length];
        for(int x=0;x<dash.length;x++)
            if (stretch*thickness > 0)
                newDash[x] = dash[x] * stretch * thickness;  // include thickness so we dont' get overlaps -- will this confuse users?
                
        XYItemRenderer renderer = getRenderer();
            
        renderer.setSeriesStroke(getSeriesIndex(),
                                 new BasicStroke(thickness, BasicStroke.CAP_ROUND, 
                                                 BasicStroke.JOIN_ROUND,0,newDash,0));

        renderer.setSeriesPaint(getSeriesIndex(),strokeColor);
        repaint();
        }
        
    public void buildAttributes()
        {
        // The following three variables aren't defined until AFTER construction if
        // you just define them above.  So we define them below here instead.
                                                
        dash = dashes[0];
        stretch = 1.0f;
        thickness = 2.0f;

        // strokeColor = Color.black;  // rebuildGraphicsDefinitions will get called by our caller afterwards
        XYItemRenderer renderer = getRenderer();
        //Paint paint = renderer.getSeriesPaint(getSeriesIndex());
        
        //In jfc 1.0.6 getSeriesPaint returns null!!!
        //You need lookupSeriesPaint(), but that's not backward compatible.
        //The only thing consistent in all versions is getItemPaint 
        //(which looks like a gross miss-use, but gets the job done)
        Paint paint = renderer.getItemPaint(getSeriesIndex(), -1);
        
        strokeColor = (Color)paint;
        
        ColorWell well = new ColorWell(strokeColor)
            {
            public Color changeColor(Color c) 
                {
                strokeColor = c;
                rebuildGraphicsDefinitions();
                return c;
                }
            };
        addLabelled("Line",well);
                        
        NumberTextField thickitude = new NumberTextField(2.0,true)
            {
            public double newValue(double newValue) 
                {
                if (newValue < 0.0) 
                    newValue = currentValue;
                thickness = (float)newValue;
                rebuildGraphicsDefinitions();
                return newValue;
                }
            };
        addLabelled("Width",thickitude);
        final JComboBox list = new JComboBox();
        list.setEditable(false);
        list.setModel(new DefaultComboBoxModel(new java.util.Vector(Arrays.asList(
                                                                        new String[] { "Solid", "__  __  __", "_  _  _  _", "_ _ _ _ _", "_ _ . _ _ .", 
                                                                                       "_ . _ . _ .", "_ . . _ . .", ". . . . . . .", ".  .  .  .  ." }))));
        list.setSelectedIndex(0);
        list.addActionListener(new ActionListener()
            {
            public void actionPerformed ( ActionEvent e )
                {
                dash = dashes[list.getSelectedIndex()];
                rebuildGraphicsDefinitions();
                }
            });
        addLabelled("Dash",list);
        NumberTextField stretchField = new NumberTextField(1.0,true)
            {
            public double newValue(double newValue) 
                {
                if (newValue < 0.0) 
                    newValue = currentValue;
                stretch = (float)newValue;
                rebuildGraphicsDefinitions();
                return newValue;
                }
            };
        addLabelled("Stretch",stretchField);
        }
    }
