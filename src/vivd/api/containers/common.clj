(ns vivd.api.containers.common
  (:require [vivd.utils :refer :all]))

(defn can-clean? [{:keys [docker-image-id]}]
  ; anything with a built image can be cleaned
  docker-image-id)

(defn- links-for-container [{:keys [config]} {:keys [id docker-container-id] :as container}]
  (let [default-url           (get-config config container :default-url)
        self                  (str "/a/containers/" id)]
    {:self self
     :logs (if docker-container-id
             ; cannot fetch logs until container is created
             (str self "/logs"))
     :clean (if (can-clean? container)
              (str self "/clean"))
     :app  (str "/" id default-url)}))

(def iso8601-formatter
  (clj-time.format/formatters :date-time))

(defn- iso8601 [timestamp]
  (clj-time.format/unparse iso8601-formatter timestamp))

(defn- container-resource-attributes [{:keys [timestamp] :as container}]
  (merge
   (select-keys container [:status :git-ref :git-revision :git-oneline :git-log])
   {:timestamp (iso8601 timestamp)}))

(defn container-resource [services {:keys [id] :as container}]
  {:id         id
   :type       "containers"
   :attributes (container-resource-attributes container)
   :links      (links-for-container services container)})
