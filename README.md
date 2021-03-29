## Akka Scala Cluster Example

### Introduction
This is a Scala, Akka project that demonstrates how to setup a basic [Akka Cluster](https://doc.akka.io/docs/akka/current/index-cluster.html).

This project is simply the scala version of [akka-java-cluster](https://github.com/mckeeh3/akka-java-cluster).


### The ClusterListenerActor Actor
The first actor we will look at is named ClusterListenerActor. This actor is set up to receive messages about cluster events. 
As nodes join and leave the cluster, this actor receives messages about these events. 
Theses received messages are then written to a logger.

The ClusterListenerActor provides a simple view of cluster activity. Here is an example of the log output:
```
22:52:12.006 INFO  [ClusterSystem-akka.actor.default-dispatcher-23] [akka.tcp://ClusterSystem@127.0.0.1:2552/user/clusterListener] - 1 (LEADER)(OLDEST)Member(address = akka.tcp://ClusterSystem@127.0.0.1:2551, status = Up)
22:52:12.007 INFO  [ClusterSystem-akka.actor.default-dispatcher-23] [akka.tcp://ClusterSystem@127.0.0.1:2552/user/clusterListener] - 2 Member(address = akka.tcp://ClusterSystem@127.0.0.1:2552, status = Joining)
22:52:12.007 INFO  [ClusterSystem-akka.actor.default-dispatcher-23] [akka.tcp://ClusterSystem@127.0.0.1:2552/user/clusterListener] - 3 Member(address = akka.tcp://ClusterSystem@127.0.0.1:53206, status = Joining)
```

### Run a cluster (Mac, Linux)
