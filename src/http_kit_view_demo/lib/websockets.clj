(ns http-kit-view-demo.lib.websockets
  (:import
   [java.io ByteArrayInputStream ByteArrayOutputStream])
  (:require
   [clojure.java.io :as io]
   [cognitect.transit :as transit]
   [org.httpkit.server :refer [send!]]
   [clojure.tools.logging :refer [info]]
   ))

(defn send-transit!
  [channel msg]
  (info "MESG? " msg)
  (let [out         (ByteArrayOutputStream. 4096)
        json-writer (transit/writer out :json)]
    (transit/write json-writer msg)
    (send! channel (.toByteArray out))))

(defn receive-transit!
  [data]
  (let [in          (io/input-stream (.getBytes data))
        json-reader (transit/reader in :json)]
    (info "DATA? " data)
    (transit/read json-reader)))
