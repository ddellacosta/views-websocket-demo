(ns http-kit-view-demo.core
  (:require
   [org.httpkit.server :refer [send! with-channel on-close on-receive run-server websocket?]]
   [http-kit-view-demo.websocket-subprotocol :as ws-subprotocol :refer [with-subprotocol-channel]]
   [ring.util.response :refer [resource-response content-type]]
   [ring.middleware.logger :as logger]
   [ring.middleware.resource :refer [wrap-resource]]
   [ring.middleware.content-type :refer [wrap-content-type]]
   ;; [ring.middleware.session :refer [wrap-session]]
   [ring.middleware.session.memory :refer [memory-store]]
   [compojure.route :as route]
   [compojure.handler :refer [site]]
   [compojure.core :refer [defroutes GET POST DELETE ANY context]]
   [clojure.tools.logging :refer [info]]))

(def session-store (atom {}))

(def channels (atom {}))

(defn close-handler
  [status]
  (println "channel closed: " status))

(defn receive-handler
  [channel data]
  ;;(send! channel data)
  (info "SESSION: " @session-store)
  (doseq [[k c] @channels] (send! c data)))

(defn default-handler [req]
  (with-subprotocol-channel req channel (re-pattern "http://localhost:8080") "default"
    (info "")
    (info "channel? " channel)
    (info "req? " req)
    (info "")
    (swap! channels assoc (get-in req [:headers "sec-websocket-key"]) channel)
    ;; communicate with client using method defined above
    (on-close channel close-handler)
    (on-receive channel #(receive-handler channel %))))

(defroutes all-routes
  (GET "/" [] (content-type (resource-response "templates/index.html") "Content-Type: Document"))
  (GET "/ws" [] handler)                      ;; websocket
  (route/not-found "<p>Page not found.</p>")) ;; all other, return 404

(defonce server (atom nil))

(defn stop-server! []
  (when-not (nil? @server)
    ;; graceful shutdown: wait 100ms for existing requests to be finished
    ;; :timeout is optional, when no timeout, stop immediately
    (@server :timeout 100)
    (reset! server nil)))

(defn run-app
  []
  ;; The #' is useful when you want to hot-reload code
  ;; You may want to take a look: https://github.com/clojure/tools.namespace
  ;; and http://http-kit.org/migration.html#reload
  (-> #'all-routes
      (site :store (memory-store session-store))
      (wrap-resource "static")
      wrap-content-type
      logger/wrap-with-logger))

(defn start-server! [&args]
  (reset! server (run-server (run-app) {:port 8080})))
