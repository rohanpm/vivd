(ns vivd.http_handler
  (:require [vivd
             [proxy :as proxy]
             [logging :as log]
             [types :refer :all]
             [index :as index]
             [index-page :as index-page]
             [build :as build]]
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

(defn make-index-handler [index-page-ref]
  (->> (fn [request]
         {:status 200
          :headers {"content-type" "text/html"}
          :body @index-page-ref})
       (ensuring-method :get)))

(ann create-handler* RingHandler)
(defn- create-handler* [request]
  {:status 500
   :body "not yet implemented"})

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

(defn- make-proxy-handler [& args]
  (log/info "make-proxy-handler args" args)
  (fn [request]
    (let [request (augmented-proxy-request request)]
      (apply proxy/proxy-to-container request args))))

(defn make-handler [config]
  "Returns a top-level handler for all HTTP requests"
  (let [index          (index/make)
        index-page-ref (index-page/make index)
        index-handler  (make-index-handler index-page-ref)
        builder        (build/builder config)
        proxy-handler  (make-proxy-handler config builder index)]
    (fn [request]
      (let [^String uri (:uri request)]
        (log/debug uri)
        (cond
         (= uri "/")       (index-handler request)
         (= uri "/create") (create-handler request)
         :else             (proxy-handler request))))))
