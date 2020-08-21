package tv.anypoint.jonathan.pingpong.actor

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
            logger.info("received startMessage")
            context.system.actorSelection("akka://chat-app/user/pong").tell(PingMessage(), self)
        }
        .match(PongMessage::class.java) {
            logger.info("pong: $it")
            logger.info("received PongMessage.")
            logger.info("Actor System terminated.")
            context.system.terminate()
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
            logger.info("received PingMessage.")
            sender.tell(PongMessage(), self)
        }
        .build()

    companion object : KLogging()
}
