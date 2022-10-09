val testEnvs_B = listOf(
    testEnvBuilder {
        intake.println(
            """
            2 2
            3 1 4 7
            2 5 9
            1 3
            2 1
        """.trimIndent()
        )
        outlet.readLine() == "7"
                && outlet.readLine() == "5"
    },
    testEnvBuilder {
        intake.println(
            """
3 4
4 128 741 239 901
2 1 1
3 314 159 26535
1 1
2 2
3 3
1 4
        """.trimIndent()
        )
        outlet.readLine() == "128"
                && outlet.readLine() == "1"
                && outlet.readLine() == "26535"
                && outlet.readLine() == "901"
    }

)