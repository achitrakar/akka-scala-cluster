package cluster

import akka.actor.{ActorLogging, ActorSystem}
import akka.cluster.{Cluster, Member, MemberStatus}
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.headers.RawHeader
import akka.http.scaladsl.model.{ContentTypes, HttpEntity}
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.server.Directives.{path, _}
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.scala.DefaultScalaModule

import java.io.StringWriter

case class HttpServer(actorSystem: ActorSystem) {

  def start(): Unit = {
    val port = memberPort(Cluster(actorSystem).selfMember)
    if (isValidPort(port)) {
      start(port + 7000)
    } else {
      val msg = s"HTTP server not started. Node port ${port} is invalid. The port must be >=2551 and <= 2559."
      println(msg) // TODO: Use proper logger here
      throw new Exception(msg)
    }
  }

  private def start(port: Int): Unit = {
    implicit val system = actorSystem
    Http(actorSystem).newServerAt("localhost", port).bind(route())
  }

  private def route(): Route = concat(
    get {
      concat(
        path(""){getFromResource("dashboard.html", ContentTypes.`text/html(UTF-8)`)},
        path("dashboard.html"){getFromResource("dashboard.html", ContentTypes.`text/html(UTF-8)`)},
        path("dashboard.js"){getFromResource("dashboard.js", ContentTypes.`application/json`)},
        path("p5.js"){getFromResource("p5.js", ContentTypes.`application/json`)},
        path("cluster-state") {respondWithHeaders(RawHeader("Access-Control-Allow-Origin", "*")) {complete(clusterState)}},

        path("greet") { complete(HttpEntity(ContentTypes.`text/html(UTF-8)`, "<html><body>Hello world!</body></html>"))},
        path("ping") {complete("PONG!")},
        path("crash") {sys.error("BOOM!")}
      )
    }
  )

  private def clusterState: String = loadNodes().toJSON

  private def memberPort(member: Member): Int = member.address.port.fold(0)(p => p)

  private def loadNodes(): Nodes = {
    import scala.collection.convert.ImplicitConversions.`iterable AsScalaIterable`

    val cluster = Cluster.get(actorSystem)
    val clusterState = cluster.state

    val unreachableMembers: Set[Member] = clusterState.getUnreachable.toSet

    val oldestMember = clusterState.getMembers
      .filter(member => member.status.equals(MemberStatus.Up))
      .filter(member => !unreachableMembers.contains(member))
      .foldLeft(cluster.selfMember){(older, member) =>
      older match {
        case old if old.isOlderThan(member) => old
        case _ => member
      }
    }

    val seedNodePorts = actorSystem.settings.config.getList("akka.cluster.seed-nodes")
      .map(_.unwrapped().toString)
      .map{ s =>
        val split = s.split(":")
        if (split.length== 0){
          0
        } else {
          split(split.length - 1).toInt
        }
      }.toList

    val nodes = Nodes(
      memberPort(cluster.selfMember),
      cluster.selfMember.address.equals(clusterState.getLeader),
      oldestMember.equals(cluster.selfMember)
    )

    clusterState.getMembers.foreach{ member =>
        nodes.add(member, leader(member), oldest(member), seedNode(member))
        def leader(member: Member): Boolean = member.address.equals(clusterState.getLeader)
        def oldest(member: Member): Boolean = oldestMember.equals(member)
        def seedNode(member: Member): Boolean = seedNodePorts.contains(memberPort(member))
    }

    clusterState.getUnreachable.foreach{member =>
      nodes.addUnreachable(member)
    }

    nodes
  }

  private def isValidPort(port: Int): Boolean = port >= 2551 && port <= 2559

  private def state(memberStatus: MemberStatus): String = {
    memberStatus match {
      case MemberStatus.Down => "down"
      case MemberStatus.Joining => "starting"
      case MemberStatus.WeaklyUp => "starting"
      case MemberStatus.Up => "up"
      case MemberStatus.Exiting => "stopping"
      case MemberStatus.Leaving => "stopping"
      case MemberStatus.Removed => "stopped"
      case _ => "offline"
    }
  }

  private def memberStatus(memberStatus: MemberStatus): String = {
    memberStatus match {
      case MemberStatus.Down => "down"
      case MemberStatus.Joining => "joining"
      case MemberStatus.WeaklyUp => "weaklyUp"
      case MemberStatus.Up => "up"
      case MemberStatus.Exiting => "exiting"
      case MemberStatus.Leaving => "leaving"
      case MemberStatus.Removed => "removed"
      case _ => "unknown"
    }
  }

  case class Nodes(
        selfPort: Int,
        leader: Boolean,
        oldest: Boolean,
        var nodes: List[Node] = List.empty
    ) extends Serializable {

    def add(member: Member, leader: Boolean, oldest: Boolean, seedNode: Boolean): Unit = {
      val port = memberPort(member)
      if (isValidPort(port)) {
        val node = Node(port, state(member.status), memberStatus(member.status), leader, oldest, seedNode)
        nodes = nodes :+ node
      }
    }

    // FIXME: TODO
    def addUnreachable(member: Member): Unit = {
      val port = memberPort(member)
      if (isValidPort(port)) {
        val node = Node(port, "unreachable", "unreachable", false, false, false)
        // node.remove(node)
        // node.add(node)
      }
    }

    def toJSON: String = {
      val mapper = new ObjectMapper()
      mapper.registerModule(DefaultScalaModule)
      val out = new StringWriter
      mapper.writeValue(out, this)
      out.toString
    }
  }

  case class Node(port:Int,
                  state: String,
                  memberState: String,
                  leader: Boolean,
                  oldest: Boolean,
                  seedNode: Boolean) extends Serializable {}
}

