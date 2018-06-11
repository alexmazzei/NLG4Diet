(defproject nlg-for-diet "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :repositories [["mvn_local_repo" "file:/Users/mazzei/Applications/libs/mvn_local_repo/"]]
  :dependencies [[org.clojure/clojure "1.8.0"]
                 [org.clojure/math.combinatorics "0.1.4"]
                 [random-seed "1.0.0"]
                 [simplenlg-it "1.0.0"] ]
  :main ^:skip-aot nlg-for-diet.core
  :target-path "target/%s"
  :profiles {:uberjar {:aot :all}})
