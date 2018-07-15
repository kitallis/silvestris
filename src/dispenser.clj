(ns dispenser
  (:require [gpio :as gpio]
            [thread :as thread]))

(def ^:private dispenser (atom nil))
(def ^:private schedule-ms 2000)
(def ^:private delivery-ms 1000)

(defn dispense! [pin]
  (gpio/pwm! pin 2)
  (Thread/sleep delivery-ms)
  (gpio/pwm! pin 0))

(defn stop  []
  (thread/stop-tp @dispenser))

(defn start []
  (gpio/configure-pwm! :pwm-range 2000 :pwm-clock 192)
  (let [pin (gpio/provision-pin! gpio/default-pwm-pin)]
    (reset! dispenser (thread/create-scheduled-tp (partial dispense! pin)
                                                  schedule-ms)))
  (println (format "started the dispenser, scheduled to run every %dms" schedule-ms)))
