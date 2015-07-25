(ns vivd.proxy
  (:refer-clojure :exclude [defn])
  (:require [clojure.data.json :as json]
            [clojure.core
             [typed :refer [typed-deps defn]]]
            [vivd
             [container :as container]
             [logging :as log]
             [types :refer :all]]))

(typed-deps vivd.types)
(set! *warn-on-reflection* true)

(defn proxy-to-container [request :- ContainerRequest] :- RingResponse
  "Proxy an HTTP request to the container with the given id"
  (let [c       (container/load-info (:container-vivd-id request))
        inspect (container/ensure-started c)]
    {:status 200
     :body   (json/write-str c)}))
