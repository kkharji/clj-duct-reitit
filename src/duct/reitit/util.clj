(ns duct.reitit.util
  (:require [clojure.string :as str]
            [integrant.core :refer [init-key]]
            [jsonista.core :as jsonista]))

(defn- qualify-key [key ns]
  (if (str/includes? (str key) "/")
    (str ns "." (namespace key) "/" (name key))
    (str ns "/" (name key))))

(defn- resolve-key* [str]
  (cond (get-method init-key (keyword str))
        (keyword str)
        (resolve (symbol str))
        (var-get (resolve (symbol str)))))

(defn resolve-key
  "if key is valid integrant keyword, then return it,
   elseif the key result to a symbol, return it's value."
  {:test
   #(let [namespaces ['foo.handler 'foo.middleware]
          resolve-key (partial resolve-key namespaces)
          check (fn [k t] (assert (t (resolve-key k))))]
      (check :ping keyword?)
      (check :plus/with-body keyword?)
      (check 'plus/with-query map?)
      (check :plus/with-email nil?))}
  [namespaces key]
  (let [qualify #(conj %1 (qualify-key key %2))
        result (->> (reduce qualify [] namespaces)
                    (mapv resolve-key*)
                    (remove nil?))]
    (if (second result)
      (throw
       (-> "duct.reitit: Confliction detected: "
           (str (pr-str result))
           (ex-info {:data result})))
      (first result))))

(defn get-namespaces
  {:test
   #(assert (= ["foo.handler" "foo.middleware"]
               (get-namespaces 'foo ['handler 'middleware])))}
  [root nss]
  (mapv (partial str root ".") nss))

(defn compact
  "Remove nils from a given coll (map, list, vector)"
  {:test
   #(do (assert (= [1 2] (compact [1 nil 2])))
        (assert (= {:y "1"} (compact {:x nil :y "1"})))
        (assert (= [1] (compact '(nil nil nil 1)))))}
  [coll]
  (cond (or (list? coll) (vector? coll))
        (into [] (filter (complement nil?) coll))
        (map? coll)
        (into {} (filter (comp not nil? second) coll))))

(defn member?
  "same as contains?, but check if v is part of coll."
  {:test
   #(do (assert (true? (member? [1 2 3] 1)))
        (assert (false? (member? [1 2 3] 6)))
        (assert (true? (member? ["a"] "a"))))}
  [coll v]
  (true? (some (fn [x] (= x v)) coll)))

(defn to-edn [response]
  (-> response
      (:body)
      (slurp)
      (jsonista/read-value jsonista/keyword-keys-object-mapper)))

(comment
  (test #'resolve-key)
  (test #'get-namespaces)
  (test #'compact)
  (test #'member?))


