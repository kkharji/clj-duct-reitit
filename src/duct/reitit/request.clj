(ns duct.reitit.request
  (:require [duct.reitit.util :refer [compact]]
            [clojure.string :as str]))

(defn params
  "Given a request, return a map with avaiable params based on type"
  [{:keys [body-params form-params query-params path-params]}]
  (compact
   {:body (not-empty body-params)
    :form (not-empty form-params)
    :query (not-empty query-params)
    :path (not-empty path-params)}))

(def ^:private info-key-title
  {:request-method "Request Method"
   :uri "Request URI"
   :params "Request Params"
   :start-date "Request Date"})

(defn- get-info-key-title
  "Get title from info-key-title or create new one"
  [key]
  (or (info-key-title key)
      (->> (str/split (name key) #"-")
           (mapv str/capitalize)
           (str/join " "))))

(defn- get-info-key-value
  "Get request key value."
  [request key]
  (case key
    :params (params request)
    :request-method (-> key request name str/upper-case)
    :start-date (-> key request str)
    (request key)))

(defn info
  "Return formatted description of a given request"
  ([request] (info request [:start-date :request-method :uri :params]))
  ([request request-keys]
   (->> request-keys
        (mapv #(when-let [value (get-info-key-value request %)]
                 (str (get-info-key-title %) ": " (pr-str value))))
        (compact)
        (str/join "\n"))))

