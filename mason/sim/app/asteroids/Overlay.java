/*
  Copyright 2009  by Sean Luke and Vittorio Zipparo
  Licensed under the Academic Free License version 3.0
  See the file "LICENSE" for more information
*/

package sim.app.asteroids;
import sim.portrayal.*;
import java.awt.*;
import java.awt.geom.*;
import java.awt.font.*;
import sim.display.*;

/** A FieldPortrayal2D which has no field, but rather draws some text on the screen.  We use
    this to draw score information etc. */

public class Overlay extends FieldPortrayal2D
    {
    AsteroidsWithUI ui;
    public static final int GUTTER = 48;
    public static final int BORDER = 8;
    public static final int FONTSIZE = 20;
    Font font = new Font("SansSerif", Font.BOLD, FONTSIZE);
    Color color = new Color(255,255,255,64);

    public Overlay(AsteroidsWithUI ui) { this.ui = ui; }
        
    int firstTimeScoreY = 0;  // surprisingly, the bounds height doesn't stay the same, so we store it here

    public void draw(Object object, Graphics2D graphics, DrawInfo2D info)
        {
        Asteroids asteroids = (Asteroids)(ui.state);
        graphics.setFont(font);
                
        // first figure out the Y location for the scores etc.  This is done ONCE and stored away because
        // the font numbers sometimes don't have the same bounding box vertically, causing the text to jump about.
        Rectangle2D bounds = new TextLayout("" + asteroids.score, font,  graphics.getFontRenderContext()).getBounds();
        if (firstTimeScoreY == 0)
            firstTimeScoreY = (int)((GUTTER + bounds.getHeight()) / 2);
                
        // if we're paused, say so
        if (((SimpleController)(ui.controller)).getPlayState() == SimpleController.PS_PAUSED)
            {
            bounds = new TextLayout("Paused", font,  graphics.getFontRenderContext()).getBounds();
            graphics.setColor(Color.white);
            graphics.drawString("Paused", (int)((info.clip.width - bounds.getWidth()) / 2), (int)((info.clip.height - bounds.getHeight()) / 2));
            }
        
        // show scores at top
        graphics.setColor(color);
        String text = "Deaths: " + asteroids.deaths;
        drawOutline(graphics, text, BORDER, firstTimeScoreY);
        text = "Level: " + asteroids.level;
        drawOutline(graphics, text, BORDER, firstTimeScoreY + FONTSIZE  * 1.5 );
        text = "Score: " + asteroids.score;
        drawOutline(graphics, text, BORDER, firstTimeScoreY + FONTSIZE  * 3 );

        // show the text at bottom
        text = "M: MASON";
        drawOutline(graphics, text, BORDER, info.clip.height - GUTTER + firstTimeScoreY - FONTSIZE * 4.5);
        text = "P: Pause";
        drawOutline(graphics, text, BORDER, info.clip.height - GUTTER + firstTimeScoreY- FONTSIZE * 3);
        text = "R: Reset";
        drawOutline(graphics, text, BORDER, info.clip.height - GUTTER + firstTimeScoreY- FONTSIZE * 1.5);
        text = "\u2190\u2192\u2191\u2193 space";
        drawOutline(graphics, text, BORDER, info.clip.height - GUTTER + firstTimeScoreY);

        }
                
        
    public void drawOutline(Graphics2D graphics, String text, double x, double y)
        {
        TextLayout textlo = new TextLayout(text, font,  graphics.getFontRenderContext());
        Shape outline = textlo.getOutline(null);
        AffineTransform transform = graphics.getTransform();
        AffineTransform oldTransform = graphics.getTransform();
        transform.translate(x,y);
        graphics.transform(transform);
        graphics.draw(outline);
        graphics.setTransform(oldTransform);
        }
        
        
    }