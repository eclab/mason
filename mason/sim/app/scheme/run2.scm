(load "tutorial1.scm")
(load "ca.scm")
(load "tutorial2.scm")

(set! t2 (<tutorial2>))
(set! c (<sim.display.Console> t2))
(c:setVisible #t)

