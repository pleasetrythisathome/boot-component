(ns boot-component.reloaded
  {:boot/export-tasks true}
  (:require [boot.core :as boot :refer [deftask]]
            [boot.from.backtick :refer [template]]
            [boot.pod :as pod]
            [boot.util :as util]
            [clojure.java.io :as io]
            [clojure.tools.namespace.repl :refer [disable-reload! refresh set-refresh-dirs]]
            [com.stuartsierra.component :as component]
            [tangrammer.component.co-dependency :as co-dependency]))

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
  (alter-var-root #'system co-dependency/start-system)
  :started)

(defn stop []
  (alter-var-root #'system stop-system)
  :stopped)

(defn go []
  (init)
  (start))

(defn clear []
  (let [error (atom nil)]
    (alter-var-root #'system (fn [system]
                               (try (stop-system system)
                                    (catch Throwable e
                                      (reset! error e)))
                               nil))
    (some-> error deref throw))
  :ok)

(defn reset []
  (clear)
  (refresh :after 'boot-component.reloaded/go))

(deftask reload-system
  ""
  [s system-var SYM sym "The var of the function that returns the component system"]
  (let [ns-sym (symbol (namespace system-var))]
    (boot/cleanup
     (stop))
    (util/info "reload-system initializer: %s\n" system-var)
    (comp
     (boot/with-pre-wrap fileset
       (->> (boot/get-env)
            ((juxt :source-paths :directories))
            (reduce into)
            (apply set-refresh-dirs))
       (set-init! (fn []
                    (require ns-sym)
                    ((ns-resolve ns-sym system-var))))
       fileset))))

(def ^:private deps
  (delay (remove pod/dependency-loaded? '[[quile/component-cljs "0.2.2"]])))

(defn- write-cljs! [file system-var]
  (util/info "Writing %s...\n" (.getName file))
  (util/info "reload-system-cljs initializer: %s\n" system-var)
  (->> (template
        ((ns boot-component.reloaded
           (:require [quile.component :as component]
                     ~@[(symbol (namespace system-var))]))

         (defonce system (atom nil))

         (defn- stop-system [s]
           (when s (component/stop s)))

         (defn init []
           (swap! system #(do (stop-system %) (~system-var)))
           :ok)

         (defn start []
           (swap! system component/start-system)
           :started)

         (defn stop []
           (swap! system stop-system)
           :stopped)

         (defn clear []
           (let [error (atom nil)]
             (swap! system (fn [system]
                             (try (stop-system system)
                                  (catch js/Error e
                                    (reset! error e)))
                             nil))
             (some-> error deref throw))
           :ok)

         (defn try-throw-cause
           [f]
           (try (f)
                (catch ExceptionInfo e
                  (.log js/console e)
                  (if-let [c (ex-cause e)]
                    (throw c)
                    (throw e)))))

         (defn go
           []
           (init)
           (try-throw-cause #(start))
           :ok)

         (defn reset []
           (clear)
           (go))
         ))
       (map pr-str) (interpose "\n") (apply str) (spit file)))

(defn- add-init!
  [in-file out-file]
  (let [ns 'boot-component.reloaded
        spec (-> in-file slurp read-string)]
    (when (not= :nodejs (-> spec :compiler-options :target))
      (util/info "Adding :require %s to %s...\n" ns (.getName in-file))
      (io/make-parents out-file)
      (-> spec
          (update-in [:require] conj ns)
          pr-str
          ((partial spit out-file))))))

(deftask reload-system-cljs
  ""
  [s system-var SYM sym "The var of the function that returns the component system"]
  (let [src  (boot/temp-dir!)
        tmp  (boot/temp-dir!)
        out  (doto (io/file src "boot_component" "reloaded.cljs") io/make-parents)]
    (boot/set-env! :source-paths #(conj % (.getPath src))
                   :dependencies #(into % (vec (seq @deps))))
    (write-cljs! out system-var)
    (comp
     (boot/with-pre-wrap fileset
       (doseq [f (->> fileset boot/input-files (boot/by-ext [".cljs.edn"]))]
         (let [path     (boot/tmppath f)
               in-file  (boot/tmpfile f)
               out-file (io/file tmp path)]
           (add-init! in-file out-file)))
       (-> fileset (boot/add-resource tmp) boot/commit!)))))
