(ns http-kit-view-demo.lib.views.router
  (:require
    [views.core :refer [subscribe! unsubscribe! unsubscribe-all!]]
;    [de.web.views.filters :refer [authorized-subscription?]]
    [clojure.core.async :refer [go sub chan <! filter<]]
    [clojure.tools.logging :refer [debug error info]]
    [http-kit-view-demo.lib.exceptions :refer [exception-info]]))

;; Stubbing this out for now
(defn authorized-subscription?
  [sub view]
  true)

(defn req-firm-id
  [req]
  (get-in req [:session :firm :firm_id]))

(defn handle-subscriptions!
  [view-system subscriptions]
  (go (while true
        (try
          (let [sub (<! subscriptions)]
            (debug "Subscribing (in router): " sub)
            (doseq [view (:body sub)]
              (try
                (if (authorized-subscription? sub view)
                  (subscribe! view-system (req-firm-id sub) (first view) (rest view) (:session-id sub))
                  (debug "View subscription not authorized:" sub))
                (catch Exception e (error "when subscribing to view" view (exception-info e))))))
          (catch Exception e (error "when subscribing" (exception-info e)))))))

(defn handle-unsubscriptions!
  [view-system unsubscriptions]
  (go (while true
        (try
          (let [unsub (<! unsubscriptions)]
            (debug "Unsubscribing (in router): " unsub)
            (doseq [view (:body unsub)]
              (unsubscribe! view-system (req-firm-id unsub) (first view) (rest view) (:session-id unsub))))
          (catch Exception e (error "when unsubscribing" (exception-info e)))))))

(defn handle-disconnects!
  [view-system disconnects]
  (go (while true
        (try
          (let [disc (<! disconnects)]
            (debug "Disconnecting (in router): " disc)
            (unsubscribe-all! view-system (:session-id disc)))
          (catch Exception e (error "disconnect" (exception-info e)))))))

(defn init-views-router!
  [view-system client-chan]
  (let [subs        (chan 200)
        unsubs      (chan 200)
        control     (chan 200)
        disconnects (filter< #(= :disconnect (:body %)) control)]
    (sub client-chan :views.subscribe subs)
    (sub client-chan :views.unsubscribe unsubs)
    (sub client-chan :client-channel disconnects)
    (handle-subscriptions! view-system subs)
    (handle-unsubscriptions! view-system unsubs)
    (handle-disconnects! view-system disconnects)))
