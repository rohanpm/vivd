(ns vivd.utils
  (:require [clojure.java.io :as io]
            [clojure.java.shell :refer [sh]]
            [clojure.tools.logging :as log]
            [clojure.edn :as edn])
  (:import org.apache.commons.io.FileUtils))

(defn get-config [global-config container-config key]
  (or (get-in container-config [:config key])
      (global-config key)))

(defn reader-for-file ^java.io.PushbackReader [& args]
  (->  (apply io/file args)
       (io/reader)
       (java.io.PushbackReader.)))

(defn read-edn [& args]
  (with-open [cfg (apply reader-for-file args)]
    (edn/read cfg)))

(defn sh! [cmd & args]
  (log/debug "sh:" cmd args)
  (let [result (apply sh cmd args)
        exit   (:exit result)]
    (if (not= 0 exit)
      (throw (ex-info "command failed" {:result result}))
      result)))

(def FILE-LOCK (Object.))

(defn ensure-directory-exists [dir]
  (let [dir-f (io/as-file dir)]
    (if (not (.exists dir-f))
      (locking FILE-LOCK
        (if (not (.exists dir-f))
          (FileUtils/forceMkdir dir-f))))))
