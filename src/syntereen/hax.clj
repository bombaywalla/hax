(ns syntereen.hax
  (:require [clojure.spec.alpha :as s]
            [cambium.core :as log]
            [cambium.mdc  :as mlog]
            [integrant.core :as ig]
            [schema.core :as schema]
            [buddy.sign.jwt :as jwt]
            [buddy.hashers :as hashers]
            [yada.yada :as yada]
            [syntereen.hax.specs :as hspecs]
            [syntereen.hax.system :as system]
            ))

;;; ---------------- Mock db ----------------

(defonce mock-db
  (atom {}))

(defn user-valid?
  [user]
  (s/valid? :hspecs/user user))

(defn sanitize-db-user
  "Sanitize the db user before sending back in a response."
  [db-user]
  (dissoc db-user :password :password-hash))

(defn reset-mock-db
  "Reset the mock-db to be empty.
  REPL use only."
  []
  (reset! mock-db {}))

(defn get-user-key
  "The value to be used as the key for a `user` in the db."
  [user]
  (:email user))

(defn get-user
  "Gets the user specified by`user-key`. Else `nil`."
  [user-key]
  (get @mock-db user-key))

(defn add-user
  "Add a user.
  Returns the added user. Or throws ex-info."
  [user]
  (let [user-key (get-user-key user)
        maybe-user (when user-key (get-user user-key))]
    (cond (not (user-valid? user)) (throw (ex-info "Invalid user info provided"
                                                   {:error-code "invalid-user-info" :user user}))
          (not user-key) (throw (ex-info "Could not get the key from the specified user"
                                         {:error-code "user-no-key" :user user}))
          maybe-user (throw (ex-info "User already exists"
                                     {:error-code "user-exists" :user user}))
          :else (let [passwd (:password user)
                      ;; TODO: Consider adding pepper to the hash, in addition to the salt.
                      passwd-hash (hashers/derive passwd {:alg :pbkdf2+sha512})
                      stored-user (assoc (dissoc user :password) :password-hash passwd-hash)]
                  (swap! mock-db assoc user-key stored-user)
                  stored-user))))

(defn delete-user
  "Removes the user specified by `user-key`.
  Whether or not it existed.
  Returns `nil`."
  [user-key]
  (swap! mock-db dissoc user-key)
  nil)

(defn update-user
  "Update the user specified by `user-key`.
  Assumes a valid `updated-user`.
  Returns `nil` if user does not exist.
  Else non-nil."
  [user-key updated-user]
  (let [maybe-user (get-user user-key)]
    (when maybe-user
      (swap! mock-db assoc user-key updated-user))))

(defn all-user-keys
  "Returns a vector of all the user keys."
  []
  (into [] (keys @mock-db)))

(defn set-user-token
  "Set the token for the user to `token`. Overwrites any existing.
  Returns `nil` on error.
  Else non-nil."
  [user-key token]
  (let [maybe-user (get-user user-key)]
    (when (and maybe-user token)
      (swap! mock-db assoc-in [user-key :token] token)
      token)))

(defn remove-user-token
  "Removes any token associated with the `user-key`.
  Returns `nil` on error.
  Else non-nil."
  [user-key]
  (let [maybe-user (get-user user-key)]
    (when maybe-user
      (swap! mock-db update-in [user-key dissoc :token]))))

;;; ---------------- Main ----------------

