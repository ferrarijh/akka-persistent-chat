#CLI chat app using akka cluster
CLI chat app for studying akka cluster, persistence and serialization.
(Akka journal with snapshot for persistence, google protobuf for message serialization)

##FYI
In this implementation a server has its own actor system and each client also runs on respective actor system. This is not a good practice and a single actor system is enough to handle much larger traffic.
Using only a single actor system for the whole scheme would be desirable.

##Demo
Let's run the app!