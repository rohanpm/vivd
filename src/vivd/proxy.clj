(ns vivd.proxy
  (:refer-clojure :exclude [defn])
  (:require [clojure.data.json :as json]
            [clojure.core
             [typed :refer [typed-deps defn]]]
            [clj-http.client :as http]
            [clj-time.core :as time]
            [vivd
             [container :as container]
             [logging :as log]
             [index :as index]
             [types :refer :all]]))

(typed-deps vivd.types)
(set! *warn-on-reflection* true)

(defn get-proxy-headers [request]
  {"x-forwarded-for" (:remote-addr request)
   "x-forwarded-host" (get-in request [:headers "host"])})

(defn get-proxy-request [request c]
  (let [[ip port] (container/get-host-port c)]
    (merge
     {:uri (str "/" (:container-uri request))
      :scheme :http
      :throw-exceptions false
      :headers (get-proxy-headers request)
      :server-name ip
      :server-port port}
     (select-keys request [:query-string :request-method :body]))))

(defn try-request [config request c]
  (let [continue?
        (fn []
          (let [started (container/started-time c)
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

(defn proxy-to-container [request config builder index]
  "Proxy an HTTP request to the container with the given id"
  (let [c       (index/get index (:container-vivd-id request))
        c       (container/ensure-built c builder)
        _       (index/update index c)
        _       (log/debug "after build" c)
        c       (container/ensure-started config c)
        _       (index/update index c)
        req     (get-proxy-request request c)
        _       (log/debug "proxy request" req)
        resp    (try-request config req c)
        _       (log/debug "remote response" resp)]
    resp))
