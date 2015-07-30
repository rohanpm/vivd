(ns vivd.index
  (:require [clojure.java.io :as io]
            [vivd
             [container :as container]
             [logging :as log]]))

(set! *warn-on-reflection* true)

(defn- try-load-info [config id]
  (try
    (container/load-info config id)
    (catch Exception e
      (log/debug "Ignoring unloadable container" id e)
      nil)))

(defn- read-index [config]
  (let [container-dir   (io/as-file "data/containers")
        container-files (.listFiles container-dir)
        container-ids   (map #(.getName ^java.io.File %1) container-files)
        _               (log/debug "containers" container-ids)
        container-data  (keep (partial try-load-info config) container-ids)
        container-map   (reduce #(merge %1 {(:id %2) %2}) {} container-data)
        _               (log/info "loaded containers:" container-map)]
    container-map))

(defn make [config]
  (let [out (agent {})]
    (send-off out (fn [_] (read-index config)))
    out))
