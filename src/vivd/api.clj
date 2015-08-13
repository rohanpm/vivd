(ns vivd.api
  (:require [vivd
             [http :refer :all]
             [index :as index]]
            [clojure.java.io :as io]
            [clojure.data.json :as json]
            [clojure.tools.logging :as log]
            clj-time.core
            ring.middleware.params))

(set! *warn-on-reflection* true)

(def CHARS "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789_")
(defn- generate-id []
  (apply str (map (fn [_] (rand-nth CHARS)) (range 8))))

(defn- read-json-stream [stream]
  (with-open [reader (io/reader stream)]
    (json/read reader :key-fn keyword)))

(defn- create-container [index data]
  (let [id (generate-id)
        c  (merge data {:id        id
                        :status    :new
                        :timestamp (clj-time.core/now)})]
    (index/update index c)
    (log/info "Created:" data "id:" id)
    {:status 201
     :headers {"location" (str "/" id)}}))

(defn- truthy? [x]
  (#{"1" 1 "true" true "yes"} x))

(defn- lookup-container [index {:keys [git-revision]} {:keys [query-params] :or {query-params {}}}]
  (let [unique   (query-params "unique-git-revision")
        all-c    (index/vals index)
        matching (filter #(= git-revision (:git-revision %)) all-c)]
    (if (truthy? unique)
      (if-let [{:keys [id]} (first matching)]
        {:status  302
         :headers {"location" (str "/" id)}}))))

(defn- create-handler* [index request]
  (let [request                                 (ring.middleware.params/assoc-query-params request "UTF-8")
        body                                    (:body request)
        data                                    (read-json-stream body)
        {:keys [git-ref git-revision] :as data} (select-keys data [:git-ref :git-revision])]
    (assert git-ref "Missing git-ref from request")
    (assert git-revision "Missing git-revision from request")
    (or (lookup-container index data request)
        (create-container index data))))

(defn make-create-handler [index]
  (->> (partial create-handler* index)
       (ensuring-method :post)
       (if-uri-is "/a/container")))
