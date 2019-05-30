;;; Copyright 2006 by Sean Luke
;;; Licensed under the Academic Free License version 3.0
;;; See the file "LICENSE" for more information

(load "tutorial1.scm")
(load "ca.scm")
(load "tutorial2.scm")

(set! t2 (<tutorial2>))
(set! c (<sim.display.Console> t2))
(c:setVisible #t)

