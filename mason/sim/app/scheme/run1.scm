;;; Copyright 2006 by Sean Luke
;;; Licensed under the Academic Free License version 3.0
;;; See the file "LICENSE" for more information

(load "tutorial1.scm")
(load "ca.scm")

;;; Because of a bug in Kawa, we can't call <tutorial1>:class to get the class
;;; out of the tutorial1 Scheme "class" yet...
;;; (<sim.engine.SimState>:doLoop <tutorial1>:class (<java.lang.String[]> (*:toString "-until") (*:toString 5000))

;; So instead we'll just do our own start/step/finish cycle for now:

(let ((t1 (<tutorial1> 1234)))
  (t1:start)
  (do ((x 0 (+ x 1))) ((= x 5000))
    (if (= (modulo (+ x 1) 500) 0) 
	(begin (write x) (newline)))
    (t1:schedule:step t1))
  (t1:finish))

(exit)
