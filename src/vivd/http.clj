(ns vivd.http)

(set! *warn-on-reflection* true)

(defn if-uri-is [desired-uri handler]
  (fn [{:keys [uri] :as request}]
    (if (= uri desired-uri)
      (handler request))))

(defn if-uri-starts-with [desired-uri handler]
  (fn [{:keys [^String uri] :as request}]
    (if (.startsWith uri desired-uri)
      (handler request))))

(defn ensuring-method [method handler]
  (fn [request]
    (let [request-method (:request-method request)]
      (if (= request-method method)
        (handler request)
        {:status 405
         :body (str "Method " request-method " not allowed")}))))
