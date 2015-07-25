(ns vivd.proxy
  (:refer-clojure :exclude [defn])
  (:require [clojure.data.json :as json]
            [clojure.core
             [typed :refer [typed-deps defn]]]
            [clj-http.client :as http]
            [vivd
             [container :as container]
             [logging :as log]
             [types :refer :all]]))

(typed-deps vivd.types)
(set! *warn-on-reflection* true)

(defn get-host-port [inspect :- DockerInspect] :- RingRequest
  (let [cfg          (get-in inspect [:NetworkSettings :Ports :80/tcp 0])
        ip           (:HostIp cfg)
        ^String port (:HostPort cfg)]
    {:server-name ip
     :server-port (Integer/valueOf port)}))

(defn get-proxy-request [request :- ContainerRequest inspect :- DockerInspect] :- RingRequest
  (merge
   (get-host-port inspect)
   {:uri (str "/" (:container-uri request))
    :scheme :http
    :throw-exceptions false}
   (select-keys request [:query-string :request-method :body])))

(defn proxy-to-container [request :- ContainerRequest] :- RingResponse
  "Proxy an HTTP request to the container with the given id"
  (let [c       (container/load-info (:container-vivd-id request))
        inspect (container/ensure-started c)
        req     (get-proxy-request request inspect)
        _       (log/debug "proxy request" req)
        resp    (http/request req)
        _       (log/debug "remote response" resp)]
    resp))
