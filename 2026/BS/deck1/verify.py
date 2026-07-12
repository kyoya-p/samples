import math

def comb(n, r):
    if n < r or r < 0:
        return 0
    return math.comb(n, r)

def verify():
    print("=== DECK#0 精密計算 ===")
    # 1. 初期手札4枚
    p_init0 = 1 - comb(37, 4) / comb(40, 4)
    print(f"初期手札4枚でチヒロを引く確率 (p_init_0): {p_init0*100:.4f}% (組み合わせ数: {comb(40,4)-comb(37,4)} / {comb(40,4)})")
    
    # 2. マリガン
    # 36枚から4枚引き、戻した4枚を混ぜて36枚から1枚引く
    # 失敗確率 = (COMBIN(33, 4)/COMBIN(36, 4)) * (33/36)
    p_fail_mulligan0 = (comb(33, 4) / comb(36, 4)) * (33 / 36)
    p_mulligan0 = 1 - p_fail_mulligan0
    print(f"マリガン成功確率 (p_mulligan_0): {p_mulligan0*100:.4f}%")
    
    # 3. 最終成功確率
    p_final0 = p_init0 + (1 - p_init0) * p_mulligan0
    print(f"★最終成功確率 (P_final_0): {p_final0*100:.4f}%")
    
    print("\n=== DECK#1 精密計算 ===")
    # 1. 初期3枚でチヒロもサーチも0枚（マリガンする確率）
    # ハズレ 31枚
    p_mulligan_trigger = comb(31, 3) / comb(39, 3)
    p_keep = 1 - p_mulligan_trigger
    print(f"マリガン選択確率 (P_mulligan): {p_mulligan_trigger*100:.4f}% (組み合わせ数: {comb(31,3)} / {comb(39,3)})")
    print(f"キープ確率 (P_keep): {p_keep*100:.4f}%")
    
    # 2. キープ時の成功（4ドローで直接チヒロ、またはチヒロ0かつサーチ1以上でサーチ成功）
    # 直接チヒロ
    p_direct1 = 1 - comb(36, 4) / comb(39, 4)
    # チヒロ0かつサーチ1以上
    p_search_avail1 = (comb(36, 4) - comb(31, 4)) / comb(39, 4)
    # サーチ成功確率 (35枚から3枚オープン)
    p_search_succ1 = 1 - comb(32, 3) / comb(35, 3)
    p_search_route1 = p_search_avail1 * p_search_succ1
    p_keep_success = p_direct1 + p_search_route1
    print(f"4ドロー直接チヒロ確率: {p_direct1*100:.4f}%")
    print(f"4ドローチヒロ0かつサーチ1以上確率: {p_search_avail1*100:.4f}%")
    print(f"サーチ単体成功確率: {p_search_succ1*100:.4f}%")
    print(f"サーチ経由成功確率: {p_search_route1*100:.4f}%")
    print(f"キープかつ成功確率 (P_keep_success): {p_keep_success*100:.4f}%")
    
    # 3. マリガン精密計算
    # 4枚ドロー (山札36枚、サーチ5、チヒロ3、ハズレ28)
    p_fail_k = [0]*6
    
    # K=0 (s=0, sd=0)
    p_fail_k[0] = (comb(5,0)*comb(28,4)/comb(36,4)) * (28/36)
    
    # K=1
    p_fail_k[1] = ((comb(5,1)*comb(28,3)/comb(36,4)) * (29/36) + (comb(5,0)*comb(28,4)/comb(36,4)) * (5/36)) * (comb(32,3)/comb(35,3))
    
    # K=2
    p_fail_k[2] = ((comb(5,2)*comb(28,2)/comb(36,4)) * (30/36) + (comb(5,1)*comb(28,3)/comb(36,4)) * (4/36)) * (comb(32,6)/comb(35,6))
    
    # K=3
    p_fail_k[3] = ((comb(5,3)*comb(28,1)/comb(36,4)) * (31/36) + (comb(5,2)*comb(28,2)/comb(36,4)) * (3/36)) * (comb(32,9)/comb(35,9))
    
    # K=4
    p_fail_k[4] = ((comb(5,4)*comb(28,0)/comb(36,4)) * (32/36) + (comb(5,3)*comb(28,1)/comb(36,4)) * (2/36)) * (comb(32,12)/comb(35,12))
    
    # K=5
    p_fail_k[5] = ((comb(5,4)*comb(28,0)/comb(36,4)) * (1/36)) * (comb(32,15)/comb(35,15))
    
    p_fail_mulligan1 = sum(p_fail_k)
    p_mulligan1 = 1 - p_fail_mulligan1
    print(f"マリガン成功確率 (p_mulligan_1): {p_mulligan1*100:.4f}% (失敗詳細: {[x*100 for x in p_fail_k]})")
    
    # 4. 最終成功確率
    p_final1 = p_keep_success + p_mulligan_trigger * p_mulligan1
    print(f"★最終成功確率 (P_final_1): {p_final1*100:.4f}%")

if __name__ == "__main__":
    verify()
