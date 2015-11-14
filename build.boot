(set-env!
  :source-paths #{"src"}
  :dependencies '[[adzerk/bootlaces "0.1.11" :scope "test"]
                  [com.stuartsierra/component "0.3.0"]
                  [org.clojure/clojure "1.8.0-RC1" :scope "provided"]
                  [org.clojure/tools.namespace "0.3.0-alpha2"]
                  [prismatic/schema "1.0.3"]])

(require '[adzerk.bootlaces :refer :all])

(def +version+ "0.1.4-SNAPSHOT")
(bootlaces! +version+)

(task-options!
 pom  {:project     'ib5k/boot-component
       :version     +version+
       :description "Boot tasks for component systems"
       :url         "https://github.com/ib5k/boot-component"
       :scm         {:url "https://github.com/ib5k/boot-component"}
       :license     {"Eclipse Public License" "http://www.eclipse.org/legal/epl-v10.html"}})