;; TODO: Note that this way of calculating the jwt-secret
;; (in contrast to having the secret in the config)
;; implies that the user session is pinned to an instance of this server.
;; So, if the server dies, so does the session.
;; Another thing I don't like is that this does some computation on load.
(def jwt-secret
  "Return a random string used as a secret to sign JWT tokens.
  The string is 16 characters.
  Each char is one of the 94 printable non-white-space ASCII characters.
  Note that this generates a new token each time there is a new server process,
  not each time this function is called. This function is still \"pure\",
  but only within a particular server instance."
  (let [secret-len 16
        secret (apply str (take secret-len (repeatedly #(char (+ (rand 94) 33)))))]
    (fn jwt-secret [] secret)))

(defn gen-token
  "Base64 encoded token for a speified `user`."
  [user]
  (let [email (:email user)]
    (try (jwt/sign {:email email} (jwt-secret))
         (catch Exception e nil))))     ; TODO: At least log this condition. Do even beter.

(defn login-creds-valid?
  "Returns `true` if the login credentials presented are valid. Else `false`."
  [db-user login-user]
  (let [db-password-hash (:password-hash db-user)
        login-password (:password login-user)]
    (hashers/check login-password db-password-hash)))

;;; TODO: If the incoming JSON is not proper, we get a 500. Probably should get a 400, no?
;;; In this case, an email field with a null value.
;;; 11:35:40.745 [manifold-pool-2-16] ERROR yada.handler - Internal Error 
;;; java.lang.ClassCastException: null

(defn do-register
  [ctx]
  ;; {:parameters {:body {:user {:full-name ... :email ... :password ...}}}}
  (let [user (:user (:body (:parameters ctx)))
        response (:response ctx)]
    (log/debug "\n----------------------------------------------------------------")
    (log/debug "The register API was called.")
    (log/debug (str "context parameters: " (pr-str user)))
    (try (let [new-user (add-user user)
               _ (log/debug (str "new-user: " (pr-str new-user)))
               body {:user (sanitize-db-user new-user)}]
           (log/debug (str "response body: " (pr-str body)))
           (assoc response :status 200 :body body))
         (catch Exception e
           (log/debug (str "Exception: " (pr-str e)))
           (if-let [exdata (ex-data e)] ; TODO: assumes only we throw ex-info. Others could too.
             (assoc response :status 400 :body {:errors exdata})
             (assoc response :status 500 :body {:errors {:exception e}}))))))

(defn do-login
  [ctx]
  ;; {:parameters {:body {:user {:email ... :password ...}}}}
  (let [user (:user (:body (:parameters ctx)))
        response (:response ctx)
        user-key (get-user-key user)
        db-user (get-user user-key)
        token (gen-token db-user)
        body {:user (assoc (sanitize-db-user db-user) :token token)}]
    (log/debug "\n----------------------------------------------------------------")
    (log/debug "The login API was called.")
    (log/debug (str "context parameters: " (pr-str user)))
    (log/debug (str "response body: " (pr-str body)))
    (if (login-creds-valid? db-user user)
      (assoc response :status 200 :body body)
      (assoc response :status 400 :body {:errors "Login failed"})) ;TODO: Mention user id/email
    ))

(defn do-list-users
  [ctx]
  (all-user-keys))

(def access-control
  {
   :access-control {:allow-origin "*"
                    :allow-credentials false
                    :expose-headers #{"X-Custom"}
                    :allow-methods #{:get :post :options}
                    :allow-headers ["Api-Key" "Content-Type"]
                    }
   })

(def logger
  {
   :logger (fn [ctx]
             (log/debug "\n----------------------------------------------------------------")
             (log/debug (str "Request: " (pr-str (:request ctx))))
             (log/debug (str "Keys: " (keys ctx)))
             (log/debug (str "Parameters: " (pr-str (:parameters ctx)))))
   })

(def register-resource
  (yada/resource
   (merge 
    {:id :hax/register-resource
     :description "Resource for handling users."
     :summary "Handle users."
     :methods {
               :post {:description "Register (aka sign up) a new user."
                      :summary "Register a new user."
                      :consumes "application/json"
                      :produces "application/json"
                      :parameters {:body {:user {:full-name schema/Str
                                                 :email schema/Str
                                                 :password schema/Str
                                                 }}}
                      :response do-register
                      :responses {200 {:description "Registration successful"}
                                  400 {:description "Registration unsuccessful: Client error"}
                                  500 {:description "Registration unsuccessful: Server error"}
                                  }}

               :get {:description "List all user keys. If authorized to do so."
                     :summary "List all user keys."
                     :produces "application/json"
                     :response do-list-users
                     }
               }
     }
    logger
    access-control)))

(def login-resource
  (yada/resource
   (merge
    {:id :hax/login-resource
     :description "Resource for a user to log in."
     :summary "Log in a user."
     :methods {
               :post {:description "Login a user."
                      :summary "User login."
                      :consumes "application/json"
                      :produces "application/json"
                      :parameters {:body {:user {:email schema/Str
                                                 :password schema/Str
                                                 }}}
                      :response do-login
                      :responses {200 {:description "Login successful"}
                                  400 {:description "Login unsuccessful: Client error"}
                                  500 {:description "Login unsuccessful: Server error"}
                                  }}}
     }
    logger
    access-control)))

(def hax-routes
  ["" [
       ["/api" [
                ["/users/login" login-resource] ; has to come before /users
                ["/users" register-resource]
                [true (yada/as-resource nil)]
                ]]
       [true (yada/as-resource nil)]
       ]])

(defn create-web-server
  [port]
  (log/info "Starting yada.")
  (yada/listener hax-routes {:port port}))

(defn stop [server]
 ((:close server)))

(defmethod ig/init-key ::webserver [_ {:keys [port] :as value}]
  (create-web-server port))

(defmethod ig/halt-key! ::webserver [_ server]
  (stop server))

(defn -main
  "Start up the production hax server."
  [& args]
  (log/info "Hax is starting.")
  (let [config (system/config-and-load :prod)
        prepped-config (ig/prep config)
        sys (ig/init prepped-config)]
    (log/info "Hax initialized.")
    (.addShutdownHook
      (Runtime/getRuntime)
      (Thread.
        (fn []
          (ig/halt! sys)))))
  @(promise))

(comment
  (require '[syntereen.hax :as hax] :reload)
  (def server (hax/create-web-server (config/webserver-port)))
  (hax/stop server)
  )
