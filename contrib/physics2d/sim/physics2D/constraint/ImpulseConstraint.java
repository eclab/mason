
package sim.physics2D.constraint;

import sim.util.matrix.BorderedDiagonalIdentityMatrix;

/** Represents a constraint on objects' velocities. Impulse constraints
 * are used to solve for legal velocities after the impulses are applied to 
 * the objects.
 */
public interface ImpulseConstraint 
    {
    public int GetCollisionResponseRows();
    public void setCollisionMatrices(int curConstraintRow, BorderedDiagonalIdentityMatrix collisionMatrix, sim.util.matrix.Vector answerVector);
    public void applyImpulses(int curAnswerRow, sim.util.matrix.Vector answers);
    }
