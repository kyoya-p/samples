package com.example.model

import kotlinx.serialization.Serializable

@Serializable
data class SampleItem(
    val id: String? = null,
    val name: String,
    val createdAt: String
)
