(ns core
  (:use [clojure.tools.nrepl.server :only [start-server stop-server]])
  (:import [com.pi4j.io.gpio GpioController GpioFactory GpioPinDigitalOutput PinState RaspiPin]))

(def nrepl-port 4815)
(def nrepl-server (atom nil))

(defn add-shutdown-hooks []
  (.addShutdownHook (Runtime/getRuntime)
                    (Thread. (fn []
                               (println "silvestris is shutting down...")
                               (swap! nrepl-server (fn [server]
                                                     (let [stopped (stop-server server)]
                                                       (println (str "silvestris has stopped the nrepl server at port " nrepl-port "."))
                                                       stopped)))))))

(defn -main []
  (println "silvestris has begun.")
  (println "silvestris is now starting the nrepl server...")
  (reset! nrepl-server (start-server :port nrepl-port))
  (println (str "silvestris has started the nrepl server at port " nrepl-port "."))
  (add-shutdown-hooks))
