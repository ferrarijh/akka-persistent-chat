import akka.actor.ActorSystem
import akka.actor.Props
import akka.pattern.Patterns.ask
import akka.pattern.Patterns.pipe
import com.typesafe.config.Config
import com.typesafe.config.ConfigFactory
import mu.KotlinLogging.logger
import tv.anypoint.jonathan.actor.*
import java.time.Duration

//PingActor
//1)displays 'received startMessage'
//2)then tell(PingMessage(), self) to actor of path "akka://chat-app/user/pong"
//

//PongActor
//1)displays "ping: $it" on log
//2)then tells(PongMessage(), self) to sender


fun main(){
    val config: Config = ConfigFactory.load("application")
    val system = ActorSystem.create("chat-app", config)
    val pingActor = system.actorOf(Props.create(PingActor::class.java))
    val pongActor = system.actorOf(Props.create(PongActor::class.java))

    val t = Duration.ofMillis(1000)
    val futurePingStart =
        ask(pingActor, StartMessage(), t).toCompletableFuture()
    //해당 actor가 누구한테 보내게 설정돼있든(의존성?) 상관없나?

    val futurePongStart =
        ask(pongActor, PingMessage(), t).toCompletableFuture()
    //ask가 return하는게 future.

    pipe(futurePingStart, system.dispatcher()).to(pongActor)
    pipe(futurePingStart, system.dispatcher()).to(pongActor)

    val pingRes = futurePingStart.get() as PingMessage
    val pongRes = futurePongStart.get() as PongMessage

    if (pingRes == PingMessage())
        logger("PingActor received StartMessage and sent PingMessage.")
    else
        logger("PingActor failed to send PingMessage.")

    if (pongRes == PongMessage())
        logger("PongActor received PingMessage and sent PongMessage.")
    else
        logger("PongActor failed to send PongMessage.")
}
