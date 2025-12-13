package com.example.iotapp.model

data class PlantInformation(
    val temperature: String,
    val humidity: String,
    val rainStatus: String,
    val connectStatus: String,
    val schedule: String
) {
}