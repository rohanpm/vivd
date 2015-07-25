(ns vivd.types
  (:use [clojure.core.typed :exclude [atom doseq let fn defn ref dotimes
                                      defprotocol loop for]]))

(defalias ContainerInfo
  (HMap
   :mandatory {:docker-id String}
   :complete? true))

(defalias ShellResult
  (HMap
   :mandatory {:exit Num, :out String, :err String}
   :complete? true))

(defalias JsonObject
  (U (Vec Any) (Map Any Any)))

(defalias HttpMethod
  (U
   (Val :get)
   (Val :post)
   (Val :put)
   (Val :patch)
   (Val :delete)))

(defalias ContainerRequestAttrs
  (HMap
   :mandatory {:container-vivd-id String
               :container-uri String}))

(defalias RingRequest
  (HMap
   :mandatory {:uri String
               :request-method HttpMethod}))

(defalias ContainerRequest
  (I RingRequest ContainerRequestAttrs))

(defalias RingResponse
  (HMap
   :mandatory {:status Integer}))

(defalias RingHandler
  [RingRequest -> RingResponse])

(defalias HostIpPort
  (HMap
   :mandatory {:HostIp String
               :HostPort String}
   :complete? true))

(defalias DockerInspect
  (HMap
   :mandatory {:NetworkSettings
               (HMap :mandatory {:Ports
                                 (Map String (Vec HostIpPort))})
               }))

(ann ^:no-check clojure.java.io/reader
     [Any -> java.io.Reader])

(ann ^:no-check clojure.java.io/file
     [String * -> java.io.File])

(ann ^:no-check clojure.java.shell/sh
     [String Any * -> ShellResult])

(ann ^:no-check clojure.data.json/read-str
     [String -> JsonObject])

(ann ^:no-check clojure.data.json/write-str
     [JsonObject -> String])

(ann ^:no-check clojure.core.cache/through 
     (All [cache key]
          [[key -> Any] cache key -> cache]))

(ann ^:no-check clojure.core.cache/lookup
     [Any Any -> Any])

(ann ^:no-check clojure.core.cache/evict 
     (All [cache]
          [cache Any -> cache]))

(ann ^:no-check clojure.edn/read
     [java.io.PushbackReader -> Any])

(ann ^:no-check ring.adapter.jetty-async/run-jetty-async
     [RingHandler (HMap) -> Any])

(ann ^:no-check ring.middleware.reload/wrap-reload
     [RingHandler -> RingHandler])
