(ns vivd.api.containers
  (:require [vivd.json-api :refer [wrap-json-api]]
            [vivd.index :as index]
            [clojure.tools.logging :as log]
            [compojure.core :refer [GET routing wrap-routes]]))

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

(defn- int-param [params key default]
  (if-let [val (params key)]
    (Integer/valueOf val)
    default))

(defn- first-link [uri offset limit]
  (str uri "?page[offset]=0&page[limit]=" limit))

(defn- next-link [uri offset limit]
  (str uri "?page[offset]=" (+ offset limit) "&page[limit]=" limit))

(defn- adjust-limit [offset limit]
  (if (> 0 offset)
    (+ offset limit)
    limit))

(defn- prev-link [uri offset limit]
  (let [new-offset (- offset limit)
        new-limit  (adjust-limit new-offset limit)]
    (if (< 0 new-limit)
      (str uri "?page[offset]=" (max 0 new-offset) "&page[limit]=" new-limit))))

(defn- paginate [{:keys [data] :as body} {:keys [params uri] :as request}]
  (let [offset     (int-param params "page[offset]" 0)
        limit      (int-param params "page[limit]" 200)
        objects    (drop offset data)
        objects    (take (+ 1 limit) objects)
        have-next  (> (count objects) limit)
        objects    (take limit objects)
        first-link (first-link uri offset limit)
        next-link  (if have-next
                     (next-link uri offset limit))
        prev-link  (prev-link uri offset limit)]
    {:data  objects,
     :links {:first first-link,
             :next  next-link,
             :prev  prev-link}}))

(defn- get-containers [{:keys [index]} request]
  (let [vals (index/vals index)]
    {:status 200,
     :body   (paginate
              {:data (map container-resource vals)}
              request)}))

(defn make [services]
  (wrap-routes
   (fn [request]
     (routing request
      (GET "/a/containers/:id" [id :as r]
           (get-container services r id))
      (GET "/a/containers" [:as r]
           (get-containers services r))))
   wrap-json-api))
