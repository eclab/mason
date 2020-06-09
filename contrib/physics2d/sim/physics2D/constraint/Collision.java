
package sim.physics2D.constraint;

import sim.util.matrix.*;
import sim.physics2D.physicalObject.MobileObject2D;
import sim.physics2D.physicalObject.PhysicalObject2D;
//import sim.physics2D.util.Double2D;
import sim.util.Double2D;

/**
 * Created when a collision is detected between two objects and 
 * used to solve for the collision impulses.  
 */
public class Collision implements ImpulseConstraint
    {
    public  PhysicalObject2D obj1;
    public PhysicalObject2D obj2;
        
    private Double2D r1;
    private Double2D r2;
        
    private DenseMatrix subCollisionRowsMatrix1;
    private DenseMatrix subCollisionColsMatrix1;
    private DenseMatrix subCollisionRowsMatrix2;
    private DenseMatrix subCollisionColsMatrix2;
    private DenseMatrix subCollisionIntersectionMatrix;
    private sim.util.matrix.Vector subCollisionAnswersVector;
        
    Double2D colNormal;
    double relVel;
    private boolean sticky;
        
    public Collision()
        {
        sticky = false;
                
        subCollisionRowsMatrix1 = new DenseMatrix(2, 3);
        subCollisionColsMatrix1 = new DenseMatrix(3, 2);
        subCollisionRowsMatrix2  = new DenseMatrix(2, 3);
        subCollisionColsMatrix2 = new DenseMatrix(3, 2);
        subCollisionIntersectionMatrix = new DenseMatrix(2, 2);
        subCollisionAnswersVector = new sim.util.matrix.Vector(2);
        }
        
    /** Set this collision to be perfectly inelastic */
    public void setSticky()
        {
        sticky = true;
        }

    /** Returns the number of rows in the collision response matrix */
    public int GetCollisionResponseRows()
        {
        return 2;
        }
        
    public void setColNormal(Double2D colNormal)
        {
        this.colNormal = colNormal;
        }
        
    /** Sets the relative velocity along the collision normal */
    public void setRelVel(double relVel)
        {
        this.relVel = relVel;
        }
        
    public void AddPhysicalObject(PhysicalObject2D mobjRigidBody)
        {
        if (obj1 == null)
            obj1 = mobjRigidBody;
        else
            obj2 = mobjRigidBody;
        }
        
    /** Add a physical object to the collision and specify the vector pointing
     * from the center of the object to the collision point.
     */
    public void AddPhysicalObject(PhysicalObject2D mobjRigidBody, Double2D connectionPoint)
        {
        if (obj1 == null)
            {
            obj1 = mobjRigidBody;
            r1 = connectionPoint;
            }
        else
            {
            obj2 = mobjRigidBody;
            r2 = connectionPoint;
            }
        }
        
    /** Sets the blocks of the global matrices represented by this collision */
    public void setCollisionMatrices(int curConstraintRow, BorderedDiagonalIdentityMatrix collisionMatrix, sim.util.matrix.Vector answerVector)
        {
        int constraintRows = 2;
                
        // Prepare the local constraint state matrix
        setCollisionRowsMatrix();
        setCollisionColsMatrix();
        setCollisionAnswersVector();
        setCollisionIntersectionMatrix();
                
        // Answer and intersection matrices have all zeros, so do not need to be
        // set
        int globalPosStart1 = obj1.index * 3;
        collisionMatrix.setBlock(curConstraintRow, globalPosStart1, subCollisionRowsMatrix1.vals);
        collisionMatrix.setBlock(globalPosStart1, curConstraintRow, subCollisionColsMatrix1.vals);
                
        int globalPosStart2 = obj2.index * 3;
        collisionMatrix.setBlock(curConstraintRow, globalPosStart2, subCollisionRowsMatrix2.vals);
        collisionMatrix.setBlock(globalPosStart2, curConstraintRow, subCollisionColsMatrix2.vals);
                
        collisionMatrix.setBlock(curConstraintRow, curConstraintRow, subCollisionIntersectionMatrix.vals);
        answerVector.vals[curConstraintRow] = subCollisionAnswersVector.vals[0];
        answerVector.vals[curConstraintRow + 1] = subCollisionAnswersVector.vals[1];
        }
        
    private void setCollisionRowsMatrix()
        {
        Double2D r1 = this.r1;
        Double2D r2 = this.r2;
                
        double colPoint1PDot = r1.perpDot(colNormal);
        double colPoint2PDot = r2.perpDot(colNormal);
                
        subCollisionRowsMatrix1.vals[0][0] = colNormal.x;
        subCollisionRowsMatrix1.vals[0][1] = colNormal.y;
        subCollisionRowsMatrix1.vals[0][2] = colPoint1PDot;
        subCollisionRowsMatrix1.vals[1][0] = 0;
        subCollisionRowsMatrix1.vals[1][1] = 0;
        subCollisionRowsMatrix1.vals[1][2] = 0;
                
        subCollisionRowsMatrix2.vals[0][0] = -colNormal.x;
        subCollisionRowsMatrix2.vals[0][1] = -colNormal.y;
        subCollisionRowsMatrix2.vals[0][2] = -colPoint2PDot;
        subCollisionRowsMatrix2.vals[1][0] = 0;
        subCollisionRowsMatrix2.vals[1][1] = 0;
        subCollisionRowsMatrix2.vals[1][2] = 0;
        }
        
    private void setCollisionColsMatrix()
        {
        PhysicalObject2D mobj1 = this.obj1;
        PhysicalObject2D mobj2 = this.obj2;
                
        Double2D r1 = this.r1;
        Double2D r2 = this.r2;
                
        subCollisionColsMatrix1.vals[0][0] = -mobj1.getMassInverse();
        subCollisionColsMatrix1.vals[0][1] = 0;
        subCollisionColsMatrix1.vals[1][0] = 0;
        subCollisionColsMatrix1.vals[1][1] = -mobj1.getMassInverse();
        subCollisionColsMatrix1.vals[2][0] = mobj1.getMassMomentOfInertiaInverse() * r1.y;
        subCollisionColsMatrix1.vals[2][1] = -mobj1.getMassMomentOfInertiaInverse() * r1.x;
                
        subCollisionColsMatrix2.vals[0][0] = mobj2.getMassInverse();
        subCollisionColsMatrix2.vals[0][1] = 0;
        subCollisionColsMatrix2.vals[1][0] = 0;
        subCollisionColsMatrix2.vals[1][1] = mobj2.getMassInverse();
        subCollisionColsMatrix2.vals[2][0] = -mobj2.getMassMomentOfInertiaInverse() * r2.y;
        subCollisionColsMatrix2.vals[2][1] = mobj2.getMassMomentOfInertiaInverse() * r2.x;
        }
        
    private void setCollisionIntersectionMatrix()
        {
        subCollisionIntersectionMatrix.vals[0][0] = 0;
        subCollisionIntersectionMatrix.vals[0][1] = 0;
        subCollisionIntersectionMatrix.vals[1][0] = -colNormal.y;
        subCollisionIntersectionMatrix.vals[1][1] = colNormal.x;
        }
        
    private void setCollisionAnswersVector()
        {
        PhysicalObject2D mobj1 = this.obj1;
        PhysicalObject2D mobj2 = this.obj2;
                
        //relVel = colNormal.multiply(relVel.dotProduct(colNormal));
        // Make the combined coefficient of restitution the product of the two
        // CFs. If sticky is set, make the coefficient 0 to reduce the relative
        // velocity of the two bodies to 0 (for resting contact)
        double CF;
                
        if (this.sticky)
            CF = 0;
        else
            CF = mobj1.getCoefficientOfRestitution() * mobj2.getCoefficientOfRestitution();
                
        subCollisionAnswersVector.vals[0] = -relVel * CF;
        subCollisionAnswersVector.vals[1] = 0;
        }
        
    /** Applies the calculated impulses to the objects involved in the collision */ 
    public void applyImpulses(int curAnswerRow, sim.util.matrix.Vector answers)
        {
        Double2D R = new Double2D(answers.vals[curAnswerRow], answers.vals[curAnswerRow + 1]);
                
        if (obj1 instanceof MobileObject2D)
            {
            MobileObject2D mobj1 = (MobileObject2D)obj1;
            mobj1.setVelocity(mobj1.getVelocity().add(R.multiply(mobj1.getMassInverse())));
            mobj1.setAngularVelocity(mobj1.getAngularVelocity() + r1.perpDot(R.multiply(mobj1.getMassMomentOfInertiaInverse())));
            }
        if (obj2 instanceof MobileObject2D)
            {                               
            MobileObject2D mobj2 = (MobileObject2D)obj2;
            mobj2.setVelocity(mobj2.getVelocity().subtract(R.multiply(mobj2.getMassInverse())));
            mobj2.setAngularVelocity(mobj2.getAngularVelocity() - r2.perpDot(R.multiply(mobj2.getMassMomentOfInertiaInverse())));
            }
        }
    }
