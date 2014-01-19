(ns kancolle-worker.game
  (:require [kancolle-worker.api :as api]))

(def ^:dynamic *state* (atom {}))

(defn update-ship!
  []
  (let [ships (api/ship)]
    (swap! *state* assoc :ships ships)
    (if ships true false)))

