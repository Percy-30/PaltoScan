package com.atpdev.paltoscan.features.diseaseInfo

import com.atpdev.paltoscan.domain.model.DiseaseInfo

/**
 * Base de datos de enfermedades y plagas del palto (Persea americana).
 * Clases basadas en los datasets:
 *  - AvocadoPest3 (Mendeley DOI: 10.17632/r3cswdjnpd.1): 9 clases de plagas
 *  - K-Kotagiri Avocado Leaf Dataset (Mendeley DOI: 10.17632/6zy6wxhf2v.1): Healthy + OtherDiseases
 */
object DiseaseRepository {

    // ──────────────────────────────────────────────────────────────────────────
    // Textos reutilizables
    // ──────────────────────────────────────────────────────────────────────────
    private val prevCaloptilia =
        "Para prevenir el minador de la hoja (<i>Caloptilia perseae</i>): <br><br>" +
            "<b>&#42; Monitoreo regular:</b> Inspeccionar el envés de las hojas jóvenes en busca de larvas o galerías sinuosas. <br><br>" +
            "<b>&#42; Control biológico:</b> Favorecer la presencia de parasitoides naturales como <i>Cirrospilus</i> spp. evitando el uso indiscriminado de insecticidas. <br><br>" +
            "<b>&#42; Poda sanitaria:</b> Eliminar y destruir hojas fuertemente infestadas para reducir la población de la plaga. <br><br>" +
            "<b>&#42; Riego adecuado:</b> Mantener la planta con buena nutrición para que produzca brotes vigorosos y resistentes."

    private val causasCaloptilia =
        "Plaga causada por la polilla minadora <i>Caloptilia perseae</i> (Lepidoptera: Gracillariidae). <br><br>" +
            "<b>&#42; Larvas que minan el tejido foliar, creando galerías sinuosas. </b><br>" +
            "<b>&#42; Altas temperaturas (>25°C) aceleran el ciclo de vida. </b><br>" +
            "<b>&#42; Brotes tiernos jóvenes son el objetivo principal. </b><br>" +
            "<b>&#42; Mayor incidencia en períodos secos. </b>"

    private val tratCaloptilia =
        "Para tratar el minador de la hoja en palto: <br><br>" +
            "<b>&#42; Insecticidas sistémicos:</b> Aplicar Imidacloprid o Spinosad en las primeras etapas de infestación para alcanzar las larvas dentro del tejido. <br><br>" +
            "<b>&#42; Control biológico:</b> Liberar avispas parasitoides del género <i>Cirrospilus</i> o <i>Pnigalio</i> cuando la infestación supera el umbral económico. <br><br>" +
            "<b>&#42; Aceites minerales:</b> Aplicar aceite agrícola en estadios tempranos para sufocar huevos y larvas jóvenes. <br><br>" +
            "<b>&#42; Eliminación de hojas infestadas:</b> En ataques avanzados, retirar manualmente las hojas más afectadas."

    private val prevOligonychus =
        "Para prevenir los ácaros (<i>Oligonychus</i> spp.) en palto: <br><br>" +
            "<b>&#42; Monitoreo periódico:</b> Revisar el envés de las hojas con lupa para detectar colonias en etapas tempranas. <br><br>" +
            "<b>&#42; Control biológico:</b> Conservar ácaros depredadores naturales como <i>Phytoseiidae</i> evitando el uso excesivo de insecticidas. <br><br>" +
            "<b>&#42; Manejo del riego:</b> Mantener humedad adecuada; la sequía favorece las poblaciones de ácaros. <br><br>" +
            "<b>&#42; Evitar exceso de nitrógeno:</b> El follaje suculento por exceso de nitrógeno atrae y favorece la reproducción de ácaros."

    private val causasOligonychusPerseae =
        "Plaga causada por el ácaro de la persea <i>Oligonychus perseae</i> (Acari: Tetranychidae). <br><br>" +
            "<b>&#42; Ácaros que colonizan el haz de la hoja, succionando savia. </b><br>" +
            "<b>&#42; Temperaturas cálidas (25–35°C) y baja humedad favorecen su reproducción. </b><br>" +
            "<b>&#42; Producen clorosis y bronceado en el haz de la hoja. </b><br>" +
            "<b>&#42; En ataques severos causan defoliación prematura. </b>"

    private val causasOligonychusPublicae =
        "Plaga causada por el ácaro rojo de la palma <i>Oligonychus punicae</i> (Acari: Tetranychidae). <br><br>" +
            "<b>&#42; Ácaros que atacan principalmente el haz de las hojas maduras. </b><br>" +
            "<b>&#42; Produce manchas pardas y rojas típicas en la lámina foliar. </b><br>" +
            "<b>&#42; Climas cálidos y secos aceleran su ciclo reproductivo. </b><br>" +
            "<b>&#42; Puede generar telarañas finas sobre la superficie foliar. </b>"

