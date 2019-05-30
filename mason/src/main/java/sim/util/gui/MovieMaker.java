/*
  Copyright 2006 by Sean Luke and George Mason University
  Licensed under the Academic Free License version 3.0
  See the file "LICENSE" for more information
*/

package sim.util.gui;
import java.awt.*;
import javax.swing.*;
import java.io.*;
import java.awt.image.*;


/**
   A class which gives a GUI front-end to sim.util.media.MovieEncoder.  You create a MovieMaker by passing in some Frame as a parent.  Then you call start(image), where the image is a "typical" sized image for the movie frame.  MovieEncoder will then show dialog panels allowing the user to specify where to save the movie and what format and frame rate to use.  If the user cancels, then start(image) returns false.  Else it returns true and the MovieMaker is ready for action.
    
   <p>At this point you can start feeding the MovieMaker frames with add(image).  When you are finished, call stop() and the MovieMaker will flush out the remaining movie frames to disk and create the file.  Throw your MovieMaker away at this point.
    
   <p>MovieMaker, like MovieEncoder, relies on the Java Media Framework (JMF) to do its magic.  If JMF doesn't exist, MovieMaker doesn't produce an error; instead, it will produce a dialog box informing the user of his mistake.  MovieMaker is coded in an odd way: no actual direct references are made to MovieEncoder.  This is in case the JVM is too smart and tries to load MovieEncoder (and the JMF) immediately rather than lazily as it gets referenced by MovieMaker.

   <p><b>Note:</b> Sun's JMF spawns threads in the background which it never cleans up.
   Thus if you use this class, you'll need to call System.exit(0) to quit your program
   rather than just dropping out of main().

*/

