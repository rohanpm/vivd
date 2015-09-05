(ns vivd.api.events
  (:require [compojure.core :refer [GET POST routing wrap-routes]]
            [immutant.web.sse :as sse]
            [clojure.tools.logging :as log]
            [clojure.data.json :as json]
            [vivd.index :as index]
            [vivd.api.containers :refer [container-resource]]))

(defn- get-events [event-channels {:keys [index]} request]
  (sse/as-channel request
   :on-open  (fn [ch]
               (log/debug "sse channel opened" ch)
               (swap! event-channels conj ch))
   :on-error (fn [ch error]
               (log/error "Error on channel" ch error))
   :on-close (fn [ch why]
               (log/debug "close" why)
               (swap! event-channels disj ch))))

(defn- send-update [ch resource]
  (sse/send! ch resource))

(defn- send-all-updates [services channels-ref c]
  (log/debug "sending update" c)
  (let [channels @channels-ref
        resource (container-resource services c)
        str      (json/write-str resource)]
    (doseq [ch channels]
           (send-update ch str))))

(defn make [config {:keys [index] :as services}]
  (fn [request]
    (let [event-channels (atom #{})]
      (index/watch-update index (partial send-all-updates services event-channels))
      (routing request
               (GET "/a/events" [:as r]
                    (get-events event-channels services r))))))
