package tv.anypoint.jonathan

import akka.actor.ActorRef
import akka.actor.ActorSystem
import akka.actor.Props
import com.typesafe.config.Config
import com.typesafe.config.ConfigFactory
import mu.KLogging
import tv.anypoint.jonathan.actor.PingActor
import tv.anypoint.jonathan.actor.PongActor
import tv.anypoint.jonathan.actor.StartMessage

class Application {
    companion object : KLogging()
}

fun main() {
    Application.logger.info("start application")
    val config: Config = ConfigFactory.load("application")
    val actorSystem = ActorSystem.create("chat-app", config)
    val pingActorRef = actorSystem.actorOf(Props.create(PingActor::class.java), "ping")
    val pongActorRef = actorSystem.actorOf(Props.create(PongActor::class.java), "pong")

    pingActorRef.tell(StartMessage(), ActorRef.noSender())
}
