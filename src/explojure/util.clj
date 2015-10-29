(ns explojure.util
  (:require [clojure.java.io :as io]))

(defmacro +>
  "Provides basically the same functionality as (as->), but probably
not as well."
  [x & forms]
  `(-> ~x ~@(for [f forms] `((fn [~'%] ~f)))))

(defn rows->cols
  "Convert [[1 2] [3 4] [5 6]] into [[1 3 5] [2 4 6]]. Makes as many
passes as there are columns, causing a lazy sequence to be fully
realized after the first pass."
  [rows]
  (let [n-col (count (first rows))]
    (vec (for [i (range n-col)]
           (vec (map #(nth % i) rows))))))

(defn equal-length
  "For one or more vectors, determine if all vectors have the same length."
  [v1 & vs]
  (apply = (map count (concat [v1] vs))))

(defn t
  "Transform vectors of vectors."
  [vs]
  {:pre [apply equal-length vs]}
  ;; for all minor vectors in the top-level vector
  (loop [vs vs
         result (array-map)]
    (if (seq vs)
      (recur (next vs)
             ;; for all elements in each minor vector
             (loop [v-elements (first vs)
                    result result
                    i 0]
               (if (seq v-elements)
                 (recur (next v-elements)
                        (assoc result
                               i
                               (conj (get result i [])
                                     (first v-elements)))
                        (inc i))
                 result)))
      ;; re
      (mapv #(get result %)
            (sort (keys result))))))

(defn rand-n
  ([n] (rand-n 1 n))
  ([max n]
   (vec (repeatedly n #(rand max)))))

(defn random-seq
  "
  Return a lazy sequence where each element is a size 1 random sample
  from coll. The returned sequence will have the same theoretical frequency
  distribution as coll.

  Performance Notes
   - processes 10,000,000 in about 3.5 seconds, where (count coll) is 26
   - processes 10,000,000 in about 6.5 seconds, where (count coll) is 10e6.
  "
  [coll]
  (lazy-seq
   (cons (rand-nth coll) (random-seq coll))))

(defn choose
  "Need to allow with/without replacement options."
  [x n]
  (let [population (vec x)
        indices (take n (shuffle (range (count population))))]
    (mapv population indices)))

(defn maybe?
  "
  Written as a predicate function for filter that randomly returns true
  with a certain probability (default 0.5).

  Arguments
   - _: placeholder for when maybe? is used as a predicate for a filter
   - p: change the probability maybe? will return true
  "
  ([] (maybe? nil 0.5))
  ([_] (maybe? nil 0.5))
  ([_ p] (< (rand) p)))


(defn filter-ab
  "
  Return the elements of sequence b for which the application of pred to the
  corresponding elements in sequence b yields truthy results.

  If pred is not given, it defaults to \"identity,\" which means that for
  every element in a that is truthy, the corresponding element of b will be
  returned.
  "
  ([a b] (filter-ab identity a b))
  ([pred a b]
   (map #(second %)
        (filter #(pred (first %))
                (map vector a b)))))

(defn where
  "
  Return the 0-based indices where coll is truthy.

  Notes
   - processes 10,000,000 in approximately 23 seconds
  "
  [coll]
  (filter-ab coll (range (count coll))))

(defn cart-prod
  "
  Given two collections, xs and ys, return a lazy seq of vectors where
  each vector is one of all possible unique permutations of x and y.

  For example, (cart-prod [1 2 3] [\"a\" \"b\"]) yields:
    ([1 \"a\"] [1 \"b\"] [2 \"a\"] [2 \"b\"] [3 \"a\"] [3 \"b\"])
  "
  [xs ys]
  (for [x xs, y ys] [x y]))

(defn print-cp
  "Print a list of directories visible from the classpath."
  []
  (let [cl (ClassLoader/getSystemClassLoader)
        urls (.getURLs (cast java.net.URLClassLoader cl))]
    (doseq [u urls]
      (println (.getFile u)))))

(defmacro time-n
  "Evaluates expr n times and returns the average amount of time (ms) that it took."
  [expr n]
  `(/ (apply + (for [i# (range ~n)]
                  (let [start# (. System (nanoTime))
                        ret# ~expr
                        elapsed# (/ (double (- (. System (nanoTime)) start#)) 1000000.0)]
                    elapsed#)))
      (double ~n)))


(defn assoc-by
  "Associate (val-fn val) into map m, with a key generated by (key-fn val)."
  [m key-fn val-fn val]
  (assoc m (key-fn val) (val-fn val)))


(defn assoc-many
  "For use when you want to associate many values (in a vector) with one key.
  Puts things in a vector automatically if they're not there already."
  [m key val]
  (let [current-val (get m key [])]
    (assoc m key (conj (if (sequential? current-val)
                         current-val
                         [current-val])
                       val))))

(defn exclude
  "
  Return coll, with exclude-vals excluded.

  Arguments:
   - coll: a vector or a list
   - exclude-vals: a vector or list of values that should be excluded
     from coll
  "
  [coll exclude-vals]
  (println "enter exclude")
  (let [result   (let [exclude-set (set exclude-vals)]
                   (filter (complement (fn [x] (contains? exclude-set x))) coll))]
    (println "exit exclude")
    result))

(defn unique
  "
  Return the unique values of coll in the order in which they appeared.

  Arguments:
   - coll: a vector or a list
  "
  [coll]
  (vec (keys (apply array-map (interleave coll (repeat true))))))

(defn get-tmp-filepath [prefix suffix]
  (.getCanonicalPath (java.io.File/createTempFile prefix suffix)))

(defn move-file
  "Move file f to new-path.  Returns true if successful"
  [f new-f]
  (java.nio.file.Files/move
   (java.nio.file.Paths/get "" (into-array String [f]))
   (java.nio.file.Paths/get "" (into-array String [new-f]))
   (into-array java.nio.file.CopyOption [])))

(defn pmap-n
  "
  Like pmap, but you choose the number of threads to execute simultaneously.
  
  Arguments:
   - f: the mapping function
   - coll: the list of arguments
   - n: the number of simultaneous threads to execute
  "
  ([f coll n]
   (let [rets (map #(future (f %)) coll)
         step (fn step [[x & xs :as vs] fs]
                (lazy-seq
                 (if-let [s (seq fs)]
                   (cons (deref x) (step xs (rest s)))
                   (map deref vs))))]
     (step rets (drop n rets)))))

(defn all? [coll]
  (every? identity coll))

(defn count-trues [coll]
  (loop [remaining coll
         c 0]
    (if (seq remaining)
      (cond (first remaining) (recur (rest remaining) (inc c))
            :else (recur (rest remaining) c))
      c)))

(defn pmap-n-2
  ;; if no n is given, run on as many processors as we can! + 2!
  ([f coll]
   (pmap-n-2 f coll (+ 2 (.. Runtime getRuntime availableProcessors))))
  
  ;; if n is given, restrict
  ([f coll n]
   (if (= (count coll) 0)
     coll
     (loop [remaining coll
            acc []]
       (if (seq remaining)
         ;; polling is inefficient, but whatever
         (do
           (while (>= (count-trues (map #(not (realized? %)) acc))
                      n)
             (Thread/sleep 100))
           (recur (rest remaining) (conj acc (future (f (first remaining))))))
         (do
           ;; wait for final computation to finish
           (while (not (realized? (last acc)))
             (Thread/sleep 100))
           acc))))))

(defn rand-between
  "Provide a continuous random number between min and max."
  [min max]
  (let [rng (- max min)]
    (+ (* rng (rand)) min)))

(defn readable-timestamp
  "
  Returns a timestamp string in the form of yyyyMMddHHmmssSSS.
  (year, month, date, hours, minutes, seconds, milliseconds).
  "
  []
  (let [curr-time (java.util.Date.)
        ft (java.text.SimpleDateFormat. "yyyyMMddHHmmssSSS")]
    (.format ft curr-time)))

(defn combine-paths
  "Get a combined path string for one or more paths."
  [first-path & rest]
  (.toString (java.nio.file.Paths/get first-path
                                      (into-array java.lang.String
                                                  rest))))

(defn file-exists
  "Determine if a file or directory at path exists."
  [path]
  (.exists (io/as-file path)))

(defn rfirst
  "
  rfirst stands for \"Recursive (first)\"
  
  (n-first coll 1) is equivalent to (first coll).
  (n-first coll 2) is equivalent to (first (first coll)).
  (n-first coll 3) is equivalent to (first (first (first coll))).
  etc.
  "
  [coll n]
  (if (> n 0)
    (recur (first coll) (dec n))
    coll))


(defn multi-filter
  "Apply multiple filters to a collection."
  [coll & filters]
  (loop [rem-coll coll
         rem-fs filters]
    (if (and (seq rem-coll) (seq rem-fs))
      (recur (filter (first rem-fs) rem-coll)
             (rest rem-fs))
      rem-coll)))

(defn dir
  "Find all descendents of path with one or more optional filter functions."
  [path & filters]
  (apply multi-filter
         (concat [(let [path-file (io/as-file path)]
                    (for [f (file-seq path-file)]
                      (.getPath f)))]
                 filters)))

(defn discard
  "Return nil. Useful at the REPL for discarding large results of functions called
primarily for side effects."
  [& args]
  nil)

(defn boolean?
  "Determine if something is a strict boolean value (true/false)."
  [x]
  (case x
    true  true
    false true
    false))

(defn vconcat
  "Like concat, but ensure vector output."
  [& args]
  (vec (apply concat args)))

(defn vdistinct
  "Like distinct, but ensure vector output."
  [& args]
  (vec (apply distinct args)))

(defn vrange [& args]
  "Like range, but ensure vector output."
  (vec (apply range args)))

(defn vmap [& args]
  "Like map, but ensure vector output."
  (vec (apply map args)))

(defn vfilter [& args]
  "like filter, but ensure vector output."
  (vec (apply filter args)))

(defn vflatten [& args]
  "Like flatten, but ensure vector output."
  (vec (apply flatten args)))

(defn vrepeat [& args]
  "Like repeat, but ensure vector output"
  (vec (apply repeat args)))

(defn M
  "Shorthand for getting metadata from an object."
  ([x key]
   (M x key nil))
  ([x key not-found-val]
   (get (meta x) key not-found-val)))


(defn exc-get
  "Throw an exception if key is not found."
  [m k]
  (let [result (get m k :EXC-not-found)]
    (if (= result :EXC-not-found)
      (throw (new Exception (str "Key '" k "' not found.")))
      result)))

(defn no-nil?
  "Return true if xs contains no nils. Assumes that xs is sequential."
  [xs]
  (if (seq xs)
    (if (nil? (first xs))
        false
        (recur (next xs)))
    true))

(defn make-vector
  "If x is a scalar value, wrap in a vector.  If sequential, ensure vector (vec)."[x]
  (if (sequential? x)
    (vec x)
    [x]))

(defn same-type
  "Assumes that xs is sequential. If all xs have same type, return that type. Else false."
  ([xs] (same-type (next xs) (type (first xs))))
  ([xs t]
   (if (not (seq xs))
     t
     (let [new-t (type (first xs))]
       (if (not= t new-t)
         false
         (recur (next xs) t))))))

