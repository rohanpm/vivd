(ns vivd.api.containers
  (:require [vivd.api.schema :refer [ContainerResourceIn]]
            [vivd.json-api.middleware :refer [wrap-json-api]]
            [vivd.json-api.utils :refer [extract-resource]]
            [vivd.index :as index]
            [clojure.tools.logging :as log]
            clj-time.core
            clj-time.format
            [compojure.core :refer [GET POST routing wrap-routes]]))

(def CHARS "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789_")
(defn- generate-id []
  (apply str (map (fn [_] (rand-nth CHARS)) (range 8))))

(defn- links-for-container [{:keys [config]} {:keys [id] :as container}]
  (let [{:keys [default-url]} config]
    {:self (str "/a/containers/" id)
     :app  (str "/" id default-url)}))

(def iso8601-formatter
  (clj-time.format/formatters :date-time))

(defn- iso8601 [timestamp]
  (clj-time.format/unparse iso8601-formatter timestamp))

(defn- container-resource-attributes [{:keys [timestamp] :as container}]
  (merge
   (select-keys container [:status :git-ref :git-revision :git-oneline])
   {:timestamp (iso8601 timestamp)}))

(defn- container-resource [services {:keys [id] :as container}]
  {:id         id
   :type       "containers"
   :attributes (container-resource-attributes container)
   :links      (links-for-container services container)})

(defn- get-container [{:keys [index] :as services} request id]
  (if-let [val (index/get index id)]
    {:status    200
     :body {:data (container-resource services val)}}
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

(defn get-containers [{:keys [index] :as services} request]
  (let [vals (index/vals index)]
    {:status 200,
     :body   (paginate
              {:data (map (partial container-resource services) vals)}
              request)}))

(defn- truthy? [x]
  (#{"1" 1 "true" true "yes"} x))

(defn- lookup-by-git-revision [{:keys [index]} git-revision]
  (->> index
       (index/vals)
       (filter #(= git-revision (:git-revision %)))
       (first)))

(defn- force-create-container [{:keys [index]} data]
  (let [id (generate-id)
        c  (merge data {:id        id
                        :status    :new
                        :timestamp (clj-time.core/now)})]
    (index/update index c)
    (log/info "Created:" data "id:" id)
    c))

(defn- create-container [services {:keys [body params] :as request}]
  (let [{:keys [attributes]}           (extract-resource body ContainerResourceIn)
        {:keys [git-ref git-revision]} attributes
        unique-git-revision            (params "unique-git-revision")]
    (log/debug "create" params attributes)
    (if-let [{:keys [id]} (and (truthy? unique-git-revision)
                               (lookup-by-git-revision services git-revision))]
      {:status  303,
       :headers {"location" (str "/a/containers/" id)}}
      (let [{:keys [id] :as created} (force-create-container
                                      services
                                      {:git-ref git-ref :git-revision git-revision})]
        {:status  201
         :body    {:data (container-resource services created)}
         :headers {"location" (str "/a/containers/" id)}}))))

(defn make [services]
  (wrap-routes
   (fn [request]
     (routing request
      (GET "/a/containers/:id" [id :as r]
           (get-container services r id))
      (GET "/a/containers" [:as r]
           (get-containers services r))
      (POST "/a/containers" [:as r]
            (create-container services r))))
   wrap-json-api))
