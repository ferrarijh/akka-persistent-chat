package tv.anypoint.jonathan

import akka.actor.ActorRef
import akka.actor.ActorSystem
import akka.actor.Props
import tv.anypoint.jonathan.actor.PingActor
import tv.anypoint.jonathan.actor.PongActor
import tv.anypoint.jonathan.actor.StartMessage

class Application

fun main() {
    val actorSystem = ActorSystem.create("chat-app")
    val pingActorRef = actorSystem.actorOf(Props.create(PingActor::class.java))
    val pongActorRef = actorSystem.actorOf(Props.create(PongActor::class.java))

    pingActorRef.tell(StartMessage(), ActorRef.noSender())
}
