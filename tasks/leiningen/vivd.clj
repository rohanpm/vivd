(ns leiningen.vivd
  (:require leiningen.uberjar
            leiningen.bower
            leiningen.run
            leiningen.npm
            [clojure.java.io :as io]
            [clojure.java.shell :refer [sh]]
            robert.hooke)
  (:import org.apache.commons.io.FileUtils))

(defn- resources-dir []
  (io/file "resources/public/vendor"))

(defn- copy-dist [srcdir component]
  (let [dist-dir (io/file srcdir component "dist")
        dest-dir (io/file (resources-dir) component)]
    (println "Copy:" (.getPath dist-dir) "=>" (.getPath dest-dir))
    (FileUtils/copyDirectory dist-dir dest-dir)))

(defn- copy-bower-dists [project]
  (let [srcdir     (get-in project [:bower :directory])
        components (get-in project [:bower :copy-dist])
        copier     (partial copy-dist srcdir)]
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

(defn- install-bower-deps [project]
  (println "bower install...")
  (let  [result (leiningen.bower/bower project "install")]
    (assert (= result 0) "bower install failed")))

(defn- install-npm-deps [project]
  (println "npm install...")
  (leiningen.npm/npm project "install"))

(defn- copy-bower-deps [project]
  (println "bower copy...")
  (copy-bower-dists project)
  (copy-bower-files project))

(defn- run! [& cmd]
  (let [{:keys [exit] :as result} (apply sh cmd)]
    (if (not (= 0 exit))
      (throw (ex-info "external command failed" (merge result {:cmd cmd}))))))

(def browserify "node_modules/.bin/browserify")
(def watchify   "node_modules/.bin/watchify")
(def uglify     "node_modules/.bin/uglify")
(def app-bundle "resources/public/js/app-bundle.js")

(def browserify-args ["resources/public/js/app.jsx"
                      "-g" "babelify"
                      "--extension" ".jsx"
                      "--outfile" app-bundle])

(defn run-browserify []
  (println "browserify...")
  (apply run! browserify browserify-args))

(defn run-uglify []
  (println "uglify...")
  (run! uglify "-s" app-bundle "-o" app-bundle))

(defn with-watchify-proc [f]
  (println "watchify...")
  (let [proc (-> (ProcessBuilder.
                  (into-array (concat [watchify] browserify-args)))
                 (.start))]
    (try
      (f)
      (finally
        (.destroy proc)))))

(def in-hook #{})

(defn- run-before [key f orig-f & args]
  (if (in-hook key)
    (apply orig-f args)
    (with-redefs [in-hook (conj in-hook key)]
      (f)
      (apply orig-f args))))

(defn- with-browserify [f & args]
  (apply run-before :a run-browserify f args))

(defn- with-copy-bower-deps [f project & args]
  (apply run-before :b #(copy-bower-deps project) f project args))

(defn- with-install-bower-deps [f project & args]
  (apply run-before :c #(install-bower-deps project) f project args))

(defn- with-uglify [f & args]
  (apply run-before :d run-uglify f args))

(defn- with-install-npm-deps [f project & args]
  (apply run-before :e #(install-npm-deps project) f project args))

(defn- with-watchify [f & args]
  (if (in-hook watchify)
    (apply f args)
    (with-redefs [in-hook (conj in-hook watchify)]
      (with-watchify-proc
        #(apply f args)))))

(defn add-hooks []
  (robert.hooke/add-hook #'leiningen.uberjar/uberjar #'with-install-bower-deps)
  (robert.hooke/add-hook #'leiningen.uberjar/uberjar #'with-copy-bower-deps)
  (robert.hooke/add-hook #'leiningen.uberjar/uberjar #'with-install-npm-deps)
  (robert.hooke/add-hook #'leiningen.uberjar/uberjar #'with-browserify)
  (robert.hooke/add-hook #'leiningen.uberjar/uberjar #'with-uglify)

  (robert.hooke/add-hook #'leiningen.deps/deps #'with-install-bower-deps)

  (robert.hooke/add-hook #'leiningen.run/run #'with-install-bower-deps)
  (robert.hooke/add-hook #'leiningen.run/run #'with-copy-bower-deps)
  (robert.hooke/add-hook #'leiningen.run/run #'with-install-npm-deps)
  (robert.hooke/add-hook #'leiningen.run/run #'with-watchify))