    private val tratOligonychus =
        "Para tratar los ácaros en palto: <br><br>" +
            "<b>&#42; Acaricidas:</b> Aplicar productos a base de Abamectina, Bifenazato o Spirodiclofen rotando principios activos para evitar resistencia. <br><br>" +
            "<b>&#42; Azufre mojable:</b> Efectivo como preventivo y en etapas iniciales de la infestación. <br><br>" +
            "<b>&#42; Aceites minerales o de neem:</b> Sufocan huevos y estados móviles jóvenes. <br><br>" +
            "<b>&#42; Riego por aspersión:</b> En algunas situaciones, mojar el follaje ayuda a reducir poblaciones al romper las telarañas."

    // ──────────────────────────────────────────────────────────────────────────
    // Base de datos de enfermedades — 11 clases
    // ──────────────────────────────────────────────────────────────────────────
    val diseaseDatabase =
        mapOf(
            // ─── CALOPTILIA PERSEAE ───────────────────────────────────────────
            "Caloptilia_perseae_Inicial" to
                DiseaseInfo(
                    name = "Minador de Hoja — Nivel Inicial",
                    description =
                        "Infestación incipiente de <i>Caloptilia perseae</i>, el minador de la hoja del palto. " +
                            "Se observan las primeras galerías sinuosas superficiales en hojas jóvenes, causadas por larvas que minan el tejido entre la epidermis y el parénquima. " +
                            "El daño es puntual y la planta aún no muestra síntomas sistémicos.",
                    prevention = prevCaloptilia,
                    causes = causasCaloptilia,
                    treatment = tratCaloptilia,
                ),
            "Caloptilia_perseae_Intermedio" to
                DiseaseInfo(
                    name = "Minador de Hoja — Nivel Intermedio",
                    description =
                        "Infestación moderada de <i>Caloptilia perseae</i>. " +
                            "Las galerías se extienden por varias hojas con decoloración amarillenta visible. " +
                            "Algunas hojas comienzan a enrollarse en los bordes (síntoma característico de larvas en estadios más avanzados). " +
                            "La fotosíntesis se ve parcialmente comprometida.",
                    prevention = prevCaloptilia,
                    causes = causasCaloptilia,
                    treatment = tratCaloptilia,
                ),
            "Caloptilia_perseae_Avanzado" to
                DiseaseInfo(
                    name = "Minador de Hoja — Nivel Avanzado",
                    description =
                        "Infestación severa de <i>Caloptilia perseae</i>. " +
                            "Múltiples hojas presentan extensa necrosis de los tejidos minados, enrollamiento pronunciado y caída prematura. " +
                            "La pérdida foliar impacta directamente la producción del árbol y facilita la entrada de enfermedades secundarias.",
                    prevention = prevCaloptilia,
                    causes = causasCaloptilia,
                    treatment = tratCaloptilia,
                ),

            // ─── OLIGONYCHUS PERSEAE ──────────────────────────────────────────
            "Oligonychus_perseae_Inicial" to
                DiseaseInfo(
                    name = "Ácaro de la Persea — Nivel Inicial",
                    description =
                        "Colonias incipientes de <i>Oligonychus perseae</i> en el haz de las hojas. " +
                            "Se observan pequeñas manchas cloróticas puntiformes y presencia de finas telarañas. " +
                            "El impacto en la planta es mínimo en esta etapa.",
                    prevention = prevOligonychus,
                    causes = causasOligonychusPerseae,
                    treatment = tratOligonychus,
                ),
            "Oligonychus_perseae_Intermedio" to
                DiseaseInfo(
                    name = "Ácaro de la Persea — Nivel Intermedio",
                    description =
                        "Infestación moderada de <i>Oligonychus perseae</i>. " +
                            "Las hojas muestran clorosis generalizada en el haz, con aspecto plateado-plateado o bronceado. " +
                            "Se evidencia presencia masiva de ácaros, huevos y exuvias. " +
                            "La tasa fotosintética de la planta disminuye notablemente.",
                    prevention = prevOligonychus,
                    causes = causasOligonychusPerseae,
                    treatment = tratOligonychus,
                ),
            "Oligonychus_perseae_Avanzado" to
                DiseaseInfo(
                    name = "Ácaro de la Persea — Nivel Avanzado",
                    description =
                        "Infestación severa de <i>Oligonychus perseae</i>. " +
                            "Hojas con bronceado intenso, defoliación prematura y presencia de densas telarañas. " +
                            "El árbol puede perder gran parte de su follaje, reduciendo significativamente la producción de frutos.",
                    prevention = prevOligonychus,
                    causes = causasOligonychusPerseae,
                    treatment = tratOligonychus,
                ),

            // ─── OLIGONYCHUS PUNICAE ──────────────────────────────────────────
            "Oligonychus_punicae_Inicial" to
                DiseaseInfo(
                    name = "Ácaro Rojo — Nivel Inicial",
                    description =
                        "Primeras colonias de <i>Oligonychus punicae</i> sobre hojas maduras. " +
                            "Se aprecian manchas pequeñas de color pardo-rojizo en el haz, sin telarañas visibles a simple vista. " +
                            "La infestación es localizada y controlable.",
                    prevention = prevOligonychus,
                    causes = causasOligonychusPublicae,
                    treatment = tratOligonychus,
                ),
            "Oligonychus_punicae_Intermedio" to
                DiseaseInfo(
                    name = "Ácaro Rojo — Nivel Intermedio",
                    description =
                        "Infestación moderada de <i>Oligonychus punicae</i>. " +
                            "Las manchas pardas cubren áreas más extensas del haz, con presencia de ácaros visibles y telarañas finas. " +
                            "La hoja adquiere un aspecto sucio rojizo-pardo. Impacto moderado sobre la fotosíntesis.",
                    prevention = prevOligonychus,
                    causes = causasOligonychusPublicae,
                    treatment = tratOligonychus,
                ),
            "Oligonychus_punicae_Avanzado" to
                DiseaseInfo(
                    name = "Ácaro Rojo — Nivel Avanzado",
                    description =
                        "Infestación severa de <i>Oligonychus punicae</i>. " +
                            "Las hojas presentan coloración roja-parda generalizada, abundantes telarañas y posible defoliación. " +
                            "El árbol entra en estrés hídrico y nutricional. Requiere intervención inmediata.",
                    prevention = prevOligonychus,
                    causes = causasOligonychusPublicae,
                    treatment = tratOligonychus,
                ),

            // ─── HEALTHY ──────────────────────────────────────────────────────
            "Healthy" to
                DiseaseInfo(
                    name = "Hoja Saludable",
                    description =
                        "La hoja de palto se encuentra en condiciones óptimas: coloración verde uniforme, superficie limpia y sin síntomas de plagas, manchas, necrosis ni deformaciones. " +
                            "Indica un manejo agronómico adecuado del cultivo.",
                    prevention =
                        "Para mantener la salud del palto: <br><br>" +
                            "<b>&#42; Fertilización balanceada:</b> Aplicar nutrientes según análisis de suelo para evitar deficiencias y excesos. <br><br>" +
                            "<b>&#42; Riego eficiente:</b> Mantener humedad uniforme; el palto es sensible tanto a la falta como al exceso de agua. <br><br>" +
                            "<b>&#42; Podas sanitarias periódicas:</b> Eliminar ramas secas, enfermas o con signos de plaga. <br><br>" +
                            "<b>&#42; Monitoreo constante:</b> Inspeccionar el cultivo regularmente para detectar problemas en etapas tempranas.",
                    causes = "No aplica. La hoja está sana.",
                    treatment = "No aplica. Continuar con el manejo agronómico preventivo establecido.",
                ),

            // ─── OTHER DISEASES ───────────────────────────────────────────────
            "OtherDiseases" to
                DiseaseInfo(
                    name = "Otras Enfermedades",
                    description =
                        "La hoja de palto muestra síntomas de una condición patológica no clasificada dentro de las plagas principales detectadas por PaltoScan. " +
                            "Puede tratarse de enfermedades fúngicas (Antracnosis, Cercospora, Roña), deficiencias nutricionales, daños abióticos u otras patologías.",
                    prevention =
                        "Medidas preventivas generales: <br><br>" +
                            "<b>&#42; Inspección visual frecuente:</b> Detectar síntomas anómalos a tiempo reduce la gravedad del daño. <br><br>" +
                            "<b>&#42; Registro fotográfico:</b> Documentar la evolución de los síntomas para facilitar el diagnóstico especializado. <br><br>" +
                            "<b>&#42; Consultar a un agrónomo:</b> Un diagnóstico presencial o de laboratorio permite identificar con precisión el agente causal.",
                    causes = "El agente causal específico no pudo ser identificado por el modelo. Puede ser de origen fúngico, bacteriano, viral, abiótico o por deficiencias nutricionales.",
                    treatment =
                        "Recomendaciones generales: <br><br>" +
                            "<b>&#42; Muestra de laboratorio:</b> Enviar material vegetal afectado a un laboratorio fitopatológico para diagnóstico definitivo. <br><br>" +
                            "<b>&#42; Fungicidas preventivos:</b> Aplicar cúpricos como medida provisional si se sospecha de un hongo. <br><br>" +
                            "<b>&#42; Consulta especializada:</b> Contactar al servicio de extensión agrícola o a un ingeniero agrónomo con experiencia en paltos.",
                ),
        )
}
