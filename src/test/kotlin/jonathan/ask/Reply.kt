package jonathan.ask

import akka.actor.ActorRef
import akka.actor.ActorSystem
import akka.actor.Props
import com.typesafe.config.ConfigFactory
import akka.pattern.Patterns.ask
import java.time.Duration
import java.util.concurrent.CompletableFuture

fun main(){
    val myConfig = ConfigFactory.parseString("akka.remote.netty.tcp.port=2551")
        .withFallback(ConfigFactory.load())
    val sys = ActorSystem.create("ClusterSystem", myConfig)
    val r = sys.actorOf(Props.create(Replier::class.java), "replier")
}
