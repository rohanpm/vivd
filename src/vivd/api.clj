(ns vivd.api
  (:require [vivd
             [http :refer :all]
             [index :as index]]
            [clojure.java.io :as io]
            [clojure.data.json :as json]
            [clojure.tools.logging :as log]
            clj-time.core))

(set! *warn-on-reflection* true)

(def CHARS "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789_")
(defn- generate-id []
  (apply str (map (fn [_] (rand-nth CHARS)) (range 8))))

(defn- read-json-stream [stream]
  (with-open [reader (io/reader stream)]
    (json/read reader :key-fn keyword)))

(defn- create-handler* [index request]
  (let [body                           (:body request)
        data                           (read-json-stream body)
        {:keys [git-ref git-revision]} data
        _                              (assert git-ref "Missing git-ref in request")
        _                              (assert git-revision "Missing git-revision in request")
        id                             (generate-id)
        c                              {:id           id
                                        :git-ref      git-ref
                                        :git-revision git-revision
                                        :status       :new
                                        :timestamp    (clj-time.core/now)}]
    (index/update index c)
    (log/info "Created:" data "id:" id)
    {:status 201
     :headers {"location" (str "/" id)}}))

(defn make-create-handler [index]
  (->> (partial create-handler* index)
       (ensuring-method :post)
       (if-uri-is "/a/container")))
