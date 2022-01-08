(ns duct.reitit-test
  (:require [clojure.test :refer [are deftest is testing]]
            [duct.core :as core]
            [duct.reitit]
            [foo.handler]
            [foo.handler.plus]
            [integrant.core :as ig]
            [duct.test-helpers :refer [base-config init-system request with-base-config test-options routes]]
            [reitit.core :as r]
            [duct.reitit.util :refer [to-edn spy]]
            [clojure.string :as str]))

(core/load-hierarchy)

(deftest test-default-config
  (let [config (core/prep-config base-config)]
    (is (= (->> (keys config)
                (filterv #(= "duct.reitit" (namespace %)))
                (mapv #(keyword (name %))))
           [:options :registry :log :routes :handler :router]))

    (are [key value] (= (key config) value)
      ;; Default middleware namespace
      :duct.core/handler-ns    'handler
      ;; Default middleware namespace
      :duct.core/middleware-ns 'middleware
      ;; Log function
      :duct.reitit/log    (ig/ref :duct.reitit/options)
      ;; Resolve routes
      :duct.reitit/routes   {:routes nil
                             :namespaces ["foo.handler" "foo.middleware"]
                             :registry (ig/ref :duct.reitit/registry)}
      ;; Configuration Pass it reitit router to initialize it
      :duct.reitit/router    {:routes (ig/ref :duct.reitit/routes)
                              :options (ig/ref :duct.reitit/options)
                              :log (ig/ref :duct.reitit/log)}
      ;; Configuration Pass it ring handler to initialize it
      :duct.reitit/handler    {:router (ig/ref :duct.reitit/router)
                               :options (ig/ref :duct.reitit/options)
                               :log (ig/ref :duct.reitit/log)})))

(deftest test-default-base-options
  ;; Reitit Module keys using default base default options only
  (let [get-in-options (test-options base-config)]
    (are [path value] (= (get-in-options path) value)
      :muuntaja true                 ;; Muuntaja formatting is enabled by default
      :environment {}                ;; Empty Environment
      :middleware []                 ;; Empty Middleware
      :coercion nil                  ;; no :coercion configuration
      [:logging :exceptions?] true   ;; default types supported by default
      [:logging :pretty?] false      ;; No pretty logging by default.
      [:logging :logger] nil)))      ;; No logger by default.

(deftest test-default-dev-options
  ;; Reitit Module keys using default base + default dev options
  (let [get-in-options (-> (assoc base-config :duct.profile/dev {})
                           (test-options [:duct.profile/dev]))]
    (are [path value] (= (get-in-options path) value)
      [:logging :exceptions?] true     ;; exceptions enabled by default
      [:logging :coercions?] true      ;; coercions enabled by default
      [:logging :requests?] true       ;; requests enabled by default
      [:logging :pretty?] true         ;; pretty logging by default.
      [:logging :logger] nil           ;; No logger by default.
      :muuntaja true                   ;; Muuntaja formatting is enabled by default
      :environment {}                  ;; Empty Environment
      :middleware []                   ;; Empty Middleware
      [:cross-origin :methods] [:get :post :delete :options])  ;; Cross-origin methods
    (is (= ".*" (str (get-in-options [:cross-origin :origin 0])))))) ;; Cross-origin origin allowed

(deftest test-default-prod-options
  (let [get-in-options (-> (assoc base-config :duct.profile/prod {})
                           (test-options [:duct.profile/prod]))]
    (are [path value] (= (get-in-options path) value)
      :muuntaja true                ;; Muuntaja formatting is enabled by default
      :environment {}               ;; Empty Environment
      :middleware []                ;; Empty Middleware
      :cross-origin nil             ;; No Cross-origin
      [:logging :requests?] true    ;; default types supported by default
      [:logging :coercions?] false  ;; default types supported by default
      [:logging :exceptions?] false ;; default types supported by default
      [:logging :pretty?] false     ;; No pretty logging by default.
      [:logging :logger] nil)))        ;; No logger by default.

(deftest test-foo-module
  (let [extra {:duct.reitit/coercion {:enable true :coercer 'spec}}
        config (-> extra with-base-config core/prep-config ig/init)
        router (config :duct.reitit/router)
        routes (routes router)
        handler (config :duct.reitit/handler)]

    (testing "Registry-Merge:"
      (are [x] (not= nil (x config))
        :foo.handler/ping
        :foo.handler/index
        :foo.handler.plus/with-body))

    (testing "Resulting-Router:"
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

    (testing "Custom-Error-Repsonse:"
      (let [divide-by-zero-response (to-edn (handler (request :get "/divide" {:body-params {:y 0 :x 0}})))
            no-params-response (to-edn (handler (request :get "/divide" {})))]

        (is (= "Divide by zero" (:cause divide-by-zero-response)))
        (is (= {:y 0 :x 0} (:data divide-by-zero-response)))
        (is (= {:y 0 :x 0} (:data divide-by-zero-response)))
        (is (= "No parameters received" (:cause no-params-response)))))))

(deftest test-logging-behavior
  (testing "Logging:"
    (let [does-include* (fn [ptr] (fn [str] (str/includes? str ptr)))
          spec-pretty?  (does-include* "-- Spec failed --------------------")
          spec-compact? (does-include* "-- Spec failed --------------------")
          ex-pretty?    (does-include* "-- Exception Thrown ----------------")
          ex-compact?   (does-include* "Exception: :uri")
          test-behavior (fn [method url extra-request-keys base cfg checkfn]
                          (let [request (request method url extra-request-keys)
                                handler (-> base (core/merge-configs cfg) with-base-config init-system :duct.reitit/handler)]
                            (->> request handler with-out-str checkfn)))]

      (testing "Exception-Logging:"
        (let [base {:duct.reitit/logging {:enable true :pretty? false :exception? true}}]
          (are [checkfn cfg] (test-behavior :get "/divide" {:body-params {:y 0 :x 0}} base cfg checkfn)
            ;; Enabled
            ex-compact? {}
            ;; Enabled + logger (cant' check or confirm that)
            empty? {:duct.reitit/logging {:logger (ig/ref :duct/logger)}}
            ;; Enabled + Pretty
            ex-pretty? {:duct.reitit/logging {:pretty? true}}
            ;; Disabled
            empty? {:duct.reitit/logging {:enable false}}
            ;; Disabled through disabling
            empty? {:duct.reitit/logging {:exceptions? false}})))

      (testing "Coercion-Logging:"
        (let [base {:duct.reitit/coercion {:enable true :coercer 'spec}
                    :duct.reitit/logging  {:enable true :pretty? false :coercions? true}}]
          (are [checkfn cfg] (test-behavior :get "/plus" {:query-params {:y "str" :x 0}} base cfg checkfn)
            ;; Enabled
            spec-compact? {}
            ;; Enabled + logger (cant' check or confirm that)
            empty? {:duct.reitit/logging {:logger (ig/ref :duct/logger)}}
            ;; Enabled + Pretty
            spec-pretty? {:duct.reitit/logging {:pretty? true}}
            ;; Disabled Logging
            empty? {:duct.reitit/logging {:enable false}}
            ;; Disabled Coercion Logging, loggs with exceptions handler instead
            ex-pretty? {:duct.reitit/logging {:pretty? true :exceptions? true :coercions? false}}))))))

