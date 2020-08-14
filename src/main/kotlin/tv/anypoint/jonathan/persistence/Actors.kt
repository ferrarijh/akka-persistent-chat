package tv.anypoint.jonathan.persistence

import akka.actor.ActorPath
import akka.persistence.AbstractPersistentActor
import akka.persistence.SnapshotOffer
import tv.anypoint.jonathan.pubsub.AbstractActorKL
import java.io.*
import akka.pattern.Patterns.ask
import java.time.Duration
import java.util.concurrent.CompletableFuture

class ChatMessage(val name: String, val content: String): Serializable

class ChatState(val userState: MutableMap<String, Int>, var lastLine: Int): Serializable{  //map<name, cnt>
    fun update(cAck: ConnectAck) = userState[cAck.userId]?.plus(cAck.increm)
    fun update(rAck: ReceiveAck) = userState[rAck.userId]?.plus(1)
    fun update(c: ChatMessage) = lastLine++
    fun copy() = ChatState(userState, lastLine)
}

class ConnectReq: Serializable
class ConnectReqAck(val users: List<String>, val log: List<ChatMessage>, val increm: Int): Serializable
class ConnectAck(val userId: String, val increm: Int): Serializable
class ByeMessage(val userId: String): Serializable

class ReceiveAck(val userId: String): Serializable

class AskCurrentUsers: Serializable
class CurrentUsers(val users: List<String>): Serializable

class ChatPersistentServer(private var chatState: ChatState): AbstractPersistentActor() {
    private val connected = hashSetOf<ActorPath>()  //name, (isCon, path)
    private val snapShotInterval = 1000L

    private val logFile = File("log.txt")
    private val writer = FileWriter(logFile, true)

    override fun persistenceId() = "persistent-chat-server"
    private fun newReader() = BufferedReader(FileReader(logFile))

    override fun preStart(){    //initialize lastLine
        super.preStart()
        val reader = newReader()
        var buf: String?
        while (true){
            buf = reader.readLine()
            if (buf == null) break
            chatState.lastLine++
        }
    }

    override fun createReceiveRecover(): Receive = receiveBuilder()
        .match(ReceiveAck::class.java){
            chatState.update(it)
        }.match(ConnectAck::class.java){
            chatState.update(it)
        }.match(SnapshotOffer::class.java){
            chatState = it.snapshot() as ChatState
        }.build()

    override fun createReceive(): Receive = receiveBuilder()
        .match(ConnectReq::class.java){
            //1)add sender to connected. 2)send ConnectReqAck(contentPair<userList, log>) 3)send previous messages if client's log is out-of-date.
            val sName = sender.path().name()
            logger.info("received ConnectReq() from: [$sName}]")
            
            val clientLast: Int? = chatState.userState[sName]
            val log = mutableListOf<ChatMessage>()
            var increm: Int = 0

            if (clientLast == null) //if new user,
                chatState.userState[sName] = chatState.lastLine
            else if (clientLast < chatState.lastLine) {  //if out-of-date, include log.
                increm = chatState.lastLine - clientLast
                val reader = newReader()
                for(i in 0..chatState.lastLine){
                    val buf = reader.readLine()
                    if (i >= clientLast){
                        val spl = buf.split('\t')
                        log.add(ChatMessage(spl[1], spl[2]))
                    }
                }
            }
            val users = mutableListOf<String>()
            connected.forEach{ users.add(it.name()) }
            val conRA = ConnectReqAck(users, log, increm)
            sender.tell(conRA, self)
        }.match(ConnectAck::class.java) { cAck ->
            connected.add(sender.path())
            logger.info("connected: ${sender.path()}")
            persist(cAck) {
                chatState.update(it)
                if (lastSequenceNr() % snapShotInterval == 0L && lastSequenceNr() != 0L)
                    saveSnapshot(chatState.copy())
            }
        }.match(ReceiveAck::class.java){ rAck ->
            persist(rAck){
                chatState.update(it)
                if (lastSequenceNr() % snapShotInterval == 0L && lastSequenceNr() != 0L)
                    saveSnapshot(chatState.copy())
            }
        }.match(ChatMessage::class.java){ cMsg ->  //1)write to fs, 2)acknowledge client of receiving cMsg, 3)broadcast. 4)persist message 'event'(modify lastLine).
            writer.write("${++chatState.lastLine}\\t${cMsg.name}\\t${cMsg.content}\\n") //1)
            sender.tell(ReceiveAck(""), self)   //2)
            for (iPath in connected){   //3)
                logger.info("- asking cMsg to: $iPath ..")
                if (iPath.name() == cMsg.name)
                    continue
                ask(context.actorSelection(iPath), cMsg, Duration.ofSeconds(2)).toCompletableFuture()    //여기서 원래 sender 정보 죽는듯?
                    .whenComplete{ rAck, e ->
                        if(e != null){
                            connected.remove(iPath)
                            logger.info("- timeout for $iPath . Removing from connected.")
                        }else
                            logger.info("- rAck received from: $iPath")
                    }
            }
            persist(cMsg){  //4)
                chatState.update(it)
            }
        }.match(ByeMessage::class.java){
            connected.remove(sender.path())
            logger.info("disconnected: ${sender.path()}")
            val bMsg = ByeMessage(sender.path().name())
            connected.forEach{ e ->
                val aSel = context.actorSelection(e)
                aSel.tell(bMsg, self)
            }
        }.match(AskCurrentUsers::class.java){
            logger.info("CurrentUsers asked from: ${sender.path()}")
            val lis = mutableListOf<String>
            connected.forEach{
                lis.add(it.name())
            }
            sender.tell(CurrentUsers(lis), self)
        }.build()
    
