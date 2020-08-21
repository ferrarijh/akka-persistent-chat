import akka.actor.AbstractActor
import akka.actor.ActorSystem
import akka.actor.Props
import akka.cluster.Cluster
import akka.cluster.ClusterEvent
import com.typesafe.config.Config
import com.typesafe.config.ConfigFactory
import mu.KLogging


fun main(){
    try {
        val sys2551 = ActorSystem.create("ClusterSystem", configWithPort(2551))
        val actor2551 = sys2551.actorOf(Props.create(MyClusterListener::class.java))
        val sys2552 = ActorSystem.create("ClusterSystem", configWithPort(2552))
        sys2552.actorOf(Props.create(MyClusterListener::class.java))
        val sysDef = ActorSystem.create("ClusterSystem", configWithPort(0))
        sysDef.actorOf(Props.create(MyClusterListener::class.java))

        val k = KLogging()
        val path = actor2551.path().toString()
        k.logger.info(">>path : $path")
    }catch(e: Exception){
        e.printStackTrace()
    }
}

fun configWithPort(port: Int): Config{
    val overrides = mutableMapOf<String, Any>()
    overrides["akka.remote.netty.tcp.port"] = port
    return ConfigFactory.parseMap(overrides).withFallback(ConfigFactory.load())
}

open class MyClusterListener: AbstractActor(){
    companion object: KLogging()
    private val cluster = Cluster.get(context.system)

    override fun preStart() = cluster.subscribe(self, ClusterEvent.initialStateAsEvents(), ClusterEvent.MemberEvent::class.java,
        ClusterEvent.UnreachableMember::class.java)
    //what are the latter 2 param.s?

    override fun postStop() = cluster.unsubscribe(self)

    override fun createReceive(): Receive = receiveBuilder()
        .match(ClusterEvent.MemberUp::class.java){
            logger.info("Member is Up: $it")
        }.match(ClusterEvent.UnreachableMember::class.java){
            logger.info("Member is Unreachable: $it")
        }.match(ClusterEvent.MemberRemoved::class.java) {
            logger.info("Member is Removed: $it")
        }.match(ClusterEvent.MemberEvent::class.java){
                messsage -> //ignore
        }.build()
}
