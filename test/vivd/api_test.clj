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

(defn str-stream [str]
  (-> (java.io.StringReader. str) clojure.lang.LineNumberingPushbackReader.))

(facts "create-handler"
  (let [existing-container {:id "aBcD" :git-revision "00112233"}
        index              (fn [] (index-for [existing-container]))]
    (fact "can create new object"
      (test-create (index) {:uri            "/a/container"
                            :request-method :post
                            :body           (str-stream "{\"git-ref\":\"foo\",\"git-revision\":\"aabbcc\"}")})
      => (contains {:response   (contains {:status  201
                                           :headers (contains {"location" truthy})})
                    :index-vals (just [existing-container
                                       (contains {:git-ref      "foo"
                                                  :git-revision "aabbcc"
                                                  :status       :new})]
                                      :in-any-order)}))

    (fact "can create new object with unique-git-revision"
      (test-create (index) {:uri            "/a/container"
                            :query-string   "unique-git-revision=1x"
                            :request-method :post
                            :body           (str-stream "{\"git-ref\":\"foo\",\"git-revision\":\"aabbcc\"}")})
      => (contains {:response   (contains {:status  201
                                           :headers (contains {"location" truthy})})
                    :index-vals (just [existing-container
                                       (contains {:git-ref      "foo"
                                                  :git-revision "aabbcc"
                                                  :status       :new})]
                                      :in-any-order)}))

    (fact "looks up existing container by git revision"
      (test-create (index) {:uri            "/a/container"
                            :query-string   "foo=bar&unique-git-revision=1"
                            :request-method :post
                            :body           (str-stream "{\"git-ref\":\"foo\",\"git-revision\":\"00112233\"}")})
      => (contains {:response   (contains {:status  302
                                           :headers (contains {"location" "/aBcD"})})
                    :index-vals (just [existing-container])}))))
