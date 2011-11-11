/*
  Copyright 2009  by Sean Luke and Vittorio Zipparo
  Licensed under the Academic Free License version 3.0
  See the file "LICENSE" for more information
*/

package sim.app.pacman;
import java.awt.*;
import sim.field.grid.*;
import sim.portrayal.*;
import sim.portrayal.grid.*;
import java.awt.geom.*;
import sim.util.*;

/** 
    A special portrayal for drawing part of the maze.  The way this portion
    is drawn (as a curved or straight line) depends in part on neighboring
    parts of the maze, making the portrayal a bit more complex.  We use the
    new *location* field in DrawInfo2D to determine where we are in the maze,
    and hence what our neighbors are.  Drawing is then done using Java2D
    operators.
*/

public class MazeCellPortrayal extends SimplePortrayal2D
    {
    private static final long serialVersionUID = 1;

    IntGrid2D field;
        
    public MazeCellPortrayal(IntGrid2D field) { this.field = field; }
        
    QuadCurve2D.Double curve = new QuadCurve2D.Double();
    Line2D.Double line = new Line2D.Double();
    Color color = new Color(33,33,222);  // traditional pacman color
    BasicStroke stroke = new BasicStroke(2f);

    public void draw(Object object, Graphics2D g, DrawInfo2D info)
        {
        int[][] grid = field.field;
        MutableInt2D location = (MutableInt2D)(info.location);
        int x = location.x;
        int y = location.y;
                
        double ox = info.draw.x;
        double oy = info.draw.y;
        double sc = info.draw.width / 2;
                
        // only certain grid patterns are allowed in PacMan
        //
        //      O    O    O    X    X    X    X    X    X
        //  O  OXX  XXO  XXX  OXX  XXO  XXX  XXO  OXX  XXX
        //      X    X    X    O    O    O    X    X    X
        //
        //  ... where X = wall, O = open, space = don't care.  
        //  That is, if there is an O on one side, there MUST be
        //  an X on the other side.  Except when the object itself is an O.
        //  
        //  There are also inside curves:
        //
        //  OX   XO
        //  XX   XX  XX  XX
        //           OX  XO
        //
        //  We identify which patern you have:
                
        if ((int)(((MutableDouble)object).val) == 0)  // we're open
            {
            return;
            }
        else            // we're a wall.  Need to draw
            {
            // get the N/S/E/W values
            int height = field.getHeight() - 1;
            int width = field.getWidth() - 1;
            int n = (y == 0 ? 1 : grid[x][y-1]);
            int w = (x == 0 ? 1 : grid[x-1][y]);
            int s = (y == height ? 1 : grid[x][y+1]);
            int e = (x == width ? 1 : grid[x+1][y]);
                        
            g.setColor(color);
            g.setStroke(stroke);
                        
            if (n == 0)
                {
                if (w == 0)
                    {
                    curve.setCurve(ox + sc, oy, ox, oy, ox, oy + sc);  // curve left to up
                    g.draw(curve);
                    }
                else if (e == 0)
                    {
                    curve.setCurve(ox - sc, oy, ox, oy, ox, oy + sc);  // curve right to up
                    g.draw(curve);
                    }
                else  // neither
                    {
                    line.setLine(ox + sc, oy, ox - sc, oy); // horizontal line
                    g.draw(line);
                    }
                }
            else if (s == 0)
                {
                if (w == 0)
                    {
                    curve.setCurve(ox + sc, oy, ox, oy, ox, oy - sc);  // curve left to down
                    g.draw(curve);
                    }
                else if (e == 0)
                    {
                    curve.setCurve(ox - sc, oy, ox, oy, ox, oy - sc);  // curve right to down
                    g.draw(curve);
                    }
                else  // neither
                    {
                    line.setLine(ox + sc, oy, ox - sc, oy); // horizontal line
                    g.draw(line);
                    }
                }
            else if (e == 0)
                {
                line.setLine(ox, oy + sc, ox, oy - sc); // vertical line
                g.draw(line);
                }
            else if (w == 0)
                {
                line.setLine(ox, oy + sc, ox, oy - sc); // vertical line
                g.draw(line);
                }
            else  // perhaps an inside curve?
                {
                int nw = (y == 0 || x == 0 ? 1 : grid[x-1][y-1]);
                int sw = (y == height || x == 0 ? 1 : grid[x-1][y+1]);
                int ne = (y == 0 || x == width ? 1 : grid[x+1][y-1]);
                int se = (y == height || x == width ? 1 : grid[x+1][y+1]);

                if (nw == 0)
                    {
                    curve.setCurve(ox - sc, oy, ox, oy, ox, oy - sc);
                    g.draw(curve);
                    }
                else if (sw == 0)
                    {
                    curve.setCurve(ox - sc, oy, ox, oy, ox, oy + sc);
                    g.draw(curve);
                    }
                else if (ne == 0)
                    {
                    curve.setCurve(ox + sc, oy, ox, oy, ox, oy - sc);
                    g.draw(curve);
                    }
                else if (se == 0)
                    {
                    curve.setCurve(ox + sc, oy, ox, oy, ox, oy + sc);
                    g.draw(curve);
                    }
                else
                    {
                    // nada
                    }
                }
            }
                        
        }
    }