package sim.physics2D.shape;

import java.awt.Graphics2D;

import sim.portrayal.DrawInfo2D;
//import sim.physics2D.util.Double2D;
import sim.util.matrix.*;
import sim.util.Double2D;

/** Polygons represents any convex multi-sided object. Convex means
 * that every angle measured between the insides of two edges must be
 * less than 180 degrees. To create a new polygon, create a new class
 * that inherits from polygon and implement the abstract functions based
 * on the descriptions given below. Use rectangle as a reference.
 */
public abstract class Polygon extends Shape
    {       
    protected double maxXDistanceFromCenter;
    protected double maxYDistanceFromCenter;
        
    protected DenseMatrix vertices;
    protected DenseMatrix edges;
    protected DenseMatrix normals;
        
    private Double2D[] verticesCache;
    private Double2D[] edgesCache;
    private Double2D[] normalsCache;
        
    private boolean vertCacheValid;
    private boolean edgesCacheValid;
    private boolean normalsCacheValid;
        
    public Polygon(boolean stationary)
        {
        super(stationary);
                
        vertCacheValid = false;
        edgesCacheValid = false;
        normalsCacheValid = false;
        }
        
    public Polygon()
        {
        this(false);
        }
        
    protected DenseMatrix scale = new DenseMatrix(new double[][] 
        {{0, 0, 0},
             {0, 0, 0},
             {0, 0, 1}});

    /** Returns a list of the vertexes in a clockwise direction with
     * positive Y pointing up (vs. pointing down as on a computer screen) */
    public Double2D[] getVertices()
        {
        if (vertCacheValid)
            return verticesCache;
        else
            {
            DenseMatrix rotTranDenseMatrix = Polygon.rotationTranslationMatrix2D(this.getOrientation().radians, this.getPosition());
            DenseMatrix rotVertices = rotTranDenseMatrix.times(this.vertices);
            Double2D[] verts = new Double2D[rotVertices.n];
            for (int i = 0; i < rotVertices.n; i++)
                verts[i] = new Double2D(rotVertices.vals[0][i], rotVertices.vals[1][i]);
            verticesCache = verts;
                        
            if (stationary)
                vertCacheValid = true;
                        
            return verticesCache;
            }
        }
        
    /** Returns a list of the normalized edges in a clockwise direction. The
     * starting vertex of the edge must be the vertex with the corresponding index.
     * For example, edge 0 goes from vertex 0 to vertex 1 */
    public Double2D[] getEdges()
        {
        if (edgesCacheValid)
            return edgesCache;
        else
            {
            DenseMatrix rotDenseMatrix = Polygon.rotationTranslationMatrix2D(this.getOrientation().radians, new Double2D(0,0));
            DenseMatrix rotEdges = rotDenseMatrix.times(this.edges);
            Double2D[] result = new Double2D[rotEdges.n];
            for (int i = 0; i < rotEdges.n; i++)
                result[i] = new Double2D(rotEdges.vals[0][i], rotEdges.vals[1][i]);
                        
            edgesCache = result;
            if (stationary) edgesCacheValid = true;
                        
            return edgesCache;
            }
        }
        
    /** Returns a list of the unit normals in a clockwise direction. Each 
     * normal's index must correspond to its edge's index. For example,
     * the normal to edge 0 must have index 0 */
    public Double2D[] getNormals()
        {
        if (normalsCacheValid)
            return normalsCache;
        else
            {
            DenseMatrix rotDenseMatrix = Polygon.rotationTranslationMatrix2D(this.getOrientation().radians, new Double2D(0,0));
            DenseMatrix rotNormals = rotDenseMatrix.times(this.normals);
            Double2D[] result = new Double2D[rotNormals.n];
            for (int i = 0; i < rotNormals.n; i++)
                result[i] = new Double2D(rotNormals.vals[0][i], rotNormals.vals[1][i]);
                        
            if (stationary)
                {
                normalsCache = result;
                normalsCacheValid = true;
                }
            return result;
            }
        }
        
    // force polygons to set stuff up correctly
    /** Set up the vertices DenseMatrix. The vertices DenseMatrix gives the
     * homogenous coordinates of the vertices centered around 0,0.
     * They must be defined clockwise. */
    abstract public void initVertices();
    /** Set up the edges DenseMatrix. The edges DenseMatrix gives the edge 
     * vectors in homogenous coordinates between the 
     * vertices. The number of the edge should
     * correspond to the number of the vertex from which the vector
     * points (i.e. edge 0 points from vertex 0 to vertex 1). Edges
     * should point clockwise and must be normalized. */
    abstract public void initEdges();
    /** Set up the normals DenseMatrix. The normals DenseMatrix gives 
     * the normal vectors in homogenous coordinates. The numbers of
     * the normals must correspond to the edge to which the normal
     * is perpendicular to (i.e. normal 0 points out from edge 0). 
     * The normals must be of unit length */
    abstract public void initNormals();
        
    /** Display the polygon */
    public void draw(Object object, Graphics2D graphics, DrawInfo2D info)
        {
        graphics.setPaint(paint);

        // set the scaling DenseMatrix
        scale.vals[0][0] = info.draw.width;
        scale.vals[1][1] = info.draw.height;
                
        DenseMatrix scaledMat = scale.times(this.vertices);
        DenseMatrix rottedMat = Polygon.rotationTranslationMatrix2D(getOrientation().radians, new Double2D(info.draw.x, info.draw.y)).times(scaledMat);
        graphics.fillPolygon(Polygon.getRow(0, rottedMat),Polygon.getRow(1, rottedMat), this.vertices.n);
        }
        
    public double getMaxXDistanceFromCenter()
        {
        return this.maxXDistanceFromCenter;
        }
        
    public double getMaxYDistanceFromCenter()
        {
        return this.maxYDistanceFromCenter;
        }
        
    /** Returns a DenseMatrix in homogenous coordinates to rotate a 2 dimensional 
     * rigid body given the angle theta (in radians)
     */
    public static DenseMatrix rotationMatrix2D(double theta)
        {
        double cosTheta = Math.cos(theta);
        double sinTheta = Math.sin(theta);

        double[][] vals = 
            {{cosTheta, -sinTheta},
                 {sinTheta, cosTheta},
                 {0, 1}};
                
        return new DenseMatrix(vals);
        }

    /** Returns a DenseMatrix in homogenous coordinates to rotate and translate 
     * a 2 dimensional rigid body given the angle theta (in radians) and a translation vector
     */
    public static DenseMatrix rotationTranslationMatrix2D(double theta, Double2D translation)
        {
        double cosTheta = Math.cos(theta);
        double sinTheta = Math.sin(theta);
        
        double[][] vals = 
            {{cosTheta, -sinTheta, translation.x},
                 {sinTheta, cosTheta, translation.y},
                 {0, 0, 1}};
                
        return new DenseMatrix(vals);
        }
        
    /** Returns a row of the DenseMatrix rounded to integers */
    public static int[] getRow(int row, DenseMatrix mat)
        {
        int cols = mat.n;
        int[] result = new int[cols];
        for (int i = 0; i < cols; i++)
            result[i] = (int)Math.round(mat.vals[row][i]);
        return result;
        }
    }
