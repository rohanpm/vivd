(ns vivd.main
  (:require [vivd 
             [types :refer :all]
             [logging :as log]
             [utils :refer :all]
             http_handler]
            [ring.adapter.jetty-async :as ring-jetty]
            [ring.middleware.reload :as reload]
            [clojure.core.typed :refer [typed-deps ann Any]]
            [clojure.edn :as edn]))

(typed-deps vivd.types)
(set! *warn-on-reflection* true)

(defn- read-config []
  (with-open [cfg (reader-for-file "data/config.clj")]
    (edn/read cfg)))

(ann -main [String * -> Any])
(defn -main [& args]
  (println "vivd")
  (let [config (read-config)
        _      (log/debug "Config:" config)]
    (ring-jetty/run-jetty-async (-> (partial vivd.http_handler/handler config)
                                    reload/wrap-reload)
                                {:port 8080})))
