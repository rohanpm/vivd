(ns vivd.utils
  (:require [clojure.java.io :as io]))

(defn reader-for-file ^java.io.PushbackReader [& args]
  (->  (apply io/file args)
       (io/reader)
       (java.io.PushbackReader.)))
