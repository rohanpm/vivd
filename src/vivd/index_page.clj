(ns vivd.index-page
  (:refer-clojure :rename {map clj-map meta clj-meta time clj-time})
  (:require [clj-template.html :refer :all]))

(set! *warn-on-reflection* true)

(defn container-elem [c]
  (div {:class "container"}
       (p
        (a {:href (str (:id c) "/")
            :class "container-link"}
           (:id c))
        (br)
        "Docker: " (:docker-container-id c))))

(defn from-index [index]
  (let [ordered (vals index)
        ordered (sort-by :timestamp ordered)
        ordered (reverse ordered)]
    (html
     (head (title "Containers"))
     (body
      (h1 "Containers")
      (apply div (clj-map container-elem ordered))))))

(defn index-updater [page-ref]
  (fn [_ _ _ index]
    (swap! page-ref (fn [&_]
                      (from-index index)))))

(defn make [index-ref]
  (let [out (atom (from-index @index-ref))]
    (add-watch index-ref "index-page-maker" (index-updater out))
    out))
