
(define-simple-class <ca> (<sim.engine.Steppable>)

    ;; tempGrid
    (temp-grid type: <sim.field.grid.IntGrid2D>
               init-form: (<sim.field.grid.IntGrid2D> 0 0))

    ;; public void step(SimState state)
    ((step (state :: <sim.engine.SimState>)) :: <void>

        (let ((width :: <int> (temp-grid:getWidth))
               (height :: <int> (temp-grid:getHeight)))

            (temp-grid:setTo (<tutorial1>:.grid state))

            (let ((f :: <int[][]> temp-grid:field)
                  (g :: <int[][]> (<tutorial1>:.grid state):field))
                        
                (do ((x :: <int> 0 (+ x 1))) ((= x width))
                    (do ((y :: <int> 0 (+ y 1))) ((= y height))
                        (let ((count :: <int> 0))
                            (do ((dx :: <int> -1 (+ dx 1))) ((= dx 2))
                                (do ((dy :: <int> -1 (+ dy 1))) ((= dy 2))
                                    (set! count (+ count
                                        ((f (*:stx temp-grid (+ x dx)))
                                            (*:sty temp-grid (+ y dy)))))))
    
                             (if (or (<= count 2) (>= count 5))
                                 (set! ((g x) y) 0)
                                 (if (= count 3)
                                    (set! ((g x) y) 1))))))))))
                                    
