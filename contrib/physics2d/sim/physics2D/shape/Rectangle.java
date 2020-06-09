package sim.physics2D.shape;

import java.awt.*;
import sim.util.matrix.*;
import sim.physics2D.util.*;
import sim.util.Double2D;

/** Rectangle implementation of Polygon
 */
public class Rectangle extends Polygon 
    {
    private double width;
    private double height;
        
    public Rectangle(double width, double height, Paint paint)
        {
        this(width, height, paint, false);
        }
        
    public Rectangle(double width, double height, Paint paint, boolean stationary)
        {
        super(stationary);
                
        this.width = width;
        this.height = height;
        this.paint = paint;
                
        initVertices();
        initEdges();
        initNormals();
        }
        
    /** Set up the vertices matrix in homogenous coordinates */
    public void initVertices()
        {
        // going clockwise starting from the bottom left
        double[][] verts = 
            {{-width / 2, -width / 2, width / 2, width / 2},
                 {-height / 2, height / 2, height / 2, -height / 2},
                 {1, 1, 1, 1}};
        vertices = new DenseMatrix(verts);
        }

    /** Set up the edges matrix */
    public void initEdges()
        {
        edges = new DenseMatrix(3, 4);
        Double2D curEdge;
        for (int i = 0; i < 3; i++)
            {
            // edges must be normalized
            curEdge = new Double2D(vertices.vals[0][i+1], vertices.vals[1][i+1]).subtract(new Double2D(vertices.vals[0][i], vertices.vals[1][i])).normalize();
            edges.vals[0][i] = curEdge.x;
            edges.vals[1][i] = curEdge.y;
            edges.vals[2][i] = 1;
            }
                
        // Get the last one
        curEdge = new Double2D(vertices.vals[0][0], vertices.vals[1][0]).subtract(new Double2D(vertices.vals[0][3], vertices.vals[1][3])).normalize();
        edges.vals[0][3] = curEdge.x;
        edges.vals[1][3] = curEdge.y;
        edges.vals[2][3] = 1;
        }
                
    /** Set up the normals matrix */
    public void initNormals()
        {
        // Just rotate the edges 90 degrees
        normals = new DenseMatrix(3,4);
        normals.vals[0][0] = -edges.vals[1][0];
        normals.vals[1][0] = edges.vals[0][0];
        normals.vals[2][0] = 1;
                
        normals.vals[0][1] = -edges.vals[1][1];
        normals.vals[1][1] = edges.vals[0][1];
        normals.vals[2][1] = 1;
                
        normals.vals[0][2] = -edges.vals[1][2];
        normals.vals[1][2] = edges.vals[0][2];
        normals.vals[2][2] = 1;
                
        normals.vals[0][3] = -edges.vals[1][3];
        normals.vals[1][3] = edges.vals[0][3];
        normals.vals[2][3] = 1;
        }
        
    public double getWidth()
        {
        return width;
        }
    public double getHeight()
        {
        return height;
        }

    /////////////////////////////////////////////////////////////////
    // These functions are used by the broad phase Collision detection 
    // logic
    /////////////////////////////////////////////////////////////////
        
    /** Calculate the max distance a point can be from the center of the object.
        If the object is stationary, we can give more exact values. Remember
        that stationary objects can have orientation, though, so it can be more
        complicated than width / 2 and height / 2 */
    public void calcMaxDistances(boolean mobile)
        {
        if (mobile)
            {
            maxXDistanceFromCenter = Math.sqrt(this.width * this.width + this.height * this.height) / 2;
            maxYDistanceFromCenter = maxXDistanceFromCenter;
            }
        else
            {
            // Get the max distances from the center for a stationary object
            // taking into account its orientation
            DenseMatrix rotated = Polygon.rotationTranslationMatrix2D(this.getOrientation().radians, new Double2D(0, 0)).times(this.vertices);
                        
            maxXDistanceFromCenter = 0;
            maxYDistanceFromCenter = 0;
            for (int i = 0; i < 3; i++)
                {
                if (rotated.vals[0][i] > maxXDistanceFromCenter)
                    maxXDistanceFromCenter = rotated.vals[0][i];
                if (rotated.vals[1][i] > maxYDistanceFromCenter)
                    maxYDistanceFromCenter = rotated.vals[1][i];
                }
            }
                
        // Adding padding will help to give the narrow phase
        // collision logic a chance to run before objects penetrate.
        // TODO: if we keep the dimension reduction strategy for broad phase,
        // make this customizable so users can tune their own applications.
        maxXDistanceFromCenter += .1;
        maxYDistanceFromCenter += .1;
        }
        
    /** Calculate the mass moment of intertia of the object.
     * This can be done through integration, or by finding a precomputed
     * equation for the polygon being defined 
     */
    public double getMassMomentOfInertia(double mass)
        {
        return (mass / 12) * (width * width + height * height);
        }
    }
