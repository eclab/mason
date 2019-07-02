name= $artifactId
docker run -v  target:/data -it -t spagnuolocarmine/docker-mpi-java mpirun --allow-run-as-root -np 4 java -Xmx3G -jar /data/$name-jar-with-dependencies.jar