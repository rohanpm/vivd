(ns vivd.json-api
  (:require [clojure.tools.logging :as log]
            [clojure.data.json :as json]
            [clojure.string :as str]))

(def json-api-content-type "application/vnd.api+json")

(defn- request-content-type-ok? [{:keys [headers body] :or {headers {}}}]
  (let [content-type (headers "content-type")]
    (or (and (not content-type) (not body))
        (= content-type json-api-content-type))))

(defn- request-accept-ok? [{:keys [headers] :or {headers {}}}]
  (let [accept (headers "accept")]
    (if (not accept)
      true
      (let [types (str/split accept #"\s*,\s*")]
        (some #(= % json-api-content-type) types)))))

(defn- wrap-request-content-type [handler]
  (fn [request]
    (if (request-content-type-ok? request)
      (handler request)
      {:status 415})))

(defn- wrap-request-accept [handler]
  (fn [request]
    (if (request-accept-ok? request)
      (handler request)
      {:status 406})))

(defn- wrap-response-content-type [handler]
  (fn [request]
    (let [{:keys [headers] :or {headers {}} :as response}
                                         (handler request)
          content-type                   (headers "content-type")
          bad-content-type               (and content-type
                                              (not (= content-type json-api-content-type)))]
      (if bad-content-type
        {:status 500}
        (merge response {:headers (merge headers {"content-type" json-api-content-type})})))))

(defn wrap-json-api
  ([handler]
     (wrap-json-api handler {}))
  
  ([handler options]
     (-> handler
         (wrap-request-content-type)
         (wrap-request-accept)
         (wrap-response-content-type))))
