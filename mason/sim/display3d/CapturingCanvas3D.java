/*
  Copyright 2006 by Sean Luke and George Mason University
  Licensed under the Academic Free License version 3.0
  See the file "LICENSE" for more information
*/

package sim.display3d;

import java.awt.*;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;

import javax.vecmath.*;
import javax.media.j3d.*;

/** 
 * Canvas3D that is synchronized with Display3D and 
 * uses postswap to save the contextGraphics into
 * an image.  You can wait() on the Canvas3D to be notified
 * when a new image has arrived.
 *
 * <p>The original CapturingCanvas3D in the URL above is due to Peter Z. Kunszt at Johns Hopkins University.
 * Our version borrows enough snippets and ideas from the original that due credit is definitely deserved.
 *
 * @author Gabriel Catalin Balan
 * @see http://www.j3d.org/faq/examples/CapturingCanvas3D.java
 * 
 * @todo try offscreen rendering
 */


public class CapturingCanvas3D extends Canvas3D
    {
    boolean writeBuffer_;
    boolean keepOnWriting_;
    BufferedImage buffer_;
    int x, y, width, height;
    
    public CapturingCanvas3D(GraphicsConfiguration graphicsConfiguration) 
        {
        super(graphicsConfiguration);
        }
    
    public CapturingCanvas3D(GraphicsConfiguration graphicsConfiguration, boolean offScreen)
        {
        super(graphicsConfiguration, offScreen);
        }
    
    public synchronized BufferedImage getLastImage()
        {
        return buffer_;
        }

    // how big should the image be to draw?
    Rectangle2D getImageSize()
        {
        Dimension s = getSize();
        Rectangle2D clip = new Rectangle2D.Double(0,0,s.width,s.height);
        Rectangle bounds = getGraphics().getClipBounds();
        if(bounds != null)
            clip = clip.createIntersection(bounds);
        return clip;
        }

    /** sets the capturing regime and area */
    public void beginCapturing(boolean movie)
        {
        Rectangle2D r = getImageSize();
        synchronized(this)  // must be synchronized here
            {
            x  = (int)r.getX();
            y  = (int)r.getY();
            width  = (int)r.getWidth();
            height  = (int)r.getHeight();
                
            writeBuffer_ = true;
            keepOnWriting_ |= movie;
            //(the user can record movie and take still shots)
            }
        fillBuffer(true);
        }
    
    // doubleRasterRead is a workaround for a Java bug where the first
    // image may be black.
    void fillBuffer(boolean doubleRasterRead)
        {
        GraphicsContext3D  ctx = getGraphicsContext3D();
        // The raster components need all be set!
        Raster ras = new Raster(
            new Point3f(-1.0f,-1.0f,-1.0f),
            Raster.RASTER_COLOR,
            x, y,
            width, height,
            new ImageComponent2D(
                ImageComponent.FORMAT_RGB,
                new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB)),
            null);
        ctx.readRaster(ras);
        if(doubleRasterRead)ctx.readRaster(ras);  // in case the first was empty
        // Now strip out the image info
        buffer_ = ras.getImage().getImage();
        buffer_.flush();  // prevents possible os x 1.4.2 memory leak
        }
    
    public synchronized void stopCapturing()
        {
        keepOnWriting_ = false;
        }

    public void postSwap()
        {
        if(writeBuffer_)
            {
            fillBuffer(false);
            if(!keepOnWriting_)
                writeBuffer_ = false;
            }
        }
    
    public void postRender()
        {
        synchronized(this){ notify(); }
        }
    }
