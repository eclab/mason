/*
  Copyright 2006 by Sean Luke and George Mason University
  Licensed under the Academic Free License version 3.0
  See the file "LICENSE" for more information
*/

package sim.util.gui;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

/** <p>A very simple histogram class.  Displays values as different colors left to right.
    Tooltips pop up describing the actual buckets and their densities.
    
    <p>This class can be used to describe any one-dimensional array of doubles: just provide 
    an array of doubles to setBuckets and it'll show them on-screen.  If you change the doubles in that
    array, no need to call setBuckets again: as soon as a repaint occurs, MiniHistogram will update them.
    You can also provide a
    set of bucket labels, one for each bucket, which get popped up in the tooltip along with the current
    bucket value.
    
    <p>As the class was created to describe histograms in particular, it has two functions 
    (makeBuckets and makeBucketLabels) which do just that.

    Sample usage.  The following values are placed into a histogram of four buckets.
    <tt><pre>
    MiniHistogram m = new MiniHistogram();
    double[] d = new double[] {1, 1, 1, 1, 2, 2, 5, 1, 2, 3, 1, 2, 1, 1, 5, 4};
    m.setBuckets(m.makeBuckets(d, 4, 1, 5, false));
    m.setBucketLabels(m.makeBucketLabels(4, 1, 5));
    </pre></tt>
*/

