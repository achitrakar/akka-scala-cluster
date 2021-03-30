package cluster

import akka.Done
import akka.management.scaladsl.AkkaManagement
import akka.actor.{ActorSystem, CoordinatedShutdown, Props}
import akka.cluster.Cluster
import com.typesafe.config.{Config, ConfigFactory}
import org.slf4j.LoggerFactory

import scala.concurrent.Future

object AppMain {
  val logger = LoggerFactory.getLogger(this.getClass.getCanonicalName)
  def main(args: Array[String]): Unit = {
    logger.info("First line in running application.")
    args match {
      case list if list.isEmpty => startupClusterNodes(List("2551", "2552", "0"))
      case list => startupClusterNodes(list)
    }
  }

  def startupClusterNodes(ports: Seq[String]) = {
    logger.debug(s"Start cluster on port(s) ${ports}")
    ports.foreach { port =>

      // Create an Actor system
      val system = ActorSystem("ClusterSystem", setupClusterNodeConfig(port))

      AkkaManagement(system).start()
      HttpServer(system).start()

      system.actorOf(Props[ClusterListenerActor], "clusterListener")

      addCoordinatedShutdownTask(system, CoordinatedShutdown.PhaseClusterShutdown)
      system.log.info(s"Akka node ${Cluster(system).selfAddress.hostPort}")
    }
  }

  private def setupClusterNodeConfig(port: String): Config = {
    val configStr =
      s"""
         |akka.remote.netty.tcp.port=$port
         |akka.remote.artery.canonical.port=$port
         |akka.management.http.port=855${port.last}
         |""".stripMargin
    ConfigFactory.parseString(configStr)
      .withFallback(ConfigFactory.load())
  }

  private def addCoordinatedShutdownTask(system: ActorSystem, coordinatedShutdownPhase: String): Unit = {
    CoordinatedShutdown(system).addTask(
      coordinatedShutdownPhase,
      coordinatedShutdownPhase
    ) { () =>
      system.log.warning(s"Coordinated shutdown phase ${coordinatedShutdownPhase}")
      Future.successful(Done.getInstance())
    }
  }
}
