(ns vivd.api.events
  (:require [compojure.core :refer [GET POST routing wrap-routes]]
            [immutant.web.sse :as sse]
            [clojure.tools.logging :as log]
            [clojure.data.json :as json]
            [vivd.index :as index]
            [vivd.json-api.links :as links]
            [vivd.api.containers.common :refer [container-resource]]))

(defn- get-events [event-channels {:keys [index]} request]
  ; In order to generate events, we need to access the request
  ; (used to generate absolute URLs).
  ; Keep the needed subset of the request only.
  (let [request-lite (select-keys request [:headers :scheme :server-port :server-name])]
    (sse/as-channel request
     :on-open  (fn [ch]
                 (log/debug "sse channel opened" ch)
                 (swap! event-channels assoc ch request-lite))
     :on-error (fn [ch error]
                 ; debug, not warn/error, since broken pipe etc are normal
                 (log/debug "Error on channel" ch error))
     :on-close (fn [ch why]
                 (log/debug "close" why)
                 (swap! event-channels dissoc ch)))))

(defn- send-update [ch resource request]
  (let [prefix   (links/link-prefix request)
        resource (update resource :links (partial links/prefix-link-object prefix))
        str      (json/write-str resource)]
    (sse/send! ch str)))

(defn- send-all-updates [services channels-ref c]
  (log/debug "sending update" c)
  (let [channels @channels-ref
        resource (container-resource services c)]
    (doseq [[ch request] (seq channels)]
           (send-update ch resource request))))

(defn make [config {:keys [index] :as services}]
  (let [event-channels (atom {})]
    (index/watch-update index (partial send-all-updates services event-channels))
    (fn [request]
      (routing request
               (GET "/a/events" [:as r]
                    (get-events event-channels services r))))))
