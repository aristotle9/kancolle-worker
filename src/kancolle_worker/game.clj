(ns kancolle-worker.game
  (:require [kancolle-worker.api :as api]))

(def ^:dynamic *state* (atom {}))

(defn id->ship
  [id]
  (get-in @*state* [:id->ship id]))

(defn update-ship!
  []
  (if-let [ships (api/ship)]
    (let [id->ship (zipmap (map :id ships) ships)]
      (swap! *state* assoc :ships ships)
      (swap! *state* assoc :id->ship id->ship)
      true)))

(defn update-deck-port!
  []
  (if-let [deck-port (api/deck-port)]
    (do
      (swap! *state* assoc :deck-port deck-port)
      true)))