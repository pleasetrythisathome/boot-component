(set-env!
  :source-paths #{"src"}
  :dependencies '[[org.clojure/clojure "1.7.0-alpha4" :scope "provided"]
                  [adzerk/bootlaces "0.1.5" :scope "test"]])

(require '[adzerk.bootlaces :refer :all])

(def +version+ "0.1.0")
(bootlaces! +version+)

(task-options!
 pom  {:project     'boot-component
       :version     +version+
       :description "Boot tasks for component systems"
       :url         "https://github.com/pleasetrythisathome/boot-component"
       :scm         {:url "https://github.com/pleasetrythisathome/boot-component"}
       :license     {:name "Eclipse Public License"
                     :url  "http://www.eclipse.org/legal/epl-v10.html"}})
