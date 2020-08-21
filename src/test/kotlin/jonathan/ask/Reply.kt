package jonathan.ask

import akka.actor.ActorRef
import akka.actor.ActorSystem
import akka.actor.Props
import com.typesafe.config.ConfigFactory
import akka.pattern.Patterns.ask
import java.time.Duration
import java.util.concurrent.CompletableFuture

fun main(){
    val sys = ActorSystem.create("ClusterSystem", ConfigFactory.load())
    val r = sys.actorOf(Props.create(Replier::class.java), "replier")
    //a.tell(BirthMessage(), ActorRef.noSender())


    //val sel = sys.actorSelection("/user/*")
    //sel.tell(Reveal(), ActorRef.noSender())
}
