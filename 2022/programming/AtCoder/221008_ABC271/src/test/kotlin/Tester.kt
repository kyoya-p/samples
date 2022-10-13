import A.A
import B.B
import org.junit.jupiter.api.Test

class Tester {
    @Test
    fun test_A() = stdioEmulators(testEnvs_A) { A() }
    @Test
    fun test_B() = stdioEmulators(testEnvs_B) { B() }
//    @Test
//    fun test_B2() = stdioEmulatiors(testEnv_AtCoder) { B() }
//    fun test_C() = stdioEmulatiors(testEnvs_C) { C() }
//    fun test_D() = stdioEmulatiors(testEnvs_D) { D() }
//    fun test_E() = stdioEmulatiors(testEnvs_E) { E() }
//    fun test_F() = stdioEmulatiors(testEnvs_F) { F() }
}
