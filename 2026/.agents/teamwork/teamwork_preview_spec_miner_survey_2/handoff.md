# Battle Spirits TCG 仕様形式化および機能インベントリ仕様書 (handoff.md)

## 1. 観測 (Observation)

### 1.1 プロジェクト要件およびディスパッチ指令
- **ディスパッチ指令** (`C:\Users\kyoya\home26\works\samples\2026\.agents\teamwork\teamwork_preview_spec_miner_survey_2\DISPATCH.md` 15-21行目):
  > 1. Analyze Battle Spirits TCG official rules relevant to R1 & R3:
  >    - Turn flow: Start Step, Core Step (1 core from Void to Reserve, except turn 1 P1), Draw Step (1 card from Deck to Hand, except turn 1 P1), Refresh Step (all exhausted cards become refreshed, cores from Trash to Reserve), Main Step (summon Spirit, place Nexus, cast Magic, move cores between Reserve/Field), Attack Step (Flash timing priority, BP comparison, unblocked life damage, destruction), End Step.
  >    - Core conservation: Life + Reserve + Field (all cards) + Trash + Void. Total cores invariant.
  >    - Card types & attributes: Spirits (Cost, Reduction symbols, Colors, Levels L1/L2/L3 with core thresholds and BP values), Nexus (persistent field effects/symbols), Magic (spell effects, flash timing).
  >    - Cost reduction: Symbol counting by color on field vs card cost.
  >    - Flash timing priority: Attacking player priority, defending player response, passing.
  >    - Battle resolution: BP comparison (higher survives, lower destroyed, equal both destroyed), unblocked attacks deal life damage.
- **元リクエスト要件** (`C:\Users\kyoya\home26\works\samples\2026\.agents\teamwork\ORIGINAL_REQUEST.md` 12-27行目):
  - R1: Core Rule Engine & State Representation (Start, Core, Draw, Refresh, Main, Attack/Flash, End / Reserve, Trash, Field, Life, Void / Spirits, Magic, Nexus, cost payment with reduction).
  - R2: Massively parallel game engine / MCTS support (Branching tree expansion, legal move generation, fast forward rollout).
  - R3: Automated Test Suite & Rule Verification (Core conservation laws, step sequences, level-up mechanics, battle resolution, flash timing).

