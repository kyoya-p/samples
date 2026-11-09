-- 🤖[AI生成: scramble.kts の LCG(法 2^k, a ≡ 1 mod 4, c 奇数)が 1 周 2^k 内で同じ値を出さないことの形式証明。Mathlib 不使用]

/-- LCG を n 回適用(mod は取らず、合同は 2^k での割り算で扱う) -/
def L (a c : Nat) : Nat → Nat → Nat
  | 0, x => x
  | n + 1, x => a * L a c n x + c

/-- S n = Σ_{i<n} a^i -/
def S (a : Nat) : Nat → Nat
  | 0 => 0
  | n + 1 => a * S a n + 1

theorem L_add (a c x i d : Nat) : L a c (i + d) x = L a c d (L a c i x) := by
  induction d with
  | zero => rfl
  | succ d ih => simp [L, ih]

theorem L_closed (a c y d : Nat) : L a c d y = a ^ d * y + c * S a d := by
  induction d with
  | zero => simp [L, S]
  | succ d ih => simp only [L, S, ih, Nat.pow_succ]; grind

theorem S_add (a m n : Nat) : S a (n + m) = a ^ m * S a n + S a m := by
  induction m with
  | zero => simp [S]
  | succ m ih => rw [← Nat.add_assoc]; simp only [S, ih, Nat.pow_succ]; grind

theorem pow_eq (a d : Nat) (ha : 1 ≤ a) : a ^ d = 1 + (a - 1) * S a d := by
  induction d with
  | zero => simp [S]
  | succ d ih =>
    obtain ⟨b, rfl⟩ : ∃ b, a = b + 1 := ⟨a - 1, by omega⟩
    simp only [Nat.add_sub_cancel, S, Nat.pow_succ] at *
    rw [ih]; grind

theorem pow_mod4 (a e : Nat) (ha : a % 4 = 1) : a ^ e % 4 = 1 := by
  induction e with
  | zero => rfl
  | succ e ih => rw [Nat.pow_succ, Nat.mul_mod, ih, ha]

