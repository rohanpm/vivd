(ns vivd.main
  (:gen-class)
  (:require [vivd 
             [utils :refer :all]
             http_handler
             [container :as container]
             services
             index]
            [ring.adapter.jetty-async :as ring-jetty]
            [ring.middleware.reload :as reload]
            [clojure.tools.logging :as log]
            [clojure.edn :as edn]
            [clojure.java.io :as io]))

(set! *warn-on-reflection* true)

(defn- default-config []
  {:startup-timeout   120,
   :max-containers    100,
   :max-containers-up 4,
   :title             "Containers",
   :default-url       "/",
   :docker-http-port  80})

(defn- default-jetty-config []
  {:port 8080
   :max-threads 100})

(defn- read-config []
  (with-open [cfg (reader-for-file "data/config.clj")]
    (edn/read cfg)))

(defn -main [& args]
  (let [config       (read-config)
        config       (merge (default-config) config)
        jetty-config (or (:jetty-config config) {})
        jetty-config (merge (default-jetty-config) jetty-config)
        _            (log/debug "Config:" config jetty-config)
        services     (vivd.services/make config)]
    (ring-jetty/run-jetty-async (-> (vivd.http_handler/make config services)
                                    reload/wrap-reload)
                                jetty-config)))
