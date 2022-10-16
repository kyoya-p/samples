val testEnvs_C = listOf(
    testEnvBuilder {
        intake.put(
            """6
2 7 1 8 2 8
"""
        )

        outlet.observe(
            """2
1
2
1
0
0
"""
        )
    },
    testEnvBuilder {
        intake.put(
            """1
1"""
        )
        outlet.observe(
            """1"""
        )
    },
    testEnvBuilder {
        intake.put(
            """10
979861204 57882493 979861204 447672230 644706927 710511029 763027379 710511029 447672230 136397527"""
        )
        outlet.observe(
            """2
1
2
1
2
1
1
0
0
0"""
        )
    }
)