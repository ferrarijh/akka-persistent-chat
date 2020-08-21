package tv.anypoint.jonathan.persistence

import akka.actor.ActorSystem
import akka.actor.Props
import com.typesafe.config.ConfigFactory
import tv.anypoint.jonathan.persistence.actors.ChatClient
import tv.anypoint.jonathan.persistence.actors.LoginListener

fun main(){
    println("Waiting for server response... Please wait until additional message appears...")
    val myConfig = ConfigFactory.parseString("akka.loglevel=OFF").withFallback(ConfigFactory.load())
    val system = ActorSystem.create("ClusterSystem", myConfig)
    val login = system.actorOf(Props.create(LoginListener::class.java), "login")
    val id = readLine()!!
    val client = system.actorOf(Props.create(ChatClient::class.java), id)
    runClient(client)
}
