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

public /*strictfp*/ class Ant extends OvalPortrayal2D implements Steppable
    {

    public static final int ADD_PHEROMONE = 0;
    public static final int MAX_PHEROMONE = 1;
    public static final int LOCAL_PHEROMONE = 2;

    public static final int PHEROMONE_TYPE = LOCAL_PHEROMONE;

    public static final boolean TOROIDAL_WORLD = false;

    // type of Ant
    public static final int ORIENTED_ANT = 0;
    public static final int NSEW_ANT = 1;
    public static final int EIGHT_NEIGHBOURS_ANT = 2;

    public static final int ANT_TYPE = ORIENTED_ANT;

    public static final boolean GREEDY_REPOSITIONING = true;

    public static final boolean GREEDY_EXPLORATION = true;

    public static final int N = 0;
    public static final int NE = 1;
    public static final int E = 2;
    public static final int SE = 3;
    public static final int S = 4;
    public static final int SW = 5;
    public static final int W = 6;
    public static final int NW = 7;

    public double pheromoneToLeaveBehind;
    public double minPheromone;
    public double maxPheromone;
    public int timeToLive;

    double subtractingRatio;
    double pheromoneRatio;

    int orientation;

    public boolean getHasFoodItem() { return hasFoodItem; }
    public void setHasFoodItem(boolean val) { hasFoodItem = val; }
    public boolean hasFoodItem;

    public static final double ANT_K = 0.001;
    public static final double ANT_N = 10.0;

    boolean justCreated;

    public Ant( int orientation,
        double pheromoneToLeaveBehind,
        double minPheromone,
        double maxPheromone,
        int timeToLive ) 
        {
        this.orientation = orientation;
        this.pheromoneToLeaveBehind = pheromoneToLeaveBehind;
        this.minPheromone = minPheromone;
        this.maxPheromone = maxPheromone;
        this.timeToLive = timeToLive;

        subtractingRatio = ( 1.0 / timeToLive ) * maxPheromone;
        pheromoneRatio = 1.0 * maxPheromone;

        hasFoodItem = false;
        justCreated = true;
        }
        
    protected void addInformation( final SimState state, int x, int y, final int orientation )
        {
        final AntsForage af = (AntsForage)state;
        final DecisionInfo di = af.decisionInfo;
        final DecisionMaker decisionMaker = af.decisionMaker;

        if( TOROIDAL_WORLD )
            {
            x = (x+AntsForage.GRID_WIDTH)%AntsForage.GRID_WIDTH;
            y = (y+AntsForage.GRID_HEIGHT)%AntsForage.GRID_HEIGHT;
            }
        else
            {
            if( x < 0 || x >= AntsForage.GRID_WIDTH || y < 0 || y >= AntsForage.GRID_HEIGHT )
                return;
            }
        if( ( af.buggrid.getObjectsAtLocation(x,y) == null ||
                af.buggrid.getObjectsAtLocation(x,y).numObjs < AntsForage.MAX_ANTS_PER_LOCATION ) &&
            af.obstacles.field[x][y] <= 0.5 )
            {
            // toroidal coordinates!
            di.position.x = x;
            di.position.y = y;
            di.orientation = orientation;

//            di.homePheromoneAmount = /*Strict*/Math.pow( ANT_K + ((AntsForage)state).toHomeGrid.field[di.position.x][di.position.y], ANT_N) ;
//            di.foodPheromoneAmount = /*Strict*/Math.pow( ANT_K + ((AntsForage)state).toFoodGrid.field[di.position.x][di.position.y], ANT_N );;
            di.homePheromoneAmount = 0.001 + af.toHomeGrid.field[di.position.x][di.position.y];
            di.foodPheromoneAmount = 0.001 + af.toFoodGrid.field[di.position.x][di.position.y];
            decisionMaker.addInfo( di );
            }
        }

    public DecisionInfo decideAction( final SimState state, final int myx, final int myy, final int orientation )
        {

        final AntsForage af = (AntsForage)state;
        final DecisionMaker decisionMaker = af.decisionMaker;

        decisionMaker.reset();

        // collect the sensory information for the new grid model
        // this should be done separately in the model, but whatever....

        switch( ANT_TYPE )
            {
            case ORIENTED_ANT:
                switch( orientation )
                    {
                    case 0: addInformation( state, myx-1, myy+1, (orientation+7)%8 );  // forward-left
                        addInformation( state, myx,   myy+1, orientation );        // forward
                        addInformation( state, myx+1, myy+1, (orientation+1)%8 );  // forward-right
                        break;
                    case 1: addInformation( state, myx,   myy+1, (orientation+7)%8 );  // forward-left
                        addInformation( state, myx+1, myy+1, orientation );        // forward
                        addInformation( state, myx+1, myy,   (orientation+1)%8 );  // forward-right
                        break;
                    case 2: addInformation( state, myx+1, myy+1, (orientation+7)%8 );  // forward-left
                        addInformation( state, myx+1, myy,   orientation );        // forward
                        addInformation( state, myx+1, myy-1, (orientation+1)%8 );  // forward-right
                        break;
                    case 3: addInformation( state, myx+1, myy,   (orientation+7)%8 );  // forward-left
                        addInformation( state, myx+1, myy-1, orientation );        // forward
                        addInformation( state, myx,   myy-1, (orientation+1)%8 );  // forward-right
                        break;
                    case 4: addInformation( state, myx+1, myy-1, (orientation+7)%8 );  // forward-left
                        addInformation( state, myx,   myy-1, orientation );        // forward
                        addInformation( state, myx-1, myy-1, (orientation+1)%8 );  // forward-right
                        break;
                    case 5: addInformation( state, myx,   myy-1, (orientation+7)%8 );  // forward-left
                        addInformation( state, myx-1, myy-1, orientation );        // forward
                        addInformation( state, myx-1, myy,   (orientation+1)%8 );  // forward-right
                        break;
                    case 6: addInformation( state, myx-1, myy-1, (orientation+7)%8 );  // forward-left
                        addInformation( state, myx-1, myy,   orientation );        // forward
                        addInformation( state, myx-1, myy+1, (orientation+1)%8 );  // forward-right
                        break;
                    case 7: addInformation( state, myx-1, myy,   (orientation+7)%8 );  // forward-left
                        addInformation( state, myx-1, myy+1, orientation );        // forward
                        addInformation( state, myx,   myy+1, (orientation+1)%8 );  // forward-right
                        break;
                    }
                break;
            case NSEW_ANT:
                addInformation( state, myx-1, myy,   (orientation+7)%8 ); // N
                addInformation( state, myx+1, myy,   (orientation+7)%8 ); // S
                addInformation( state, myx, myy-1,   (orientation+7)%8 ); // E
                addInformation( state, myx, myy+1,   (orientation+7)%8 ); // W
                break;
            case EIGHT_NEIGHBOURS_ANT:
                addInformation( state, myx-1, myy-1,   (orientation+7)%8 ); // N
                addInformation( state, myx-1, myy,   (orientation+7)%8 ); // N
                addInformation( state, myx-1, myy+1,   (orientation+7)%8 ); // N
                addInformation( state, myx+1, myy-1,   (orientation+7)%8 ); // S
                addInformation( state, myx+1, myy,   (orientation+7)%8 ); // S
                addInformation( state, myx+1, myy+1,   (orientation+7)%8 ); // S
                addInformation( state, myx-1, myy,   (orientation+7)%8 ); // E
                addInformation( state, myx+1, myy,   (orientation+7)%8 ); // W
                break;
            }

        if( hasFoodItem )
            return decisionMaker.getHomeGreedyDecision( state );
        else
            {
            if( GREEDY_EXPLORATION )
                return decisionMaker.getFoodGreedyDecision( state );
            else
                return decisionMaker.getFoodDecision( state );
            }
        }

    public void addPheromone(DoubleGrid2D grid, int x, int y, double pheromone)
        {
        switch( PHEROMONE_TYPE )
            {
            case ADD_PHEROMONE:
                grid.field[x][y] += /*Strict*/Math.abs(pheromoneToLeaveBehind);
                if (grid.field[x][y] > maxPheromone)
                    grid.field[x][y] = maxPheromone;
                break;
            case MAX_PHEROMONE:
                grid.field[x][y] = /*Strict*/Math.max( grid.field[x][y], pheromone );
                break;
            case LOCAL_PHEROMONE:
                double amount = /*Strict*/Math.max( grid.field[x][y], pheromone );
                if( x > 0 && y > 0 )
                    amount = /*Strict*/Math.max( /*Strict*/Math.max( grid.field[x-1][y-1]-subtractingRatio, minPheromone ), amount );
                if( x > 0)
                    amount = /*Strict*/Math.max( /*Strict*/Math.max( grid.field[x-1][y]-subtractingRatio, minPheromone ), amount );
                if( x > 0 && y < grid.field[x].length-1 )
                    amount = /*Strict*/Math.max( /*Strict*/Math.max( grid.field[x-1][y+1]-subtractingRatio, minPheromone ), amount );
                if( y > 0 )
                    amount = /*Strict*/Math.max( /*Strict*/Math.max( grid.field[x][y-1]-subtractingRatio, minPheromone ), amount );
                if( y < grid.field.length-1 )
                    amount = /*Strict*/Math.max( /*Strict*/Math.max( grid.field[x][y+1]-subtractingRatio, minPheromone ), amount );
                if( x < grid.field.length-1 && y > 0 )
                    amount = /*Strict*/Math.max( /*Strict*/Math.max( grid.field[x+1][y-1]-subtractingRatio, minPheromone ), amount );
                if( x < grid.field.length-1 )
                    amount = /*Strict*/Math.max( /*Strict*/Math.max( grid.field[x+1][y]-subtractingRatio, minPheromone ), amount );
                if( x < grid.field.length-1 && y < grid.field[x].length-1 )
                    amount = /*Strict*/Math.max( /*Strict*/Math.max( grid.field[x+1][y+1]-subtractingRatio, minPheromone ), amount );
                grid.field[x][y] = amount;
                pheromoneRatio = amount;
                break;
            }
        }

    public DecisionInfo decideGreedyAction( final SimState state, final int myx, final int myy, final int orientation )
        {

        final AntsForage af = (AntsForage)state;
        final DecisionMaker decisionMaker = af.decisionMaker;

        decisionMaker.reset();

        // add all neighboring cells and move to one of them
        addInformation( state, myx,   myy+1, 0 );
        addInformation( state, myx+1, myy+1, 1 );
        addInformation( state, myx+1, myy,   2 );
        addInformation( state, myx+1, myy-1, 3 );
        addInformation( state, myx,   myy-1, 4 );
        addInformation( state, myx-1, myy-1, 5 );
        addInformation( state, myx-1, myy,   6 );
        addInformation( state, myx-1, myy+1, 7 );

        if( hasFoodItem )
            return decisionMaker.getHomeGreedyDecision( state );
        else
            return decisionMaker.getFoodGreedyDecision( state );

        }

    public void step( final SimState state )
        {
        final AntsForage af = (AntsForage)state;
        final DecisionMaker decisionMaker = af.decisionMaker;
        
        Int2D location = af.buggrid.getObjectLocation(this);
        int myx = location.x;
        int myy = location.y;

        if( justCreated )
            {
            DecisionInfo temp = decideGreedyAction( state, myx, myy, orientation );
            if( temp == null )
                return;
            orientation = temp.orientation;
            justCreated = false;
            }

        // final int START=-1;
        int bestx, besty, besto;

        DecisionInfo movingDecision = null;
        if( hasFoodItem )
            movingDecision = decideGreedyAction( state, myx, myy, orientation );
        else
            {
            decisionMaker.reset();
            addInformation( state, myx,   myy+1, 0 );
            addInformation( state, myx+1, myy+1, 1 );
            addInformation( state, myx+1, myy,   2 );
            addInformation( state, myx+1, myy-1, 3 );
            addInformation( state, myx,   myy-1, 4 );
            addInformation( state, myx-1, myy-1, 5 );
            addInformation( state, myx-1, myy,   6 );
            addInformation( state, myx-1, myy+1, 7 );
            int max = 0;
            int howMany = 1;
            for( int i = 1 ; i < decisionMaker.numInfos ; i++ )
                if( decisionMaker.info[max].foodPheromoneAmount ==
                    decisionMaker.info[i].foodPheromoneAmount )
                    {
                    howMany++;
                    }
                else if( decisionMaker.info[max].foodPheromoneAmount <
                    decisionMaker.info[i].foodPheromoneAmount )
                    {
                    max = i;
                    howMany = 1;
                    }
            if( howMany == 1 )
                movingDecision = decideGreedyAction( state, myx, myy, orientation );
            else
                movingDecision = decideAction( state, myx, myy, orientation );
            }
        if( movingDecision == null )
            {
            movingDecision = decideGreedyAction( state, myx, myy, orientation );
            if( movingDecision == null )
                {
                bestx = myx;
                besty = myy;
                besto = orientation;
                }
            else
                {
                bestx = movingDecision.position.x;
                besty = movingDecision.position.y;
                besto = movingDecision.orientation;
                }
            }
        else
            {
            bestx = movingDecision.position.x;
            besty = movingDecision.position.y;
            besto = movingDecision.orientation;
            }

        if( ( bestx != myx || besty != myy ))
            {
            // add some pheromones
            if( hasFoodItem )
                {
                addPheromone(af.toFoodGrid,myx,myy,(/*pheromoneToLeaveBehind*/pheromoneRatio));
                }
            else
                {
                addPheromone(af.toHomeGrid,myx,myy,(/*pheromoneToLeaveBehind*/pheromoneRatio));
                }
            if( bestx != myx && besty != myy )
                pheromoneRatio -= subtractingRatio*1.4142;
            else
                pheromoneRatio -= subtractingRatio;
            if( pheromoneRatio < 0 )
                {
                die( state );
                return;
                }


            // adjust the position of the agent, and then deposit the "to food" and "to home" pheromones
            af.buggrid.setObjectLocation(this,bestx,besty);
            orientation = besto;
            if( ( besty >= AntsForage.HOME_YMIN ) && ( besty <= AntsForage.HOME_YMAX ) &&
                ( bestx >= AntsForage.HOME_XMIN ) && ( bestx <= AntsForage.HOME_XMAX ) )
                {
                if( hasFoodItem )
                    {
                    af.foodCollected++;
                    hasFoodItem = false;
                    pheromoneRatio = 1.0 * maxPheromone;
                    if( GREEDY_REPOSITIONING )
                        {
                        // pick greediest orientation!
                        DecisionInfo temp = decideGreedyAction( state, myx, myy, orientation );
                        if( temp != null )
                            orientation = temp.orientation;
                        else
                            orientation = (orientation+4)%8;
                        }
                    else
                        orientation = (orientation+4)%8; // rotate 180
                    }
                }
            else if(  ( besty >= AntsForage.FOOD_YMIN ) && ( besty <= AntsForage.FOOD_YMAX ) &&
                ( bestx >= AntsForage.FOOD_XMIN ) && ( bestx <= AntsForage.FOOD_XMAX ) )
                {
                if( !hasFoodItem )
                    {
                    hasFoodItem = true;
                    pheromoneRatio = 1.0 * maxPheromone;
                    if( GREEDY_REPOSITIONING )
                        {
                        // pick greediest orientation!
                        DecisionInfo temp = decideGreedyAction( state, myx, myy, orientation );
                        if( temp != null )
                            orientation = temp.orientation;
                        else
                            orientation = (orientation+4)%8;
                        }
                    else
                        orientation = (orientation+4)%8; // rotate 180
                    }
                }

            timeToLive--;
            if( timeToLive <= 0 )
                {
                die( state );
                return;
                }
            }
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
    
    public Stoppable toDiePointer = null;
    public void die( final SimState state )
        {
        AntsForage antsforage = (AntsForage)state;
        antsforage.numberOfAnts--;
        antsforage.buggrid.remove( this );
        if(toDiePointer!=null) toDiePointer.stop();
        }

    }
