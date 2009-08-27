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

    static final public int PATTERN_DASH = 0;
    static final public int PATTERN_DASH_SKIP = 1;
    static final public int PATTERN_DASH_SPACE = 2;
    static final public int PATTERN_DASH_SPACE_DASH_SPACE_DOT_SPACE = 3;
    static final public int PATTERN_DASH_SPACE_DOT_SPACE = 4;
    static final public int PATTERN_DASH_SPACE_DOT_SPACE_DOT_SPACE = 5;
    static final public int PATTERN_DOT_SPACE =6;
    static final public int PATTERN_DOT_SKIP = 7;

                
    /** Nine dash combinations that the user might find helpful. */
    static final float[][] dashPatterns = 
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
    
    /** How much we should stretch the dashPatterns listed above.  1.0 is normal. */
    float stretch;
    NumberTextField stretchField;
    /** Line thickness. */
    float thickness;
    NumberTextField thicknessField;
    /** Line dash pattern (one of the dashPatterns above). */
    float[] dashPattern;
    JComboBox dashPatternList;
    /** Line color. */
    Color strokeColor;
    ColorWell strokeColorWell;
                
    public void setThickness(float value) { thicknessField.setValue(thicknessField.newValue(value));  }
    public float getThickness() { return (float)(thicknessField.getValue()); }

    public void setStretch(float value) { stretchField.setValue(stretchField.newValue(value));  }
    public float getStretch() { return (float)(stretchField.getValue()); }

    public void setDashPattern(int value) 
        { 
        if (value >= 0 && value < dashPatterns.length) 
            { 
            dashPatternList.setSelectedIndex(value);
            dashPattern = dashPatterns[value];
            }
        }
    public float getDashPattern() { return dashPatternList.getSelectedIndex(); }

    public void setStrokeColor(Color value) { strokeColorWell.setColor(strokeColor = value);}
    public Color getStrokeColor() { return strokeColor; }

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
        float[] newDashPattern = new float[dashPattern.length];
        for(int x=0;x<dashPattern.length;x++)
            if (stretch*thickness > 0)
                newDashPattern[x] = dashPattern[x] * stretch * thickness;  // include thickness so we dont' get overlaps -- will this confuse users?
                
        XYItemRenderer renderer = getRenderer();
            
        renderer.setSeriesStroke(getSeriesIndex(),
            new BasicStroke(thickness, BasicStroke.CAP_ROUND, 
                BasicStroke.JOIN_ROUND,0,newDashPattern,0));

        renderer.setSeriesPaint(getSeriesIndex(),strokeColor);
        repaint();
        }
        
    public void buildAttributes()
        {
        // The following three variables aren't defined until AFTER construction if
        // you just define them above.  So we define them below here instead.
                                                
        dashPattern = dashPatterns[0];
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
        
        strokeColorWell = new ColorWell(strokeColor)
            {
            public Color changeColor(Color c) 
                {
                strokeColor = c;
                rebuildGraphicsDefinitions();
                return c;
                }
            };
        addLabelled("Color",strokeColorWell);
                        
        thicknessField = new NumberTextField(2.0,true)
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
        addLabelled("Width",thicknessField);
        
        dashPatternList = new JComboBox();
        dashPatternList.setEditable(false);
        dashPatternList.setModel(new DefaultComboBoxModel(new java.util.Vector(Arrays.asList(
                        new String[] { "Solid", "__  __  __", "_  _  _  _", "_ _ _ _ _", "_ _ . _ _ .", 
                                       "_ . _ . _ .", "_ . . _ . .", ". . . . . . .", ".  .  .  .  ." }))));
        dashPatternList.setSelectedIndex(0);
        dashPatternList.addActionListener(new ActionListener()
            {
            public void actionPerformed ( ActionEvent e )
                {
                dashPattern = dashPatterns[dashPatternList.getSelectedIndex()];
                rebuildGraphicsDefinitions();
                }
            });
        addLabelled("Dash",dashPatternList);
        stretchField = new NumberTextField(1.0,true)
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
