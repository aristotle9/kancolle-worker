(ns kancolle-worker.game
  (:require [kancolle-worker.api :as api]))

(def ^:dynamic *state* (atom {}))

(defn id->ship
  [id]
  (get-in @*state* [:id->ship id]))

(defn id->fleet
  [id]
  (get-in @*state* [:deck-ports (dec id)]))

(defn update-ships!
  [ships]
  (let [id->ship (zipmap (map :id ships) ships)]
    (swap! *state* assoc :ships ships)
    (swap! *state* assoc :id->ship id->ship)
    true))

(defn update-partial-ships!
  [partial-ships]
  (let [id->ship (:id->ship @*state*)
        p->new-ship (fn [{:keys [id] :as p-ship}]
                      (merge (id->ship id) p-ship))
        new-ships (doall (map p->new-ship partial-ships))]
    (update-ships! new-ships)))

(defn update-deck-ports!
  [deck-ports]
  (swap! *state* assoc :deck-ports deck-ports)
  true)

(defn ship!
  []
  (let [ships (api/ship)]
    (when-not (empty? ships)
      (update-ships! ships))))

(defn ship2!
  []
  (let [{partial-ships :data deck-ports :data-deck } (api/ship2 1 2)]
    (when-not (empty? partial-ships)
      (update-partial-ships! partial-ships))
    (when-not (empty? deck-ports)
      (update-deck-ports! deck-ports))))

(defn ship3!
  []
  (let [{partial-ships :ship-data deck-ports :deck-data} (api/ship3 1 2)]
    (when-not (empty? partial-ships)
      (update-partial-ships! partial-ships))
    (when-not (empty? deck-ports)
      (update-deck-ports! deck-ports))))

(defn deck-port!
  []
  (let [deck-ports (api/deck-port)]
    (when-not (empty? deck-ports)
      (update-deck-ports! deck-ports))))

(defn charge!
  [id]
  (let [ship-ids
        (->> id
          (id->fleet)
          (:ship)
          (take-while #(not= % -1))
          (interpose ",")
          (apply str))]
    (api/charge 3 ship-ids)))

(defn fuel-bull-need
  "id fleet-id 1~ return [fuel bull]"
  [id]
  (->> id
    (id->fleet)
    (:ship)
    (take-while #(not= % -1))
    (map id->ship)
    (map #(let [{:keys [bull bull-max fuel fuel-max]} %]
            [(- fuel-max fuel) (- bull-max bull)]))
    (reduce (fn [[s t] [x y]]
              [(+ s x) (+ t y)]) [0 0])))

(defn mission-result!
  [id]
  (let [{[_ _ time-complete] :mission} (id->fleet id)
        now (.getTime (java.util.Date.))]
    (if (and (pos? time-complete)
             (> now time-complete))
      (api/mission-result id))))
