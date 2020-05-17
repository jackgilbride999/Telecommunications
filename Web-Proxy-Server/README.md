# Web Proxy Server
A Java implementation of a web proxy server which accepts a connection from a web browser, responds to HTTP/HTTPS requests, blocks URLs and caches webpages. The full report [here](Proxy-Server-Report.pdf), details the entire technical specification of the project.

## Usage
1. Configure Proxy Access from your web browser to localhost, port 9999.
2. Compile the project in this directory. If using the command line, use `javac ProxyMultiThread.java`.
3. Run the project. If using the command line, use `java ProxyMultiThread`.
4. Within the command line:
  - Enter `HELP` to see the list of possible commands.
  - Enter `BLOCKED` to view the list of blocked URLs.
  - Enter `CACHED` to view the list of cached webpages.
  - Enter `CLOSE` to close the proxy server.
  - Otherwise, enter a URL to add it to the blocked list.
5. Within your web browser:
  - Enter a previously accessed HTTP URL. The program will fetch it from the cache and return it to your browser.
  - Enter a non-previously accessed HTTP URL or HTTPS URL. The program will fetch it from the web and return it to your browser.
  - Enter a blocked URL. The program will send a 403 response to your browser.