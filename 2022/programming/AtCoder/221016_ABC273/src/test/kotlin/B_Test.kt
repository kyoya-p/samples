val testEnvs_B = listOf(
    testEnvBuilder {
        intake.println("2048 2")
        outlet.readLine() == "2100"
    },
    testEnvBuilder {
        intake.println("1 15")
        outlet.readLine() == "0"
    },
    testEnvBuilder {
        intake.println("999 3")
        outlet.readLine() == "1000"
    },
    testEnvBuilder {
        intake.println("314159265358979 12")
        outlet.readLine() == "314000000000000"
    }

)