(ns vivd.proxy
  (:refer-clojure :exclude [defn])
  (:require [clojure.data.json :as json]
            [clojure.core
             [typed :refer [typed-deps defn]]]
            [clj-http.client :as http]
            clj-time.format
            [clj-time.core :as time]
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

(defn get-proxy-headers [request]
  {"x-forwarded-for" (:remote-addr request)
   "x-forwarded-host" (get-in request [:headers "host"])})

(defn get-proxy-request [request :- ContainerRequest inspect :- DockerInspect] :- RingRequest
  (merge
   (get-host-port inspect)
   {:uri (str "/" (:container-uri request))
    :scheme :http
    :throw-exceptions false
    :headers (get-proxy-headers request)}
   (select-keys request [:query-string :request-method :body])))

(defn started-time [inspect]
  (let [timestr (get-in inspect [:State :StartedAt])]
    (clj-time.format/parse timestr)))

(defn try-request [config inspect request]
  (let [continue?
        (fn []
          (let [started (started-time inspect)
                _       (log/debug "container started at " started)
                timeout (:startup-timeout config)
                timeout (time/seconds timeout)
                deadline (time/plus- started timeout)
                _       (log/debug "deadline" deadline)
                now     (time/now)
                continue (time/after? deadline now)
                _        (log/debug "continue" continue)]
            continue))]
    (loop []
      (or (try
            (http/request request)
            (catch Exception e
              (if (not (continue?))
                (throw e))
              (log/debug "no response - will retry" e)
              (Thread/sleep 2000)
              nil))
          (recur)))))

(defn proxy-to-container [config request]
  "Proxy an HTTP request to the container with the given id"
  (let [c       (container/load-info config (:container-vivd-id request))
        _       (container/ensure-built c)
        inspect (container/ensure-started c)
        req     (get-proxy-request request inspect)
        _       (log/debug "proxy request" req)
        resp    (try-request config inspect req)
        _       (log/debug "remote response" resp)]
    resp))