public class MovieMaker
    {
    Frame parentForDialogs;
    Object encoder;
    Class encoderClass;
    boolean isRunning;
        
    static final float DEFAULT_FRAME_RATE = 10.0f;
    
    public MovieMaker(Frame parent)
        {
        this.parentForDialogs = parent;
        try
            {
            encoderClass = Class.forName("sim.util.media.MovieEncoder", true, Thread.currentThread().getContextClassLoader());
            }
        catch (Throwable e) { encoderClass = null; }  // JMF's not installed
        }
    
    /** Create a dialog box allowing the user to specify where to save the file, and in what format and frame rate (default = 10 frames per second), and set up the movie encoding process ready to go, using typicalImage as an example image (for size purposes).  Return false if failed to start. */
    public synchronized boolean start(BufferedImage typicalImage)
        {
        return start(typicalImage, DEFAULT_FRAME_RATE);
        }
        
    /** Create a dialog box allowing the user to specify where to save the file, and in what format and frame rate (default provided), and set up the movie encoding process ready to go, using typicalImage as an example image (for size purposes).  Return false if failed to start. */
    public synchronized boolean start(BufferedImage typicalImage, float fps)
        {
        if (isRunning) return false;
   
        int encodeFormatIndex = 0;
        
        try
            {
            // get the list of supported formats
            Object[] f = (Object[]) encoderClass.
                getMethod("getEncodingFormats", new Class[] {Float.TYPE, BufferedImage.class}).
                invoke(null, new Object[] { new Float(fps), typicalImage });
            if (f==null) return false;
            
            // init the dialog panel
            JPanel p = new JPanel();
            p.setLayout(new BorderLayout());
            
            // Word-wrap the format names for display
            String[] fmts = new String[f.length];
            
            // get the standard font
            String font = p.getFont().getFamily();
            
            // ultimate classy list mode
            for(int i=0; i<fmts.length; i++)
                fmts[i] = "<html><font face=\"" + font + "\" size=\"-2\">" + WordWrap.toHTML(WordWrap.wrap(f[i].toString(), 80)) + "</font></html>";
            
            // add widgets
            JTextField framerate = new JTextField(""+fps);
            JPanel panel = new JPanel();
            panel.setLayout(new BorderLayout());
            panel.setBorder(new javax.swing.border.TitledBorder("Frame Rate"));
            panel.add(framerate, BorderLayout.CENTER);
                        
            JPanel panel2 = new JPanel();
            panel2.setLayout(new BorderLayout());
            panel2.setBorder(new javax.swing.border.TitledBorder("Format"));
            JComboBox encoding = new JComboBox(fmts);
            if (fmts.length == 3)  // we will assume that this is the default formats, so we want the 24-bit one, which is #2
                encoding.setSelectedIndex(1);  // item 2
            panel2.add(encoding, BorderLayout.CENTER);
                        
            // add a warning
            String text1 =      "MASON uses Sun's JMF movie generation code.  JMF saves out movies in uncompressed RGB: they are gigantic. Once saved out, you need to " +
                "convert them to a better codec (we recommend H.264). " +
                "However, JMF also produces videos with an incorect codec label. " +
                "This breaks a number of movie players and converters.  Here's some useful information " +
                "on various software and how it handles JMF's bugs:\n\n" + 
                "Quicktime 10.  You can load the movie (which converts it) and save it out as H.264. " +
                "However Quicktime does not work with 16-bit RGB: use the 24 or 32 bit RGB formats in MASON.\n\n" +
                "iMovie 10.  iMovie does not work with 32-bit RGB: use the 16 or 24 RGB bit formats in MASON.\n\n"+
                "Final Cut Pro.  FCP works with any of the RGB formats.\n\n" +
                "IN SHORT: We suggest you save out in 24-bit RGB.\n\n" +
                "See http://cs.gmu.edu/~eclab/projects/mason/extensions/movies/ for more information.";
            int myNumberOfPixels = 600; // our word-wrap pixel length
            JLabel label = new JLabel();
            label.setText("<html><br><b>Notes</b><br><font size='-2'>" + 
                WordWrap.toHTML(WordWrap.wrap(text1, myNumberOfPixels, label.getFontMetrics(label.getFont()))) +
                "</font></html>");

            JPanel panel3 = new JPanel();
            panel3.setLayout(new BorderLayout());
            panel3.add(panel2, BorderLayout.NORTH);
            panel3.add(label, BorderLayout.CENTER);

            p.add(panel, BorderLayout.NORTH);
            p.add(panel3, BorderLayout.SOUTH);
            
            // ask
            if(JOptionPane.showConfirmDialog(parentForDialogs, p,"Create a Quicktime Movie...",
                    JOptionPane.OK_CANCEL_OPTION) != JOptionPane.OK_OPTION)
                return false;
     
            // get the selected fps and format
            fps = Float.valueOf(framerate.getText()).floatValue();
            encodeFormatIndex = encoding.getSelectedIndex();            // OK so now we have the index
            
            
            // ok, so now that the correct frames per second is available and the index of the
            // format with the right color depth etc. is known, re-query
            // in order to get a composite format that has both the correct fps and color depth
            
            f = (Object[])encoderClass.
                getMethod("getEncodingFormats", new Class[] {Float.TYPE, BufferedImage.class}).
                invoke(null, new Object[] { new Float(fps), typicalImage });
            
            // now choose the same one as before but with the fps
            // And we hope that the same encoding formats show up with the different framerate's query--
            // this should always be true
            
            // end dan mods
            
            FileDialog fd = new FileDialog(parentForDialogs,"Stream to Quicktime File...", FileDialog.SAVE);
            fd.setFile("Untitled.mov");
            fd.setVisible(true);;
            if (fd.getFile()!=null) 
                {
                //                encoder = new sim.util.media.MovieEncoder(fps,  // frames per second
                //                                        new File(fd.getDirectory(), ensureFileEndsWith(fd.getFile(),".mov")),
                //                                        typicalImage,
                //                                        (javax.media.Format)f[encodeFormatIndex]);
                encoder = encoderClass.getConstructor(new Class[]{
                        Float.TYPE, 
                        File.class, 
                        BufferedImage.class, 
                        Class.forName("javax.media.Format")
                        }).
                    newInstance(new Object[]{new Float(fps), 
                                             new File(fd.getDirectory(), Utilities.ensureFileEndsWith(fd.getFile(),".mov")),
                                             typicalImage,
                                             f[encodeFormatIndex]});
                }
            else return false;
            }
        catch (Throwable e) // (NoClassDefFoundError e)  // uh oh, JMF's not installed
            {
            e.printStackTrace();
            Object[] options = {"Oops"};
            JOptionPane.showOptionDialog(
                parentForDialogs,
                "JMF is not installed on your computer.\nTo create Quicktime movies of your simulation:\n\n" +
                "1. Download JMF at http://java.sun.com/products/java-media/jmf/\n" +
                "2. Mac users should download the \"Cross-platform Java\" version\n" +
                "3. Install the JMF libraries.\n" +
                "4. Make certain that the jmf.jar file is in your CLASSPATH.\n",
                "Java Media Framework (JMF) Not Installed",
                JOptionPane.OK_OPTION,
                JOptionPane.ERROR_MESSAGE,
                null, options, options[0]);
            encoder = null;
            isRunning = false;
            return false;
            }
            
        isRunning = true;
        return true;
        }
    
    /** Add an image to the movie stream.  Do this only after starting. */
    public synchronized boolean add(BufferedImage image)
        {
        if (!isRunning) return false;
        //              ((sim.util.media.MovieEncoder)encoder).add(image);
                {
                try  // NOT LIKELY TO HAPPEN
                    {
                    encoderClass.getMethod("add", new Class[]{BufferedImage.class}).
                        invoke(encoder, new Object[]{image});
                    }
                catch(Exception ex)
                    {
                    ex.printStackTrace();
                    return false;
                    }
                }
        return true;
        }
    
    /** End the movie stream, finish up writing to disk, and clean up. */
    public synchronized boolean stop()
        {
        boolean success = true;
        if (!isRunning) return false;  // not running -- why stop?
        try
            {
            //            ((sim.util.media.MovieEncoder)encoder).stop();
            success = ((Boolean)(encoderClass.getMethod("stop", new Class[0]).invoke(encoder, new Object[0]))).booleanValue();
            }
        catch(Exception ex)  // NOT LIKELY TO HAPPEN
            {
            ex.printStackTrace();
            return false;
            }
        isRunning = false;
        return success;
        }
    }
