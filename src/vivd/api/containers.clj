(ns vivd.api.containers
  (:require [vivd.json-api :refer [wrap-json-api]]
            [vivd.index :as index]
            [compojure.core :refer [GET routing]]))

(defn- container-resource [{:keys [id] :as container}]
  {:id         id
   :type       "container"
   :attributes (select-keys container [:status])})

(defn- get-container [{:keys [index]} request id]
  (if-let [val (index/get index id)]
    {:status    200
     :body {:data (container-resource val)}}
    ; TODO need "errors" object?
    {:status 404}))

(defn make [services]
  (->
   (fn [request]
     (routing
      request
      (GET "/a/containers/:id" [id]
           (get-container services request id))))
   (wrap-json-api)))
