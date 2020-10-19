
# MASON Layout
This GitHub Repository for MASON contains the core MASON project in the mason/ directory and also contains various extensions to MASON in the contrib/ directory. Each extension in contrib/ is a project on its own. More details about MASON and its extensions can be found at the MASON website.

## Website
https://cs.gmu.edu/~eclab/projects/mason/



# Build MASON Toolkit

## Build Core MASON Only

Inside the folder mason/mason:

```bash
mvn clean install
```

The output should say:

```[INFO] BUILD SUCCESS```


## Import in Eclipse

1. Clone the project on your local machine, and import the Maven project.

```
File-> Import -> Existing Maven Projects -> Root Directory
```

2. Build and Install on the mason-build project.

```
Run as -> Maven install
```


## Build MASON with 3d

To use MASON in 3D, you must install Java3D. Unfortunately, installing Java3D is not as easy as it once was. You can try [Oracle's older distribution](http://www.oracle.com/technetwork/java/javase/tech/index-jsp-138252.html), but you may have more luck, particularly recent OS X version users, in installing Java3D and JOGL directly from the [jogamp website](http://jogamp.org/). To do this, you'll need to install three [Java3D libraries](http://jogamp.org/deployment/java3d/) (j3dcore.jar, j3dutils.jar, and vecmath.jar), and a few [JOGL libraries](http://jogamp.org/deployment/) (gluegen-rt.jar gluegen-rt-natives-your-platform.jar, joal.jar, joal-all-natives-your-platform.jar, jogl-all.jar, and jogl-natives-your-platform.jar — look in a recent version directory like v2.1.4/jar; the jogamp-current/jar directory often has broken files).

Often the Java3D libraries are buggy and in flux for OS X users, so I've made a collection [here](https://cs.gmu.edu/~eclab/projects/mason/j3dlibs.zip) for OS X which seems to work okay. Install these in your system-wide Java library location (on the Mac, it's /Library/Java/Extensions/). For more instructions, see [this posting](http://gouessej.wordpress.com/2012/08/01/java-3d-est-de-retour-java-3d-is-back/). 
