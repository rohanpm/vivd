(ns vivd.proxy-test
  (:require [vivd.proxy :refer [get-proxy-request]]
            vivd.container
            clojure.core.cache
            [midje.sweet :refer :all]))

(def test-container {:id                  "aAbBcC"
                     :docker-container-id "abcdef"})

(def test-config {:docker-http-port 123})

(def test-inspect {"abcdef" {:NetworkSettings {:Ports {:123/tcp [{:HostIp   "127.0.0.2"
                                                                   :HostPort "5566"}]}}}})
(def test-inspect-cache
  (atom (clojure.core.cache/basic-cache-factory test-inspect)))

(def common-request-parts {:as               :stream
                           :follow-redirects false
                           :throw-exceptions false
                           :scheme           :http
                           :server-name      "127.0.0.2"
                           :server-port      5566})

(defn expected-request [r]
  (merge common-request-parts r))

(def base-request {:container-uri     "foo/bar"
                   :container-vivd-id "aAbBcC"
                   :remote-addr       "10.0.0.1"
                   :headers           {"host" "example.com"}})

(with-redefs [vivd.container/INSPECT-CACHE test-inspect-cache]
  (facts "get-proxy-request"
    (fact "returns expected values"
      (get-proxy-request base-request test-config test-container)
      => (expected-request {:uri "/foo/bar"
                            :headers {"host"             "127.0.0.2"
                                      "connection"       "close"
                                      "x-forwarded-for"  "10.0.0.1"
                                      "x-forwarded-host" "example.com"}}))

    (fact "passes through expected values"
      (get-proxy-request (merge base-request {:request-method :foo
                                              :query-string   "hi&there"
                                              :body           "some-body"}) test-config test-container)
      => (contains {:request-method :foo
                    :query-string   "hi&there"
                    :body           "some-body"}))

    (fact "passes through headers"
      (get-proxy-request (merge base-request {:headers {"content-type" "text/plain"
                                                        "accept"       "application/json"}})
                         test-config test-container)
      => (contains {:headers (contains {"content-type" "text/plain"
                                        "accept"       "application/json"})}))
    
    (fact "strips content-length"
      (get-proxy-request (merge base-request {:headers {"content-length" "22"}})
                         test-config test-container)
      =not=> (contains {:headers (contains {"content-length" anything})}))))
