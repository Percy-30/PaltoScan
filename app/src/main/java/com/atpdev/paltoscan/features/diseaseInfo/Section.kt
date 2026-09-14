package com.atpdev.paltoscan.features.diseaseInfo

sealed class Section(val title: String) {
    /*object Enfermedad : Section("Enfermedad")
    object Tratamiento : Section("Tratamiento")
    object Causas : Section("Causas")
    object Prevencion : Section("Prevención")

    companion object {
        fun fromSection(section: Section): Section = section
    }*/

    object Enfermedad : Section("Enfermedad")

    object Tratamiento : Section("Tratamiento")

    object Causas : Section("Causas")

    object Prevencion : Section("Prevención")

    companion object {
        fun fromTitle(title: String): Section? {
            return when (title) {
                "Enfermedad" -> Enfermedad
                "Tratamiento" -> Tratamiento
                "Causas" -> Causas
                "Prevención" -> Prevencion
                else -> null
            }
        }
    }
}
