(ns vivd.container
  (:refer-clojure :exclude [let])
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]
            [clojure.java.shell :refer [sh]]
            [clojure.data.json :as json]
            [vivd.logging :as log]
            [vivd.types :refer :all]
            [vivd.utils :refer :all]
            [vivd.build :as build]
            [clojure.core.cache :as cache]
            [clojure.core.typed :refer [ann typed-deps Any defalias tc-ignore let]]
            [clojure.core.async :refer [<!!]]
            [clojure.string :refer [trim]]
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
        (throw (ex-info (str "No port 80 bound") {:docker-container-id did}))
        (do
          (Thread/sleep 200)
          (docker-inspect-evict did)
          (recur (inc attempt) (docker-inspect did)))))))

(defn create-container [{:keys [docker-image-id docker-http-port] :as c}]
  (let [container-id (-> (sh! "docker" "run" "-d" "-p" (str "127.0.0.1::" docker-http-port) docker-image-id)
                         (:out)
                         (trim))
        _            (log/info "Created container" container-id "from image" docker-image-id)]
    container-id))

(defn ensure-started [{:keys [docker-container-id] :as c}]
  (let [docker-container-id (or docker-container-id (create-container c))
        c                   (merge c {:docker-container-id docker-container-id})
        inspect             (docker-inspect docker-container-id)
        _                   (log/debug "inspect " inspect)
        running             (get-in inspect [:State :Running])]
    (if (not running)
      (do
        (log/info "Starting:" docker-container-id)
        (docker-start docker-container-id)))
    (wait-for-network docker-container-id)))

(defn container-exists? [{:keys [:docker-container-id] :as c}]
  (if (lookup-inspect @INSPECT-CACHE docker-container-id)
    (let [result (sh (docker) "inspect" docker-container-id :out-enc "UTF-8")
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

(defn build [{:keys [git-url git-ref git-revision id builder] :as c}]
  (let [ref (str "refs/container/" id)
        c   (merge c {:git-local-ref ref :git-dir (gitdir)})
        _   (ensure-git-fetched c)]
    (log/debug "requesting build")
    (let [new-image (<!! (build/request-build builder c))]
      (log/info "Built image" new-image "for" git-revision)
      new-image)))

(defn- image-exists? [{:keys [docker-image-id]}]
  (if (not docker-image-id)
    false
    (-> (sh "docker" "inspect" docker-image-id)
        (:exit)
        (= 0))))

(defn ensure-built [{:keys [docker-container-id] :as c}]
  (cond
   ; if a container was created, the image must exist too...
   docker-container-id
     c
   (image-exists? c)
     c
   :else
     (merge c {:docker-image-id (build c)})))
