(ns leiningen.vivd
  (:require leiningen.uberjar
            leiningen.bower
            leiningen.run
            [clojure.java.io :as io]
            robert.hooke)
  (:import org.apache.commons.io.FileUtils))

(defn- copy-dist [project component]
  (let [name     (.getName component)
        dist-dir (io/file component "dist")
        dest-dir (io/file "resources/public" name)]
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

(defn- prepare-bower-deps [project]
  (let  [result (leiningen.bower/bower project "install")]
    (assert (= result 0) "bower install failed"))
  (copy-bower-dists project))

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
