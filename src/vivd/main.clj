(ns vivd.main
  (:gen-class)
  (:require [vivd 
             [utils :refer :all]
             http-handler
             [container :as container]
             services
             index]
            vivd
            [immutant.web :as web]
            [ring.middleware.reload :as reload]
            [clojure.tools.logging :as log]
            [clojure.edn :as edn]
            [clojure.java.io :as io]))

(set! *warn-on-reflection* true)

(defn- default-config []
  {:startup-timeout   120,
   :max-containers    10000,
   :max-containers-built 100,
   :max-containers-up 4,
   :per-page          20,
   :title             "Containers",
   :default-url       "/",
   :docker-http-port  80
   :http-port         8080
   :http-host         "0.0.0.0"})

(defn- read-config []
  (with-open [cfg (reader-for-file "data/config.clj")]
    (edn/read cfg)))

(defn- make-web-config [{:keys [http-port http-host]}]
  {:host http-host
   :port http-port})

(defn -main [& args]
  (println "vivd" vivd/version)
  (let [config       (read-config)
        config       (merge (default-config) config)
        web-config   (make-web-config config)
        _            (log/debug "Config:" config web-config)
        services     (vivd.services/make config)
        services     (merge services {:config config})]
    (web/run
      (-> (vivd.http-handler/make config services)
          reload/wrap-reload)
      web-config)))
