(ns vivd.refresher
  (:require [clojure.core.async :refer [go-loop <! >!! chan thread]]
            [clojure.tools.logging :as log]
            [vivd
             [container :as container]
             [index :as index]]))

(set! *warn-on-reflection* true)

(defn- do-refresh-container [config index id]
  (let [container (index/get index id)
        updated   (container/with-refreshed-status config container)]
    (if (not (= container updated))
      (do
        (log/debug "Updating due to refresh" container)
        (index/update index updated)))))

(defn- do-refresh [config index]
  (log/debug "Refreshing all...")
  (doall (map (partial do-refresh-container config index) (index/keys index))))

(defn- refresh-loop [refresh-chan config index]
  (go-loop []
    (if-let [_ (<! refresh-chan)]
      (do
        (try
          (do-refresh config index)
          (catch Exception e
            (log/error e)))
        (recur)))))

(defn refresh-now [refresher]
  (>!! refresher true))

(defn- periodically-refresh [refresh-chan]
  (thread
    (let [th (Thread/currentThread)]
      (.setName th "refresh-timer"))
    (loop []
      (log/debug "periodic refresh trigger")
      (refresh-now refresh-chan)
      (Thread/sleep (* 1000 60 30))
      (recur))))

(defn make [config index]
  "Starts the refresher service.
   This service periodically checks and updates the status of containers."
  (let [refresh-chan (chan 50)]
    (refresh-loop refresh-chan config index)
    (periodically-refresh refresh-chan)
    refresh-chan))
