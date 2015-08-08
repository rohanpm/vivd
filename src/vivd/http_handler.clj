(ns vivd.http_handler
  (:require [vivd
             [proxy :as proxy]
             [container :as container]
             [index :as index]
             [index-page :as index-page]
             [build :as build]
             [reap :as reap]
             [api :as api]
             [http :refer :all]]
            [ring.util.response :refer :all]
            [clojure.tools.logging :as log]
            [clojure.java.io :as io]
            [clojure.string :as str]))

(set! *warn-on-reflection* true)

(defn make-index-handler [config index]
  (->> (fn [request]
         {:status 200
          :headers {"content-type" "text/html"}
          :body (index-page/from-index config index)})
       (ensuring-method :get)
       (if-uri-is "/")))

(defn- augmented-proxy-request [request]
  (let [uri         (:uri request)
        uri-parts   (str/split uri #"/" 3)
        [_ id rest] uri-parts]
    (assert (not (nil? id)))
    (merge request {:container-vivd-id id
                    :container-uri rest})))

(defn- make-proxy-handler [config builder index]
  (fn [request]
    (let [{:keys [container-vivd-id] :as request} (augmented-proxy-request request)]
      (if (index/get index container-vivd-id)
        (proxy/proxy-to-container request config builder index)
        (do
          (log/debug "doesn't look like a valid container - " container-vivd-id)
          {:status 404
           :body "resource not found"})))))

(defn- vivd-url [{:keys [headers]}]
  (if-let [host (headers "host")]
    ; FIXME: do not assume http, do not assume served at /
    (str "http://" host "/")))

(defn- request-referred-from-container? [{:keys [headers] :as request} id]
  (if-let [^String referer (headers "referer")]
    (let [container-url (str (vivd-url request) id)]
      (or (= referer container-url)
          (.startsWith referer (str container-url "/"))))))

(defn- redirect-handler* [index {:keys [^String uri query-string headers] :as request}]
  (let [all-c           (index/keys index)
        matches         (partial request-referred-from-container? request)
        match-c         (filter matches all-c)
        id              (first match-c)]
    (if (and id
             (not (.startsWith uri (str "/" id))))
      (let [relative-dest (str id uri)
            dest          (str (vivd-url request) relative-dest)
            dest          (if query-string
                            (str dest "?" query-string)
                            dest)]
        (log/debug "redirect to" dest)
        {:status 307
         :headers {"location" dest}}))))

(defn- make-redirect-handler [index]
  "Redirect requests which appear to accidentally escape the container"
  (partial redirect-handler* index))

(defn guess-content-type [^String filename]
  (cond
   (.endsWith filename ".js")  "application/javascript; charset=utf-8"
   (.endsWith filename ".css") "text/css; charset=utf-8"))

(defn resource-handler* [{:keys [^String uri] :as request}]
  (let [relative (.substring uri 1)]
    (log/debug "looking for resource" relative)
    (if-let [resource (io/resource relative)]
      {:status 200
       :headers (if-let [type (guess-content-type relative)]
                  {"content-type" type}
                  {})
       :body (io/input-stream resource)}
      {:status 404
       :body "resource not found"})))

(def resource-handler
  (->> resource-handler*
       (if-uri-starts-with "/public/")))

(defn- refresh-status [index]
  (let [containers (index/vals index)
        containers (map container/with-refreshed-status containers)]
    (doall (map #(index/update index %) containers))))

(defn make-handler [config]
  "Returns a top-level handler for all HTTP requests"
  (let [index          (index/make)
        _              (refresh-status index)
        _              (reap/run-async config index)
        builder        (build/builder config index)
        all-handlers   [(make-redirect-handler index)
                        resource-handler
                        (make-index-handler config index)
                        (api/make-create-handler index)
                        (make-proxy-handler config builder index)]]
    (fn [request]
      (first (keep #(% request) all-handlers)))))
