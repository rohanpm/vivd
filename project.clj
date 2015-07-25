(defproject vivd "1.0.0"
  :main vivd.main
  :plugins [[lein-typed "0.3.5"]]
  :core.typed {:check [vivd.container vivd.main vivd.proxy vivd.http_handler vivd.types]}
  :dependencies [[ring "1.4.0"]
                 [org.clojure/clojure "1.7.0"]
                 [org.clojure/tools.logging "0.3.1"]
                 [org.clojure/data.json "0.2.6"]
                 [org.clojure/core.cache "0.6.4"]
                 [org.clojure/core.typed "0.3.8"]
                 [com.ninjudd/ring-async "0.2.0"]
                 [clj-http "2.0.0"]
                 [log4j/log4j "1.2.17"]
                 [clj-time "0.10.0"]])