public class MiniHistogram extends JComponent
    {
    final static JLabel DEFAULT_SIZE_COMPARISON = new JLabel("X");
    double[] buckets;
    String[] labels;
    
    public MiniHistogram()
        {
        setBuckets(new double[0]);
        addMouseListener(adapter);
        addMouseMotionListener(motionAdapter);
        setBackground(DEFAULT_SIZE_COMPARISON.getBackground());
        }
                
    public MiniHistogram(double[] buckets, String[] labels)
        {
        this();
        setBucketsAndLabels(buckets,labels);
        setBackground(DEFAULT_SIZE_COMPARISON.getBackground());
        setOpaque(true);
        }

    public Dimension getPreferredSize() 
        {
        return DEFAULT_SIZE_COMPARISON.getPreferredSize();
        }

    public Dimension getMinimumSize() 
        { 
        return DEFAULT_SIZE_COMPARISON.getMinimumSize();
        }
                
    /** Sets the displayed bucket array.  If you change values in the provided array, then
        MiniHistogram will update itself appropriately on next repaint. */
    public void setBuckets(double[] buckets)
        {
        if (buckets == null) buckets = new double[0];
        this.buckets = buckets;
        repaint();
        }
   
    /** Sets labels for the buckets provided in setBuckets. */
    public void setBucketLabels(String[] labels)
        {
        this.labels = labels;
        }
   
    public void setBucketsAndLabels(double[] buckets, String[] labels)
        {
        setBuckets(buckets);
        setBucketLabels(labels);
        }
   
    MouseMotionAdapter motionAdapter = new MouseMotionAdapter()
        {
        public void mouseMoved(MouseEvent event)
            {
            String s = null;
            if (buckets !=null)
                {
                int x = (int)((event.getX() * buckets.length) / (double)(getBounds().width));
                if (labels != null && x < labels.length) 
                    s = "<html><font size=\"-1\" face=\"" + getFont().getFamily() + "\">" +
                        "Bucket: " + x + "<br>Range: " + labels[x] + "<br>Value: " + buckets[x] +
                        "</font></html>";
                else if (buckets != null && buckets.length != 0)
                    s = "<html><font size=\"-1\" face=\"" + getFont().getFamily() + "\">" +
                        "Bucket: " + x + "<br>>Value: " + buckets[x] +
                        "</font></html>";
                else s=null;
                }
                        
            if ((s != null) && !s.equalsIgnoreCase(getToolTipText()))
                setToolTipText(s);
            }
        };
    
    // the purpose of this adapter is to force the tooltip manager to turn on
    // its initial delay
    MouseAdapter adapter = new MouseAdapter()
        {
        int initialDelay;
        int dismissDelay;
        int reshowDelay;
        public void mouseEntered(MouseEvent event)
            {
            initialDelay = ToolTipManager.sharedInstance().getInitialDelay();
            ToolTipManager.sharedInstance().setInitialDelay(0);
            dismissDelay = ToolTipManager.sharedInstance().getDismissDelay();
            ToolTipManager.sharedInstance().setDismissDelay(Integer.MAX_VALUE);
            reshowDelay = ToolTipManager.sharedInstance().getReshowDelay();
            ToolTipManager.sharedInstance().setReshowDelay(0);
            }

        public void mouseExited(MouseEvent event)
            {
            ToolTipManager.sharedInstance().setInitialDelay(initialDelay);
            ToolTipManager.sharedInstance().setDismissDelay(dismissDelay);
            ToolTipManager.sharedInstance().setReshowDelay(reshowDelay);
            }
        };

    public synchronized void paintComponent(final Graphics graphics)
        {
        int len = 0;
        if (buckets != null) len = buckets.length;
        if (len==0) return; // don't bother drawing
        Rectangle bounds = getBounds();
        graphics.setColor(getBackground());
        graphics.fillRect(0,0,bounds.width,bounds.height);
        int height = bounds.height - 2;
        if (height <= 0) return;
                
        graphics.setColor(getForeground());
                
        // find maxbucket, minbucket
        double maxbucket = buckets[0];
        double minbucket = buckets[0];
        for(int i=1;i<len;i++)
            {
            if (buckets[i] < minbucket) minbucket = buckets[i];
            if (buckets[i] > maxbucket) maxbucket = buckets[i];
            }
                        
        if (maxbucket==minbucket)
            {
            graphics.fillRect(0,0,bounds.width,height);
            return;
            }
                
        for(int i=0;i<len;i++)
            {
            int x0 = (int)(bounds.width / (double)len * i);
            int x1 = (int)(bounds.width / (double)len * (i+1));
            int y0 = 0;
            int y1;
            if (buckets[i]==Double.POSITIVE_INFINITY) y1 = height;
            else if (buckets[i]!=buckets[i]) y1 = 0;  // it's NaN
            else y1 = (int)(height * ((buckets[i] - minbucket) / (double)(maxbucket-minbucket)));
            // flip the y's
            y0 = height - y0;
            y1 = height - y1;
            graphics.fillRect(x0+1,y1+1,x1-x0+1,y0-y1 + 1);
            }
        }
    
    /** Generates a set of <i>numBuckets</i> bucket labels appropriate for use in a histogram.
        The values <i>numBuckets</i>, <i>min</i>, and <i>max</i> should be the same values you had
        provided to makeBuckets(...).  Pass the resulting array into the setBuckets(...) function.*/

    public static String[] makeBucketLabels(int numBuckets, double min, double max, boolean logScale)
        {
        String[] s = new String[numBuckets];
        
        if (min>max) {double tmp = min; min = max; max = tmp;} // duh, stupid user
        else if (min==max) { s[0] = "["+min+"..."+max+"]"; for(int x=1;x<s.length;x++) s[x] = ""; return s;}  // duh, stupider user
        else if (logScale)
            {
            min = Math.log(min);
            max = Math.log(max);
            for(int x=0;x<s.length;x++)
                s[x] = "[" + Math.exp((x/(double)numBuckets)*(max-min) + min) + 
                    "..." + Math.exp(((x+1)/(double)numBuckets)*(max-min) + min) +
                    (x==s.length-1 ? "]" : ")");
            }
        else
            for(int x=0;x<s.length;x++)
                s[x] = "[" + ((x/(double)numBuckets)*(max-min) + min) + 
                    "..." + (((x+1)/(double)numBuckets)*(max-min) + min) +
                    (x==s.length-1 ? "]" : ")");
        return s;
        }

    public static double[] makeBucketPositions(int numBuckets, double min, double max, boolean logScale)
        {
        double[] s = new double[numBuckets];
        
        if (min>=max) { for(int x=0;x<s.length;x++) s[x] = max; return s;}  // duh, stupider user
        else if (logScale)
            {
            min = Math.log(min);
            max = Math.log(max);
            for(int x=0;x<s.length;x++)
                s[x] =  (Math.exp((x/(double)numBuckets)*(max-min) + min) + 
                    Math.exp(((x+1)/(double)numBuckets)*(max-min) + min)) / 2;
            }
        else
            for(int x=0;x<s.length;x++)
                s[x] =  (((x/(double)numBuckets)*(max-min) + min) + 
                    (((x+1)/(double)numBuckets)*(max-min) + min)) / 2;
        return s;
        }

    /** Returns the minimum over the provided vals.  You might use this to set the minimum in makeBuckets
        if you don't have a prescribed minimum */
    public static double minimum(double[] vals)
        {
        double min = Double.POSITIVE_INFINITY;
        for(int i=0;i<vals.length;i++)   // gather minimum
            if (min > vals[i])
                min = vals[i];
        return min;
        }
                
    /** Returns the minimum over the provided vals.  You might use this to set the minimum in makeBuckets
        if you don't have a prescribed minimum */
    public static double maximum(double[] vals)
        {
        double max = Double.NEGATIVE_INFINITY;
        for(int i=0;i<vals.length;i++)   // gather maximum
            if (max < vals[i])
                max = vals[i];
        return max;
        }

    /** Generates a set of <i>numBuckets</i> buckets describing a histogram over the provided values in <i>vals</i>.
        <i>min</i> and <i>max</i> describe the ends of the histogram, inclusive: values outside those ranges are discarded.
        You can tell the histogram to bucket based on a log scale if you like (min and max are computed prior to log)),
        and in this case all values less than 0 are discarded as well.  Pass the resulting array into the setBuckets(...) function.
    */
    
    public static double[] makeBuckets(double[] vals, int numBuckets, double min, double max, boolean logScale)
        {
        double[] b = new double[numBuckets];
        if (vals == null || numBuckets == 0) return b;         // duh, stupid user

        if (logScale) { min = Math.log(min); max = Math.log(max); }
        
        if (min>max) {double tmp = min; min = max; max = tmp;} // duh, stupid user
        else if (min==max) { b[0] += vals.length; return b; }  // duh, stupider user

        int count = 0;
        for(int x=0;x<vals.length;x++)
            {
            double v = vals[x];
            if (logScale)
                {
                if (v<0) continue;
                v = Math.log(v);
                }
            if (v < min || v > max) continue;
            int bucketnum = (int)((v - min) * numBuckets / (max-min));
            if (bucketnum >= numBuckets) bucketnum = numBuckets-1;
            b[bucketnum]++;
            count++;
            }
            
        if (count != 0) 
            for(int x=0;x<b.length;x++)
                b[x] /= count;
        
        return b;
        }
    }
    
    
