---- MODULE BSChoices ----
\* AI: バトルスピリッツ 局面別 選択肢列挙仕様 (TLA+)。AI生成。
\* 根拠: BSRuleEngineServer/engine/src/Rules.kt, Model.kt の実装。公式総合ルールPDFとは未照合。
EXTENDS Integers, FiniteSets, TLC

Min(a, b) == IF a < b THEN a ELSE b
Max(a, b) == IF a > b THEN a ELSE b

RECURSIVE SumF(_, _)
SumF(f, S) == IF S = {} THEN 0 ELSE LET x == CHOOSE x \in S : TRUE IN f[x] + SumF(f, S \ {x})

\* ---------------- サンプル局面 ----------------
\* 手札: kind=spirit|nexus|magic, cost, red=軽減シンボル色集合, timing=magic時 main|flash
Hand == { [id|->"S_cost2", kind|->"spirit", cost|->2, red|->{"R"}, timing|->"-"],
          [id|->"S_cost4", kind|->"spirit", cost|->4, red|->{"R"}, timing|->"-"],
          [id|->"N_cost2", kind|->"nexus",  cost|->2, red|->{"R"}, timing|->"-"],
          [id|->"M_main1", kind|->"magic",  cost|->1, red|->{},    timing|->"main"],
          [id|->"M_flash", kind|->"magic",  cost|->1, red|->{},    timing|->"flash"] }
\* 自フィールド: cores=コア数, sym=シンボル数, col=色, exh=疲労
MyField == { [id|->"f1", cores|->2, sym|->1, col|->"R", exh|->FALSE],
             [id|->"f2", cores|->1, sym|->1, col|->"R", exh|->TRUE ] }
OppField == { [id|->"o1", cores|->2, sym|->1, col|->"B", exh|->FALSE],
              [id|->"o2", cores|->1, sym|->1, col|->"B", exh|->TRUE ] }
Reserve == 3

\* ---------------- コア供給元・支払い・配置 ----------------
Src == {"R"} \cup { f.id : f \in MyField }
Avail == [s \in Src |-> IF s = "R" THEN Reserve ELSE (CHOOSE f \in MyField : f.id = s).cores]
Dists(total, av) == { d \in [Src -> 0..Reserve+4] : (\A s \in Src : d[s] <= av[s]) /\ SumF(d, Src) = total }
Sub(av, d) == [s \in Src |-> av[s] - d[s]]
Active == { f \in MyField : ~f.exh }

\* 軽減: 疲労させたスピリットのシンボル(カードの軽減色と同色のみ)分だけコスト減
Reduction(c, X) == SumF([x \in X |-> IF x.col \in c.red THEN x.sym ELSE 0], X)
MinPlace(c) == IF c.kind = "spirit" THEN 1 ELSE 0

PlaceMax(c, d1) == IF c.kind = "magic" THEN 0 ELSE SumF(Sub(Avail, d1), Src)
PlaceDists(c, d1) == UNION { Dists(n, Sub(Avail, d1)) : n \in MinPlace(c)..PlaceMax(c, d1) }

\* カード使用の全選択肢 = 疲労軽減集合 X × 支払い元内訳 d1 × 配置元内訳 d2
PlaysX(c, verb, X) ==
  UNION { { [act|->verb, card|->c.id, exhaust|->{x.id : x \in X}, pay|->d1, place|->d2] : d2 \in PlaceDists(c, d1) }
          : d1 \in Dists(Max(0, c.cost - Reduction(c, X)), Avail) }
Plays(c, verb) == UNION { PlaysX(c, verb, X) : X \in SUBSET Active }

PlayVerb(c) == CASE c.kind = "spirit" -> "Summon" [] c.kind = "nexus" -> "PlaceNexus" [] OTHER -> "CastMagic"
MainPlays  == UNION { Plays(c, PlayVerb(c))  : c \in { h \in Hand : h.kind # "magic" \/ h.timing = "main" } }
FlashPlays == UNION { Plays(c, "CastFlash")  : c \in { h \in Hand : h.kind = "magic" /\ h.timing = "flash" } }

CoreMovesFrom(s) == UNION { { [act|->"MoveCore", from|->s, to|->u, n|->k] : k \in 1..Avail[s] } : u \in Src \ {s} }
CoreMoves == UNION { CoreMovesFrom(s) : s \in Src }

\* ---------------- 局面別 選択肢 ----------------
Steps == {"START","CORE","DRAW","REFRESH","MAIN","ATTACK_DECLARATION","FLASH_TIMING","BLOCK_DECLARATION","MAIN_2","END"}

Choices(step) ==
  CASE step \in {"START","CORE","DRAW","REFRESH","END"} -> { [act|->"AutoAdvance"] }
    [] step = "MAIN" -> MainPlays \cup CoreMoves \cup { [act|->"GoAttackStep"] }
    [] step = "ATTACK_DECLARATION" -> { [act|->"Attack", attacker|->f.id] : f \in Active } \cup { [act|->"EndAttackStep"] }
    [] step = "FLASH_TIMING" -> { [act|->"PassFlash"] } \cup FlashPlays
    \* 防御側(相手)の選択: 回復状態のスピリット1体でブロック or しない (+フラッシュ)
    [] step = "BLOCK_DECLARATION" -> { [act|->"Block", blocker|->o.id] : o \in { q \in OppField : ~q.exh } } \cup { [act|->"NoBlock"] } \cup FlashPlays
    [] step = "MAIN_2" -> MainPlays \cup CoreMoves \cup { [act|->"EndTurn"] }

ASSUME \A s \in Steps : PrintT(<<s, Cardinality(Choices(s))>>)
ASSUME \A s \in {"ATTACK_DECLARATION","FLASH_TIMING","BLOCK_DECLARATION"} : \A o \in Choices(s) : PrintT(<<s, o>>)
ASSUME \A o \in Choices("MAIN") : o.act \in {"GoAttackStep","MoveCore"} => PrintT(<<"MAIN", o>>)
ASSUME \A o \in Choices("MAIN") : o.act \in {"Summon","PlaceNexus","CastMagic"} => PrintT(<<"MAIN", o.act, o.card, o.exhaust, o.pay, o.place>>)

VARIABLE x
Init == x = 0
Next == x' = x
Spec == Init /\ [][Next]_x
====
