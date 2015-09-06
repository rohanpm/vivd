(ns leiningen.vivd
  (:require leiningen.uberjar
            leiningen.bower
            leiningen.run
            leiningen.npm
            [clojure.java.io :as io]
            [clojure.java.shell :refer [sh]]
            robert.hooke)
  (:import org.apache.commons.io.FileUtils))

(def browserify "node_modules/.bin/browserify")
(def watchify   "node_modules/.bin/watchify")
(def uglify     "node_modules/.bin/uglify")
(def cleancss   "node_modules/.bin/cleancss")
(def app-bundle "resources/public/js/app-bundle.js")

(def browserify-args ["resources/js/load.jsx"
                      "-g" "babelify"
                      "--extension" ".jsx"
                      "--outfile" app-bundle])

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

(defn- install-bower-deps [project & args]
  (println "bower install...")
  (let  [result (leiningen.bower/bower project "install")]
    (assert (= result 0) "bower install failed")))

(defn- install-npm-deps [project & args]
  (println "npm install...")
  (leiningen.npm/npm project "install"))

(defn- copy-bower-deps [project & args]
  (println "bower copy...")
  (copy-bower-dists project)
  (copy-bower-files project))

(defn- run! [& cmd]
  (let [{:keys [exit] :as result} (apply sh cmd)]
    (if (not (= 0 exit))
      (throw (ex-info "external command failed" (merge result {:cmd cmd}))))))

(defn run-cleancss [project]
  (println "cleancss...")
  (run! cleancss "resources/public/css/vivd.css" "-o" "resources/public/css/app-bundle.css"))

(defn run-browserify [project]
  (println "browserify...")
  (apply run! browserify browserify-args))

(defn run-uglify [project]
  (println "uglify...")
  (run! uglify "-s" app-bundle "-o" app-bundle))

; since watchify is async, wait a bit for it to write the bundle
(defn wait-for-bundle [file]
  (if (or (not (.exists file))
          (= 0 (.length file)))
    (do
      (Thread/sleep 1000)
      (recur file))))

(defn with-watchify [f]
  (println "watchify...")
  (let [bundle-file (io/file app-bundle)
        _           (.delete bundle-file)
        proc        (-> (ProcessBuilder.
                         (into-array (concat [watchify] browserify-args)))
                        (.start))]
    (try
      (wait-for-bundle bundle-file)
      (f)
      (finally
        (.destroy proc)))))

(defn- do-external-deps [project & args]
  (doto project
    (install-bower-deps)
    (install-npm-deps)))

(defn- run-before-uberjar [project & args]
  (doto project
    (do-external-deps)
    (copy-bower-deps)
    (run-cleancss)
    (run-browserify)
    (run-uglify)))

(defn- run-before-run [project & args]
  (doto project
    (do-external-deps)
    (copy-bower-deps)
    (run-cleancss)))

(def in-hook #{})
(defn- run-before [f before-f & args]
  (if (in-hook f)
    (apply f args)
    (with-redefs [in-hook (conj in-hook f)]
      (apply before-f args)
      (apply f args))))

(defn- hook-uberjar [f & args]
  (apply run-before f run-before-uberjar args))

(defn- hook-deps [f & args]
  (apply run-before f do-external-deps args))

(defn- hook-run [f & args]
  (let [bound-f (fn []
                  (apply f args))
        new-f   (fn [&_]
                  (with-watchify bound-f))]
   (apply run-before new-f run-before-run args)))

(defn add-hooks []
  (robert.hooke/add-hook #'leiningen.uberjar/uberjar #'hook-uberjar)
  (robert.hooke/add-hook #'leiningen.deps/deps #'hook-deps)
  (robert.hooke/add-hook #'leiningen.run/run #'hook-run))