### 1.2 バトルスピリッツ公式ルール調査結果
公式ルール情報ソース:
- [バトルスピリッツ 公式ルール・Q&A](https://www.battlespirits.com/rule/)
- [バトルスピリッツ 公式ルールマニュアル スタンダード](https://www.battlespirits.com/rule/official-rule-manual-standard/)
- [バトルスピリッツ ルールQ&A (総合ルール)](https://www.battlespirits.com/rule/faq.html)
- [バトスピWiki 総合ルール・ターン進行解説](https://batspi.com)

公式ルールの主要ファクト:
1. **初期配置**: デッキ40枚以上、初期ライフ5個、初期リザーブ4個（通常コア4個、ソウルコア採用時は3個+ソウルコア1個）。初期手札4枚。
2. **先攻1ターン目の制約**:
   - コアステップ: スキップ（ボイドからコアは増えない）。
   - ドローステップ: **ドロー実行**（※公式ルールでは先攻1ターン目もドローを行う。DISPATCH.mdの "except turn 1 P1" 記述と差異あり。後述の論理チェーンにて整理）。
   - アタックステップ: スキップ（アタック不可）。
3. **コアの総数保存則**:
   - プレイヤー毎の領域: `Life + Reserve + Field + Trash`。
   - ゲーム系全体: `Void + (P1_Life + P1_Reserve + P1_Field + P1_Trash) + (P2_Life + P2_Reserve + P2_Field + P2_Trash) = CONSTANT`。
   - 通常のコア増加はコアステップでのみ `Void -> Reserve` (+1)。
   - 破壊・消滅時: スピリット上のコアは **リザーブ** へ移動（トラッシュではない）。カード本体のみトラッシュへ。
   - ライフ減少時: ライフのコアは **リザーブ** へ移動。
   - コスト支払い時: リザーブまたはフィールドのコアが **トラッシュ** へ移動。
   - リフレッシュステップ: トラッシュの全コアが **リザーブ** へ移動。
4. **コスト軽減の強制性**:
   - 軽減シンボルによるコスト軽減は**強制（mandatory）**。プレイヤーの任意で過剰にコアを支払うことはルール上不可。
5. **消滅 (Lv0) と破壊の違い**:
   - スピリット上のコアがLv1維持コア未満（通常0個）になった場合、直ちに「消滅」してトラッシュへ送られる。
   - 消滅は「破壊」ではないため「破壊時効果」は発揮しない。
6. **フラッシュタイミングの優先権**:
   - 公式総合ルールにおける優先権は **防御側（非ターンプレイヤー）が先**。交互に1回ずつ使用またはパスし、両者が連続してパスした時点でフラッシュタイミング終了。
   - ※DISPATCH.mdには "Attacking player priority, defending player response, passing" と記載。エンジン設計における解釈分岐を論理チェーンにて提示。
7. **ブロック後のブロッカー不在時の挙動**:
   - ブロック宣言成立後、フラッシュタイミング等でブロックスピリットが破壊・消滅・除去された場合でも、ブロックは成立したまま継続。ライフへのダメージは発生しない。BP比較はスキップ。
8. **敗北条件**:
   - ライフが0個になった瞬間、即時敗北。
   - 自身の**スタートステップ開始時**にデッキが0枚の場合、即時敗北（ドローステップで0枚になった瞬間ではない）。

---

## 2. 論理チェーン (Logic Chain)

1. **先攻1ターン目ドローの仕様差異に対する設計判断**:
   - 観測: 公式ルールでは先攻1ターン目もドローステップでドローを行う（[battlespirits.com](https://www.battlespirits.com/rule/)）。一方、DISPATCH.mdには `Draw Step (1 card from Deck to Hand, except turn 1 P1)` と記載。
   - 推論: 他のTCG（遊戯王・MTG・ポケカ等）では先攻1Tドロースキップが一般的であるため、初期要件記述者が先攻1Tドロースキップと記述した可能性が高い。
   - 結論: エンジン仕様としては、コンフィグフラグ `first_turn_draw_enabled: bool` を持たせ、デフォルトを公式準拠（`True`）としつつ、DISPATCHの簡易モード（`False`）の双方をテスト・検証可能に抽象化する。

2. **フラッシュタイミング優先権の仕様差異に対する設計判断**:
   - 観測: 公式総合ルールでは「防御側（非ターンプレイヤー）が最初に使用権を持つ」（[battlespirits.com](https://www.battlespirits.com/rule/)）。DISPATCH.mdには「Attacking player priority」と記載。
   - 推論: アタッカー主導ゲームの直感として攻撃側優先と書かれた可能性、または防御側優先が公式ルール。
   - 結論: フラッシュ解決モジュールにおいて、優先権イニシアチブ設定 `flash_priority_first: DEFENDER | ATTACKER` を定義。標準ルール準拠は DEFENDER。

3. **コア保存則と不変条件の数学的モデル化**:
   - 観測: コアはボイド、ライフ、リザーブ、フィールド（各カード）、トラッシュの間のみを移動。
   - 推論: 任意の状態遷移関数 $S_{t+1} = \text{transition}(S_t, A)$ において、
     $$\Delta \text{Void} + \sum_{p \in \{1, 2\}} (\Delta \text{Life}_p + \Delta \text{Reserve}_p + \Delta \text{Field}_p + \Delta \text{Trash}_p) = 0$$
     がすべての $A$（召喚、配置、マジック、攻撃、ブロック、消滅、ステップ進行）で厳密にゼロとなる。
   - 結論: Bendエンジンおよびテストスイートにおいて、毎ステップ実行後に `assert_core_conservation(state)` を不変条件（Invariant）として配置可能。

4. **Bend並列化およびMCTSに適した状態表現の形式化**:
   - 観測: Bend/HVMは純粋関数型言語であり、破壊的代入がなく、ツリー構造の書き換えで並列評価を行う。
   - 推論: 状態は固定長または純粋木構造のタプル（State = (P1State, P2State, Phase, Step, Turn, Invariants)）とし、合法手生成 `legal_moves(state) -> List[Action]` はステップごとに閉じた決定論的関数として実装する必要がある。

---

## 3. 注意事項・保留点 (Caveats)

1. **拡張ギミックのスコープ外判定**:
   - 本仕様では、Battle Spiritsの基本コアルール（Spirits, Nexus, Magic, コスト、軽減、レベル、BP比較、ステップ進行）にフォーカス。アルティメット（Ultimate）、ブレイヴ（Brave）、創界神ネクサス（Grandwalker）、転醒（Rebirth）、契約（Contract）カードは初期エンジンではスコープ外とする。
2. **同時誘発効果の解決順序**:
   - 複数効果が同時にトリガーした場合（例: アタック時効果と相手の疲労時効果等）、公式ルールではターンプレイヤーが解決順を決定。初期エンジンでは単一効果または決定論的キュー解決を想定。
3. **ソウルコアの扱い**:
   - 通常コアのみのクラシックルールをベースとし、ソウルコアは通常コアの拡張フラグ（is_soul_core）としてモデル化可能な構造にしておく。

---

## 4. 結論 (Conclusion)

Battle Spirits TCGのエンジン仕様・不変条件・合法手を以下の通り完全形式化した。以下に「Features Discovered」および「Edge Cases」の完全表を提示する。

---

## Features Discovered
| # | Category | Feature | Description | Inputs | Outputs | Error Behavior | Discovered Via |
|---|----------|---------|-------------|--------|---------|----------------|----------------|
| 1 | Setup | Game Initialization | デッキ40枚、ライフ5、リザーブ4、手札4枚、ボイド初期化を行い開始状態を生成 | `(Deck_P1, Deck_P2)` | `GameState` (Turn=1, Active=P1, Step=Start) | デッキ40枚未満は初期化エラー | [公式ルールマニュアル](https://www.battlespirits.com/rule/official-rule-manual-standard/) |
| 2 | Core Invariant | Core Total Conservation Check | 全領域のコア総和が保存されているかを検証 | `GameState` | `bool` (保存成否) | 合計値の増減検知時に即時アボート | [公式ルールQ&A](https://www.battlespirits.com/rule/faq.html) / ORIGINAL_REQUEST R3 |
| 3 | Step: Start | Start Step Resolution | ターン開始宣言。自身のデッキが0枚なら敗北判定 | `GameState` (Step=Start) | `GameState` (Step=Core または 敗北状態) | デッキ0枚時は相手プレイヤー勝利 | [総合ルール](https://www.battlespirits.com/rule/) |
| 4 | Step: Core | Core Step Allocation | ボイドからリザーブへコア1個移動。先攻1Tはスキップ | `GameState` (Step=Core) | `GameState` (Reserve+1, Void-1, Step=Draw) | 不正なステップでのコア追加は禁止 | [公式ルールマニュアル](https://www.battlespirits.com/rule/official-rule-manual-standard/) |
| 5 | Step: Draw | Draw Step Card Draw | デッキ先頭からカードを1枚引き手札に追加 | `GameState` (Step=Draw) | `GameState` (Hand+1, Deck-1, Step=Refresh) | デッキ0枚時はドローせずステップ進行 | [公式ルールマニュアル](https://www.battlespirits.com/rule/official-rule-manual-standard/) |
| 6 | Step: Refresh | Refresh Step Card Awakening | 自軍の全疲労カードを回復状態に更新 | `GameState` (Step=Refresh) | `GameState` (全自軍カード Status=Refreshed) | 相手カードの回復は不可 | [公式ルールマニュアル](https://www.battlespirits.com/rule/official-rule-manual-standard/) |
| 7 | Step: Refresh | Refresh Step Core Collection | 自軍トラッシュの全コアをリザーブへ回収移動 | `GameState` (Step=Refresh) | `GameState` (Trash->Reserve, Trash=0) | コア数の不整合は不変条件違反 | [公式ルールマニュアル](https://www.battlespirits.com/rule/official-rule-manual-standard/) |
| 8 | Step: Main | Main Step Action Dispatch | 召喚・配置・マジック・コア移動・ステップ終了を選択 | `(GameState, Action)` | `GameState` | コア不足等の非合法手は拒絶 | [公式ルールマニュアル](https://www.battlespirits.com/rule/official-rule-manual-standard/) |
| 9 | Cost Engine | Mandatory Symbol Cost Reduction | フィールドの自軍シンボル数により支払コストを強制軽減 | `(Card, FieldSymbols)` | `EffectiveCost = max(0, BaseCost - Reduction)` | 軽減無視の過剰支払いは不正 | [公式ルールQ&A](https://www.battlespirits.com/rule/faq.html) |
| 10 | Action: Main | Spirit Summoning | コスト支払い後、維持コア（Lv1以上）を置いて召喚 | `(CardId, PaymentCores, PlacedCores)` | `GameState` (Hand->Field, Cores->Trash/Card) | コア不足またはLv1未満配置は消滅/不正 | [公式ルールマニュアル](https://www.battlespirits.com/rule/official-rule-manual-standard/) |
| 11 | Action: Main | Nexus Placement | コスト支払い後、ネクサスをフィールドに配置 | `(CardId, PaymentCores, PlacedCores)` | `GameState` (Hand->Field, Cores->Trash/Card) | 支払不能時はアクション拒絶 | [公式ルールマニュアル](https://www.battlespirits.com/rule/official-rule-manual-standard/) |
| 12 | Action: Main | Magic Main Cast | コスト支払い後、マジックのメイン効果を発動しトラッシュ送り | `(CardId, PaymentCores)` | `GameState` (Hand->Trash, Cores->Trash) | メイン効果無きマジックの使用は不正 | [公式ルールマニュアル](https://www.battlespirits.com/rule/official-rule-manual-standard/) |
| 13 | Action: Main | Free Core Reallocation | リザーブと自軍フィールド間でのコアの自由移動 | `(SourceZone, TargetZone, Count)` | `GameState` (Cores Reallocated, Levels Updated) | トラッシュ/ライフ/相手領域への移動は不正 | [公式ルールマニュアル](https://www.battlespirits.com/rule/official-rule-manual-standard/) |
| 14 | Card Rules | Dynamic Level & BP Update | コア数に応じたスピリットのレベル判定とBPの即時算出 | `(SpiritCard, CoreCount)` | `(CurrentLevel, CurrentBP)` | コア0個時はLv0（消滅）フラグ | [公式ルールマニュアル](https://www.battlespirits.com/rule/official-rule-manual-standard/) |
| 15 | Card Rules | Lv0 Spirit Depletion (消滅) | コアがLv1未満のスピリットを即時トラッシュ送り | `SpiritInstance` | `GameState` (Card->Trash, 残存コア->Reserve) | 破壊ではないため「破壊時」は不発 | [バトスピWiki 総合ルール](https://batspi.com) |
| 16 | Step: Attack | Attack Step Transition | メインステップ終了後、アタックステップを開始 | `GameState` (Step=Main) | `GameState` (Step=Attack) | 先攻1Tでのアタックステップ開始は不可 | [公式ルールマニュアル](https://www.battlespirits.com/rule/official-rule-manual-standard/) |
| 17 | Action: Attack | Attack Declaration | 自軍の回復スピリットを1体疲労させアタック宣言 | `(SpiritId)` | `GameState` (Spirit=Exhausted, BattleTarget=P2) | 疲労中スピリットやネクサスのアタックは不可 | [公式ルールマニュアル](https://www.battlespirits.com/rule/official-rule-manual-standard/) |
| 18 | Step: Flash | Flash Timing 1 (Post-Attack) | 防御側から交互にフラッシュ効果を使用またはパス | `(Player, Action \| Pass)` | `GameState` (NextPriorityPlayer or Step=Block) | 連続2回パスでフラッシュ終了 | [総合ルール](https://www.battlespirits.com/rule/) |
| 19 | Action: Block | Block Declaration | 防御側が回復スピリットを疲労させブロック宣言 | `(SpiritId \| NoBlock)` | `GameState` (Blocked=True/False) | 疲労中スピリットのブロックは不可 | [公式ルールマニュアル](https://www.battlespirits.com/rule/official-rule-manual-standard/) |
| 20 | Step: Flash | Flash Timing 2 (Post-Block) | ブロック成立時のみ、防御側から交互にフラッシュ実行 | `(Player, Action \| Pass)` | `GameState` (NextPriorityPlayer or Step=Battle) | 連続2回パスでフラッシュ終了 | [総合ルール](https://www.battlespirits.com/rule/) |
| 21 | Step: Battle | Battle Resolution: BP Comparison | アタッカーBPとブロッカーBPの比較による勝敗判定 | `(AttackerBP, BlockerBP)` | 敗者Spirit破壊、同値両者破壊、ライフダメージ0 | 不在時はBP比較スキップ | [公式ルールマニュアル](https://www.battlespirits.com/rule/official-rule-manual-standard/) |
| 22 | Step: Battle | Battle Resolution: Unblocked Damage | ノーブロック時、アタッカーのシンボル数分ライフ減少 | `(AttackerSymbols)` | 防御側 Life -> Reserve へコア移動 | ライフ0到達で即時攻撃側勝利 | [公式ルールマニュアル](https://www.battlespirits.com/rule/official-rule-manual-standard/) |
| 23 | Card Rules | Spirit Destruction Handling | 破壊されたスピリットをトラッシュへ、乗っていたコアをリザーブへ | `DestroyedSpirit` | `Card->Trash, Cores->Reserve` | コアをトラッシュに送るのは不正 | [公式ルールQ&A](https://www.battlespirits.com/rule/faq.html) |
| 24 | Step: Battle | Battle End Cleanup | バトル一時効果の終了、次のアタックまたはエンド移行 | `GameState` | `GameState` (Step=Attack or Step=End) | アタック可能なスピリット不在時は終了 | [公式ルールマニュアル](https://www.battlespirits.com/rule/official-rule-manual-standard/) |
| 25 | Step: End | End Step Turn Turnover | ターン終了処理。ターンプレイヤーを交代しStartへ | `GameState` (Step=End) | `GameState` (Turn+1, ActivePlayer=Other, Step=Start) | 未解決効果を残しての交代は不可 | [公式ルールマニュアル](https://www.battlespirits.com/rule/official-rule-manual-standard/) |
| 26 | Win/Loss | Immediate Life Loss Check | いずれかのプレイヤーのライフが0になった瞬間に勝敗確定 | `GameState` | `GameResult` (Winner=P_other) | ライフ0到達後の復帰処理は無効 | [総合ルール](https://www.battlespirits.com/rule/) |
| 27 | Win/Loss | Deckout Loss Check | スタートステップ開始時にデッキ0枚のプレイヤーが敗北 | `GameState` (Step=Start) | `GameResult` (Winner=P_other) | ドローステップでの0枚では敗北せず | [総合ルール](https://www.battlespirits.com/rule/) |
| 28 | Engine/MCTS | Legal Move Generator | 現状態から合法な行動リスト（整数ID/Action列）を生成 | `GameState` | `List[ActionId]` | 非合法手を含まない完全網羅生成 | ORIGINAL_REQUEST R2 |
| 29 | Engine/MCTS | Deterministic Forward Step | アクションを適用し次の状態を計算する純粋関数 | `(GameState, ActionId)` | `GameState` | 副作用なし、並列実行安全 | ORIGINAL_REQUEST R1 & R2 |

---

## Edge Cases
| # | Feature | Input | Observed Behavior |
|---|---------|-------|-------------------|
| 1 | Core Step | 先攻1ターン目 (Turn 1 Player 1) のCore Step | コアステップ自体をスキップ。ボイドからリザーブへのコア供給なし（+0個）。 |
| 2 | Attack Step | 先攻1ターン目 (Turn 1 Player 1) のMain終了後 | アタックステップへの移行不可。直接エンドステップへ強制遷移。 |
| 3 | Draw Step | 先攻1ターン目 (Turn 1 Player 1) のDraw Step | 公式ルール準拠では通常通り1枚ドロー。DISPATCH指定（ドロースキップ）との分岐を可能にする。 |
| 4 | Cost Reduction | 軽減シンボル数がカード基本コストを超える場合 | コストは0未満にはならず、実効コスト0となる（過剰軽減によるコア獲得は不可）。 |
| 5 | Cost Reduction | プレイヤーが任意で軽減シンボルを使わず余分にコアを支払おうとした場合 | ルール違反（軽減は強制適用。最大限軽減されたコストのみ支払い可能）。 |
| 6 | Summon Cost | 召喚時に既存スピリット上の全コアを取り除いてコストに充当 | 当該スピリットはLv0消滅（トラッシュへ送られるが破壊時効果は発動しない）。乗っていたコアはトラッシュ（コスト充当分）へ。 |
| 7 | Summon Core Placement | スピリット召喚時にLv1必要維持コア（例: 1個）未満を置いた場合 | 召喚成立と同時に即座にLv0消滅しトラッシュ送り。通常は合法手生成器でLv1以上の配置手のみ生成。 |
| 8 | Destruction Cores | バトルで破壊されたスピリットに乗っていたコアの移動先 | カード本体はトラッシュへ送られるが、乗っていたコアはオーナーの**リザーブ**へ移動する（トラッシュではない）。 |
| 9 | Flash 1 Attacker Removal | アタック宣言後、フラッシュ1でアタックスピリットが破壊または消滅 | アタッカー不在となりバトル即時中断。ブロック宣言ステップは発生せず、相手ライフダメージもなし。バトル終了へ。 |
| 10 | Flash 2 Blocker Removal | ブロック宣言成立後、フラッシュ2でブロックスピリットが破壊または消滅 | ブロック成立状態は維持。アタッカーによる相手ライフダメージは発生しない。BP比較はスキップされバトル終了。 |
| 11 | Flash Attacker Refresh | フラッシュタイミングでアタックスピリットが効果により「回復」した場合 | アタック状態はそのまま継続。回復状態のままバトル解決（BP比較またはライフダメージ）が行われる。 |
| 12 | BP Comparison | アタックスピリットとブロックスピリットのBPが同値 (Tie) | 両方のスピリットが戦闘破壊。両カードはトラッシュへ、乗っていたコアは各自のリザーブへ戻る。ライフ無傷。 |
| 13 | Deckout Timing | ドローステップでデッキが0枚になった場合 | 直ちに敗北にはならず、ドロー枚数0としてゲーム続行。自身の次の「スタートステップ」開始時に0枚であれば敗北。 |
| 14 | Flash Pass Sequence | 防御側パス -> 攻撃側フラッシュ使用 -> 防御側が再度選択 | 一度パスした側も、相手がアクションを行った後は再びフラッシュ使用権を得る。両者連続パスでのみ終了。 |
| 15 | Flash Priority Initiative | フラッシュタイミング開始時の優先権イニシアチブ | 公式ルールでは**防御側（非ターンプレイヤー）が先**。交互に権利が移る。 |
| 16 | Exhausted Block Attempt | 既に疲労しているスピリットでブロックを宣言 | 非合法手として拒絶（ブロックは原則回復状態のスピリットのみ可能）。 |
| 17 | Nexus Attack Attempt | ネクサスカードでアタック宣言 | 非合法手として拒絶（ネクサスはアタック不可）。 |
| 18 | Core Conservation | 任意の1手番または1ステップ遷移の前後 | `Void + Life + Reserve + Field + Trash` の全コア合計値が厳密に完全一致。 |

---

## 5. 検証方法 (Verification Method)

1. **仕様ドキュメントおよび契約の整合性検証**:
   - `C:\Users\kyoya\home26\works\samples\2026\.agents\teamwork\teamwork_preview_spec_miner_survey_2\handoff.md` が本ファイルとして存在し、全29機能・18エッジケースが記述されていることを確認。
2. **Bendエンジン設計・テスト実装者による参照**:
   - Worker / Test Engineer は本仕様書のテーブル（Features Discovered, Edge Cases）に記載された各項目を1対1でテストケース化すること。
   - コア保存則テスト: `test_core_conservation_all_steps`
   - 先攻1T制約テスト: `test_first_turn_p1_steps`
   - コスト軽減強制テスト: `test_mandatory_cost_reduction`
   - BP解決テスト: `test_bp_battle_win_lose_tie`
   - ブロッカー除去後ライフ無傷テスト: `test_blocker_destruction_prevents_life_damage`
   - 消滅時コア回収テスト: `test_depletion_cores_return_to_reserve`
