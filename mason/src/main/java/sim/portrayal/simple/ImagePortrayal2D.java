/*
  Copyright 2006 by Sean Luke and George Mason University
  Licensed under the Academic Free License version 3.0
  See the file "LICENSE" for more information
*/

package sim.portrayal.simple;
import sim.portrayal.*;
import java.awt.*;
import javax.swing.*;

/**
   A simple portrayal for 2D visualization of images. It extends the SimplePortrayal2D and
   it manages the drawing and hit-testing for rectangular shapes containing images/pictures.
   
   <p>ImagePortrayal2D draws an image centered on the Portrayal's origin.  
   Images are not stretched to fill the info.draw.height x info.draw.width rectangle.  Instead, if the image
   is taller than it is wide, then the width of the image will be info.draw.width * scale and the height
   will stay in proportion; else the height of the image will be info.draw.height * scale and the
   width will stay in proportion.
*/

// if you want something more sophisticated than this (such as responding
// to the color values in a useful fashion) you may need to use Java2D's
// drawImage(BufferedImage img, BufferedImageOp op,  int x, int y) method,
// and that would be expensive.  :-)

// if you want more exact control over the scaling method for the image
// (nearest neighbor vs. bilinear), you can set the KEY_INTERPOLATION
// rendering hint in Java2D.

public class ImagePortrayal2D extends RectanglePortrayal2D
    {
    public Image image;
    
    java.awt.geom.AffineTransform preciseTransform = new java.awt.geom.AffineTransform();
    
    /** Creates an ImagePortrayal2D with the image inside the given ImageIcon, and scaled with the given scale. */
    public ImagePortrayal2D(ImageIcon icon, double scale)
        {
        this(icon.getImage(), scale);
        }
                
    /** Creates an ImagePortrayal2D with the image inside the given ImageIcon. */
    public ImagePortrayal2D(ImageIcon icon)
        {
        this(icon, 1.0);
        }
        
    /** Creates an ImagePortrayal2D by loading an Image resource using getClass().getResource(), and scaled with the given scale. */
    public ImagePortrayal2D(Class c, String resourceName, double scale)
        {
        this(new ImageIcon(c.getResource(resourceName)), scale);
        }

    /** Creates an ImagePortrayal2D by loading an Image resource using getClass().getResource() */
    public ImagePortrayal2D(Class c, String resourceName)
        {
        this(c, resourceName, 1.0);
        }

    /** Creates an ImagePortrayal2D with the given image. */
    public ImagePortrayal2D(Image image)  { this(image,1.0); }

    /** Creates an ImagePortrayal2D with the given image, and scaled with the given scale.. */
    public ImagePortrayal2D(Image image, double scale)  
        { 
        super(null,scale);  // don't bother with color
        this.image = image;
        this.scale = scale;
        }
        
    // assumes the graphics already has its color set
    public void draw(Object object, Graphics2D graphics, DrawInfo2D info)
        {
        if (image==null) return;
        // in this example we ALWAYS draw the image, even if the color is set to 0 alpha...

        final int iw = image.getWidth(null);
        final int ih = image.getHeight(null);
        double width;
        double height;
        
        if (ih > iw)
            {
            width = info.draw.width*scale;
            height = (ih*width)/iw;  // ih/iw = height / width
            }
        else
            {
            height = info.draw.height * scale;
            width = (iw * height)/ih;  // iw/ih = width/height
            }

        final double x = (info.draw.x - width / 2.0);
        final double y = (info.draw.y - height / 2.0);

        // draw centered on the origin
        if (info.precise)
            {
            preciseTransform.setToScale(width, height);
            preciseTransform.translate(x, y);
            graphics.drawImage(image, preciseTransform, null);
            }
        else graphics.drawImage(image,(int)x,(int)y,(int)width,(int)height,null);
        }
        
    }
