package sim.physics2D.integrator;

import sim.physics2D.*;
import sim.physics2D.constraint.ConstraintEngine;
import sim.physics2D.forceGenerator.ForceEngine;

/** Implements a Runge-Kutta ordinary differential equation solver. The runge-kutta solver
 * reduces errors over the euler integrator by adding terms in the taylor series expansion. 
 */
public class ODERungeKuttaSolver implements ODESolver
    {
    private ConstraintEngine objCE;
    private ForceEngine objFE;
        
    private PhysicsState physicsState;
        
    public ODERungeKuttaSolver()
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
                
        // this is for resting contact
        /*
          if (physicsState.lcp.contacts.numObjs == 0)
          {
          matrix.Vector constraintForces = objPE.calculateConstraintForces(externalForcesMatrix);                 
          matrix.Vector constraintForces = objPE.calculateConstraintForces(externalForcesVector);
                        
          // Solve for total forces and accelerations
          totalForces = constraintForces.plus(externalForcesVector);
          }
          else
          totalForces = externalForcesVector;
        */
                
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
                
        sim.util.matrix.Vector f1 = ODEFunction(stepState).times(stepSize);
        sim.util.matrix.Vector f2 = ODEFunction(stepState.plus(f1.times(0.5))).times(stepSize);
        sim.util.matrix.Vector f3 = ODEFunction(stepState.plus(f2.times(0.5))).times(stepSize);
        sim.util.matrix.Vector f4 = ODEFunction(stepState.plus(f3)).times(stepSize);
                
        // Update the step to be the new state at the end of this step
        physicsState.setStateVector(stepState.plus(f1.plus(f2.times(2)).plus(f3.times(2)).plus(f4).times((double)1/6)));
        }
    }
