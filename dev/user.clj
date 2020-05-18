(ns user
  "The user namespace for development."
  ;; Ideally, require nothing here (see jit macro)
  )

;; Stolen from @plexus, with some mods.

(defmacro jit
  "Just in time loading of dependencies.
  Enables fast time to get a repl prompt.
  Also, makes sure you don't do much in this file,
  other than define some functions/macros."
  [sym]
  `(requiring-resolve '~sym))

(defn set-prep!
  "Set the prep function.
  A no-argument function that preps keys and load namespaces as needed."
  []
  ((jit integrant.repl/set-prep!) (constantly ((jit syntereen.hax.system/config-and-load) :dev))))

(defn go
  "Start up the Integrant system."
  []
  (set-prep!)
  ((jit integrant.repl/go)))

(defn reset
  "Reset (stop, reload, start) the Integrant system."
  []
  (set-prep!)
  ((jit integrant.repl/reset)))

(defn halt
  "Stop the Integrant system."
  []
  (set-prep!)
  ((jit integrant.repl/halt)))

(defn system
  "Get the Integrant system."
  []
  @(jit integrant.repl.state/system))

(defn config
  "Get the Integrant configuration."
  []
  @(jit integrant.repl.state/config))

;; So that clojure.tools.namespace knows which dirs to scan when refreshing.
((jit clojure.tools.namespace.repl/set-refresh-dirs) "dev" "src" "test")
