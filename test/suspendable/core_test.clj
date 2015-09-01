(ns suspendable.core-test
  (:require [clojure.test :refer :all]
            [suspendable.core :refer :all]
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

(defrecord StatefulComponent [state]
  component/Lifecycle
  (start [component] (reset! state :started) component)
  (stop [component] (reset! state :stopped) component))

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
  (let [system (component/system-map
                :plain       (->PlainComponent)
                :suspendable (->SuspendableComponent)
                :stateful    (->StatefulComponent (atom nil)))]
    (is (= (-> (resume-system system system) :plain :state)
           :started))
    (is (= (-> (resume-system system system) :suspendable :state)
           :resumed))
    (is (= (-> (resume-system system (dissoc system :suspendable)) :suspendable :state)
           :started))
    (let [system (component/start system)]
      (is (= (-> system :stateful :state deref) :started))
      (resume-system (dissoc system :stateful) system)
      (is (= (-> system :stateful :state deref) :stopped)))))
