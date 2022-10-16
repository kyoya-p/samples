import A.A
import B.B
import C.C
import org.junit.jupiter.api.Test

class Tester {
    @Test
    fun test_A() = stdioEmulators(testEnvs_A) { A() }

    @Test
    fun test_B() = stdioEmulators(testEnvs_B) { B() }

    @Test
    fun test_C() = stdioEmulators(testEnvs_C) { C() }
//    fun test_D() = stdioEmulators(testEnvs_D) { D() }
//    fun test_E() = stdioEmulators(testEnvs_E) { E() }
//    fun test_F() = stdioEmulators(testEnvs_F) { F() }
}