/-- 奇数 u は 2^k と互いに素 -/
theorem dvd_of_odd_mul (u y k : Nat) (hu : u % 2 = 1) (h : 2 ^ k ∣ u * y) : 2 ^ k ∣ y := by
  induction k generalizing y with
  | zero => simp
  | succ k ih =>
    have h2 : 2 ∣ u * y := Nat.dvd_trans (Nat.dvd_mul_left 2 (2 ^ k)) (by rw [Nat.pow_succ] at h; exact h)
    have hy : y % 2 = 0 := by
      obtain ⟨t, rfl | rfl⟩ : ∃ t, y = 2 * t ∨ y = 2 * t + 1 := ⟨y / 2, by omega⟩
      · omega
      · exfalso
        have : (u * (2 * t + 1)) % 2 = 1 := by rw [Nat.mul_mod]; simp [hu]
        omega
    obtain ⟨t, rfl⟩ : ∃ t, y = 2 * t := ⟨y / 2, by omega⟩
    have h' : 2 ^ k ∣ u * t := by
      rw [Nat.pow_succ, show u * (2 * t) = u * t * 2 by grind] at h
      exact Nat.dvd_of_mul_dvd_mul_right (by decide) h
    rw [Nat.pow_succ, Nat.mul_comm 2 t]
    exact Nat.mul_dvd_mul_right (ih t h') 2

theorem pow_dvd_odd (n k : Nat) (hn : n % 2 = 1) : 2 ^ k ∣ n ↔ k = 0 := by
  constructor
  · intro h
    cases k with
    | zero => rfl
    | succ k =>
      have : 2 ∣ n := Nat.dvd_trans (Nat.dvd_mul_left 2 (2 ^ k)) (by rw [Nat.pow_succ] at h; exact h)
      omega
  · rintro rfl; simp

/-- 核心: a ≡ 1 (mod 4) なら 2^k ∣ S d ↔ 2^k ∣ d -/
theorem key (a : Nat) (ha : a % 4 = 1) : ∀ d k, 2 ^ k ∣ S a d ↔ 2 ^ k ∣ d := by
  intro d
  induction d using Nat.strongRecOn with
  | ind d ih =>
  intro k
  obtain ⟨e, rfl | rfl⟩ : ∃ e, d = 2 * e ∨ d = 2 * e + 1 := ⟨d / 2, by omega⟩
  · -- d = 2e
    rcases Nat.eq_zero_or_pos e with rfl | he
    · simp [S]
    have hS : S a (2 * e) = S a e * (a ^ e + 1) := by
      rw [show 2 * e = e + e by omega, S_add]; grind
    obtain ⟨u, hu, hu2⟩ : ∃ u, a ^ e + 1 = 2 * u ∧ u % 2 = 1 :=
      ⟨(a ^ e + 1) / 2, by have := pow_mod4 a e ha; omega, by have := pow_mod4 a e ha; omega⟩
    cases k with
    | zero => simp
    | succ k =>
      rw [hS, hu, Nat.pow_succ, show S a e * (2 * u) = u * S a e * 2 by grind,
        Nat.mul_comm 2 e]
      constructor
      · intro h
        have h1 := Nat.dvd_of_mul_dvd_mul_right (by decide) h
        exact Nat.mul_dvd_mul_right ((ih e (by omega) k).1 (dvd_of_odd_mul u _ k hu2 h1)) 2
      · intro h
        have h1 := Nat.dvd_of_mul_dvd_mul_right (by decide) h
        exact Nat.mul_dvd_mul_right (Nat.dvd_mul_left_of_dvd ((ih e (by omega) k).2 h1) u) 2
  · -- d = 2e+1: S は奇数
    have hodd : S a (2 * e + 1) % 2 = 1 := by
      have hS : S a (e + e) = a ^ e * S a e + S a e := S_add a e e
      have h4 := pow_mod4 a e ha
      simp only [S]
      rw [show 2 * e = e + e by omega, hS]
      have : (a ^ e * S a e + S a e) % 2 = 0 := by
        rw [show a ^ e * S a e + S a e = (a ^ e + 1) * S a e by grind, Nat.mul_mod]
        have : (a ^ e + 1) % 2 = 0 := by omega
        simp [this]
      rw [Nat.add_mod, Nat.mul_mod, this]; simp
    rw [pow_dvd_odd _ k hodd, pow_dvd_odd _ k (by omega)]

/-- 主定理: 0 ≤ i < j < 2^k なら i 番目と j 番目の値は法 2^k で異なる -/
theorem no_repeat (a c k x i j : Nat) (ha : a % 4 = 1) (hc : c % 2 = 1)
    (hij : i < j) (hj : j < 2 ^ k) : L a c i x % 2 ^ k ≠ L a c j x % 2 ^ k := by
  intro h
  obtain ⟨d, rfl⟩ : ∃ d, j = i + d := ⟨j - i, by omega⟩
  rw [L_add] at h
  generalize L a c i x = y at h
  rw [L_closed] at h
  have ha1 : 1 ≤ a := by omega
  have hpow := pow_eq a d ha1
  -- a^d*y + c*S - y = S * ((a-1)*y + c)
  have hdiff : a ^ d * y + c * S a d = y + S a d * ((a - 1) * y + c) := by
    rw [hpow]; grind
  rw [hdiff] at h
  have hdvd : 2 ^ k ∣ S a d * ((a - 1) * y + c) := by
    have := Nat.sub_mod_eq_zero_of_mod_eq h.symm
    rw [Nat.add_sub_cancel_left] at this
    exact Nat.dvd_of_mod_eq_zero this
  have hodd : ((a - 1) * y + c) % 2 = 1 := by
    obtain ⟨b, rfl⟩ : ∃ b, a = 4 * b + 1 := ⟨a / 4, by omega⟩
    rw [Nat.add_sub_cancel, Nat.add_mod, Nat.mul_mod, show 4 * b % 2 = 0 by omega]; simp; omega
  rw [Nat.mul_comm] at hdvd
  have hS := dvd_of_odd_mul _ _ k hodd hdvd
  have hd := (key a ha d k).1 hS
  have := Nat.le_of_dvd (by omega) hd
  omega

/-- 周期: 2^k 回で元に戻る(generateSequence が start で停止する根拠) -/
theorem period (a c k x : Nat) (ha : a % 4 = 1) :
    L a c (2 ^ k) x % 2 ^ k = x % 2 ^ k := by
  rw [L_closed, pow_eq a _ (by omega)]
  have hS : 2 ^ k ∣ S a (2 ^ k) := (key a ha _ k).2 (Nat.dvd_refl _)
  obtain ⟨t, ht⟩ := hS
  rw [ht]
  have : (1 + (a - 1) * (2 ^ k * t)) * x + c * (2 ^ k * t)
      = x + 2 ^ k * (((a - 1) * t) * x + c * t) := by grind
  rw [this, Nat.add_mul_mod_self_left]

/-! ## 🤖[AI生成] scramble.kts の出力モデルと Readme の課題 -/

/-- Kotlin の 1 ステップ: x ↦ (a * x + c) and mask(= mod m, m = 2^k) -/
def step (a c m x : Nat) : Nat := (a * x + c) % m

/-- step を i 回適用(generateSequence の i 番目) -/
def iter (a c m : Nat) : Nat → Nat → Nat
  | 0, x => x
  | i + 1, x => step a c m (iter a c m i x)

/-- 毎回 mod を取る反復は、mod なしの L を最後に mod したものと一致 -/
theorem iter_eq (a c m i x : Nat) (hx : x < m) : iter a c m i x = L a c i x % m := by
  induction i with
  | zero => simp [iter, L, Nat.mod_eq_of_lt hx]
  | succ i ih => simp only [iter, step, L, ih]; simp [Nat.add_mod, Nat.mul_mod]

/-- takeIf { it != start } の停止: 2^k 回目で start に戻り、それより前には戻らない。
    よって generateSequence はちょうど 2^k 個(i = 0 .. 2^k-1)を出力する -/
theorem stops_at (a c k s : Nat) (ha : a % 4 = 1) (hc : c % 2 = 1) (hs : s < 2 ^ k) :
    iter a c (2 ^ k) (2 ^ k) s = s ∧ ∀ j, 0 < j → j < 2 ^ k → iter a c (2 ^ k) j s ≠ s := by
  constructor
  · rw [iter_eq _ _ _ _ _ hs, period _ _ _ _ ha, Nat.mod_eq_of_lt hs]
  · intro j hj hjk h
    rw [iter_eq _ _ _ _ _ hs] at h
    exact no_repeat a c k s 0 j ha hc hj hjk (by rw [h]; simp [L, Nat.mod_eq_of_lt hs])

/-- scramble の出力: 1 周 2^k 個を逐次生成 → filter { it < n } → map { first + it } -/
def scramble (a c k s first n : Nat) : List Nat :=
  (((List.range (2 ^ k)).map (fun i => iter a c (2 ^ k) i s)).filter (· < n)).map (first + ·)

/-- 出力の値はすべて異なる(範囲 first .. first+n-1 の一般形) -/
theorem scramble_nodup (a c k s first n : Nat) (ha : a % 4 = 1) (hc : c % 2 = 1)
    (hs : s < 2 ^ k) : (scramble a c k s first n).Nodup := by
  unfold scramble List.Nodup
  rw [List.pairwise_map]
  apply List.Pairwise.imp (R := (· ≠ ·)) (fun h h' => h (Nat.add_left_cancel h'))
  apply List.Pairwise.filter
  rw [List.pairwise_map]
  apply List.pairwise_lt_range.imp_of_mem
  intro i j _ hj hij h
  simp only [List.mem_range] at hj
  rw [iter_eq _ _ _ _ _ hs, iter_eq _ _ _ _ _ hs] at h
  exact no_repeat a c k s i j ha hc hij hj h

