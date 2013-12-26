(ns sssp.core)

; Virus scanning client, using SSSP protocol.

(require '[clojure.java.io :as io])

(defn send-and-check [command reader ostream]
  "Send <command> and check command was accepted"
  '(println "send-and-check" command)
  (. ostream write (. command getBytes))
  (. ostream flush)
  (let [line (. reader readLine)
        match (re-matches #"^ACC\s+(.*?)\s*$" line)]
    (not (nil? match)))
  )

(defn is-sssp-supported [reader ostream]
  "Check if SSSP is supported by the AV daemon"
    (let [line (. reader readLine)]
    (if (. line startsWith "OK SSSP/1.0")
      (send-and-check "SSSP/1.0\n" reader ostream)
      )))

(defn read-lines [reader]
  "Read a bunch of lines from AV daemon"
  '(println "read-lines")
  (let [line (. reader readLine) line-len (. line length)]
    (if (> line-len 0)
      (cons line (read-lines reader)))
    ))

(defn send-file [path reader ostream]
  "Send file contents to be scanned"
  '(println "send-file")
  (with-open [fstream (io/input-stream path)]
    (io/copy fstream ostream))
  (. ostream flush))

(defn check-for-virus-in-result [line]
  "Look for virus matches in scan results"
  (let [match (re-matches #"^VIRUS\s+(\S+)\s+(.*)" line)]
    (if-not (nil? match) (match 1))
    ))

(defn check-scan-results [reader]
  (let [viruses (apply check-for-virus-in-result (read-lines reader))]
    (if-not (nil? viruses)
      (println "viruses" viruses))))

(defn scan-file [reader ostream path]
  (println "scan-file:" path)
  (let [file-size (. (java.io.File. path) length)]
    (if (send-and-check (str "SCANDATA " file-size "\n") reader ostream)
      (do (send-file path reader ostream)
        (check-scan-results reader))
      (do (println ">>Scandata rejected")
        false)            
      )))

(defn -main [ip-address port & paths]
  (let [port-number (java.lang.Integer/parseInt (re-find #"\A-?\d+" port))
        socket (java.net.Socket. ip-address port-number)
        socket-reader (java.io.BufferedReader. (java.io.InputStreamReader. (. socket getInputStream)))
        socket-output (io/output-stream (. socket getOutputStream))]
    (is-sssp-supported socket-reader socket-output)
    ; scan all files
    (doseq [path paths] (scan-file socket-reader socket-output path))
    ))
 
