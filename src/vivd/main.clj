(ns vivd.main
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

(defn- read-config []
  (with-open [cfg (reader-for-file "data/config.clj")]
    (edn/read cfg)))

(defn -main [& args]
  (println "vivd")
  (let [config (read-config)
        _      (log/debug "Config:" config)]
    (ring-jetty/run-jetty-async (-> ;(partial vivd.http_handler/handler config)
                                    (vivd.http_handler/make-handler config)
                                    reload/wrap-reload)
                                {:port 8080})))
