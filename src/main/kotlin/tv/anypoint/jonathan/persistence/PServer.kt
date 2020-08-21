package tv.anypoint.jonathan.persistence

import akka.actor.ActorRef
import akka.actor.ActorSystem
import akka.actor.Props
import com.typesafe.config.ConfigFactory
import tv.anypoint.jonathan.persistence.actors.ChatPersistentServer
import tv.anypoint.jonathan.persistence.actors.ChatState

fun main(){
    val system = ActorSystem.create("ClusterSystem", ConfigFactory.load())
    val defState =
        ChatState(mutableMapOf(), 0)
    val server = system.actorOf(Props.create(ChatPersistentServer::class.java, defState), "server")
    var buf: String
    while(true){
        print(">> ")
        buf = readLine()!!
        server.tell(buf, ActorRef.noSender())
    }
}
