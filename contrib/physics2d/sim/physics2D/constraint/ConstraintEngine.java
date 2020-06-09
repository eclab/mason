package sim.physics2D.constraint;

import java.util.HashSet;
import sim.util.matrix.BlockSparseMatrix;
import sim.util.matrix.*;
import sim.physics2D.PhysicsState;
import sim.physics2D.physicalObject.*;
import sim.physics2D.collisionDetection.*;
import sim.physics2D.util.PhysicalObjectPair;
import sim.util.Bag;
import sim.util.Double2D;

/** The ConstraintEngine solves for constraint forces and impulses */
public class ConstraintEngine 
    {
    // Force constraint vectors
    private sim.util.matrix.Vector constraintVector;
    private sim.util.matrix.Vector constraintDotVector;
    private sim.util.matrix.BlockSparseMatrix jacobianMatrix;
    private sim.util.matrix.BlockSparseMatrix jacobianDotMatrix;
    private sim.util.matrix.Vector qDotVector;
        
    // Impulse constraint vectors
    private BorderedDiagonalIdentityMatrix collisionResponseMatrix;
    private sim.util.matrix.Vector collisionResponseAnswersVector;
    private int constraintRows;
    private int collisionRows;
    private int collisionResponseRows;
        
    private HashSet noCollisions;
        
    private Bag constraints;
    private Bag collisions;
        
    private static int debugCounter = 0;
        
    private PhysicsState physicsState = null;
        
    private final static double ZERO_VELOCITY = 0.000001;
        
    private static ConstraintEngine instance = null;
        
    public static ConstraintEngine getInstance()
        {
        if (instance == null)
            instance = new ConstraintEngine();
        return instance;
        }
        
    public static ConstraintEngine reset()
        {
        instance = new ConstraintEngine();
        return instance;
        }
        
    private ConstraintEngine()
        {
        physicsState = PhysicsState.getInstance();
        constraintRows = 0;
        constraints = new Bag();
                
        collisionRows = 0;
        collisions = new Bag();
                
        noCollisions = new HashSet();
        }

    /** Turns off collisions for a pair of objects
     */
    public void setNoCollisions(PhysicalObject2D c1, PhysicalObject2D c2)
        {
        PhysicalObjectPair pair = new PhysicalObjectPair(c1, c2);
        noCollisions.add(pair);
        }
        
    /** Turns collisions for a pair of objects back on
     */
    public void removeNoCollisions(PhysicalObject2D c1, PhysicalObject2D c2)
        {
        PhysicalObjectPair pair = new PhysicalObjectPair(c1, c2);
        noCollisions.remove(pair);
        }
        
    /** Tests whether collisions between a pair of objects is currently turned off 
     */
    public boolean testNoCollisions(PhysicalObject2D c1, PhysicalObject2D c2)
        {
        PhysicalObjectPair pair = new PhysicalObjectPair(c1, c2);
        return noCollisions.contains(pair);
        }
        
    /** Registers a force constraint with the constraint engine
     */
    public void registerForceConstraint(ForceConstraint constraint)
        {
        constraintRows += constraint.GetConstraintRows();
        constraints.add(constraint);
        // set up the resting contact constraint
        constraint.addHolonomicConstraints();
        }
        
    /** Registers an impulse constraint with the constraint engine */
    public void registerImpulseConstraint(ImpulseConstraint collision)
        {
        collisionResponseRows += collision.GetCollisionResponseRows();
        collisions.add(collision);
        }
        
    /** Un-registers a force constraint with the constraint engine */
    // TODO - need to remove holonomic constraints
    public void unRegisterForceConstraint(ForceConstraint con)
        {
        constraints.remove(con);
        constraintRows -= con.GetConstraintRows();
        }
        
    /** Un-registers an impulse constraint with the constraint engine */
    public void unRegisterImpulseConstraint(ImpulseConstraint con)
        {
        collisionResponseRows -= ((ImpulseConstraint)con).GetCollisionResponseRows();
        collisions.remove(con);
        }
        
    /** Calculates the constraint forces based on the constraints and external forces
     * currently in the system
     */
    public sim.util.matrix.Vector calculateConstraintForces(sim.util.matrix.Vector externalForcesVector)
        {
        setMatrices();
                
        double ks = .3;
        double kd = .3;

        sim.util.matrix.DiagonalMatrix W = physicsState.getMassInverseMatrix();
        sim.util.matrix.Vector feedback = constraintVector.times(ks).plus(constraintDotVector.times(kd));
        sim.util.matrix.Vector b = jacobianDotMatrix.times(qDotVector.times(-1)).minus(jacobianMatrix.times(W.times(externalForcesVector))).minus(feedback);
                        
        sim.util.matrix.Vector lambda = new sim.util.matrix.Vector(b.m);
        sim.util.matrix.DiagonalMatrix A_t = new sim.util.matrix.DiagonalMatrix(b.m);
        for (int i = 0; i < b.m; i++)
            A_t.vals[i] = 1;
                
        lambda = sim.util.matrix.BlockSparseMatrix.solveBiConjugateGradient(jacobianMatrix, W, A_t, b, lambda, W.m * 2, 1E-10);
                
        sim.util.matrix.Vector Qhat = jacobianMatrix.transposeTimes(lambda);
        return Qhat;
        }
        
    /** Solves for and adds collision responses to the colliding objects */
    public void addCollisionResponses(Bag collidingList)
        {
        //physicsState.lcp.contacts.clear();
        for (int i = 0; i < collidingList.numObjs; i++)
            {
            CollisionPair pair = (CollisionPair)collidingList.objs[i];
                
            Collision col = new Collision();
            PhysicalObject2D collidePoly1 = (PhysicalObject2D)pair.c1;
            PhysicalObject2D collidePoly2 = (PhysicalObject2D)pair.c2;
                        
            col.AddPhysicalObject(collidePoly1, pair.getColPoint1());
            col.AddPhysicalObject(collidePoly2, pair.getColPoint2());
            col.setColNormal(pair.getNormal());
            col.setRelVel(pair.getRelativeVelocity());
                        
            if (pair.getSticky())
                col.setSticky();
                        
            //boolean sticky = false;
            //if (pair.relVel.dotProduct(pair.normal) > -STICKY_THRESHOLD)
            //{
            //      sticky = true;
            //      col.setSticky();
            //}
                        
            this.registerImpulseConstraint(col);
            this.setCollisionMatrices();
                
            sim.util.matrix.Vector answerCT = new sim.util.matrix.Vector(collisionResponseAnswersVector.m);
                        
            try
                {
                // First try with the id matrix as the preconditioner
                answerCT = BorderedDiagonalIdentityMatrix.solveBiConjugateGradient(collisionResponseMatrix, collisionResponseAnswersVector, answerCT, collisionResponseMatrix.m * 2, 1E-5, false);
                }
            catch(Exception e)
                {
                try
                    {
                    // If that fails, try again with ILU decomp
                    answerCT = BorderedDiagonalIdentityMatrix.solveBiConjugateGradient(collisionResponseMatrix, collisionResponseAnswersVector, answerCT, collisionResponseMatrix.m * 2, 1E-5, true);
                    }
                                
                catch(Exception e2)
                    {
                    // In the worst case, solve it using dense matrices
                    answerCT = new Vector(collisionResponseMatrix.getDenseMatrix().solve(collisionResponseAnswersVector.getDenseMatrix()));
                    }
                }
                        
            addCalculatedResponses(answerCT);
            this.unRegisterImpulseConstraint(col);
                        
            // Add the pair to the resting list if they are stuck. Otherwise,
            // (separating) clear the features
            double relVelNorm = pair.getRelativeVelocity(); 
            if (relVelNorm > -ZERO_VELOCITY && relVelNorm < ZERO_VELOCITY)
                {
                //physicsState.lcp.addContact(pair.c1, pair.c2, pair.normal, new Double2D(pair.relVel.x, pair.relVel.y), pair.getColPoint1(), pair.colPoint2);
                }
                        
            // Clear this pair's features
            pair.clear();
                        
            }
        }
        
    private void setMatrices()
        {
        sim.util.matrix.Vector stateVector = physicsState.getStateVector();
        int DOF = stateVector.m / 2;
                
        int conRows = this.constraintRows;
                
        constraintVector = new sim.util.matrix.Vector(conRows);
        constraintDotVector = new sim.util.matrix.Vector(conRows);
        jacobianMatrix = new BlockSparseMatrix(conRows, DOF);
        jacobianDotMatrix = new BlockSparseMatrix(conRows, DOF);
                
        qDotVector = new sim.util.matrix.Vector(DOF);
        for (int i = 0; i < DOF; i++)
            qDotVector.vals[i] = stateVector.vals[i + DOF];
                
        int curConstraintRow = 0;
                
        // Fill in matrices based on the individual constraint matrixes
        Bag constraints = this.constraints;
        for (int i = 0; i < constraints.numObjs; i++)
            {       
            ForceConstraint con = (ForceConstraint)constraints.objs[i];
            con.setConstraintMatrices(curConstraintRow, jacobianMatrix, jacobianDotMatrix, constraintVector, constraintDotVector);
            curConstraintRow += con.GetConstraintRows();
            }
        }
        
    private void setCollisionMatrices()
        {
        sim.util.matrix.Vector stateVector = physicsState.getStateVector();
        int DOF = stateVector.m / 2;
        int colResponseMatrixSize = DOF + this.collisionResponseRows;
                
        collisionResponseMatrix = new BorderedDiagonalIdentityMatrix(colResponseMatrixSize, colResponseMatrixSize - DOF);
        collisionResponseAnswersVector = new sim.util.matrix.Vector(colResponseMatrixSize);
                
        // The first half of the answers vector should be the velocities of the objects
        for (int i = 0; i < DOF; i++)
            collisionResponseAnswersVector.vals[i] = stateVector.vals[i + DOF];
                
        int curCollisionResponseRow = DOF;
                
        // Fill in matrices based on the individual constraint matrixes
        Bag collisions = this.collisions;
        for (int i = 0; i < collisions.numObjs; i++)
            {
            ImpulseConstraint col = (ImpulseConstraint)collisions.objs[i];
            col.setCollisionMatrices(curCollisionResponseRow, collisionResponseMatrix, collisionResponseAnswersVector);
            curCollisionResponseRow += col.GetCollisionResponseRows();
            }
        }
        
    private void addCalculatedResponses(sim.util.matrix.Vector answers)
        {
        sim.util.matrix.Vector stateVector = physicsState.getStateVector();
                
        int DOF = stateVector.m / 2;
        int colResponseMatrixSize = DOF + this.collisionResponseRows;
                
        int curAnswerRow = DOF;
                
        Bag collisions = this.collisions;
        for (int i = 0; i < collisions.numObjs; i++)
            {
            ImpulseConstraint col = (ImpulseConstraint)collisions.objs[i];
            col.applyImpulses(curAnswerRow, answers);
            curAnswerRow += 2;
            }
        }
    }
