(ns vivd.reaper
  (:require [clojure.core.async :refer [thread]]
            [clojure.tools.logging :as log]
            [vivd
             [index :as index]
             [container :as container]]))

(defn- try-container-running? [c]
  (try
    (if (:docker-container-id c)
      (container/running? c)
      false)
    (catch Exception e
      (log/debug "swallowed container/running? exception" e)
      false)))

(defn- by-timestamp-descending [containers]
  (->> containers
       (sort-by :timestamp)
       (reverse)))

(defn- stop-containers [index containers]
  (doseq [c containers]
    (log/info "Stopping: " (:id c))
    (index/update index (merge c {:status :stopping}))
    (container/stop c)
    (index/update index (merge c {:status :stopped}))))

(defn- remove-containers [index containers]
  (doseq [{:keys [id docker-container-id] :as c} containers]
    (try
      (if docker-container-id
        (container/remove c))
      (index/remove index id)
      (log/info "Removed:" id)
      (catch Exception e
        (log/warn "Problem removing:" id e)))))

(defn- reap-running [{:keys [max-containers-up]} index]
  {:pre [max-containers-up]}
  (let [containers (index/vals index)
        running    (filter try-container-running? containers)
        running    (by-timestamp-descending running)
        to-stop    (drop max-containers-up running)]
    (stop-containers index to-stop)))

(defn- reap-stopped [{:keys [max-containers]} index]
  (let [containers (index/vals index)
        stopped    (remove try-container-running? containers)
        stopped    (by-timestamp-descending stopped)
        to-remove  (drop max-containers stopped)]
    (remove-containers index to-remove)))

(defn- do-reap [config index]
  (log/debug "Reaping...")
  (reap-running config index)
  (reap-stopped config index))

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

(defn make [config index]
  "Start a reaper thread, in the background."
  (thread
    (run config index)))
