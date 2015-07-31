(ns vivd.utils
  (:require [clojure.java.io :as io]
            [clojure.java.shell :refer [sh]]
            [clojure.tools.logging :as log]))

(defn reader-for-file ^java.io.PushbackReader [& args]
  (->  (apply io/file args)
       (io/reader)
       (java.io.PushbackReader.)))

(defn sh! [cmd & args]
  (log/debug "sh:" cmd args)
  (let [result (apply sh cmd args)
        exit   (:exit result)]
    (if (not= 0 exit)
      (throw (ex-info "command failed" {:result result}))
      result)))
