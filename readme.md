# User Status Plugin Readme

## Overview

Openfire plugin to save the user status to the database.

This plugin automatically saves the last status (presence, IP address, logon and logoff time) per user and resource to userStatus table in the Openfire database.

Optionally you can archive user status entries (IP address, logon and logoff time) for a specified time. History entries are stored in the userStatusHistory table. The settings for history archiving can be configured on the "User Status Settings" page that you'll find on the "Server" tab of the Openfire Admin Console.

This plugin also archives the status events of the Server to Server (S2S) connections to the serverStatusHistory table. it is logging the following: 
- streamID   
- address: reference to the addressed domain which is the external domain for outgoing connections or the local server for incoming connections     
- servername: the externel Host 
- online: 1 - Connection created, 0 - Connection destroyed     
- ipAddress: the externel Host IP  
- eventDate: timestamp of the event  
- type: 1 - Incoming connection, 2 - Outgoing connection


