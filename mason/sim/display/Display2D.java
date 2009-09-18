/*
  Copyright 2006 by Sean Luke and George Mason University
  Licensed under the Academic Free License version 3.0
  See the file "LICENSE" for more information
*/

package sim.display;
import sim.portrayal.*;
import sim.engine.*;
import java.awt.*;
import java.awt.geom.*;
import java.awt.image.*;
import javax.swing.*;
import java.awt.event.*;
import java.util.*;
import sim.util.Bag;
import java.io.*;
import sim.util.gui.*;
import sim.util.media.*;
import sim.util.*;

/**
   Display2D holds, displays, and manipulates 2D Portrayal objects, allowing the user to scale them,
   scroll them, change how often they're updated, take snapshots, and generate Quicktime movies.
   Display2D is Steppable, and each time it is stepped it redraws itself.  Display2D also handles
   double-click events, routing them to the underlying portrayals as inspector requests.

   <p>In addition to various GUI widgets, Display2D holds a JScrollView which in turn holds a
   Display2D.InnerDisplay2D (a JComponent responsible for doing the actual drawing).  Display2D can be placed
   in a JFrame; indeed it provides a convenience function to sprout its own JFrame with the method
   createFrame().  You can put Display2D in your own JFrame if you like, but you should try to call
   Display2D.quit() when the frame is disposed.

   <p>Display2D's constructor takes a height and a width; this will be the "expected" height and
   width of the underlying portrayal region when the Display2D is scaled to 1.0 (the default).
   The portrayals will also have an origin at (0,0) -- the top left corner.  Display2D will automatically
   clip the portrayals to the area (0,0) to (width * scale, height * scale).

   <p>Display2D's step() method is typically called from the underlying schedule thread; this means
   that it has to be careful about painting as Swing widgets expect to be painted in the event loop thread.
   Display2D handles this in two ways.  First, on MacOS X, the step() method calls repaint(), which will
   in turn call paintComponent() from the event loop thread at a time when the underlying schedule thread
   is doing nothing -- see Console.  Second, on Windows and XWindows, the step() method immediately calls
   paintComponent().  Different OSes do it differently because MacOS X is far more efficient using standard
   repaint() calls, which get routed through Quartz.  The step() method also updates various widgets using
   SwingUtilities.invokeLater().
*/

