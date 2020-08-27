package tv.anypoint.jonathan.pubsub

import akka.actor.ActorRef
import akka.actor.ActorSystem
import akka.actor.Props
import com.typesafe.config.ConfigFactory
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking

fun main() = runBlocking<Unit>{
    val system = ActorSystem.create("ClusterSystem", ConfigFactory.load())

    delay(10000L)
    print(">> User name: ")
    val username = readLine() as String
    val user = system.actorOf(Props.create(User::class.java, username), "destination")

    var input: String
    do {
        print(">> [me]")
        input = readLine() as String
        user.tell(input, ActorRef.noSender())
    } while (true)
}
