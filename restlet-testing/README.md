Restlet Testing
===============
Code to accompany [http://www.devsumo.com/technotes/2016/10/camel-socket-permission-denied-using-port-0-with-restlets](http://www.devsumo.com/technotes/2016/10/camel-socket-permission-denied-using-port-0-with-restlets)

This project comprises a simple Camel booter application which spins up a Restlet based route accepting requests for files in a specified directory. It primarily illustrates a way of using ephemeral ports in standalone integration tests for Camel Restlet applications. Additionally it provides an example of using the `pollEnrich` mechanism to pull content from an external source as part of a Camel route (in this case by reading a file from the filesystem).

Run the main `RestletTesting` class with a path to a directory containing some files and a port number. You can then use `curl` to request the contents of a file in that directory via HTTP:-

`curl http://localhost:<port>/files/test1.txt`

It will return HTTP 200 and the content of the file if the file exists, HTTP 404 otherwise.
