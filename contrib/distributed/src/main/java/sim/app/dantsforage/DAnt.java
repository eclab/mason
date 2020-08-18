package sim.app.dantsforage;

/*
Copyright 2006 by Sean Luke and George Mason University
Licensed under the Academic Free License version 3.0
See the file "LICENSE" for more information
*/


import sim.field.partitioning.IntPoint;

import sim.portrayal.*;

import sim.util.*;

import sim.engine.*;
import java.awt.*;

public class DAnt extends DSteppable
  {
  private static final long serialVersionUID = 1;

  public boolean getHasFoodItem() { return hasFoodItem; }
  public void setHasFoodItem(boolean val) { hasFoodItem = val; }
  public boolean hasFoodItem = false;
  double reward = 0;
      
  int x;
  int y;
      
  Int2D last;
      
  public DAnt(double initialReward) { 
	  super();
	  reward = initialReward; 
	  }
      
      
  // at present we have only one algorithm: value iteration.  I might
  // revise this and add our alternate (TD) algorithm.  See the papers.
      
      
  public void depositPheromone( final SimState state)
      {
      final DAntsForage af = (DAntsForage)state;
              
      //Int2D location = af.buggrid.getObjectLocation(this); 
      //int x = location.x;
      //int y = location.y;
      
      int x = this.x;
      int y = this.y;
      
      
      if (DAntsForage.ALGORITHM == DAntsForage.ALGORITHM_VALUE_ITERATION)
          {
          // test all around
          if (hasFoodItem)  // deposit food pheromone
              {
        	  

        	  
        	          	  
              double max = af.toFoodGrid.get(new IntPoint(x, y));
              for(int dx = -1; dx < 2; dx++)
                  for(int dy = -1; dy < 2; dy++)
                      {
                      int _x = dx+x;
                      int _y = dy+y;
                      if (_x < 0 || _y < 0 || _x >= DAntsForage.GRID_WIDTH || _y >= DAntsForage.GRID_HEIGHT) continue;  // nothing to see here
                      double m = af.toFoodGrid.get(new IntPoint(_x, _y))* 
                          (dx * dy != 0 ? // diagonal corners
                          af.diagonalCutDown : af.updateCutDown) +
                          reward;
                      if (m > max) max = m;
                      }
              af.toFoodGrid.add(new IntPoint(x, y), max); 
              }
          else
              {
              double max = af.toHomeGrid.get(new IntPoint(x, y));
              for(int dx = -1; dx < 2; dx++)
                  for(int dy = -1; dy < 2; dy++)
                      {
                      int _x = dx+x;
                      int _y = dy+y;
                      if (_x < 0 || _y < 0 || _x >= DAntsForage.GRID_WIDTH || _y >= DAntsForage.GRID_HEIGHT) continue;  // nothing to see here
                      double m = af.toHomeGrid.get(new IntPoint(_x, _y)) * 
                          (dx * dy != 0 ? // diagonal corners
                          af.diagonalCutDown : af.updateCutDown) +
                          reward;
                      if (m > max) max = m;
                      }
              af.toHomeGrid.add(new IntPoint(x, y), max); 
              }
          }
      reward = 0.0;
      }

  public void act( final SimState state )
      {
      final DAntsForage af = (DAntsForage)state;
              
      //Int2D location = af.buggrid.getObjectLocation(this);
      int x = this.x;
      int y = this.y;
              
      if (hasFoodItem)  // follow home pheromone
          {
          double max = DAntsForage.IMPOSSIBLY_BAD_PHEROMONE;
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
                      _x >= DAntsForage.GRID_WIDTH || _y >= DAntsForage.GRID_HEIGHT || 
                      af.obstacles.get(new IntPoint(_x, _y)) == 1) continue;  // nothing to see here
                  double m = af.toHomeGrid.get(new IntPoint(_x, _y));
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
                  if (xm >= 0 && xm < DAntsForage.GRID_WIDTH && ym >= 0 && ym < DAntsForage.GRID_HEIGHT && af.obstacles.get(new IntPoint(xm, ym)) == 0)
                      { max_x = xm; max_y = ym; }
                  }
              }
          else if (state.random.nextBoolean(af.randomActionProbability))  // Maybe go randomly
              {
              int xd = (state.random.nextInt(3) - 1);
              int yd = (state.random.nextInt(3) - 1);
              int xm = x + xd;
              int ym = y + yd;
              if (!(xd == 0 && yd == 0) && xm >= 0 && xm < DAntsForage.GRID_WIDTH && ym >= 0 && ym < DAntsForage.GRID_HEIGHT && af.obstacles.get(new IntPoint(xm, ym)) == 0)
                  { max_x = xm; max_y = ym; }
              }
          //af.buggrid.setObjectLocation(this, new Int2D(max_x, max_y)); ////INSTEAD, use HaloField move or moveAgent, create an IntPoint for this? (move instead of set)
          af.buggrid.moveAgent(new IntPoint(this.x, this.y), new IntPoint(max_x, max_y), this);
          this.x = max_x;
          this.y = max_y;
          
          
          if (af.sites.get(new IntPoint(max_x, max_y)) == DAntsForage.HOME)  // reward me next time!  And change my status
              { reward = af.reward ; hasFoodItem = ! hasFoodItem; }
          }
      else
          {
          double max = DAntsForage.IMPOSSIBLY_BAD_PHEROMONE;
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
                      _x >= DAntsForage.GRID_WIDTH || _y >= DAntsForage.GRID_HEIGHT || 
                      af.obstacles.get(new IntPoint(_x, _y)) == 1) continue;  // nothing to see here
                  double m = af.toFoodGrid.get(new IntPoint(_x, _y));
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
                  if (xm >= 0 && xm < DAntsForage.GRID_WIDTH && ym >= 0 && ym < DAntsForage.GRID_HEIGHT && af.obstacles.get(new IntPoint(xm, ym)) == 0)
                      { max_x = xm; max_y = ym; }
                  }
              }
          else if (state.random.nextBoolean(af.randomActionProbability))  // Maybe go randomly
              {
              int xd = (state.random.nextInt(3) - 1);
              int yd = (state.random.nextInt(3) - 1);
              int xm = x + xd;
              int ym = y + yd;
              if (!(xd == 0 && yd == 0) && xm >= 0 && xm < DAntsForage.GRID_WIDTH && ym >= 0 && ym < DAntsForage.GRID_HEIGHT && af.obstacles.get(new IntPoint(xm, ym)) == 0)
                  { max_x = xm; max_y = ym; }
              }
          //af.buggrid.setObjectLocation(this, new Int2D(max_x, max_y));
          af.buggrid.moveAgent(new IntPoint(this.x, this.y), new IntPoint(max_x, max_y), this);
          this.x = max_x;
          this.y = max_y;

          if (af.sites.get(new IntPoint(max_x, max_y)) == DAntsForage.FOOD)  // reward me next time!  And change my status
              { reward = af.reward; hasFoodItem = ! hasFoodItem; }
          }
//      System.out.println(this);
      last = new Int2D(this.x,this.y);
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
  
  
  @Override
  public String toString() 
  	{
	return "DAnt [hasFoodItem=" + hasFoodItem + ", reward=" + reward + ", x=" + x + ", y=" + y + ", last=" + last
			+ ", noFoodColor=" + noFoodColor + ", foodColor=" + foodColor + "]";
    }
  }