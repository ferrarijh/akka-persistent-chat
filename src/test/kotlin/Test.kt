import akka.actor.*
import akka.testkit.javadsl.TestKit //NOT akka.testkit.TestKit
import com.typesafe.config.Config
import com.typesafe.config.ConfigFactory
import org.junit.Test
import akka.pattern.Patterns.ask
import akka.pattern.Patterns.pipe
import io.kotest.matchers.shouldBe
import mu.KLogging
import tv.anypoint.jonathan.actor.*
import java.time.Duration

//pipe(future, system.dispatcher()).to(myActor) sends future's result to myActor.
//ask() requires the recipient to reply with 'getSender.tell(reply, self)' -> only for testPongActor?

class TestPingPong() {
    @Test
    fun testPingActor() {   //not finished..
        val config: Config = ConfigFactory.load("application")
        val system: ActorSystem = ActorSystem.create("chat-app", config)
        val t: Duration = Duration.ofSeconds(1)
        object : TestKit(system){
            init{
                val targetPingActor: ActorRef = system.actorOf(Props.create(PingActor::class.java))
                //val pongActor: ActorRef = system.actorOf(Props.create(PongActor::class.java), "pong")

                val startMessage = StartMessage()
                val inputProbe = TestKit(system)

                val outputProbe = TestKit(system)
                targetPingActor.tell(inputProbe.ref, ref)
                //pongActor.tell(outputProbe.ref, ref)

                /*===thenApply 실행x
                ask(targetPingActor, startMessage, t).toCompletableFuture()
                    .thenApply {
                        inputProbe.expectMsgClass(PingMessage::class.java)  //Should be StartMessage::class.java
                    }.thenApply {
                        outputProbe.expectMsgClass(PongMessage::class.java) //should be PingMessage::class.java
                        println("111")
                    }
                 */

                //onComplete.join()     //AskTimeoutException??

                //
                //targetPingActor.tell(startMessage, ref)
                //inputProbe.expectMsgClass(PingMessage::class.java)  //should be StartMessage::class.java
                //---메세지 처리되긴하는데 probe가 못 잡음. testPongActor에선 됐는데 왜 여기선 안 됨?
                //

                //--daed letter 사용할 수 있나?
                //val deadLettersRef = system.deadLetters() //why can't I use this?
                //deadLettersRef.tell(inputProbe.ref, ref)
                //targetPingActor.tell()
                //deadLettersRef.
                //
            }
        }
        system.terminate()
    }
    @Test
    fun testPongActor() {
        val config: Config = ConfigFactory.load("application")
        val system: ActorSystem = ActorSystem.create("chat-app", config)
        val t: Duration = Duration.ofSeconds(1)
        object : TestKit(system) {
            init {
                val testProbe = TestKit(system)

                val pingMessage = PingMessage()
                val targetPongActor = system.actorOf(Props.create(PongActor::class.java), "pong")

                //===test input
                //targetPongActor.tell(probe.ref, ref)      //inject probe to targetPongActor
                //targetPongActor.tell(pingMessage, ref)    //test input not working..
                //probe.expectMsg(pingMessage)

                //===test output
                val expect = PongMessage()
                val future = ask(targetPongActor, PingMessage(), t).toCompletableFuture()
                val actual = future.get()
                actual shouldBe expect

                pipe(future, this.system.dispatcher()).to(testProbe.ref)  //'this' necessary?
                //targetPongActor.tell(pingMessage, ref)    //한 줄로 대체해도 될 듯?

                testProbe.expectMsgClass(PongMessage::class.java)     //should be PongMessage::class.java
            }
        }
        system.terminate()
    }
    companion object: KLogging()
}

fun main(){

}
