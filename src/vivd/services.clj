(ns vivd.services
  (:require [vivd
             [index :as index]
             [container :as container]
             reap
             build]))

(set! *warn-on-reflection* true)

(defn- refresh-status [index]
  (let [containers (index/vals index)
        containers (map container/with-refreshed-status containers)]
    (doall (map #(index/update index %) containers))))

(defn make [config]
  "Starts and returns various background services (threads)."
  (let [index          (index/make)
        _              (refresh-status index)
        reaper         (vivd.reap/make config index)
        builder        (vivd.build/builder config index)]
    {:index index
     :reaper reaper
     :builder builder}))
