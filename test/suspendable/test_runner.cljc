(ns suspendable.test-runner
  (:require [clojure.test :as test]
            [suspendable.core-test]))

(defn -main [& args]
  (test/run-all-tests))
