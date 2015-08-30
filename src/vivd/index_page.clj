(ns vivd.index-page
  (:refer-clojure :rename {map clj-map meta clj-meta time clj-time})
  (:require [clj-template.html5 :refer :all]
            [vivd
             [index :as index]]
            [vivd.react.renderer :as renderer]
            [vivd.api.containers :as containers]
            [clojure.tools.logging :as log]
            [clojure.data.json :as json]))

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

(defn vivd-head [config]
  (apply head
         (title (:title config))
         (meta {:charset "utf-8"})
         (stylesheets)))

(defn- request-for-state [{:keys [config]} {:keys [params] :as request}]
  {:params
   (merge {"page[limit]" (:per-page config)}
          (select-keys params ["page[limit]"
                               "page[offset]"]))
   :uri
   "/a/containers"})

(defn- state [{:keys [config index] :as services} request]
  (let [req            (request-for-state services request)
        {:keys [body]} (containers/get-containers services req)]
    {:title      (:title config)
     :containers body}))

(defn- set-state-tag [state]
  (script {:type "text/javascript"}
          (str "serverState = " state ";")))

(defn- render-for-index [{:keys [renderer] :as services} request]
  (let [st (state services request)
        st (json/write-str st)]
    (str
     (renderer/render renderer st)
     (set-state-tag st))))

(defn from-index [{:keys [config index renderer] :as services} request]
  (let [ordered (index/vals index)
        ordered (sort-by :timestamp ordered)
        ordered (reverse ordered)]
    (str
     "<!DOCTYPE html>"
     (html
      (vivd-head config)
      (body
       (render-for-index services request)
       (javascript "public/js/app-bundle.js"))))))
