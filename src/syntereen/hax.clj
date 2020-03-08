(ns syntereen.hax
  "The Hax server."
  (:require
   [cambium.core :as log]
   ;; [cambium.mdc  :as mlog]
   ;; [clojure.spec.alpha :as s]
   [integrant.core :as ig]
   [syntereen.hax.system :as system]
   ))

(defn -main
  "Start up the production hax server."
  [& _]
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
  (def config (system/config-and-load :dev))
  (def prepped-config (ig/prep config))
  (def sys (ig/init prepped-config))
  (ig/halt! sys)
  )
