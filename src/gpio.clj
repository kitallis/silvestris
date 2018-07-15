(ns gpio
  (:import [com.pi4j.wiringpi Gpio]
           [com.pi4j.io.gpio GpioController
            GpioFactory

            GpioPinDigitalOutput
            GpioPinPwmOutput

            PinState
            RaspiPin
            PinPullResistance]
           [com.pi4j.io.gpio.impl PinImpl GpioPinImpl]))

(def ^:private gpio (atom nil))

(defn start []
  (reset! gpio (GpioFactory/getInstance))
  (println "started a gpio instance."))

(defn stop []
  (swap! gpio (fn [v] (.shutdown v)))
  (println "stopped a gpio instance."))

;; wiringPi reference table
;;
;; +-----+-----+---------+------+---+---Pi 3---+---+------+---------+-----+-----+
;; | BCM | wPi |   Name  | Mode | V | Physical | V | Mode | Name    | wPi | BCM |
;; +-----+-----+---------+------+---+----++----+---+------+---------+-----+-----+
;; |     |     |    3.3v |      |   |  1 || 2  |   |      | 5v      |     |     |
;; |   2 |   8 |   SDA.1 |   IN | 1 |  3 || 4  |   |      | 5v      |     |     |
;; |   3 |   9 |   SCL.1 |   IN | 1 |  5 || 6  |   |      | 0v      |     |     |
;; |   4 |   7 | GPIO. 7 |   IN | 1 |  7 || 8  | 1 | IN   | TxD     | 15  | 14  |
;; |     |     |      0v |      |   |  9 || 10 | 1 | IN   | RxD     | 16  | 15  |
;; |  17 |   0 | GPIO. 0 |   IN | 0 | 11 || 12 | 0 | IN   | GPIO. 1 | 1   | 18  |
;; |  27 |   2 | GPIO. 2 |   IN | 0 | 13 || 14 |   |      | 0v      |     |     |
;; |  22 |   3 | GPIO. 3 |   IN | 0 | 15 || 16 | 0 | IN   | GPIO. 4 | 4   | 23  |
;; |     |     |    3.3v |      |   | 17 || 18 | 0 | IN   | GPIO. 5 | 5   | 24  |
;; |  10 |  12 |    MOSI |   IN | 0 | 19 || 20 |   |      | 0v      |     |     |
;; |   9 |  13 |    MISO |   IN | 0 | 21 || 22 | 0 | IN   | GPIO. 6 | 6   | 25  |
;; |  11 |  14 |    SCLK |   IN | 0 | 23 || 24 | 1 | IN   | CE0     | 10  | 8   |
;; |     |     |      0v |      |   | 25 || 26 | 1 | IN   | CE1     | 11  | 7   |
;; |   0 |  30 |   SDA.0 |   IN | 1 | 27 || 28 | 1 | IN   | SCL.0   | 31  | 1   |
;; |   5 |  21 | GPIO.21 |   IN | 1 | 29 || 30 |   |      | 0v      |     |     |
;; |   6 |  22 | GPIO.22 |   IN | 1 | 31 || 32 | 0 | IN   | GPIO.26 | 26  | 12  |
;; |  13 |  23 | GPIO.23 |   IN | 0 | 33 || 34 |   |      | 0v      |     |     |
;; |  19 |  24 | GPIO.24 |   IN | 0 | 35 || 36 | 0 | IN   | GPIO.27 | 27  | 16  |
;; |  26 |  25 | GPIO.25 |   IN | 0 | 37 || 38 | 0 | IN   | GPIO.28 | 28  | 20  |
;; |     |     |      0v |      |   | 39 || 40 | 0 | IN   | GPIO.29 | 29  | 21  |
;; +-----+-----+---------+------+---+----++----+---+------+---------+-----+-----+
;; | BCM | wPi |   Name  | Mode | V | Physical | V | Mode | Name    | wPi | BCM |
;; +-----+-----+---------+------+---+---Pi 3---+---+------+---------+-----+-----+

(defn configure-pwm! [& {:keys [pwm-range pwm-clock]}]
  (Gpio/pwmSetMode  Gpio/PWM_MODE_MS)
  (when (and pwm-clock
             pwm-range)
    (Gpio/pwmSetClock (or pwm-clock 500))
    (Gpio/pwmSetRange (or pwm-range 1000))))

(defn pwm  [^GpioPinPwmOutput pin] (.getPwm pin))
(defn pwm! [^GpioPinPwmOutput pin val] (.setPwm pin val))

(def pin
  {:pin-name RaspiPin/GPIO_26
   :tag "BCM.PIN.12"
   :default-state PinState/HIGH})

(def do-pin  (assoc pin :pin :digital-out))
(def pwm-pin (assoc pin :pin :pwm))

(defmulti  provision-pin! :pin)
(defmethod provision-pin! :digital-out
  [{:keys [^PinImpl pin-name tag ^PinState default-state]}]
  (doto
      (.provisionDigitalOutputPin @gpio
                                  pin-name
                                  tag
                                  default-state)
      (.setShutdownOptions true PinState/LOW PinPullResistance/OFF)))

(defmethod provision-pin! :pwm
  [{:keys [^PinImpl pin-name tag ^PinState default-state]}]
  (doto
      (.provisionPwmOutputPin @gpio
                              pin-name
                              tag)
      (.setShutdownOptions true PinState/LOW PinPullResistance/OFF)))

(defn low!    [pin] (.low pin))
(defn high!   [pin] (.high pin))
(defn toggle! [pin] (.toggle pin))

(defn unprovision-pin! [^GpioPinImpl pin]
  (.unprovisionPin @gpio (into-array GpioPinImpl [pin])))
