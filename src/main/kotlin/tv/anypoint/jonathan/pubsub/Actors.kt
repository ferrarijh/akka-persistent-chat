package tv.anypoint.jonathan.pubsub

import akka.actor.AbstractActor
import akka.actor.ActorRef
import akka.cluster.pubsub.DistributedPubSub
import akka.cluster.pubsub.DistributedPubSubMediator
import mu.KLogging
import tv.anypoint.jonathan.message.MySerializable
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

class UserMessage(val from: String, val content: String): MySerializable
/*
class LoginMessage()

class Server: AbstractActorKL(){    //server should be UP first.

}
 */

class User(val name: String): AbstractActorKL(){  //User Actor sends & displays message
    private val mediator: ActorRef = DistributedPubSub.get(context.system).mediator()
    init {
        mediator.tell(DistributedPubSubMediator.Put(self), self)
    }

    override fun createReceive() = receiveBuilder()
        .match(DistributedPubSubMediator.SubscribeAck::class.java) {
            println(">> displayer [${self.path().toString()}] subscribed successfully.")    //first, inform subscription to self
            val msg = UserMessage("", "$name joined chat!")
            mediator.tell(DistributedPubSubMediator.SendToAll("/user/destination", msg, true), self)    //then, inform others
        }.match(String::class.java){
            val msg = UserMessage(name, it)
            mediator.tell(DistributedPubSubMediator.SendToAll("/user/destination", msg, true), self)
        }.match(UserMessage::class.java){
            println("\r>> [${it.from}] ${it.content}")
            print(">> [me]")
        }.build()
}
