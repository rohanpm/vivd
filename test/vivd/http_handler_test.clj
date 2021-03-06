(ns vivd.http-handler-test
  (:require [vivd.http-handler :refer :all]
            [midje.sweet :refer :all]))

(def FAKE-INDEX {:index-ref (ref {"a1b2c3" {}
                                  "aabbcc" {}})})

(def request-referred-from-container? #'vivd.http-handler/request-referred-from-container?)
(def redirect-handler (#'vivd.http-handler/make-redirect-handler FAKE-INDEX))
(def vivd-url #'vivd.http-handler/vivd-url)

(facts "vivd-url"
  (fact "derives from Host header"
    (vivd-url {:headers {"host" "foo.example.com:8080"}}) => "http://foo.example.com:8080/"))

(facts "request-referred-from-container?"
  (fact "false when appropriate"
    (request-referred-from-container? {:headers {}} "abc") => falsey

    (request-referred-from-container? {:headers {"referer" "http://localhost/quux"
                                                 "host"    "localhost"}}
                                      "foo")
    => falsey

    (request-referred-from-container? {:headers {"referer" "http://localhost/quux/b"
                                                 "host"    "otherhost"}}
                                      "quux")
    => falsey)

  (fact "true when appropriate"
    (request-referred-from-container? {:headers {"referer" "http://example.com/abcdef/foo/bar"
                                                 "host"    "example.com"}}
                                      "abcdef")
    => truthy))


(facts "redirect-handler"

  (fact "returns nil for requests not referred by a container"
    (redirect-handler {:uri "/foo" :headers {}}) => nil
    (redirect-handler {:uri "/quux" :headers {"referer" "http://localhost/something"
                                              "host"    "localhost"}}) => nil)

  (fact "redirects for referred requests escaping the container"
    (redirect-handler {:uri "/foo" :headers {"referer" "http://localhost/a1b2c3/x"
                                             "host"    "localhost"}})
    => {:status 307 :headers {"location" "http://localhost/a1b2c3/foo"}})

  (fact "retains the query string"
    (redirect-handler {:uri "/bar" :query-string "x=y&a=b"
                       :headers {"referer" "http://example.com/aabbcc/x/y/z"
                                 "host"    "example.com"}})
    => {:status 307 :headers {"location" "http://example.com/aabbcc/bar?x=y&a=b"}}))
