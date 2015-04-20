(ns http-kit-view-demo.client-lib.websockets
  (:require
   [cognitect.transit :as t]))

(defonce ws-chans (atom {}))

(def ws-url "ws://localhost:8080/ws")
(def views-ws-url "ws://localhost:8080/ws-views")

(defn default-receive-handler
  [msg]
  (.log js/console "RECEIVED MESSAGE: " msg))

(defn make-websocket!
  ([protocol]
     (make-websocket! ws-url protocol))
  ([url protocol]
     (make-websocket! ws-url protocol default-receive-handler))
  ([url protocol receive-handler]
     (let [ws-chan (js/WebSocket. url protocol)]
       (set! (.-onmessage ws-chan) receive-handler)
       (swap! ws-chans assoc protocol ws-chan))))

(def json-reader (t/reader :json))

(defn ab2str
  [buf]
  (reduce #(str %1 (.fromCharCode js/String %2)) "" (array-seq (js/Uint8Array. buf))))

(defn receive-transit-msg-fn!
  [update-fn]
  (fn [msg]
    (let [r (new js/FileReader)]
      (.readAsArrayBuffer r (.-data msg))
      (set! (.-onload r) #(update-fn (->> (.-result r) ab2str (t/read json-reader)))))))

(def json-writer (t/writer :json))

(defn send-transit-msg!
  [ws-chan msg]
  (.send ws-chan (t/write json-writer msg)))
