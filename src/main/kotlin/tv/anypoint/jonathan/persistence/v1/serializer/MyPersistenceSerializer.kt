package tv.anypoint.jonathan.persistence.v1.serializer

import akka.serialization.SerializerWithStringManifest
import tv.anypoint.jonathan.serialization.MyPersistenceMessages.*

class MyPersistenceSerializer: SerializerWithStringManifest() {
    override fun manifest(o: Any): String = o.javaClass.name

    private val PRECONNECTD_MANIFEST = PreConnected::class.java.name

    private val CHATMESSAGE_MANIFEST = ChatMessage::class.java.name
    private val RECEIVEACK_MANIFEST = ReceiveAck::class.java.name

    private val JOINMESSAGE_MANIFEST = JoinMessage::class.java.name
    private val BYEMESSAGE_MANIFEST = ByeMessage::class.java.name

    //initiate connection with 3 way handshake
    private val CONNECTREQ_MANIFEST = ConnectReq::class.java.name
    private val CONNECTREQACK_MANIFEST = ConnectReqAck::class.java.name
    private val CONNECTACK_MANIFEST = ConnectAck::class.java.name

    private val ASKCURRENTUSERS_MANIFEST = AskCurrentUsers::class.java.name
    private val CURRENTUSERS_MANIFEST = CurrentUsers::class.java.name

    private val DEBUG_MANIFEST = ByeMessage::class.java.name

    override fun identifier(): Int = 940524

    override fun toBinary(o: Any): ByteArray = when(o){
        is PreConnected -> o.toByteArray()
        is ChatMessage -> o.toByteArray()
        is JoinMessage -> o.toByteArray()
        is ByeMessage -> o.toByteArray()
        is ReceiveAck -> o.toByteArray()
        is ConnectReq -> o.toByteArray()
        is ConnectReqAck -> o.toByteArray()
        is ConnectAck -> o.toByteArray()
        is AskCurrentUsers -> o.toByteArray()
        is CurrentUsers -> o.toByteArray()
        is Debug -> o.toByteArray()
        else -> throw IllegalArgumentException("failed to serialize into bytes.")
    }

    override fun fromBinary(bytes: ByteArray?, manifest: String?): Any = when(manifest){
        PRECONNECTD_MANIFEST -> PreConnected.parseFrom(bytes)
        CHATMESSAGE_MANIFEST -> ChatMessage.parseFrom(bytes)
        RECEIVEACK_MANIFEST -> ReceiveAck.parseFrom(bytes)
        JOINMESSAGE_MANIFEST -> JoinMessage.parseFrom(bytes)
        BYEMESSAGE_MANIFEST -> ByeMessage.parseFrom(bytes)
        CONNECTREQ_MANIFEST -> ConnectReq.parseFrom(bytes)
        CONNECTREQACK_MANIFEST -> ConnectReqAck.parseFrom(bytes)
        CONNECTACK_MANIFEST -> ConnectAck.parseFrom(bytes)
        ASKCURRENTUSERS_MANIFEST -> AskCurrentUsers.parseFrom(bytes)
        CURRENTUSERS_MANIFEST -> CurrentUsers.parseFrom(bytes)
        DEBUG_MANIFEST -> Debug.parseFrom(bytes)
        else -> throw IllegalArgumentException("failed to parse from bytes.")
    }
}
