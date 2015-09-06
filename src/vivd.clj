(ns vivd
  (:require [clojure.java.io :refer [resource]]))

(def version
  (or
   (System/getProperty "vivd.version")
   (-> (resource "project.clj")
       slurp
       read-string
       (nth 2))))
