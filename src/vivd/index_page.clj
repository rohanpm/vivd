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

(defn remove-leading [^String from ^String remove]
  (if (.startsWith from remove)
    (.substring from (.length remove))))

(defn abbr-for-gerrit [ref-end]
  (let [[_ change ps] (clojure.string/split ref-end #"/")]
    (abbr {:title (str "refs/changes/" ref-end)}
          (span {:class "small text-uppercase"}
                "change"
                change
                "patchset"
                ps))))

(defn abbr-for-ref [ref]
  (if-let [simple (or (remove-leading ref "refs/heads/")
                      (remove-leading ref "refs/tags/"))]
    (abbr {:title ref} simple)
    (if-let [chg (remove-leading ref "refs/changes/")]
      (abbr-for-gerrit chg))))

(defn git-ref-info [{:keys [git-ref]}]
  (or (abbr-for-ref git-ref)
      git-ref))

(defn git-revision-info [{:keys [git-revision git-oneline]}]
  (code
   (or
    git-oneline
    git-revision)))

(defn container-git [c]
  (span
   (git-ref-info c)
   (br-)
   (git-revision-info c)))

(defn container-last-used [{:keys [timestamp]}]
  (abbr {:class "timeago"
         :title (str timestamp)}
        (str timestamp)))

(defn status-attributes [status]
  (cond
   (= status :up)
   {:button-type "success" :text "Running" :icon-type "play"}

   (= status :new)
   {:button-type "info" :text "New" :icon-type "pause"}

   (= status :building)
   {:button-type "warning" :text "Building" :icon-type "hourglass"}

   (= status :built)
   {:button-type "warning" :text "Built" :icon-type "hourglass"}

   (= status :starting)
   {:button-type "warning" :text "Starting" :icon-type "hourglass"}

   (or (= status :stopping) (= status :stopped))
   {:button-type "danger" :text "Stopped" :icon-type "stop"}

   :else
   {:button-type "default" :text "Unknown" :icon-type "question-sign"}))

(defn glyphicon [name]
  (span {:class       (str "glyphicon glyphicon-" name)
         :aria-hidden true}))

(defn container-status [{:keys [status]}]
  (let [{:keys [button-type text icon-type]} (status-attributes status)]
    (button {:type "button"
             :class (str "btn btn-block btn-" button-type)}
            (span {:class "pull-left"}
                  "&nbsp;&nbsp;"
                  (if icon-type (glyphicon icon-type)))
            text)))

(defn container-columns [c]
  (clj-map td [(container-link c)
               (container-git c)
               (container-last-used c)
               (container-status c)]))

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
                 "Last Used"
                 "Status"]))))

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
       (div {:class "container"}
            (h1 "Containers")
            (container-table ordered)))))))
