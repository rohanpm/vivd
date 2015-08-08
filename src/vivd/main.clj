(ns vivd.main
  (:gen-class)
  (:require [vivd 
             [utils :refer :all]
             http_handler
             [container :as container]
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

(defn- read-config []
  (with-open [cfg (reader-for-file "data/config.clj")]
    (edn/read cfg)))

(defn -main [& args]
  (println "vivd")
  (let [config (read-config)
        config (merge (default-config) config)
        _      (log/debug "Config:" config)]
    (ring-jetty/run-jetty-async (-> ;(partial vivd.http_handler/handler config)
                                    (vivd.http_handler/make-handler config)
                                    reload/wrap-reload)
                                {:port 8080})))
