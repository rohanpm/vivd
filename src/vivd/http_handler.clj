(ns vivd.http_handler
  (:require [vivd.proxy :as proxy]
            [ring.util.response :refer :all]
            [clojure.tools.logging :as log]
            [clojure.string :as str]))

(defn- ensuring-method [method handler]
  (fn [request]
    (if (= (:request-method request) method)
      (handler request)
      {:status 405
       :body "Method not allowed"})))

(defn- index-handler* [request]
  {:status 200
   :body "not yet implemented"})

(defn- create-handler* [request]
  {:status 500
   :body "not yet implemented"})

(def ^{:private true} index-handler
  (->>
   index-handler*
   (ensuring-method :get)))

(def ^{:private true} create-handler
  (->>
   create-handler*
   (ensuring-method :post)))

(defn- augmented-proxy-request [request]
  (let [uri         (:uri request)
        uri-parts   (str/split uri #"/" 3)
        [_ id rest] uri-parts]
    (merge request {:container-vivd-id id
                    :container-uri rest})))

(defn- proxy-handler [request]
  (-> request
      (augmented-proxy-request)
      (proxy/proxy-to-container)))

(defn handler [request]
  "Top-level handler for all HTTP requests"
  (let [uri (:uri request)]
    (log/debug uri)
    (cond
     (= uri "/")       (index-handler request)
     (= uri "/create") (create-handler request)
     :else             (proxy-handler request))))
