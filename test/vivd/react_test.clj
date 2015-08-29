(ns vivd.react-test
  (:require [midje.sweet :refer :all]
            vivd.react.renderer)
  (:import [javax.script ScriptEngine ScriptContext]))

(def script-engine #'vivd.react.renderer/script-engine)
(def warm-script-engine #'vivd.react.renderer/warm-script-engine)
(def render #'vivd.react.renderer/render)
(def make #'vivd.react.renderer/make)

(facts "script engine"
  (fact "sanity test"
    (-> (script-engine)
        (.eval "'hi ' + 'there'"))
    => "hi there")

  (fact "utf-8 can survive"
    (-> (script-engine)
        (.eval "'hi\\u00a0there'"))
    => "hi\u00a0there")

  (fact "can load app bundle"
    (warm-script-engine)
    ; just testing it doesn't throw...
    => anything

    (-> (warm-script-engine)
        (.eval "window.renderAppForState"))
    => truthy

    (-> (warm-script-engine)
        (.eval "window.garbage"))
    => nil)

  (fact "can render"
    (-> (make)
        (render {"title" "an example title"}))
    => (contains "an example title</h1>")))
