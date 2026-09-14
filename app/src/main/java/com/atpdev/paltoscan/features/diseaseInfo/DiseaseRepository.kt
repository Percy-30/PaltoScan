package com.atpdev.paltoscan.features.diseaseInfo

import com.atpdev.paltoscan.domain.model.DiseaseInfo

object DiseaseRepository {
    val diseaseDatabase =
        mapOf(
            "Quemazon" to
                DiseaseInfo( // -> Early_Blight,   Ojo de Pájaro , Pitiquiña
                    name = "Quemazon",
                    description =
                        "La quemazón es una enfermedad fúngica causada por el hongo Alternaria solani. " +
                            "Afecta las hojas y el tallo de la papa, causando lesiones oscuras y redondas que pueden provocar la muerte de la planta. " +
                            "La enfermedad se caracteriza por la aparición de manchas oscuras y redondas en las hojas, que pueden fusionarse y cubrir grandes áreas de la planta.",
                    prevention =
                        "Para prevenir la aparición de la quemazón, es importante implementar las siguientes medidas: <br><br>" +
                            "<b>&#42; Selección de variedades resistentes:</b> Seleccionar variedades de papa que sean resistentes a la quemazón puede ayudar a reducir el riesgo de infección. <br><br>" +
                            "<b>&#42; Rotación de cultivos:</b> Rotar los cultivos puede ayudar a reducir el riesgo de infección, ya que el hongo que causa la quemazón puede sobrevivir en el suelo durante varios años. <br><br>" +
                            "<b>&#42; Control de la humedad:</b> La humedad es un factor importante en el desarrollo de la quemazón. Reducir la humedad en el campo mediante la implementación de sistemas de riego eficientes y la eliminación de malezas puede ayudar a reducir el riesgo de infección. <br><br>" +
                            "<b>&#42; Eliminación de plantas infectadas:</b> Eliminar las plantas infectadas puede ayudar a reducir la propagación de la enfermedad.",
                    causes =
                        "La quemazón es causada por el hongo Alternaria solani. Este hongo puede sobrevivir en el suelo durante varios años y se propaga mediante esporas que se liberan en el aire y el agua. <br><br>" +
                            "<b>&#42; El hongo Alternaria solani. </b><br>" +
                            "<b>&#42; Temperaturas moderadas (15-25°C). </b><br>" +
                            "<b>&#42; Humedad relativa alta (>60%). </b><br>" +
                            "<b>&#42; Lesiones en la planta. </b>",
                    treatment =
                        "El tratamiento de la quemazón implica la aplicación de fungicidas específicos y la implementación de medidas de control cultural. Algunas opciones de tratamiento incluyen: <br><br>" +
                            "<b>&#42; Aplicación de fungicidas:</b> Los fungicidas pueden ser aplicados en forma de polvo o líquido para controlar la propagación de la enfermedad. <br><br>" +
                            "<b>&#42; Control de la humedad:</b> Reducir la humedad en el campo puede ayudar a reducir la propagación de la enfermedad. <br><br>" +
                            "<b>&#42; Eliminación de plantas infectadas:</b> Eliminar las plantas infectadas puede ayudar a reducir la propagación de la enfermedad. <br><br>" +
                            "<b>&#42; Mejora de la ventilación:</b> Mejorar la ventilación en el campo puede ayudar a reducir la humedad y la propagación de la enfermedad.",
                ),
            "Hoja_Saludable" to
                DiseaseInfo(
                    name = " Hoja_Saludable",
                    description = ": La planta de papa se encuentra en un estado saludable y sin síntomas de enfermedad.",
                    prevention = "Mantener prácticas agrícolas saludables, como la rotación de cultivos y el control de plagas.",
                    causes = "Causas papa Saludable,  No aplica",
                    treatment = "Tratamineto No aplica.",
                ),
            "Rancha" to
                DiseaseInfo( // Late_Blight //Rancha
                    name = "Rancha",
                    description =
                        "La rancha es una enfermedad fúngica causada por el hongo Phytophthora infestans. " +
                            "Afecta las hojas y el tallo de la papa, causando lesiones oscuras y húmedas " +
                            "La enfermedad se caracteriza por la aparición de manchas oscuras y redondas en las hojas, que pueden fusionarse y cubrir grandes áreas de la planta." +
                            "La rancha puede causar daños significativos en la cosecha, especialmente si no se controla de manera efectiva.",
                    prevention =
                        "Para prevenir la aparición de la rancha, es importante implementar las siguientes medidas: <br><br>" +
                            "<b>&#42; Selección de variedades de papa resistentes:</b> Seleccionar variedades de papa que sean resistentes a la rancha puede ayudar a reducir el riesgo de infección. <br><br>" +
                            "<b>&#42; Rotación de cultivos:</b> Rotar los cultivos puede ayudar a reducir el riesgo de infección, ya que el hongo que causa la rancha puede sobrevivir en el suelo durante varios años. <br><br>" +
                            "<b>&#42; Control de la humedad:</b> La humedad es un factor importante en el desarrollo de la rancha. Reducir la humedad en el campo mediante la implementación de sistemas de riego eficientes y la eliminación de malezas puede ayudar a reducir el riesgo de infección. <br> <br>" +
                            "<b>&#42; Eliminación de plantas infectadas:</b> Eliminar las plantas infectadas puede ayudar a reducir la propagación de la enfermedad. <br><br>" +
                            "<b>&#42; Uso de mulch para reducir la humedad en el suelo.<b>",
                    causes =
                        "La rancha es causada por el hongo Phytophthora infestans. Este hongo puede sobrevivir en el suelo durante varios años y se propaga mediante esporas que se liberan en el aire y el agua. <br><br>" +
                            "<b>&#42; El hongo Phytophthora infestans. </b> <br>" +
                            "<b>&#42; Temperaturas frescas (10-15°C). </b> <br>" +
                            "<b>&#42; Humedad relativa alta (>80%). </b> <br>" +
                            "<b>&#42; Lesiones en la planta.</b>",
                    treatment =
                        "El tratamiento de la rancha implica la aplicación de fungicidas específicos y la implementación de medidas de control cultural. Algunas opciones de tratamiento incluyen: <br><br>" +
                            "<b>&#42; Aplicación de fungicidas:</b> Los fungicidas pueden ser aplicados en forma de polvo o líquido para controlar la propagación de la enfermedad. <br><br>" +
                            "<b>&#42; Control de la humedad:</b> Reducir la humedad en el campo puede ayudar a reducir la propagación de la enfermedad. <br><br>" +
                            "<b>&#42; Eliminación de plantas infectadas:</b> Eliminar las plantas infectadas puede ayudar a reducir la propagación de la enfermedad. ",
                ),
            "No_Identificado" to
                DiseaseInfo(
                    name = "No_Identificado",
                    description = "Descripción no disponible",
                    prevention = "Prevención No disponible",
                    causes = "Cuasas No disponible",
                    treatment = "Tratamiento no disponible",
                ),
        )
}
