(ns duct.reitit.util
  (:require [clojure.string :as str]
            [integrant.core :refer [init-key]]
            [jsonista.core :as jsonista]
            [duct.core :as duct]))

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

(defn resolve-registry
  "Resolve registry keys into a map of {k [resolve config]}"
  [namespaces registry]
  (letfn [(process [f] (reduce f {} registry))
          (resolve [k] (resolve-key namespaces k))]
    (process
     (fn [acc [k v]]
       (when-let [res (resolve k)]
         (assoc acc k [res (or v {})]))))))

(defn with-registry
  "Merge user registry integrant key with configuration in addition to other configs"
  [config registry & configs]
  (let [registry (reduce-kv (fn [m _ v] (assoc m (first v) (second v))) {} registry)]
    (apply duct/merge-configs
           (->> configs
                (cons config)
                (cons registry)))))

(defn get-namespaces
  "Get and merge namespaces using :duct.core/project-ns, :duct.core/handler-ns, and :duct.core/middleware-ns"
  [config]
  (let [root (config :duct.core/project-ns)
        nss (select-keys config [:duct.core/handler-ns :duct.core/middleware-ns])]
    (mapv (partial str root ".") (vals nss))))

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
        (into {} (filter #(and (seq %) ((comp not nil? second) %)) coll))))

(defn get-environment
  "Get environment from configuration"
  [config options]
  (:environment options (:duct.core/environment config :production)))

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

(defn try-resolve-sym
  "Attempts to resolve the given symbol ignoring any exceptions.
  Returns the resolved symbol if successful, otherwise `nil`.
  credit: @thiru"
  [sym]
  (try (requiring-resolve sym) (catch Throwable _)))

(defmacro spy
  "A simpler version of Timbre's spy, printing the original expression and the
  evaluated result. Returns the eval'd expression.
  credit: @thiru"
  [expr]
  `(let [evaled# ~expr]
     (println "SPY =>; ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;")
     (clojure.pprint/pprint '~expr)
     (clojure.pprint/pprint evaled#)
     (println ";;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;")
     evaled#))

(defn wrap-compile-middleware [f]
  (fn [opts _] (fn [handler] (fn [request] (f opts _ handler request)))))

(defmacro defm [name args body]
  `(let [fun# (fn ~args ~body)]
     (def ~name
       {:name ~(keyword (str *ns* "/" name))
        :compile (wrap-compile-middleware fun#)})))

(comment
  (test #'resolve-key)
  (test #'get-namespaces)
  (test #'compact)
  (test #'member?))
