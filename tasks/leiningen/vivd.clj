(ns leiningen.vivd
  (:require leiningen.uberjar
            leiningen.bower
            leiningen.run
            [clojure.java.io :as io]
            robert.hooke)
  (:import org.apache.commons.io.FileUtils))

(defn- resources-dir []
  (io/file "resources/public/vendor"))

(defn- copy-dist [project component]
  (let [name     (.getName component)
        dist-dir (io/file component "dist")
        dest-dir (io/file (resources-dir) name)]
    (if (.exists dist-dir)
      (do
        (println "Copy:" (.getPath dist-dir) "=>" (.getPath dest-dir))
        (FileUtils/copyDirectory dist-dir dest-dir)))))

(defn- copy-bower-dists [project]
  (let [srcdir     (get-in project [:bower :directory])
        rootdir    (get-in project [:root])
        _          (assert srcdir)
        srcdir     (io/file rootdir srcdir)
        _          (assert (.exists srcdir))
        components (.listFiles srcdir)
        _          (assert (> (alength components) 0))
        copier     (partial copy-dist project)]
    (doall (map copier components))))

(defn- copy-one-resource [file]
  (let [name (.getName file)
        dest (io/file (resources-dir) name)]
    (println "Copy:" (.getPath file) "=>" (.getPath dest))
    (FileUtils/copyFile file dest)))

(defn- copy-bower-files [{:keys [bower] :as project}]
  (let [bower-root (:directory bower)
        files      (:flat-files bower)
        files      (map #(io/file bower-root %) files)]
    (doall (map copy-one-resource files))))

(defn- prepare-bower-deps [project]
  (let  [result (leiningen.bower/bower project "install")]
    (assert (= result 0) "bower install failed"))
  (copy-bower-dists project)
  (copy-bower-files project))

(def in-hook nil)

(defn- with-bower-deps [f project & args]
  (if in-hook
    (apply f project args)
    (with-redefs [in-hook true]
      (prepare-bower-deps project)
      (apply f project args))))

(defn add-hooks []
  (robert.hooke/add-hook #'leiningen.uberjar/uberjar #'with-bower-deps)
  (robert.hooke/add-hook #'leiningen.run/run #'with-bower-deps))
