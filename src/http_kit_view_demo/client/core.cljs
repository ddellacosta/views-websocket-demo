(ns http-kit-view-demo.client.core
  (:require-macros
   [cljs.core.async.macros :refer [go]])
  (:require
   [cljs.core.async :refer [chan sub <! map< put!]]
   [om.core :as om]
   [om-tools.core :refer-macros [defcomponentk]]
   [sablono.core :as html :refer-macros [html]]
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

(defcomponentk app
  [data owner opts]
  (render-state [_ state]
    (html
     [:div
      [:div.comments
       (map
        #(into [:span.comment {:style #js {:border "1px solid black" :marginRight "3px" :padding "5px"}}] %)
        (:comments data))]
      [:div.input {:style #js {:marginTop "12px"}}
       [:input#msg {:type      "text"
                    :on-change #(om/set-state! owner :msg (.. % -target -value))
                    :on-key-up #(when (= 13 (.-keyCode %))
                                  (send-transit-msg! (get @ws-chans "views") (:msg state))
                                  (om/set-state! owner :msg ""))
                    :value     (:msg state)}]
       [:button {:on-click #(send-transit-msg! (get @ws-chans "views") (:msg state))}
        "Send message"]]])))

(def view-msg-queue (atom nil))

(defn view-msg-chan
  [chan]
  (add-watch view-msg-queue :views #(put! chan %4)) ; new value
  chan)

(defcomponentk view-wrapper
  [data owner [:opts app-root :as opts]]
  (will-mount [_]
    (let [rfn     (receive-transit-msg-fn! #(reset! view-msg-queue %))
          ws      (make-websocket! views-ws-url "views" rfn)
          vm-chan (view-msg-chan (chan))]
      (go (while true
            (let [msg (<! vm-chan)]
              (.log js/console "GOT MSG: " (pr-str msg))
              (om/update! data [:comments] msg))))))
  (render-state [_ state]
    (app-root data {:opts opts :state state})))

(defn init!
  []
  (om/root view-wrapper {} {:opts {:app-root ->app} :target (.getElementById js/document "app-root")}))

(set! (.-onload js/window) init!)
