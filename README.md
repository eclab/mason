# Build MASON Toolkit

Inside the folder mason-build:

```bash
mvn clean install 
```

The output of the package phase should be:

- on the main group id cs.gmu.edu.eclab
	- mason/target/mason-core-19.jar 
	- distributed/target/distributed-1.0.jar
- on the contrib group id cs.gmu.edu.eclab.contrib
	- geomason/target/geomason-1.0.jar 
	- socialnets/target/socialnets-1.0.jar 

Moreover, the building phase install in your local Maven registry two Maven archetype:

- mason-project-archetype, a MASON example project that execute the Flocker simulation.
- distributed-project-archetype, a Distributed MASON example project that execute the D-Flocker simulation.

### MASON/Distributed MASON Maven archetypes

We provide two different archetypes:

- mason-project-archetype, a simple MASON simulation project with all dependencies included.
- distributed-mason-project-archetype, a simple Distributed MASON simulation project with all dependencies included.

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

- The `MPIUtil.java` use a large pre-allocated buffer of 1GB. In order to run in a docker container your applications you have to ensure a large amount of memory to your Docker demon, or you can reduce the size of this buffer.

### GeoMASON Module Build

In order to include GeoMASON project you have to change the mason-build project descriptor (mason-build/pom.xml).
In the <modules> section you have to add:

```xml
<module>geomason</module>
<module>socialnets</module> 
```

#### Know bugs

- Errors in the ```sim.app.geo.cityMigration```. Missing class `CityMigrationData`. We remove for this version the GeoMASON app:
	- cityMigration.
	- conflictdiamonds.
	- riftland. 
