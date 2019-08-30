# Execute MASON/Distributed MASON simulation

## Suggested Workflow for Eclipse

#### Import in Eclipse
Import the Maven project.
``File -> Import -> Existing Maven Projects -> Root Directory``

#### Build and Install the mason-build project.
``In MASON Run as -> Maven install``
If needed update dependencies for Maven using ``right click -> maven -> update project``

#### Import the Distributed Project
``File -> Import -> Existing Maven Projects -> distributed Directory``

####Build and Install Distributed
In distributed ``Run as -> Maven install``
If needed update dependencies for Maven using ``right click -> maven -> update project``

#### Setup Classpath for Eclipse
Go to ``Java Build Path -> Add -> mason``

#### Exporting JAR
``Export -> Runnable JAR -> Specify class and destination & select “package required libraries into Generated JAR”``

  
  

#### Docker
While, you can run distributed simulation using MPI (OpenMPI, version greater than 4.0), with Java biding enabled. However, for debug purposes you can use a pre-configured Docker container: `spagnuolocarmine/docker-mpi-java`.
Inside your project folder you can run:

```bash
$ docker run -v target:/data -it -t spagnuolocarmine/docker-mpi-java mpirun --allow-run-as-root -np 4 java -Xmx3G -jar /data/sim-jar-with-dependencies.jar
```

#### Known issues

- The `MPIUtil.java` use a large pre-allocated buffer of 128 MB. In order to run in a docker container your applications you have to ensure a large amount of memory to your Docker demon, or you can reduce the size of this buffer.