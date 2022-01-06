(ns duct.reitit-test
  (:require [clojure.test :refer [deftest testing is are]]
            [duct.reitit]
            [foo.handler]
            [foo.handler.plus]
            [duct.core :as core]
            [integrant.core :as ig]
            [reitit.ring :as ring]
            [reitit.core :as r]
            [fipp.clojure :refer [pprint]]
            [duct.reitit.util :refer [to-edn spy]]
            [clojure.string :as str]))

(core/load-hierarchy)

(derive :duct.reitit/logging ::logging)
(derive :duct.reitit/coercion ::coercion)
(derive :duct.module/reitit ::module)
(derive :duct.router/reitit ::router)
(derive :foo/database :duct/const)
(derive :foo/index-path :duct/const)

(defn- new-config-handling [base & [profiles]]
  (let [config (core/build-config base profiles)
        in-config (partial get-in config)]
    #(in-config (cons :duct.reitit/options (if (vector? %) % [%])))))

(defn- request [method uri req]
  (merge req {:request-method method :uri uri}))

(defn- routes [router]
  (reduce (fn [acc [k v]] (assoc acc k v)) {} (r/routes router)))

(def base-config
  {:duct.module/reitit {}
   :duct.module/logging {}
   :duct.profile/base {:duct.core/project-ns 'foo}})

(def test-config
  {:duct.core/project-ns    'foo
   :duct.core/handler-ns    'handler ;; Where should handlers keys be localated
   :duct.core/middleware-ns 'middleware  ;; Where should middleware keys be located
   :duct.reitit/routes     [["/" :index] ;; Routes Configuration
                            ["/author" :get-author]
                            ["/ping" {:get {:handler :ping}}]
                            ["/plus" {:post :plus/with-body
                                      :get 'plus/with-query}]
                            ["/divide" {:get :divide}]]
   :duct.reitit/registry  {:index {:path  (ig/ref :foo/index-path)} ;; init foo.handler/index with {:path}
                           :ping  {:message "pong"} ;; init foo.handler/ping with {:message}
                           :plus/with-body {} ;; init foo.handler.plus/with-body
                           :get-author {} ;; init foo.handler/get-author
                           :divide {}}    ;; init foo.handler/divide
   :duct.reitit/exception   (ig/ref :foo.handler/exceptions)
   :duct.reitit/environment {:db (ig/ref :foo/database)}
   :foo/database            [{:author "tami5"}]
   :foo/index-path          "resources/index.html"
   :foo.handler/exceptions  {}})

(defn- with-base-config [config]
  (->> config
       (merge test-config)
       (assoc base-config :duct.profile/base)))

(defn- init
  "Takes reitit options and merge it to base-config for testing"
  [config]
  (-> config with-base-config core/prep-config ig/init))

(deftest test-default-base-options
  (let [config (core/prep-config base-config)
        in-options (new-config-handling base-config)]

      ;; Reitit Module keys used for futher processing
    (is (->> (keys config)
             (filterv #(= "duct.reitit" (namespace %)))
             (mapv #(keyword (name %)))
             (= [:options :registry :middleware :logging])))

    (are [key value] (= value (key config))
        ;; Defaulhandler middleware namespace
      :duct.core/handler-ns    'handler
        ;; Default middleware namespace
      :duct.core/middleware-ns 'middleware
        ;; Configuration Pass it reitit router to initialize it
      :duct.router/reitit      {:routes nil,
                                :middleware (ig/ref :duct.reitit/middleware)
                                :registry (ig/ref :duct.reitit/registry)
                                :options (ig/ref :duct.reitit/options)
                                :namespaces ["foo.handler" "foo.middleware"]}
        ;; Configuration Pass it ring handler to initialize it
      :duct.handler/root       {:router (ig/ref :duct.router/reitit)
                                :options (ig/ref :duct.reitit/options)})

      ;; Configuration Values
    (are [path value] (= (in-options path) value)
      [:logging :exceptions?] true ;; default types supported by default
      [:logging :pretty?] false      ;; No pretty logging by default.
      [:logging :logger] nil         ;; No logger by default.
      :muuntaja true                 ;; Muuntaja formatting is enabled by default
      :environment {}                ;; Empty Environment
      :middleware []                 ;; Empty Middleware
      :coercion nil)))               ;; no :coercion configuration

(deftest test-default-dev-options
  (let [in-options (-> (assoc base-config :duct.profile/dev {})
                       (new-config-handling [:duct.profile/dev]))]
    (are [path value] (= (in-options path) value)
      [:logging :exceptions?] true ;; default types supported by default
      [:logging :coercions?] true ;; default types supported by default
      [:logging :requests?] true ;; default types supported by default
      [:logging :pretty?] true         ;; pretty logging by default.
      [:logging :logger] nil           ;; No logger by default.
      :muuntaja true                   ;; Muuntaja formatting is enabled by default
      :environment {}                  ;; Empty Environment
      :middleware []                   ;; Empty Middleware
      [:cross-origin :methods] [:get :post :delete :options])  ;; Cross-origin methods
    (is (= ".*" (str (in-options [:cross-origin :origin 0])))))) ;; Cross-origin origin allowed

(deftest test-default-prod-options
  (let [in-options (-> (assoc base-config :duct.profile/prod {})
                       (new-config-handling [:duct.profile/prod]))]
    (are [path value] (= (in-options path) value)
      [:logging :requests?] true     ;; default types supported by default
      [:logging :coercions?] false ;; default types supported by default
      [:logging :exceptions?] false ;; default types supported by default
      [:logging :pretty?] false     ;; No pretty logging by default.
      [:logging :logger] nil        ;; No logger by default.
      :muuntaja true                ;; Muuntaja formatting is enabled by default
      :environment {}               ;; Empty Environment
      :middleware []                ;; Empty Middleware
      :cross-origin nil)))        ;; No Cross-origin

(deftest test-foo-module
  (let [extra {::coercion {:enable true :coercer 'spec}}
        config (-> extra with-base-config core/prep-config ig/init)
        router (config :duct.router/reitit)
        routes (routes router)
        handler (config :duct.handler/root)]

    (testing "Registry Merge"
      (are [x] (not= nil (x config))
        :foo.handler/ping
        :foo.handler/index
        :foo.handler.plus/with-body))

    (testing "Resulting Router"
      (is (= :reitit.core/router (type router)))
      (is (= 5 (count routes)))
      (is (vector? (-> (get routes "/") :environment :db)))
      (are [route path] (fn? (get-in (r/match-by-path router route) path))
        "/"     [:data :handler]
        "/ping" [:data :get :handler]
        "/plus" [:data :get :handler]
        "/plus" [:data :post :handler]
        "/author" [:data :handler])

      (is (nil? (handler {:request-method :get :uri "/not-a-route"})))

      (are [method uri extra-req-params body-path val]
           (-> (request method uri extra-req-params) handler to-edn (get-in body-path) (= val) is)
        :get "/ping" {} [:message] "pong"
        :post "/plus" {:body-params  {:y 3 :x 6}} [:total] 9
        :get "/plus"  {:query-params {:y 3 :x 6}} [:total] 9
        :get "/author" {} [:author] "tami5"))

    (testing "Custom Error Handling Repsonse"
      (let [divide-by-zero-response (to-edn (handler (request :get "/divide" {:body-params {:y 0 :x 0}})))
            no-params-response (to-edn (handler (request :get "/divide" {})))]

        (is (= "Divide by zero" (:cause divide-by-zero-response)))
        (is (= {:y 0 :x 0} (:data divide-by-zero-response)))
        (is (= {:y 0 :x 0} (:data divide-by-zero-response)))
        (is (= "No parameters received" (:cause no-params-response)))))))

(deftest module-behavior
  (testing "Logging:"
    (let [does-include* (fn [ptr] (fn [str] (str/includes? str ptr)))
          spec-pretty?  (does-include* "-- Spec failed --------------------")
          spec-compact? (does-include* "-- Spec failed --------------------")
          ex-pretty?    (does-include* "-- Exception Thrown ----------------")
          ex-compact?   (does-include* "Exception: :uri")
          test-behavior (fn [method url extra-request-keys base cfg checkfn]
                          (let [request (request method url extra-request-keys)
                                handler (-> base (core/merge-configs cfg) init :duct.handler/root)]
                            (->> request handler with-out-str checkfn)))]

      (testing "Exception-Logging:"
        (let [base {::logging {:enable true :pretty? false :logger nil :exception? true}}]
          (are [checkfn cfg] (test-behavior :get "/divide" {:body-params {:y 0 :x 0}} base cfg checkfn)
            ;; Enabled
            ex-compact? {}
            ;; Enabled + logger (cant' check or confirm that)
            empty? {::logging {:logger (ig/ref :duct/logger)}}
            ;; Enabled + Pretty
            ex-pretty? {::logging {:pretty? true}}
            ;; Disabled
            empty? {::logging {:enable false}}
            ;; Disabled through disabling
            empty? {::logging {:exceptions? false}})))

      (testing "Coercion-Logging:"
        (let [base {::coercion {:enable true :coercer 'spec}
                    ::logging  {:enable true :pretty? false :coercions? true}}]
          (are [checkfn cfg] (test-behavior :get "/plus" {:query-params {:y "str" :x 0}} base cfg checkfn)
            ;; Enabled
            spec-compact? {}
            ;; Enabled + logger (cant' check or confirm that)
            empty? {::logging {:logger (ig/ref :duct/logger)}}
            ;; Enabled + Pretty
            spec-pretty? {::logging {:pretty? true}}
            ;; Disabled Logging
            empty? {::logging {:enable false}}
            ;; Disabled Coercion Logging, loggs with exceptions handler instead
            ex-pretty? {::logging {:pretty? true :exceptions? true :coercions? false}}))))))

