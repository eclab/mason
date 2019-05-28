/*
  Copyright 2006 by Sean Luke and George Mason University
  Licensed under the Academic Free License version 3.0
  See the file "LICENSE" for more information
*/

package sim.util.media.chart;

import java.awt.*;
import javax.swing.*;
import java.awt.geom.*;
import java.awt.event.*;
import java.util.*;
import sim.util.gui.*;

// From JFreeChart
import org.jfree.chart.renderer.xy.*;
import org.jfree.data.general.*;
import org.jfree.chart.plot.*;

public class ScatterPlotSeriesAttributes extends SeriesAttributes
    {
    static Shape[] buildShapes()
        {
        Shape[] s = new Shape[7];
        GeneralPath g = null;
                
        // Circle
        s[0] = new Ellipse2D.Double(-3, -3, 6, 6);

        // Rectangle
        Rectangle2D.Double r = new Rectangle2D.Double(-3, -3, 6, 6);
        s[1] = r;
                 
        // Diamond
        s[2] = AffineTransform.getRotateInstance(Math.PI/4.0).createTransformedShape(r);

        // Cross +
        g = new GeneralPath(); 
        g.moveTo(-0.5f, -3); 
        g.lineTo(-0.5f, -0.5f); g.lineTo(-3, -0.5f); g.lineTo(-3, 0.5f);
        g.lineTo(-0.5f, 0.5f); g.lineTo(-0.5f, 3); g.lineTo(0.5f, 3);
        g.lineTo(0.5f, 0.5f); g.lineTo(3, 0.5f); g.lineTo(3, -0.5f);
        g.lineTo(0.5f, -0.5f); g.lineTo(0.5f, -3); g.closePath();
        s[3] = g;
                
        // X 
        s[4] = g.createTransformedShape(AffineTransform.getRotateInstance(Math.PI/4.0));
                
        // Up Triangle
        g = new GeneralPath();
        g.moveTo(0f, -3); 
        g.lineTo(-3, 3); g.lineTo(3, 3); g.closePath();
        s[5] = g;
                
        // Down Triangle
        s[6] = g.createTransformedShape(AffineTransform.getRotateInstance(Math.PI));
                
        return s;
        }
        
    final static Shape[] shapes = buildShapes();
    final static String[] shapeNames = new String[]
    {
    "Circle", "Square", "Diamond", "Cross", "X", "Up Triangle", "Down Triangle"
    };
                
    double[][] values; 
    public double[][] getValues() { return values; }
    public void setValues(double[][] vals) 
        {
        if (vals != null)
            {
            vals = (double[][]) (vals.clone());
            for(int i = 0; i < vals.length; i++)
                vals[i] = (double[]) (vals[i].clone());
            }
        values = vals;
        }

    Color color;
    ColorWell colorWell;
    double opacity;
    NumberTextField opacityField;
                    
    public void setSymbolOpacity(double value) { opacityField.setValue(opacityField.newValue(value));  }
    public double getSymbolOpacity() { return opacityField.getValue(); }
    
    public void setSymbolColor(Color value) { colorWell.setColor(color = value); }
    public Color getSymbolColor() { return color; }

    int shapeNum = 0;
    Shape shape = shapes[shapeNum];
    JComboBox shapeList;

    public void setShapeNum(int value) 
        { 
        if (value >= 0 && value < shapes.length) 
            { 
            shapeList.setSelectedIndex(value);
            shapeNum = value;
            shape = shapes[shapeNum];
            }
        }
    public int getShapeNum() { return shapeNum; }
    public Shape getShape() { return shape; }
        

    /** Produces a ScatterPlotSeriesAttributes object with the given generator, series name, series index,
        and desire to display margin options. */
    public ScatterPlotSeriesAttributes(ChartGenerator generator, String name, int index, double[][] values, SeriesChangeListener stoppable)
        { 
        super(generator, name, index, stoppable);
                
        setValues(values);
        super.setSeriesName(name);  // just set the name, don't update.  Bypasses standard method below.

        // increment shape counter
        ((ScatterPlotGenerator)generator).shapeCounter++;
        if (((ScatterPlotGenerator)generator).shapeCounter >= shapes.length)
            ((ScatterPlotGenerator)generator).shapeCounter = 0;
                        
        // set the shape
        shapeNum = ((ScatterPlotGenerator)generator).shapeCounter;
        shape = shapes[shapeNum];
        XYLineAndShapeRenderer renderer = (XYLineAndShapeRenderer)(((XYPlot)getPlot()).getRenderer());
        renderer.setSeriesShape(getSeriesIndex(), shape);
        renderer.setAutoPopulateSeriesShape(false);
        }

    public void setSeriesName(String val) 
        {
        super.setSeriesName(val);
        ((ScatterPlotGenerator)generator).update();
        }
                        
    public void rebuildGraphicsDefinitions()
        {
        XYLineAndShapeRenderer renderer = (XYLineAndShapeRenderer)(((XYPlot)getPlot()).getRenderer());
        renderer.setSeriesPaint(getSeriesIndex(), reviseColor(color, opacity));
        // shape may be null at this point, that's fine
        renderer.setSeriesShape(getSeriesIndex(), shape);
        renderer.setAutoPopulateSeriesShape(false);
        repaint();
        }
        
    public void buildAttributes()
        {
        // The following three variables aren't defined until AFTER construction if
        // you just define them above.  So we define them below here instead.
        opacity = 1.0;

        // NOTE:
        // Paint paint = renderer.getSeriesPaint(getSeriesIndex());        
        // In JFreeChart 1.0.6 getSeriesPaint returns null!!!
        // You need lookupSeriesPaint(), but that's not backward compatible.
        // The only thing consistent in all versions is getItemPaint 
        // (which looks like a gross miss-use, but gets the job done)
                
        color = (Color) ((((XYPlot)getPlot()).getRenderer()).getItemPaint(getSeriesIndex(), -1));

        colorWell = new ColorWell(color)
            {
            public Color changeColor(Color c) 
                {
                color = c;
                rebuildGraphicsDefinitions();
                return c;
                }
            };

        addLabelled("Color", colorWell);

        opacityField = new NumberTextField("Opacity ", opacity,1.0,0.125)
            {
            public double newValue(double newValue) 
                {
                if (newValue < 0.0 || newValue > 1.0) 
                    newValue = currentValue;
                opacity = (float)newValue;
                rebuildGraphicsDefinitions();
                return newValue;
                }
            };
        addLabelled("",opacityField);

        shapeList = new JComboBox();
        shapeList.setEditable(false);
        shapeList.setModel(new DefaultComboBoxModel(new java.util.Vector(Arrays.asList(shapeNames))));
        shapeList.setSelectedIndex(shapeNum);
        shapeList.addActionListener(new ActionListener()
            {
            public void actionPerformed ( ActionEvent e )
                {
                shapeNum = shapeList.getSelectedIndex();
                shape = shapes[shapeNum];
                rebuildGraphicsDefinitions();
                }
            });
        addLabelled("Shape",shapeList);
        }
    }

