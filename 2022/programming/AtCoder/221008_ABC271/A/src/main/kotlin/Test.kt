val testEnvs = listOf(
    testEnvBuilder {
        intake.println("99")
        outlet.readLine() == "63"
    },
    testEnvBuilder {
        intake.println("12")
        outlet.readLine() == "0C"
    },
    testEnvBuilder {
        intake.println("0")
        outlet.readLine() == "00"
    },
    testEnvBuilder {
        intake.println("255")
        outlet.readLine() == "FF"
    },
)