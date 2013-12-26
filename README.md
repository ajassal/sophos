sophos
======

Integrations with Sophos products.

sssp.clj: scan file(s) using Sophos (SSSP) AV daemon.

Example:

java -cp clojure-1.5.1.jar:./Documents/workspace/sssp/src clojure.main -m sssp.core 192.168.56.101 4010 abc.txt def.doc
