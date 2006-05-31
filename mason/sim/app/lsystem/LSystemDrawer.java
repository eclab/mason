/*
  Copyright 2006 by Daniel Kuebrich
  Licensed under the Academic Free License version 3.0
  See the file "LICENSE" for more information
*/

package sim.app.lsystem;
import sim.util.*;
import sim.engine.*;

// This is the steppable that interprets the fully expanded l-system

public /*strictfp*/ class LSystemDrawer implements Steppable
    {
    // copy the code over so that you can draw while calculating
    ByteList code;
    
    // draw settings remain local so that no changes are made mid-draw
    int draw_time;
    double x,y,theta, angle;
    double segsize;
    
    // used to keep track of popping and pushing positions
    // Bags have pop() and push() functions in addition to normal list functions
    // Meaning that they can be used either way, or as both... freaky.
    public Bag stack;
    
    // draw segment name
    Segment s;
    
    // we can stop when done drawing
    public Stoppable stopper;
    
    LSystemDrawer( LSystemData l )
        {
        // copy copy copy
        this.code = new ByteList(l.code);
        
        this.x = l.x;
        this.y = l.y;
        this.angle = l.angle;
        this.theta = l.theta;
        this.segsize = l.segsize;
        
        // -1 is an odd number to initialize to, but seems the best choice for the step loop
        draw_time = -1;
        stack = new Bag();
        }

    public void step( final SimState state )
        {
        // draw stuff
        LSystem ls = (LSystem)state;
        
        while(true)
            {
            draw_time++;
            
            // If done, then stop the sim
            if(draw_time >= code.length)
                {
                if(stopper!=null) stopper.stop();
                return;
                }

            // stack functionality
            if(code.b[draw_time] == ((byte)'['))                // push
                {
                // using a double3d.. except the z is actually the angle
                stack.push(new Double3D(x,y,theta));
                }
            else if(code.b[draw_time] == ((byte)']'))           // pop
                {
                Double3D d = (Double3D)stack.pop();
                x = d.x;
                y = d.y;
                theta = d.z;
                }
            // else normal stuff
            // rotate
            else if(code.b[draw_time] == ((byte)'-'))
                theta += angle;
            // rotate
            else if(code.b[draw_time] == ((byte)'+'))
                theta -= angle;
            // if it's a capital letter, draw forward
            else if(code.b[draw_time] >= ((byte)'A') && code.b[draw_time] <= ((byte)'Z'))
                {
                // draw a segment there
                s = new Segment(x,y,segsize,theta);
                ls.drawEnvironment.setObjectLocation(s, new Double2D(s.x, s.y));
                x += (segsize * /*Strict*/Math.cos(theta));
                y += (segsize * /*Strict*/Math.sin(theta));
                break;
                }
            // if it's a lowercase letter, skip forward but don't draw
            else if(code.b[draw_time] >= ((byte)'a') && code.b[draw_time] <= ((byte)'z'))
                {
                // don't draw, just skip the space
                x += (segsize * /*Strict*/Math.cos(theta));
                y += (segsize * /*Strict*/Math.sin(theta));
                }

            else
                {
                // this should never happen except on bad user input
                System.err.println("Error--bad code:  " + (char)code.b[draw_time] );
                break;
                }
            }
        // end while
        }
    // end step
    }
// end class
