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

(defn update-docks!
  [docks]
  (swap! *state* assoc :docks docks)
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

(defn ndock!
  []
  (let [docks (api/ndock)]
    (when-not (empty? docks)
      (update-docks! docks))))

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

(defn mission-check!
  []
  (loop [id 1]
    (let [{[_ mission-id time-complete] :mission} (id->fleet id)]
      (when time-complete;;id is valid
        (if (pos? time-complete);;has mission
          (let [now (.getTime (java.util.Date.))]
            (when (and (> now time-complete);;timeup
                       (api/mission-result id));;success
              (charge! id)
              (api/mission-start id mission-id);;repeat mission
              )))
        (recur (inc id))))))

(defn stype->rate
  "表格资料来自:
   http://wikiwiki.jp/kancollecn/?%C6%FE%B5%F4
   https://bitbucket.org/koedoyoshida/kancolle-timer-for-firefox/src/d422c1cc532013c8fad4fe01f01caf5a763dcddf/chrome/content/data.js?at=dev
   http://www56.atwiki.jp/kancolle/pages/432.html
     正确性未经完全验证(本提督没有那么多类型的船)
  "
  [stype]
  (if-let [r
           ({
             2 1;;驱逐舰
             3 1;;轻巡
             4 1;;重装雷巡
             5 1.5;;重巡
             6 1.5;;航空巡洋舰
             7 1.5;;轻空母
             8 1.5;;低速战舰
             9 2;;高速战舰
             10 2;;航空战舰
             11 2;;正规空母
             13 0.5;;潜水艇
             14 1;;潜水空母
             15 1;;补给舰??
             16 1;;水上机母舰
             17 1;;扬陆舰/登陆舰
             18 1;;装甲空母
             }
             stype)]
    r
    1))

(defn lv->a
  [lv]
  (cond
    (<= lv 14) 60
    (<= lv 19) 70
    (<= lv 26) 80
    (<= lv 35) 90
    (<= lv 46) 100
    (<= lv 59) 110
    (<= lv 74) 120
    (<= lv 91) 130
    :else 140))

(defn repair-time
  "http://wikiwiki.jp/kancollecn/?%C6%FE%B5%F4
   return seconds"
  [id]
  (let [{:keys [lv stype nowhp maxhp]} (id->ship id)
        diffhp (- maxhp nowhp)
        rate (stype->rate stype)]
    (if (zero? diffhp)
      0
      (if (<= lv 11)
        (+ 30
           (long (* diffhp rate lv 10)))
        (+ 30
           (long (* diffhp rate (+ (lv->a lv)
                                   (* lv 5)))))))))

(defn format-seconds
  [secs]
  (let [secs (long secs)
        s (rem secs 60)
        mins (quot secs 60)
        m (rem mins 60)
        hours (quot mins 60)]
    (format "%d:%02d:%02d" hours m s)))

(defn empty-docks
  []
  (let [docks (:docks @*state*)]
    (map :id (filter #(= 0 (:state %)) docks))))

(defn in-dock-ships
  []
  (->> @*state*
    :docks
    (filter #(= 1 (:state %)))
    (map :ship-id)))

(defn broken-ships
  "return ships need repairing; order by repair time ascending"
  []
  (->>
    @*state*
    :ships
    (filter (fn [{:keys [maxhp nowhp]}] (not= nowhp maxhp)))
    (map :id)
    (sort-by repair-time)
    (filter #(not ((set (in-dock-ships)) %)))))

(defn repair-ships!
  []
  (let [docks (empty-docks)
        ships (broken-ships)]
    (when-not (or (empty? docks)
                  (empty? ships))
      (doall
        (map (fn [ship-id ndock-id]
               (api/nyukyo-start ship-id ndock-id 0))
             ships docks))
      true)))

(defn login!
  []
  (api/basic)
  (ship!)
  true)

(defn front!
  []
  (api/logincheck)
  (api/material)
  (deck-port!)
  (ndock!)
  (ship3!)
  (api/basic)
  true)