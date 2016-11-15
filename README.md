#Communication Protocols Project - XMPP Proxy#

##Packaging & Running##
1. Clone the project
2. Open a terminal and `cd` into the project's `chinese-whispers` folder (i.e. where the POM is)
3. Create the executable JAR with `mvn clean compile assembly:single`
4. Run the project with `java -jar target/chinese-whispers-1.0-SNAPSHOT-jar-with-dependencies.jar <proxy-port> <admin-port> <default-xmpp-server-address> <default-xmpp-server-port>`

##Configuration##
1. Connect to the administration service (running in port `admin-port`)
2. Log in with `AUTH protos 42`
3. Refer to the administration protocol specification (or run `HELP`) for valid commands and their syntax