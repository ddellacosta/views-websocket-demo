(ns http-kit-view-demo.lib.exceptions
  (:require
   [clojure.stacktrace :refer [print-stack-trace]]))

;; not sure this needs a namespace of its own?
(defn exception-info
  "Produce a string of exception information."
  [e]
  (str
    "e: "      e
    " msg: "   (.getMessage e)
    " trace: " (with-out-str (print-stack-trace e))))
