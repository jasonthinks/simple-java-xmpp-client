# Re-written Simple XMPP Client in Java

Change xmpp domain according your xmpp server

Then run below command:
mvn compile exec:java -Dusername=[username] -Dpassword=[password]

run with different users/password in other DOS prompt.

Then you can chat with each other.

Type "help" after chatting starts for commands' list.


# Simple XMPP Client in Java

[![Build Status](https://travis-ci.org/ltg-uic/simple-java-xmpp-client.png?branch=master)](https://travis-ci.org/ltg-uic/simple-java-xmpp-client)

This is a simple XMPP client written in java and based on Smack. 

It can be used both for groupchat and regular point-to-point XMPP. 

It supports both synchronous and asynchronous modes. 

See examples in the `ltg.commons.example` package to learn how to use