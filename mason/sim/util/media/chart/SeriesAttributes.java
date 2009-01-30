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

/** The superclass for the series-attributes widgets used by subclasses of ChartGenerator to let the user
    control individual series' features.  SeriesAttributes will be placed in the list at the bottom-left of
    the ChartGenerator window, and series will be assigned a unique SeriesAttributes to control it.
        
    <p>SeriesAttributes need to override the getSeriesName and setSeriesName methods, as well as the rebuildGraphicsDefinitions
    and buildAttributes methods. */

public abstract class SeriesAttributes extends LabelledList
    {
    /** The index of the series that this SeriesAttributes is responsible for. */
    protected int seriesIndex;
    /** The ChartGenerator which holds the series that this SeriesAttributes is responsible for. */
    protected ChartGenerator generator;
    
    /** Sets the name of the series. */
    public abstract void setSeriesName(String val);
    /** Returns the name of the series. */
    public abstract String getSeriesName();
                
    /** Updates features of the series to reflect the current widget settings as specified by the user. */
    public abstract void rebuildGraphicsDefinitions();
                
    /** Constructs the widget by adding items to the LabelledList.  Will be called in the middle of the
        SeriesAttributes constructor, and so certain instance variables may not yet have been initialized. */
    public abstract void buildAttributes();

    /** Given an opaque color and a desired opacity (from 0.0 to 1.0), returns a new color of the same tint but with
        the given opacity. */
    protected Color reviseColor(Color c, double opacity)
        {
        return new Color(c.getRed(),c.getGreen(),c.getBlue(),(int)(opacity*255));
        }

    /** Returns the Chart's Plot cast into an XYPlot.  If it's not an XYPlot, this method will generate an error. */
    public XYPlot getPlot()
        {
        return generator.getChartPanel().getChart().getXYPlot();
        }
                        
    /** Returns the ChartGenerator holding the series this SeriesAttributes is responsible for. */
    public ChartGenerator getGenerator() { return generator; }
                
    /** Returns the index of the series. */
    public int getSeriesIndex() { return seriesIndex; }
    /** Sets the index of the series -- Do NOT call this if you want to move the series order; instead call moveSeries
        in the Generator. */
    public void setSeriesIndex(int val) { seriesIndex = val; }
                
                
    protected XYItemRenderer getRenderer()
        {
        return getPlot().getRenderer();
        }
        
    public static final ImageIcon I_DOWN = iconFor("DownArrow.png");
    public static final ImageIcon I_DOWN_PRESSED = iconFor("DownArrowPressed.png");
    public static final ImageIcon I_CLOSE = iconFor("CloseBox.png");
    public static final ImageIcon I_CLOSE_PRESSED = iconFor("CloseBoxPressed.png");
    public static final ImageIcon I_UP = iconFor("UpArrow.png");
    public static final ImageIcon I_UP_PRESSED = iconFor("UpArrowPressed.png");

    public static ImageIcon iconFor(String name)
        {
        return new ImageIcon(SeriesAttributes.class.getResource(name));
        }
        
    public Box manipulators;
        
    public void setManipulatorsVisible(boolean visible)
        {
        manipulators.setVisible(visible);
        }
        
    public void buildManipulators()
        {
        JButton removeButton = new JButton(I_CLOSE);
        removeButton.setPressedIcon(I_CLOSE_PRESSED);
        removeButton.setBorder(BorderFactory.createEmptyBorder(1,1,1,1));//4,4,4,4));
        removeButton.setBorderPainted(false);
        removeButton.setContentAreaFilled(false);
        removeButton.addActionListener(new ActionListener()
            {
            public void actionPerformed ( ActionEvent e )
                {
                if (JOptionPane.showOptionDialog(
                        null,"Remove the Series " + getSeriesName() + "?","Confirm",
                        JOptionPane.YES_NO_OPTION,
                        JOptionPane.QUESTION_MESSAGE,null,
                        new Object[] { "Remove", "Cancel" },
                        null) == 0)  // remove
                    getGenerator().removeSeries(getSeriesIndex());
                }
            });
        
        JButton upButton = new JButton(I_UP);
        upButton.setPressedIcon(I_UP_PRESSED);
        upButton.setBorder(BorderFactory.createEmptyBorder(1,1,1,1));//4,4,4,4));
        upButton.setBorderPainted(false);
        upButton.setContentAreaFilled(false);
        upButton.addActionListener(new ActionListener()
            {
            public void actionPerformed ( ActionEvent e )
                {
                getGenerator().moveSeries(getSeriesIndex(), true);
                }
            });
        
        JButton downButton = new JButton(I_DOWN);
        downButton.setPressedIcon(I_DOWN_PRESSED);
        downButton.setBorder(BorderFactory.createEmptyBorder(1,1,1,1));//4,4,4,4));
        downButton.setBorderPainted(false);
        downButton.setContentAreaFilled(false);
        downButton.addActionListener(new ActionListener()
            {
            public void actionPerformed ( ActionEvent e )
                {
                getGenerator().moveSeries(getSeriesIndex(), false);
                }
            });
        
        manipulators.add(removeButton);
        manipulators.add(upButton);
        manipulators.add(downButton);
        }
        
        
    /** Builds a SeriesAttributes with the provided generator, name for the series, and index for the series.  Calls
        buildAttributes to construct custom elements in the LabelledList, then finally calls rebuildGraphicsDefinitions()
        to update the series. */
    public SeriesAttributes(ChartGenerator generator, String name, int index)
        {
        super(name);
        this.generator = generator;
        seriesIndex = index;
        final JCheckBox check = new JCheckBox();
        check.setSelected(true);
        check.addActionListener(new ActionListener()
            {
            public void actionPerformed(ActionEvent e)
                {
                getRenderer().setSeriesVisible(getSeriesIndex(),
                    new Boolean(check.isSelected()));  // why in the WORLD is it Boolean?
                }
            });

        manipulators = new Box(BoxLayout.X_AXIS);
        buildManipulators();
        JLabel spacer = new JLabel("");
        spacer.setBorder(BorderFactory.createEmptyBorder(0,10,0,0));
        Box b = new Box(BoxLayout.X_AXIS);
        b.add(check);
        b.add(spacer);
        b.add(manipulators);
        b.add(Box.createGlue());
        addLabelled("Show", b);

        final PropertyField nameF = new PropertyField(name)
            {
            public String newValue(String newValue)
                {
                setSeriesName(newValue);
                getGenerator().getChartPanel().repaint();
                return newValue;
                }
            };
/*        nameF.addActionListener(new ActionListener()
          {
          public void actionPerformed(ActionEvent e)
          {
          String n = nameF.getText();
          setSeriesName(n);
          getGenerator().getChartPanel().repaint();
          }
          });*/
            
        addLabelled("Series",nameF);
                        
        buildAttributes();
        rebuildGraphicsDefinitions();
        }
    }
