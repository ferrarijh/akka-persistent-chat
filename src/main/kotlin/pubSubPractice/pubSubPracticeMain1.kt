package pubSubPractice

import akka.actor.ActorRef
import akka.actor.ActorSystem
import akka.actor.Props
import com.typesafe.config.ConfigFactory
import kotlinx.coroutines.*
import mu.KLogging


fun main() = runBlocking<Unit> {
    val k = KLogging()

    val system1 = ActorSystem.create("ClusterSystem", ConfigFactory.load())
    val displayer1 = system1.actorOf(Props.create(Displayer::class.java))
    //val sender = system1.actorOf(Props.create(PrivateSender::class.java), "sender")

    delay(10000L)
    print(">> User name: ")
    val str = readLine() as String
    val user1 = system1.actorOf(Props.create(User::class.java), str)

    //displayer not displaying joining log..
    var input: String
    do {
        input = readLine() as String
        user1.tell(input, ActorRef.noSender())
    } while (input != "bye")

    system1.terminate()
}