    companion object: KLogging()
}

class ChatClient: AbstractActorKL(){
    private val server = context.actorSelection("akka.tcp://ClusterSystem@127.0.0.1:2551/user/server")
    private var isConnected = false
    override fun createReceive(): Receive = receiveBuilder()
        .matchEquals("connect"){
            server.tell(ConnectReq(), self)
            println("connection request sent..")
        }.matchEquals("bye") {
            println("*--- good bye ---*")
            server.tell(ByeMessage(self.path().name()), self)
            context.system.terminate()
        }.matchEquals("users"){
            server.tell(AskCurrentUsers(), self)
        }.match(String::class.java){
            val cMsg = ChatMessage(self.path().name(), it)
            val future = ask(server, cMsg, Duration.ofSeconds(2)).toCompletableFuture()
                as CompletableFuture<ReceiveAck>
            future.whenComplete{ rAck, e -> //rAck only for debug!!
                if (e != null)
                    println("server response timeout...")
                else
                    print(">> ")    //this means you're connected with server :)
            }
        }.match(ConnectReqAck::class.java) {
            println("*--- Connected!")
            if (it.users.isNotEmpty())
                println("*--- current users : ${it.users}")
            else
                println("*--- you're first here!")
            if (it.log.isNotEmpty()) {
                println("...previously from where you left...")
                it.log.forEach { cMsg ->
                    println(">> [${cMsg.name}] ${cMsg.content}")
                }
                println("....................................")
            }else{
                println("...you're up to date!")
            }
            print(">> ")
            sender.tell(ConnectAck(self.path().name(), it.increm), self)
        }.match(ChatMessage::class.java){
            sender.tell(ReceiveAck(self.path().name()), self)
            println("\r>> [${it.name}] ${it.content}")
            print(">> ")
        }.match(ByeMessage::class.java){
            println("\r*--- [${it.userId}] left chat.")
            print(">> ")
        }.match(CurrentUsers::class.java){
            print("\r*--- current users : ${it.users}")
        }.build()
}

//
/*--- below are just for reference :) ---*/
//
/*
class Cmd(val data: String): Serializable   //wth is command? where's it coming from?
class Evt(val data: String): Serializable   //events=messages, in most cases, i guess..

class ExampleState(private val events: MutableList<String>) : Serializable{
    fun copy(): ExampleState = ExampleState(events)
    fun update(evt: Evt){ events.add(evt.data) }
    fun size() = events.size
    override fun toString() = events.toString()
}

class ExamplePersistenceActor(private var state: ExampleState): AbstractPersistentActor() {
    private val snapShotInterval = 2L //'state' is mutable!
    private fun getNumEvents() = state.size()

    override fun persistenceId() = "persistent-1"

    override fun createReceiveRecover(): Receive = receiveBuilder()
        .match(Evt::class.java){
            state.update(it)
        }.match(SnapshotOffer::class.java){
            state = it.snapshot() as ExampleState   //somebody.. offers snapshot
        }.build()

    override fun createReceive(): Receive = receiveBuilder()
        .match(Cmd::class.java){ c ->   //check validity of command 'Cmd'
            val data = c.data
            val evt = Evt(data + "-" + getNumEvents())
            persist(evt){ e ->
                state.update(e) //Cmd 메시지 받으면 state.update(evt), publish(evt), ... 실행.
                //context.system.eventStream.publish(e)   //???
                if (lastSequenceNr() % snapShotInterval == 0L && lastSequenceNr() != 0L){
                    println(">> lastSequenceNr: ${lastSequenceNr()}")
                    saveSnapshot(state.copy())
                    self.tell(deleteMessages(lastSequenceNr()), ActorRef.noSender())
                }
            }
        }.matchEquals("print"){
            println("/*----- current state -----*/")
            println(state)
        }.match(DeleteMessagesSuccess::class.java){
            println(">> Deletion done!")
        }.match(DeleteMessagesFailure::class.java){
            println(">> NO GOOD :(")
        }.build()
}

/*---*/

class Msg(val num: Int): Serializable

class SimplePersistentActor: AbstractPersistentActor(){
    private var stateSum :Int = 0
    override fun persistenceId() = "persistent-2"
    override fun createReceiveRecover(): Receive = receiveBuilder()
        .match(Msg::class.java){
        stateSum += it.num
    }.build()

    override fun createReceive(): Receive = receiveBuilder()
        .match(Msg::class.java){
            persist(it){msg ->
                stateSum += msg.num
            }
        }
        .matchEquals("print"){
            println(">> stateSum: $stateSum")
        }.build()
}
*/
