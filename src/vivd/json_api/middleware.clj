(ns vivd.json-api.middleware
  (:require [clojure.tools.logging :as log]
            [clojure.data.json :as json]
            [clojure.string :as str]
            [clojure.java.io :as io]
            [schema.core :as s]
            [ring.middleware.cors :refer [wrap-cors]]
            [vivd.json-api.links :refer [prefix-link-object prefix-link-val link-prefix]]
            vivd.json-api.schema))

(def json-api-content-type "application/vnd.api+json")

(defn- request-content-type-ok? [{:keys [headers body] :or {headers {}}}]
  (let [content-type (headers "content-type")]
    (or (not content-type)
        (= content-type json-api-content-type))))

(defn- request-accept-ok? [{:keys [headers] :or {headers {}}}]
  (let [accept (headers "accept")]
    (if (or (not accept) (= accept "*/*"))
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

(defn- encode-body [data]
  (json/write-str data))

(defn- wrap-modify-body [handler modify-body]
  (fn [request]
    (let [{:keys [body] :as response} (handler request)]
      (if body
        (merge response {:body (modify-body body)})
        response))))

(defn- wrap-encode-response-body [handler]
  (wrap-modify-body handler encode-body))

(defn- internal-invalid-response [bad-parts]
  {:status 500,
   :body   {:errors [{:title  "invalid response generated",
                      :status "500",
                      :detail (str
                               "The server internally generated an invalid "
                               "JSON API response. This may be a bug in vivd."),
                      :meta   {:validation-errors (prn-str bad-parts)}}]}})

(defn- wrap-validate-response-body [handler]
  (fn [request]
    (let [{:keys [body] :as response} (handler request)
          bad-parts                   (s/check vivd.json-api.schema/MaybeDocument body)]
      (if bad-parts
        (do
          (log/error "Internally generated response fails validation:" body)
          (internal-invalid-response bad-parts))
        response))))

(defn- json-read-stream [stream]
  (with-open [rdr (io/reader stream)]
    (json/read rdr :key-fn keyword :eof-error? false)))

(defn- try-read-json [{:keys [body] :as request}]
  (try
    {:body (json-read-stream body)}
    (catch Exception e
      {:error e})))

(defn- invalid-request-error [validation-errors]
  {:title  "invalid request",
   :status "400",
   :detail "The provided request body is not a valid JSON API document.",
   :meta   {:validation-errors validation-errors}})

(defn- invalid-request-response [validation-errors]
  {:status 400,
   :body   {:errors [(invalid-request-error validation-errors)]}})

(defn- wrap-decode-request-body [handler]
  (fn [{:keys [body] :as request}]
    (if body
      (let [{:keys [body error]} (try-read-json request)]
        (if error
          (invalid-request-response (.getMessage error))
          (handler (merge request {:body body}))))
      (handler request))))

(defn- wrap-validate-request-body [handler]
  (fn [{:keys [body] :as request}]
    (let [bad-parts (s/check vivd.json-api.schema/MaybeDocument body)]
      (if bad-parts
        (invalid-request-response (prn-str bad-parts))
        (handler request)))))

(defn- empty-to-nil [val]
  (if (= val "")
    nil
    val))

(defn- wrap-default-response-body [handler]
  (fn [request]
    (let [response (handler request)]
      (update response :body empty-to-nil))))

(defn- wrap-response-body [handler]
  (-> handler
      (wrap-default-response-body)
      (wrap-validate-response-body)
      (wrap-encode-response-body)))

(defn- wrap-request-body [handler]
  (-> handler
      (wrap-validate-request-body)
      (wrap-decode-request-body)))

(defn- ex-api-error [ex]
  (if-let [data (ex-data ex)]
    (:json-api-error data)))

(defn- try-str-to-int [s]
  (try
    (Integer/valueOf s)
    (catch Exception _
      nil)))

(defn- response-for-exception [ex]
  (let [error-object (or
                      (ex-api-error ex)
                      {:status "500"
                       :title  "internal error"
                       :detail "An internal server error has occurred."})
        status       (:status error-object)
        status       (or (try-str-to-int status) 500)]
    (if (= status 500)
      (log/error "An unexpected exception occurred:" ex))
    {:status status
     :body   {:errors [error-object]}}))

(defn- wrap-exceptions [handler]
  (fn [request]
    (try
      (handler request)
      (catch Exception e
        (log/debug "Exception" e)
        (response-for-exception e)))))


(defn- update-in-if-exists [object ks f]
  (if (get-in object ks)
    (update-in object ks f)
    object))

(defn- update-in-each [object ks f]
  (let [val (get-in object ks)]
    (if (sequential? val)
      (update-in object ks #(map f %))
      object)))

(defn- prefix-links [prefix response]
  (let [prefixer (partial prefix-link-object prefix)]
    (-> response
        (update-in-if-exists [:headers "location"] (partial prefix-link-val prefix))
        (update-in-each [:body :data] (fn [resource]
                                        (update-in-if-exists resource [:links] prefixer)))
        (update-in-if-exists [:body :data :links] prefixer)
        (update-in-if-exists [:body :links] prefixer))))

(defn- wrap-absolute-links [handler]
  (fn [request]
    (let [response (handler request)
          prefix   (link-prefix request)]
      (prefix-links prefix response))))

(defn wrap-json-api
  ([handler]
     (wrap-json-api handler {}))
  
  ([handler options]
     (-> handler
         (wrap-request-body)
         (wrap-request-content-type)
         (wrap-request-accept)
         (wrap-exceptions)
         (wrap-absolute-links)
         (wrap-cors :access-control-allow-origin #".*"
                    :access-control-allow-methods [:get :put :post :patch :delete])
         (wrap-response-body)
         (wrap-response-content-type))))
