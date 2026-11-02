---- MODULE Counter ----
(* AtomicInteger / synchronized による安全なカウンタのモデル。
   increment は read-modify-write を1つの原子ステップとして実行するため、
   他プロセスの割り込みを受けない。 *)
EXTENDS Naturals

CONSTANT NumProcs

ASSUME NumProcs \in Nat \ {0}

VARIABLES count, pc

vars == <<count, pc>>

Procs == 1..NumProcs

Init ==
    /\ count = 0
    /\ pc = [p \in Procs |-> "increment"]

Increment(p) ==
    /\ pc[p] = "increment"
    /\ count' = count + 1
    /\ pc' = [pc EXCEPT ![p] = "done"]

Next == \E p \in Procs : Increment(p)

Spec == Init /\ [][Next]_vars /\ WF_vars(Next)

TypeOK == count \in Nat

AllDone == \A p \in Procs : pc[p] = "done"

FinalCountCorrect == AllDone => count = NumProcs

====
