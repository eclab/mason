/*
  Copyright 2006 by Sean Luke and George Mason University
  Licensed under the Academic Free License version 3.0
  See the file "LICENSE" for more information
*/

package sim.util;
import java.awt.*;
import javax.swing.*;
import java.io.*;

/** Various static utility methods. */

public class Utilities
    {
    /** Returns a filename guaranteed to end with the given ending. */
    public static String ensureFileEndsWith(String filename, String ending)
        {
        // do we end with the string?
        if (filename.regionMatches(false,filename.length()-ending.length(),ending,0,ending.length()))
            return filename;
        else return filename + ending;
        }
                
    /** Does a repaint that is guaranteed to work (on some systems, plain repaint())
        fails if there's lots of updates going on as is the case in our simulator thread.  Notably,
        MacOS X 1.3.1 has problems. */
    public static void doEnsuredRepaint(final Component component)
        {
        SwingUtilities.invokeLater(new Runnable()
            {
            public void run()
                {
                if (component!=null) component.repaint();
                }
            });
        }
                
    /** Schedule something to occur at some specified point in the future in the Swing Event thread. */
    public static Thread doLater(final long milliseconds, final Runnable doThis)
        {
        Thread thread = new Thread(new Runnable()
            {
            public void run()
                {
                try
                    {
                    Thread.sleep(milliseconds);
                    SwingUtilities.invokeAndWait(doThis);
                    }
                catch (InterruptedException e) { /* shouldn't happen -- we could do an invokeAndWait in case tho */ }
                catch (java.lang.reflect.InvocationTargetException e) { /* shouldn't happen */ }
                }
            });
        thread.start();
        return thread;
        }
                
    static final int DIALOG_WIDTH = 400;  // in pixels
    static final int TRACE_FRAME_WIDTH = 600;
    static final int TRACE_FRAME_HEIGHT = 400;
    
    /** Pops up an error dialog box.  error should be the error proper, and description should
        be some user-informative item that's shown first (the user must explicitly ask to be shown
        the raw error itself).  frame is a window on which the error box should be centered: if it
        is null, the error box will be centered in the screen. The error is also printed to the console.  */
    
    public static void informOfError(Throwable error, String description, JFrame frame)
        {
        error.printStackTrace();
        Object[] options = { "Show Trace", "Okay" };
        JLabel label = new JLabel();
        Font labelFont = label.getFont();
        Font boldFont = labelFont.deriveFont(Font.BOLD);
        FontMetrics boldFontMetrics = label.getFontMetrics(boldFont);
        Font smallFont = labelFont.deriveFont(labelFont.getSize2D() - 2.0f);
        FontMetrics smallFontMetrics = label.getFontMetrics(smallFont);
        
        label.setText("<html><p style=\"padding-top: 12pt; padding-right: 50pt; font: " + 
            boldFont.getSize() + "pt " + boldFont.getFamily() + ";\"><b>"+
            WordWrap.toHTML(WordWrap.wrap(""+description, DIALOG_WIDTH, boldFontMetrics)) + "</b></p>" + 
            "<p style=\"padding-top: 12pt; padding-right: 50pt; padding-bottom: 12pt; font: " + 
            smallFont.getSize() + "pt " + smallFont.getFamily() + ";\">" +
            error + "</p></html>");
        int n = JOptionPane.showOptionDialog(frame, label, "Error",
            JOptionPane.YES_NO_OPTION,  
            JOptionPane.ERROR_MESSAGE,  
            null,   
            //don't use a custom Icon
            options, //the titles of buttons
            options[1]); //default button title
        if (n == 0)
            {
            StringWriter writer = new StringWriter();
            PrintWriter pWriter = new PrintWriter(writer);
            error.printStackTrace(pWriter);
            pWriter.flush();
            JTextArea area = new JTextArea(writer.toString());
            area.setFont(new Font("Monospaced",Font.PLAIN,12));
            JScrollPane pane = new JScrollPane(area)
                {
                public Dimension getPreferredSize()
                    {
                    return new Dimension(TRACE_FRAME_WIDTH,TRACE_FRAME_HEIGHT);
                    }
                public Dimension getMinimumSize()
                    {
                    return new Dimension(TRACE_FRAME_WIDTH,TRACE_FRAME_HEIGHT);
                    }
                };
            JOptionPane.showMessageDialog(null, pane);
            }
        }

    /** Pops up an message dialog box.  frame is a window on which the error box should be centered: if it
        is null, the error box will be centered in the screen. The error is also printed to the console.  */
    
    public static void inform(String description, String subDescription, JFrame frame)
        {
        Object[] options = { "Okay" };
        JLabel label = new JLabel();
        Font labelFont = label.getFont();
        Font boldFont = labelFont.deriveFont(Font.BOLD);
        FontMetrics boldFontMetrics = label.getFontMetrics(boldFont);
        Font smallFont = labelFont.deriveFont(labelFont.getSize2D() - 2.0f);
        FontMetrics smallFontMetrics = label.getFontMetrics(smallFont);

        label.setText("<html><p style=\"padding-top: 12pt; padding-right: 50pt; font: " + 
            boldFont.getSize() + "pt " + boldFont.getFamily() + ";\"><b>"+
            WordWrap.toHTML(WordWrap.wrap(""+description, DIALOG_WIDTH, boldFontMetrics)) + "</b></p>" + 
            "<p style=\"padding-top: 12pt; padding-right: 50pt; padding-bottom: 12pt; font: " + 
            smallFont.getSize() + "pt " + smallFont.getFamily() + ";\">" +
            WordWrap.toHTML(WordWrap.wrap(""+subDescription, DIALOG_WIDTH, smallFontMetrics))  + "</p></html>");
        JOptionPane.showMessageDialog(frame, label, "Error",
            JOptionPane.INFORMATION_MESSAGE);
        }

    }
