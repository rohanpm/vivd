(ns vivd.json-api.schema
  (:refer-clojure :exclude [fn defn defmethod letfn defrecord])
  (:require [schema.core :refer :all])
  (:import [java.util.regex Pattern]))

(def globally-allowed-member-character
  #"[a-zA-Z0-9\u0080-\uFFFF]")

(def inner-allowed-member-characters
  #"^[a-zA-Z0-9\u0080-\uFFFF_ \-]*$")

(defn- valid-member-name? [kw]
  (let [s     (name kw)
        begin (str (first s))
        end   (str (last s))
        mid   (apply str (butlast (drop 1 s)))]
    (and
     (re-find globally-allowed-member-character begin)
     (re-find inner-allowed-member-characters   mid)
     (re-find globally-allowed-member-character end))))

(defn- not-id-or-type? [kw]
  (not (#{:id :type} kw)))

(defschema MemberName
  (both
   Keyword
   (pred valid-member-name?)))

(defschema AttributeKeyword
  (both
   MemberName
   (pred not-id-or-type?)))

(defschema Attributes
  ; NOT VALIDATED: member naming, etc.
  {AttributeKeyword Any})

(defschema Meta
  {MemberName Any})

(defschema LinkValue
  (either
   Str
   {(required-key :href) Str
    (optional-key :meta) Meta}))

(defschema Links
  {Keyword (maybe LinkValue)})

(defschema Resource
  {(required-key :type)       Str
   ; FIXME: id should be optional on the way in,
   ; required on the way out.
   (optional-key :id)         Str
   (optional-key :attributes) Attributes
   (optional-key :links)      Links})

(defschema ApiError
  {(optional-key :id)     Str
   (optional-key :status) Str
   (optional-key :code)   Str
   (optional-key :title)  Str
   (optional-key :detail) Str
   (optional-key :meta)   Meta})

(defschema CommonDocument
  {(optional-key :links)  Links})

(defschema DocumentWithData
  (merge
   CommonDocument
   {(required-key :data) (either Resource [Resource])}))

(defschema DocumentWithErrors
  (merge
   CommonDocument
   {(required-key :errors) [ApiError]}))

(defschema Document
  (either DocumentWithData DocumentWithErrors))

(defschema MaybeDocument
  (maybe Document))
