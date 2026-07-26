package com.example.taskflow.model

data class Habito(
    var nombre: String = "",
    var dias: MutableList<Boolean> = MutableList(31) { false }
)