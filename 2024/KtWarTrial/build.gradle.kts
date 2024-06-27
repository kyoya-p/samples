plugins {
    war
    id("org.gretty") version "4.0.3"
}

gretty {
    httpPort = 8081
    servletContainer = "tomcat10"
}
