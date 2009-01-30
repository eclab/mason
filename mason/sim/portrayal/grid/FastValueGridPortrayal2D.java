/*
  Copyright 2006 by Sean Luke and George Mason University
  Licensed under the Academic Free License version 3.0
  See the file "LICENSE" for more information
*/

package sim.portrayal.grid;
import sim.portrayal.*;
import sim.field.grid.*;
import java.awt.*;
import java.awt.image.*;
import sim.util.gui.ColorMap;

/**
   This class works like a ValueGridPortrayal2D, <b>except</b> that it doesn't use an underlying Portrayal for the object
   (instead it always draws a rectangle), and may ignore the getColor() method, so you shouldn't override that method to customize
   the color function in nonlinear ways any more.  setColorTable() and setLevels() are still supported.  
   Use this class instead of
   ValueGridPortrayal2D unless you need to customize how the values are drawn (other than the color range or lookup table).

   <p>Additionally, FastValueGridPortrayal2D is useful if your grid never changes past its first drawing.  For example, if
   you're drawing static obstacles, cities, etc., which never change in value during the invocation of the simulation, then
   FastValueGridPortrayal2D can draw them efficiently by just drawing once into its buffer and re-displaying the buffer over
   and over again.  Just pass in true in the constructor.  

   <p>If your grid does change but only occasionally, you can also use this technique as well; just manually call reset() 
   whenever the grid changes to inform the FastValueGridPortrayal2D that it needs to re-compute the buffer.  reset() is threadsafe.

   <h3>Important Note on Drawing Methods</h3>

   FastValueGridPortrayal2D can draw a grid in two ways.  First, it can draw each of the rects individually ("USE_BUFFER").  Second, it can create a bitmap the size of the grid (one pixel per grid location), poke the colors into the bitmap, then stretch the bitmap over the area and draw it ("DONT_USE_BUFFER").  You can specify the method by calling the <b>setBuffering()</b> method; optionally you can just let FastValueGridPortrayal2D guess which to use ("DEFAULT").  But you should know what you're doing, as methods can be <i>much</i> faster than each other depending on the situation.  Use the following as guides

   <dl><dt><b>MacOS X</b>
   <dd>USE_BUFFER is much faster than DONT_USE_BUFFER in all cases, but can draw incorrectly aliased ("fuzzed out") rectangles when writing to media (a movie or a snapshot).  The DEFAULT is for MacOS X is set to USE_BUFFER in ordinary drawing, and DONT_USE_BUFFER when writing to media.
   <dt><b>Windows and X Windows</b>
   <dd>If you're not using any transparency (alpha), then DONT_USE_BUFFER is a tad faster than USE_BUFFER.  But if you're using transparency, then DONT_USE_BUFFER is <i>very</i> slow -- in this case, try USE_BUFFER.  Note however that in any case USE_BUFFER requires a <i>lot</i> of memory on Windows and X Windows due to poor implementation by Sun.  You'll want to increase the default memory capacity, and expect occasional pauses for full garbage collection.  You can test how often full garbage collection by running with <tt><b>java -verbose:gc ...</b></tt> and looking at how often
   the <tt>FULL GC</tt> printouts happen.  Ordinary <tt>GC</tt> you shouldn't worry about.  You can increase the default memory capacity to 50 Megabytes, for example (the default is about 20) by running with <tt><b>java -Xms50M ...</b></tt>  The DEFAULT is for XWindows and Windows to use DONT_USE_BUFFER.  You'll want to change this for sure if you're doing any transparency.
   </dl>
*/

