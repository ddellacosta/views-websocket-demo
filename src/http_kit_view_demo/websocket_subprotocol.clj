(ns http-kit-view-demo.websocket-subprotocol
  (:import
   [org.httpkit.server AsyncChannel])
  (:require
   [clojure.tools.logging :refer [info]]
   [org.httpkit.server :refer [accept]]
   [clojure.string :refer [split trim lower-case]]))
 
(defn origin-match? [origin-re req]
  (if-let [req-origin (get-in req [:headers "origin"])]
    (re-matches origin-re req-origin)))
 
(defn subprotocol? [proto req]
  (if-let [protocols (get-in req [:headers "sec-websocket-protocol"])]
    (some #{proto}
      (map #(lower-case (trim %))
        (split protocols #",")))))
 
(defmacro with-subprotocol-channel
  [request ch-name origin-re subproto & body]
  `(let [~ch-name (:async-channel ~request)]
     (if (:websocket? ~request)
       (if-let [key# (get-in ~request [:headers "sec-websocket-key"])]
         (if (origin-match? ~origin-re ~request)
           (if (subprotocol? ~subproto ~request)
             (do
               (.sendHandshake ~(with-meta ch-name {:tag `AsyncChannel})
                 {"Upgrade"    "websocket"
                  "Connection" "Upgrade"
                  "Sec-WebSocket-Accept"   (accept key#)
                  "Sec-WebSocket-Protocol" ~subproto})
               ~@body
               {:body ~ch-name})
             {:status 400 :body "missing or bad WebSocket-Protocol"})
           {:status 400 :body "missing or bad WebSocket-Origin"})
         {:status 400 :body "missing or bad WebSocket-Key"})
       {:status 400 :body "not websocket protocol"})))
