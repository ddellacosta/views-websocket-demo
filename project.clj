(defproject http-kit-view-demo "0.1.0-SNAPSHOT"

  :description "Views Demo using http-kit/websockets"

  :url "http://example.com/FIXME"

  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}

  :dependencies [
                 [org.clojure/clojure "1.7.0-beta1"]

                 ;; Web
                 [http-kit "2.1.18"]
                 [ring "1.3.2"]
                 [ring.middleware.logger "0.5.0"]
                 [compojure "1.3.3"]

                 ;; Client
                 [org.clojure/clojurescript "0.0-3196"]
                 [org.omcljs/om "0.8.8"]
                 [views "1.4.0-SNAPSHOT" :exclusions [prismatic/plumbing]] ; assumes environ dependency!?

                 [prismatic/om-tools "0.3.11"]
                 [sablono "0.3.4"]

                 ;; Various Util
                 [org.clojure/core.async "0.1.346.0-17112a-alpha"]
                 [com.cognitect/transit-cljs "0.8.207"]
                 [com.cognitect/transit-clj "0.8.271"]
                 [environ "1.0.0"]
                 [prismatic/schema "0.4.0"]
                 [prismatic/plumbing "0.4.2"] ; ensure most current version for om-tools?

                 [views/honeysql "0.1.0-SNAPSHOT"]
                 [com.h2database/h2 "1.4.187"]
                 ]
               
  :main http-kit-view-demo.core               

  :plugins [[lein-cljsbuild "1.0.5"]
            [environ "1.0.0"]]

  :cljsbuild {
    :builds [{:id "dev"
              :source-paths ["src/http_kit_view_demo/client"]
              :compiler
              {:main          http-kit-view-demo.client.core
               :source-map    "resources/static/js/cljs/main.js.map"
               :output-to     "resources/static/js/cljs/main.js"
               :output-dir    "resources/static/js/cljs"
               :asset-path    "js/cljs"
               :optimizations :none
               :pretty-print  true}}]})
