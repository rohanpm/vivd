(ns vivd.api.containers
  (:require [vivd.json-api :refer [wrap-json-api]]
            [vivd.index :as index]
            [compojure.core :refer [GET routing]]))

; TODO link (at least to self)
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

(defn- get-containers [{:keys [index]} request]
  (let [vals (index/vals index)]
    {:status 200
     ; TODO paginate
     :body   {:data (map container-resource vals)}}))

(defn make [services]
  (->
   (fn [request]
     (routing
      request

      (GET "/a/containers/:id" [id]
           (get-container services request id))

      (GET "/a/containers" []
           (get-containers services request))

      ))
   (wrap-json-api)))
