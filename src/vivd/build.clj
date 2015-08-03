(ns vivd.build
  (:require [clojure.core.async :refer [chan >!! >! <! go-loop close!]]
            [clojure.string :refer [trim]]
            [clojure.tools.logging :as log]
            [vivd.utils :refer :all]
            [clojure.java.io :as io])
  (:import org.apache.commons.io.FileUtils))

(defn- builddir []
  "data/build")

(defn request-build [builder c]
  (let [ch (chan 1)]
    (>!! builder [ch c])
    ch))

(defn- setup-src [dir c]
  (let [code-dir (io/file dir "code")
        cmd      (str "cd " code-dir " && git --git-dir=../../git --work-tree=. checkout " (:git-revision c) " -- .")]
    (FileUtils/forceMkdir code-dir)
    (sh! "/bin/sh" "-c" cmd)))

(defn- setup-dockerfile [config dirf c]
  (let [dest (io/file dirf "Dockerfile")]
    (with-open [o (io/writer dest)]
      (.write o (str "FROM " (:docker-source-image config) "\n"))
      (.write o (str "ADD code/ " (:docker-code-path config) "\n")))))

(defn- docker-build [dirf]
  ; this is pretty dumb, but "docker build" doesn't seem to have any
  ; way to print the generated image ID in a machine-readable format
  (let [tag    "__vivd_tmp_build"
        _      (sh! "docker" "build" "-t" tag "-q" (str dirf))
        result (sh! "docker" "images" "-q" tag)
        built  (-> result
                   (:out)
                   (trim))]
    (log/debug "docker build - " result)
    built))

(defn- do-build [config out c]
  (try
    (log/debug "Will build:" c)
    (let [dirf (-> (builddir) (io/file))
          _    (ensure-directory-exists dirf)]
      (FileUtils/forceDelete dirf)
      (FileUtils/forceMkdir dirf)
      (setup-src dirf c)
      (setup-dockerfile config dirf c)
      (->> (docker-build dirf)
           (>!! out)))
    (finally (close! out))))

(defn builder [config]
  (let [ch (chan 10)]
    (go-loop []
      (try
        (apply do-build config (<! ch))
        (catch Exception e
          (log/error "Build failed:" e)))
      (recur))
    ch))
