(ns vivd.container
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]
            [clojure.java.shell :refer [sh]]
            [clojure.data.json :as json]
            [clojure.tools.logging :as log]
            [clojure.core.cache :as cache]))

(def INSPECT-CACHE (atom (cache/ttl-cache-factory {} :ttl 60000)))
(def INFO-CACHE (atom (cache/lru-cache-factory {})))

(defn- datadir []
  "data/containers")

(defn- docker []
  "docker")

(defn- reader-for-info [id]
  (->> id
       (io/file (datadir))
       (io/reader)
       (java.io.PushbackReader.)))

(defn sh! [& args]
  (let [result (apply sh args)
        exit   (:exit result)]
    (if (not= 0 exit)
      (throw (ex-info "command failed" {:args args :result result}))
      result)))

(defn- docker-inspect* [did]
  (log/debug "DOCKER INSPECT CALLED:" did)
  (->> (sh! (docker) "inspect" did :out-enc "UTF-8")
       :out
       (json/read-str)))

(defn- docker-inspect [did]
  (let [newcache 
        (swap! INSPECT-CACHE (fn [cache]
                               (cache/through docker-inspect* cache did)))]
    (cache/lookup newcache did)))

(defn- docker-inspect-evict [did]
  (swap! INSPECT-CACHE cache/evict did))

(defn- docker-start [did]
  (docker-inspect-evict did)
  (sh! (docker) "start" did))

(defn- load-info* [id]
  (log/debug "READING INFO FOR:" id)
  (with-open [stream (reader-for-info id)]
    (edn/read stream)))

(defn load-info [id]
  (let [newcache 
        (swap! INFO-CACHE (fn [cache]
                               (cache/through load-info* cache id)))]
    (cache/lookup newcache id)))

(defn ensure-started [c]
  (let [did     (:docker-id c)
        inspect (docker-inspect did)
        _       (log/debug "inspect " inspect)
        running (get-in inspect [0 "State" "Running"])]
    (if (not running)
      (do
        (log/info "Starting:" did)
        (docker-start did)
        (docker-inspect did))
      inspect)))
