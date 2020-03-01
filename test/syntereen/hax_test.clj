(ns syntereen.hax-test
  (:require [clojure.test :refer [deftest is testing use-fixtures] :as ct]
            [cambium.core :as log]
            [integrant.core :as ig]
            [clj-http.client :as http]
            [cheshire.core :as cheshire]
            [syntereen.hax :as hax]
            [syntereen.hax.system :as system]
            [syntereen.hax.db :as db]))

;;; See https://stuartsierra.com/2016/05/19/fixtures-as-caches
;;; For the use of *sut* (system under test)

(def ^:dynamic ^:private *sut* (atom nil))

(defn start-sut
  "Start a Hax test server.
  Return the started system."
  []
  (let [config (ig/prep (system/config-and-load :test))]
    (ig/init config)))

(defn stop-sut
  "Stop the started system `sys`."
  [sys]
  (ig/halt! sys))

(defmacro with-sut
   "Acquires the sut and binds it locally to
   `symbol` while executing `body`. Ensures resource
   is released after body completes. If called in
   a dynamic context in which *sut* is
   already bound, reuses the existing sut and
   does not release it."
   [symbol & body]
   `(let [~symbol (or (deref *sut*)
                      (start-sut))]
      (try ~@body
           (finally
             (when-not (deref *sut*)
               (stop-sut ~symbol))))))

(defn sut-fixture
   "Fixture function to acquire a sut for all
   tests in a namespace."
   [test-fn]
   (with-sut r
     (binding [*sut* (atom r)]
       (test-fn))))

(use-fixtures :once sut-fixture)

;; For REPL use only
(defn go
  "Start the Hax system. REPL use only."
  []
  (reset! *sut* (start-sut)))

;; For REPL use only
(defn halt
  "Stop the Hax system. REPL use only."
  []
  (stop-sut @*sut*))

;;; ----------------------------------------------------------------

(def joe-password "prettyweakpassword")
(def joe-email "joe@example.com")
(def joe-full-name "Joe Smith")

(defn reset-example-db
  "Reset the db wth one example user."
  []
  (db/reset-mock-db)
  (db/add-user {:email joe-email
                :full-name joe-full-name
                :password joe-password}))

(defn login-api-url
  "Returns the API URL to be used for logins.
  NOTE: Only works after the system is started."
  []
  (with-sut sut
    (let [base-uri "http://127.0.0.1"
          api-uri "/api/users/login"
          port (:port (::hax/webserver sut))
          url (str base-uri ":" port api-uri)]
      url)))

;; Default options to use for the http client.
(def http-opts
  {
   :throw-exceptions false              ; Do not throw exception on exceptional statuses
   :content-type :json                  ; Send JSON
   :as :json                            ; Return JSON
   :coerce :always                      ; Assume return is JSON always (not just unexceptional statuses)
   })

;; Merge with http-opts if trying to debug what the http client is doing.
(def http-debug-opts
  {
   :debug true
   :debug-body true
   :save-request? true
   })

(deftest login-success-test
  (let [_ (reset-example-db)
        url (login-api-url)
        _ (log/debug (str "LOGIN API URL: " url))
        payload {:user {:email joe-email
                        :password joe-password}}
        all-params (assoc http-opts
                          :body
                          (cheshire/generate-string payload))
        response (http/post url all-params)
        body (:body response)]

    (testing "Login success test"
      (is (= 200 (:status response)) "Checking for success.")
      (is (= (:email (:user body))
             (:email (:user payload))) "Checking that the email sent and received are the same.")
      (is (not (nil? (:full-name (:user body)))) "Checking that the full-name exists in the response.")
      (is (not (nil? (:token (:user body)))) "Checking that the token exists in the response."))

    ))

(deftest login-failure-test
  (let [_ (reset-example-db)
        url (login-api-url)
        _ (log/debug (str "LOGIN API URL: " url))
        payload {:user {:email "unknown@example.com"
                        :password joe-password}}
        all-params (assoc http-opts
                          :body
                          (cheshire/generate-string payload))
        response (http/post url all-params)
        body (:body response)
        _ (log/info (str "BODY: " (pr-str body)))]

    (testing "Login failure test"
      (is (= 400 (:status response)) "Checking for failure.")
      (is (not (nil? (:errors body))) "Checking that the errors field exists."))

    ))
