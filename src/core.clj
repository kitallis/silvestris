(ns core
  (:use [clojure.tools.nrepl.server :only [start-server stop-server]])
  (:require [gpio :as gpio]))

(def ^:private nrepl-port 4815)
(def ^:private nrepl-server (atom nil))

(defn- start-nrepl-server []
  (reset! nrepl-server (start-server :port nrepl-port)))

(defn- stop-nrepl-server []
  (swap! nrepl-server (fn [server]
                        (let [stopped (stop-server server)]
                          (println (str "silvestris has stopped the nrepl server at port "
                                        nrepl-port
                                        "."))
                          stopped))))


(defn- add-shutdown-hooks [handlers]
  (doseq [handler handlers]
    (.addShutdownHook (Runtime/getRuntime)
                      (Thread. handler))))

(defn- set-system-properties []
  ;; https://github.com/Pi4J/pi4j/issues/319
  (System/setProperty "pi4j.linking" "dynamic")
  (System/setProperty "pi4j.debug" "true"))

(defn -main []
  (set-system-properties)
  (println "silvestris has begun.")
  (println "silvestris is now starting the nrepl server...")
  (start-nrepl-server)
  (println (str "silvestris has started the nrepl server at port "
                nrepl-port
                "."))
  (gpio/start)
  (println "silvestris has started gpio.")
  (add-shutdown-hooks [#(stop-nrepl-server) #(gpio/stop)]))
