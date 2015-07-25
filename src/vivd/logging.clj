(ns vivd.logging
  (:require [clojure.tools.logging :as log]
            [clojure.core.typed :as t]))

(defmacro info [& rest]
  `(t/tc-ignore
    (log/info ~@rest)))

(defmacro debug [& rest]
  `(t/tc-ignore
    (log/debug ~@rest)))
