(ns syntereen.hax.db
  "The database to store Hax data."
  (:require [clojure.spec.alpha :as s]
            [cambium.core :as log]
            [cambium.mdc  :as mlog]
            [integrant.core :as ig]
            [buddy.hashers :as hashers]
            [syntereen.hax.specs :as hspecs]
            ))

(defonce mock-db
  (atom {}))

(defn user-valid?
  [user]
  (s/valid? ::hspecs/user user))

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

