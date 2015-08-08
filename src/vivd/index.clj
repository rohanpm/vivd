(ns vivd.index
  (:refer-clojure :exclude [get update remove keys vals])
  (:require [clojure.java.io :as io]
            [clojure.core.async :refer [chan >!! >! <! go-loop close!]]
            [clojure.edn :as edn]
            [clojure.pprint :as pprint]
            [clj-time.coerce :as time-coerce]
            [clojure.tools.logging :as log]
            [vivd
             [utils :refer :all]])
  (:import org.apache.commons.io.FileUtils))

(set! *warn-on-reflection* true)

(defn- container-dir []
  "data/containers")

(defn- container-dir-f ^java.io.File []
  (let [out (-> (container-dir)
                (io/as-file))]
    (ensure-directory-exists out)
    out))

(defn- load-info [id]
  (log/debug "Reading info for:" id)
  (let [file (io/file (container-dir) id)]
    (with-open [stream (reader-for-file file)]
      (merge
       (edn/read stream)
       {:id        id
        :timestamp (-> file
                       (.lastModified)
                       (time-coerce/from-long))}))))

(defn- try-load-info [id]
  (try
    (load-info id)
    (catch Exception e
      (log/warn "Ignoring unloadable container" id e)
      nil)))

(defn- read-index []
  (let [container-files (.listFiles (container-dir-f))
        container-ids   (map #(.getName ^java.io.File %1) container-files)
        _               (log/debug "containers" container-ids)
        container-data  (keep try-load-info container-ids)
        container-map   (reduce #(merge %1 {(:id %2) %2}) {} container-data)
        _               (log/debug "loaded containers:" container-map)]
    container-map))

(defn- prepare-for-file [{:keys [timestamp] :as c}]
  (merge
   c
   {:timestamp (time-coerce/to-string timestamp)}))

(defn- remove-file [id]
  (let [file ^java.io.File (io/file (container-dir) id)]
    (if (.exists file)
      (.delete file))))

(defn- file-writer-loop [index-ref channel]
  (go-loop []
    (let [id   (<! channel)
          c    (@index-ref id)
          c    (prepare-for-file c)
          file ^java.io.File (io/file (container-dir) id)
          tmp-file ^java.io.File (io/file (str (.getPath file) ".new"))]
      (log/debug "Updating" id "with" c)
      (with-open [o ^java.io.Writer (io/writer tmp-file)]
        (pprint/pprint c o))
      (FileUtils/copyFile tmp-file file)
      (FileUtils/forceDelete tmp-file)
      (recur))))

(defn get [{:keys [index-ref]} id]
  "Look up a container in the index, by id."
  ((deref index-ref) id))

(defn keys [{:keys [index-ref]}]
  "Return all container ids."
  (clojure.core/keys @index-ref))

(defn vals [{:keys [index-ref]}]
  "Return all containers."
  (clojure.core/vals @index-ref))

(defn remove [{:keys [index-ref]} id]
  "Remove a container from the index."
  (log/debug "Removing" id)
  (swap! index-ref (fn [val]
                     (remove-file id)
                     (dissoc val id))))

(defn update [{:keys [index-ref file-writer-chan]} {:keys [id] :as c}]
  "Update the data for a single container in the index. This will synchronously
   update the in-memory index and asynchronously update the file backing store."
  (let [current-value (@index-ref id)]
    (if (= current-value c)
      (log/debug "No-op update for" id)
      (do
        (swap! index-ref (fn [val] (merge val {id c})))
        (>!! file-writer-chan id)))))

(defn make []
  "Creates and returns a new index object, with the given config. The index is
  the object where all information regarding known containers is maintained. It
  is backed by files under the 'data/container' directory, which shouldn't be
  modified other than by the calls in this namespace."
  (let [ref              (atom {})
        file-writer-chan (chan 10)
        _                (file-writer-loop ref file-writer-chan)]
    (swap! ref (fn [_] (read-index)))
    {:index-ref ref
     :file-writer-chan file-writer-chan}))
