(ns vivd.reaper-test
  (:require [midje.sweet :refer :all]
            vivd.reaper
            vivd.container
            vivd.api.containers.clean
            [vivd.index-test :refer :all]
            [vivd.index :as index]
            [clojure.tools.logging :as log]
            [clj-time.core :refer [minutes ago]]))


(def do-reap
  #'vivd.reaper/do-reap)

(defn index-with-too-many-stopped []
  (index-for #{{:id                  "aaaaa1"
                :status              :stopped
                :docker-container-id "1",
                :docker-image-id     "11",
                :timestamp           (-> 5 minutes ago)}
               {:id                  "aaaaa2"
                :status              :stopped
                :docker-container-id "2",
                :docker-image-id     "22",
                :timestamp           (-> 10 minutes ago)}
               {:id                  "aaaaa3"
                :status              :stopped
                :docker-container-id "3",
                :docker-image-id     "33",
                :timestamp           (-> 15 minutes ago)}
               {:id                  "aaaaa4"
                :status              :stopped
                :docker-container-id "4",
                :docker-image-id     "44",
                :timestamp           (-> 20 minutes ago)}
               {:id                  "aaaaa5"
                :docker-container-id "aabbccdd"
                :docker-image-id     "55",
                :status              :stopped
                :timestamp           (-> 25 minutes ago)}
               {:id                  "aaaaa6"
                :status              :new
                :timestamp           (-> 30 minutes ago)}}))

(defn index-with-too-many-stopped-and-up []
  (index-merge
   (index-with-too-many-stopped)
   (index-for #{{:docker-container-id "1b"
                 :docker-image-id     "11bb",
                 :id                  "b1"
                 :status              :up
                 :timestamp           (-> 40 minutes ago)}
                {:docker-container-id "2b"
                 :docker-image-id     "22bb",
                 :id                  "b2"
                 :status              :up
                 :timestamp           (-> 50 minutes ago)}
                {:docker-container-id "3b"
                 :docker-image-id     "33bb",
                 :id                  "b3"
                 :status              :up
                 :timestamp           (-> 60 minutes ago)}
                {:docker-container-id "4b"
                 :docker-image-id     "44bb",
                 :id                  "b4"
                 :status              :up
                 :timestamp           (-> 70 minutes ago)}})))

(defn index-with-starting-and-up []
  (index-for #{{:docker-container-id "1c"
                :docker-image-id     "11cc",
                :id                  "c1"
                :status              :up
                :timestamp           (-> 40 minutes ago)}
               {:docker-container-id "2c"
                :docker-image-id     "22cc",
                :id                  "c2"
                :status              :starting
                :timestamp           (-> 50 minutes ago)}
               {:docker-container-id "3c"
                :docker-image-id     "33cc",
                :id                  "c3"
                :status              :starting
                :timestamp           (-> 60 minutes ago)}
               {:docker-container-id "4c"
                :docker-image-id     "44cc",
                :id                  "c4"
                :status              :up
                :timestamp           (-> 70 minutes ago)}}))

(defn test-config []
  {:max-containers       4
   :max-containers-built 3
   :max-containers-up    2})

(defn docker-inspect-running [& ids]
  (fn [id]
    (if (some #(= id %) ids)
      {:State {:Running true}})))

(defn reaped-containers [{:keys [config index running-containers]
                          :or {config (test-config) running-containers []}}]
  (let [stopped (atom #{})
        removed (atom #{})
        cleaned (atom #{})]
    (with-redefs [vivd.container/remove
                  (fn [{:keys [id] :as c}]
                    (swap! removed conj id))

                  vivd.container/stop
                  (fn [{:keys [id] :as c}]
                    (swap! stopped conj id))

                  vivd.api.containers.clean/do-clean-container
                  (fn [_ {:keys [id] :as c}]
                    (swap! cleaned conj id))

                  vivd.container/docker-inspect
                  (apply docker-inspect-running running-containers)

                  index/remove-file
                  (fn [_])]
      (do-reap config index))
    {:stopped @stopped
     :removed @removed
     :cleaned @cleaned
     :index-vals (index/vals index)}))

(defn expected-reap [index {:keys [container-stop
                                   container-remove
                                   container-clean
                                   index-remove
                                   index-set-status]
                            :or   {container-stop   #{}
                                   container-remove #{}
                                   container-clean  #{}
                                   index-remove     #{}
                                   index-set-status {}}}]
  (let [index-vals    (index/vals index)
        should-remove (fn [{:keys [id]}]
                        (some #(= % id) index-remove))
        updated-vals  (filter #(not (should-remove %)) index-vals)
        update-status (fn [{:keys [id status] :as c}]
                        (if-let [new-status (index-set-status id)]
                          (merge c {:status new-status})
                          c))
        updated-vals  (map update-status updated-vals)]
    {:stopped    container-stop
     :removed    container-remove
     :cleaned    container-clean
     :index-vals (index/vals (index-for updated-vals))}))

(facts "do-reap"
  (fact "does nothing on empty index"
    (let [index (index-for #{})]
      (reaped-containers {:index index})
      => (expected-reap index {})))

  (let [index (index-with-too-many-stopped)]
    (fact "removes stopped containers OK"
      (reaped-containers {:index index})
      => (expected-reap index {:container-remove #{"aaaaa5"}
                               :container-clean  #{"aaaaa4" "aaaaa5"}
                               :index-remove #{"aaaaa5" "aaaaa6"}})))


  (let [index (index-with-too-many-stopped-and-up)]
    (fact "stops and removes containers OK"
      (reaped-containers {:index index
                          :running-containers ["1b" "2b" "3b" "4b"]})
      => (expected-reap index {:container-stop   #{"b3" "b4"}
                               :container-remove #{"aaaaa5"}
                               :container-clean  #{"aaaaa4" "aaaaa5"}
                               :index-remove     #{"aaaaa5" "aaaaa6"}
                               :index-set-status {"b3" :stopped
                                                  "b4" :stopped}})))

  (let [index (index-with-starting-and-up)]
    (fact "does not reap starting containers"
      (reaped-containers {:index index
                          :running-containers ["1c" "2c" "3c" "4c"]})
      => (expected-reap index {:container-stop   #{"c4" "c1"}
                               :index-set-status {"c4" :stopped
                                                  "c1" :stopped}}))))
