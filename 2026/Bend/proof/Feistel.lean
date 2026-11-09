-- 🤖[AI生成: feistel.kts の Feistel 置換が [0, 2^(2h)) 上の全単射であること、cycle walking の出力が [0, n) の置換であることの形式証明。Mathlib 不使用。PRP 性(Luby–Rackoff)は仮定付きの定理で、ここでは対象外]

/-! ## cycle walking: [0,M) 上の単射 E を [0,n) に制限する一般論 -/

/-- E を k 回適用 -/
def it (E : Nat → Nat) : Nat → Nat → Nat
  | 0, x => x
  | k + 1, x => E (it E k x)

theorem it_succ' (E : Nat → Nat) (k x : Nat) : it E (k + 1) x = it E k (E x) := by
  induction k with
  | zero => rfl
  | succ k ih => simp only [it] at *; rw [ih]

theorem it_lt (E : Nat → Nat) (M : Nat) (hE : ∀ x, x < M → E x < M) (k x : Nat)
    (hx : x < M) : it E k x < M := by
  induction k with
  | zero => exact hx
  | succ k ih => exact hE _ ih

/-- 単射なら左から E を剥がせる: E^i x = E^j y, i ≤ j → x = E^(j-i) y -/
theorem it_cancel (E : Nat → Nat) (M : Nat) (hE : ∀ x, x < M → E x < M)
    (hinj : ∀ x y, x < M → y < M → E x = E y → x = y) :
    ∀ i j x y, i ≤ j → x < M → y < M → it E i x = it E j y → x = it E (j - i) y := by
  intro i
  induction i with
  | zero => intro j x y _ _ _ h; simpa [it] using h
  | succ i ih =>
    intro j x y hij hx hy h
    obtain ⟨j, rfl⟩ : ∃ j', j = j' + 1 := ⟨j - 1, by omega⟩
    simp only [it] at h
    have h' := hinj _ _ (it_lt E M hE i x hx) (it_lt E M hE j y hy) h
    rw [ih j x y (by omega) hx hy h']
    congr 1
    omega

/-- 鳩の巣: 重複なしで全要素 < M のリストは長さ ≤ M -/
theorem nodup_length_le (M : Nat) (l : List Nat) (hl : l.Nodup) (h : ∀ x ∈ l, x < M) :
    l.length ≤ M := by
  simpa using hl.length_le_of_subset (l₂ := List.range M) (fun x hx => List.mem_range.2 (h x hx))

/-- 軌道は M 回以内に出発点へ戻る -/
theorem orbit_return (E : Nat → Nat) (M : Nat) (hE : ∀ x, x < M → E x < M)
    (hinj : ∀ x y, x < M → y < M → E x = E y → x = y) (x : Nat) (hx : x < M) :
    ∃ d, 0 < d ∧ d ≤ M ∧ it E d x = x := by
  apply Classical.byContradiction
  intro hne
  have hnd : ((List.range (M + 1)).map (fun i => it E i x)).Nodup := by
    unfold List.Nodup
    rw [List.pairwise_map]
    apply List.pairwise_lt_range.imp_of_mem
    intro i j _ hj hij heq
    simp only [List.mem_range] at hj
    have hc := it_cancel E M hE hinj i j x x (by omega) hx hx heq
    exact hne ⟨j - i, by omega, by omega, hc.symm⟩
  have hl := nodup_length_le M _ hnd (by
    intro y hy
    simp only [List.mem_map, List.mem_range] at hy
    obtain ⟨i, _, rfl⟩ := hy
    exact it_lt E M hE i x hx)
  simp only [List.length_map, List.length_range] at hl
  omega

/-- cycle walking 1 要素分: E を [0,n) に入るまで適用(最大 fuel+1 回)。
    feistel.kts の `var x = e(i); while (x >= n) x = e(x)` -/
def walk (E : Nat → Nat) (n : Nat) : Nat → Nat → Nat
  | 0, x => E x
  | f + 1, x => if E x < n then E x else walk E n f (E x)

/-- walk の結果は E^t x(t ≥ 1)で、途中の E^s x(0 < s < t)は範囲外 -/
theorem walk_spec (E : Nat → Nat) (n : Nat) :
    ∀ f x, ∃ t, 0 < t ∧ walk E n f x = it E t x ∧ ∀ s, 0 < s → s < t → n ≤ it E s x := by
  intro f
  induction f with
  | zero => intro x; exact ⟨1, by omega, rfl, fun s _ _ => by omega⟩
  | succ f ih =>
    intro x
    simp only [walk]
    split
    · exact ⟨1, by omega, rfl, fun s _ _ => by omega⟩
    · rename_i hge
      obtain ⟨t, ht, heq, hs⟩ := ih (E x)
      refine ⟨t + 1, by omega, by rw [heq, it_succ'], fun s h1 h2 => ?_⟩
      rcases Nat.lt_or_ge 1 s with h | h
      · obtain ⟨s, rfl⟩ : ∃ s', s = s' + 1 := ⟨s - 1, by omega⟩
        rw [it_succ']
        exact hs s (by omega) (by omega)
      · obtain rfl : s = 1 := by omega
        show n ≤ E x
        omega

/-- 停止性: fuel = M なら必ず [0,n) に入る(while ループが終わる根拠) -/
theorem walk_lt (E : Nat → Nat) (M n : Nat) (hE : ∀ x, x < M → E x < M)
    (hinj : ∀ x y, x < M → y < M → E x = E y → x = y) (hn : n ≤ M) (x : Nat) (hx : x < n) :
    walk E n M x < n := by
  obtain ⟨d, hd0, hdM, hdx⟩ := orbit_return E M hE hinj x (by omega)
  suffices ∀ f y d, 0 < d → d ≤ f + 1 → it E d y < n → walk E n f y < n from
    this M x d hd0 (by omega) (by rw [hdx]; exact hx)
  intro f
  induction f with
  | zero =>
    intro y d _ h1 h
    obtain rfl : d = 1 := by omega
    exact h
  | succ f ih =>
    intro y d h0 h1 h
    simp only [walk]
    split
    · assumption
    · rename_i hge
      rcases Nat.lt_or_ge 1 d with hd | hd
      · obtain ⟨d, rfl⟩ : ∃ d', d = d' + 1 := ⟨d - 1, by omega⟩
        rw [it_succ'] at h
        exact ih (E y) d (by omega) (by omega) h
      · obtain rfl : d = 1 := by omega
        exact absurd h hge

/-- 単射性: 異なる入力は異なる出力 -/
theorem walk_inj (E : Nat → Nat) (M n : Nat) (hE : ∀ x, x < M → E x < M)
    (hinj : ∀ x y, x < M → y < M → E x = E y → x = y) (hn : n ≤ M) (f x y : Nat)
    (hx : x < n) (hy : y < n) (h : walk E n f x = walk E n f y) : x = y := by
  obtain ⟨t, _, hx', hsx⟩ := walk_spec E n f x
  obtain ⟨u, _, hy', hsy⟩ := walk_spec E n f y
  rw [hx', hy'] at h
  rcases Nat.le_total t u with htu | hut
  · have hc := it_cancel E M hE hinj t u x y htu (by omega) (by omega) h
    rcases Nat.eq_zero_or_pos (u - t) with h0 | h0
    · rw [h0] at hc; exact hc
    · have hge := hsy (u - t) h0 (by omega); omega
  · have hc := it_cancel E M hE hinj u t y x hut (by omega) (by omega) h.symm
    rcases Nat.eq_zero_or_pos (t - u) with h0 | h0
    · rw [h0] at hc; exact hc.symm
    · have hge := hsx (t - u) h0 (by omega); omega

/-- 出力列: i = 0 .. n-1 を順に cycle walking -/
def cw (E : Nat → Nat) (M n : Nat) : List Nat := (List.range n).map (walk E n M)

theorem cw_nodup (E : Nat → Nat) (M n : Nat) (hE : ∀ x, x < M → E x < M)
    (hinj : ∀ x y, x < M → y < M → E x = E y → x = y) (hn : n ≤ M) : (cw E M n).Nodup := by
  unfold cw List.Nodup
  rw [List.pairwise_map]
  apply List.pairwise_lt_range.imp_of_mem
  intro i j hi hj hij h
  simp only [List.mem_range] at hi hj
  have := walk_inj E M n hE hinj hn M i j hi hj h
  omega

/-- 出力は [0,n) の置換(重複なし + 全値網羅) -/
theorem cw_perm (E : Nat → Nat) (M n : Nat) (hE : ∀ x, x < M → E x < M)
    (hinj : ∀ x y, x < M → y < M → E x = E y → x = y) (hn : n ≤ M) :
    (cw E M n).Perm (List.range n) := by
  have hd := cw_nodup E M n hE hinj hn
  have hlt : ∀ y ∈ cw E M n, y < n := by
    intro y hy
    simp only [cw, List.mem_map, List.mem_range] at hy
    obtain ⟨i, hi, rfl⟩ := hy
    exact walk_lt E M n hE hinj hn i hi
  rw [List.perm_ext_iff_of_nodup hd List.nodup_range]
  intro a
  refine ⟨fun ha => List.mem_range.2 (hlt a ha), fun ha => ?_⟩
  -- 網羅: a が出力に無ければ、長さ n の重複なし列が n-1 個の値に収まり矛盾
  apply Classical.byContradiction
  intro hna
  have hsub : cw E M n ⊆ (List.range n).erase a := by
    intro y hy
    have hya : y ≠ a := fun e => hna (e ▸ hy)
    exact (List.mem_erase_of_ne hya).2 (List.mem_range.2 (hlt y hy))
  have hl := hd.length_le_of_subset hsub
  rw [List.length_erase_of_mem ha] at hl
  simp only [cw, List.length_map, List.length_range] at hl
  have : 0 < n := by simp only [List.mem_range] at ha; omega
  omega

/-! ## Feistel 置換 -/

/-- (r * H + s) を H で割った商と余り -/
theorem divmod (H r s : Nat) (hs : s < H) : (r * H + s) / H = r ∧ (r * H + s) % H = s := by
  have hH : 0 < H := by omega
  constructor
  · rw [Nat.add_comm, Nat.add_mul_div_right s r hH, Nat.div_eq_of_lt hs, Nat.zero_add]
  · rw [Nat.add_comm, Nat.add_mul_mod_self_right, Nat.mod_eq_of_lt hs]

/-- 1 ラウンド: x = L·H + R ↦ R·H + (L xor F(R) mod H)、H = 2^h。
    feistel.kts の `((y and mask) shl h) or ((y ushr h) xor f(i, y and mask))` -/
def round (h : Nat) (F : Nat → Nat) (x : Nat) : Nat :=
  x % 2 ^ h * 2 ^ h + ((x / 2 ^ h) ^^^ (F (x % 2 ^ h) % 2 ^ h))

theorem round_lt (h : Nat) (F : Nat → Nat) (x : Nat) (hx : x < 2 ^ h * 2 ^ h) :
    round h F x < 2 ^ h * 2 ^ h := by
  have hH : 0 < 2 ^ h := Nat.two_pow_pos h
  have hr : x % 2 ^ h < 2 ^ h := Nat.mod_lt _ hH
  have hl : x / 2 ^ h < 2 ^ h := Nat.div_lt_of_lt_mul hx
  have hs := Nat.xor_lt_two_pow hl (Nat.mod_lt (F (x % 2 ^ h)) hH)
  unfold round
  have : (x % 2 ^ h + 1) * 2 ^ h ≤ 2 ^ h * 2 ^ h := Nat.mul_le_mul_right _ hr
  rw [Nat.add_mul, Nat.one_mul] at this
  omega

theorem round_inj (h : Nat) (F : Nat → Nat) (x y : Nat) (hx : x < 2 ^ h * 2 ^ h)
    (hy : y < 2 ^ h * 2 ^ h) (e : round h F x = round h F y) : x = y := by
  have hH : 0 < 2 ^ h := Nat.two_pow_pos h
  have hsx := Nat.xor_lt_two_pow (Nat.div_lt_of_lt_mul hx) (Nat.mod_lt (F (x % 2 ^ h)) hH)
  have hsy := Nat.xor_lt_two_pow (Nat.div_lt_of_lt_mul hy) (Nat.mod_lt (F (y % 2 ^ h)) hH)
  unfold round at e
  obtain ⟨qx, mx⟩ := divmod _ (x % 2 ^ h) _ hsx
  obtain ⟨qy, my⟩ := divmod _ (y % 2 ^ h) _ hsy
  have hr : x % 2 ^ h = y % 2 ^ h := by rw [← qx, ← qy, e]
  have hs := mx.symm.trans ((congrArg (· % 2 ^ h) e).trans my)
  rw [hr] at hs
  have hl : x / 2 ^ h = y / 2 ^ h := by
    have := congrArg (· ^^^ (F (y % 2 ^ h) % 2 ^ h)) hs
    simpa [Nat.xor_assoc, Nat.xor_self, Nat.xor_zero] using this
  rw [← Nat.div_add_mod x (2 ^ h), ← Nat.div_add_mod y (2 ^ h), hl, hr]

/-- ラウンド関数の列 fs を順に適用(feistel.kts では fs = [f(0,·), f(1,·), ...]) -/
def feistel (h : Nat) (fs : List (Nat → Nat)) (x : Nat) : Nat :=
  fs.foldl (fun y F => round h F y) x

theorem feistel_lt (h : Nat) (fs : List (Nat → Nat)) :
    ∀ x, x < 2 ^ h * 2 ^ h → feistel h fs x < 2 ^ h * 2 ^ h := by
  induction fs with
  | nil => intro x hx; exact hx
  | cons F fs ih => intro x hx; exact ih _ (round_lt h F x hx)

/-- ラウンド関数 F が何であっても、何ラウンドでも Feistel は単射 -/
theorem feistel_inj (h : Nat) (fs : List (Nat → Nat)) :
    ∀ x y, x < 2 ^ h * 2 ^ h → y < 2 ^ h * 2 ^ h → feistel h fs x = feistel h fs y → x = y := by
  induction fs with
  | nil => intro x y _ _ e; exact e
  | cons F fs ih =>
    intro x y hx hy e
    exact round_inj h F x y hx hy
      (ih _ _ (round_lt h F x hx) (round_lt h F y hy) e)

/-- feistel.kts の出力(first = 0)は [0,n) の置換 -/
theorem feistel_scramble_perm (h n : Nat) (fs : List (Nat → Nat)) (hn : n ≤ 2 ^ h * 2 ^ h) :
    (cw (feistel h fs) (2 ^ h * 2 ^ h) n).Perm (List.range n) :=
  cw_perm _ _ n (feistel_lt h fs) (feistel_inj h fs) hn

/-- Readme の課題: 入力 0..N(n = N + 1)で出力はすべて異なる -/
theorem readme (h N : Nat) (fs : List (Nat → Nat)) (hn : N + 1 ≤ 2 ^ h * 2 ^ h) :
    (cw (feistel h fs) (2 ^ h * 2 ^ h) (N + 1)).Nodup :=
  cw_nodup _ _ _ (feistel_lt h fs) (feistel_inj h fs) hn

#print axioms walk_lt
#print axioms cw_perm
#print axioms feistel_inj
#print axioms feistel_scramble_perm
#print axioms readme
