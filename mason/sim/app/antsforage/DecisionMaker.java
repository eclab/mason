/*
  Copyright 2006 by Sean Luke and George Mason University
  Licensed under the Academic Free License version 3.0
  See the file "LICENSE" for more information
*/

package sim.app.antsforage;

import sim.engine.SimState;

public /*strictfp*/ class DecisionMaker implements java.io.Serializable
    {

    public DecisionInfo[] info = null;
    public int numInfos = 0;

    public DecisionMaker()
        {
        info = new DecisionInfo[8];
        for( int i = 0 ; i < info.length ; i++ )
            info[i] = new DecisionInfo();
        }

    public void reset() { numInfos = 0; }

    public void addInfo( DecisionInfo di )
        {
        info[numInfos].position.x = di.position.x;
        info[numInfos].position.y = di.position.y;
        info[numInfos].orientation = di.orientation;
        info[numInfos].homePheromoneAmount = di.homePheromoneAmount;
        info[numInfos].foodPheromoneAmount = di.foodPheromoneAmount;
        numInfos++;
        }

    public DecisionInfo getHomeDecision( final SimState state )
        {
        for( int i = 0 ; i < numInfos ; i++ )
            {
            processForHomeDecision( info[i] );
            }
        return getDecision( state );
        }

    public DecisionInfo getFoodDecision( final SimState state )
        {
        for( int i = 0 ; i < numInfos ; i++ )
            {
            processForFoodDecision( info[i] );
            }
        return getDecision( state );
        }

    protected void processForHomeDecision( final DecisionInfo info )
        {
        info.profit = info.homePheromoneAmount;
        }

    protected void processForFoodDecision( final DecisionInfo info )
        {
        info.profit = info.foodPheromoneAmount;
        }

    public DecisionInfo getDecision( final SimState state )
        {

        int x, index;

        if( numInfos == 0 )
            {
            return null;
            }

        // first normalize
        double sum=0.0;
        for(x=0;x<numInfos;x++)
            {
            if (info[x].profit<0.0)
                throw new ArithmeticException("Distribution has negative probabilities");
            sum += info[x].profit;
            }
        if (sum==0.0) throw new ArithmeticException("Distribution has all 0 probabilities");
        for(x=0;x<numInfos;x++)
            info[x].profit /= sum;

        // now sum
        sum=0.0;
        for(x=0;x<numInfos;x++)
            {
            sum += info[x].profit;
            info[x].profit = sum;
            }

        // now we need to work backwards setting 0 values
        for(x=numInfos-1; x > 0; x--)
            if (info[x].profit==info[x-1].profit)  // we're 0.0
                info[x].profit = 1.0;
            else
                break; 
        info[x].profit = 1.0;

        //
        // make the decision (pick randomly)
        //
        double prob = state.random.nextDouble();
        if (numInfos==1) // quick 
            return info[0];
        // simple linear scan
        for(x=0;x<numInfos-1;x++)
            if (info[x].profit>prob)
                {
                index = x;
                if (info[index].profit==0.0) // I need to scan forward because I'm in a left-trail
                    while(index < numInfos-1 && info[index].profit==0.0)
                        index++;
                else
                    while(index > 0 && info[index].profit==info[index-1].profit)
                        index--;
                return info[index];
                }
        index = numInfos-1;
        if (info[index].profit==0.0) // I need to scan forward because I'm in a left-trail
            while(index < numInfos-1 && info[index].profit==0.0)
                index++;
        else
            while(index > 0 && info[index].profit==info[index-1].profit)
                index--;
        return info[index];
        }

    public DecisionInfo getHomeGreedyDecision( final SimState state )
        {

        int index;

        if( numInfos == 0 )
            {
            return null;
            }

        for( int i = 0 ; i < numInfos ; i++ )
            {
            processForHomeDecision( info[i] );
            }

        // compute the maximum value
        index = 0;
        for( int i = 0 ; i < numInfos ; i++ )
            if( info[i].profit > info[index].profit )
                index = i;

        int howMany = 0;
        for( int i = 0 ; i < numInfos ; i++ )
            if( info[i].profit == info[index].profit )
                howMany++;

        int x = state.random.nextInt( howMany );
        for( int i = 0 ; i < numInfos ; i++ )
            if( info[i].profit == info[index].profit )
                if( x == 0 )
                    return info[i];
                else
                    x--;
        return null;
    
        }

    public DecisionInfo getFoodGreedyDecision( final SimState state )
        {

        int index;

        if( numInfos == 0 )
            {
            return null;
            }

        for( int i = 0 ; i < numInfos ; i++ )
            {
            processForFoodDecision( info[i] );
            }

        // compute the maximum value
        index = 0;
        for( int i = 0 ; i < numInfos ; i++ )
            if( info[i].profit > info[index].profit )
                index = i;

        int howMany = 0;
        for( int i = 0 ; i < numInfos ; i++ )
            if( info[i].profit == info[index].profit )
                howMany++;

        int x = state.random.nextInt( howMany );
        for( int i = 0 ; i < numInfos ; i++ )
            if( info[i].profit == info[index].profit )
                if( x == 0 )
                    return info[i];
                else
                    x--;
        return null;
    
        }

    }
