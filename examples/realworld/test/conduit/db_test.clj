(ns conduit.db-test
  (:require [clojure.test :refer [testing deftest is use-fixtures]]
            [conduit.db.user :as user]
            [conduit.test-util :refer [*database* with-system]]
            [clojure.string :as string]))

(use-fixtures :once with-system)

(deftest users
  (let [user {:username "tami5"
              :email "example@mail.com"
              :password "123456789"
              :bio "Building spaceships"
              :image "/api/assets/users/tami5.png"}]

    (testing "Register New User"
      (let [user (user/register *database* user)]
        (is (:id user)
            "should return with :id")
        (is (string/starts-with? (:password user) "bcrypt")
            "should encrypt password")))

    (testing "Registering user with same email or username"
      (let [try-register #(try (user/register *database* %) (catch Exception e e))
            same-email-and-useranme (try-register user)
            same-email (try-register (assoc user :username "xxxx"))
            same-username (try-register (assoc user :email "vam@gmail.com"))]
        (testing "Exception Type"
          (is (= org.postgresql.util.PSQLException (type same-email-and-useranme)))
          (is (= org.postgresql.util.PSQLException (type same-email)))
          (is (= org.postgresql.util.PSQLException (type same-username))))
        (testing "Exception Message"
          (is (string/includes? (ex-message same-email-and-useranme) "users_email_key"))
          (is (string/includes? (ex-message same-email) "users_email_key"))
          (is (string/includes? (ex-message same-username) "users_username_key")))))

    (testing "Correct Login Information"
      (is (map? (user/login *database* user))))))
