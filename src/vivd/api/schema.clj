(ns vivd.api.schema
  (:require [vivd.json-api.schema :refer [Resource]]
            [schema.core :refer [defschema required-key Str eq both]]))

(defschema ContainerResourceInAttributes
  {(required-key :git-ref)      Str
   ; TODO validate git revision
   (required-key :git-revision) Str})

(defschema ContainerResourceIn
  (both
   Resource
   {(required-key :type)      (eq "containers")
    (required-key :attributes) ContainerResourceInAttributes}))
