package cluster

import akka.actor.{Actor, ActorLogging}
import akka.cluster.{Cluster, Member}
import akka.cluster.ClusterEvent.{CurrentClusterState, InitialStateAsEvents, MemberEvent}

class ClusterListenerActor extends Actor with ActorLogging {

  val cluster = Cluster(context.system)

  // subscribe to cluster changes, re-subscribe when restart
  override def preStart(): Unit = {
    log.debug("Start");
    cluster.subscribe(self, initialStateMode=InitialStateAsEvents, classOf[MemberEvent])
  }

  override def postStop(): Unit = {
    log.debug("Stop")
    cluster.unsubscribe(self)
  }

  override def receive: Receive = {
    case msg: MemberEvent => logClusterEvent(msg)
    case _ => // Ignore
  }

  private def logClusterEvent(msg: MemberEvent): Unit = {
    log.info("{} sent to {}", msg, cluster.selfMember);
    logClusterMembers()
  }

  private def logClusterMembers(): Unit = {
    logClusterMembers(cluster.state)
  }

  private def logClusterMembers(currentClusterState: CurrentClusterState): Unit ={
    import scala.collection.convert.ImplicitConversions.`iterable AsScalaIterable`

    val oldestMember = currentClusterState.getMembers.foldLeft(cluster.selfMember){(older, member) =>
      older match {
        case old if old.isOlderThan(member) => old
        case _ => member
      }
    }

    currentClusterState.getMembers.zip(Stream from 1).foreach{
      case (member, counter) => log.info("{} {}{}{}", counter, leader(member), oldest(member), member)
        def leader(member: Member): String = if (member.address.equals(currentClusterState.getLeader)) "(LEADER)" else ""
        def oldest(member: Member): String = if (oldestMember.equals(member)) "(OLDEST)" else ""
    }
  }
}
