(ns vivd.http_handler
  (:require [vivd
             [proxy :as proxy]
             [index :as index]
             [index-page :as index-page]
             [build :as build]
             [reap :as reap]]
            [ring.util.response :refer :all]
            [clojure.tools.logging :as log]
            [clojure.java.io :as io]
            [clojure.data.json :as json]
            [clojure.string :as str]
            clj-time.core))

(set! *warn-on-reflection* true)

(defn- ensuring-method [method handler]
  (fn [request]
    (let [request-method (:request-method request)]
      (if (= request-method method)
        (handler request)
        {:status 405
         :body (str "Method " request-method " not allowed")}))))

(defn make-index-handler [index-page-ref]
  (->> (fn [request]
         {:status 200
          :headers {"content-type" "text/html"}
          :body @index-page-ref})
       (ensuring-method :get)))

(defn- read-json-stream [stream]
  (with-open [reader (io/reader stream)]
    (json/read reader :key-fn keyword)))

(def CHARS "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789_")
(defn- generate-id []
  (apply str (map (fn [_] (rand-nth CHARS)) (range 8))))

(defn- make-create-handler* [index]
  (fn [request]
    (let [body                           (:body request)
          data                           (read-json-stream body)
          {:keys [git-ref git-revision]} data
          _                              (assert git-ref "Missing git-ref in request")
          _                              (assert git-revision "Missing git-revision in request")
          id                             (generate-id)
          c                              {:id           id
                                          :git-ref      git-ref
                                          :git-revision git-revision
                                          :timestamp    (clj-time.core/now)}]
      (index/update index c)
      (log/info "Created:" data "id:" id)
      {:status 201
       :headers {"location" (str "/" id)}})))

(defn- make-create-handler [index]
  (->> (make-create-handler* index)
       (ensuring-method :post)))

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

(defn- make-redirect-handler [index]
  "Redirect requests which appear to accidentally escape the container"
  (fn [{:keys [^String uri headers] :as request}]
    (let [^String referer
                  (or (headers "referer") "")
          host    (headers "host")
          prefix  (str "http://" host "/")
          all-c   (index/keys index)
          matches (fn [id]
                    (let [needle ^String (str prefix id)
                          found  (.startsWith referer needle)]
                      (log/debug needle found)
                      found))          
          match-c (filter matches all-c)
          matched (first match-c)]
      (log/debug "referer " referer " from container " matched " looking at " uri)
      (if (and matched
               (not (.startsWith uri (str "/" matched))))
        (let [dest (str matched uri)]
          (log/debug "redirect to" dest)
          {:status 307
           :headers {"location" (str prefix dest)}})
        nil))))

(defn make-handler [config]
  "Returns a top-level handler for all HTTP requests"
  (let [index          (index/make)
        index-page-ref (index-page/make index)
        index-handler  (make-index-handler index-page-ref)
        _              (reap/run-async config index)
        builder        (build/builder config)
        create-handler (make-create-handler index)
        proxy-handler  (make-proxy-handler config builder index)
        redirect-handler (make-redirect-handler index)]
    (fn [request]
      (let [^String uri (:uri request)]
        (log/debug uri)
        (or (redirect-handler request)
            (cond
             (= uri "/")       (index-handler request)
             (= uri "/create") (create-handler request)
             :else             (proxy-handler request)))))))
