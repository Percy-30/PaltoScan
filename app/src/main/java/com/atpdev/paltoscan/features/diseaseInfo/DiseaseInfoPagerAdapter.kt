package com.atpdev.paltoscan.features.diseaseInfo

import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter

class DiseaseInfoPagerAdapter(fragment: Fragment, private val diseaseName: String, private val fromHistory: Boolean = false) :
    FragmentStateAdapter(fragment) {
    // Lista de secciones usando la sealed class
    private val sections =
        listOf(
            Section.Enfermedad,
            Section.Tratamiento,
            Section.Causas,
            Section.Prevencion,
        )

    /*private val sections = listOf(
        "Enfermedad",
        "Tratamiento",
        "Causas",
        "Prevención"
    )*/

    override fun getItemCount(): Int = sections.size

    override fun createFragment(position: Int): Fragment {
        // require(position in sections.indices) { "Posición inválida: $position" }
        val section = sections[position]
        // return DiseaseDetailFragment.newInstance(diseaseName, section.title)
        // Pasamos la posición para identificar únicamente cada fragmento
        return DiseaseDetailFragment.newInstance(diseaseName, section.title, position, fromHistory = fromHistory)
        // return DiseaseDetailFragment.newInstance(diseaseName, sections[position])
    }

    // override fun getItemCount(): Int = 4 // Número de pestañas
    /*override fun createFragment(position: Int): Fragment {
        return when (position) {
            0 -> DiseaseDetailFragment.newInstance(diseaseName, "Enfermedad")
            1 -> DiseaseDetailFragment.newInstance(diseaseName, "Tratamiento")
            2 -> DiseaseDetailFragment.newInstance(diseaseName, "Causas")
            3 -> DiseaseDetailFragment.newInstance(diseaseName, "Prevención")
            else -> Fragment() // Fragmento vacío por defecto
        }
    }*/
}
