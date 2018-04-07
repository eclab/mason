package conflictdiamonds;

import conflictdiamonds.ConflictDiamonds.Goal;
import conflictdiamonds.ConflictDiamonds.Action;

/**
 * After the Intensity Analyzer has determined the action-guiding motive, the Action Sequence executes the action.
 * Activities include finding employment in diamond industry, continue working, stay home, or rebel
 * 
 * This is adapted from Schmidt's (2000) PECS framework
 * 
 * @author bpint
 *
 */
public class ActionSequence {
	
    public static Action runActionSequence( Person me, Goal currentGoal, ConflictDiamonds conflict ) {
		
        ConflictDiamonds.Action currentAction = Action.Do_Nothing;
        
        //if my goal to seek employment in the diamond mines, move closer to diamond mines
        if ( currentGoal == Goal.Find_Employment_As_Miner ) {
            currentAction = Action.Move_Closer_to_Diamond_Mines_Mine;
        }

        //if my goal is to remain employed, then stay home (do nothing)
        else if ( currentGoal == Goal.Remain_Employed ) {
            currentAction = Action.Do_Nothing;
        }
        
        //if my goal is to rebel, then move close to diamond mines
        else if ( currentGoal == Goal.Rebel ) {
            currentAction = Action.Move_Close_to_Diamond_Mines_Rebel;
        }
        
        //otherwise, stay home (do nothing)
        else {
            currentAction = Action.Do_Nothing;
        }

        return currentAction;
    }
	
}
