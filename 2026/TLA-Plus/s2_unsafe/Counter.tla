---- MODULE Counter ----
(* count++ の非アトミックなカウンタのモデル。
   read（count を temp に読む）と write（temp+1 を count に書く）が
   別々のステップに分かれているため、他プロセスの読み書きが
   その間に割り込み、更新が失われる（lost update）ことがある。 *)
EXTENDS Naturals

CONSTANT NumProcs

ASSUME NumProcs \in Nat \ {0}

VARIABLES count, pc, temp

vars == <<count, pc, temp>>

Procs == 1..NumProcs

Init ==
    /\ count = 0
    /\ pc = [p \in Procs |-> "read"]
    /\ temp = [p \in Procs |-> 0]

Read(p) ==
    /\ pc[p] = "read"
    /\ temp' = [temp EXCEPT ![p] = count]
    /\ pc' = [pc EXCEPT ![p] = "write"]
    /\ UNCHANGED count

Write(p) ==
    /\ pc[p] = "write"
    /\ count' = temp[p] + 1
    /\ pc' = [pc EXCEPT ![p] = "done"]
    /\ UNCHANGED temp

Next == \E p \in Procs : Read(p) \/ Write(p)

Spec == Init /\ [][Next]_vars /\ WF_vars(Next)

TypeOK == count \in Nat

AllDone == \A p \in Procs : pc[p] = "done"

FinalCountCorrect == AllDone => count = NumProcs

====
