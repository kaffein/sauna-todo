(ns frontend.main
  (:require [common.localization :refer [tr]]
            [reagent.core :as reagent]
            [eines.client :as eines]
            [reagent-dev-tools.state-tree :as dev-state]
            [reagent-dev-tools.core :as dev-tools]))

;;
;; State
;;

(defonce state (reagent/atom nil))

(dev-state/register-state-atom "App state" state)

(defn update-todos! [todos]
  (swap! state assoc :todos todos))

;;
;; Views
;;

(defn todo-item-view [todo]
  [:div.todo-list-item
   [:span.todo-list-item__span (:text todo)]])

(defn todo-list-view []
  [:div.todo-list
   (for [todo (:todos @state)]
     ^{:key (:id todo)}
     [todo-item-view todo])])

(defn todo-input-view []
  [:input.todo-input
   {:placeholder (tr :new-todo)
    :auto-focus true
    :value (:new-todo @state)
    :on-change (fn [e]
                 (let [new-todo (-> e .-target .-value)]
                   (swap! state assoc :new-todo new-todo)))
    :on-key-press (fn [e]
                    (let [new-todo (:new-todo @state)]
                      (if (= (.-charCode e) 13)
                        (eines/send! {:message-type :new-todo
                                      :body new-todo}
                                     (fn [_]
                                       (swap! state dissoc :new-todo))))))}])

(defn main-view []
  [:div.todo-container
   [:h1.todo-title (tr :page-title)]
   [:div.todo-content
    [todo-input-view]
    [todo-list-view]]
   [:div.todo-footer (tr :we-love-clojure)]])

;;
;; Websockets
;;

(defn on-connect []
  (js/console.log "Connected to backend.")
  (eines/send! {:message-type :get-todos}
               (fn [response]
                 (update-todos! (:body response)))))

(defn on-message [message]
  (if (= (:message-type message) :todos)
    (update-todos! (:body message))
    (js/console.warn "Got unrecognized message from backend: "
                     (pr-str message))))

(defn on-close []
  (js/console.log "Disconnected from backend."))

(defn on-error []
  (js/console.warn "Disconnected from backend because of an error."))

;;
;; Main
;;

(defn ^:export main []
  (eines/init! {:on-connect on-connect
                :on-message on-message
                :on-close on-close
                :on-error on-error})
  (reagent/render [main-view] (.getElementById js/document "app"))
  (if-let [dev-el (.getElementById js/document "dev")]
    (reagent/render [dev-tools/dev-tool {}] dev-el)))

(main)
