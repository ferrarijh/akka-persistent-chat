package pubSubPractice

import akka.actor.AbstractActor
import akka.actor.ActorRef
import akka.cluster.pubsub.DistributedPubSub
import akka.cluster.pubsub.DistributedPubSubMediator
import akka.serialization.JSerializer
import mu.KLogging
import java.io.Serializable

abstract class AbstractActorKL: AbstractActor(){
    companion object: KLogging()
}

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

interface MySerializable{}
class UserMessage(val from: String, val content: String): Serializable

class User(val name: String): AbstractActor(){  //Sender
    val mediator = DistributedPubSub.get(context.system).mediator()

    override fun createReceive() = receiveBuilder()
        .match(String::class.java){
            val msg = UserMessage(name, it)
            mediator.tell(DistributedPubSubMediator.SendToAll("/user/destination", msg, true), self)
        }.build()
}

class Displayer: AbstractActorKL(){  //Destination actor should be created with actorOf(props, "destination")
    init {
        val mediator = DistributedPubSub.get(context.system).mediator()
        mediator.tell(DistributedPubSubMediator.Put(self), self)
    }
    override fun createReceive(): Receive = receiveBuilder()
        .match(DistributedPubSubMediator.SubscribeAck::class.java){
            logger.info(">> displayer [${self.path().toString()}] subscribed successfully.")
        }.match(UserMessage::class.java){
            for(i in 1..100)
                print("\b")
            println(">> [${it.from}] ${it.content}")
            print(">> [me]")
        }.build()
}
