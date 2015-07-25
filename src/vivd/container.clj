(ns vivd.container
  (:refer-clojure :exclude [let])
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]
            [clojure.java.shell :refer [sh]]
            [clojure.data.json :as json]
            [vivd.logging :as log]
            [vivd.types :refer :all]
            [vivd.utils :refer :all]
            [clojure.core.cache :as cache]
            [clojure.core.typed :refer [ann typed-deps Any defalias tc-ignore let]]))

(typed-deps vivd.types)
(set! *warn-on-reflection* true)

(def INSPECT-CACHE (atom
                    (tc-ignore
                     (cache/ttl-cache-factory {} :ttl 60000))))

(def INFO-CACHE (atom
                 (tc-ignore
                  (cache/lru-cache-factory {}))))

(ann ^:no-check lookup-inspect [Any String -> DockerInspect])
(tc-ignore
 (defn- lookup-inspect [cache id]
   (cache/lookup cache id)))

(ann ^:no-check read-container-info [java.io.PushbackReader -> ContainerInfo])
(tc-ignore
 (defn- read-container-info [stream]
   (edn/read stream)))

(ann ^:no-check lookup-container-info [Any String -> ContainerInfo])
(tc-ignore
 (defn- lookup-container-info [cache id]
   (cache/lookup cache id)))

(ann datadir [-> String])
(defn- datadir []
  "data/containers")

(ann datadir [-> String])
(defn- docker []
  "docker")

(ann reader-for-info [String -> java.io.PushbackReader])
(defn- reader-for-info ^java.io.PushbackReader [id]
  (reader-for-file (datadir) id))

(ann sh! [String Any * -> ShellResult])
(defn sh! [cmd & args]
  (let [result (apply sh cmd args)
        exit   (:exit result)]
    (if (not= 0 exit)
      (throw (ex-info "command failed" {:result result}))
      result)))

(ann ^:no-check read-docker-inspect [String -> DockerInspect])
(tc-ignore
 (defn- read-docker-inspect [str]
   (-> str
       (json/read-str :key-fn keyword)
       (first))))

(ann docker-inspect* [String -> DockerInspect])
(defn- docker-inspect* [did]
  (log/debug "DOCKER INSPECT CALLED:" did)
  (->> (sh! (docker) "inspect" did :out-enc "UTF-8")
       :out
       (read-docker-inspect)))

(ann docker-inspect [String -> DockerInspect])
(defn- docker-inspect [did]
  (let [newcache 
        (swap! INSPECT-CACHE (fn [cache]
                               (cache/through docker-inspect* cache did)))]
    (lookup-inspect newcache did)))

(ann docker-inspect-evict [String -> Any])
(defn- docker-inspect-evict [did]
  (swap! INSPECT-CACHE cache/evict did))

(ann docker-start [String -> ShellResult])
(defn- docker-start [did]
  (docker-inspect-evict did)
  (sh! (docker) "start" did))

(ann load-info* [String -> ContainerInfo])
(defn- load-info* [id]
  (log/debug "READING INFO FOR:" id)
  (with-open [stream (reader-for-info id)]
    (read-container-info stream)))

(ann load-info [String -> ContainerInfo])
(defn load-info [id]
  (let [newcache 
        (swap! INFO-CACHE (fn [cache]
                               (cache/through load-info* cache id)))]
    (lookup-container-info newcache id)))

(defn wait-for-network [did]
  (loop [attempt 0
         inspect (docker-inspect did)]
    (if (get-in inspect [:NetworkSettings :Ports :80/tcp 0 :HostPort])
      inspect
      (if (> attempt 4)
        (throw (ex-info (str "No port 80 bound on " did)))
        (do
          (Thread/sleep 200)
          (docker-inspect-evict did)
          (recur (inc attempt) (docker-inspect did)))))))

(ann ensure-started [ContainerInfo -> DockerInspect])
(defn ensure-started [c]
  (let [did     (:docker-id c)
        inspect (docker-inspect did)
        _       (log/debug "inspect " inspect)
        running (get-in inspect [:State :Running])]
    (if (not running)
      (do
        (log/info "Starting:" did)
        (docker-start did)))
    (wait-for-network did)))
