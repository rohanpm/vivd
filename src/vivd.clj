(ns vivd)

(def version
  (-> "project.clj"
      slurp
      read-string
      (nth 2)))
