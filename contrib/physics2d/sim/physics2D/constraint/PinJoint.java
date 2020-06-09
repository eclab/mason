/*
 * Created on Oct 14, 2004
 *
 */
package sim.physics2D.constraint;

import sim.physics2D.physicalObject.MobileObject2D;
import sim.physics2D.physicalObject.PhysicalObject2D;
//import sim.physics2D.util.Double2D;
import sim.util.matrix.*;
import sim.util.Double2D;

/** A PinJoint represents a point where two objects can not move relative to
 * each other. A door hinge is an example of a pin joint - the door and the frame
 * can not move relative to each other at the point where they are joined.
 * 
 * PinJoint implements both ForceConstraint and ImpulseConstraint. When there are
 * no collisions, the ForceConstraint can solve for the accelerations that keep the
 * objects joined. If one of the objects is involved in a collision, however, the 
 * assumption of legal velocities is violated (since an impulse instanteously changes
 * velocity). Therefore, an impulse needs to be applied at the pin joint to keep the
 * velocities legal.
 */
public class PinJoint implements ForceConstraint, ImpulseConstraint
    {
    private Double2D r1;
    private Double2D r2;
        
    private PhysicalObject2D obj1;
    private PhysicalObject2D obj2;
        
    // Constraint sub matrices
    private DenseMatrix subJacobianMatrix1;
    private DenseMatrix subJacobianDotMatrix1;
    private DenseMatrix subJacobianMatrix2;
    private DenseMatrix subJacobianDotMatrix2;
    private Vector subConstraintVector;
    private Vector subConstraintDotVector;
        
    // Collision sub matrices
    private DenseMatrix subCollisionRowsMatrix1;
    private DenseMatrix subCollisionColsMatrix1;
    private DenseMatrix subCollisionRowsMatrix2;
    private DenseMatrix subCollisionColsMatrix2;
    private DenseMatrix subCollisionIntersectionMatrix;
    private Vector subCollisionAnswerVector;
        
    // These things get used a lot while setting up the constraint
    // matrices, so only set them up once per call.
    // Caching the sins and cosines created a big speed up.
    private double theta1;
    private double theta2;
        
    private double cosTheta1;
    private double sinTheta1;
    private double cosTheta2;
    private double sinTheta2;
        
    // temp matrices and vectors for adding and multiplying
    private Vector tempConstraintDotVector;
        
    public PinJoint(Double2D pos, PhysicalObject2D obj1, PhysicalObject2D obj2)
        {
        this.obj1 = obj1;
        this.obj2 = obj2;
                
        // get the position of the pin joint in the objects' local 
        // coordinate frames
        // x_global = R * x_local + T
        // ==> x_local = R_inv(x_global - T)
                
        // Get the R_inv matrix - since rotation matrices are
        // "special orthogonal" their inverse is the same as their transpose.
                
        // OBJ1
        double theta = obj1.getOrientation().radians;
        double cosTheta = Math.cos(theta);
        double sinTheta = Math.sin(theta);

        double[][] vals = 
            {{cosTheta, sinTheta},
                 {-sinTheta, cosTheta}};
                
        DenseMatrix rotInverse = new DenseMatrix(vals);
        Double2D objPos = obj1.getPosition();
        Vector vecPJPos = new Vector(2);
        vecPJPos.vals[0] = pos.x;
        vecPJPos.vals[1] = pos.y;
                
        Vector vecPos = new Vector(2);
        vecPos.vals[0] = objPos.x;
        vecPos.vals[1] = objPos.y;
                
        Vector local = rotInverse.times(vecPJPos.minus(vecPos));
        r1 = new Double2D(local.vals[0], local.vals[1]);
                
        // OBJ2
        theta = obj2.getOrientation().radians;
        cosTheta = Math.cos(theta);
        sinTheta = Math.sin(theta);
                
        rotInverse.vals[0][0] = cosTheta;
        rotInverse.vals[0][1] = sinTheta;
        rotInverse.vals[1][0] = -sinTheta;
        rotInverse.vals[1][1] = cosTheta;

        objPos = obj2.getPosition();
        vecPos.vals[0] = objPos.x;
        vecPos.vals[1] = objPos.y;
                
        local = rotInverse.times(vecPJPos.minus(vecPos));
        r2 = new Double2D(local.vals[0], local.vals[1]);
                
        // Other initialization
        // Init constraint vars
        subConstraintVector = new Vector(2);
        subConstraintDotVector = new Vector(2);
        tempConstraintDotVector = new Vector(2);
        subJacobianMatrix1 = new DenseMatrix(2, 3);
        subJacobianMatrix2 = new DenseMatrix(2, 3);
        subJacobianDotMatrix1 = new DenseMatrix(2, 3);
        subJacobianDotMatrix2 = new DenseMatrix(2, 3);
                
        // Set some constant values
        subJacobianMatrix1.vals[0][0] = 1;
        subJacobianMatrix1.vals[1][1] = 1;
                
        subJacobianMatrix2.vals[0][0] = -1;
        subJacobianMatrix2.vals[1][1] = -1;
                
        // Init collision vars
        subCollisionRowsMatrix1 = new DenseMatrix(2, 3);
        subCollisionColsMatrix1 = new DenseMatrix(3, 2);
        subCollisionRowsMatrix2 = new DenseMatrix(2, 3);
        subCollisionColsMatrix2 = new DenseMatrix(3, 2);
                
        subCollisionIntersectionMatrix = new DenseMatrix(2, 2);
        subCollisionAnswerVector = new Vector(2);
        }
        
    public int GetConstraintRows()
        {
        return 2;
        }
        
    public int GetCollisionResponseRows()
        {
        return 2;
        }
        
    /** Used in resting contact calculations */
    public void addHolonomicConstraints()
        {
        sim.physics2D.PhysicsState ps = sim.physics2D.PhysicsState.getInstance();
        //ps.lcp.addHolonomicContact((PhysicalObject2D)this.physicalObjects.get(0), (PhysicalObject2D)this.physicalObjects.get(1), new Double2D(-1, 0), ((Double2D)connections.get(1)).subtract((Double2D)connections.get(0)), new Double2D(0, 0));
        //ps.lcp.addHolonomicContact((PhysicalObject2D)this.physicalObjects.get(0), (PhysicalObject2D)this.physicalObjects.get(1), new Double2D(0, -1), ((Double2D)connections.get(1)).subtract((Double2D)connections.get(0)), new Double2D(0, 0));
        }
        
    public void setConstraintMatrices(int curConstraintRow, BlockSparseMatrix jacobianMatrix, BlockSparseMatrix jacobianDotMatrix, Vector constraintVector, Vector constraintDotVector)
        {
        int DOF = sim.physics2D.PhysicsState.getInstance().numObjs();
                
        theta1 = obj1.getOrientation().radians;
        theta2 = obj2.getOrientation().radians;
                
        cosTheta1 = Math.cos(theta1);
        sinTheta1 = Math.sin(theta1);
        cosTheta2 = Math.cos(theta2);
        sinTheta2 = Math.sin(theta2);
                
        setConstraintVector();
        setJacobianMatrix1();
        setJacobianMatrix2();
        setJacobianDotMatrix1();
        setJacobianDotMatrix2();
        setConstraintDotVector(subJacobianMatrix1, subJacobianMatrix2);
                
        // Set the constraint and constraint dot vectors
        constraintVector.vals[curConstraintRow] = subConstraintVector.vals[0];
        constraintDotVector.vals[curConstraintRow] = subConstraintDotVector.vals[0];
        constraintVector.vals[1 + curConstraintRow] = subConstraintVector.vals[1];
        constraintDotVector.vals[1 + curConstraintRow] = subConstraintDotVector.vals[1];
                
        int ind = obj1.index;
        int globalPosStart = ind * 3;
        int globalVelStart = DOF * 3 + globalPosStart;
                
        jacobianMatrix.setBlock(curConstraintRow, globalPosStart, subJacobianMatrix1.vals);
        jacobianDotMatrix.setBlock(curConstraintRow, globalPosStart, subJacobianDotMatrix1.vals);
                
        ind = this.obj2.index;
        globalPosStart = ind * 3;
        globalVelStart = DOF * 3 + globalPosStart;
                
        jacobianMatrix.setBlock(curConstraintRow, globalPosStart, subJacobianMatrix2.vals);
        jacobianDotMatrix.setBlock(curConstraintRow, globalPosStart, subJacobianDotMatrix2.vals);               
        }
        
    private void setConstraintVector()
        {
        // [x1 + r1x * cos(theta1) - r1y * sin(theta1) - (x2 + r2x * cos(theta2) - r2y * sin(theta2))
        // y1 + r1x * sin(theta1) + r1y * cos(theta1) - (y2 + r2x * sin(theta2) + r2y * cos(theta2))]
                
        Double2D pos1 = obj1.getPosition();
        Double2D pos2 = obj2.getPosition();
                
        double[] vals = subConstraintVector.vals;
                
        vals[0] = 
            pos1.x + r1.x * cosTheta1 - r1.y * sinTheta1
            - (pos2.x + r2.x * cosTheta2 - r2.y * sinTheta2);  
                
        vals[1] =
            pos1.y + r1.x * sinTheta1 + r1.y * cosTheta1
            - (pos2.y + r2.x * sinTheta2 + r2.y * cosTheta2);       
        }
        
    private void setJacobianMatrix1()
        {
        Double2D r1 = this.r1;
        double theta1 = this.theta1;
                
        double[][] vals = subJacobianMatrix1.vals;
        vals[0][2] = -r1.x * sinTheta1 - r1.y * cosTheta1;
        vals[1][2] = r1.x * cosTheta1 - r1.y * sinTheta1;
        }
        
    private void setJacobianMatrix2()
        {
        Double2D r2 = this.r2;
        double theta2 = this.theta2;
                
        double[][] vals = subJacobianMatrix2.vals;
        vals[0][2] = r2.x * sinTheta2 + r2.y * cosTheta2;
        vals[1][2] = -r2.x * cosTheta2 + r2.y * sinTheta2;
        }
        
    private void setJacobianDotMatrix1()
        {
        double w1 = obj1.getAngularVelocity();
                
        double[][] vals = subJacobianDotMatrix1.vals;
        vals[0][2] = -r1.x * cosTheta1 * w1 + r1.y * sinTheta1 * w1;
        vals[1][2] = -r1.x * sinTheta1 * w1 - r1.y * cosTheta1 * w1;
        }
        
    private void setJacobianDotMatrix2()
        {
        double w2 = obj2.getAngularVelocity();
                
        double[][] vals = subJacobianDotMatrix2.vals;
        vals[0][2] = r2.x * cosTheta2 * w2 - r2.y * sinTheta2 * w2;
        vals[1][2] = r2.x * sinTheta2 * w2 + r2.y * cosTheta2 * w2;
        }
        
    private void setConstraintDotVector(DenseMatrix jacobian1, DenseMatrix jacobian2)
        {
        Double2D vel1 = obj1.getVelocity();
        Double2D vel2 = obj2.getVelocity();
                
        double[] vec = { vel1.x, vel1.y, obj1.getAngularVelocity() };
        Vector cdot = new Vector(vec);
        
        double[] vec2 = { vel2.x, vel2.y, obj2.getAngularVelocity() };
        Vector cdot2 = new Vector(vec2);
                
        subConstraintDotVector = jacobian1.times(cdot, subConstraintDotVector).plus(jacobian2.times(cdot2, tempConstraintDotVector), subConstraintDotVector);
        }
        
    //////////////
    // COLLISION RESPONSE
    //////////////
    public void setCollisionMatrices(int curConstraintRow, BorderedDiagonalIdentityMatrix collisionMatrix, Vector answerVector)
        {
        int constraintRows = 2;
                
        // Prepare the local constraint state matrix
        setCollisionRowsMatrix();
        setCollisionColsMatrix();
                
        // Answer and intersection matrices have all zeros, so do not need to be
        // set
        int globalPosStart = obj1.index * 3;
        collisionMatrix.setBlock(curConstraintRow, globalPosStart, subCollisionRowsMatrix1.vals);
        collisionMatrix.setBlock(globalPosStart, curConstraintRow, subCollisionColsMatrix1.vals);
                
        globalPosStart = obj2.index * 3;
        collisionMatrix.setBlock(curConstraintRow, globalPosStart, subCollisionRowsMatrix2.vals);
        collisionMatrix.setBlock(globalPosStart, curConstraintRow, subCollisionColsMatrix2.vals);       
        }
        
    private void setCollisionRowsMatrix()
        {       
        /*
         * [1 0 -r1y -1  0  r2y
         *  0 1 r1x   0 -1 -r2x]
         */
        Double2D r1 = this.r1.rotate(theta1);
        Double2D r2 = this.r2.rotate(theta2);
                
        subCollisionRowsMatrix1.vals[0][0] = 1;
        subCollisionRowsMatrix1.vals[0][1] = 0;
        subCollisionRowsMatrix1.vals[0][2] = -r1.y;
        subCollisionRowsMatrix1.vals[1][0] = 0;
        subCollisionRowsMatrix1.vals[1][1] = 1;
        subCollisionRowsMatrix1.vals[1][2] = r1.x;
                
        subCollisionRowsMatrix2.vals[0][0] = -1;
        subCollisionRowsMatrix2.vals[0][1] = 0;
        subCollisionRowsMatrix2.vals[0][2] = r2.y;
        subCollisionRowsMatrix2.vals[1][0] = 0;
        subCollisionRowsMatrix2.vals[1][1] = -1;
        subCollisionRowsMatrix2.vals[1][2] = -r2.x;
        }
        
    public void setCollisionColsMatrix()
        {
        Double2D r1 = this.r1.rotate(theta1);
        Double2D r2 = this.r2.rotate(theta2);
                
        subCollisionColsMatrix1.vals[0][0] = -obj1.getMassInverse();
        subCollisionColsMatrix1.vals[0][1] = 0;
                
        subCollisionColsMatrix1.vals[1][0] = 0;
        subCollisionColsMatrix1.vals[1][1] = -obj1.getMassInverse();
                
        subCollisionColsMatrix1.vals[2][0] = r1.y * obj1.getMassMomentOfInertiaInverse();
        subCollisionColsMatrix1.vals[2][1] = -r1.x * obj1.getMassMomentOfInertiaInverse();
                
        subCollisionColsMatrix2.vals[0][0] = obj2.getMassInverse();
        subCollisionColsMatrix2.vals[0][1] = 0;
                
        subCollisionColsMatrix2.vals[1][0] = 0;
        subCollisionColsMatrix2.vals[1][1] = obj2.getMassInverse();
                
        subCollisionColsMatrix2.vals[2][0] = -r2.y * obj2.getMassMomentOfInertiaInverse();
        subCollisionColsMatrix2.vals[2][1] = r2.x * obj2.getMassMomentOfInertiaInverse();
        }
        
    public void applyImpulses(int curAnswerRow, Vector answers)
        {
        Double2D R = new Double2D(answers.vals[curAnswerRow], answers.vals[curAnswerRow + 1]);
                
        PhysicalObject2D obj1 = this.obj1;
        Double2D r1 = this.r1.rotate(obj1.getOrientation().radians);
                
        PhysicalObject2D obj2 = this.obj2;
        Double2D r2 = this.r2.rotate(obj2.getOrientation().radians);
                                
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
