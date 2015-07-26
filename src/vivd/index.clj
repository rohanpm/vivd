(ns vivd.index
  (:require [clojure.java.io :as io]
            [vivd
             [container :as container]
             [logging :as log]]))

(set! *warn-on-reflection* true)

(defn- try-load-info [id]
  (try
    (container/load-info id)
    (catch Exception e
      (log/debug "Ignoring unloadable container" id e)
      nil)))

(defn- read-index []
  (let [container-dir   (io/as-file "data/containers")
        container-files (.listFiles container-dir)
        container-ids   (map #(.getName ^java.io.File %1) container-files)
        _               (log/debug "containers" container-ids)]
    (doall (keep try-load-info container-ids))))

(defn make []
  (let [out (agent {})]
    (send-off out (fn [_] (read-index)))
    out))
