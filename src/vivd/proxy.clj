(ns vivd.proxy
  (:require [clojure.data.json :as json]
            [clj-http.client :as http]
            [clj-time.core :as time]
            [clojure.tools.logging :as log]
            [vivd
             [container :as container]
             [index :as index]]))

(set! *warn-on-reflection* true)

(defn get-proxy-headers [{:keys [headers remote-addr]} ip port]
  (-> headers
      (dissoc "content-length")
      (merge {"host"             ip
              "connection"       "close"
              "x-forwarded-for"  remote-addr
              "x-forwarded-host" (headers "host")})))

; For proxying a request, we need the stream to be buffered, so that
; we can retry if something goes wrong. (ignore close for the same reason)
(defn- to-buffered-stream [stream]
  (if stream
    (proxy [java.io.BufferedInputStream] [stream 16384]
      (close []))))

(defn- buffer-request-body [{:keys [body] :as request}]
  (if body
    (update request :body to-buffered-stream)
    request))

(defn get-proxy-request [{:keys [container-uri body] :as request} config c]
  (let [[ip port] (container/get-host-port config c)
        request   (buffer-request-body request)]
    (merge
     {:uri (str "/" container-uri)
      :scheme :http
      :throw-exceptions false
      :headers (get-proxy-headers request ip port)
      :server-name ip
      :server-port port
      :as :stream
      :follow-redirects false}
     (select-keys request [:query-string :request-method :body]))))

(defn try-request [config {:keys [^java.io.InputStream body] :as request} c]
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
      (if body (.mark body 16384))
      (or (try
            (http/request request)
            (catch Exception e
              (if (not (continue?))
                (throw e))
              (log/debug "no response - will retry" e)
              (if body (.reset body))
              (Thread/sleep 2000)
              nil))
          (recur)))))

(defn proxy-to-container [request config builder index]
  "Proxy an HTTP request to the container with the given id"
  (let [c       (index/get index (:container-vivd-id request))
        c       (merge c {:timestamp (time/now)})
        c       (container/ensure-built config c builder)
        _       (index/update index c)
        _       (log/debug "after build" c)
        c       (container/ensure-started config c)
        _       (index/update index c)
        req     (get-proxy-request request config c)
        _       (log/debug "proxy request" req)
        _       (if (not (= :up (:status c)))
                  (index/update index (merge c {:status :starting})))
        resp    (try-request config req c)
        _       (index/update index (merge c {:status :up}))
        _       (log/debug "remote response" resp)]
    resp))
