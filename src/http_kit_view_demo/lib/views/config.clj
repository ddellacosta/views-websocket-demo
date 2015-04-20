(ns http-kit-view-demo.lib.views.config
  (:require
   [views.core :refer [update-watcher! log-statistics! add-hint! refresh-views! hint]]
   [views.protocols :refer [IView id data relevant?]]
   [clojure.tools.logging :refer [debug error info]]
   [http-kit-view-demo.lib.websockets :refer [send-transit!]]))

(defonce subscribers (atom {}))

(defn websockets-send-fn!
  [sk data]
  (info "sending to:" sk "data:" (last data))
  (send-transit! (get @subscribers sk) (last data)))
  ;;(send! (get @subscribers sk) (pr-str (last data))))

(defonce memory-store
  (atom {:db1 {:comments []}
         :db2 {:comments []}}))

(defrecord MemoryView [id ks]
  IView
  (id [_] id)
  (data [_ namespace parameters]
    (get-in @memory-store (-> [namespace] (into ks) (into parameters))))
  (relevant? [_ namespace parameters hints]
    (some #(and (= namespace (:namespace %)) (= ks (:hint %))) hints)))

(defonce view-config
  (atom
   {:views   {:comments (MemoryView. :comments [:comments])}
    :send-fn websockets-send-fn!}))

;; Our update function, very basic
(defn update-memory-store!
  [ns view-id value]
  (swap! memory-store update-in (-> [ns] (into view-id)) conj value)
  (println view-id value)
  (refresh-views! view-config [(hint ns view-id)]))

(defn init-views!
  ([view-config] (init-views! view-config -1 20))
  ([view-config threads] (init-views! view-config -1 threads))
  ([view-config stats-interval threads]
     (update-watcher! view-config 5000 threads)
     (when (pos? stats-interval)
       (info "view statistics enabled with interval" stats-interval "msecs")
       (log-statistics! view-config stats-interval))))
