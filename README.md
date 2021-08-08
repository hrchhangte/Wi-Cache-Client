# Wi-Cache-Client

Wi-Cache-Client can be executed on wireless clients that are connected to the Wi-Cache WiFi network.

An example client application provided is the MobileClient class. This will use the WiCacheClientModule which handles the file download. 
Currently TCP and SCTP can be used for file download. However, mobility support is provided only for TCP at this point.

To run the MobileClient program, run as below:

```
java MobileClient <server IP> <servrer Port> <client Port for data> <Protocol=tcp or sctp>
```
