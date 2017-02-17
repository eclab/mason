# mason
MASON Multiagent Simulation Toolkit

To develop you own 2d simulations using mason it is as easy as adding a dependency for mason by following the instructions at https://jitpack.io/.  For example if you have a maven project in your pom.xml add

```xml
<repositories>
		<repository>
		    <id>jitpack.io</id>
		    <url>https://jitpack.io</url>
		</repository>
	</repositories>
  
  <dependency>
	    <groupId>com.github.User</groupId>
	    <artifactId>Repo</artifactId>
	    <version>Tag</version>
	</dependency>
```

Replace User with the username of this repo, Repo with mason, and if you want the latest version replace Tag with -SNAPSHOT (to get the latest), otherwise you can replace Tag with a specific short commit id, or version id if there is a release.  If you are using gradle or some other build tool see the instructions on https://jitpack.io/.

MASON can also be built by following the instructions in the README here https://github.com/eclab/mason/blob/master/mason/README