public class Display2D extends JComponent implements Steppable
    {
    /** Option pane */
    public class OptionPane extends JFrame
        {
        // buffer stuff
        public int buffering;
        
        public JRadioButton useNoBuffer = new JRadioButton("By Drawing Separate Rectangles");
        public JRadioButton useBuffer = new JRadioButton("Using a Stretched Image");
        public JRadioButton useDefault = new JRadioButton("Let the Program Decide How");
        public ButtonGroup usageGroup = new ButtonGroup();
        
        public JCheckBox antialias = new JCheckBox("Antialias Graphics");
        public JCheckBox alphaInterpolation = new JCheckBox("Better Transparency");
        public JCheckBox interpolation = new JCheckBox("Bilinear Interpolation of Images");
        public JCheckBox tooltips = new JCheckBox("Tool Tips");
        
        public NumberTextField xOffsetField = new NumberTextField(0,1,50)
            {
            public double newValue(final double val)
                {
                double scale = getScale();
                insideDisplay.xOffset = val / scale;
                Display2D.this.repaint();  // redraw the inside display
                return insideDisplay.xOffset * scale;
                }
            };
            
        public NumberTextField yOffsetField = new NumberTextField(0,1,50)
            {
            public double newValue(final double val)
                {
                double scale = getScale();
                insideDisplay.yOffset = val / scale;
                Display2D.this.repaint();  // redraw the inside display
                return insideDisplay.yOffset * scale;
                }
            };

        public OptionPane(String title)
            {
            super(title);
            useDefault.setSelected(true);
            useNoBuffer.setToolTipText("<html>When not using transparency on Windows/XWindows,<br>this method is often (but not always) faster</html>");
            usageGroup.add(useNoBuffer);
            usageGroup.add(useBuffer);
            useBuffer.setToolTipText("<html>When using transparency, <i>or</i> when on a Mac,<br>this method is usually faster, but may require more<br>memory (especially on Windows/XWindows) --<br>increasing heap size can help performance.</html>");
            usageGroup.add(useDefault);
            
            JPanel p2 = new JPanel();
            
            Box b = new Box(BoxLayout.Y_AXIS);
            b.add(useNoBuffer);
            b.add(useBuffer);
            b.add(useDefault);
            JPanel p = new JPanel();
            p.setLayout(new BorderLayout());
            p.setBorder(new javax.swing.border.TitledBorder("Draw Grids of Rectangles..."));
            p.add(b,BorderLayout.CENTER);
            p2.setLayout(new BorderLayout());
            p2.add(p,BorderLayout.NORTH);

            LabelledList l = new LabelledList("Offset in Pixels");
            l.addLabelled("X Offset", xOffsetField);
            l.addLabelled("Y Offset", yOffsetField);
            p2.add(l,BorderLayout.CENTER);
            getContentPane().add(p2,BorderLayout.NORTH);

            b = new Box(BoxLayout.Y_AXIS);
            b.add(antialias);
            b.add(interpolation);
            b.add(alphaInterpolation);
            b.add(tooltips);
            p = new JPanel();
            p.setLayout(new BorderLayout());
            p.setBorder(new javax.swing.border.TitledBorder("Graphics Features"));
            p.add(b,BorderLayout.CENTER);
            getContentPane().add(p,BorderLayout.CENTER);
            
            ActionListener listener = new ActionListener()
                {
                public void actionPerformed(ActionEvent e)
                    {
                    useTooltips = tooltips.isSelected();
                    if (useDefault.isSelected())
                        buffering = FieldPortrayal2D.DEFAULT;
                    else if (useBuffer.isSelected())
                        buffering = FieldPortrayal2D.USE_BUFFER;
                    else buffering = FieldPortrayal2D.DONT_USE_BUFFER;
                    insideDisplay.setupHints(antialias.isSelected(), alphaInterpolation.isSelected(), interpolation.isSelected());
                    Display2D.this.repaint();  // redraw the inside display
                    }
                };
            useNoBuffer.addActionListener(listener);
            useBuffer.addActionListener(listener);
            useDefault.addActionListener(listener);
            antialias.addActionListener(listener);
            alphaInterpolation.addActionListener(listener);
            interpolation.addActionListener(listener);
            tooltips.addActionListener(listener);
            pack();
            }
        }
    
    /** The object which actually does all the drawing.  Perhaps we should move this out. */
    public class InnerDisplay2D extends JComponent
        {
        /** Image buffer for doing buffered draws, mostly for screenshots etc. */
        BufferedImage buffer = null;

        /** The width of the display when the scale is 1.0 */
        public double width;
        /** The height of the display when the scale is 1.0 */
        public double height;
        /** x offset */
        public double xOffset;
        /** y offset */
        public double yOffset;
                        
        /** Creates an InnerDisplay2D with the provided width and height. */
        public InnerDisplay2D(double width, double height)
            {
            this.width = width;
            this.height = height;
            setupHints(false,false,false);  // go for speed
            }
        
        /** Overloaded to return (width * scale, height * scale) */
        public Dimension getPreferredSize() 
            { return new Dimension((int)(width*getScale()),(int)(height*getScale())); }
            
        /** Overloaded to return (width * scale, height * scale) */
        public Dimension getMinimumSize() 
            { return getPreferredSize();  }

        /** Overloaded to return (width * scale, height * scale) */
        public Dimension getMaximumsize()
            { return getPreferredSize(); }
        
        /** Paints a movie, by drawing to a buffer, then
            encoding the buffer to disk, then optionally 
            writing the buffer to the provided Graphics2D. 
            If the Graphics2D is null, it's just written out to disk.
            This method will only write to disk when "appropriate", that is,
            if the current schedule has advanced to the point that a new
            frame is supposed to be outputted (given the frame rate of the
            movie).  In any rate, it'll write to the Graphic2D if
            provided. */
        public void paintToMovie(Graphics g)
            {
            // although presently paintToMovie is called solely from paintComponent,
            // which already has synchronized on the schedule, we do this anyway for
            // good measure.  See stopMovie() and startMovie() for why synchronization
            // is important.
            synchronized(Display2D.this.simulation.state.schedule)
                {
                // only paint if it's appropriate
                long steps = Display2D.this.simulation.state.schedule.getSteps();
                if (steps > lastEncodedSteps &&
                    steps % getInterval() == 0 &&
                    Display2D.this.simulation.state.schedule.time() < Schedule.AFTER_SIMULATION)
                    {
                    Display2D.this.movieMaker.add(paint(g,true,false));
                    lastEncodedSteps = steps;
                    }
                else paint(g,false,false);
                }
            }
        
        /** Hints used to draw objects to the screen or to a buffer */
        public RenderingHints unbufferedHints;
        /** Hints used to draw the buffered image to the screen */
        public RenderingHints bufferedHints;
        
        /** The default method for setting up the given hints.
            By default they suggest that Java2D emphasize efficiency over prettiness.*/
        public void setupHints(boolean antialias, boolean niceAlphaInterpolation, boolean niceInterpolation)
            {
            unbufferedHints = new RenderingHints(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_SPEED);  // in general
            unbufferedHints.put(RenderingHints.KEY_INTERPOLATION,
                niceInterpolation ? RenderingHints.VALUE_INTERPOLATION_BILINEAR :
                RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
            // dunno what to do here about antialiasing on MacOS X
            // -- if it's on, then circles can get drawn as squares (see woims demo at 1.5 scale)
            // -- but if it's off, then stuff gets antialiased in pictures but not on a screenshot.
            // My inclination is to leave it off. 
            unbufferedHints.put(RenderingHints.KEY_ANTIALIASING, 
                antialias ? RenderingHints.VALUE_ANTIALIAS_ON :
                RenderingHints.VALUE_ANTIALIAS_OFF);
            unbufferedHints.put(RenderingHints.KEY_TEXT_ANTIALIASING,
                antialias ? RenderingHints.VALUE_TEXT_ANTIALIAS_ON :
                RenderingHints.VALUE_TEXT_ANTIALIAS_OFF);
            unbufferedHints.put(RenderingHints.KEY_ALPHA_INTERPOLATION, 
                niceAlphaInterpolation ? 
                RenderingHints.VALUE_ALPHA_INTERPOLATION_QUALITY :
                RenderingHints.VALUE_ALPHA_INTERPOLATION_SPEED);

            bufferedHints = new RenderingHints(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_SPEED);  // in general
            bufferedHints.put(RenderingHints.KEY_INTERPOLATION,
                niceInterpolation ? RenderingHints.VALUE_INTERPOLATION_BILINEAR :
                RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
            // similarly
            bufferedHints.put(RenderingHints.KEY_ANTIALIASING, 
                antialias ? RenderingHints.VALUE_ANTIALIAS_ON :
                RenderingHints.VALUE_ANTIALIAS_OFF);
            bufferedHints.put(RenderingHints.KEY_TEXT_ANTIALIASING,
                antialias ? RenderingHints.VALUE_TEXT_ANTIALIAS_ON :
                RenderingHints.VALUE_TEXT_ANTIALIAS_OFF);
            bufferedHints.put(RenderingHints.KEY_ALPHA_INTERPOLATION, 
                niceAlphaInterpolation ? 
                RenderingHints.VALUE_ALPHA_INTERPOLATION_QUALITY :
                RenderingHints.VALUE_ALPHA_INTERPOLATION_SPEED);
            }
        
        java.lang.ref.WeakReference toolTip = new java.lang.ref.WeakReference(null);
        public JToolTip createToolTip()
            {
            JToolTip tip = super.createToolTip();
            toolTip = new java.lang.ref.WeakReference(tip);
            return tip;
            }
                
        protected MouseEvent lastToolTipEvent = null;
        // could be called from the model thread OR the swing thread -- must be careful
        public String getToolTipText(MouseEvent event)
            {
            if (useTooltips)
                {
                lastToolTipEvent = event;
                final Point point = event.getPoint();
                return createToolTipText( new Rectangle2D.Double(point.x,point.y,1,1), Display2D.this.simulation);
                }
            else return null;
            }
        
        String lastToolTipText = null;
        // this will be called from the model thread most likely, so we need
        // to make sure that we actually do the updating in an invokeLater...
        public void updateToolTips()
            {
            if (useTooltips && lastToolTipEvent != null)
                {
                final String s = getToolTipText(lastToolTipEvent);
                if (s==null || !s.equals(lastToolTipText))
                    {
                    // ah, we need to update.
                    SwingUtilities.invokeLater(new Runnable() 
                        {
                        public void run() 
                            {
                            setToolTipText(lastToolTipText = s);
                            JToolTip tip = (JToolTip)(toolTip.get());
                            if (tip!=null && tip.getComponent()==InnerDisplay2D.this) tip.setTipText(s);
                            }
                        });
                    }
                }
            }
        
        boolean paintLock = false;
        /** Swing's equivalent of paint(Graphics g).   Called by repaint().  In turn calls
            paintComponent(g,false);   You should not call this method directly.  Instead you probably want to
            call paintComponent(Graphics, buffer).  */
        public synchronized void paintComponent(final Graphics g)
            {
            // Swing gets overreaching when scaling in.  This allows us to temporarily refuse to paint so we don't do so multiple times.
            if (paintLock) return; 
            // I'm likely being updated due to scrolling, so change rect
            if (SwingUtilities.isEventDispatchThread())
                setViewRect(port.getViewRect());
            paintComponent(g,false);
            }
        
        /** The top-level repainting method.  If we're writing to a movie, we do a paintToMovie
            (which also does a buffered paint to the screen) else we do an ordinary paint.
            <tt>buffer</tt> determines if we do our ordinary paints buffered or not. */
        public void paintComponent(Graphics g, boolean buffer)
            {
            synchronized(Display2D.this.simulation.state.schedule)  // for time()
                {
                if (movieMaker!=null)  // we're writing a movie
                    insideDisplay.paintToMovie(g);
                else paint(g,buffer,true);
                }
            }
            
            
        /** Computes the expected clip for drawing. */
        Rectangle2D computeClip()
            {
            // This line is in case we're drawing from something other than the main
            // event loop.  In this situation, we need to get the view rect and figure
            // our clip region accordingly.  But port.getViewRect() is a problem because it's
            // calling magic synchronized code inside it, and so can deadlock if Swing is
            // locked (which it likely is if it's ALSO calling paintComponent and we beat
            // it to it).  So we maintain the last rect here using our own magic getViewRect
            // function (see explanations below)
            Rectangle2D clip = getViewRect();
            
            // centering
            double scale = getScale();
            int origindx = 0;
            int origindy = 0;
            if (clip.getWidth() > (width * scale))
                origindx = (int)((clip.getWidth() - width*scale)/2);
            if (clip.getHeight() > (height*scale))
                origindy = (int)((clip.getHeight() - height*scale)/2);
                
            if (isClipping())               
                {
                Dimension s = getPreferredSize();
                clip = clip.createIntersection(new Rectangle2D.Double(origindx,origindy,s.width,s.height));
                }
            return clip;
            }


        /** Paints an image to the screen either buffered or unbuffered.  If buffered, the
            buffer is returned for your convenience.   If SHARED is true, then the buffer
            is shared internally, so if
            you want to hold onto it without it changing, you'll need to make a copy.  If SHARED
            is false, then the buffer is given to you to do as you like and a new buffer is
            created next time (less efficient).
            This method is called from Swing
            when the window is resized or scrolled or whatnot, but it's also called as
            a result of the underlying model thread advancing a step so we can see
            what happened.  In the second case, the method could conceivably be
            called both from the Swing event thread (if making a movie, or if in MacOS X)
            or from the underlying model thread (if in Windows or X Windows -- to change
            this, see Display2D.step).  This is important because this if we're being called
            from the Swing event thread (repaint/movie/MacOS X), we must ensure that we're
            not updating the model at the same time the model is changing, and to do that
            we lock on the simulation's schedule so we know the model is done with step()
            methods and nothing's moving.  But because we're blocking on the schedule,
            a deadlock opportunity arises.  For example, what if some foolish person writes
            a step() method in the model which calls some blocking item in the Swing event
            thread?  Then they're blocking waiting for the Swing event thread to come
            available again, but the event thread is blocking because we're waiting for them
            to get out of the model.  A similar problem could arise if in implementing a
            portrayal's draw method the user decides to call some blocking item in Swing,
            because this paint() method could conceivably be called from the model itself
            (in Windows/XWindows), and it's possible that at the very same time the user
            has pressed the stop button or done some operation in Swing which is blocking
            waiting for the schedule.  Long story short, try not to call a blocking method
            on Swing from inside this method. */
        public BufferedImage paint(final Graphics graphics, boolean buffered, boolean shared)
            {
            synchronized(Display2D.this.simulation.state.schedule)
                {
                BufferedImage result = null;
                Rectangle2D clip = computeClip();
                if (!buffered)
                    paintUnbuffered((Graphics2D)graphics,clip);
                else
                    result= paintBuffered((Graphics2D)graphics,clip);
                if (!shared) buffer = null; // kill it so paintBuffered(graphics,clip) makes a new one next time
                if (result != null) result.flush();  // just in case
                return result;
                }
            }
                 
        /** Draws the image into a buffer, then IF graphics is not null,
            draws the resulting buffer to the graphics.  Returns the buffer,
            which is shared internally, so you need to copy out of it as soon as possible. */
        BufferedImage paintBuffered(final Graphics2D graphics, final Rectangle2D clip)
            {
            // make buffer big enough
            double ww = clip.getWidth();
            double hh = clip.getHeight();
            if (buffer==null || (buffer.getWidth(null)) != ww || (buffer.getHeight(null)) != hh)
                // note < would be more efficient than != but
                // it would create incorrect-sized images for snapshots,
                // and it's more memory wasteful anyway
                {
                buffer = getGraphicsConfiguration().createCompatibleImage((int)ww,(int)hh);
                }
            
            // draw into the buffer
            Graphics2D g = (Graphics2D)(buffer.getGraphics());
            g.setColor(port.getBackground());
            g.fillRect(0,0,buffer.getWidth(null),buffer.getHeight(null));
            g.translate(-(int)clip.getX(),-(int)clip.getY());
            paintUnbuffered(g,clip);
            g.dispose();  // because we got it with getGraphics(), we're responsible for it
            
            // paint and return the buffer
            if (graphics!=null)
                {
                graphics.setRenderingHints(bufferedHints);
                graphics.drawImage(buffer,(int)(clip.getX()),(int)(clip.getY()),null);
                }
            return buffer;
            }
        
        /** Paints an image unbuffered inside the provided clip. Not synchronized.
            You should probably call paintComponent() instead. */
        void paintUnbuffered(Graphics2D g, Rectangle2D clip)
            {
            if (g==null) return;
            
            g.setRenderingHints(unbufferedHints);

            // dunno if we want this
            if (isClipping()) g.setClip(clip);
            if (clip.getWidth()!=0 && clip.getHeight()!=0)
                {
                // presently not scaled
                if (backdrop!=null)
                    {
                    g.setPaint(backdrop);
                    g.fillRect((int)clip.getX(),(int)clip.getY(),(int)clip.getWidth(),(int)clip.getHeight());
                    }
                
                /*
                // get scale
                final double scale = getScale();
                // compute WHERE we need to draw
                int origindx = 0;
                int origindy = 0;

                // for information on why we use getViewRect, see computeClip()
                Rectangle2D fullComponent = getViewRect();
                if (fullComponent.getWidth() > (width * scale))
                origindx = (int)((fullComponent.getWidth() - width*scale)/2);
                if (fullComponent.getHeight() > (height*scale))
                origindy = (int)((fullComponent.getHeight() - height*scale)/2);
                    
                // offset origin as user had requested
                origindx += (int)(xOffset*scale);
                origindy += (int)(yOffset*scale);
                */
                
                Iterator iter = portrayals.iterator();
                while (iter.hasNext())
                    {
                    FieldPortrayal2DHolder p = (FieldPortrayal2DHolder)(iter.next());
                    if (p.visible)
                        {
/*
  Rectangle2D rdraw = new Rectangle2D.Double(
  // we floor to an integer because we're dealing with exact pixels at this point
  (int)(p.bounds.x * scale) + origindx,
  (int)(p.bounds.y * scale) + origindy,
  (int)(p.bounds.width * scale),
  (int)(p.bounds.height * scale));
*/

                        // set buffering if necessary
                        int buf = p.portrayal.getBuffering();
                        p.portrayal.setBuffering(optionPane.buffering);
                        
                        // MacOS X 10.3 Panther has a bug which resets the clip, YUCK
                        g.setClip(g.getClip());
                        
                        // do the drawing
/*
  p.portrayal.draw(p.portrayal.getField(), // I could have passed null in here too
  g, new DrawInfo2D(rdraw,clip));
*/
                        p.portrayal.draw(p.portrayal.getField(), // I could have passed null in here too
                            g, getDrawInfo2D(p, clip));
                        
                        // reset the buffering if necessary
                        p.portrayal.setBuffering(buf);
                        }
                    }
                }
            }

        /*  TO FIX A SUBTLE BUG.  Can't call getViewRect() to get the proper
            clipping rect, because getViewRect calls some unknown synchronized gunk
            further up in Swing; thus if I'm in Windoze and splat to the screen from my own
            thread, and at the same time the Swing thread is trying to draw me, we
            have a problem -- it grabs the unknown gunk lock, I synchronize on this,
            then I try to call getViewRect on the port and lock, and we're both hung.
            if we do redrawing via repaint() like in MacOS X, then we're fine, but if
            we do it a-la X or Windows, this bug rears its ugly head.  So we get the
            most recent viewRect and set it here and keep it for when the redraw needs
            it, so it doesn't have to ask for it from Swing (which could be locked). */        

        Rectangle viewRect = new Rectangle(0,0,0,0);  // no access except via synchronization
        
        /** Lock for the viewRect above.  Don't want to lock on the Display2D itself. */
        final Object viewRectLock = new Object();
        
        /** Gets the last viewRect */
        Rectangle getViewRect()
            {
            synchronized(viewRectLock)
                {
                return new Rectangle(viewRect);
                }
            }
        
        /** Sets the viewRect to a new value */
        void setViewRect(Rectangle rect)
            {
            synchronized(viewRectLock)
                {
                viewRect = new Rectangle(rect);
                }
            }
        }    
        
    /** Holds all the relevant information for a given FieldPortrayal. */
    class FieldPortrayal2DHolder
        {
        /** The translation and scale of the FieldPortrayal.  Presently this
            is always 0,0 translation and 1.0 scale, but we'll allow the
            user to change this soon. */
        public Rectangle2D.Double bounds;
        /** The portrayal proper */
        public FieldPortrayal2D portrayal;
        /** The name of the portrayal, as shown in the Layers menu on the Display2D window */
        public String name;
        /** The menu item of the portrayal, in the Layers menu. */
        public JCheckBoxMenuItem menuItem;
        /** Whether we should draw the portrayal on updates */
        public boolean visible;
        /** Returns the portrayal's name in the Layers menu */
        public String toString() { return name; }
        /** Creates a menu item which selects or unselects the portrayal for drawing. */
        public FieldPortrayal2DHolder(FieldPortrayal2D p, String n, Rectangle2D.Double bounds, boolean visible)
            {
            this.bounds = bounds;
            portrayal=p; 
            name=n;
            this.visible = visible;
            menuItem = new JCheckBoxMenuItem(name,visible);
            menuItem.addActionListener(new ActionListener()
                {
                public void actionPerformed(ActionEvent e)
                    {
                    FieldPortrayal2DHolder.this.visible = menuItem.isSelected(); 
                    repaint();
                    }
                });
            }
        }


    // Windows draws faster by handling our own double buffering into our own Java-created
    // buffered image.  MacOS X draws faster by doing a repaint and letting the OS handle
    // the buffering.  Dunno about X Windows, haven't checked, I presume it's same situation
    // as Windows as MacOS X is doing lots of odd hard-coding underneath.

    /** Set to true if we're running on a Mac */
    public static final boolean isMacOSX = isMacOSX();
                                    
    /** Set to true if we're running on Windows */
    public static final boolean isWindows = isWindows();

    /** Set to the version number */
    public static final String javaVersion = getVersion();

    static boolean isMacOSX() 
        {
        try  // we'll try to get certain properties if the security permits it
            {
            return (System.getProperty("mrj.version") != null);  // Apple's official approach
            }
        catch (Throwable e) { return false; }  // Non-Mac Web browsers will fail here
        }

    static boolean isWindows() 
        {
        try  // we'll try to get certain properties if the security permits it
            {
            return !isMacOSX() && (System.getProperty("os.name").startsWith("Win"));
            }
        catch (Throwable e) { return false; }
        }

    static String getVersion()
        {
        try
            {
            return System.getProperty("java.version");
            }
        catch (Throwable e) { return "unknown"; }
        }


    /** Sets various MacOS X features */
    static 
        {
        // use heavyweight tooltips -- otherwise they get obscured by the Canvas3D
        // [this appears to be ignored by MacOS X Java 1.4.1 and 1.4.2.  A bug? ]
        ToolTipManager.sharedInstance().setLightWeightPopupEnabled(false);

        // Use Quaqua if it exists
        try
            {
            System.setProperty( "Quaqua.TabbedPane.design","auto" );  // UI Manager Properties docs differ
            System.setProperty( "Quaqua.visualMargin","1,1,1,1" );
            UIManager.put("Panel.opaque", Boolean.TRUE);
            UIManager.setLookAndFeel((String)(Class.forName("ch.randelshofer.quaqua.QuaquaManager").
                    getMethod("getLookAndFeelClassName",(Class[])null).invoke(null,(Object[])null)));
            } 
        catch (Exception e) { /* e.printStackTrace(); */ }

        try  // now we try to set certain properties if the security permits it
            {
            // turn on hardware acceleration on MacOS X.  As of September 2003, 1.3.1
            // turns this off by default, which makes 1.3.1 half the speed (and draws
            // objects wrong to boot).
            System.setProperty("com.apple.hwaccel","true");  // probably settable as an applet.  D'oh! Looks like it's ignored.
            System.setProperty("apple.awt.graphics.UseQuartz","true");  // counter the awful effect in OS X's Sun Renderer
            // the following are likely not settable
            // macOS X 1.4.1 java doesn't show the grow box.  We force it here.
            System.setProperty("apple.awt.showGrowBox","true");
            // we set this so that macos x application packages appear as files
            // and not as directories in the file viewer.  Note that this is the 
            // 1.3.1 version -- Apple gives us an obnoxious warning in 1.4.1 when
            // we call forth an open/save panel saying we should now use
            // apple.awt.use-file-dialog-packages instead, as if 1.3.1 isn't also
            // in common use...
            System.setProperty("com.apple.macos.use-file-dialog-packages","true");
            }
        catch (Exception e) { }
        }
    
    /** Returns icons for a given filename, such as "Layers.png". A utility function. */
    static ImageIcon iconFor(String name)
        {
        return new ImageIcon(Display2D.class.getResource(name));
        }
    
    public static final ImageIcon LAYERS_ICON = iconFor("Layers.png");
    public static final ImageIcon LAYERS_ICON_P = iconFor("LayersPressed.png");
    public static final ImageIcon MOVIE_ON_ICON = iconFor("MovieOn.png");
    public static final ImageIcon MOVIE_ON_ICON_P = iconFor("MovieOnPressed.png");
    public static final ImageIcon MOVIE_OFF_ICON = iconFor("MovieOff.png");
    public static final ImageIcon MOVIE_OFF_ICON_P = iconFor("MovieOffPressed.png");
    public static final ImageIcon CAMERA_ICON = iconFor("Camera.png");
    public static final ImageIcon CAMERA_ICON_P = iconFor("CameraPressed.png");
    public static final ImageIcon OPTIONS_ICON = iconFor("Options.png");
    public static final ImageIcon OPTIONS_ICON_P = iconFor("OptionsPressed.png");
    
    /** Use tool tips? */
    public boolean useTooltips;

    /** The last steps for a frame that was painted to the screen.  Keeping this
        variable around enables our movie maker to ensure that it doesn't write
        a frame twice to its movie stream. */
    long lastEncodedSteps = -1;  // because we want to encode the start of the simulation prior to any steps.  That's step 0.
    /** Our movie maker, if one is running, else null. */
    public MovieMaker movieMaker;

    /** The 2D display inside the scroll view.  Does the actual drawing of the simulation. */
    public InnerDisplay2D insideDisplay;

    /** Our option pane */
    public OptionPane optionPane = new OptionPane("");
    
    /** The list of portrayals the insideDisplay draws.  Each element in this list is a Portrayal2DHolder. */
    ArrayList portrayals = new ArrayList();
    /** The scroll view which holds the insideDisplay. */
    JScrollPane display;
    /** The scroll view's viewport. */
    JViewport port;
    /** The stoppable for the repeat object which redraws the Display2D in the schedule. */
    Stoppable stopper;
    /** The simulation proper. */
    GUIState simulation;
    /** The component bar at the top of the Display2D. */
    public Box header;
    /** The popup layers menu */
    public JPopupMenu popup;
    /** The button which pops up the layers menu */
    public JToggleButton togglebutton;  // for popup
    /** The button which starts or stops a movie */
    public JButton movieButton;
    /** The button which snaps a screenshot */
    public JButton snapshotButton;
    /** The button which pops up the option pane */
    public JButton optionButton;
    /** The field for scaling values */
    public NumberTextField scaleField;
    /** The field for skipping frames */
    public NumberTextField skipField;
        
    /** Scale (zoom value).  1.0 is 1:1.  2.0 is zoomed in 2 times.  Etc. */
    double scale = 1.0;
    final Object scaleLock = new Object();  // scale lock
    /** Sets the scale (the zoom value) of the Display2D */
    public void setScale(double val) { synchronized (scaleLock)  { if (val > 0.0) scale = val; } }
    /** Returns the scale (the zoom value) of the Display2D */
    public double getScale() { synchronized (scaleLock) { return scale; } }

    /** How many steps are skipped before the display updates itself.  */
    long interval = 1;
    Object intervalLock = new Object();  // interval lock
    /** Sets how many steps are skipped before the display updates itself. */
    public void setInterval(long i) { synchronized(intervalLock) { if (i > 0) interval = i; } }
    /** Gets how many steps are skipped before the display updates itself. */
    public long getInterval() { synchronized(intervalLock) { return interval; } }
    
    /** Whether or not we're clipping */
    boolean clipping = true;
    /** Returns true if the Display2D is clipping the drawing area to the user-specified
        height and width */
    public boolean isClipping() { return clipping; }
    /** Sets the Display2D to clip or to not clip to the user-specified height and width when drawing */
    public void setClipping(boolean val) { clipping = val; }
        
    /** Backdrop color or other paint.  This is the color/paint that the simulation is whitewashed with prior to
        the portrayals redrawing themselves.  This differs from the scroll view's BACKGROUND
        color, which is the color of any area that the simulation doesn't draw on. */
    Paint backdrop = Color.white;  // default.  It'll get changed.
    /** Specify the backdrop color or other paint.  The backdrop is the region behind where the simulation 
        actually draws.  If set to null, no color/paint is used -- and indeed the background you're drawing on
        is not defined.  Only set to null if you know you're filling the entire background with something else
        anyway. */
    public void setBackdrop(Paint c) { backdrop = c; }
    /** Returns the backdrop color or paint.  The backdrop is the region behind where the simulation actually draws.
        If set to null, no color/paint is used. */
    public Paint getBackdrop() { return backdrop; }
        
    /** Quits the Display2D.  Okay, so finalize is evil and we're not supposed to rely on it.
        We're not.  But it's an additional cargo-cult programming measure just in case. */
    protected void finalize() throws Throwable
        {
        super.finalize();
        quit();
        }
    
    /** Quits the Display2D.  Called by the Display2D's frame if the Display2D made the frame itself.
        Also called by finalize().  Otherwise you should call this method before destroying the Display2D. */
    public void quit()
        {
        if (stopper!=null) stopper.stop();
        stopper = null;
        stopMovie();
        }
        
    /** Resets the Display2D so it reschedules itself and clears out all selections.  This is useful when reusing the Display2D. */
    public void reset()
        {
        // now reschedule myself
        if (stopper!=null) stopper.stop();
        stopper = simulation.scheduleImmediateRepeat(true,this);
                
        // deselect existing objects
        for(int x=0;x<selectedWrappers.size();x++)
            {
            LocationWrapper wrapper = ((LocationWrapper)(selectedWrappers.get(x)));
            wrapper.getFieldPortrayal().setSelected(wrapper,false);
            }
        selectedWrappers.clear();
        }
    
    /** Attaches a portrayal to the Display2D, along with the provided human-readable name for the portrayal.
        The portrayal will be attached with an origin at (0,0) and a width and height equal to the Display2D's
        default width and height. 
        Portrayals are drawn on-screen in the order that they are attached; thus the "top-most" portrayal
        will be the last one attached. */
    public void attach(FieldPortrayal2D portrayal, String name )
        {
        attach(portrayal, name, true);
        }

    /** Attaches a portrayal to the Display2D, along with the provided human-readable name for the portrayal.
        The portrayal's attached origin, width and height is given in the bounds rectangle. 
        Portrayals are drawn on-screen in the order that they are attached; thus the "top-most" portrayal
        will be the last one attached.*/
    public void attach(FieldPortrayal2D portrayal, String name, Rectangle2D.Double bounds )
        {
        attach(portrayal, name, bounds, true);
        }
        
    /** Attaches a portrayal to the Display2D, along with the provided
        human-readable name for the portrayal.  The portrayal will be attached
        with an origin at (0,0) and a width and height equal to the Display2D's
        default width and height.  The portrayal may be set to initially 
        visible or not visible.  Portrayals are drawn
        on-screen in the order that they are attached; thus the "top-most" portrayal
        will be the last one attached.*/
    public void attach(FieldPortrayal2D portrayal, String name, boolean visible )
        {
        attach(portrayal, name, 0, 0, visible);
        }

    /** Attaches a portrayal to the Display2D, along with the provided human-readable name for the portrayal.
        The portrayal's attached origin is given with the coordinates provided.
        The width and height will be equal to the Display2D's default width and height (display2D.insideDisplay.width and
        display2D.insideDisplay.height respectively). To put the origin at the
        center of the display, you can set the x and y coordinates to display2D.insideDisplay.width/2, display2D.insideDisplay.height/2).
        The portrayal may be set to initially visible or not visible.  Portrayals are drawn 
        on-screen in the order that they are attached; thus the "top-most" portrayal
        will be the last one attached. 
    */
    public void attach(FieldPortrayal2D portrayal, String name, double x, double y, boolean visible)
        {
        attach(portrayal, name, new Rectangle2D.Double(x,y,insideDisplay.width, insideDisplay.height), visible);
        }


    /** Attaches a portrayal to the Display2D, along with the provided 
        human-readable name for the portrayal.  The portrayal's attached 
        origin, width and height is given in the bounds rectangle.  The portrayal
        may be set to initially visible or not visible.  Portrayals are drawn 
        on-screen in the order that they are attached; thus the "top-most" portrayal
        will be the last one attached.  */
    public void attach(FieldPortrayal2D portrayal, String name, 
        Rectangle2D.Double bounds, boolean visible )
        {
        FieldPortrayal2DHolder p = new FieldPortrayal2DHolder(portrayal,name,bounds,visible);
        portrayals.add(p);
        popup.add(p.menuItem);
        }
                
    /** A convenience function: creates a popup menu item of the given name which, when selected, will display the
        given inspector in the Console.  Used rarely, typically for per-field Inspectors. */
    public void attach(final sim.portrayal.Inspector inspector, final String name)
        {
        JMenuItem consoleMenu = new JMenuItem("Show " + name);
        popup.add(consoleMenu);
        consoleMenu.addActionListener(new ActionListener()
            {
            public void actionPerformed(ActionEvent e)
                {
                Bag inspectors = new Bag();
                inspectors.add(inspector);
                Bag names = new Bag();
                names.add(name);
                simulation.controller.setInspectors(inspectors,names);
                }
            });
        }
    
    public void createConsoleMenu()
        {
        if (simulation != null && simulation.controller != null &&
            simulation.controller instanceof Console)
            {
            final Console c = (Console)(simulation.controller);
            JMenuItem consoleMenu = new JMenuItem("Show Console");
            popup.add(consoleMenu);
            consoleMenu.addActionListener(new ActionListener()
                {
                public void actionPerformed(ActionEvent e)
                    {
                    c.setVisible(true);;
                    }
                });
            }
        popup.addSeparator();
        }
        
    /** Detatches all portrayals from the Display2D. */
    public ArrayList detatchAll()
        {
        ArrayList old = portrayals;
        popup.removeAll();
        createConsoleMenu();
        portrayals = new ArrayList();
        return old;
        }
        
    /** Creates a Display2D with the provided width and height for its portrayal region, 
        attached to the provided simulation, and displaying itself with the given interval (which must be > 0). */
    public Display2D(final double width, final double height, GUIState simulation, long interval)
        {
        setInterval(interval);
        this.simulation = simulation;
        
        reset();  // must happen AFTER simulation and interval are assigned
        
        final Color transparentBackground = new JPanel().getBackground();  // sacrificial JPanel

        // create the inner display and put it in a Scroll Panel
        insideDisplay = new InnerDisplay2D(width,height);
        display = new JScrollPane(insideDisplay,
            JScrollPane.VERTICAL_SCROLLBAR_ALWAYS,
            JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS);
        display.setMinimumSize(new Dimension(0,0));
        display.setBorder(null);
        display.getHorizontalScrollBar().setBorder(null);
        display.getVerticalScrollBar().setBorder(null);
        port = display.getViewport();
        insideDisplay.setViewRect(port.getViewRect());
        insideDisplay.setOpaque(true);  // radically increases speed in OS X, maybe others
        // Bug in Panther causes this color to be wrong, ARGH
//        port.setBackground(UIManager.getColor("window"));  // make the nice stripes on MacOS X
        insideDisplay.setBackground(UIManager.getColor("Panel.background"));
        display.setBackground(UIManager.getColor("Panel.background")); // this is the one that has any affect
        port.setBackground(UIManager.getColor("Panel.background"));
        
        // create the button bar at the top.
        header = new Box(BoxLayout.X_AXIS);

        //Create the popup menu.
        togglebutton = new JToggleButton(LAYERS_ICON);
        togglebutton.setPressedIcon(LAYERS_ICON_P);
        togglebutton.setBorder(BorderFactory.createEmptyBorder(4,4,4,4));
        togglebutton.setBorderPainted(false);
        togglebutton.setContentAreaFilled(false);
        togglebutton.setToolTipText("Show and hide different layers");
        
        header.add(togglebutton);
        popup = new JPopupMenu();
        popup.setLightWeightPopupEnabled(false);

        //Add listener to components that can bring up popup menus.
        togglebutton.addMouseListener(new MouseAdapter()
            {
            public void mousePressed(MouseEvent e)
                {
                popup.show(e.getComponent(),
                    togglebutton.getLocation().x,
                    togglebutton.getSize().height);
                }
            public void mouseReleased(MouseEvent e)
                {
                togglebutton.setSelected(false);
                }
            });

        // add mouse listener for the inspectors
        insideDisplay.addMouseListener(new MouseAdapter()
            {
            public void mouseClicked(MouseEvent e) 
                {
                final Point point = e.getPoint();
                if( e.getClickCount() == 2 )
                    createInspectors( new Rectangle2D.Double( point.x, point.y, 1, 1 ),
                        Display2D.this.simulation );
                if (e.getClickCount() == 1 || e.getClickCount() == 2)  // in both situations
                    performSelection( new Rectangle2D.Double( point.x, point.y, 1, 1 ));
                repaint();
                }
            
            // clear tool-tip updates
            public void mouseExited(MouseEvent event)
                {
                insideDisplay.lastToolTipEvent = null;
                }
            });
            
        insideDisplay.setToolTipText("Display");  // sacrificial
                
        // add the movie button
        movieButton = new JButton(MOVIE_OFF_ICON);
        movieButton.setPressedIcon(MOVIE_OFF_ICON_P);
        movieButton.setBorder(BorderFactory.createEmptyBorder(4,4,4,4));
        movieButton.setBorderPainted(false);
        movieButton.setContentAreaFilled(false);
        movieButton.setToolTipText("Create a Quicktime movie");
        movieButton.addActionListener(new ActionListener()
            {
            public void actionPerformed(ActionEvent e)
                {
                if (movieMaker==null)
                    {
                    startMovie();
                    }
                else 
                    {
                    stopMovie();
                    }
                }
            });
        header.add(movieButton);

        // add the snapshot button
        snapshotButton = new JButton(CAMERA_ICON);
        snapshotButton.setPressedIcon(CAMERA_ICON_P);
        snapshotButton.setBorder(BorderFactory.createEmptyBorder(4,4,4,4));
        snapshotButton.setBorderPainted(false);
        snapshotButton.setContentAreaFilled(false);
        snapshotButton.setToolTipText("Create a snapshot (as a PNG or PDF file)");
        snapshotButton.addActionListener(new ActionListener()
            {
            public void actionPerformed(ActionEvent e)
                {
                takeSnapshot();
                }
            });
        header.add(snapshotButton);
        
        // add the option button
        optionButton = new JButton(OPTIONS_ICON);
        optionButton.setPressedIcon(OPTIONS_ICON_P);
        optionButton.setBorder(BorderFactory.createEmptyBorder(4,4,4,4));               
        optionButton.setBorderPainted(false);
        optionButton.setContentAreaFilled(false);
        optionButton.setToolTipText("Show the Option Pane");
        optionButton.addActionListener(new ActionListener()
            {
            public void actionPerformed(ActionEvent e)
                {
                optionPane.setTitle(getFrame().getTitle() + " Options");
                optionPane.pack();
                optionPane.setVisible(true);;
                }
            });
        header.add(optionButton);
        
        // add the scale field
        scaleField = new NumberTextField("  Scale: ", 1.0, true)
            {
            public double newValue(double newValue)
                {
                if (newValue <= 0.0) newValue = currentValue;

                // lock the paint lock so we don't try repainting until we request it.
                // JScrollView tries to jump the gun, making things flashy.
                insideDisplay.paintLock = true;
                
                // grab the original location
                Rectangle r = port.getViewRect();

                // scroll to keep the zoomed-in region centered -- this is prettier
                double centerx = r.x + r.width/2.0;
                double centery = r.y + r.height/2.0;
                centerx *= (newValue / (double) currentValue);
                centery *= (newValue / (double) currentValue);
                Point topleft = new Point((int)(centerx - r.width/2.0), (int)(centery - r.height/2.0));
                if (topleft.x < 0) topleft.x = 0;
                if (topleft.y < 0) topleft.y = 0;

                setScale(newValue);
                optionPane.xOffsetField.setValue(insideDisplay.xOffset * newValue);
                optionPane.yOffsetField.setValue(insideDisplay.yOffset * newValue);
                port.setView(insideDisplay);
                
                // now release the paint lock and repaint
                insideDisplay.paintLock = false;
                
                port.setViewPosition(topleft);
                Display2D.this.repaint();
                return newValue;
                }
            };
        scaleField.setToolTipText("Zoom in and out");
        header.add(scaleField);
        
        // add the interval (skip) field
        skipField = new NumberTextField("  Skip: ", 1, false)
            {
            public double newValue(double newValue)
                {
                int val = (int) newValue;
                if (val < 1) val = (int)currentValue;
                        
                // reset with a new interval
                setInterval(val);
                reset();
                        
                return val;
                }
            };
        skipField.setToolTipText("Specify the number of steps between screen updates");
        header.add(skipField);

        // put everything together
        setLayout(new BorderLayout());
        add(header,BorderLayout.NORTH);  // so it gets repainted first hopefully
        add(display,BorderLayout.CENTER);

        createConsoleMenu();
        }

    /** Returns LocationWrappers for all the objects which fall within the coordinate rectangle specified by rect.  This 
        rectangle is in the coordinate system of the (InnerDisplay2D) component inside the scroll
        view of the Display2D class.  The return value is an array of Bags.  For each FieldPortrayal
        attached to the Display2D, one Bag is returned holding all the LocationWrappers for objects falling within the
        rectangle which are associated with that FieldPortrayal's portrayed field.  The order of
        the Bags in the array is the same as the order of the FieldPortrayals in the Display2D's
        <code>portrayals</code> list.
    */
    public Bag[] objectsHitBy( final Rectangle2D.Double rect )
        {
        Bag[] hitObjs = new Bag[portrayals.size()];
        Iterator iter = portrayals.iterator();
        int x=0;
                
        while (iter.hasNext())
            {
            hitObjs[x] = new Bag();
            FieldPortrayal2DHolder p = (FieldPortrayal2DHolder)(iter.next());
            if (p.visible)
                {
                p.portrayal.hitObjects(getDrawInfo2D(p, rect), hitObjs[x]);
                }
            x++;
            }
        return hitObjs;
        }
        
    /** Constructs a DrawInfo2D for the given portrayal, or null if failed.  O(num portrayals). */
    public DrawInfo2D getDrawInfo2D(FieldPortrayal2D portrayal, Point2D point)
        {
        return getDrawInfo2D(portrayal, new java.awt.geom.Rectangle2D.Double( point.getX(), point.getY(), 1, 1 )); 
        }

    /** Constructs a DrawInfo2D for the given portrayal, or null if failed.  O(num portrayals). */
    public DrawInfo2D getDrawInfo2D(FieldPortrayal2D portrayal, Rectangle2D clip)
        {
        Iterator iter = portrayals.iterator();
        while(iter.hasNext())
            {
            FieldPortrayal2DHolder p = (FieldPortrayal2DHolder)(iter.next());
            if (p.portrayal == portrayal) { return getDrawInfo2D(p, clip); }
            }
        return null;
        }
        
        
    DrawInfo2D getDrawInfo2D(FieldPortrayal2DHolder holder, Rectangle2D clip)
        {
        if (holder==null) return null;
        
        double scale = getScale();
        // compute WHERE we need to draw
        int origindx = 0;
        int origindy = 0;

        // offset according to user's specification
        origindx += (int)(insideDisplay.xOffset*scale);
        origindy += (int)(insideDisplay.yOffset*scale);

        // for information on why we use getViewRect, see computeClip()
        Rectangle2D fullComponent = insideDisplay.getViewRect();
        if (fullComponent.getWidth() > (insideDisplay.width * scale))
            origindx = (int)((fullComponent.getWidth() - insideDisplay.width*scale)/2);
        if (fullComponent.getHeight() > (insideDisplay.height*scale))
            origindy = (int)((fullComponent.getHeight() - insideDisplay.height*scale)/2);
                                
        Rectangle2D.Double region = new Rectangle2D.Double(
            // we floor to an integer because we're dealing with exact pixels at this point
            (int)(holder.bounds.x * scale) + origindx,
            (int)(holder.bounds.y * scale) + origindy,
            (int)(holder.bounds.width * scale),
            (int)(holder.bounds.height * scale));
        return new DrawInfo2D(region, clip);
        }

    static final int MAX_TOOLTIP_LINES = 10;
    public String createToolTipText( Rectangle2D.Double rect, final GUIState simulation )
        {
        String s = "<html><font face=\"" +
            getFont().getFamily() + "\" size=\"-1\">";
        Bag[] hitObjects = objectsHitBy(rect);
        int count = 0;
        for(int x=0;x<hitObjects.length;x++)
            {
            FieldPortrayal2DHolder p = (FieldPortrayal2DHolder)(portrayals.get(x));
            for( int i = 0 ; i < hitObjects[x].numObjs ; i++ )
                {
                if (count > 0) s += "<br>";
                if (count >= MAX_TOOLTIP_LINES) { return s + "...<i>etc.</i></font></html>"; }
                count++;
                String status = p.portrayal.getStatus((LocationWrapper) (hitObjects[x].objs[i]));
                if (status != null) s += status;  // might return null, sort of meaning "leave me alone"
                }
            }
        if (count==0) return null;
        s += "</font></html>";
        return s;
        }
                
    /** */
    ArrayList selectedWrappers = new ArrayList();
    
    public void performSelection( LocationWrapper wrapper)
        {
        Bag b = new Bag();
        b.add(wrapper);
        performSelection(b);
        }
    
    public void performSelection( final Bag locationWrappers )
        {
        // deselect existing objects
        for(int x=0;x<selectedWrappers.size();x++)
            {
            LocationWrapper wrapper = ((LocationWrapper)(selectedWrappers.get(x)));
            wrapper.getFieldPortrayal().setSelected(wrapper,false);
            }
        selectedWrappers.clear();
        
        if (locationWrappers == null) return;  // deselect everything
        
        // add new wrappers
        for(int x=0;x < locationWrappers.size(); x++)
            {
            LocationWrapper wrapper = ((LocationWrapper)(locationWrappers.get(x)));
            wrapper.getFieldPortrayal().setSelected(wrapper, true);
            selectedWrappers.add(wrapper);
            }
        }
        
    public void performSelection( final Rectangle2D.Double rect )
        {
        // gather objects hit and select them, and put in selectedObjects
        Bag[] hitObjects = objectsHitBy(rect);
        Bag collection = new Bag();
        for(int x=0;x<hitObjects.length;x++)
            collection.addAll(hitObjects[x]);
        performSelection(collection);
        }

    /** Determines the inspectors appropriate for the given selection region (rect), and sends
        them on to the Controller. */
    public void createInspectors( final Rectangle2D.Double rect, final GUIState simulation )
        {
        Bag inspectors = new Bag();
        Bag names = new Bag();
        
        Bag[] hitObjects = objectsHitBy(rect);
        for(int x=0;x<hitObjects.length;x++)
            {
            FieldPortrayal2DHolder p = (FieldPortrayal2DHolder)(portrayals.get(x));
            for( int i = 0 ; i < hitObjects[x].numObjs ; i++ )
                {
                LocationWrapper wrapper = (LocationWrapper) (hitObjects[x].objs[i]);
                inspectors.add(p.portrayal.getInspector(wrapper,simulation));
                names.add(p.portrayal.getName(wrapper));
                }
            }
        simulation.controller.setInspectors(inspectors,names);
        }

    final static int SCROLL_BAR_SCROLL_RATIO = 10;

    /** Creates a frame holding the Display2D.  This is the best method to create the frame,
        rather than making a frame and putting the Display2D in it.  If you prefer the latter,
        then you need to handle two things.  First, when the frame is disposed, you need to
        call quit() on the Display2D.  Second, if you care about distribution to MacOS X
        Java 1.3.1, you need to call Utilities.doEnsuredRepaint(header) whenever the window is resized. */
    public JFrame createFrame()
        {
        JFrame frame = new JFrame()
            {
            public void dispose()
                {
                quit();       // shut down the movies
                super.dispose();
                }
            };
            
        frame.setResizable(true);
        
        // these bugs are tickled by our constant redraw requests.
        frame.addComponentListener(new ComponentAdapter()
            {
            // Bug in MacOS X Java 1.3.1 requires that we force a repaint.
            public void componentResized (ComponentEvent e) 
                {
                Utilities.doEnsuredRepaint(header);
                display.getHorizontalScrollBar().setUnitIncrement(display.getViewport().getWidth() / SCROLL_BAR_SCROLL_RATIO);
                display.getVerticalScrollBar().setUnitIncrement(display.getViewport().getHeight() / SCROLL_BAR_SCROLL_RATIO);
                }
            });
                                
        frame.getContentPane().setLayout(new BorderLayout());
        frame.getContentPane().add(this,BorderLayout.CENTER);
        
        frame.setTitle(GUIState.getName(simulation.getClass()) + " Display");
        frame.pack();
        return frame;
        }
    
    /** Returns the frame holding this Component.  If there is NO such frame, an error will
        be generated (probably a ClassCastException). */
    public Frame getFrame()
        {
        Component c = this;
        while(c.getParent() != null)
            c = c.getParent();
        return (Frame)c;
        }

    /** Takes a snapshot of the Display2D's currently displayed simulation.
        Ought only be done from the main event loop. */
        
    // Why are we using PNG?  For a couple of reasons.  First, GIF only supports 256 colors.  That had begun
    // to bite us.  Second, JPEG is good for photos but quite poor for screenshots which have lots of
    // straight lines and aliased stuff.  PNG is the RIGHT choice for what we need to do.  Unfortunately
    // it's not properly supported by old versions Internet Exploder, and so to make web-ready snapshots 
    // you may need to convert it.  :-(

    // We're using our own small PNG package (see sim.util.media) because the JAI isn't standard across
    // all platforms (notably 1.3.1) and at the time we made the call, it wasn't available on the Mac at all.

    private Object sacrificialObj = null;
    
    public void takeSnapshot()
        {
        synchronized(Display2D.this.simulation.state.schedule)
            {
            if (SimApplet.isApplet)
                {
                Object[] options = {"Oops"};
                JOptionPane.showOptionDialog(
                    this, "You cannot save snapshots from an applet.",
                    "MASON Applet Restriction",
                    JOptionPane.OK_OPTION, JOptionPane.ERROR_MESSAGE,
                    null, options, options[0]);
                return;
                }

            // do we have the PDFEncoder?
            boolean havePDF = false;

            // snap the shot FIRST
            Graphics g = insideDisplay.getGraphics();
            BufferedImage img = insideDisplay.paint(g,true,false);  // notice we're painting to a non-shared buffer
            try
                {
                sacrificialObj = Class.forName("com.lowagie.text.Cell").newInstance(); // sacrificial
                // if we survived that, then iText is installed and we're good.
                havePDF = true; 
                }
            catch (Exception e)
                {
                // oh well...
                }
                                
            g.dispose();  // because we got it with getGraphics(), we're responsible for it
                        
            // Ask what kind of thing we want to save?
            int result = 2;  // PNG by default
            if (havePDF) 
                {
                Object[] options = { "Cancel", "Save to PDF", "Save to PNG Bitmap" };
                result = JOptionPane.showOptionDialog(getFrame(), "Save window snapshot to what kind of file format?", "Save Format", 
                    JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE,
                    null, options, options[0]);
                }
                        
            if (result == 2)  // PNG
                {
                // NOW pop up the save window
                FileDialog fd = new FileDialog(getFrame(), 
                    "Save Snapshot as 24-bit PNG...", FileDialog.SAVE);
                fd.setFile("Untitled.png");
                fd.setVisible(true);
                if (fd.getFile()!=null) try
                                            {
                                            OutputStream stream = new BufferedOutputStream(new FileOutputStream(
                                                    new File(fd.getDirectory(), Utilities.ensureFileEndsWith(fd.getFile(),".png"))));
                                            PngEncoder tmpEncoder = new
                                                PngEncoder(img, false,PngEncoder.FILTER_NONE,9);
                                            stream.write(tmpEncoder.pngEncode());
                                            stream.close();
                                            }
                    catch (Exception e) { e.printStackTrace(); }
                }
            else if (result == 1)  // PDF
                {
                FileDialog fd = new FileDialog(getFrame(), 
                    "Save Snapshot as PDF...", FileDialog.SAVE);
                fd.setFile("Untitled.pdf");
                fd.setVisible(true);
                if (fd.getFile()!=null) try
                                            {
                                            PDFEncoder.generatePDF(port, new File(fd.getDirectory(), Utilities.ensureFileEndsWith(fd.getFile(),".pdf")));
                                            }
                    catch (Exception e) { e.printStackTrace(); }
                }
            else // (result == 0)  // Cancel
                {
                // don't bother
                }
            }
        }

    /** Starts a Quicktime movie on the given Display2D.  The size of the movie frame will be the size of
        the display at the time this method is called.  This method ought to be called from the main event loop.
        Most of the default movie formats provided will result in a gigantic movie, which you can
        re-encode using something smarter (like the Animation or Sorenson codecs) to put to a reasonable size.
        On the Mac, Quicktime Pro will do this quite elegantly. */
    public void startMovie()
        {
        // we synchronize because movieMaker.add() could
        // get called, via paintToMovie(), from inside the model thread rather
        // than the Swing thread (see step(...) below).  This allows us to guarantee,
        // everywhere where movieMaker is set (to null or to new), that paintToMovie
        // isn't doing anything.
        synchronized(Display2D.this.simulation.state.schedule)
            {
            // can't start a movie if we're in an applet
            if (SimApplet.isApplet)
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
            Graphics g = insideDisplay.getGraphics();
            final BufferedImage typicalImage = insideDisplay.paint(g,true,false);
            g.dispose();
                    
            if (!movieMaker.start(typicalImage))
                movieMaker = null;  // failed
            else 
                {
                movieButton.setIcon(MOVIE_ON_ICON);
                movieButton.setPressedIcon(MOVIE_ON_ICON_P);

                // start up simulation paused if necessary
                final Console console = (Console)(simulation.controller);
                if (console.getPlayState() == Console.PS_STOPPED)  // either after simulation or we just started the program
                    console.pressPause();
                
                lastEncodedSteps = -1;

                // capture the currently shown frame (important if we just paused the simulation)
                insideDisplay.paintToMovie(null);
                
                // set ourselves up to quit when stopped
                simulation.scheduleAtEnd(new Steppable()   // to stop movie when simulation is stopped
                    {
                    public void step(SimState state) { stopMovie(); }
                    });
                }
            }
        }
        

    /** Stops a Quicktime movie and cleans up, flushing the remaining frames out to disk. 
        This method ought to be called from the main event loop. */
    public void stopMovie()
        {
        // we synchronize because movieMaker.add() could
        // get called, via paintToMovie(), from inside the model thread rather
        // than the Swing thread (see step(...) below).  This allows us to guarantee,
        // everywhere where movieMaker is set (to null or to new), that paintToMovie
        // isn't doing anything.
        synchronized(Display2D.this.simulation.state.schedule)
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
                movieButton.setIcon(MOVIE_OFF_ICON);
                movieButton.setPressedIcon(MOVIE_OFF_ICON_P);
                }
            }
        }

    /** Steps the Display2D in the GUIState schedule.  If we're in MacOS X, this results in a repaint()
        request generated.  If we're in Windows or X Windows, this results in a direct call to
        paintComponent on the insideDisplay.  It's OS-dependent because different operating systems
        draw faster in different ways. */
    public void step(final SimState state)
        {
        long steps = simulation.state.schedule.getSteps();
    
        if (steps % getInterval() == 0)       // time to update!
            {
            if (insideDisplay.isShowing())
                {
                insideDisplay.repaint();
                }
            else if (movieMaker != null)  // we're not being displayed but we still need to output to a movie
                {
                insideDisplay.paintToMovie(null);
                }
            insideDisplay.updateToolTips();
            }
        }
    }
