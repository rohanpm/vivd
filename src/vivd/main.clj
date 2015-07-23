(ns vivd.main
  (:require vivd.http_handler
            [ring.adapter.jetty :as ring-jetty]
            [ring.middleware.reload :as reload]))


(defn -main [& args]
  (println "vivd")
  (ring-jetty/run-jetty (-> vivd.http_handler/handler
                            reload/wrap-reload)
                        {:port 8080}))
