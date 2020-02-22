(ns syntereen.hax.system
  "System configuration for the hax server."
  (:require [aero.core :as aero]
            [clojure.java.io :as io]
            [integrant.core :as ig]
            )
  )

(def config-file-name "hax/system.edn")

;; Tell the aero reader about ig/ref
(defmethod aero/reader 'ig/ref
  [_ tag value]
  (ig/ref value))

;; Tell the aero reader about ig/refset
(defmethod aero/reader 'ig/refset
  [_ tag value]
  (ig/refset value))

(defn config
  "Get the system config from the config file,
  as per the supplied `profile`.
  Note that returned system config (key :ig/system)
  is a processed version of what was in the config file."
  [profile]
  (let [raw-aero-config (aero/read-config (io/resource config-file-name)
                                          {:profile profile})]
    (:ig/system raw-aero-config)))

(defn config-and-load
  "Read the system config.
  Load posisble namespaces.
  Return the config."
  [profile]
  (let [config (config profile)]
    (ig/load-namespaces config)
    config))
