(ns vivd.json-api-schema-test
  (:require [vivd.json-api.schema :refer :all]
            [midje.sweet :refer :all]
            [schema.core :as s]))

(defn check [val]
  (let [str (->> val
                 (s/check MaybeDocument)
                 (pr-str))]
    (or (try
          (read-string str)
          (catch Exception _))
        str)))

(facts "validation"
  (fact "fails when expected"
    (check [])
    => '(not (map? []))

    (check {:data {:id "x"}})
    => truthy
    
    (check {:data {:id "xyz", :type "quux", :attributes {:id "oops"}}})
    => truthy

    (check {:data {:id "xyz", :type "quux", :attributes {:_bad "oops"}}})
    => truthy))
