(ns vivd.index-test
  (:require [clojure.core.async :refer [sliding-buffer chan]]
            [vivd.index :as index]))

(defn- merge-by-id [map {:keys [id] :as val}]
  (merge map {id val}))

(defn index-for [vals]
  (let [index-map (reduce merge-by-id {} vals)]
    {:index-ref        (atom index-map)
     :file-writer-chan (chan (sliding-buffer 1))}))

(defn index-merge [& indexes]
  (index-for (mapcat #(index/vals %) indexes)))
