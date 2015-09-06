(ns vivd.react.renderer
  (:require [clojure.java.io :as io]
            [clojure.tools.logging :as log]
            [clojure.data.json :as json])
  (:import [javax.script ScriptContext ScriptEngine ScriptEngineManager]))

(set! *warn-on-reflection* true)

(def version (System/getProperty "vivd.version"))
(def server-js "js/server.js")
(def bundle-js (str "public/js/app-bundle-" version ".js"))

(defn- get-rhino [^ScriptEngineManager mgr]
  (.getEngineByName mgr "rhino"))

(defn- get-nashorn [^ScriptEngineManager mgr]
  (if-let [out (.getEngineByName mgr "nashorn")]
    (do
      ; Nashorn crashes like this within react:
      ; java.lang.ArrayIndexOutOfBoundsException: 10
      ; at java.lang.invoke.MethodHandleImpl$ArrayAccessor.getElementL(MethodHandleImpl.java:130)
      ; at jdk.nashorn.internal.scripts.Script$Recompilation$390$853429AA$\^eval\_.L:20194$L:20195$instantiateReactComponent(<eval>:20277)
      ; ... in at least java 1.8.0.51.
      ; I haven't investigated in depth but http://stackoverflow.com/questions/26189940/java-8-nashorn-arrayindexoutofboundsexception
      ; gave me the impression the bug is likely to be in Nashorn.
      (log/warn (str
                 "Rhino not available; using Nashorn script engine. "
                 "NOT RECOMMENDED: crashes have been observed. "
                 "If you are using Java >= 1.8 and experience crashes, "
                 "try Java 1.7."))
      out)))

(defn- ^ScriptEngine script-engine []
  (let [mgr   (ScriptEngineManager.)]
    (or
     (get-rhino mgr)
     (get-nashorn mgr))))

(defn- eval-resource [^ScriptEngine engine resource]
  (with-open [rdr (-> resource
                      (io/resource)
                      (io/reader))]
    (.eval engine rdr)))

(defn- ^ScriptEngine warm-script-engine* []
  (let [engine (script-engine)
        _      (eval-resource engine server-js)
        _      (eval-resource engine bundle-js)]
    engine))

; This is a hack to support development...
; When using watchify, the bundle JS can be empty when the server starts.
; Avoid crashing in that case.
(defn warm-script-engine
  ([]
     (warm-script-engine 30))
  ([i]
     (let [engine (warm-script-engine*)
           f      (.eval engine "window.renderAppForState")]
       (if f
         engine
         (if (< i 0)
           (throw (ex-info "bundle did not expose render function" {}))
           (do
             (log/debug "bundle js did not load properly, retry soon")
             (java.lang.Thread/sleep 1000)
             (recur (- i 1))))))))

(defn render [{:keys [^ScriptEngine engine lock]} state]
  (locking lock
    (let [state (if (string? state)
                  state
                  (json/write-str state))
          prog (str
                                        ; there's surely a way to more directly pass in the object,
                                        ; but I can't figure it out ...
                "window.renderAppForState("
                state
                ")")
          out  (.eval engine prog)]
      out)))

(defn make []
  {:engine (warm-script-engine)
   :lock   (Object.)})
