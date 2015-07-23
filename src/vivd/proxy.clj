(ns vivd.proxy
  (:require [clojure.data.json :as json]
            [vivd.container :as container]))

(defn proxy-to-container [request]
  "Proxy an HTTP request to the container with the given id"
  (let [c (container/load-info (:container-vivd-id request))]
    (do
      (container/ensure-started c)
      {:status 200
       :body   (json/write-str c)})))
