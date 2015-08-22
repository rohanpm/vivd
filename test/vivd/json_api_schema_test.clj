(ns vivd.json-api-schema-test
  (:require [vivd.json-api.schema :refer :all]
            [midje.sweet :refer :all]
            [schema.core :as s]))

(defn check [val]
  (let [checked     (s/check MaybeDocument val)
        checked-str (pr-str checked)]
    (if checked
      (or (try
            (read-string checked-str)
            (catch Exception _))
          checked-str))))

(facts "validation"
  (fact "fails when expected"
    (check [])
    => '(not (map? []))

    (check {:data {:id "x"}})
    => truthy
    
    (check {:data {:id "xyz", :type "quux", :attributes {:id "oops"}}})
    => truthy

    (check {:data {:id "xyz", :type "quux", :attributes {:_bad "oops"}}})
    => truthy

    ; cannot contain both data and errors
    (check {:data [] :errors []})
    => truthy))
