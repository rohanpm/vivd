(ns vivd.proxy
  (:require [clojure.data.json :as json]
            [clojure.core
             [typed :refer [ann typed-deps]]]
            [vivd
             [container :as container]
             [logging :as log]
             [types :refer :all]]))

(typed-deps vivd.types)
(set! *warn-on-reflection* true)

(ann proxy-to-container [ContainerRequest -> RingResponse])
(defn proxy-to-container [request]
  "Proxy an HTTP request to the container with the given id"
  (let [c (container/load-info (:container-vivd-id request))]
    (do
      (container/ensure-started c)
      {:status 200
       :body   (json/write-str c)})))