public class FastValueGridPortrayal2D extends ValueGridPortrayal2D
    {
    /** If immutableField is true, we presume that the grid doesn't change.  This allows us to just
        re-splat the buffer. */
    public FastValueGridPortrayal2D(String valueName, boolean immutableField)
        {
        super(valueName);
        setImmutableField(immutableField);
        }

    public FastValueGridPortrayal2D(String valueName)
        {
        this(valueName,false);
        }
        
    /** If immutableField is true, we presume that the grid doesn't change.  This allows us to just
        re-splat the buffer. */
    public FastValueGridPortrayal2D(boolean immutableField)
        {
        super();
        setImmutableField(immutableField);
        }

    public FastValueGridPortrayal2D()
        {
        this(false);
        }

    public void reset()
        {
        synchronized(this)
            {
            buffer = null;
            }
        }

    // Determines if we should buffer
    boolean shouldBuffer(Graphics2D graphics)
        {
        // We can either draw lots of rects, or we can pixels to a small bitmap, then
        // stretch the bitmap into rects using drawImage.  Which technique is faster depends
        // on the OS unfortunately.  Solaris prefers the bitmap.  Linux prefers the rects
        // very much.  Windows prefers the rects,
        // except for small draws where bitmaps have a slight edge (which we'll not consider).
        // MacOS X prefers bitmaps, but will not stretch and draw to an image buffer
        // without doing fancy-pants interpolation which looks horrible, so we have to check for that.

        // For now we'll do:
        // The user can override us if he likes in the options pane.  Otherwise...
        // in Windows, only use the buffer if it's an immutable grid
        // in MacOS X, use the buffer for all non-image writes ONLY
        // in X Windows don't use the buffer ever
        // ...this puts Solaris at a disadvantage but given that Linux is more common...
        
        int buffering = getBuffering();
        if (buffering==USE_BUFFER) return true;
        else if (buffering==DONT_USE_BUFFER) return false;
        else if (sim.display.Display2D.isMacOSX)
            return (graphics.getDeviceConfiguration().
                getDevice().getType() != GraphicsDevice.TYPE_IMAGE_BUFFER);
        else if (sim.display.Display2D.isWindows)
            return (immutableField && !dirtyField);
        else // it's Linux or Solaris
            return false;
        }
        
    BufferedImage buffer;
    WritableRaster raster;
    int[] data = new int[0];

    // Should draw itself within the box from (0,0) to (1,1)
    public void draw(Object object, Graphics2D graphics, DrawInfo2D info)
        {
        final Grid2D field = (Grid2D)this.field;
        if (field==null) return;
        
        // first question: determine the range in which we need to draw.
        final int maxX = field.getWidth();
        final int maxY = field.getHeight(); 
        if (maxX == 0 || maxY == 0) return;

        final double xScale = info.draw.width / maxX;
        final double yScale = info.draw.height / maxY;
        int startx = (int)((info.clip.x - info.draw.x) / xScale);
        int starty = (int)((info.clip.y - info.draw.y) / yScale);
        int endx = /*startx +*/ (int)((info.clip.x - info.draw.x + info.clip.width) / xScale) + /*2*/ 1;  // with rounding, width may be as much as 1 off
        int endy = /*starty +*/ (int)((info.clip.y - info.draw.y + info.clip.height) / yScale) + /*2*/ 1;  // with rounding, height may be as much as 1 off
        
        // next we determine if this is a DoubleGrid2D or an IntGrid2D
        
//        final Rectangle clip = (graphics==null ? null : graphics.getClipBounds());

        final boolean isDoubleGrid2D = (field instanceof DoubleGrid2D);
        final double[][] doubleField = (isDoubleGrid2D ? ((DoubleGrid2D) field).field : null);
        final int[][] intField = (isDoubleGrid2D ? null : ((IntGrid2D) field).field);
        
        
        if (shouldBuffer(graphics))
            {
            // create new buffer if needed
            boolean newBuffer = false;
            //BufferedImage _buffer = null;  // make compiler happy
            
            synchronized(this)
                {
                if (buffer==null || buffer.getWidth() != maxX || buffer.getHeight() != maxY)
                    {
                    // interestingly, this is not quite as fast as just making a BufferedImage directly!
                    // at present, transparent images can't take advantage of new Sun efficiency improvements.
                    // Perhaps we should have a new option for opaque images...
                    //buffer = graphics.getDeviceConfiguration().createCompatibleImage(maxX,maxY,Transparency.TRANSLUCENT);
                    
                    // oops, it looks like createCompatibleImage has big-time HILARIOUS bugs on OS X Java 1.3.1!
                    // So for the time being we're sticking with the (very slightly faster) 
                    // new BufferedImage(...)
                    if (buffer != null) buffer.flush();  // in case Java forgets to clear memory -- bug in OS X
                    buffer = new BufferedImage(maxX,maxY,BufferedImage.TYPE_INT_ARGB); // transparency allowed
                    
                    // I had thought that TYPE_INT_ARGB_PRE would be faster because
                    // it's natively supported by MacOS X CoreGraphics so no optimization needs to be done
                    // in 1.4.1 -- but in fact it is SLOWER on 1.3.1 by 2/3.  So for the time being we're
                    // going to stay with the orgiginal.
                    // see http://developer.apple.com/documentation/Java/Reference/Java14SysProperties/System_Properties/chapter_2_section_6.html
                    //
                    // UPDATE: Running with Quartz and Java 1.6, it doesn't seem that ARGB and ARGB_PRE have
                    // any really significant difference in speed.  Maybe 5%.  Sticking with ARGB to be more compatible with
                    // Windows.
                    raster = buffer.getRaster();
                    newBuffer = true;
                    }
                //_buffer = buffer;
                }

            if (newBuffer || !immutableField || dirtyField)  // we have to load the buffer
                {
                if (endx > maxX) endx = maxX;
                if (endy > maxY) endy = maxY;
                if( startx < 0 ) startx = 0;
                if( starty < 0 ) starty = 0;
                
                final int ex = endx;
                final int ey = endy;
                final int sx = startx;
                final int sy = starty;
                
                if (immutableField)
                    {
                    // must load ENTIRE buffer
                    startx = 0; starty = 0; endx = maxX; endy = maxY;
                    }

                final ColorMap map = this.map;
            
                if (ex-sx > 0 && ey-sy > 0)  // could be otherwise if drawing off-screen...
                    {
                    int[] data = this.data;  // reuse
                    if (data.length != (ex-sx)*(ey-sy)) 
                        data = this.data = new int[(ex-sx)*(ey-sy)];
                    int i = 0;
                    if (isDoubleGrid2D)
                        for(int y=sy;y<ey;y++)
                            for(int x=sx;x<ex;x++)
                                data[i++] = map.getRGB(doubleField[x][y]);
                    else
                        for(int y=sy;y<ey;y++)
                            for(int x=sx;x<ex;x++)
                                data[i++] = map.getRGB(intField[x][y]);
                    raster.setDataElements(sx,sy,ex-sx,ey-sy,data);
                    }

// setting data elements above is faster than individual setRGBs
//                if (isDoubleGrid2D)
//                    for(int x=sx;x<ex;x++)
//                        for(int y=sy;y<ey;y++)
//                            _buffer.setRGB(x,y,map.getRGB(doubleField[x][y]));
//                else
//                    for(int x=sx;x<ex;x++)
//                        for(int y=sy;y<ey;y++)
//                            _buffer.setRGB(x,y,map.getRGB(intField[x][y]));
                }
                
            // MacOS X 10.3 Panther has a bug which resets the clip, YUCK
            //                    graphics.setClip(clip);
            graphics.drawImage(buffer, (int)info.draw.x, (int)info.draw.y, (int)info.draw.width, (int)info.draw.height,null);
            }
        else
            {
            buffer = null;  // GC the buffer in case the user had changed his mind
            
            if (endx > maxX) endx = maxX;
            if (endy > maxY) endy = maxY;
            if( startx < 0 ) startx = 0;
            if( starty < 0 ) starty = 0;

            final int ex = endx;
            final int ey = endy;
            final int sx = startx;
            final int sy = starty;

            int _x = 0;
            int _y = 0;
            int _width = 0;
            int _height = 0;

            // locals are faster...
            final ColorMap map = this.map;
            final double infodrawx = info.draw.x;
            final double infodrawy = info.draw.y;
 
            // 1.3.1 doesn't hoist -- does 1.4.1?
            if (isDoubleGrid2D)
                for(int x=sx;x<ex;x++)
                    for(int y=sy;y<ey;y++)
                        {
                        final Color c = map.getColor(doubleField[x][y]);
                        if (c.getAlpha() == 0) continue;
                        graphics.setColor(c);
                            
                        _x = (int)(infodrawx + (xScale) * x);
                        _y = (int)(infodrawy + (yScale) * y);
                        _width = (int)(infodrawx + (xScale) * (x+1)) - _x;
                        _height = (int)(infodrawy + (yScale) * (y+1)) - _y;
                    
                        // draw
                        // MacOS X 10.3 Panther has a bug which resets the clip, YUCK
                        //                    graphics.setClip(clip);
                        graphics.fillRect(_x,_y,_width,_height);
                        }
            else
                for(int x=sx;x<ex;x++)
                    for(int y=sy;y<ey;y++)
                        {
                        final Color c = map.getColor(intField[x][y]);
                        if (c.getAlpha() == 0) continue;
                        graphics.setColor(c);
                            
                        _x = (int)(infodrawx + (xScale) * x);
                        _y = (int)(infodrawy + (yScale) * y);
                        _width = (int)(infodrawx + (xScale) * (x+1)) - _x;
                        _height = (int)(infodrawy + (yScale) * (y+1)) - _y;
                    
                        // draw
                        // MacOS X 10.3 Panther has a bug which resets the clip, YUCK
                        //                    graphics.setClip(clip);
                        graphics.fillRect(_x,_y,_width,_height);
                        }
            }

        // finally, clear dirty flag if we've just drawn (don't clear if we're doing hit testing)
        if (graphics!=null) dirtyField = false;
        }
    }
