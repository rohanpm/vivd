(ns vivd.index-page
  (:refer-clojure :rename {map clj-map meta clj-meta time clj-time})
  (:require [clj-template.html5 :refer :all]
            [vivd
             [index :as index]]))

(set! *warn-on-reflection* true)

(defn stylesheet [href]
  (link- {:rel "stylesheet"
          :href href}))

(defn stylesheets []
  (clj-map stylesheet ["public/vendor/bootstrap/css/bootstrap.min.css"
                       "public/vendor/bootstrap/css/bootstrap-theme.min.css"]))

(defn javascript [src]
  (script {:type "text/javascript"
           :src  src}))

(defn javascripts []
  (clj-map javascript ["public/vendor/jquery/jquery.min.js"
                       "public/vendor/jquery.timeago.js"
                       "public/vivd.js"]))

(defn vivd-head []
  (apply head
         (title "Containers")
         (concat (stylesheets)
                 (javascripts))))

(defn container-link [c]
  (a {:href (str (:id c) "/")
      :class "container-link"}
     (:id c)))

(defn container-git [{:keys [git-revision]}]
  git-revision)

(defn container-last-used [{:keys [timestamp]}]
  (abbr {:class "timeago"
         :title (str timestamp)}
        (str timestamp)))

(defn container-columns [c]
  (clj-map td [(container-link c)
               (container-git c)
               (container-last-used c)]))

(defn container-row [container]
  (apply tr
         (container-columns container)))

(defn container-rows [containers]
  (clj-map container-row containers))

(defn container-table-header []
  (thead
   (apply tr
    (clj-map td ["ID"
                 "Git"
                 "Last Used"]))))

(defn container-table [containers]
  (apply table {:class "table table-striped"}
         (container-table-header)
         (container-rows containers)))

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
       (container-table ordered))))))
