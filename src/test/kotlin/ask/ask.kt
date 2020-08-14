package ask

import akka.actor.ActorRef
import akka.actor.ActorSystem
import akka.actor.Props
import com.typesafe.config.ConfigFactory

fun main(){
    val sys = ActorSystem.create("ClusterSystem", ConfigFactory.load())
    val a = sys.actorOf(Props.create(Asker::class.java), "asker")
    var buf: String = ""
    while(true) {
        buf = readLine()!!
        a.tell("ask", ActorRef.noSender())
        if (buf == "ask")
            println("ask sent.")
    }
}
