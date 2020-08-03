package pubSubPractice

import akka.actor.AbstractActor
import akka.actor.ActorRef
import akka.cluster.Cluster
import akka.cluster.ClusterEvent
import akka.cluster.pubsub.DistributedPubSub
import akka.cluster.pubsub.DistributedPubSubMediator
import mu.KLogging

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
    /*
    private val cluster = Cluster.get(context.system)

    override fun preStart() = cluster.subscribe(self, ClusterEvent.initialStateAsEvents(), ClusterEvent.MemberEvent::class.java,
        ClusterEvent.UnreachableMember::class.java)
     */
    override fun createReceive(): Receive = receiveBuilder()
        .match(String::class.java){
            mediator.tell(DistributedPubSubMediator.Publish("topic", it), self)
        }.build()
}

class PrivateSubscriber: AbstractActorKL(){
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

class PrivateSender: AbstractActorKL(){ //"DistributedPubSubMediator.Send" is for point-to-point.
    private val mediator = DistributedPubSub.get(context.system).mediator()
    override fun createReceive(): Receive = receiveBuilder()
        .match(String::class.java) {
            mediator.tell(DistributedPubSubMediator.Send("/user/dest", it.toUpperCase(), true)
                , self)
        }.build()
}

class User: Publisher(){
    override fun createReceive() = receiveBuilder()
        .match(DistributedPubSubMediator.SubscribeAck::class.java) {
            mediator.tell(DistributedPubSubMediator.Publish("topic", "${self.path().name()} joined chat!"), self)
        }.match(String::class.java){
            mediator.tell(DistributedPubSubMediator.Publish("topic", "[${self.path().name()}] $it"), self)
        }.build()
}
class Displayer: Subscriber(){
    override fun createReceive(): Receive = receiveBuilder()
        .match(DistributedPubSubMediator.SubscribeAck::class.java){
            logger.info(">> displayer [${self.path().toString()}] subscribed successfully.")
        }.match(String::class.java){
            logger.info(">> $it")
        }.build()
}

/*
fun configWithArteryPort(port: Int): Config{
    val overrides = mutableMapOf<String, Any>()
    overrides["akka.remote.artery.canonical.port"] = port
    return ConfigFactory.parseMap(overrides).withFallback(ConfigFactory.load())
}
 */
