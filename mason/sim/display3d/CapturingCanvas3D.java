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
 * an image.
 *
 * @author Gabriel Catalin Balan
 * @see http://www.j3d.org/faq/examples/CapturingCanvas3D.java
 * 
 * @todo try offscreen rendering
 **/

/*
  The original CapturingCanvas3D in the URL above is due to Peter Z. Kunszt at Johns Hopkins University.
  Our version borrows enough snippets and ideas from the original that due credit is deserved.
*/


public class CapturingCanvas3D extends Canvas3D
    {
    boolean writeBuffer_;
    boolean keepOnWriting_;
    BufferedImage buffer_;
    int x, y, width, height;
    
    public CapturingCanvas3D(GraphicsConfiguration gc) 
        {
        super(gc);
        }
    
    public CapturingCanvas3D(GraphicsConfiguration arg0, boolean arg1)
        {
        super(arg0, arg1);
        }
    
    public synchronized BufferedImage getFrameAsImage()
        {
        return buffer_;
        }
    
    /** sets the capturing regime and area */
    public void setWritingParams(Rectangle2D r, boolean movie)
        {
        synchronized(this)
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
        if(doubleRasterRead)ctx.readRaster(ras); 
        // Now strip out the image info
        buffer_ = ras.getImage().getImage();
        buffer_.flush();  // prevents possible os x 1.4.2 memory leak
        }
    
    public synchronized void stopMovie()
        {
        keepOnWriting_ =false;
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
