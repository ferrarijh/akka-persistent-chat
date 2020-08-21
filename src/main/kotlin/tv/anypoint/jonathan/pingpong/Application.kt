package tv.anypoint.jonathan.pingpong

import akka.actor.ActorRef
import akka.actor.ActorSystem
import akka.actor.Props
import akka.cluster.Cluster
import com.typesafe.config.Config
import com.typesafe.config.ConfigFactory
import mu.KLogging
import tv.anypoint.jonathan.pingpong.actor.PingActor
import tv.anypoint.jonathan.pingpong.actor.PongActor
import tv.anypoint.jonathan.pingpong.actor.StartMessage

class Application {
    companion object : KLogging()
}

fun main() {
    Application.logger.info("start application")
    val config: Config = ConfigFactory.load("application")
    val actorSystem = ActorSystem.create("chat-app", config)
    val cluster  = Cluster.get(actorSystem)
    Application.logger.info("roles: ${cluster.selfMember().roles}")

//    if (cluster.selfMember().hasRole("master")) {
//
//    } else {
//
//    }

    val pingActorRef = actorSystem.actorOf(Props.create(PingActor::class.java), "ping")
    val pongActorRef = actorSystem.actorOf(Props.create(PongActor::class.java), "pong")

    //val k = KLogging()
    //val path1 = pingActorRef.path().toString()
    //k.logger.info(">> pingActor : $path1")

    pingActorRef.tell(StartMessage(), ActorRef.noSender())
}
