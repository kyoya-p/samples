val testEnvs_A = listOf(
    testEnvBuilder {
        intake.println("2")
        outlet.readLine() == "2"
    },
    testEnvBuilder {
        intake.println("3")
        outlet.readLine() == "6"
    },
    testEnvBuilder {
        intake.println("10")
        outlet.readLine() == "3628800"
    }
)