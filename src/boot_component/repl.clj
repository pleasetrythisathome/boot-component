(ns boot-component.repl
  {:boot/export-tasks true}
  (:require
   [com.stuartsierra.component :as component]
   [clojure.tools.namespace.repl :refer [disable-reload! refresh set-refresh-dirs]]
   [boot.core :as core :refer [deftask]]
   [boot.pod :as pod]
   [boot.util :as util]))

(disable-reload!)

(def system nil)

(def ^:private initializer nil)

(defn set-init! [init]
  (alter-var-root #'initializer (constantly init)))

(defn- stop-system [s]
  (when s (component/stop s)))

(defn init []
  (if-let [init initializer]
    (do (alter-var-root #'system #(do (stop-system %) (init))) :ok)
    (throw (Error. "No system initializer function found."))))

(defn start []
  (alter-var-root #'system component/start)
  :started)

(defn stop []
  (alter-var-root #'system stop-system)
  :stopped)

(defn go []
  (init)
  (start))

(defn clear []
  (alter-var-root #'system #(do (stop-system %) nil))
  :ok)

(defn reset []
  (clear)
  (refresh :after 'boot-component.repl/go))

(deftask reload-system
  ""
  [s system-var SYM sym "The var of the function that returns the component system"]
  (let [sys-init system-var
        ns-sym (symbol (namespace sys-init))]
    (apply set-refresh-dirs (core/get-env :source-paths))
    (set-init! (fn []
                 (require ns-sym)
                 ((ns-resolve ns-sym sys-init))))
    identity))
