## About
GeoMason is an optional extension to MASON that adds support for vector and raster geospatial data.

You can find the Manual on the [Project Site](https://cs.gmu.edu/~eclab/projects/mason/extensions/geomason/)

<br/>
GeoMason has a dependency on the Java Topology Suite (JTS).  Building
GeoMason presumes that you have a JTS jar file in your java class
path.

The JTS main web site is http://tsusiatsoftware.net/jts/main.html.

Also, GeoMason has optional dependencies on GeoTools and GDAL/OGR for
their respective importers.  GeoTools can be downloaded from
http://geotools.org and OGR can be downloaded from http://gdal.org/.
Note that in OGR's case you will not only have to include the jar file
in the class path but also ensure that the shared libraries are
properly installed as well as the corresponding JNI file.

If you wish to enable GeoTools or OGR GeoMason support, then uncomment
the corresponding make file variable in the "jar" rule.

And, GeoMason obviously has a dependency on Mason, so please ensure
that it is included in your class path, too.

### GeoMason Examples
Included with GeoMason are several example models which you can run as:

```
java sim.app.geo.campusworld.CampusWorldWithUI
java sim.app.geo.colorworld.ColorWorldWithUI
java sim.app.geo.dadaab.DadaabGUI
java sim.app.geo.ebola.EbolaWithUI
java sim.app.geo.gridlock.GridlockWithUI
java sim.app.geo.haiti.HaitiFoodWithUI
java sim.app.geo.hotspots.HotspotsWithUI
java sim.app.geo.kibera.KiberaWithUI
java sim.app.geo.nearbyworld.NearbyWorldWithUI
java sim.app.geo.networkworld.NetworkWorldWithUI
java sim.app.geo.refugee.MigrationWithUI
java sim.app.geo.schellingpolygon.PolySchellingWithUI
java sim.app.geo.schellingspace.SchellingSpaceWithUI
java sim.app.geo.sickStudents.SickStudentsModelWithUI
java sim.app.geo.sillypeds.SillyPedsWithUI
java sim.app.geo.sleuth.SleuthWorldWithUI
java sim.app.geo.touchingworld.TouchingWorldWithUI
java sim.app.geo.turkana.TurkanaSouthModelWithUI
java sim.app.geo.waterworld.WaterWorldWithUI
```

Note that some of these models will require large data files. As mentioned above, they are located in jar files in geomason_data.zip (1GB) which you will need to download and add to your CLASSPATH.

These examples are provided to highlight the basic functionality of GeoMason and act as examples for how geographically explicit models can be built. The source for these demos can be found in GeoMason's sim/app/geo directory; the corresponding data is in sim/app/data.