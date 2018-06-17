(ns core
  (:use [clojure.tools.nrepl.server :only [start-server stop-server]])
  (:import [com.pi4j.io.gpio GpioController GpioFactory GpioPinDigitalOutput PinState RaspiPin]))

(def nrepl-port 4815)
(def nrepl-server (atom nil))

(defn start-nrepl-server []
  (reset! nrepl-server (start-server :port nrepl-port)))

(defn stop-nrepl-server []
  (swap! nrepl-server (fn [server]
                        (let [stopped (stop-server server)]
                          (println (str "silvestris has stopped the nrepl server at port "
                                        nrepl-port
                                        "."))
                          stopped))))


(defn add-shutdown-hooks []
  (.addShutdownHook (Runtime/getRuntime)
                    (Thread. (fn []
                               (println "silvestris is shutting down...")
                               (stop-nrepl-server)))))

(defn set-system-properties []
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
  (add-shutdown-hooks))
