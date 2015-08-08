(ns vivd.index-page
  (:refer-clojure :rename {map clj-map meta clj-meta time clj-time})
  (:require [clj-template.html5 :refer :all]))

(set! *warn-on-reflection* true)

(defn container-elem [c]
  (div {:class "container"}
       (p
        (a {:href (str (:id c) "/")
            :class "container-link"}
           (:id c))
        (br)
        "Docker: " (:docker-container-id c))))

(defn vivd-head []
  (head (title "Containers")
        (link- {:rel "stylesheet"
                :href "public/bootstrap/css/bootstrap.min.css"})))

(defn from-index [index]
  (let [ordered (vals index)
        ordered (sort-by :timestamp ordered)
        ordered (reverse ordered)]
    (str
     "<!DOCTYPE html>"
     (html
      (vivd-head)
      (body
       (h1 "Containers")
       (apply div (clj-map container-elem ordered)))))))

(defn index-updater [page-ref]
  (fn [_ _ _ index]
    (swap! page-ref (fn [&_]
                      (from-index index)))))

(defn make [index]
  (let [index-ref (:index-ref index)
        out       (atom (from-index @index-ref))]
    (add-watch index-ref "index-page-maker" (index-updater out))
    out))
