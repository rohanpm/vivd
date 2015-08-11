(ns vivd.services
  (:require [vivd
             [index :as index]
             [container :as container]
             reap
             refresher
             build]))

(set! *warn-on-reflection* true)

(defn make [config]
  "Starts and returns various background services (threads)."
  (let [index          (index/make)
        reaper         (vivd.reap/make config index)
        refresher      (vivd.refresher/make config index)
        builder        (vivd.build/builder config index)]
    {:index     index
     :reaper    reaper
     :refresher refresher
     :builder   builder}))
