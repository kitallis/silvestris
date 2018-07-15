(ns gpio
  (:import [com.pi4j.wiringpi Gpio]
           [com.pi4j.io.gpio GpioController
            GpioFactory

            GpioPinDigitalOutput
            GpioPinPwmOutput

            PinState
            RaspiPin
            PinPullResistance]
           [com.pi4j.io.gpio.exception GpioPinNotProvisionedException]
           [com.pi4j.io.gpio.impl GpioControllerImpl PinImpl GpioPinImpl])
  (:require [clj-time.core :as t]))

(def ^:private gpio (ref nil))
(def ^:private provisioned-pins (ref ()))

(defn start []
  (dosync (ref-set gpio (GpioFactory/getInstance)))
  (println "started a gpio instance."))

(defn unprovision-pin! [^GpioPinImpl pin]
  (.unprovisionPin @gpio (into-array GpioPinImpl [pin])))

(defn shutdown-pins [pins]
  (doseq [pin pins]
    (try
      (unprovision-pin! (:pin pin))
      (catch GpioPinNotProvisionedException e))))

(defn shutdown-gpio [^GpioControllerImpl gpio] (.shutdown gpio))

(defn stop []
  (dosync
   (alter gpio shutdown-gpio)
   (alter provisioned-pins shutdown-pins))
  (println "stopped a gpio instance."))

(defn track-provisioned-pins [pin type]
  (dosync
   (alter provisioned-pins conj {:pin pin
                                 :ts (t/now)
                                 :type type}))
  pin)

(def pwm :pwm)
(def digital-out :digital-output)

(defn configure-pwm! [& {:keys [pwm-range pwm-clock]}]
  (Gpio/pwmSetMode Gpio/PWM_MODE_MS)
  (when (and pwm-clock
             pwm-range)
    (Gpio/pwmSetClock (or pwm-clock 500))
    (Gpio/pwmSetRange (or pwm-range 1000))))

(defn pwm  [^GpioPinPwmOutput pin] (.getPwm pin))
(defn pwm! [^GpioPinPwmOutput pin val] (.setPwm pin val))

(def default-pin
  {:pin-name RaspiPin/GPIO_26
   :tag "BCM.PIN.12"
   :default-state PinState/HIGH})

(def default-do-pin  (assoc default-pin :pin digital-out))
(def default-pwm-pin (assoc default-pin :pin pwm))

(defmulti  provision-pin! :pin)
(defmethod provision-pin! digital-out
  [{:keys [^PinImpl pin-name tag ^PinState default-state]}]
  (doto
      (.provisionDigitalOutputPin @gpio
                                  pin-name
                                  tag
                                  default-state)
      (track-provisioned-pins digital-out)
      (.setShutdownOptions true PinState/LOW PinPullResistance/OFF)))

(defmethod provision-pin! pwm
  [{:keys [^PinImpl pin-name tag ^PinState default-state]}]
  (doto
      (.provisionPwmOutputPin @gpio
                              pin-name
                              tag)
      (track-provisioned-pins pwm)
      (.setShutdownOptions true PinState/LOW PinPullResistance/OFF)))

(defn low!    [^GpioPinImpl pin] (.low pin))
(defn high!   [^GpioPinImpl pin] (.high pin))
(defn toggle! [^GpioPinImpl pin] (.toggle pin))
