(set-env!
  :source-paths #{"src"}
  :dependencies '[[adzerk/bootlaces "0.1.11" :scope "test"]
                  [com.stuartsierra/component "0.2.3"]
                  [org.clojure/clojure "1.7.0-alpha5" :scope "provided"]
                  [org.clojure/tools.namespace "0.2.7"]
                  [juxt.modular/co-dependency "0.2.0"]])

(require '[adzerk.bootlaces :refer :all])

(def +version+ "0.1.1-SNAPSHOT")
(bootlaces! +version+)

(task-options!
 pom  {:project     'pleasetrythisathome/boot-component
       :version     +version+
       :description "Boot tasks for component systems"
       :url         "https://github.com/pleasetrythisathome/boot-component"
       :scm         {:url "https://github.com/pleasetrythisathome/boot-component"}
       :license     {"Eclipse Public License" "http://www.eclipse.org/legal/epl-v10.html"}})
