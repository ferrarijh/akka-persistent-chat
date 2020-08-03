package pubSubPractice

import akka.actor.ActorRef
import akka.actor.ActorSystem
import akka.actor.Props
import com.typesafe.config.ConfigFactory
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import mu.KLogging


fun main() = runBlocking<Unit>{
    val k = KLogging()

    val system2 = ActorSystem.create("ClusterSystem", ConfigFactory.load())
    val displayer2 = system2.actorOf(Props.create(Displayer::class.java), "destination")
    //val destination = system2.actorOf(Props.create(PrivateSubscriber::class.java), "dest")

    delay(10000L)
    print(">> User name: ")
    val str = readLine() as String
    val user2 = system2.actorOf(Props.create(User::class.java, str))

    //displayer not displaying joining log..
    var input: String
    do {
        print(">> [me]")
        input = readLine() as String
        user2.tell(input, ActorRef.noSender())
    } while (input != "bye")

    system2.terminate()
}
