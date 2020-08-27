package tv.anypoint.jonathan.persistence.v1

import akka.actor.ActorRef
import akka.actor.ActorSystem
import akka.actor.Props
import com.typesafe.config.ConfigFactory
import tv.anypoint.jonathan.persistence.v1.actors.ChatPersistentServer
import tv.anypoint.jonathan.persistence.v1.actors.ChatState

fun main(){
    val myconfig = ConfigFactory.parseString("akka.loglevel=DEBUG").withFallback(ConfigFactory.load())
    val system = ActorSystem.create("ClusterSystem", myconfig)
    val defState = ChatState(mutableMapOf(), 0)
    val server = system.actorOf(Props.create(ChatPersistentServer::class.java, defState), "server")
    var buf: String
    while(true){
        print(">> ")
        buf = readLine()!!
        server.tell(buf, ActorRef.noSender())
    }
}
