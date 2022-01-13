(ns conduit.handler-test
  (:require [conduit.handler.user]
            [conduit.test-util :refer [with-system *ring-handler*]]
            [clojure.test :refer [is deftest use-fixtures testing]]
            [conduit.spec]))

(use-fixtures :once with-system)

(defn- request [method path extra-req-info]
  (*ring-handler* (merge {:request-method method :uri path} extra-req-info)))

(deftest user
  (let [test-body {:user {:username "tami5" :email "example@mail.com" :password "12345678"}}]
    (testing "POST: /api/users"

      (testing "Correct Parameters"
        (let [response (request :post "/api/users" {:body-params test-body})]
          (is (= 200 (:status response)))
          (is (not= nil (get-in response [:body :user :id])))))

      (testing "Email or Username already exists"
        (let [response (request :post "/api/users" {:body-params test-body})]
          (is (= 400 (:status response)))))

      (testing "Missing Parameters or invalid field type"
        (let [response (->> (assoc-in test-body [:user :password] 123456)
                            (hash-map :body-params)
                            (request :post "/api/users"))]
          (is (= 400 (:status response))))))

    (testing "POST: /api/users/login"

      (testing "Correct Parameters"
        (let [response (request :post "/api/users/login" {:body-params test-body})]
          (is (= 200 (:status response)))
          (is (not= nil (get-in response [:body :user :id])))))

      (testing "Wrong Password or email"
        (let [incorrect-email (assoc-in test-body [:user :email] "xxx@xxx.com")
              incorrect-pass  (assoc-in test-body [:user :password] "120jhffsdf")
              email-response  (request :post "/api/users" {:body-params incorrect-email})
              pass-response   (request :post "/api/users" {:body-params incorrect-pass})]
          (is (= 400 (:status email-response)))
          (is (= 400 (:status pass-response)))))

      (testing "Missing Parameters or invalid field type"
        (let [body {:body-params (update test-body :user select-keys [:email])}
              response  (request :post "/api/users/login" body)]
          (is (= 400 (:status response))))))))
