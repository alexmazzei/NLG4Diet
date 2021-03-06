(ns nlg-for-diet.core
  (:require [clojure.math.combinatorics :as combo])
  (:require [random-seed.core :refer :all]) ;For pseudo-random seed
  (:refer-clojure :exclude [rand rand-int rand-nth])
  (:gen-class))

(import
 [simplenlg.framework]
 [simplenlgl.lexicon]
 [simplenlg.lexicon.italian]
 [simplenlg.realizer]
 [simplenlg.phrasespec]
 [simplenlg.features] )



;;seed
(set-random-seed! 42)




(defn create-explanation
  "If there is a i1, cut all but i1. If there is a i2, cut all but
  i2. Else, if it is compatible, order c3>c2>c1"
  [main-answer out-reasoner]
  (cond
    (= main-answer :strong-not-compatible)
    (map (fn [x] (hash-map :macro-nut (first x)
                           :cat (:cat (second x))
                           :dev (:dev (second x))))
         (filter (fn [[k v]] (= (:cat v) :i1) ) out-reasoner) )
    (= main-answer :weak-not-compatible)
    (map (fn [x] (hash-map :macro-nut (first x)
                           :cat (:cat (second x))
                           :dev (:dev (second x))))
         (filter (fn [[k v]] (= (:cat v) :i2) ) out-reasoner) )
    (= main-answer :compatible)
    (map (fn [x] (hash-map :macro-nut (first x)
                           :cat (:cat (second x))
                           :dev (:dev (second x))))
         (sort-by #(:cat (val %1)) #(compare %1 %2) out-reasoner))
    ) )


(defn extract-categories
  ""
  [out-reasoner]
  (map #(:cat %1) (map val out-reasoner)))

(defn compute-main-answer-class
  "Produce a classification of the kind of aggregation, that
  is: :compatible :strong-not-compatible :weak-not-compatible"
  [out-reasoner]
  (cond
    (some #{:i1} (extract-categories out-reasoner)) :strong-not-compatible
    (some #{:i2} (extract-categories out-reasoner)) :weak-not-compatible
    (= (extract-categories out-reasoner) '(:c3 :c3 :c3)) :perfect
    :else :compatible) )

(defn compute-aggregation-case
  "Produce a classification of the kind of aggregatin, that
  is: :compatible :strong-not-compatible :weak-not-compatible"
  [out-reasoner]
  (cond

    (= (sort (extract-categories out-reasoner)) '(:i1 :i1 :i1)) -6
    (= (sort (extract-categories out-reasoner)) '(:i1 :i1 :i2)) -5
    (= (sort (extract-categories out-reasoner)) '(:i1 :i2 :i2)) -4
    (some #{:i1} (extract-categories out-reasoner))             -3
    (= (sort (extract-categories out-reasoner)) '(:i2 :i2 :i2)) -2
    (some #{:i2} (extract-categories out-reasoner))            -1
    (= (extract-categories out-reasoner)        '(:c1 :c1 :c1)) 1
    (= (sort (extract-categories out-reasoner)) '(:c1 :c1 :c2)) 2
    (= (sort (extract-categories out-reasoner)) '(:c1 :c1 :c3)) 3
    (= (sort (extract-categories out-reasoner)) '(:c1 :c2 :c2)) 4
    (= (sort (extract-categories out-reasoner)) '(:c1 :c3 :c3)) 5
    (= (sort (extract-categories out-reasoner)) '(:c1 :c2 :c3)) 6
    (= (extract-categories out-reasoner)        '(:c2 :c2 :c2)) 7
    (= (sort (extract-categories out-reasoner)) '(:c2 :c2 :c3)) 8
    (= (sort (extract-categories out-reasoner)) '(:c2 :c3 :c3)) 9
    (= (extract-categories out-reasoner)        '(:c3 :c3 :c3)) 10
    :else 0) )



(defn text-planner
  "Produce a text-plan in the form of triple: (1) answer, (2)
  explanation, (3) suggestion."
  [out-reasoner]
  (let
      [main-answer (compute-main-answer-class out-reasoner)
       explanation (create-explanation main-answer out-reasoner)
       suggestion  (rand-nth ["Pasta al pomodoro" "Pollo arrostito" "Minestrone"])]
    {:out-reasoner out-reasoner
     :main-answer main-answer
     :explanation explanation
     :suggestion suggestion } 
    ) )




(defn sentence-planner
  "Produce a sequence of sentence-plans. Each sentence-plan, that will
  be realized by simplenlg-it, so it is isomorphic to simplenlg input
  format.  Initially, we suppose: 1 sentence for main-answer, 3
  sentences for car-lip-pro respectively, 1 sentence for the suggestion."
  [text-plan]  
  (cond
    (= (:main-answer text-plan) :perfect)
    [{:type :exclamative :content :perfect-choice}
     {:type :copula :sub :dish :pred :perfect}]

    (= (:main-answer text-plan) :strong-not-compatible)
    (vec
     (cons
      {:type :declarative :content :very-bad}
      (map (fn [x] (hash-map :type :copula 
                             :sub :dish 
                             :pred (cond (= (:dev x) :+) :rich (= (:dev x) :-) :poor)  
                             :mod :too-much 
                             :prep-in (:macro-nut x))) 
           (:explanation text-plan))))

    (= (:main-answer text-plan) :weak-not-compatible)
    (vec
     (cons
      {:type :declarative :content :bad}
      (map (fn [x] (hash-map :type :copula 
                             :sub :dish 
                             :pred (cond (= (:dev x) :+) :rich (= (:dev x) :-) :poor)  
                             :prep-in (:macro-nut x))) 
           (:explanation text-plan))))

    (= (:main-answer text-plan) :compatible)
    (vec
     (cons
      {:type :declarative :content (cond
                                     (= (:cat (first (:explanation text-plan))) :c1) :good
                                     (= (:cat (first (:explanation text-plan))) :c2) :very-good
                                     (= (:cat (first (:explanation text-plan))) :c3) :perfect)}
      (map (fn [x] (hash-map :type :copula 
                             :sub :dish 
                             :pred (cond (= (:dev x) :+) :toorich (= (:dev x) :-) :poor (= (:dev x) :=) :perfect)  
                             :mod (cond ;(= (:cat x) :c1) :very
                                        (= (:cat x) :c2) :lightly) 
                             :prep-in (:macro-nut x))) 
           (:explanation text-plan))))
    ) )  


    
;;(let [aggregation-case (compute-aggregation-case (:out-reasoner ))]


    



(defn determiner
  [kind]
  (cond
    (= kind :this) "questo"
    (= kind :def) "il"
    (= kind :undef) "un"))



;;Lexicalization: based on the "ontology" of instances/concepts/relation of the domain
;;The ontology formalization and the function application is done with make-hierarchy
;;The multimethod interface allows to call different lexicalization for different classes/instances
;;https://clojure.org/reference/multimethods


(defn lexicalize
  "returns the a lexicalizezd of a concept or a complex explanation"
  ( [input] (lexicalize input "ITA" "STD") )
  ( [input language user]
   (cond
     (keyword? input) (lexicalize-concept input language user)
     (vector? input) nil
   )))





(defn lexicalize-concept
  "returns the a lexicalizezd token for the specific concept-user-language"
  [concept language user]
   (let
       [italian-standard-dictionary {:to-be ["essere" "risultare" "sembrare"] :this ["questo"] :dish ["piatto" "portata"] :choice ["scelta" "decisione" "idea" "mossa"] :good ["buono"] :very ["molto"] :very-good ["buonissima" "ottima" "eccellente" "fantastica"] :too-much ["troppo"] :perfect ["perfetto"] :car ["carboidrati"] :lip [ "lipidi" "grassi"] :pro ["proteine"] :to-go ["andare"] :rich ["ricco"] :toorich ["abbondante"] :poor ["povero"] :toopoor ["mancante"] :okay ["bene"] :lightly ["leggermente"]
        } ]
     (cond
       (and (= language "ITA") (= user "STD"))
       ;;(rand-nth
        (first
         (concept italian-standard-dictionary)))))

;;;call simplenlg-it
;; lexicons
(def lexIta (new simplenlg.lexicon.italian.ITXMLLexicon))
(def lexFoodIta (new simplenlg.lexicon.XMLLexicon "foodLexicon-ITA-01.xml"))
(def multiLexIta (new simplenlg.lexicon.MultipleLexicon "it"))
(. multiLexIta addInitialLexicon lexFoodIta)
(. multiLexIta addFinalLexicon lexIta)

(def nlgFactory (new simplenlg.framework.NLGFactory multiLexIta))
;;(def nlgFactory (new simplenlg.framework.NLGFactory lexIta))
(def realiser  (new simplenlg.realiser.Realiser))


;;; prove

(defn test-negation
  []
  (do
    (def clauseT (. nlgFactory createClause))

    ;;VPPhraseSpec verb = nlgFactory.createVerbPhrase("chase");
    ;;(def verbP (. (. nlgFactory createClause) "correre"))
    (. clauseT setVerb "correre")
    (. clauseT setSubject "Paolo")
    (. clauseT setNegated true)
    (. realiser  realiseSentence clauseT)))


(defn quasi-tree-generator
  "returns a q-tree (in SimpleNLG-IT format): it accounts for variety of
  the lexicon"
  [sentence-plan]
  (cond
    (and (= (:type sentence-plan) :exclamative)
         (= (:content sentence-plan) :perfect-choice))       
    (do
      (def clause (. nlgFactory createClause))
      (. clause setVerb (lexicalize :to-be))
      (. clause setSubject (. nlgFactory createNounPhrase (determiner :this) (lexicalize :dish)))
      (def cobj (. nlgFactory createNounPhrase (determiner :undef) (lexicalize :choice)))      
      (. cobj addPreModifier (lexicalize :very-good))
      (. clause setObject cobj)
      clause  )

    (and (= (:type sentence-plan) :declarative)
         (= (:content sentence-plan) :very-bad))       
    (do
      (def clause (. nlgFactory createClause))
      (. clause setVerb (lexicalize :to-be))
      (. clause setSubject (. nlgFactory createNounPhrase (determiner :this) (lexicalize :dish)))
      (. clause setObject (lexicalize :good))
      (. clause setNegated true)
      ;; (. clause setFeature
      ;;    (. simplenlg.features.Feature NEGATED)
      ;;    true)  
      ;; (. clause setFeature
      ;;    (. simplenlg.features.Feature TENSE)
      ;;    (. simplenlg.features.Tense PAST))
      clause   )

    (and (= (:type sentence-plan) :declarative)
         (= (:content sentence-plan) :bad))       
    (do
      (def clause (. nlgFactory createClause))
      (. clause setVerb (lexicalize :to-go))
      (. clause setSubject (. nlgFactory createNounPhrase (determiner :this) (lexicalize :dish)))
      (. clause setObject (lexicalize :okay))
      (. clause setNegated true)
      clause   )

    (and (= (:type sentence-plan) :declarative)
         (= (:content sentence-plan) :good))       
    (do
      (def clause (. nlgFactory createClause))
      (. clause setVerb (lexicalize :to-be))
      (. clause setSubject (. nlgFactory createNounPhrase (determiner :this) (lexicalize :dish)))
      (. clause setObject (lexicalize :good))      
      clause   )

    (and (= (:type sentence-plan) :declarative)
         (= (:content sentence-plan) :very-good))       
    (do
      (def clause (. nlgFactory createClause))
      (. clause setVerb (lexicalize :to-be))
      (. clause setSubject (. nlgFactory createNounPhrase (determiner :this) (lexicalize :dish)))
      (def cobj (. nlgFactory createNounPhrase (lexicalize :good)))
      (. cobj addPreModifier (lexicalize :very))
      (. clause setObject cobj)
      clause   )

    (and (= (:type sentence-plan) :copula)
         (= (:sub sentence-plan)  :dish) )
    (do
      (def clause (. nlgFactory createClause))
      (. clause setVerb (lexicalize :to-be))
      (. clause setSubject (. nlgFactory createNounPhrase (determiner :def) (lexicalize :dish)))
      (def cobj (. nlgFactory createNounPhrase (lexicalize (:pred sentence-plan))))
      (if (:mod sentence-plan) (. cobj addPreModifier (lexicalize (:mod sentence-plan))))     
      (. clause setObject cobj)
      (if (:prep-in sentence-plan)  (. clause addComplement (str "in " (lexicalize (:prep-in sentence-plan)))))
      clause  )) ) 

(defn realize-sentences-old
  "Produce a message by intefacing to simplenlg-it (tail recursion)"
  ([sentence-plans] (realize-sentences sentence-plans nil))
  ([sentence-plans acc]
  (if (empty? sentence-plans) acc
      (recur (rest sentence-plans) (cons (realize (first sentence-plans)) acc) ) ) ) )

(defn realize-old
  "Produce a message by intefacing to simplenlg-it"
  [sentence-plan]
  (cond
    (= (:type sentence-plan) :exclamative)
    (decl2exclSentence (. realiser  realiseSentence (quasi-tree-generator sentence-plan)) )
    
    :else (. realiser  realiseSentence (quasi-tree-generator sentence-plan))
    ) ) 


(defn realize-aggregated-sentences
  ""
  [sentence-plans aggregation-case]
  (let
      [quasi-trees
       (map quasi-tree-generator sentence-plans)
       ;;(def document (. nlgFactory (. createSentence (first quasi-trees)))      )
       ]
    
    (do
      (def coordS (. nlgFactory createCoordinatedPhrase))
      (doseq [qt (rest quasi-trees)]
        (. coordS addCoordinate qt))
      
      (str (. realiser  realiseSentence (nth quasi-trees 0))
           " "
           (. realiser  realiseSentence coordS) )
      )))

    

(defn realize
  "Produce a message by intefacing to simplenlg-it"
  [sentence-plan]
  (cond
    (= (:type sentence-plan) :exclamative)
    (decl2exclSentence (. realiser  realiseSentence (quasi-tree-generator sentence-plan)) )    
    :else (. realiser  realiseSentence (quasi-tree-generator sentence-plan))
    ) ) 



;;@BotConfBot BotConf@kiwifarm.it #karlok
;;

;;;Reasoner connection/simulation
(defn simulate-STP-reasoner
  "This function will produce a possible output of the diet
  reasoner (cf. anselma-mazzei-2107). The hash-map in output
  represents: car, lip, pro"
  [& args]
  (let
      [values-macronutrients (if (empty? args)
                               [{:cat :i1 :dev :+} {:cat :i1 :dev :-} {:cat :i2 :dev :+} {:cat :i2 :dev :-} {:cat :c1 :dev :+} {:cat :c1 :dev :-} {:cat :c2 :dev :+} {:cat :c2 :dev :-} {:cat :c3 :dev :=}] 
                               [{:cat :c1 :dev :+} {:cat :c1 :dev :-} {:cat :c2 :dev :+} {:cat :c2 :dev :-} {:cat :c3 :dev :=}])
       possible-selections  (combo/selections values-macronutrients 3)
       selection (rand-nth possible-selections)]
    {:car (nth selection 0) :lip (nth selection 1) :pro (nth selection 2)}))

(defn dish-suggester
  "Return a correct dish for the future on the basis of the reasoner"
  [out-reasoner]
  ;;TODO
  )


(defn -main
  "I don't do a whole lot ... yet."
  [& args]  
  ;;(println "Hello, World!")
  ;;(text-planner )
  (let [;;simulated-reasoning  {:car {:cat :c1, :dev :+}, :lip {:cat :c1, :dev :-}, :pro {:cat :c2, :dev :+}}
        simulated-reasoning (simulate-STP-reasoner)
        ;;{:car {:cat :i1, :dev :+}, :lip {:cat :i1, :dev :+}, :pro {:cat :i1, :dev :+}}
        ]
    (do
      (print  simulated-reasoning)
      (realize-aggregated-sentences     
       (sentence-planner (text-planner simulated-reasoning))
       (compute-aggregation-case simulated-reasoning)
       ))))
                      
  

;; https://github.com/JoshRosen/cmps140_creative_cooking_assistant



;;auxiliary functions
(defn decl2exclSentence
  "Replace the last . with !"
  [sentence]
  (clojure.string/replace sentence #"\.$" "!") )



;; Basic aggregation mechanisms:
;; –  Simple conjunction
;; –  Conjunction via shared participants (ellipsis)
;; –  Conjunction via shared structure (set introduction)
;; –  Embedding

;;Shared: sub, obj, sub-obj
