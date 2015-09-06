(ns vivd.resource-handler
  (:require [vivd.http :refer :all]
            [clojure.java.io :as io]
            [clojure.tools.logging :as log]
            [clj-time.format :refer [formatter unparse]]
            [clj-time.core :refer [now plus years]]))

(set! *warn-on-reflection* true)

(defn guess-content-type [^String filename]
  (cond
   (.endsWith filename ".js")  "application/javascript; charset=utf-8"
   (.endsWith filename ".css") "text/css; charset=utf-8"))

(def expires-formatter
  ;Thu, 01 Dec 1994 16:00:00 GMT
  (formatter "EEE, dd MMM yyyy HH:mm:ss zzz"))

(defn- distant-expires []
  (->> 2
       (years)
       (plus (now))
       (unparse expires-formatter)))

(defn- headers-for-resource [^String name]
  (merge
   {}
   (if-let [type (guess-content-type name)]
     {"content-type" type})
   (if (not (.contains name "-git"))
     {"expires" (distant-expires)})))

(defn- resource-handler* [{:keys [^String uri] :as request}]
  (let [relative (.substring uri 1)]
    (log/debug "looking for resource" relative)
    (if-let [resource (io/resource relative)]
      {:status 200
       :headers (headers-for-resource relative)
       :body (io/input-stream resource)}
      {:status 404
       :body "resource not found"})))

(def resource-handler
  (->> resource-handler*
       (if-uri-starts-with "/public/")))

(defn make []
  resource-handler)
