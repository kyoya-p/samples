val testEnvs_C = listOf(
    testEnvBuilder {
        intake.println(
            """6
2 7 1 8 2 8
"""
        )
        val testRes = """2
1
2
1
0
0
"""
        val r = outlet.readText()
        println("[$r]:[$testRes]")
        r == testRes
    },
    testEnvBuilder {
        intake.println(
            """1
                1
"""
        )
        outlet.readText() == """1"""
    },
    testEnvBuilder {
        intake.println(
            """10
979861204 57882493 979861204 447672230 644706927 710511029 763027379 710511029 447672230 136397527
"""
        )
        outlet.readText() == """2
1
2
1
2
1
1
0
0
0
"""
    }
)