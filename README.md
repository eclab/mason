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

### Execute MASON/Distributed MASON simulation

You can build your simulation using Maven. In your project folder:

```bash
$ mvn clean install 
```

The output of the build phase should be a jar-with-dependencies in the target folder.

You can run MASON simulation using:
 
```bash
$ java -jar sim-jar-with-dependencies.jar
```

While, you can run distributed simulation using MPI (OpenMPI, version greater than 4.0), with Java biding enabled. However, for debug purposes you can use a pre-configured Docker container: `spagnuolocarmine/docker-mpi-java`.
Inside your project folder you can run:

```bash
$ docker run -v target:/data -it -t spagnuolocarmine/docker-mpi-java mpirun --allow-run-as-root -np 4 java -Xmx3G -jar /data/sim-jar-with-dependencies.jar
```

#### Know bugs

- The `MPIUtil.java` use a large pre-allocated buffer of 128 MB. In order to run in a docker container your applications you have to ensure a large amount of memory to your Docker demon, or you can reduce the size of this buffer.
