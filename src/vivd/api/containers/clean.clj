(ns vivd.api.containers.clean
  (:require [vivd.index :as index]
            [vivd.container :as container]
            [vivd.api.containers.common :refer :all]
            [clojure.core.async :refer [thread]]
            [clojure.tools.logging :as log]))

(defn do-clean-container [{:keys [index config]} {:keys [id] :as c}]
  (let [updated-c (-> c
                      (dissoc :docker-image-id :docker-container-id)
                      (merge {:status :cleaning}))
        _         (index/update index updated-c)
        ; note: not transactional...
        ; could result in orphaned running containers if killed right here.
        ; Also, the removal is done async and with one retry because it's slow
        ; and prone to error with some storage backends.
        _         (thread
                    (try
                      (container/remove c)
                      (catch Exception e
                        (log/warn "Remove" id "failed, will try again" e)
                        (container/remove c))))
        out-c     (merge updated-c {:status :new})]
    (index/update index out-c)
    out-c))

(defn clean-container [{:keys [index] :as services} request id]
  (if-let [c (index/get index id)]
    (if (can-clean? c)
      (let [cleaned (do-clean-container services c)]
        {:status 200
         :body {:data (container-resource services cleaned)}})
      {:status 409})
    {:status 404}))
