;;; Copyright 2006 by Sean Luke
;;; Licensed under the Academic Free License version 3.0
;;; See the file "LICENSE" for more information

(define-simple-class <tutorial2> (<sim.display.GUIState>)
  
  (display type: <sim.display.Display2D>)
  (displayFrame type: <javax.swing.JFrame>)
  (gridPortrayal type: <sim.portrayal.grid.FastValueGridPortrayal2D>
		 init-form: (<sim.portrayal.grid.FastValueGridPortrayal2D>))            
  
  ((*init*)
   (invoke-special <sim.display.GUIState> (this) '*init*
		   (<tutorial1> (<java.lang.System>:currentTimeMillis))))
  
  ((*init* (state :: <sim.engine.SimState>))
   (invoke-special <sim.display.GUIState> (this) '*init* state))

  ((getName) :: <java.lang.String>
   allocation: 'static
   (*:toString "Tutorial2: Life"))
  
  ((setupPortrayals) :: <void>
   (gridPortrayal:setField (<tutorial1>:.grid state))
   (gridPortrayal:setMap 
    (<sim.util.gui.SimpleColorMap>
     (<java.awt.Color[]> (<java.awt.Color> 0 0 0 0)
		     <java.awt.Color>:blue))))
  
  ((start) :: <void>
   (invoke-special <sim.display.GUIState> (this) 'start)
   (setupPortrayals)
   (display:reset)
   (display:repaint))
  
  ((init c :: <sim.display.Controller>) :: <void>
   (invoke-special <sim.display.GUIState> (this) 'init c)
   (let ((tut :: <tutorial1> (as <tutorial1> state)))
     (set! display (<sim.display.Display2D> 
		    (* tut:gridWidth 4) 
		    (* tut:gridHeight 4) 
		    (this) 1))
     (set! displayFrame (display:createFrame))
     (c:registerFrame displayFrame)
     (displayFrame:setVisible  #t))
   (display:attach gridPortrayal (*:toString "Life"))
   (display:setBackdrop <java.awt.Color>:black))
  
  ((load state :: <sim.engine.SimState>) :: <void>
   (invoke-special <sim.display.GUIState> (this) 'load state)
   (setupPortrayals)
   (display:reset)
   (display:repaint)))

        
                    
