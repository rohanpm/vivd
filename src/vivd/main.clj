(ns vivd.main
  (:require [vivd 
             [types :refer :all]
             http_handler]
            [ring.adapter.jetty :as ring-jetty]
            [ring.middleware.reload :as reload]
            [clojure.core.typed :refer [typed-deps ann Any]]))

(typed-deps vivd.types)
(set! *warn-on-reflection* true)

(ann -main [String * -> Any])
(defn -main [& args]
  (println "vivd")
  (ring-jetty/run-jetty (-> vivd.http_handler/handler
                            reload/wrap-reload)
                        {:port 8080}))
