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
            [clojure.core.typed :refer [ann typed-deps Any defalias tc-ignore let]]
            [clj-time.coerce :as time-coerce]))

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

(defn- gitdir []
  "data/git")

(ann datadir [-> String])
(defn- docker []
  "docker")

(defn- reader-for-info ^java.io.PushbackReader [file-arg]
  (reader-for-file file-arg))

(ann sh! [String Any * -> ShellResult])
(defn sh! [cmd & args]
  (log/debug "sh:" cmd args)
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
  (let [file (io/file (datadir) id)]
    (with-open [stream (reader-for-info file)]
      (merge
       (read-container-info stream)
       {:id        id
        :timestamp (-> file
                       (.lastModified)
                       (time-coerce/from-long))}))))

(ann load-info [String -> ContainerInfo])
(defn load-info [config id]
  (let [newcache 
        (swap! INFO-CACHE (fn [cache]
                               (cache/through load-info* cache id)))]
    (-> (lookup-container-info newcache id)
        (merge config))))

(defn wait-for-network [did]
  (loop [attempt 0
         inspect (docker-inspect did)]
    (if (get-in inspect [:NetworkSettings :Ports :80/tcp 0 :HostPort])
      inspect
      (if (> attempt 4)
        (throw (ex-info (str "No port 80 bound") {:docker-id did}))
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

(defn exists? [{did :docker-id :as c}]
  (if (lookup-inspect @INSPECT-CACHE did)
    (let [result (sh (docker) "inspect" did :out-enc "UTF-8")
          out    (-> result (:out) (json/read-str))
          code   (:exit result)]
      (cond
       (= 0 code) true
       (= [] out) false
       :else      (throw (ex-info "docker inspect failed" {:result result})))))
  nil)

(def GIT-INIT-LOCK (Object.))
(defn- ensure-git-init []
  (let [dir (gitdir)
        file (io/as-file dir)]
    (if (not (.exists file))
      (locking GIT-INIT-LOCK
        (if (not (.exists file))
          (do
            (log/info "Initializing" dir)
            (sh! "git" "init" "--bare" dir)))))))

(defn- have-git-revision? [rev]
  (->> rev
       (sh "git" (str "--git-dir=" (gitdir)) "rev-parse")
       :exit
       (= 0)))

(defn git! [& args]
  (apply sh! "git" (str "--git-dir=" (gitdir)) args))

(defn- git-fetch [{:keys [git-url git-ref git-local-ref git-revision]}]
  (git! "fetch" git-url (str "+" git-ref ":" git-local-ref))
  (if (not (have-git-revision? git-revision))
    (throw (ex-info (str "Fetching " git-url " " git-ref " did not provide " git-revision) {}))))

(defn- ensure-git-fetched [{:keys [git-revision] :as c}]
  (ensure-git-init)
  (if (have-git-revision? git-revision)
    (log/debug "already have" git-revision)
    (git-fetch c)))

(defn build [{:keys [git-url git-ref git-revision id] :as c}]
  (let [ref (str "refs/container/" id)
        c   (merge c {:git-local-ref ref})
        _   (ensure-git-fetched c)]
    (log/info "pretended to build")))

(defn ensure-built [{did :docker-id :as c}]
  (if (exists? c)
    c
    (build c)))


