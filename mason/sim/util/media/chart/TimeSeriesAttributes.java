/*
  Copyright 2006 by Sean Luke and George Mason University
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
import sim.util.*;

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
    static final float[][] dashPatterns = 
        { 
        { DASH, 0.0f }, // --------
            { DASH * 2, SKIP }, // -- -- --
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
                
    public void setThickness(double value) { thicknessField.setValue(thicknessField.newValue(value));  }
    public double getThickness() { return (double)(thicknessField.getValue()); }

    public void setStretch(double value) { stretchField.setValue(stretchField.newValue(value));  }
    public double getStretch() { return (double)(stretchField.getValue()); }

    public void setDashPattern(int value) 
        { 
        if (value >= 0 && value < dashPatterns.length) 
            { 
            dashPatternList.setSelectedIndex(value);
            dashPattern = dashPatterns[value];
            }
        }
    public int getDashPattern() { return dashPatternList.getSelectedIndex(); }

    public void setStrokeColor(Color value) { strokeColorWell.setColor(strokeColor = value);}
    public Color getStrokeColor() { return strokeColor; }

    /** The time series in question.  */
    XYSeries series;
    public void setName(String val) { series.setKey(val); }
    public String getSeriesName() { return "" + series.getKey(); }
                
    /** Builds a TimeSeriesAttributes with the given generator, series, and index for the series. */
    public TimeSeriesAttributes(ChartGenerator generator, XYSeries series, int index, SeriesChangeListener stoppable)
        { 
        super(generator, "" + series.getKey(), index, stoppable);
        this.series = series;
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

        // NOTE:
        // Paint paint = renderer.getSeriesPaint(getSeriesIndex());        
        // In JFreeChart 1.0.6 getSeriesPaint returns null!!!
        // You need lookupSeriesPaint(), but that's not backward compatible.
        // The only thing consistent in all versions is getItemPaint 
        // (which looks like a gross miss-use, but gets the job done)
                
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


    public boolean possiblyCull()
        {
        DataCuller dataCuller = ((TimeSeriesChartGenerator)generator).getDataCuller();
        if(dataCuller!=null && dataCuller.tooManyPoints(series.getItemCount()))
            {
            deleteItems(dataCuller.cull(getXValues(), true));
            return true;
            }
        else
            return false;
        }
                
    static Bag tmpBag = new Bag();
    void deleteItems(IntBag items)
        {
        if(items.numObjs==0)
            return;

        tmpBag.clear();
        int currentTabooIndex = 0;
        int currentTaboo = items.objs[0];
        Iterator iter = series.getItems().iterator();
        int index=0;
        while(iter.hasNext())
            {
            Object o = iter.next();
            if(index==currentTaboo)
                {
                //skip the copy, let's move on to next taboo index
                if(currentTabooIndex<items.numObjs-1)
                    {
                    currentTabooIndex++;
                    currentTaboo = items.objs[currentTabooIndex];
                    }
                else
                    currentTaboo=-1;//no more taboos
                }
            else//save o
                tmpBag.add(o);
            index++;
            }
        //now we clear the series and then put back the saved objects only.
        series.clear();
        //In my test this did not cause the chart to flicker.
        //But if it does, one could do an update for the part the will be refill and 
        //only clear the rest using delete(start, end).
        for(int i=0;i<tmpBag.numObjs;i++)
            series.add((XYDataItem)(tmpBag.objs[i]), false);//no notifying just yet.
        tmpBag.clear();
        //it doesn't matter that I clear this twice in a row 
        //(once here, once at next time through this fn), the second time is O(1).
        series.fireSeriesChanged();
        }
                
    double[] getXValues()
        {
        double[] xValues = new double[series.getItemCount()];
        for(int i=0;i<xValues.length;i++)
            xValues[i]=series.getX(i).doubleValue();
        return xValues;
        }





    }
