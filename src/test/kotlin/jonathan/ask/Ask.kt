package jonathan.ask

import akka.actor.ActorRef
import akka.actor.ActorSelection
import akka.actor.ActorSystem
import akka.actor.Props
import com.typesafe.config.ConfigFactory
import akka.pattern.Patterns.ask

fun main(){
    /*
    val sys = ActorSystem.create("ClusterSystem", ConfigFactory.load())
    val a = sys.actorOf(Props.create(Asker::class.java), "asker")
    var buf: String
    while(true) {
        print(">>")
        buf = readLine()!!
        a.tell("ask", ActorRef.noSender())
        if (buf == "ask")
            println("ask sent.")
    }

     */
}
