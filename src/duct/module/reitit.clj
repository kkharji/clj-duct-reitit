(ns duct.module.reitit
  (:require [integrant.core :refer [init-key] :as ig]
            [duct.core :as duct]
            [clojure.string :as str]))

(defn- qualify-key [key ns]
  (if (str/includes? (str key) "/")
    (str ns "." (namespace key) "/" (name key))
    (str ns "/" (name key))))

(defn- try-resolve [str]
  (cond (get-method init-key (keyword str))
        (keyword str)
        (resolve (symbol str))
        (var-get (resolve (symbol str)))))

(defn- resolve-key
  "if key is valid integrant keyword, then return it,
   elseif the key result to a symbol, return it's value."
  {:test #(let [namespaces ['foo.handler 'foo.middleware]
                resolve-key (fn [k] (resolve-key k namespaces))]
            (-> :ping resolve-key keyword? assert)
            (-> :plus/with-body resolve-key keyword? assert)
            (-> 'plus/with-query resolve-key map? assert)
            (-> :plus/with-email resolve-key nil? assert))}
  [key namespaces]
  (let [qualify #(conj %1 (qualify-key key %2))
        result (->> (reduce qualify [] namespaces)
                    (mapv try-resolve)
                    (remove nil?))]
    (if (second result)
      (throw
       (-> "duct.reitit: Found conflict detected: "
           (str (pr-str result))
           (ex-info {:data result})))
      (first result))))

(defn- resolve-registry
  "Given a registry"
  {:test #(-> [[:index {:path (ig/ref :index-path)}]
               [:ping {:message "pong"}]
               [:plus/with-body]]
              (resolve-registry '{:duct.core/project-ns foo
                                  :duct.core/handler-ns handler
                                  :duct.core/middleware-ns middleware})
              (= {:index [:foo.handler/index {:path (ig/ref :index-path)}]
                  :plus/with-body [:foo.handler.plus/with-body {}]
                  :ping  [:foo.handler/ping {:message "pong"}]})
              (assert))}
  [registry config]
  (let [{:duct.core/keys [project-ns middleware-ns handler-ns]} config
        to-path    (fn [ns] (str project-ns "." ns))
        namespaces (mapv to-path [middleware-ns handler-ns])
        collect    (fn [f] (reduce f {} registry))]
    (collect
     (fn [acc [k v]]
       (when-let [res (resolve-key k namespaces)]
         (assoc acc k [res (or v {})]))))))

(comment
  (test #'resolve-key)
  (test #'resolve-registry))

(def ^:private default-config
  {:duct.core/handler-ns 'handler-ns
   :duct.core/middleware-ns 'middleware-ns})

(defn- merge-with-defaults [config]
  (merge default-config config))

(defn- to-map [registry]
  (-> #(assoc %1 (first %3) (second %3))
      (reduce-kv {} registry)))

(defmethod init-key :duct.module/reitit [_ {:keys [routes registry]}]
  (fn [{:duct.module.reitit/keys [cors opts] :as config}]
    (let [config   (merge-with-defaults config)
          registry (resolve-registry registry config)
          extra    (to-map registry)
          router   {:routes routes :registry registry :opts opts}
          update   (-> extra
                       (assoc :duct.router/reitit router))]
      (duct/merge-configs config update))))
