(defproject vivd "1.1.2-git"
  :main vivd.main
  :dependencies [[ring "1.4.0"]
                 [org.clojure/clojure "1.7.0"]
                 [org.clojure/tools.logging "0.3.1"]
                 [org.clojure/data.json "0.2.6"]
                 [org.clojure/core.cache "0.6.4"]
                 [org.immutant/web "2.0.2"]
                 [org.clojure/core.async "0.1.346.0-17112a-alpha"]
                 [compojure "1.4.0"]
                 [clj-http "2.0.0"]
                 [clj-template "1.0.1"]
                 [log4j/log4j "1.2.17"]
                 [prismatic/schema "0.4.4"]
                 [clj-time "0.10.0"]]
  :bower-dependencies [[bootstrap "3.3.5"]
                       [jquery-timeago "1.4.1"]]
  :bower {:directory "bower_components"
          :flat-files ["jquery-timeago/jquery.timeago.js"]}
  :hooks [leiningen.vivd/add-hooks]
  :profiles {:dev {:plugins      [[lein-midje "3.1.3"]
                                  [lein-bower "0.5.1"]]
                   :dependencies [[midje "1.6.3"]]}
             :uberjar {:aot :all}})
