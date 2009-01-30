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
import sim.util.*;

// we don't benefit from being a subclass of HexaValueGridPortrayal2D, but
// it makes us easily swappable in (see HexaBugs for example).  And also
// consistent with the subclass relationship between ValueGridPortrayal2D
// and FastValueGridPortrayal2D.
public class FastHexaValueGridPortrayal2D extends HexaValueGridPortrayal2D
    {
    /** If immutableField is true, we presume that the grid doesn't change.  This allows us to just
        re-splat the buffer. */
    public FastHexaValueGridPortrayal2D(String valueName, boolean immutableField)
        {
        super(valueName);
        setImmutableField(immutableField);
        }

    public FastHexaValueGridPortrayal2D(String valueName)
        {
        this(valueName,false);
        }
        
    /** If immutableField is true, we presume that the grid doesn't change.  This allows us to just
        re-splat the buffer. */
    public FastHexaValueGridPortrayal2D(boolean immutableField)
        {
        super();
        setImmutableField(immutableField);
        }

    public FastHexaValueGridPortrayal2D()
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
    DataBufferInt dbuffer;

    // our object to pass to the portrayal
    final MutableDouble valueToPass = new MutableDouble(0);

    protected void hitOrDraw(Graphics2D graphics, DrawInfo2D info, Bag putInHere)
        {
        final Grid2D field = (Grid2D)this.field;
        if (field==null) return;

        final boolean isDoubleGrid2D = (field instanceof DoubleGrid2D);
        final int maxX = field.getWidth();
        final int maxY = field.getHeight(); 
        if (maxX == 0 || maxY == 0) return;
                
        final double divideByX = ((maxX%2==0)?(3.0*maxX/2.0+0.5):(3.0*maxX/2.0+2.0));

        final double scaleWidth = 1.5 * info.draw.width / ((maxX%2==0)?(3.0*maxX/2.0+0.5):(3.0*maxX/2.0+2.0));
        final double translateWidth = info.draw.width / divideByX - scaleWidth/2.0;

        final double[][] doubleField = (isDoubleGrid2D ? ((DoubleGrid2D) field).field : null);
        final int[][] intField = (isDoubleGrid2D ? null : ((IntGrid2D) field).field);

//        final double xScale = info.draw.width / maxX;
        final double yScale = info.draw.height / (2*maxY+1);
        int startx = (int)((info.clip.x - translateWidth - info.draw.x) / scaleWidth);
        int starty = (int)((info.clip.y - info.draw.y) / (2*yScale)) - 1;
        int endx = /*startx +*/ (int)((info.clip.x - translateWidth - info.draw.x + info.clip.width) / scaleWidth) + /*2*/ 1;  // with rounding, width may be as much as 1 off
        int endy = /*starty +*/ (int)((info.clip.y - info.draw.y + info.clip.height) / (2*yScale)) + /*2*/ 1;  // with rounding, height may be as much as 1 off

        // next we determine if this is a DoubleGrid2D or an IntGrid2D
        
//        final Rectangle clip = (graphics==null ? null : graphics.getClipBounds());        

        if (graphics!=null && shouldBuffer(graphics))
            {
            // create new buffer if needed
            boolean newBuffer = false;
            
            synchronized(this)
                {
                if (buffer==null || buffer.getWidth() != maxX || buffer.getHeight() != (2*maxY+1))
                    {
                    // interestingly, this is not quite as fast as just making a BufferedImage directly!
                    // at present, transparent images can't take advantage of new Sun efficiency improvements.
                    // Perhaps we should have a new option for opaque images...
                    //buffer = graphics.getDeviceConfiguration().createCompatibleImage(maxX,(2*maxY+1),Transparency.TRANSLUCENT);
                    
                    // oops, it looks like createCompatibleImage has big-time HILARIOUS bugs on OS X Java 1.3.1!
                    // So for the time being we're sticking with the (very slightly faster) 
                    // new BufferedImage(...)
                    if (buffer != null) buffer.flush();  // in case Java forgets to clear memory -- bug in OS X
                    buffer = new BufferedImage(maxX,(2*maxY+1),BufferedImage.TYPE_INT_ARGB); // transparency allowed
                    
                    // I had thought that TYPE_INT_ARGB_PRE would be faster because
                    // it's natively supported by MacOS X CoreGraphics so no optimization needs to be done
                    // in 1.4.1 -- but in fact it is SLOWER on 1.3.1 by 2/3.  So for the time being we're
                    // going to stay with the orgiginal.
                    // see http://developer.apple.com/documentation/Java/Reference/Java14SysProperties/System_Properties/chapter_2_section_6.html
                    newBuffer = true;
                    raster = buffer.getRaster();
                    dbuffer = (DataBufferInt)(raster.getDataBuffer());
                    }
                }
            //WritableRaster _raster = raster;
            DataBufferInt _dbuffer = dbuffer;


            if (newBuffer || !immutableField || dirtyField)  // we have to load the buffer
                {
                if (endx > maxX) endx = maxX;
                if (endy > maxY) endy = maxY;
                if( startx < 0 ) startx = 0;
                if( starty < 0 ) starty = 0;
                
                if (immutableField)
                    {
                    // must load ENTIRE buffer
                    startx = 0; starty = 0; endx = maxX; endy = maxY;
                    }

                final int ex = endx;
                final int ey = endy;
                final int sx = startx;
                final int sy = starty;

                final ColorMap map = this.map;
                
                // Some history here.  We initially started by using setRGB in BufferedImage.
                // But based on some hints, we dug down.  First we grabbed the Raster and
                // used setDataElements, which gave us a big speed boost.
                // Now we're digging even further down and grabbing the data buffer, which
                // we know is a DataBufferInt, using a scanlineStride as shown.  The DataBufferInt
                // docs show how to compute which value in the (one-dimensional) data buffer to
                // poke in order to set the equivalent pixel value.  This gives us a slight
                // improvement.  We've since deleted the setRGB code, but we've kept the more
                // readable setDataElements code in comments because the scanlineStride code 
                // is so illegible.  :-)
                //
                // FastValueGridPortrayal2D has a significantly faster method still: directly setting elements
                // in the data array.  But while this works great for rectangular regions, it's
                // quite complex to do with hexagonal stuff so instead we're just setting the
                // various pixels via setElem as we go.
                //
                // Apparently for opaque BufferedImages in Sun implementations (Windows, etc.),
                // there's a new "Managed Buffer" notion -- if you don't extract the buffer, they
                // do what they can to make things as fast for you as possible -- but it doesn't
                // help in the case of transparent images.  If we add a transparency constructor,
                // we should undo the dbuffer code and go back to the raster code and see if that's
                // faster in Windows in 1.4.2 and on.  See
                // http://weblogs.java.net/blog/chet/archive/2003/08/bufferedimage_a_1.html
                
                int load;
                
                int scanlineStride = 
                    ((SinglePixelPackedSampleModel)(raster.getSampleModel())).getScanlineStride();
                if (isDoubleGrid2D)                  
                    for(int x=sx;x<ex;x++)
                        for(int y=sy;y<ey;y++)
                            {
                            if((x&1)==0)
                                {
                                // btw, setting each pixel separately is faster 
                                // than setting a 1x2 pixel grid with setDataElements!
                                load = map.getRGB(doubleField[x][y]);
                                _dbuffer.setElem((2*y)*scanlineStride + x, load);
                                _dbuffer.setElem((2*y+1)*scanlineStride + x, load);
//                              load[0] = map.getRGB(doubleField[x][y]);
//                              _raster.setDataElements(x,2*y,load);
//                              _raster.setDataElements(x,2*y+1,load);
                                }
                            else
                                {
                                load = map.getRGB(doubleField[x][y]);
                                _dbuffer.setElem((2*y+1)*scanlineStride + x, load);
                                _dbuffer.setElem((2*y+2)*scanlineStride + x, load);
//                              load[0] = map.getRGB(doubleField[x][y]);
//                              _raster.setDataElements(x,2*y+1,load);
//                              _raster.setDataElements(x,2*y+2,load);
                                }
                            }
                else
                    for(int x=sx;x<ex;x++)
                        for(int y=sy;y<ey;y++)
                            {
                            if((x&1)==0)
                                {
                                load = map.getRGB(intField[x][y]);
                                _dbuffer.setElem((2*y)*scanlineStride + x, load);
                                _dbuffer.setElem((2*y+1)*scanlineStride + x, load);
//                              load[0] = map.getRGB(intField[x][y]);
//                              _raster.setDataElements(x,2*y,load);
//                              _raster.setDataElements(x,2*y+1,load);
                                }
                            else
                                {
                                load = map.getRGB(intField[x][y]);
                                _dbuffer.setElem((2*y+1)*scanlineStride + x, load);
                                _dbuffer.setElem((2*y+2)*scanlineStride + x, load);
//                              load[0] = map.getRGB(intField[x][y]);
//                              _raster.setDataElements(x,2*y+1,load);
//                              _raster.setDataElements(x,2*y+2,load);
                                }
                            }
                }            
            
            // MacOS X 10.3 Panther has a bug which resets the clip, YUCK
            //                    graphics.setClip(clip);
            graphics.drawImage(buffer, (int)(info.draw.x+translateWidth), (int)info.draw.y, (int)(maxX*scaleWidth), (int)info.draw.height,null);
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

                        _x = (int)(translateWidth + infodrawx + scaleWidth * x);
                        _y = (int)(infodrawy + (yScale) * ((x&1)==0?2*y:2*y+1));
                        _width = (int)(translateWidth + infodrawx + scaleWidth * (x+1)) - _x;
                        _height = (int)(infodrawy + (yScale) * ((x&1)==0?2*y+2:2*y+3)) - _y;
                    
                        // draw
                        // MacOS X 10.3 Panther has a bug which resets the clip, YUCK
                        //                    graphics.setClip(clip);
                        if( graphics!=null )
                            {
                            graphics.setColor(c);
                            graphics.fillRect(_x,_y,_width,_height);
                            }
                        else
                            {
                            if( info.clip.intersects(_x,_y,_width,_height) )
                                putInHere.add(getWrapper((doubleField[x][y]), x, y));
                            }
                        }
            else
                for(int x=sx;x<ex;x++)
                    for(int y=sy;y<ey;y++)
                        {
                        final Color c = map.getColor(intField[x][y]);
                        if (c.getAlpha() == 0) continue;

                        _x = (int)(translateWidth + infodrawx + scaleWidth * x);
                        _y = (int)(infodrawy + (yScale) * ((x&1)==0?2*y:2*y+1));
                        _width = (int)(translateWidth + infodrawx + scaleWidth * (x+1)) - _x;
                        _height = (int)(infodrawy + (yScale) * ((x&1)==0?2*y+2:2*y+3)) - _y;
                    
                        // draw
                        // MacOS X 10.3 Panther has a bug which resets the clip, YUCK
                        //                    graphics.setClip(clip);
                        if( graphics!=null )
                            {
                            graphics.setColor(c);
                            graphics.fillRect(_x,_y,_width,_height);
                            }
                        else
                            {
                            if( info.clip.intersects(_x,_y,_width,_height) )
                                putInHere.add(getWrapper((intField[x][y]), x, y));
                            }
                        }
            }
        // finally, clear dirty flag if we've just drawn (don't clear if we're doing hit testing)
        if (graphics!=null) dirtyField = false;
        }
    }
