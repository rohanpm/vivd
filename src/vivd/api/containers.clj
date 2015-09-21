(ns vivd.api.containers
  (:require [vivd.api.schema :refer [ContainerResourceIn]]
            [vivd.api.containers.common :refer :all]
            [vivd.api.containers.logs :refer [container-logs]]
            [vivd.api.containers.clean :refer [clean-container]]
            [vivd.json-api.middleware :refer [wrap-json-api]]
            [vivd.json-api.utils :refer [extract-resource]]
            [vivd.index :as index]
            [clojure.tools.logging :as log]
            clj-time.core
            clj-time.format
            [clojure.string :as string]
            [compojure.core :refer [GET POST routing wrap-routes]]))

(def CHARS "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789_")
(defn- generate-id []
  (apply str (map (fn [_] (rand-nth CHARS)) (range 8))))

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

(def all-query-keys ["filter[*]"
                     "page[offset]"
                     "page[limit]"])

(defn- query-string 
  ([params]
     (query-string params all-query-keys))
  ([params keys]
     (->> keys
          (map
           (fn [key]
             (if-let [val (params key)]
               (str key "=" val))))
          (keep identity)
          (string/join "&"))))

(defn- paginate-copy-params [{:keys [params] :or {params {}} :as request}]
  (let [end (query-string params ["filter[*]"])]
    (if (not (empty? end))
      (str "&" end))))

(defn- first-link [{:keys [uri] :as request} offset limit]
  (str uri "?page[offset]=0&page[limit]=" limit (paginate-copy-params request)))

(defn- link-adjusting-offset [{:keys [uri] :as request} offset limit]
  {:href 
   (str uri "?page[offset]=" offset "&page[limit]=" limit (paginate-copy-params request))
   :meta {:query-params {"page[offset]" offset}}})

(defn- next-link [request offset limit]
  (link-adjusting-offset request (+ offset limit) limit))

(defn- adjust-limit [offset limit]
  (if (> 0 offset)
    (+ offset limit)
    limit))

(defn- prev-link [{:keys [uri] :as request} offset limit]
  (let [new-offset (- offset limit)
        new-limit  (adjust-limit new-offset limit)]
    (if (< 0 new-limit)
      (link-adjusting-offset request new-offset new-limit))))

(defn- paginate [{:keys [data links] :as body} {:keys [params] :as request}]
  (let [offset     (int-param params "page[offset]" 0)
        limit      (int-param params "page[limit]" 200)
        objects    (drop offset data)
        objects    (take (+ 1 limit) objects)
        have-next  (> (count objects) limit)
        objects    (take limit objects)
        first-link (first-link request offset limit)
        next-link  (if have-next
                     (next-link request offset limit))
        prev-link  (prev-link request offset limit)]
    {:data  objects,
     :links (merge links {:first first-link,
                          :next  next-link,
                          :prev  prev-link})}))

(defn- container-matches? [needle container]
  (some #(if-let [val (% container)]
           (.contains (.toLowerCase val) needle))
        [:id :git-ref :git-revision :git-oneline]))

(defn- apply-filter [vals {:keys [params] :as request}]
  (if-let [needle (params "filter[*]")]
    (let [needle (.toLowerCase needle)]
      (filter (partial container-matches? needle) vals))
    vals))

(defn- self-link [{:keys [params uri] :as request}]
  (str uri "?" (query-string params)))

(defn get-containers [{:keys [index] :as services} request]
  (let [vals (index/vals index)
        ; TODO: allow sorting from request
        vals (reverse (sort-by :timestamp vals))
        vals (apply-filter vals request)]
    {:status 200,
     :body   (paginate
              {:data   (map (partial container-resource services) vals)
               :links  {:self   (self-link request)
                        :events "/a/events"}}
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

(defn- wrap-default-params [handler {:keys [config]}]
  (fn [{:keys [params] :as request}]
    (let [new-params  (merge {"page[limit]"  (:per-page config)
                              "page[offset]" 0}
                             params)
          new-request (merge request {:params new-params})]
      (handler new-request))))

(defn- wrap-mw [handler services]
  (-> handler
      wrap-json-api
      (wrap-default-params services)))

(defn- with-mw-handler [services]
   (fn [request]
     (routing request
      (GET "/a/containers/:id" [id :as r]
           (get-container services r id))
      (GET "/a/containers" [:as r]
           (get-containers services r))
      (POST "/a/containers" [:as r]
            (create-container services r))
      (POST "/a/containers/:id/clean" [id :as r]
            (clean-container services r id)))))

(defn- other-handler [services]
   (fn [request]
     (routing request
      (GET "/a/containers/:id/logs" [id :as r]
           (container-logs services r id)))))

(defn make [services]
  (let [mw-handler (with-mw-handler services)
        mw-handler (wrap-routes mw-handler wrap-mw services)
        other-handler (other-handler services)]
    (fn [request]
      (or
       (mw-handler request)
       (other-handler request)))))
