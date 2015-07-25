(ns vivd.http_handler
  (:require [vivd
             [proxy :as proxy]
             [logging :as log]
             [types :refer :all]]
            [ring.util.response :refer :all]
            [clojure.string :as str]
            [clojure.core.typed :refer [typed-deps ann]]))

(typed-deps vivd.types)
(set! *warn-on-reflection* true)

(ann ensuring-method [HttpMethod RingHandler -> RingHandler])
(defn- ensuring-method [method handler]
  (fn [request]
    (if (= (:request-method request) method)
      (handler request)
      {:status 405
       :body "Method not allowed"})))

(ann index-handler* RingHandler)
(defn- index-handler* [request]
  {:status 200
   :body "not yet implemented"})

(ann create-handler* RingHandler)
(defn- create-handler* [request]
  {:status 500
   :body "not yet implemented"})

(ann index-handler RingHandler)
(def ^{:private true} index-handler
  (->>
   index-handler*
   (ensuring-method :get)))

(ann create-handler RingHandler)
(def ^{:private true} create-handler
  (->>
   create-handler*
   (ensuring-method :post)))

(ann augmented-proxy-request [RingRequest -> ContainerRequest])
(defn- augmented-proxy-request [request]
  (let [uri         (:uri request)
        uri-parts   (str/split uri #"/" 3)
        [_ id rest] uri-parts]
    (assert (not (nil? id)))
    (merge request {:container-vivd-id id
                    :container-uri rest})))

(ann proxy-handler RingHandler)
(defn- proxy-handler [request]
  (-> request
      (augmented-proxy-request)
      (proxy/proxy-to-container)))

(ann handler RingHandler)
(defn handler [request]
  "Top-level handler for all HTTP requests"
  (let [^String uri (:uri request)]
    (log/debug uri)
    (cond
     (= uri "/")       (index-handler request)
     (= uri "/create") (create-handler request)
     :else             (proxy-handler request))))
