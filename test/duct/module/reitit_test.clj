(ns duct.module.reitit-test
  (:require [clojure.test :refer [are deftest is testing]]
            [duct.core :as core]
            [duct.module.logging]
            [duct.module.reitit]
            [foo.handler]
            [foo.handler.plus]
            [integrant.core :as ig]
            [duct.test-helpers :refer [base-config init-system request with-base-config test-options routes]]
            [reitit.core :as r]
            [duct.reitit.util :refer [to-edn spy]]
            [clojure.string :as str]
            [medley.core :refer [dissoc-in]]))

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
      [:logging :exceptions?] true   ;; default types supported by default
      [:logging :pretty?] false      ;; No pretty logging by default.
      [:logging :logger] nil      ;; No logger by default.
      :coercion {:with-formatted-message? true})))

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
      [:cross-origin :methods] [:get :post :delete :options]  ;; Cross-origin methods
      :coercion {:with-formatted-message? true})
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
      [:logging :logger] nil        ;; No logger by default.
      :coercion {:with-formatted-message? true})))

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

(deftest test-auto-exception-handlers-detection
  (let [system (->> [:duct.profile/base :duct.reitit/exception]
                    (dissoc-in (with-base-config {}))
                    (init-system))]
    (is (get-in system [:duct.reitit/options :exception]))))

(deftest test-pretty-coercion-response
  (let [system (->> {:duct.reitit/muuntaja false
                     :duct.reitit/coercion
                     {:with-formatted-message? true
                      :enable true
                      :coercer 'spec}}
                    with-base-config
                    init-system)
        handler (:duct.reitit/handler system)]
    (is (-> {:request-method :get :uri "/plus" :query-params {:y "str" :x 5}}
            (handler)
            (:message)
            (str/includes? "-- Spec failed")))))

(deftest test-cors-middleware
  (let [system (->> {:duct.reitit/muuntaja false
                     :duct.reitit/cross-origin {:methods [:get :post] :origin [#"http://example.com"]}}
                    with-base-config
                    init-system)
        handler (:duct.reitit/handler system)]
    (is (= (handler
            {:request-method :options
             :uri "/author"
             :headers {"origin" "http://example.com"
                       "access-control-request-method" "POST"
                       "access-control-request-headers" "Accept, Content-Type"}})
           {:status 200,
            :headers {"Access-Control-Allow-Methods" "GET, POST",
                      "Access-Control-Allow-Origin" "http://example.com",
                      "Access-Control-Allow-Headers" "Accept, Content-Type"},
            :body "preflight complete"}))

    (is (not= (-> {:request-method :options
                   :uri "/author"
                   :headers {"origin" "https://go.vm"}}
                  (handler)
                  (:body))
              "preflight complete"))))

(defn- req-with-cfg [{:keys [req-opts config with-str? testfn]}]
  (let [request (apply request req-opts)
        handler (-> config with-base-config init-system :duct.reitit/handler)]
    (cond
      (and with-str? testfn)
      (->> request handler with-out-str testfn)
      (true? with-str?)
      (->> request handler with-out-str)
      (fn? testfn)
      (->> request handler testfn)
      :else (->> request handler))))

(defn does-include [ptr]
  (fn [str] (str/includes? str ptr)))

(def ex-pretty?    (does-include "-- Exception Thrown "))
(def ex-compact?   (does-include "Exception: :uri"))
(defn cmerge [b e] (core/merge-configs b e))

(deftest test-exception-logging
  (let [base {:duct.reitit/logging {:enable true :pretty? false :exception? true}}
        req-opts [:get "/divide" {:body-params {:y 0 :x 0}}]]
    (are [testfn cfg] (req-with-cfg
                       {:req-opts req-opts :config (cmerge base cfg) :tesfn testfn :with-str? true})
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

(deftest test-coercion-logging-spec
  (let [base {:duct.reitit/coercion {:enable true :coercer 'spec}
              :duct.reitit/logging  {:enable true :pretty? false :coercions? true}}
        req-opts [:get "/plus" {:query-params {:y "str" :x 0}}]
        spec-pretty?  (does-include "-- Spec failed")
        spec-compact? (does-include "Spec Coercion Error")]

    (are [testfn cfg] (req-with-cfg {:req-opts req-opts :config (cmerge base cfg) :testfn testfn :with-str? true})
      ;; Enabled
      spec-compact? {}
      ;; Enabled + logger (cant' check or confirm that)
      empty? {:duct.reitit/logging {:logger (ig/ref :duct/logger)}}
      ;; Enabled + Pretty
      spec-pretty? {:duct.reitit/logging {:pretty? true}}
     ;; Disabled Logging
      empty? {:duct.reitit/logging {:enable false}}
      ;; Disabled Coercion Logging, loggs with exceptions handler instead
      ex-pretty? {:duct.reitit/logging {:pretty? true :exceptions? true :coercions? false}})))

(deftest test-coercion-logging-malli
  (let [req-opts [:get "/plus" {:query-params {:y "str"}}]
        handler {:parameters {:query [:map [:x :int] [:y :int]]}
                 :handler (fn [{{{:keys [x y]} :query} :parameters}]
                            {:status 200 :body {:total (+ x y)}})}
        base {:duct.reitit/coercion {:enable true :coercer 'malli}
              :duct.reitit/logging  {:enable true :pretty? false :coercions? true}
              :duct.reitit/routes   [["/plus" {:get handler}]]}
        malli-pretty?   (does-include "2 Errors detected:")
        malli-compact?  (does-include "Malli Coercion Error")]

    (are [testfn cfg] (req-with-cfg {:req-opts req-opts :config (cmerge base cfg) :testfn testfn :with-str? true})
      ;; Enabled
      malli-compact? {}
      ;; Enabled + logger (cant' check or confirm that)
      empty? {:duct.reitit/logging {:logger (ig/ref :duct/logger)}}
      ;; Enabled + Pretty
      malli-pretty? {:duct.reitit/logging {:pretty? true}}
      ;; Disabled Logging
      empty? {:duct.reitit/logging {:enable false}}
      ;; Disabled Coercion Logging, loggs with exceptions handler instead
      ex-pretty? {:duct.reitit/logging {:pretty? true :exceptions? true :coercions? false}})

    (req-with-cfg {:req-opts req-opts :config base})))

(deftest test-request-logging
  (let [base {:duct.reitit/logging
              {:exceptions? false :coercions? false :requests? true :level :report}}
        data-format    (does-include "[:starting {:method")
        pretty-format? (does-include "Starting Request")

        req-opts [:get "/divide" {:body-params {:y 2 :x 2}}]]

    (are [testfn cfg] (req-with-cfg
                       {:req-opts req-opts :config (cmerge base cfg) :with-str? true :testfn testfn})
      data-format {}
      pretty-format? {:duct.reitit/logging {:pretty? true}}
      pretty-format? {:duct.reitit/logging {:pretty? true :logger (ig/ref :duct/logger)}}
      empty? {:duct.reitit/logging {:requests? false}})))

    ; (request-with-config
    ;  {:req-opts [:get "/divide" {:body-params {:y 2 :x 2}}]
    ;   :config (core/merge-configs base #_{:duct.reitit/logging {:logger (ig/ref :duct/logger)}})})))
