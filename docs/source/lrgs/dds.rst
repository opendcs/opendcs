############
DDS Protocol
############


The DDS Protocol is used to communicate from LRGS to LRGS or from
the Routing Spec program (DEOCDES) to an LRGS.

The protocol is a simple message in a synchronous request/response.

Basic Message Structure
-----------------------

Each message consists of at least a 10 byte header that includes a synchronization sequence,
msg id character, and a length.

.. 

    xxxxzyyyyyaaaaaaaaa...aaa

+-------+----------+-------------------------------------------------------------------+
|Element|Range     |Description                                                        |
+=======+==========+===================================================================+
|XXXX   |FAFO      |The synchronization header value                                   |
+-------+----------+-------------------------------------------------------------------+
|z      |[a-u]     |One of the valid DDS command characters, described in detail later.|
+-------+----------+-------------------------------------------------------------------+
|yyyyy  |0-99000   |The length of the message body                                     |
+-------+----------+-------------------------------------------------------------------+
|aaa..aa|raw bytes |Body contents, if applicaable                                      |
+-------+----------+-------------------------------------------------------------------+


Commands
--------

This is a list of command with a basic request/response and description.
Additional details and examples will be provided below.

Text within square brackets is optional.

+-------------+--+---------------------------+-------------------------------+---------------------------------+
|Command      |Id|Request                    |Response                       |Description                      |
+=============+==+===========================+===============================+=================================+
|Hello        |a |username [dds version]     |- username [dds version]       |Unauthenticated login.           |
|             |  |                           |- hello ?                      |                                 |
|             |  |                           |- hello ???                    |                                 |
+-------------+--+---------------------------+-------------------------------+---------------------------------+            
|Goodbye      |b |nothing                    |(goodbye)                      |Inform the LRGS we're            |
|             |  |                           |                               |disconnecting                    |
+-------------+--+---------------------------+-------------------------------+---------------------------------+
|Status       |c |? (the character)          |xml data                       |status of the LRGS system.       |
|             |  |                           |                               |                                 |
|             |  |                           |                               |                                 |
+-------------+--+---------------------------+-------------------------------+---------------------------------+
|AuthHello    |m |username timestr           |- username timestr dds version |Authenticated login              |
|             |  |authenticator [dss version]|- ?                            |Uses a digest form of the        |
|             |  |                           |                               |password                         |
+-------------+--+---------------------------+-------------------------------+---------------------------------+