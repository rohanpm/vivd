(ns vivd.container
  (:refer-clojure :exclude [remove])
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]
            [clojure.java.shell :refer [sh]]
            [clojure.data.json :as json]
            [clojure.tools.logging :as log]
            [vivd.utils :refer :all]
            [vivd.builder :as builder]
            [clojure.core.cache :as cache]
            [clojure.core.async :refer [<!!]]
            [clojure.string :refer [trim]]
            [clj-time.coerce :as time-coerce]
            [clj-time.core :as time]
            [clj-http.client :as http]
            clj-time.format))

(set! *warn-on-reflection* true)

(def INSPECT-CACHE (atom
                    (cache/ttl-cache-factory {} :ttl 60000)))

(defn- lookup-inspect [cache id]
  (cache/lookup cache id))

(defn- read-container-info [stream]
  (edn/read stream))

(defn- lookup-container-info [cache id]
  (cache/lookup cache id))

(defn- gitdir []
  "data/git")

(defn- docker []
  "docker")

(defn- reader-for-info ^java.io.PushbackReader [file-arg]
  (reader-for-file file-arg))

(defn- read-docker-inspect [str]
  (-> str
      (json/read-str :key-fn keyword)
      (first)))

(defn- docker-inspect* [did]
  (log/debug "DOCKER INSPECT CALLED:" did)
  (->> (sh! (docker) "inspect" did :out-enc "UTF-8")
       :out
       (read-docker-inspect)))

(defn- docker-inspect [did]
  (let [newcache 
        (swap! INSPECT-CACHE (fn [cache]
                               (cache/through docker-inspect* cache did)))]
    (lookup-inspect newcache did)))

(defn- docker-inspect-evict [did]
  (swap! INSPECT-CACHE cache/evict did))

(defn- docker-start [did]
  (docker-inspect-evict did)
  (sh! (docker) "start" did))

(defn- docker-stop [did]
  (docker-inspect-evict did)
  (sh! (docker) "stop" did))

(defn- docker-rm [did]
  (docker-inspect-evict did)
  (sh! (docker) "rm" "-f" "-v" did))

(defn- docker-rmi [image-id]
  (sh! (docker) "rmi" "-f" image-id))

(defn- port-key [{:keys [docker-http-port]}]
  (keyword (str docker-http-port "/tcp")))

(defn wait-for-network [config did]
  (loop [attempt 0
         inspect (docker-inspect did)]
    (if (get-in inspect [:NetworkSettings :Ports (port-key config) 0 :HostPort])
      inspect
      (if (> attempt 4)
        (throw (ex-info (str "HTTP port was not bound in container") {:docker-container-id did}))
        (do
          (Thread/sleep 200)
          (docker-inspect-evict did)
          (recur (inc attempt) (docker-inspect did)))))))

(defn create-container [{:keys [docker-http-port docker-run-arguments] :as config} {:keys [docker-image-id] :as c}]
  (let [cmd          (concat ["docker" "run" "-d" "-p" (str "127.0.0.1::" docker-http-port)]
                             docker-run-arguments
                             [docker-image-id])
        container-id (-> (apply sh! cmd)
                         (:out)
                         (trim))
        _            (log/info "Created container" container-id "from image" docker-image-id)]
    container-id))

(defn running? [{:keys [docker-container-id]}]
  (if docker-container-id
    (let [inspect (docker-inspect docker-container-id)]
      (get-in inspect [:State :Running]))))

(defn ensure-started [config {:keys [docker-container-id] :as c}]
  (let [docker-container-id (or docker-container-id (create-container config c))
        c                   (merge c {:docker-container-id docker-container-id})
        running             (running? c)]
    (if (not running)
      (do
        (log/info "Starting:" docker-container-id)
        (docker-start docker-container-id)))
    (wait-for-network config docker-container-id)
    c))

(defn stop [{:keys [docker-container-id] :as c}]
  (docker-stop docker-container-id))

(defn remove [{:keys [docker-container-id] :as c}]
  (let [inspect  (docker-inspect docker-container-id)
        image-id (:Image inspect)]
    (docker-rm docker-container-id)
    (log/info "Removed container" docker-container-id)
    (try
      (docker-rmi image-id)
      (log/info "Removed image" image-id)
      (catch Exception e
          ; tolerating rmi errors because the image can still be in use by
          ; another container.
          (log/warn "rmi failed (may be OK)" e)))))

