# test_SimpleWebServer
Designed with security flaws on purpose (xss, cmd injection, pathtraversal etc)



Server file-> src/main/java/hello/JavaHTTPServer.java


### Vulns
* XSS -> Takes query parameter and directly puts it into HTML body
* Path Traversal -> read file path from query param, read file, return its content
* Cmd Injection -> Take query parameter, run exec on it, grab output and return it (please don't try commands that don't close stdout ... aka ping) 

