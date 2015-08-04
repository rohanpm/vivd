(ns vivd.reap
  (:require [clojure.core.async :refer [thread]]
            [clojure.tools.logging :as log]
            [vivd
             [index :as index]
             [container :as container]]))

(defn- by-timestamp-descending [containers]
  (->> containers
       (sort-by :timestamp)
       (reverse)))

(defn- stop-containers [containers]
  (doseq [c containers]
    (log/info "Stopping: " (:id c))
    (container/stop c)))

(defn- reap-running [{:keys [max-containers-up]} {:keys [index-ref] :as index}]
  (let [containers (vals @index-ref)
        running    (filter container/container-running? containers)
        running    (by-timestamp-descending running)
        to-stop    (drop max-containers-up running)]
    (stop-containers to-stop)))

(defn- do-reap [config index]
  (log/debug "Reaping...")
  (reap-running config index))

(defn- run [config index]
  (let [th (Thread/currentThread)]
    (.setName th "vivd-reaper")
    (loop []
      (try
        (do-reap config index)
        (catch Exception e
            (log/warn "Problem during reaping" e)))
      (Thread/sleep (* 1000 60 10))
      (recur))))

(defn run-async [config index]
  "Start a reaper thread, in the background."
  (thread
    (run config index)))
