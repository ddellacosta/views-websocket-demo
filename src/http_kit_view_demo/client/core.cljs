(ns http-kit-view-demo.client.core
  (:require-macros
   [cljs.core.async.macros :refer [go]])
  (:require
   [cljs.core.async :refer [chan sub <! map< put!]]
   [om.core :as om]
   [om-tools.core :refer-macros [defcomponentk]]
   [om-tools.mixin :refer-macros [defmixin]]
   [sablono.core :as html :refer-macros [html]]
   [http-kit-view-demo.client-lib.websockets :refer [ws-chans send-transit-msg! receive-transit-msg-fn! views-ws-url make-websocket!]]))

(defcomponentk app
  [data owner opts]
  (render-state [_ state]
    (.log js/console (pr-str data))
    (html
     [:div.main
      [:div.messages {:style {:width "50%" :display "table-cell" :valign "top"}}
       [:h2 "Chat!!"]
        [:div.input {:style #js {:marginTop "12px"}}
         [:input#msg {:type      "text"
                      :on-change #(om/set-state! owner :msg (.. % -target -value))
                      :on-key-up #(when (= 13 (.-keyCode %))
                                    (send-transit-msg! (get @ws-chans "views") (:msg state))
                                    (om/set-state! owner :msg ""))
                      :value     (:msg state)}]
         [:button {:on-click #(send-transit-msg! (get @ws-chans "views") (:msg state))}
          "Send message"]]
        [:div.comments
         (map
          #(into [:div.comment {:style #js {:borderBottom "1px solid black" :marginRight "3px" :padding "5px"}}] %)
          (:comments data))]]
      [:div.todos {:style {:display "table-cell"}}
       [:h2 "TODOS!"]
       [:ul
        (for [i (:todos data)]
         [:li (:name i)])]]
      ]))) ; :div.main

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
              (om/update! data [(ffirst msg)] (second msg)))))))
  (render-state [_ state]
    (app-root data {:opts opts :state state})))

(defn init!
  []
  (om/root view-wrapper {} {:opts {:app-root ->app} :target (.getElementById js/document "app-root")}))

(set! (.-onload js/window) init!)
