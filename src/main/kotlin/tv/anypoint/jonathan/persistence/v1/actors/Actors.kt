package tv.anypoint.jonathan.persistence.v1.actors

import akka.actor.ActorPath
import akka.cluster.Cluster
import akka.cluster.ClusterEvent
import tv.anypoint.jonathan.pubsub.AbstractActorKL
import java.io.*
import akka.pattern.Patterns.ask
import akka.pattern.Patterns.pipe
import akka.persistence.*
import mu.KLogging
import java.time.Duration
import java.time.LocalDateTime
import java.util.concurrent.CompletableFuture

import tv.anypoint.jonathan.serialization.MyPersistenceMessages.*

class ChatState(val userState: MutableMap<String, Int>, var lastLine: Int): Serializable {  //map<name, cnt>
    fun update(cAck: ConnectAck){
        if (userState[cAck.userId] == null)
            userState[cAck.userId] = cAck.increm
        else {
            val newLast: Int = userState[cAck.userId]!!.plus(cAck.increm)
            userState[cAck.userId] = newLast-1
        }
    }
    fun update(rAck: ReceiveAck){
        val newLast: Int = userState[rAck.userId]!!.inc()
        userState[rAck.userId] = newLast-1
    }
    fun update(c: ChatMessage){
        ++lastLine
        userState[c.userId] = lastLine-1
    }
    fun copy() = ChatState(userState, lastLine)
}

class ChatPersistentServer(private var chatState: ChatState): AbstractPersistentActor() {
    private val cluster = Cluster.get(context.system)
    private val connected = HashMap<String, ActorPath>()    //first: set<userId>, second: set<path>
    private val snapShotInterval = 10L

    override fun persistenceId() = "persistent-chat-server"
    private fun newReader() = BufferedReader(FileReader("log.txt"))

    override fun preStart() {
        cluster.subscribe(self, ClusterEvent.MemberUp::class.java)
    }

    override fun postStop() {
        cluster.unsubscribe(self)
    }

    override fun createReceiveRecover(): Receive = receiveBuilder()
        .match(ReceiveAck::class.java) {
            chatState.update(it)
        }.match(ConnectAck::class.java) {
            chatState.update(it)
        }.match(ChatMessage::class.java) {
            chatState.update(it)
        }.match(SnapshotOffer::class.java) {
            chatState = it.snapshot() as ChatState
        }.build()

    override fun createReceive(): Receive = receiveBuilder()
        .match(ClusterEvent.MemberUp::class.java) {
            val targetAdd = it.member().address().toString() + "/user/*"
            val pCon = PreConnected.newBuilder()
                .build()
            context.actorSelection(targetAdd).tell(pCon, self)
        }.match(ConnectReq::class.java) {
            //1)send ConnectReqAck(contentPair<userList, log>) 2)send previous messages if client's log is out-of-date.
            val sName = sender.path().name()

            val clientLast: Int? = chatState.userState[sName]
            val log = mutableListOf<ChatMessage>()
            var increm: Int = 0

            if (clientLast == null) //if new user,
                chatState.userState[sName] = chatState.lastLine
            else if (clientLast < chatState.lastLine-1) {  //if out-of-date, include log.
                increm = chatState.lastLine - clientLast
                logger.info("increm: $increm")
                val reader = newReader()
                for (i in 0 until chatState.lastLine) {
                    try {
                        val buf = reader.readLine()
                        if (i >= clientLast) {
                            val spl = buf.split('\t')
                            val cMsg = ChatMessage.newBuilder()
                                .setUserId(spl[1])
                                .setContent(spl[2])
                                .build()
                            log.add(cMsg)
                        }
                    } catch (e: IllegalStateException) {   //possible EOF related exception
                        e.printStackTrace()
                        logger.info("i: $i, chatState.lastLine: ${chatState.lastLine}, clientLast: $clientLast")
                        print(">> ")
                        break
                    }
                }
                reader.close()
            }

            val users = mutableListOf<String>()
            val kNames = connected.keys
            kNames.forEach { users.add(it) }

            val conRA = ConnectReqAck.newBuilder()
                .addAllUsers(users)
                .addAllLog(log)
                .setIncrem(increm)
                .build()
            sender.tell(conRA, self)

        }.match(ConnectAck::class.java) { cAck ->
            val sName = sender.path().name()

            val jMsg = JoinMessage.newBuilder()
                .setUserId(sName)
                .build()
            connected.forEach{ context.actorSelection(it.value).tell(jMsg, self) }

            connected[sName] = sender.path()
            logger.info("\nconnected: ${sender.path()}")
            persist(cAck) {
                //logger.info("in persist block of ConnectAck..")
                chatState.update(it)
                if (lastSequenceNr() % snapShotInterval == 0L && lastSequenceNr() != 0L)
                    saveSnapshot(chatState.copy())
            }
        }.match(ReceiveAck::class.java) { rAck ->
            persist(rAck) {
                //logger.info("in persist block of ReceiveAck..")
                chatState.update(it)
                if (lastSequenceNr() % snapShotInterval == 0L && lastSequenceNr() != 0L)
                    saveSnapshot(chatState.copy())
            }
        }.match(ChatMessage::class.java) { cMsg ->  //1)write to fs, 2)acknowledge client of receiving cMsg, 3)broadcast. 4)persist message 'event'(modify lastLine).
            if (connected[cMsg.userId] != null) {
                val writer = FileWriter("log.txt", true)
                val time = LocalDateTime.now().toString()
                writer.write("${chatState.lastLine}\t${cMsg.userId}\t${cMsg.content}\t$time\n") //1)
                writer.close()
                val rAck = ReceiveAck.newBuilder()
                    .setUserId("")
                    .build()
                sender.tell(rAck, self)   //2) since cMsg is 'asked', sender is not transparent.
                for (ipr in connected) {   //3)
                    if (ipr.key == cMsg.userId)
                        continue
                    //logger.info("asking cMsg to: ${ipr.value} ..")
                    val future = ask(context.actorSelection(ipr.value), cMsg, Duration.ofSeconds(2)).toCompletableFuture()    //여기서 원래 sender 정보 죽는듯?
                        as CompletableFuture<ReceiveAck>
                    pipe(future, context.dispatcher).to(self)   //to apply match(ReceiveAck::class.java) class.
                }
                persist(cMsg) {
                    //logger.info("in persist block of ChatMessage..")
                    chatState.update(it)    //4)
                    if (lastSequenceNr() % snapShotInterval == 0L && lastSequenceNr() != 0L)
                        saveSnapshot(chatState.copy())
                }
            }
        }.match(ByeMessage::class.java) {
            connected.remove(sender.path().name())
            logger.info("disconnected: ${sender.path()}")
            print(">> ")
            val bMsg = ByeMessage.newBuilder()
                .setUserId(sender.path().name())
                .build()
                //ByeMessage(sender.path().name())
            connected.forEach { e ->
                val aSel = context.actorSelection(e.value)
                aSel.tell(bMsg, self)
            }
        }.match(AskCurrentUsers::class.java) {
            logger.info("CurrentUsers asked from: ${sender.path()}")
            print(">> ")
            val lis = connected.keys.toList()
            val msg = CurrentUsers.newBuilder()
                .addAllUsers(lis)
                .build()
            sender.tell(msg, self)
//for debugging below
        }.match(Debug::class.java) {
            logger.info("/*--- chatState for debugging ---*/")
            chatState.userState.forEach{print("$it ")}
            println()
            logger.info("lastLine: ${chatState.lastLine}")
            logger.info("lastSequenceNr(): ${lastSequenceNr()}")
            logger.info("/*--------------------------------*/")
            print(">> ")
        }.match(String::class.java){
            when(it){
                "debug"->self.tell(Debug.newBuilder().build(), self)
                "delete snapshot"->deleteSnapshots(
                    SnapshotSelectionCriteria.create(lastSequenceNr(), System.currentTimeMillis())
                )
                "shutdown" -> context.system.terminate()
            }
        }.match(SaveSnapshotSuccess::class.java){
            logger.info("successfully saved snapshot with lastSequenceNr(): ${lastSequenceNr()} ")
            print(">> ")
        }.match(DeleteSnapshotsSuccess::class.java){
            logger.info("successfully deleted snapshot with lastSequenceNr(): ${lastSequenceNr()}")
            print(">> ")
        }.build()

