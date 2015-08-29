(ns vivd.services
  (:require [vivd
             [index :as index]
             [container :as container]
             reaper
             refresher
             builder]
            [vivd.react.renderer :as renderer]))

(set! *warn-on-reflection* true)

(defn make [config]
  "Starts and returns various background services (threads)."
  (let [index          (index/make)
        reaper         (vivd.reaper/make config index)
        refresher      (vivd.refresher/make config index)
        builder        (vivd.builder/make config index)
        renderer       (renderer/make)]
    {:index     index
     :reaper    reaper
     :refresher refresher
     :builder   builder
     :renderer  renderer}))
