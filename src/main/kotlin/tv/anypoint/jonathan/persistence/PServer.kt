package tv.anypoint.jonathan.persistence

import akka.actor.ActorSystem
import akka.actor.Props
import com.typesafe.config.ConfigFactory

fun main(){
    val system = ActorSystem.create("ClusterSystem", ConfigFactory.load())
    val defState = ChatState(mutableMapOf<String, Int>(), -1)
    val server = system.actorOf(Props.create(ChatPersistentServer::class.java, defState), "server")
}
