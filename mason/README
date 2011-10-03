Welcome to MASON, a simulator designed for simple-multiagent simulations,
or to be used as a core library inside more sophisticated multiagent
simulators. 

MASON was written by Sean Luke, Gabriel Catalin Balan, and Liviu Panait. 
Additional code and apps were written by Daniel Kuebrich and Sean Paus. 
Much help was provided by Claudio Cioffi-Revilla and Ken De Jong.  MASON
is a joint effort of George Mason University's Evolutionary Computation
Laboratory (in the Computer Science Department), and GMU's Center for
Social Complexity. 

Here are files you should know about. 

LICENSE.  MASON largely uses the Academic Free License version 3.0.
	  Please read it first.  It is located in the LICENSE file.

INSTALLATION AND INTRODUCTION.  Installation information, and the root
          point for all documentation, can be found at docs/index.html . 

CHANGES.  The changes from previous versions are listed in the CHANGES file. 

If you have any questions, you have two routes.  First, check out the
MASON-INTEREST-L discussion group on the MASON website.  Second, the MASON
website also lists a help email address you can send mail to if you get
desperate.  The website URL is http://cs.gmu.edu/~eclab/projects/mason/
Please do NOT mail the developers directly.  Post to the discussion group. 

Note that most of the subdirectories contain README files describing the
classes and basic files in those directories.  This might or might not be
useful to you when poking around.

---------------------------------------------------------------------------


Quick Start: In the same directory as this file there is a directory
called 'start'.  Inside this directory there are scripts to fire up MASON. 

	Windows:	double-click on 'mason.bat'
	OS X:		double-click on 'mason.command'
	Linux/X11:	exec 'mason.sh'

...and pick an application from the list which appears.  Try
sim.app.heatbugs.HeatBugsWithUI for example. 

To run 3D demos, you have to install Java3D.  To make movies, you have to
install the Java Media Framework (JMF).  To do charting, you have to install
JCommon, JFreeChart, and iText.  See the web page for information about
where to obtain and install these packages.  If the jcommon...jar, 
jfreechart...jar, itext...jar, and jmf.jar files (or other jar files) 
are not in your CLASSPATH, the scripts will still recognize them if you 
drop them in the 'mason' directory.

---------------------------------------------------------------------------

Using the Jar File:

1. Install Java3D.

2. Install JMF (or add the 'cross-platform' jmf.jar file to your
   CLASSPATH or to your 'mason' directory)

3. Install JCommon, JFreeChart, and iText (or add the jcommon...jar,
   jfreechart...jar, and itext...jar files to your CLASSPATH or in the
   'mason' directory)

4. In the 'jar' directory there is a file named something like
   'mason.jar'.  Try double-clicking on it (you won't be able to run
   all the 3D demos due to lack of memory).

5. Alternatively, you can run the mason.jar file, with expanded
   memory, like this:

   java -Xmx200M -jar jar/mason.jar sim.display.Console


--------------------------------------------------------------------------

Not So Quick Start With Full Source:

1. Install Java3D.

2. Install JMF (or add the 'cross-platform' jmf.jar file to your 
   CLASSPATH or to your 'mason' directory)

3. Install JCommon, JFreeChart, and iText (or add the jcommon...jar,
   jfreechart...jar, and itext...jar files to your CLASSPATH or in the
   'mason' directory)

4. Add the 'mason' directory to your CLASSPATH.   Or add the 'jar' file
   located in the jar/ directory if you don't care about source.

5. Run MASON like this:

   java -Xmx200M sim.display.Console

   ...and pick an application from the pop-up list which appears.  The
   -Xmx200M is because one application (Particles3D) won't run unless you
   give Java more memory.  You can also run any particular application by
   firing up its sim.app.foo.FooWithUI class, such as: 

   java sim.app.heatbugs.HeatBugsWithUI

6. MASON's top-level java packages are the sim and ec directories.  You can
   use the Makefile to build MASON easily from MacOS X/Linux/UNIX.  To see
   the Makefile options, try

   make help

---------------------------------------------------------------------------

Where to go from here?:

1. Play with the demos.
2. Poke around in the docs.
3. Try tutorial 0 in the docs (just a tour of what MASON does)
4. Try tutorials 1-7 (which teach how to code MASON apps).  They're
   in sim/app.  
5. Read the how-tos for advanced questions.
6. If you get stuck, post to the discussion group.
7. There are various modules for MASON avaialble on the web page: check 'em out!

