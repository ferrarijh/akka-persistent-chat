package tv.anypoint.jonathan.pubsub

import akka.actor.AbstractActor
import akka.actor.ActorRef
import akka.cluster.pubsub.DistributedPubSub
import akka.cluster.pubsub.DistributedPubSubMediator
import mu.KLogging
import java.io.Serializable

abstract class AbstractActorKL: AbstractActor(){
    companion object: KLogging()
}

class UserMessage(val from: String, val content: String): Serializable
class ConnectAck(val name: String): Serializable
class Bye(val name: String): Serializable

class User(private var name: String): AbstractActorKL(){  //User Actor sends & displays message
    private val connected = mutableMapOf<String, Boolean>()
    private val path: String = "/user/destination"
    private val mediator: ActorRef = DistributedPubSub.get(context.system).mediator()
    init {
        mediator.tell(DistributedPubSubMediator.Put(self), self)
    }

    override fun preStart() {
        super.preStart()
        mediator.tell(DistributedPubSubMediator.SendToAll(path,
            ConnectAck(name), true), self)
    }
    override fun createReceive(): Receive = receiveBuilder()
        .match(ConnectAck::class.java) {
            if (connected[it.name] != true) {
                connected[it.name] = true
                println("\r>> new user '${it.name}' connected!")
                print(">> [me]")
            }
        }.match(String::class.java){
            if (it=="bye"){
                mediator.tell(DistributedPubSubMediator.SendToAll(path,
                    Bye(name), true), self)
                print(">> See you again!")
                context.system.terminate()
            }else {
                val msg = UserMessage(name, it)
                mediator.tell(DistributedPubSubMediator.SendToAll(path, msg, true), self)
            }
        }.match(UserMessage::class.java) {
            println("\r>> [${it.from}] ${it.content}")
            print(">> [me]")
        }.match(Bye::class.java){
            println("\r>> user '${it.name}' disconnected.")
            print(">> [me]")
            connected[it.name] = false
        }.build()
}

/*--- below are just for reference :) ---*/


open class Subscriber: AbstractActorKL() {
    init{
        val mediator = DistributedPubSub.get(context.system).mediator()
        mediator.tell(DistributedPubSubMediator.Subscribe("topic", self), self)
    }

    override fun createReceive(): Receive = receiveBuilder()
        .match(DistributedPubSubMediator.SubscribeAck::class.java){
            logger.info(">> ${self.path().toString()} subscribed successfully.")
        }.match(String::class.java){
            logger.info(">> [${self.path().name()}]$it")
        }.build()
}

open class Publisher: AbstractActor(){
    protected val mediator: ActorRef = DistributedPubSub.get(context.system).mediator()
    override fun createReceive(): Receive = receiveBuilder()
        .match(String::class.java){
            mediator.tell(DistributedPubSubMediator.Publish("topic", it), self)
        }.build()
}

open class PrivateSubscriber: AbstractActorKL(){
    init{
        //mediator에 subscribe 신청
        val mediator = DistributedPubSub.get(context.system).mediator()
        mediator.tell(DistributedPubSubMediator.Put(self), self)
    }
    override fun createReceive(): Receive = receiveBuilder()
        .match(DistributedPubSubMediator.SubscribeAck::class.java){
            logger.info(">> [Private] Successfully put ${self.path().toString()}.")
        }
        .match(String::class.java){
            logger.info(">> [Private] $it")
        }.build()
}

open class PrivateSender: AbstractActorKL(){ //"DistributedPubSubMediator.Send" is for point-to-point.
    protected val mediator = DistributedPubSub.get(context.system).mediator()
    override fun createReceive(): Receive = receiveBuilder()
        .match(String::class.java) {
            mediator.tell(DistributedPubSubMediator.Send("/user/dest", it.toUpperCase(), true)
                , self)
        }.build()
}
