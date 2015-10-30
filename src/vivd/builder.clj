(ns vivd.builder
  (:require [clojure.core.async :refer [chan >!! >! <! go-loop close!]]
            [clojure.string :refer [trim]]
            [clojure.tools.logging :as log]
            [vivd.utils :refer :all]
            [vivd
             [index :as index]]
            [clojure.java.io :as io]
            [clojure.data.json :as json])
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

(defn- write-json-directive [stream directive value]
  (if value
    (.write stream (str directive " " (json/write-str value) "\n"))))

(defn- setup-dockerfile [config dirf c]
  (let [dest (io/file dirf "Dockerfile")
        img  (get-config config c :docker-source-image)
        code (get-config config c :docker-code-path)
        cmd  (get-config config c :docker-cmd)
        ep   (get-config config c :docker-entrypoint)]
    (with-open [o (io/writer dest)]
      (.write o (str "FROM " img "\n"))
      (.write o (str "ADD code/ " code "\n"))
      (write-json-directive o "CMD" cmd)
      (write-json-directive o "ENTRYPOINT" ep))))

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

(defn- keep-if-exists [file]
  (if (.exists file)
    file))

(defn- config-from-src [{:keys [src-config]} dirf]
  (if src-config
    (if-let [file (-> (io/file dirf "code" src-config)
                      (keep-if-exists))]
      (try
        (-> file
            (read-edn)
            (select-keys [:startup-timeout
                          :default-url
                          :docker-source-image
                          :docker-http-port
                          :docker-code-path
                          :docker-entrypoint
                          :docker-cmd
                          :docker-run-arguments]))
        (catch Exception e
          (log/warn "Could not read" file ", ignoring." e))))))

(defn- do-build* [config index out {:keys [id git-revision] :as c}]
  (try
    (log/debug "Will build:" c)
    (index/update index (merge c {:status :building}))
    (let [dirf (-> (builddir) (io/file))
          _    (ensure-directory-exists dirf)]
      (FileUtils/forceDelete dirf)
      (FileUtils/forceMkdir dirf)
      (setup-src dirf c)
      (let [c-config (config-from-src config dirf)
            new-c    (if c-config
                       (merge c {:config c-config})
                       c)]
        (log/debug "container after config:" new-c)
        (setup-dockerfile config dirf new-c)
        (let [new-image (docker-build dirf)            
              new-c     (merge new-c
                               {:status          :built
                                :docker-image-id new-image}
                               (config-from-src config dirf))]
          (log/info "Built image" new-image "for" git-revision)
          (index/update index new-c)
          (>!! out new-c))))
    (finally (close! out))))

(defn do-build [config index out {:keys [id git-revision] :as c}]
  (let [updated-c (index/get index id)]
    (if (:docker-image-id updated-c)
      (do
        (log/debug "returning already built:" updated-c)
        (>!! out updated-c))
      (do-build* config index out c))))

(defn make [config index]
  (let [ch (chan 10)]
    (go-loop []
      (try
        (apply do-build config index (<! ch))
        (catch Exception e
          (log/error "Build failed:" e)))
      (recur))
    ch))
