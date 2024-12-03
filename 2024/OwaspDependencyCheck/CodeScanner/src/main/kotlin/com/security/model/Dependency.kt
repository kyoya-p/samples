package com.security.model

data class Dependency(
    val name: String,
    val version: String,
    val groupId: String?,
    val artifactId: String?,
    val vulnerabilities: List<Vulnerability>
)

data class Vulnerability(
    val name: String,
    val severity: String,
    val description: String
)