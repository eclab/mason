/*
  Copyright 2017 by Sean Luke and George Mason University
  Licensed under the Academic Free License version 3.0
  See the file "LICENSE" for more information
*/

package sim.portrayal.simple;
import sim.portrayal.*;
import java.awt.*;
import java.awt.geom.*;
import sim.field.continuous.*;
import sim.field.*;

import sim.display.*;
import java.awt.event.*;

/**
   A SimplePortrayal2D for the visualization and manipulation of JBox2D Bodies and their fixtures.  
   This works by building a java.awt.Shape equivalent to the underlyhing JBox2D Body and then either
   displaying it or hit-testing on it.  This is not particularly efficient, but there you have it.
   */

public class JBox2DPortrayal extends SimplePortrayal2D
    {
    Rectangle2D.Float bounds;
    static final Stroke defaultStroke = new BasicStroke();
    Stroke stroke;
 	Paint paint;
    boolean filled;
    AffineTransform transform = new AffineTransform();

    public JBox2DPortrayal(Rectangle2D.Double bounds, Color color)
    	{ 
    	this(color, true, bounds); 
    	}
    	
    public JBox2DPortrayal(Rectangle2D.Double bounds) 
    	{
    	this(Color.GRAY, true, bounds);
    	}
    	
    public JBox2DPortrayal(Paint paint, boolean filled, Rectangle2D.Double bounds)
        {
        this.filled = filled;
        this.paint = paint;
    	this.bounds = new Rectangle2D.Float((float)bounds.x, (float)bounds.y, (float)bounds.width, (float)bounds.height);
        }

    public void setStroke(Stroke s)
        {
        stroke = s;
        }
        
    public Stroke getStroke()
    	{
    	return stroke;
    	}
    	
    public void setPaint(Paint p)
    	{
    	paint = p;
    	}
    	
    public Paint getPaint()
    	{
    	return paint;
    	}

    public Shape buildShape(org.jbox2d.dynamics.Body body, boolean invertY, float height)
    {
        return buildShape(body.getFixtureList(), invertY, height);
    }

    public Shape buildShape(org.jbox2d.dynamics.Fixture fixture, boolean invertY, float height)
    	{
        if (fixture.getNext() == null)
          {
            return buildSimpleShape(fixture, invertY, height);
          }
        else
          {
            Area first = new Area(buildSimpleShape(fixture, invertY, height));
            Shape rest = buildShape(fixture.getNext(), invertY, height);
            if (rest instanceof Area)
              {
                first.add((Area)rest);
                return first;
              }
            else
              {
                first.add(new Area(rest));
                return first;
              }
          }
        }
      public Shape buildSimpleShape(org.jbox2d.dynamics.Fixture fixture, boolean invertY, float height)
          {
    	org.jbox2d.collision.shapes.Shape shp = fixture.getShape();
        if (shp instanceof org.jbox2d.collision.shapes.PolygonShape)
        	{
        	org.jbox2d.collision.shapes.PolygonShape sh = (org.jbox2d.collision.shapes.PolygonShape) shp;
        	Path2D.Float path = new Path2D.Float();
        	int count = sh.getVertexCount();
        	if (count > 0)
        		{
        		org.jbox2d.common.Transform trans = fixture.getBody().getTransform();
        		org.jbox2d.common.Vec2 vec = org.jbox2d.common.Transform.mul(trans, sh.m_vertices[0]);
	        	path.moveTo(vec.x, invertY ? height - (vec.y) : vec.y );

	        	for(int i = 1; i < count; i++)
	        		{
        			vec = org.jbox2d.common.Transform.mul(trans, sh.m_vertices[i]);
	        		path.lineTo(vec.x, invertY ? height - (vec.y) : vec.y);
	        		}

				path.closePath();
				}

			return path;
        	}
        else if (shp instanceof org.jbox2d.collision.shapes.EdgeShape)
        	{
        	org.jbox2d.collision.shapes.EdgeShape sh = (org.jbox2d.collision.shapes.EdgeShape) shp;
        	org.jbox2d.common.Transform trans = fixture.getBody().getTransform();
        	org.jbox2d.common.Vec2 vec1 = org.jbox2d.common.Transform.mul(trans, sh.m_vertex1);
        	org.jbox2d.common.Vec2 vec2 = org.jbox2d.common.Transform.mul(trans, sh.m_vertex2);
        	return new Line2D.Float(vec1.x, invertY ? height - (vec1.y) : vec1.y,
        							vec2.x, invertY ? height - (vec2.y) : vec2.y);
        	}
        else if (shp instanceof org.jbox2d.collision.shapes.CircleShape)
        	{
        	org.jbox2d.common.Vec2 position = fixture.getBody().getPosition();
        	org.jbox2d.collision.shapes.CircleShape sh = (org.jbox2d.collision.shapes.CircleShape) shp;
        	Ellipse2D e =  new Ellipse2D.Float(position.x - sh.m_radius, invertY ? height - (position.y - sh.m_radius) - (sh.m_radius * 2) : position.y - sh.m_radius,
        							   			sh.m_radius * 2, invertY ? sh.m_radius * 2 : sh.m_radius * 2);
        	return e;
        	}
        else if (shp instanceof org.jbox2d.collision.shapes.ChainShape)
        	{
        	org.jbox2d.collision.shapes.ChainShape sh = (org.jbox2d.collision.shapes.ChainShape) shp;
        	Path2D.Float path = new Path2D.Float();
        	int count = sh.m_vertices.length;
        	org.jbox2d.common.Transform trans = fixture.getBody().getTransform();
        	if (count > 0)
        		{
        		org.jbox2d.common.Vec2 vec = org.jbox2d.common.Transform.mul(trans, sh.m_vertices[0]);
        		path.moveTo(vec.x, invertY ? height - (vec.y) : vec.y);

        		for(int i = 1; i < count; i++)
        			{
        			vec = org.jbox2d.common.Transform.mul(trans, sh.m_vertices[i]);
        			path.lineTo(vec.x, invertY ? height - (vec.y) : vec.y);
        			}
				// don't close the path
				}

			return path;
        	}
        else // uh....
        	{
        	return new Path2D.Float();
        	}
    	}

    // assumes the graphics already has its color set
    public void draw(Object object, Graphics2D graphics, DrawInfo2D info)
        {
        Continuous2D field = (Continuous2D)(info.fieldPortrayal.getField());

        JBox2DObject obj = ((JBox2DObject) object);
        org.jbox2d.dynamics.Body body = obj.getBody();
        Shape shape = buildShape(body, obj.getInvertY(), (float)field.getHeight());
        graphics.setPaint(paint);

    		final double width = info.draw.width * field.width / bounds.width;
    		final double height = info.draw.height * field.height / bounds.height;

    		transform.setToTranslation(info.parent.draw.x, info.parent.draw.y);
    		transform.scale(width, height);

    		Shape transformedShape = transform.createTransformedShape(shape);

    		// now draw
    		org.jbox2d.collision.shapes.Shape shp = body.getFixtureList().getShape();

    		if ((shp instanceof org.jbox2d.collision.shapes.CircleShape ||
    			 shp instanceof org.jbox2d.collision.shapes.PolygonShape) &&
    			 filled)
    			{
    			graphics.fill(transformedShape);
    			}
    		else
    			{
    			graphics.setStroke(stroke == null ? defaultStroke : stroke);
    			graphics.draw(transformedShape);
    			}
        }

    public boolean hitObject(Object object, DrawInfo2D range)
        {
        Continuous2D field = (Continuous2D)(range.fieldPortrayal.getField());

        JBox2DObject obj = ((JBox2DObject) object);
        org.jbox2d.dynamics.Fixture fixture = obj.getBody().getFixtureList();
        Shape shape = buildShape(fixture, obj.getInvertY(), (float)field.getHeight());

		final double width = range.draw.width * field.width / bounds.width;
		final double height = range.draw.height * field.height / bounds.height;

		transform.setToTranslation(range.parent.draw.x, range.parent.draw.y);
		transform.scale(width, height);

		Shape transformedShape = transform.createTransformedShape(shape);

        // now hit-test
        return new Area(transformedShape).intersects(range.clip.x, range.clip.y, range.clip.width, range.clip.height);
        }
    }