(defn container-exists? [{:keys [docker-container-id] :as c}]
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
  (->> (str rev "^{commit}")
       (sh "git" (str "--git-dir=" (gitdir)) "rev-parse" "--verify")
       :exit
       (= 0)))

(defn git! [& args]
  (apply sh! "git" (str "--git-dir=" (gitdir)) args))

(defn- git-fetch [{:keys [git-url git-local-ref] :as config} {:keys [git-ref git-revision]}]
  (log/debug "git-fetch" config)
  (assert git-url)
  (assert git-local-ref)
  (git! "fetch" git-url (str "+" git-ref ":" git-local-ref))
  (if (not (have-git-revision? git-revision))
    (throw (ex-info (str "Fetching " git-url " " git-ref " did not provide " git-revision) {}))))

(defn- git-log [& args]
  (-> (apply git! "log" "-n1" args)
      (:out)))

(defn- extended-git-info [{:keys [git-revision]}]
  {:git-oneline (git-log git-revision "--oneline")
   :git-log     (git-log git-revision)})

(defn- ensure-git-fetched [config {:keys [git-revision] :as c}]
  (ensure-git-init)
  (if (have-git-revision? git-revision)
    (log/debug "already have" git-revision)
    (git-fetch config c)))

(defn build [config {:keys [git-ref git-revision id] :as c} builder]
  (let [ref    (str "refs/container/" id)
        config (merge config {:git-local-ref ref :git-dir (gitdir)})
        _      (log/debug "config now" config)
        _      (ensure-git-fetched config c)]
    (log/debug "requesting build")
    (let [c (<!! (builder/request-build builder c))]
      (assert c (str "Container failed to build for " git-revision))
      c)))

(defn- image-exists? [{:keys [docker-image-id]}]
  (if (not docker-image-id)
    false
    (-> (sh "docker" "inspect" docker-image-id)
        (:exit)
        (= 0))))

(defn ensure-built [config {:keys [docker-container-id] :as c} builder]
  (cond
   ; if a container was created, the image must exist too...
   docker-container-id
   c

   (image-exists? c)
   c

   :else
   (let [c (build config c builder)]
     (merge c
            (extended-git-info c)))))

(defn get-host-port [config {:keys [docker-container-id] :as c}]
  (let [inspect      (docker-inspect docker-container-id)
        cfg          (get-in inspect [:NetworkSettings :Ports (port-key config) 0])
        ip           (:HostIp cfg)
        ^String port (:HostPort cfg)]
    [ip
     (Integer/valueOf port)]))

(defn started-time [{:keys [docker-container-id] :as c}]
  (let [inspect (docker-inspect docker-container-id)
        timestr (get-in inspect [:State :StartedAt])]
    (clj-time.format/parse timestr)))

(defn- starting-container-overdue? [{:keys [startup-timeout] :as config} {:keys [timestamp] :as c}]
  (let [now      (time/now)
        how-long (time/interval timestamp now)
        timeout  (* 4 startup-timeout)
        secs     (time/in-seconds how-long)]
    (< timeout secs)))

(defn- test-starting-container [{:keys [default-url startup-timeout] :as config} {:keys [timestamp] :as c}]
  (log/debug "Checking starting container" c)
  ; If the container is far beyond the timeout, don't even try...
  (if (starting-container-overdue? config c)
    (do
      (log/debug "Container is overdue: " c)
      :timed-out)
    (let [[ip port] (get-host-port config c)
          request   {:uri              default-url
                     :scheme           :http
                     :throw-exceptions true
                     :server-name      ip
                     :server-port      port
                     :follow-redirects false
                     :request-method   :get
                     :conn-timeout     10000
                     :socket-timeout   10000}]
      (try
        (http/request request)
        (log/debug "Successfully contacted container" c)
        :up
        (catch Exception e
          (log/debug "Container not (yet?) accessible" c e)        
          :starting)))))

(defn- running-status? [status]
  (some #(= status %) [:up :starting]))

(defn with-refreshed-status [config {:keys [docker-container-id status] :as c}]
  "Refreshes stale container :status value."
  ; Need to evict, since the whole point is to get the up-to-date status...
  (docker-inspect-evict docker-container-id)
  (let [status (cond
                (and (not docker-container-id) (not (= status :building)))
                :new

                (and (running-status? status)
                     (not (try
                            (running? c)
                            (catch Exception e
                              (log/warn "Can't determine if" c "is running (assuming not)" e)
                              false))))
                :stopped

                (= :starting status)
                (test-starting-container config c)

                :else
                (:status c))]
    (merge c {:status status})))
