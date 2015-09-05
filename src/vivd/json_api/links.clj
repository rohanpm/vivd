(ns vivd.json-api.links
  (:require [clojure.tools.logging :as log]))

(defn link-prefix [{:keys [headers scheme server-port server-name]}]
  (let [host (or (and headers (headers "host"))
                 (str server-name ":" server-port))]
    (str (name scheme) "://" host)))

(defn prefix-link-val [prefix val]
  (log/debug "prefix-link-val" prefix val)
  (cond
   (nil? val)    nil
   (string? val) (str prefix val)
   :else         (merge val {:href 
                             (prefix-link-val prefix (:href val))})))

(defn prefix-link-object [prefix object]
  (log/debug "prefix" prefix object)
  (let [prefixed-kvs (mapcat #(vector
                               (first %)
                               (prefix-link-val prefix (second %))) object)]
    (apply hash-map prefixed-kvs)))
