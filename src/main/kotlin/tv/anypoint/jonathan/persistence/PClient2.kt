package tv.anypoint.jonathan.persistence

import akka.actor.ActorRef
import akka.actor.ActorSystem
import akka.actor.Props
import com.typesafe.config.ConfigFactory
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking

fun main() = runBlocking<Unit>{
    val system = ActorSystem.create("ClusterSystem", ConfigFactory.load())
    delay(10000)
    print(">> Your name is: ")
    var buf = readLine()
    print(">> ")
    val client = system.actorOf(Props.create(ChatClient::class.java), buf)
    outer@ while(true){
        buf = readLine()
        when(buf){
            "bye" -> break@outer
            else -> client.tell(buf, ActorRef.noSender())
        }
    }
}
