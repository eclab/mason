;;; Copyright 2006 by Sean Luke
;;; Licensed under the Academic Free License version 3.0
;;; See the file "LICENSE" for more information

(define-simple-class <tutorial1> (<sim.engine.SimState>)

  (grid type: <sim.field.grid.IntGrid2D>)
  (gridWidth type: <int> init-form: 100)
  (gridHeight type: <int> init-form: 100)
  
  ;; the bHeptomino
  (bHeptomino type: <int[][]>
	      allocation: 'static
	      init-form: (<int[][]>
			      (<int[]> 0 1 1)
			      (<int[]> 1 1 0)
			      (<int[]> 0 1 1)
			      (<int[]> 0 0 1)))

    ;;; constructor
  ((*init* seed :: <long>)
   (invoke-special <sim.engine.SimState> (this) '*init* seed))
  
  ;; void seedGrid
  ((seedGrid) :: <void>
   (let ((b :: <int[][]> bHeptomino)
	 (f :: <int[][]> grid:field))
     (do ((x :: <int> 0 (+ x 1))) ((= x b:length))
       (do ((y :: <int> 0 (+ y 1))) ((= y (b x):length))
	 (set! ((f (+ x (/ f:length 2) (- (floor (/ b:length 2)))))
		(+ y (/ (f x):length 2) (- (floor (/ (b x):length 2)))))
	       ((b x) y))))))
  
  ;; public void start()
  ((start) :: <void>
   (invoke-special <sim.engine.SimState> (this) 'start)
   (set! grid (<sim.field.grid.IntGrid2D> gridWidth gridHeight))
   (seedGrid)
   (schedule:scheduleRepeating (<ca>))))
