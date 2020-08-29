# CLI chat app using akka cluster

CLI chat app for studying akka cluster, persistence(event sourcing) and serialization.
(Akka journal with snapshot for persistence, google protobuf for message serialization)

## FYI

In this implementation a server has its own actor system and each client also runs on respective actor system. This is not a good practice and a single actor system is enough to handle much larger traffic.
Using only a single actor system for the whole scheme would be desirable.

# Scheme

<div>
    <img src="https://github.com/ferrarijh/akka-persistent-chat/blob/develop/demo/scheme.png">
</div>

 Here are 3 types of actors - server actor, client actor, login lister. Server subscribes to MemberUp event so it can send
PreConnected message to login listener. Login listener then prompts commandline to input user id. When user press enter
with after the id, client system creates client actor having name of the id. After creation, the client actor attempts
to establish connection with server via 3-way handshake. The handshake process is enabled with 3 types of messages - ConnectReq,
ConnectReqAck, ConnectAck.
<br>
<br>
 Behind the scene all the messages are serialized and deserialized with google protobuf, chat log is saved, and the state
of each user's last entry(telling the point where the user submitted last message before 'bye') will be persisted with
event sourcing via akka journal. State will be saved at some points regularly since replaying a large pile of events
can be slow after some time.

<div>
    <img src="https://github.com/ferrarijh/akka-persistent-chat/blob/develop/demo/scheme2.png">
</div>

When client 'john' sends a message wrapped with ChatMessage class, server will distribute the message to other clients in the chat room via ```ask ``` method.
Once the server receives ReceiveAck the sender's chat state is be persisted. If server does not receive ReceiveAck in designated timeout the client('doe' in this case) will be popped from currently connected user hashmap.

## Demo

Let's run the app!

<div>
    <img src="https://github.com/ferrarijh/akka-persistent-chat/blob/develop/demo/1.png">
</div>
Server's up at port 2551.
<br></br>
<div>
    <img src="https://github.com/ferrarijh/akka-persistent-chat/blob/develop/demo/2.png">
</div>
Then first client is up at 2552. It shows current users in the chat room, which is yet empty.
<br></br>
<div>
    <img src="https://github.com/ferrarijh/akka-persistent-chat/blob/develop/demo/3 firstcon.png">
</div>
This client connected with userid 'jonathan'. Since 'jonathan' is first to join chat, it tells him that he's first in the chat.
<br></br>
<div>
    <img src="https://github.com/ferrarijh/akka-persistent-chat/blob/develop/demo/4 richardcon.png">
</div>
Second client 'richard' connects. He can see 'jonathan' is in the chat before typing in his id.
<br></br>
<div>
    <img src="https://github.com/ferrarijh/akka-persistent-chat/blob/develop/demo/6.png">
</div>
'jonathan' gets notified of 'richard' joining chat.
<br></br>
<div>
    <img src="https://github.com/ferrarijh/akka-persistent-chat/blob/develop/demo/7 richard bye.png">
</div>
Bye typing in 'bye' user can leave chat.
<br></br>
<div>
    <img src="https://github.com/ferrarijh/akka-persistent-chat/blob/develop/demo/8 kevin.png">
</div>
'richard' comes back and 3rd user 'kevin' joins chat. Messages submitted by 'jonathan' after richard left are recovered
at richard's screen. New user 'kevin' is told he's up to date since he have just joined the chat.

## Persisted state

State of server representing each chat state of a client will be recovered in need through akka journal's event sourcing - with akka snapshot, at somepoint.
Three types of messages are persisted to represent client's chat state.

```kotlin
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
```

## Protobuf serialization
11 types of user defined messages are used in the app. They're all defined in 'Message.proto', compiled with protocol buffer compiler as java class(MyPersistenceMessages.java),
and further implemented with 'MyPersistenceSerializer.kt'. Compiled messages and serializer is matched by configuring 'application.conf', 

```
syntax = "proto3";

option java_package = "tv.anypoint.jonathan.serialization";
option java_outer_classname = "MyPersistenceMessages";

message ChatMessage{
    string userId = 1;
    string content = 2;
}
message ConnectReq{}
message ConnectReqAck{
    repeated string users = 1;
    repeated ChatMessage log = 2;
    int32 increm = 3;
}
message ConnectAck{
    string userId = 1;
    int32 increm = 2;
}
message JoinMessage{
    string userId = 1;
}
message ByeMessage{
    string userId = 1;
}
message ReceiveAck{
    string userId = 1;
}
message AskCurrentUsers{}
message CurrentUsers{
    repeated string users = 1;
}
message Debug{}
message PreConnected{}
```
