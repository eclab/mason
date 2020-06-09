package sim.physics2D.constraint;

import sim.util.matrix.*;

/** Represents a constraint on objects' accelerations. Force constraints assume
 * legal positions and velocities and solve for legal accelerations.
 */
public interface ForceConstraint
    {
    public int GetConstraintRows();
    public void setConstraintMatrices(int curConstraintRow, BlockSparseMatrix jacobianMatrix, BlockSparseMatrix jacobianDotMatrix, Vector constraintVector, Vector constraintDotVector);
    void addHolonomicConstraints();
    }
