(ns vivd.json-api.utils
  (:require [schema.core :as s]))

(defn- throw-error [{:keys [title] :as error}]
  (throw (ex-info title {:json-api-error error})))

(defn- ensuring-single [data]
  (if (map? data)
    data
    (throw-error {:title  "incorrectly supplied array of resources"
                  :detail (str "An array was supplied under the `data' "
                               "key. This endpoint expects a single resource.")
                  :status "400"})))

(defn- ensuring-schema [schema data]
  (if-let [bad-parts (s/check schema data)]
    (throw-error {:title  "invalid resource provided"
                  :detail "An invalid resource was provided. Please check the request."
                  :status "400"
                  :meta   {:validation-errors (prn-str bad-parts)}}))
  data)

(defn extract-resource [{:keys [data]} schema]
  (let [resource (ensuring-single data)
        resource (ensuring-schema schema data)]
    resource))
