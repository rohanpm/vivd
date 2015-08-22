(ns vivd.json-api-schema-test
  (:require [vivd.json-api.schema :refer :all]
            [midje.sweet :refer :all]
            [schema.core :as s]))

(def sample-containers-response
  {:data [{:id "Q8pLcQMn"
           :type "container"
           :attributes {:status :new}},
          {:id "tKdQ3AGf"
           :type "container"
           :attributes {:status :new}}],
   :links {:first "/a/containers?page[offset]=0&page[limit]=200"
           :next nil
           :prev nil}})

(defn check 
  ([val] (check MaybeDocument val))
  ([schema val]
     (let [checked     (s/check schema val)
           checked-str (pr-str checked)]
       (if checked
         (or (try
               (read-string checked-str)
               (catch Exception _))
             checked-str)))))

(facts "validation"
  (fact "validates when expected"
    (check nil)
    => nil

    (check DocumentWithData {:data []})
    => nil

    (check {:data []})
    => nil

    (check {:errors [{:title "oops"}]})
    => nil

    (check [Resource] (:data sample-containers-response))
    => nil

    (check Links (:links sample-containers-response))
    => nil

    (check sample-containers-response)
    => nil)

  (fact "fails when expected"
    (check [])
    => truthy

    (check {:data {:id "x"}})
    => truthy
    
    (check {:data {:id "xyz", :type "quux", :attributes {:id "oops"}}})
    => truthy

    (check {:data {:id "xyz", :type "quux", :attributes {:_bad "oops"}}})
    => truthy

    ; cannot contain both data and errors
    (check {:data [] :errors []})
    => truthy))
