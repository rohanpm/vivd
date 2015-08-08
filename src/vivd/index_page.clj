(ns vivd.index-page
  (:refer-clojure :rename {map clj-map meta clj-meta time clj-time})
  (:require [clj-template.html5 :refer :all]
            [vivd
             [index :as index]]))

(set! *warn-on-reflection* true)

(defn container-elem [c]
  (div {:class "container"}
       (p
        (a {:href (str (:id c) "/")
            :class "container-link"}
           (:id c))
        (br)
        "Docker: " (:docker-container-id c))))

(defn stylesheet [href]
  (link- {:rel "stylesheet"
          :href href}))

(defn stylesheets []
  (clojure.core/map stylesheet ["public/bootstrap/css/bootstrap.min.css"
                                "public/bootstrap/css/bootstrap-theme.min.css"]))

(defn vivd-head []
  (apply head
         (title "Containers")
         (stylesheets)))

(defn from-index [index]
  (let [ordered (index/vals index)
        ordered (sort-by :timestamp ordered)
        ordered (reverse ordered)]
    (str
     "<!DOCTYPE html>"
     (html
      (vivd-head)
      (body
       (h1 "Containers")
       (apply div (clj-map container-elem ordered)))))))
