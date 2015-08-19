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

(defschema AttributeKeyword
  (both
   Keyword
   (pred not-id-or-type?)
   (pred valid-member-name?)))

(defschema Attributes
  ; NOT VALIDATED: member naming, etc.
  {AttributeKeyword Any})

(defschema Resource
  {(required-key :type)       Str
   (required-key :id)         Str
   (optional-key :attributes) Attributes})

(defschema Document
  ; HACKED ; not complete
  {(optional-key :data)       Resource})

(defschema MaybeDocument
  (maybe Document))
