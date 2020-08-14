package ask

import akka.actor.ActorSystem
import akka.actor.Props
import com.typesafe.config.ConfigFactory

fun main(){
    val sys = ActorSystem.create("ClusterSystem", ConfigFactory.load())
    val a = sys.actorOf(Props.create(Replier::class.java), "replier")
}
