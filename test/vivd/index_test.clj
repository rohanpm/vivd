(ns vivd.index-test
  (:require [clojure.core.async :refer [sliding-buffer chan]]
            [midje.sweet :refer :all]
            [vivd.index :as index]))

(defn- merge-by-id [map {:keys [id] :as val}]
  (merge map {id val}))

(defn index-for [vals]
  (let [index-map (reduce merge-by-id {} vals)]
    {:index-ref        (atom index-map)
     :file-writer-chan (chan (sliding-buffer 1))
     :on-update        (atom [])}))

(defn index-merge [& indexes]
  (index-for (mapcat #(index/vals %) indexes)))

(facts "index"
  (fact "update callbacks are invoked"
    (let [index   (index-for [])
          updated (atom [])
          updater (fn [c]
                    (swap! updated conj c))
          ; using multiple instances of same callback
          _       (index/watch-update index updater)
          _       (index/watch-update index updater)
          _       (index/update index {:id "foo"})
          _       (index/update index {:id "quux"})]
      @updated)
    => [{:id "foo"}  {:id "foo"}
        {:id "quux"} {:id "quux"}])

  (fact "errors from callbacks are caught"
    (let [index   (index-for [{:id "hi"}])
          updated (atom [])
          updater (fn [c]
                    (swap! updated conj c))
          crasher (fn [_]
                    (throw (ex-info "simulated error..." {})))
          _       (index/watch-update index crasher)
          _       (index/watch-update index updater)
          _       (index/update index {:id "hi" :foo "bar"})]
      @updated)
    => [{:id "hi" :foo "bar"}]))
