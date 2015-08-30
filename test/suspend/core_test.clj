(ns suspend.core-test
  (:require [clojure.test :refer :all]
            [suspend.core :refer :all]
            [com.stuartsierra.component :as component]))

(defrecord PlainComponent []
  component/Lifecycle
  (start [component] (assoc component :state :started))
  (stop  [component] (assoc component :state :stopped)))

(defrecord SuspendableComponent []
  component/Lifecycle
  (start [component] (assoc component :state :started))
  (stop  [component] (assoc component :state :stopped))
  Suspendable
  (suspend [component]   (assoc component :state :suspended))
  (resume  [component _] (assoc component :state :resumed)))

(deftest test-default-suspend
  (is (= (-> (->PlainComponent) suspend :state) :stopped)))

(deftest test-default-resume
  (let [component (->PlainComponent)]
    (is (= (-> component (resume component) :state) :started))))

(deftest test-suspend-system
  (let [system (suspend-system
                (component/start
                 (component/system-map
                  :plain (->PlainComponent)
                  :suspendable (->SuspendableComponent))))]
    (is (= (-> system :plain :state) :stopped))
    (is (= (-> system :suspendable :state) :suspended))))

(deftest test-resume-system
  (let [system  (component/system-map
                 :plain (->PlainComponent)
                 :suspendable (->SuspendableComponent))
        system' (resume-system system system)]
    (is (= (-> system' :plain :state) :started))
    (is (= (-> system' :suspendable :state) :resumed))))
