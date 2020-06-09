package sim.physics2D.integrator;

import sim.physics2D.*;
import sim.physics2D.constraint.ConstraintEngine;
import sim.physics2D.forceGenerator.ForceEngine;

/** Implements an Euler ordinary differential equation solver. The Euler integrator
 * solves with these equations: 
 * 
 * x = x + x';
 * x' = x' + x''  
 */
public class ODEEulerSolver implements ODESolver
    {
    private ConstraintEngine objCE;
    private ForceEngine objFE;
        
    private PhysicsState physicsState;
        
    public ODEEulerSolver()
        {
        physicsState = PhysicsState.getInstance();
        this.objCE = ConstraintEngine.getInstance();
        this.objFE = ForceEngine.getInstance();
        }

    private sim.util.matrix.Vector ODEFunction(sim.util.matrix.Vector state)
        {
        int stateRowDimension = state.m;
        int halfStateRowDimension = stateRowDimension / 2;
                
        // Update the physics engine so it thinks this is the current state
        physicsState.setStateVector(state);
        objFE.addForces();
        sim.util.matrix.Vector externalForcesVector = physicsState.getExternalForcesVector();
        sim.util.matrix.Vector totalForces;
                
        sim.util.matrix.Vector constraintForces = objCE.calculateConstraintForces(externalForcesVector);
        totalForces = constraintForces.plus(externalForcesVector);
        sim.util.matrix.Vector acc = physicsState.getMassInverseMatrix().times(totalForces);
                
        // Set the first half of the stateDot matrix to velocity
        // (second half of state matrix) and the second half of the
        // stateDot matrix to the accelerations just calculated
        sim.util.matrix.Vector stateDot = new sim.util.matrix.Vector(stateRowDimension);
        for (int i = 0; i < halfStateRowDimension; i++)
            {
            stateDot.vals[i] = state.vals[i + halfStateRowDimension];
            stateDot.vals[i + halfStateRowDimension] = acc.vals[i];
            }
                
        return stateDot;
        }
        
    public void solve(double stepSize)
        {
        // Make a copy of the state at the start of the current step
        sim.util.matrix.Vector stepState = physicsState.getStateVectorCopy();
        sim.util.matrix.Vector stateDot = ODEFunction(stepState).times(stepSize);
                
        // Update the step to be the new state at the end of this step
        physicsState.setStateVector(stepState.plus(stateDot));
        }
    }
