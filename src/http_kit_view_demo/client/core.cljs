(ns http-kit-view-demo.client.core
  (:require
   [om.core :as om]
   [om-tools.core :refer-macros [defcomponentk]]
   [sablono.core :as html :refer-macros [html]]))

(defonce ws-chan (atom nil))

(defn init-websockets!
  [receive-handler]
  (let [ws-chan' (js/WebSocket. "ws://localhost:8080/ws", "default")]
    (set! (.-onmessage ws-chan') receive-handler)
    (reset! ws-chan ws-chan')))

(defn receive-message!
  [msg]
  (.log js/console (str "GOT MESSAGE! " (.-data msg))))

(defn send-message!
  [ws-chan msg]
;;  (.log js/console (str "MSG? " msg))
  (.send ws-chan msg))

(defcomponentk app
  [data owner opts]
  (render-state [_ state]
    (html
     [:div
      [:input#msg {:type      "text"
                   :on-change #(om/set-state! owner :msg (.. % -target -value))
                   :on-key-up #(when (= 13 (.-keyCode %))
                                 (send-message! @ws-chan (:msg state))
                                 (om/set-state! owner :msg ""))
                   :value     (:msg state)}]
      [:button {:on-click #(send-message! @ws-chan (:msg state))}
       "Send message!"]])))

(defn init!
  []
  (init-websockets! receive-message!)
  (om/root app {} {:target (.getElementById js/document "app-root")}))

(set! (.-onload js/window) init!)
