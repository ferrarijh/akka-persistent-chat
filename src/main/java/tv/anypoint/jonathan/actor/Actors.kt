package tv.anypoint.jonathan.actor

import akka.actor.AbstractActor
import mu.KLogging

class StartMessage
class PingMessage
class PongMessage

class PingActor : AbstractActor() {
    override fun preStart() {
        logger.info("PingActor ,${self.path()}")
    }

    override fun createReceive(): Receive = receiveBuilder()
        .match(StartMessage::class.java) {
            context.system.actorSelection("akka://chat-app/user/pong").tell(PingMessage(), self)
            context.system.actorSelection("akka://chat-app/user/pong").tell(PingMessage(), self)
        }
        .match(PongMessage::class.java) {
            logger.info("pong: $it")
        }
        .build()

    companion object : KLogging()
}

class PongActor : AbstractActor() {
    override fun preStart() {
        logger.info("PongActor ,${self.path()}")
    }

    override fun createReceive(): Receive = receiveBuilder()
        .match(PingMessage::class.java) {
            logger.info("ping: $it")
            sender.tell(PongMessage(), self)
        }
        .build()

    companion object : KLogging()
}
