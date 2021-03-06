(ns explojure.dataframe.impl.modify
  (:require [explojure.dataframe.construct :as ctor]
            [explojure.dataframe.impl.get :as raw]))

(defn columnize
  "If value is a sequential, return value unchanged. If value is
  not sequential then repeat the value to create a vector of
  appropriate length for the specified dataframe."
  [df value]
  
  (if (not (sequential? value))
    (vec (repeat (raw/nrow df) value))
    value))

(defn add-col
  ([this colname column]
   (let [colnames (raw/colnames this)
         existing (set colnames)
         columns  (raw/columns this)
         rownames (raw/rownames this)]
     (assert (not (existing colname))
             (str "add-col: column \"" colname "\" already exists"))
     (ctor/new-dataframe (conj colnames colname)
                         (conj columns (columnize this column))
                         rownames)))
  
  ([this add-map]
   (assert (associative? add-map)
           (str "add-col: add-map must be associative. (Received " (type add-map) ".)"))
   (reduce (fn [d [c v]]
             (add-col d c v))
           this
           (seq add-map))))


(defn rename-col
  ([this old-colname new-colname]
   (let [colname-idx (raw/colname-idx this)
         colnames (raw/colnames this)
         columns (raw/columns this)
         rownames (raw/rownames this)]
     (assert (contains? colname-idx old-colname)
             (str "rename-col: cannot rename a column that doesn't exist (" old-colname ")."))
     (assert (not (contains? colname-idx new-colname))
             (str "rename-col: cannot rename column to something that already exists (" new-colname ")."))
     (let [old-cn-idx (get colname-idx old-colname)]
       (ctor/new-dataframe (assoc colnames old-cn-idx new-colname)
                           columns
                           rownames))))
  
  ([this rename-map]
   (assert (associative? rename-map)
           (str "rename-col: rename-map must be associative. (Received " (type rename-map) ".)"))
   (reduce (fn [d [o n]]
             (rename-col d o n))
           this
           (seq rename-map))))

(defn replace-col
  ([this colname column]
   (let [colname-idx (raw/colname-idx this)
         columns  (raw/columns this)
         colnames (raw/colnames this)
         rownames (raw/rownames this)]
     (assert (contains? colname-idx colname)
             (str "replace-col: Cannot replace a column that doesn't exist (" colname ")."))
     (ctor/new-dataframe colnames
                         (assoc columns
                                (get colname-idx colname)
                                (columnize this column))
                         rownames)))
  
  ([this replace-map]
   (assert (associative? replace-map)
           (str "replace-col: replace-map must be associative. (Received " (type replace-map) ".)"))
   (reduce (fn [d [oc nd]]
             (replace-col d oc nd))
           this
           (seq replace-map))))



(defn set-col
  ([this colname column]
   (let [colname-idx (raw/colname-idx this)]
     (if (contains? colname-idx colname)
       (replace-col this colname column)
       (add-col this colname column))))
  
  ([this set-map]
   (assert (associative? set-map)
           (str "set-col: set-map must be associative. (Received " (type set-map) ".)"))
   (reduce (fn [df [colname column]]
             (set-col df colname column))
           this
           (seq set-map))))

(defn set-colnames [this new-colnames]
  (ctor/new-dataframe new-colnames
                      (raw/columns this)
                      (raw/rownames this)))

(defn set-rownames [this new-rownames]
  (ctor/new-dataframe (raw/colnames this)
                      (raw/columns this)
                      new-rownames))
