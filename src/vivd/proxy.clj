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

(defn try-request [config inspect request]
  (loop [attempt 0]
    (or (try
          (http/request request)
          (catch Exception e
            (if (> attempt 5)
              (throw e))
            (log/debug "no response - will retry" attempt e)
            (Thread/sleep 500)
            nil))
        (recur (inc attempt)))))

(defn proxy-to-container [config request]
  "Proxy an HTTP request to the container with the given id"
  (let [c       (container/load-info (:container-vivd-id request))
        inspect (container/ensure-started c)
        req     (get-proxy-request request inspect)
        _       (log/debug "proxy request" req)
        resp    (try-request config inspect req)
        _       (log/debug "remote response" resp)]
    resp))
