/*
  Copyright 2017 by Sean Luke and George Mason University
  Licensed under the Academic Free License version 3.0
  See the file "LICENSE" for more information
*/

package sim.portrayal;

/**
   A Constrained object has control over how it is moved by a Field Portrayal, notably
   via MovablePortrayal2D.
   
   <p>Objects which implement this method can:
   <ul>
   <li>Prevent MovablePortrayal2D or other portrayals from moving the object in the field.
   <li>Revise the location to which they are being moved (for example, to only allow movement
       along a vertical line, say).
   <li>Discover that they're about to be moved and update their internal belief about their
       location accordingly.
	</ul>
   
   <p>Objects which do NOT implement this interface have no say: MovablePortrayal2D will
   have FieldPortrayals go directly to their fields and move them about.
*/

public interface Constrained
    {
    /** Given a field and a proposed location to move the Object to, returns a new location
		to which the Object should actually be moved, or (optionally) null, which means that
		the Object should not be moved. 
		
		<p>
		<ul>
		<li>If you are fine with being moved to <i>location</i>, just return <i>location</i>.
		<li>If you don't want to be moved, return null.
		<li>If you want to override the proposed location and change it to somewhere else,
			simply return a revised location.
		</ul>
		*/
    public Object constrainLocation(Object field, Object location);
    }
