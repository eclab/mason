This contains the various data structures for both the applications and the partitioning for those applications.

Of particular interest in HaloField, which allows agents to check for interest within a certain range, regardless of where it lies on a processor.

This is still underwork, as many of the data structures that exist in Mason have not quite been ported over into a distributed version of itself here.

continuous/ only has NContinuousGrid2D, for example.
grid/ has NDoubleGrid@d, NIntGrid2D, NIntQTGrid2D, NObjectGrid2D, NObjectsGrid2D

storage/ contains the actual value storage facilities for each of the types of DataStructures above. (ContStorage is used for NContinuous2D, for example).

The types of partitions here are DQuadTreePartition and
storage/ contains the actual value storage facilities for each of the types of DataStructures above. (ContStorage is used for NContinuous2D, for example).

The types of partitions here are DQuadTreePartition and DNonUniformPartition. If you require your own type of custom partitioning, you may extend from DPartition.


