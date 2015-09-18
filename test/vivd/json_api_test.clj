(ns vivd.json-api-test
  (:require [vivd.json-api.middleware :refer [wrap-json-api]]
            [vivd.api-test :refer [str-stream]]
            [clojure.data.json :as json]
            [midje.sweet :refer :all]))

(def json-api-content-type "application/vnd.api+json")

(defn throw-or-return [val]
  (fn [request]
    (if (instance? Throwable val)
      (throw val)
      val)))

(defn wrap-handler [response]
  (-> response
      (throw-or-return)
      (wrap-json-api)))

(defn- maybe-json-read-str [val]
  (if val
    (json/read-str val :key-fn keyword)))

(defn test-handler
  ([response]
     (test-handler response {}))
  ([response request]
     (let [handler (wrap-handler response)
           request (merge  {:scheme         :http
                            :server-port    80
                            :server-name    "vivd.example.com"
                            :request-method :get}
                           request)
           out     (handler request)]
       (update out :body maybe-json-read-str))))

(facts "request content type"
  (fact "accepts no content-type and no body"
    (test-handler {:status 200} {})
    => (contains {:status 200}))

  (fact "accepts no content-type and json body"
    ; by my reading of JSON API, it is not a MUST for servers to reject
    ; a request with no Content-Type.
    (test-handler {:status 200} {:body (str-stream "{\"data\":[]}")})
    => (contains {:status 200}))

  (fact "refuses body with wrong content-type"
    (test-handler {:status 200} {:headers {"content-type" "quux"}, :body (str-stream "{}")})
    => (contains {:status 415}))

  (fact "accepts body with correct content-type"
    (test-handler {:status 200} {:headers {"content-type" json-api-content-type}, :body (str-stream "{\"data\":[]}")})
    => (contains {:status 200})))

(facts "response content type"
  (fact "is set by default"
    (test-handler {:status 200})
    => (contains {:status  200
                  :headers (contains {"content-type" json-api-content-type})}))

  (fact "must not be set to anything else"
    (test-handler {:status 200, :headers {"content-type" "text/plain"}})
    => (contains {:status 500})))

(facts "request Accept"
  (fact "can be unset"
    (test-handler {:status 200})
    => (contains {:status 200}))
  
  (fact "can be set to the correct type"
    (test-handler {:status 200} {:headers {"accept" json-api-content-type}})
    => (contains {:status 200})

    (test-handler {:status 200} {:headers {"accept" "text/plain, application/vnd.api+json, text/html"}})
    => (contains {:status 200}))

  (fact "cannot be set to unrelated types"
    (test-handler {:status 200} {:headers {"accept" "application/json"}})
    => (contains {:status 406}))

  (fact "can be set to correct type and a type with parameters"
    (test-handler {:status 200} {:headers {"accept" "application/vnd.api+json; ext=some-extension, application/vnd.api+json"}})
    => (contains {:status 200}))

  (fact "cannot be set to type with parameters only"
    (test-handler {:status 200} {:headers {"accept" "application/vnd.api+json; ext=some-extension"}})
    => (contains {:status 406})))

(facts "response body"
  (fact "is serialized as JSON"
    (test-handler {:status 200, :body {:data {:id "foo", :type "x"}}})
    => (contains {:status 200,
                  :body   {:data {:id "foo", :type "x"}}}))

  (fact "refuses incorrect body"
    (test-handler {:status 200, :body {:foo :bar}})
    => (contains {:status 500})

    ; cannot have id/type within attributes
    (test-handler {:status 200, :body {:data {:id "foo", :type "x", :attributes {:type "bar"}}}})
    => (contains {:status 500})))

(facts "exceptions"
  (fact "generic exceptions translate to 500"
    (test-handler (NullPointerException.))
    => (contains  {:body {:errors [{:detail "An internal server error has occurred.",
                                    :status "500",
                                    :title "internal error"}]},
                   :status 500}))

  (fact "errors can be passed via ex-info"
    (test-handler (ex-info "test" {:json-api-error {:status "444",
                                                    :title  "foo bar"}}))
    => (contains {:status 444,
                  :body   {:errors [{:title "foo bar", :status "444"}]}})))

(facts "links"
  (fact "are made absolute in body"
    (test-handler {:body {:data {:id    "abc"
                                 :type  "abc"
                                 :links {:self "/foo/bar"}}}})
    => (contains {:body
                  (contains {:data
                             (contains {:links
                                        {:self
                                         "http://vivd.example.com:80/foo/bar"}})})})

    (test-handler {:body {:data [{:id    "abc"
                                   :type  "abc"
                                   :links {:self "/foo/bar"}}]}})
    => (contains {:body
                  (contains {:data (just
                                    [(contains {:links
                                                {:self
                                                 "http://vivd.example.com:80/foo/bar"}})])})})

    (test-handler {:body {:data  []
                          :links {:foo {:href "/foo/bar"}}}})
    => (contains {:body
                  (contains {:links
                             {:foo
                              {:href 
                               "http://vivd.example.com:80/foo/bar"}}})}))

  (fact "is made absolute in Location"
    (test-handler {:body {:data []} :headers {"location" "/foo/bar"}})
    => (contains {:headers (contains {"location" "http://vivd.example.com:80/foo/bar"})})))

(facts "CORS"
  (fact "allows all Origin"
    (test-handler {:status 200} {:headers {"origin" "http://example.com/"}})
    => (contains {:status 200
                  :headers (contains {"Access-Control-Allow-Origin" "http://example.com/"})})))
