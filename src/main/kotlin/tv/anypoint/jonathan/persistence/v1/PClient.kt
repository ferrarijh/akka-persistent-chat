package tv.anypoint.jonathan.persistence.v1

import akka.actor.ActorRef
import akka.actor.ActorSystem
import akka.actor.Props
import com.typesafe.config.ConfigFactory
import tv.anypoint.jonathan.persistence.v1.actors.ChatClient
import tv.anypoint.jonathan.persistence.v1.actors.LoginListener

fun main(){
    println("Waiting for server response... Please wait until additional message appears...")
    val myConfig = ConfigFactory.parseString("akka.loglevel=OFF").withFallback(ConfigFactory.load())
    val system = ActorSystem.create("ClusterSystem", myConfig)
    val login = system.actorOf(Props.create(LoginListener::class.java), "login")
    val id = readLine()!!
    val client = system.actorOf(Props.create(ChatClient::class.java), id)
    runClient(client)
}

fun runClient(clientRef: ActorRef){
    println("*--- 1)type 'connect' to join chat. 2)type 'users' to see list of current users. 3)type 'bye' to exit.")
    print(">> ")
    var buf: String
    do{
        buf = readLine()!!
        clientRef.tell(buf, ActorRef.noSender())
    }while(buf != "bye")
}
