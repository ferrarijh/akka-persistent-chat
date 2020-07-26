import akka.actor.ActorRef
import akka.actor.ActorSystem
import akka.actor.Props
import akka.testkit.javadsl.TestKit //NOT akka.testkit.TestKit
import com.typesafe.config.Config
import com.typesafe.config.ConfigFactory
import org.junit.Test
import akka.pattern.Patterns.ask
import akka.pattern.Patterns.pipe
import akka.testkit.TestProbe
import tv.anypoint.jonathan.actor.*
import java.time.Duration

//pipe(future, system.dispatcher()).to(myActor) sends future's result to myActor.
//ask() requires the recipient to reply with 'getSender.tell(reply, self)' -> only for testPongActor?

class TestPingPong() {
    private val config: Config = ConfigFactory.load("application")
    private val system: ActorSystem = ActorSystem.create("chat-app", config)
    private val t: Duration = Duration.ofSeconds(1)

    @Test
    fun testPingActor() {
        object : TestKit(system){
            init{
                val startMessage = StartMessage()
                val probe = TestKit(system)
                val targetPingActor = system.actorOf(Props.create(PingActor::class.java))
                val pongActor = system.actorOf(Props.create(PongActor::class.java)) //pong actor not receiving message..
                pongActor.tell(probe.ref, ref)

                targetPingActor.tell(startMessage, ActorRef.noSender())
                probe.expectMsgClass(PingMessage::class.java)
            }
        }
    }
    @Test
    fun testPongActor() {
        object : TestKit(system) {
            init {
                //val probe = TestKit(system)
                //val pingMessage = PingMessage()
                val targetPongActor = system.actorOf(Props.create(PongActor::class.java), "pongActor")

                //===test input
                //targetPongActor.tell(probe.ref, ref)      //inject probe to targetPongActor
                //targetPongActor.tell(pingMessage, ref)    //test input not working..
                //probe.expectMsg(pingMessage)

                //===test output
                val future = ask(targetPongActor, PingMessage(), t).toCompletableFuture()
                pipe(future, this.system.dispatcher()).to(ref)  //'this' necessary?
                //targetPongActor.tell(pingMessage, ref)    //한 줄로 대체해도 될 듯?

                expectMsgClass(PongMessage::class.java) //test output
            }
        }
    }
}

fun main(){
    TestPingPong().testPongActor()
}
