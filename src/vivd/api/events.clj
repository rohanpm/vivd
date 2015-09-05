(ns vivd.api.events
  (:require [compojure.core :refer [GET POST routing wrap-routes]]
            [immutant.web.sse :as sse]
            [clojure.tools.logging :as log]
            [clojure.data.json :as json]
            [vivd.index :as index]
            [vivd.json-api.links :as links]
            [vivd.api.containers :refer [container-resource]]))

(defn- get-events [event-channels {:keys [index]} request]
  (sse/as-channel request
   :on-open  (fn [ch]
               (log/debug "sse channel opened" ch)
               (swap! event-channels conj ch))
   :on-error (fn [ch error]
               ; debug, not warn/error, since broken pipe etc are normal
               (log/debug "Error on channel" ch error))
   :on-close (fn [ch why]
               (log/debug "close" why)
               (swap! event-channels disj ch))))

(defn- send-update [ch resource]
  (sse/send! ch resource))

(defn- send-all-updates [services channels-ref link-prefix c]
  (log/debug "sending update" c)
  (let [channels @channels-ref
        resource (container-resource services c)
        resource (update resource :links (partial links/prefix-link-object @link-prefix))
        str      (json/write-str resource)]
    (doseq [ch channels]
           (send-update ch str))))

(defn make [config {:keys [index] :as services}]
  (let [event-channels (atom #{})
        ; bit of a hack.  we need a request to figure out prefix for absolute links,
        ; but doesn't matter which one. So we just use the first one for that.
        link-prefix (atom nil)]
    (index/watch-update index (partial send-all-updates services event-channels link-prefix))
    (fn [request]
      (swap! link-prefix
             (fn [val]
               (or val (links/link-prefix request))))
      (routing request
               (GET "/a/events" [:as r]
                    (get-events event-channels services r))))))
