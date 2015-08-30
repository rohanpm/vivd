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
  :bower-dependencies [[bootstrap "3.3.5"]]
  :bower {:directory "bower_components"
          :copy-dist ["bootstrap"]}
  :node-dependencies [[babelify "6.2.0"]
                      [browserify "11.0.1"]
                      [watchify "3.3.1"]
                      [barracks "6.0.4"]
                      [react "0.13.3"]
                      [uglify "0.1.5"]
                      [react-timeago "2.2.1"]
                      [query-string "2.4.0"]]
  :hooks [leiningen.vivd/add-hooks]
  :profiles {:dev {:plugins      [[lein-midje "3.1.3"]
                                  [lein-bower "0.5.1"]
                                  [lein-npm "0.5.1"]]
                   :dependencies [[midje "1.6.3"]]}
             :uberjar {:aot :all}})