/-- 課題: 入力 0..N(first = 0, n = N + 1)の場合、出力はすべての値が異なる -/
theorem readme (a c k s N : Nat) (ha : a % 4 = 1) (hc : c % 2 = 1) (hs : s < 2 ^ k) :
    (scramble a c k s 0 (N + 1)).Nodup :=
  scramble_nodup a c k s 0 (N + 1) ha hc hs

/-- Kotlin のパラメータ生成 a = ((r shl 2) + 1) and mask, c = (r' or 1) and mask は
    k ≥ 2 で前提 a % 4 = 1, c % 2 = 1 を満たす(k ≤ 1 は出力 2 個以下) -/
theorem kotlin_params (r r' k : Nat) (hk : 2 ≤ k) :
    (4 * r + 1) % 2 ^ k % 4 = 1 ∧ (2 * r' + 1) % 2 ^ k % 2 = 1 := by
  have h4 : 4 ∣ 2 ^ k := by
    have := Nat.pow_dvd_pow 2 hk
    simpa using this
  have h2 : 2 ∣ 2 ^ k := Nat.dvd_trans (by decide) h4
  rw [Nat.mod_mod_of_dvd _ h4, Nat.mod_mod_of_dvd _ h2]
  omega

#print axioms no_repeat
#print axioms period
#print axioms stops_at
#print axioms readme
#print axioms kotlin_params