    companion object : KLogging()
}

class ChatClient: AbstractActorKL(){
    private val server = context.actorSelection("akka.tcp://ClusterSystem@127.0.0.1:2551/user/server")
    private var isConnected = false

    override fun createReceive(): Receive = receiveBuilder()
        .matchEquals("connect"){
            if (!isConnected){
                server.tell(ConnectReq.newBuilder().build(), self)
                println("connection request sent..")
            } else {
                println("*--- YOU'RE ALREADY CONNECTED!")
                print(">> ")
            }
        }.matchEquals("bye") {
            println("*--- good bye ---*")
            val bMsg = ByeMessage.newBuilder()
                .setUserId(self.path().name())
                .build()
            server.tell(bMsg, self)
            context.system.terminate()
        }.matchEquals("users"){
            server.tell(AskCurrentUsers.newBuilder().build(), self)
        }.match(String::class.java){
            val cMsg = ChatMessage.newBuilder()
                .setUserId(self.path().name())
                .setContent(it)
                .build()
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
            isConnected = true
            if (it.usersList.isNotEmpty())
                println("*--- current users : ${it.usersList}")
            else
                println("*--- you're first here!")
            if (it.logList.isNotEmpty()) {
                println("...previously from where you left...")
                it.logList.forEach { cMsg ->
                    println("[${cMsg.userId}] ${cMsg.content}")
                }
                println("....................................")
            }else{
                println("...you're up to date!")
            }
            print(">> ")
            val newCAck = ConnectAck.newBuilder()
                .setUserId(self.path().name())
                .setIncrem(it.increm)
                .build()
            sender.tell(newCAck, self)
        }.match(ChatMessage::class.java){
            val rAck = ReceiveAck.newBuilder()
                .setUserId(self.path().name())
                .build()
            sender.tell(rAck, self)
            println("\r>> [${it.userId}] ${it.content}")
            print(">> ")
        }.match(JoinMessage::class.java){
            println("\r*--- [${it.userId}] joined chat!")
            print(">> ")
        }.match(ByeMessage::class.java){
            println("\r*--- [${it.userId}] left chat.")
            print(">> ")
        }.match(CurrentUsers::class.java){
            println("\r*--- current users : ${it.usersList}")
            print(">> ")
        }.build()
}

class LoginListener: AbstractActorKL(){
    override fun createReceive(): Receive = receiveBuilder()
        .match(PreConnected::class.java) {
            println("requesting current users info to server..")
            val future = ask(sender, AskCurrentUsers.newBuilder().build(), Duration.ofSeconds(5)).toCompletableFuture()
                as CompletableFuture<CurrentUsers>
            future.whenComplete{ cu, ex ->
                if (ex != null)
                    print("server request timeout.. please restart.")
                else {
                    println("\r*--- current users : ${cu.usersList}")
                    print(">> Your user ID is: ")
                }
            }
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
