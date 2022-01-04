(ns duct.reitit.middleware-test
  (:require [duct.reitit.middleware]
            [clojure.test :refer [is are deftest testing]]
            [integrant.core :as ig :refer [init-key]]
            [reitit.ring :as ring]
            [duct.reitit.util :refer [compact spy]]
            [reitit.coercion.spec :as coercion.spec]
            [clojure.string :as str])
  (:import [clojure.lang PersistentArrayMap]
           [java.util UUID]
           [java.util Date]
           [java.lang String]))

(defn- new-router [routes config]
  (ring/router routes {:data config}))

(defn- new-middleware [cfg]
  (init-key :duct.reitit/middleware cfg))

(defn- count-result [res]
  (-> res :reitit.core/match :result compact count))

(deftest default-middleware-behavior
  (let [middleware (new-middleware {:munntaja false :coercion nil})
        routes     [["/identity" identity]
                    ["/identity-get" {:get identity}]
                    ["/user/:id" {:get identity}]
                    ["/err" #(throw (ex-info "error" {}))]]
        router     (new-router routes {:middleware middleware :environment {:name "tami5"}})
        app        (ring/ring-handler router)]

    (testing "Match result"
      (are [m req c] (= c (count-result (app (assoc req :request-method m))))
        :get {:uri "/identity"} 9            ;; Creates the same handler for all http methods.
        :get {:uri "/identity-get"} 2))      ;; Creates get and options handler

    (testing "Request Keys"
      (let [request (app {:request-method :get :uri "/identity"})]
        (is (= [:form-params                 ;; ..
                :id                          ;; Request id [environment-middleware]
                :name                        ;; injected key [environment-middleware]
                :params                      ;; Merge of all types of params. NOTE: Doesn't seem accurate.
                :path-params                 ;; ..
                :query-params                ;; ..
                :request-method              ;; Request method
                :start-date                  ;; Request Start date [environment-middleware]
                :uri                         ;; ..
                :reitit.core/match           ;; Matches
                :reitit.core/router]         ;; Reitit Router
               (sort (keys request))))
        (are [path expected-type] (= expected-type (type (get-in request path)))
          [:id] UUID                         ;; Random UUID to the request injected by [environment-middleware]
          [:start-date] Date                 ;; Start date injected by [environment-middleware]
          [:name] String                     ;; injected key by [environment-middleware]
          [:params] PersistentArrayMap)))))  ;; A marge of all params types injected by [parameters-middleware]
(defn is-int [str] (try (Integer/parseInt str) (catch Exception _ nil)))

(deftest coercion-middleware-behavior
  (let [middleware (new-middleware {:munntaja false :coercion {}})
        base {:get {:coercion coercion.spec/coercion
                    :parameters {:path {:company #(and (string? %)
                                                       (not (is-int %)))
                                        :user-id int?}}
                    :handler identity}}
        routes [["/identity/:company/users/:user-id" base]
                ["/:company/users/:user-id"
                 (-> base
                     (assoc-in [:get :response]
                               {200 {:body #(and (string? %) (str/includes? % "github"))}})
                     (assoc-in [:get :handler]
                               (fn [{{:keys [db]}  :environment
                                     {{:keys [user-id company]} :path} :parameters}]
                                 {:status 200 :body (format "%s works at %s" (get-in db [:users user-id :user-name]) company)})))]]
        environment {:db {:companies {1 {:company "github"}}
                          :users {1 {:company 1 :user-name "tami5"}}}}
        router (new-router routes {:middleware middleware :environment environment})
        app (ring/ring-handler router)]

    (testing "Request Keys"
      (let [request (app {:request-method :get :uri "/identity/github/users/1"})]
        (is (= {:user-id 1 :company "github"} (:path (:parameters request)))
            "Should place path arguments inside parameters")

        (are [path expected-type] (= expected-type (type (get-in request path)))
          [:id] UUID                                ;; Random UUID to the request injected by environment-middleware
          [:start-date] Date                        ;; Start date injected by environment-middleware
          [:db] PersistentArrayMap                  ;; injected key by [environment-middleware]
          [:params] PersistentArrayMap)))

    (testing "Coercion Spec Response"
      (let [request (app {:request-method :get :uri "/github/users/1"})]
        (is (= 200 (:status request)))
        (is (string? (:body request)))))

    (testing "Coercion Spec Exception"
      (let [requesta (app {:request-method :get :uri (str "/identity/" 32 "/users/" 1)})
            requestb (app {:request-method :get :uri (str "/identity/company/users/tami")})
            requestc (app {:request-method :get :uri "/apple/users/1"})]

        (is (= [:spec      ;; the actual spec?
                :problems  ;; Problems, where to look to understand the issue?
                :type      ;; :reitit.coercion/request-coercion?
                :coercion  ;; coercion type used?
                :value     ;; value checked {:company "32" :user-id "1"}
                :in]       ;; path to where the spec validation fail?
               (keys (:body requesta)))
            "Should provide the following keys in body")

        (testing "Parameters validation"
          (is (= 400 (:status requesta)) "Should result in 400 status")
          (comment "get-in :body :problems" [{:path [:company] :pred ":clojure.spec.alpha/unknown" :val "32" :via [:spec$57761/company] :in [:company]}])

          (is (= 400 (:status requestb)) "Should result in 400 status")
          (comment "get-in :body :problems" [{:path [:user-id] :pred "clojure.core/int?" :p:val "tami" :p:via [:spec$59018/user-id] :p:in [:user-id]}]))

        (testing "Response Validation"
          (is (= 200 (:status requestc)))
          (is (string? (:body requestc)))
           ;; doesn't fail even thoguh the body won't pass the test
          (is (not (str/includes? (:body requestc) "github"))))))

    (testing "Coercion Pretty Exception"
      (let [middleware (new-middleware {:munntaja false :coercion {} :logging {:pretty? true
                                                                               :types [:coercion]}})
            app  (->> {:middleware middleware :environment environment}
                      (new-router routes)
                      (ring/ring-handler))
            request {:request-method :get :uri (str "/identity/company/users/tami")}]

        (is (= [:reitit.ring.middleware.parameters/parameters
                :duct.reitit.middleware/environment-middleware
                :reitit.ring.middleware.exception/exception
                :reitit.ring.coercion/coerce-request
                :reitit.ring.coercion/coerce-response]
               (mapv :name middleware))
            "Shouldn't include coerce-exceptions")

        (is (str/includes? (with-out-str (app request)) "-- Spec failed --------------------")
            "Should only print to stdout and not return it")))))

(deftest custom-error-handling
  (let [exception {java.lang.NullPointerException
                   (fn [_ r]
                     {:status 500
                      :cause "No parameters received"
                      :uri (:uri r)})
                   java.lang.ArithmeticException
                   (fn [e r]
                     {:status 500
                      :cause (ex-message e)
                      :data (:body-params r)
                      :uri (:uri r)})}
        middleware (new-middleware {:munntaja false :coercion nil :exception exception})
        router (new-router [["/math" (fn [{{:keys [lhs rhs]} :body-params}] (/ lhs rhs))]] {:middleware middleware})
        handler (ring/ring-handler router)
        math-response (handler {:request-method :get :uri "/math" :body-params {:lhs 5 :rhs 0}})
        no-params-response (handler {:request-method :get :uri "/math"})]
    (is (= "Divide by zero" (:cause math-response)))
    (is (= {:lhs 5, :rhs 0} (:data math-response)))
    (is (= "No parameters received" (:cause no-params-response)))))
