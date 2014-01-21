(ns kancolle-worker.api
  (:use [kancolle-worker.http :only [defkcs-api]]))

;;return ship data
(defkcs-api ship "~get_member/ship" [])

;;sort-key 1
;;sort-order 2
;;return :data ship updated partial data
;;return :data-deck deck
(defkcs-api ship2 "~get_member/ship2" [sort-key sort-order]
  (select-keys result [:data :data-deck]))

;;sort-key 1
;;sort-order 2
;;return :ship-data ship updated partial data
;;return :deck-data deck
(defkcs-api ship3 "~get_member/ship3" [sort-key sort-order])

(defkcs-api ndock "~get_member/ndock" [])

(defkcs-api material "~get_member/material" [])

(defkcs-api logincheck "~auth_member/logincheck" [])

(defkcs-api deck-port "~get_member/deck_port" [])

(defkcs-api basic "~get_member/basic" [])

;;maparea-id 1
(defkcs-api mission "~get_master/mission" [maparea-id])

;;deck-id fleet-id 1~
;;mission-id 1~
(defkcs-api mission-start "~req_mission/start" [deck-id mission-id])

;;deck-id fleet-id 1~
(defkcs-api mission-result "~req_mission/result" [deck-id])

;;ship_id 55
;;ndock_id 1..
;;highspeed 0/1
(defkcs-api nyukyo-start "~req_nyukyo/start" [ship-id ndock-id highspeed]
  result)

;;kind 1 fueld only 2 bullet only 3 all
;;id-items ship ids
(defkcs-api charge "~req_hokyu/charge" [kind id-items])