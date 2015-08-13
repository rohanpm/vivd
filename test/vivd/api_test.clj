(ns vivd.api-test
  (:require [vivd.api :refer [make-create-handler]]
            [vivd.index :as index]
            [vivd.index-test :refer :all]
            [midje.sweet :refer :all]
            [clojure.java.io :as io]))

(defn test-create [index request]
  (let [handler  (make-create-handler index)
        response (handler request)]
    {:response   response
     :index-vals (index/vals index)}))

(facts "create-handler"
  (let [existing-container {:id "aBcD" :git-revision "00112233"}
        index              (index-for [existing-container])]
    (with-in-str "{\"git-ref\":\"foo\",\"git-revision\":\"aabbcc\"}"
      (fact "can create new object"
        (test-create index {:uri            "/a/container"
                            :request-method :post
                            :body           *in*})
        => (contains {:response   (contains {:status  201
                                             :headers (contains {"location" truthy})})
                      :index-vals (just [existing-container
                                         (contains {:git-ref      "foo"
                                                    :git-revision "aabbcc"
                                                    :status       :new})]
                                        :in-any-order)})))))
