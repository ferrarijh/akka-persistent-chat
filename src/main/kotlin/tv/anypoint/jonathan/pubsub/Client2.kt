package tv.anypoint.jonathan.pubsub

import akka.actor.ActorRef
import akka.actor.ActorSystem
import akka.actor.Props
import com.typesafe.config.ConfigFactory
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import mu.KLogging


fun main() = runBlocking<Unit>{
    val k = KLogging()

    val system = ActorSystem.create("ClusterSystem", ConfigFactory.load())

    delay(10000L)
    print(">> User name: ")
    val username = readLine() as String
    val user = system.actorOf(Props.create(User::class.java, username), "destination")

    //displayer not displaying joining log..
    var input: String
    do {
        print(">> [me]")
        input = readLine() as String
        user.tell(input, ActorRef.noSender())
    } while (input != "bye")

    system.terminate()
}
