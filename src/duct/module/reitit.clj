(ns duct.module.reitit
  (:require [integrant.core :refer [init-key]]
            [duct.core :as duct]
            [integrant.core :as ig]
            [clojure.walk :as walk]
            [clojure.string :as str]))

(defn- try-to-resolve
  "Return a keyword, if str is valid integrant init-key or the value if str is valid symbol."
  [str]
  (cond (get-method ig/init-key (keyword str))
        (keyword str)
        (resolve (symbol str))
        (-> str symbol resolve var-get)
        :else nil))

(defn- key->qualified [key acc ns]
  (conj acc (if (str/includes? (str key) "/")
              (str ns "." (namespace key) "/" (name key))
              (str ns "/" (name key)))))

(defn- resolve-key
  "if key is valid integrant keyword, then return it,
   else key is a symbol or result to a symbol, then return it's value."
  {:test #(let [namespaces ['foo.handler 'foo.middleware]
                resolve (fn [k] (resolve-key k namespaces))]
            (assert (keyword? (resolve :ping)))
            (assert (keyword? (resolve :plus/with-body)))
            (assert (map? (resolve 'plus/with-query)))
            (assert (nil? (resolve :plus/with-email))))}
  [key namespaces]
  (let [res (->> namespaces
                 (reduce (partial key->qualified key) [])
                 (mapv try-to-resolve)
                 (remove nil?))]
    (if (< 1 (count res))
      (throw
       (-> "duct.module.reitit: found conflicting keyword/symbol names: "
           (str (pr-str res))
           (ex-info {:data res})))
      (first res))))

(defn resolve-registry
  "Given a registry"
  {:test #(let [reg [[:index {:path (ig/ref :index-path)}]
                     [:ping {:message "pong"}]
                     [:plus/with-body]]]
            (assert (= (resolve-registry reg ['foo.handler 'foo.middleware])
                       {:index [:foo.handler/index {:path (ig/ref :index-path)}]
                        :ping  [:foo.handler/ping {:message "pong"}]
                        :plus/with-body [:foo.handler.plus/with-body {}]})))}
  [registry namespaces]
  (reduce
   (fn [acc [k v]]
     (when-let [res (resolve-key k namespaces)]
       (assoc acc k [res (or v {})])))
   {}
   registry))

(comment
  (->> [#'resolve-key
        #'resolve-registry]
       (mapv test)))

(defmethod init-key :duct.module/reitit [_ {:keys [routes registry]}]
  (fn [{:duct.core/keys [project-ns handler-ns middleware-ns]
        :duct.module.reitit/keys [cors opts]
        :as config}]
    (let [namespaces [(or handler-ns 'handler)
                      (or middleware-ns 'middleware)]
          registry   (->> namespaces
                          (mapv #(str project-ns "." %))
                          (resolve-registry registry))
          rconfigs   (reduce-kv #(assoc %1 (first %3) (second %3)) {} registry)
          rkeys      (keys rconfigs)
          additions  (-> rconfigs
                         (assoc :duct.router/reitit
                                {:routes routes :registry registry}))]
      (duct/merge-configs config additions))))
