/*
  Copyright 2006 by Sean Luke and George Mason University
  Licensed under the Academic Free License version 3.0
  See the file "LICENSE" for more information
*/

package sim.app.antsforage;

import sim.field.grid.*;
import sim.portrayal.*;
import sim.portrayal.simple.*;
import sim.util.*;
import sim.engine.*;
import java.awt.*;

public class Ant extends OvalPortrayal2D implements Steppable
    {
    private static final long serialVersionUID = 1;

    public boolean getHasFoodItem() { return hasFoodItem; }
    public void setHasFoodItem(boolean val) { hasFoodItem = val; }
    public boolean hasFoodItem = false;
    double reward = 0;
        
    int x;
    int y;
        
    Int2D last;
        
    public Ant(double initialReward) { reward = initialReward; }
        
        
    // at present we have only one algorithm: value iteration.  I might
    // revise this and add our alternate (TD) algorithm.  See the papers.
        
        
    public void depositPheromone( final SimState state)
        {
        final AntsForage af = (AntsForage)state;
                
        Int2D location = af.buggrid.getObjectLocation(this);
        int x = location.x;
        int y = location.y;
                
        if (AntsForage.ALGORITHM == AntsForage.ALGORITHM_VALUE_ITERATION)
            {
            // test all around
            if (hasFoodItem)  // deposit food pheromone
                {
                double max = af.toFoodGrid.field[x][y];
                for(int dx = -1; dx < 2; dx++)
                    for(int dy = -1; dy < 2; dy++)
                        {
                        int _x = dx+x;
                        int _y = dy+y;
                        if (_x < 0 || _y < 0 || _x >= AntsForage.GRID_WIDTH || _y >= AntsForage.GRID_HEIGHT) continue;  // nothing to see here
                        double m = af.toFoodGrid.field[_x][_y] * 
                            (dx * dy != 0 ? // diagonal corners
                            af.diagonalCutDown : af.updateCutDown) +
                            reward;
                        if (m > max) max = m;
                        }
                af.toFoodGrid.field[x][y] = max;
                }
            else
                {
                double max = af.toHomeGrid.field[x][y];
                for(int dx = -1; dx < 2; dx++)
                    for(int dy = -1; dy < 2; dy++)
                        {
                        int _x = dx+x;
                        int _y = dy+y;
                        if (_x < 0 || _y < 0 || _x >= AntsForage.GRID_WIDTH || _y >= AntsForage.GRID_HEIGHT) continue;  // nothing to see here
                        double m = af.toHomeGrid.field[_x][_y] * 
                            (dx * dy != 0 ? // diagonal corners
                            af.diagonalCutDown : af.updateCutDown) +
                            reward;
                        if (m > max) max = m;
                        }
                af.toHomeGrid.field[x][y] = max;
                }
            }
        reward = 0.0;
        }

    public void act( final SimState state )
        {
        final AntsForage af = (AntsForage)state;
                
        Int2D location = af.buggrid.getObjectLocation(this);
        int x = location.x;
        int y = location.y;
                
        if (hasFoodItem)  // follow home pheromone
            {
            double max = AntsForage.IMPOSSIBLY_BAD_PHEROMONE;
            int max_x = x;
            int max_y = y;
            int count = 2;
            for(int dx = -1; dx < 2; dx++)
                for(int dy = -1; dy < 2; dy++)
                    {
                    int _x = dx+x;
                    int _y = dy+y;
                    if ((dx == 0 && dy == 0) ||
                        _x < 0 || _y < 0 ||
                        _x >= AntsForage.GRID_WIDTH || _y >= AntsForage.GRID_HEIGHT || 
                        af.obstacles.field[_x][_y] == 1) continue;  // nothing to see here
                    double m = af.toHomeGrid.field[_x][_y];
                    if (m > max)
                        {
                        count = 2;
                        }
                    // no else, yes m > max is repeated
                    if (m > max || (m == max && state.random.nextBoolean(1.0 / count++)))  // this little magic makes all "==" situations equally likely
                        {
                        max = m;
                        max_x = _x;
                        max_y = _y;
                        }
                    }
            if (max == 0 && last != null)  // nowhere to go!  Maybe go straight
                {
                if (state.random.nextBoolean(af.momentumProbability))
                    {
                    int xm = x + (x - last.x);
                    int ym = y + (y - last.y);
                    if (xm >= 0 && xm < AntsForage.GRID_WIDTH && ym >= 0 && ym < AntsForage.GRID_HEIGHT && af.obstacles.field[xm][ym] == 0)
                        { max_x = xm; max_y = ym; }
                    }
                }
            else if (state.random.nextBoolean(af.randomActionProbability))  // Maybe go randomly
                {
                int xd = (state.random.nextInt(3) - 1);
                int yd = (state.random.nextInt(3) - 1);
                int xm = x + xd;
                int ym = y + yd;
                if (!(xd == 0 && yd == 0) && xm >= 0 && xm < AntsForage.GRID_WIDTH && ym >= 0 && ym < AntsForage.GRID_HEIGHT && af.obstacles.field[xm][ym] == 0)
                    { max_x = xm; max_y = ym; }
                }
            af.buggrid.setObjectLocation(this, new Int2D(max_x, max_y));
            if (af.sites.field[max_x][max_y] == AntsForage.HOME)  // reward me next time!  And change my status
                { reward = af.reward ; hasFoodItem = ! hasFoodItem; }
            }
        else
            {
            double max = AntsForage.IMPOSSIBLY_BAD_PHEROMONE;
            int max_x = x;
            int max_y = y;
            int count = 2;
            for(int dx = -1; dx < 2; dx++)
                for(int dy = -1; dy < 2; dy++)
                    {
                    int _x = dx+x;
                    int _y = dy+y;
                    if ((dx == 0 && dy == 0) ||
                        _x < 0 || _y < 0 ||
                        _x >= AntsForage.GRID_WIDTH || _y >= AntsForage.GRID_HEIGHT || 
                        af.obstacles.field[_x][_y] == 1) continue;  // nothing to see here
                    double m = af.toFoodGrid.field[_x][_y];
                    if (m > max)
                        {
                        count = 2;
                        }
                    // no else, yes m > max is repeated
                    if (m > max || (m == max && state.random.nextBoolean(1.0 / count++)))  // this little magic makes all "==" situations equally likely
                        {
                        max = m;
                        max_x = _x;
                        max_y = _y;
                        }
                    }
            if (max == 0 && last != null)  // nowhere to go!  Maybe go straight
                {
                if (state.random.nextBoolean(af.momentumProbability))
                    {
                    int xm = x + (x - last.x);
                    int ym = y + (y - last.y);
                    if (xm >= 0 && xm < AntsForage.GRID_WIDTH && ym >= 0 && ym < AntsForage.GRID_HEIGHT && af.obstacles.field[xm][ym] == 0)
                        { max_x = xm; max_y = ym; }
                    }
                }
            else if (state.random.nextBoolean(af.randomActionProbability))  // Maybe go randomly
                {
                int xd = (state.random.nextInt(3) - 1);
                int yd = (state.random.nextInt(3) - 1);
                int xm = x + xd;
                int ym = y + yd;
                if (!(xd == 0 && yd == 0) && xm >= 0 && xm < AntsForage.GRID_WIDTH && ym >= 0 && ym < AntsForage.GRID_HEIGHT && af.obstacles.field[xm][ym] == 0)
                    { max_x = xm; max_y = ym; }
                }
            af.buggrid.setObjectLocation(this, new Int2D(max_x, max_y));
            if (af.sites.field[max_x][max_y] == AntsForage.FOOD)  // reward me next time!  And change my status
                { reward = af.reward; hasFoodItem = ! hasFoodItem; }
            }
        last = location;
        }

    public void step( final SimState state )
        {
        depositPheromone(state);
        act(state);
        }

    // a few tweaks by Sean
    private Color noFoodColor = Color.black;
    private Color foodColor = Color.red;
    public final void draw(Object object, Graphics2D graphics, DrawInfo2D info)
        {
        if( hasFoodItem )
            graphics.setColor( foodColor );
        else
            graphics.setColor( noFoodColor );

        // this code was stolen from OvalPortrayal2D
        int x = (int)(info.draw.x - info.draw.width / 2.0);
        int y = (int)(info.draw.y - info.draw.height / 2.0);
        int width = (int)(info.draw.width);
        int height = (int)(info.draw.height);
        graphics.fillOval(x,y,width, height);

        }
    }
