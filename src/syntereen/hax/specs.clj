(ns syntereen.hax.specs
  "Specs for hax."
  (:require [clojure.spec.alpha :as s]
            )
  )

(s/def ::string string?)
(s/def ::full-name ::string)
(s/def ::password ::string)
(s/def ::password-hash ::string)
(s/def ::token ::string)

(def email-regex #"^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\.[a-zA-Z]{2,63}$")
(s/def ::email-type (s/and string? #(re-matches email-regex %)))
(s/def ::email ::email-type)

(s/def ::user (s/keys :req-un [::email]
                      :opt-un [::full-name ::password ::password-hash ::token]))
  
