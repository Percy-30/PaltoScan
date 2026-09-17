package com.atpdev.paltoscan.features.diseaseInfo

import com.atpdev.paltoscan.domain.model.DiseaseInfo

/**
 * Base de datos de enfermedades y plagas del palto (Persea americana).
 * Clases basadas en el entrenamiento oficial de PaltoScan:
 *  - CrystalMite (Oligonychus perseae - Ácaro cristalino de la persea): Early, Intermediate, Advanced
 *  - LeafMiner (Caloptilia perseae - Minador de hoja): Early, Intermediate, Advanced
 *  - RedMite (Oligonychus punicae - Ácaro rojo): Early, Intermediate, Advanced
 *  - Healthy (Hoja sana)
 *  - OtherDisease (Otras enfermedades de la palta)
 */
object DiseaseRepository {

    // ──────────────────────────────────────────────────────────────────────────
    // Textos reutilizables
    // ──────────────────────────────────────────────────────────────────────────
    private val prevLeafMiner =
        "Para prevenir el minador de la hoja (<i>Caloptilia perseae</i>): <br><br>" +
            "<b>&#42; Monitoreo regular:</b> Inspeccionar el envés de las hojas jóvenes en busca de larvas o galerías sinuosas. <br><br>" +
            "<b>&#42; Control biológico:</b> Favorecer la presencia de parasitoides naturales como <i>Cirrospilus</i> spp. evitando el uso indiscriminado de insecticidas. <br><br>" +
            "<b>&#42; Poda sanitaria:</b> Eliminar y destruir hojas fuertemente infestadas para reducir la población de la plaga. <br><br>" +
            "<b>&#42; Riego adecuado:</b> Mantener la planta con buena nutrición para que produzca brotes vigorosos y resistentes."

    private val causasLeafMiner =
        "Plaga causada por la polilla minadora <i>Caloptilia perseae</i> (Lepidoptera: Gracillariidae). <br><br>" +
            "<b>&#42; Larvas que minan el tejido foliar, creando galerías sinuosas. </b><br>" +
            "<b>&#42; Altas temperaturas (>25°C) aceleran el ciclo de vida. </b><br>" +
            "<b>&#42; Brotes tiernos jóvenes son el objetivo principal. </b><br>" +
            "<b>&#42; Mayor incidencia en períodos secos. </b>"

    private val tratLeafMiner =
        "Para tratar el minador de la hoja en palto: <br><br>" +
            "<b>&#42; Insecticidas sistémicos:</b> Aplicar Imidacloprid o Spinosad en las primeras etapas de infestación para alcanzar las larvas dentro del tejido. <br><br>" +
            "<b>&#42; Control biológico:</b> Liberar avispas parasitoides del género <i>Cirrospilus</i> o <i>Pnigalio</i> cuando la infestación supera el umbral económico. <br><br>" +
            "<b>&#42; Aceites minerales:</b> Aplicar aceite agrícola en estadios tempranos para sufocar huevos y larvas jóvenes. <br><br>" +
            "<b>&#42; Eliminación de hojas infestadas:</b> En ataques avanzados, retirar manualmente las hojas más afectadas."

    private val prevMites =
        "Para prevenir los ácaros (<i>Oligonychus</i> spp.) en palto: <br><br>" +
            "<b>&#42; Monitoreo periódico:</b> Revisar el envés y haz de las hojas con lupa para detectar colonias en etapas tempranas. <br><br>" +
            "<b>&#42; Control biológico:</b> Conservar ácaros depredadores naturales como <i>Phytoseiidae</i> evitando el uso excesivo de insecticidas de amplio espectro. <br><br>" +
            "<b>&#42; Manejo del riego:</b> Mantener humedad adecuada; el estrés por sequía favorece la explosión poblacional de ácaros. <br><br>" +
            "<b>&#42; Evitar exceso de nitrógeno:</b> El follaje suculento por exceso de fertilización nitrogenada atrae y favorece la reproducción rápida de ácaros."

    private val causasCrystalMite =
        "Plaga causada por el ácaro cristalino de la persea <i>Oligonychus perseae</i> (Acari: Tetranychidae). <br><br>" +
            "<b>&#42; Ácaros que colonizan el haz y envés de la hoja, succionando savia celular. </b><br>" +
            "<b>&#42; Temperaturas cálidas (25–35°C) y baja humedad ambiental favorecen su multiplicación. </b><br>" +
            "<b>&#42; Producen clorosis moteada y bronceado en la superficie foliar. </b><br>" +
            "<b>&#42; En ataques severos provocan defoliación prematura y caída de frutos. </b>"

    private val causasRedMite =
        "Plaga causada por el ácaro rojo del palto <i>Oligonychus punicae</i> (Acari: Tetranychidae). <br><br>" +
            "<b>&#42; Ácaros que atacan principalmente el haz de las hojas maduras. </b><br>" +
            "<b>&#42; Produce manchas pardas, rojizas y bronceadas características en la lámina foliar. </b><br>" +
            "<b>&#42; Climas cálidos, secos y con presencia de polvo en las hojas aceleran su ciclo. </b><br>" +
            "<b>&#42; Puede tejer finas telas sobre la superficie foliar acumulando polvo. </b>"

    private val tratMites =
        "Para tratar los ácaros en palto: <br><br>" +
            "<b>&#42; Acaricidas específicos:</b> Aplicar productos a base de Abamectina, Bifenazato o Spirodiclofen rotando modos de acción para evitar resistencia. <br><br>" +
            "<b>&#42; Azufre mojable:</b> Excelente preventivo y curativo en etapas iniciales (evitar aplicar a temperaturas superiores a 32°C). <br><br>" +
            "<b>&#42; Aceites agrícolas o de neem:</b> Sufocan huevos, ninfas y adultos. <br><br>" +
            "<b>&#42; Lavados foliares:</b> El riego por aspersión o lavado con agua a presión ayuda a reducir densidades poblacionales."

    // ──────────────────────────────────────────────────────────────────────────
    // Definiciones base
    // ──────────────────────────────────────────────────────────────────────────
    private val infoCrystalMiteEarly =
        DiseaseInfo(
            name = "Ácaro Cristalino (Persea) — Nivel Inicial",
            description =
                "Colonias incipientes de <i>Oligonychus perseae</i> (Crystal Mite) en las hojas. " +
                    "Se observan pequeños puntos cloróticos aislados a lo largo de las nervaduras. " +
                    "El impacto en la capacidad fotosintética de la planta aún es mínimo y fácil de controlar.",
            prevention = prevMites,
            causes = causasCrystalMite,
            treatment = tratMites,
        )

    private val infoCrystalMiteIntermediate =
        DiseaseInfo(
            name = "Ácaro Cristalino (Persea) — Nivel Intermedio",
            description =
                "Infestación moderada de <i>Oligonychus perseae</i>. " +
                    "Las manchas cloróticas se extienden formando un bronceado característico a lo largo de las nervaduras. " +
                    "Se evidencia presencia de ninfas activas, huevos y telas finas. Disminuye la eficiencia fotosintética.",
            prevention = prevMites,
            causes = causasCrystalMite,
            treatment = tratMites,
        )

    private val infoCrystalMiteAdvanced =
        DiseaseInfo(
            name = "Ácaro Cristalino (Persea) — Nivel Avanzado",
            description =
                "Infestación severa y crítica de <i>Oligonychus perseae</i>. " +
                    "Hojas con necrosis parcial o total, bronceado intenso generalizado y densas telas protectoras. " +
                    "Existe alto riesgo de defoliación masiva, lo que expondrá los frutos a quemaduras solares.",
            prevention = prevMites,
            causes = causasCrystalMite,
            treatment = tratMites,
        )

    private val infoLeafMinerEarly =
        DiseaseInfo(
            name = "Minador de Hoja — Nivel Inicial",
            description =
                "Infestación incipiente de <i>Caloptilia perseae</i> (Leaf Miner). " +
                    "Se aprecian las primeras galerías sinuosas y transparentes en hojas tiernas, causadas por larvas minadoras. " +
                    "El daño es superficial y focalizado.",
            prevention = prevLeafMiner,
            causes = causasLeafMiner,
            treatment = tratLeafMiner,
        )

    private val infoLeafMinerIntermediate =
        DiseaseInfo(
            name = "Minador de Hoja — Nivel Intermedio",
            description =
                "Infestación moderada de <i>Caloptilia perseae</i>. " +
                    "Múltiples galerías visibles por hoja con decoloración y marchitez en los bordes. " +
                    "Algunas hojas empiezan a doblarse o enrollarse en la punta para que la larva construya su cámara pupal.",
            prevention = prevLeafMiner,
            causes = causasLeafMiner,
            treatment = tratLeafMiner,
        )

    private val infoLeafMinerAdvanced =
        DiseaseInfo(
            name = "Minador de Hoja — Nivel Avanzado",
            description =
                "Infestación severa de <i>Caloptilia perseae</i>. " +
                    "Extensa necrosis del tejido foliar afectado, deformación severa de las hojas y caída prematura. " +
                    "Afecta drásticamente el desarrollo de brotes nuevos y la producción del palto.",
            prevention = prevLeafMiner,
            causes = causasLeafMiner,
            treatment = tratLeafMiner,
        )

    private val infoRedMiteEarly =
        DiseaseInfo(
            name = "Ácaro Rojo — Nivel Inicial",
            description =
                "Primeras colonias de <i>Oligonychus punicae</i> (Red Mite) sobre el haz foliar. " +
                    "Se aprecian manchas pequeñas pardo-rojizas aisladas. " +
                    "La infestación es temprana y se encuentra en etapa óptima para control biológico o acaricida preventivo.",
            prevention = prevMites,
            causes = causasRedMite,
            treatment = tratMites,
        )

    private val infoRedMiteIntermediate =
        DiseaseInfo(
            name = "Ácaro Rojo — Nivel Intermedio",
            description =
                "Infestación moderada de <i>Oligonychus punicae</i>. " +
                    "Las manchas pardo-rojizas cubren amplios sectores del haz de las hojas, adquiriendo una apariencia sucia o herrumbrosa. " +
                    "Afecta significativamente la transpiración y fotosíntesis del cultivo.",
            prevention = prevMites,
            causes = causasRedMite,
            treatment = tratMites,
        )

    private val infoRedMiteAdvanced =
        DiseaseInfo(
            name = "Ácaro Rojo — Nivel Avanzado",
            description =
                "Infestación severa de <i>Oligonychus punicae</i>. " +
                    "Coloración parda-rojiza generalizada en toda la copa, hojas acorchadas, quebradizas y defoliación prematura. " +
                    "Requiere control químico inmediato con rotación de acaricidas para salvar el follaje productivo.",
            prevention = prevMites,
            causes = causasRedMite,
            treatment = tratMites,
        )

    private val infoHealthy =
        DiseaseInfo(
            name = "Hoja Saludable",
            description =
                "La hoja de palto se encuentra en condiciones óptimas: coloración verde intensa y uniforme, sin signos de plagas (ácaros, minador), necrosis ni manchas patológicas. " +
                    "Indica un cultivo vigoroso y un excelente manejo agronómico.",
            prevention =
                "Para mantener la sanidad del palto: <br><br>" +
                    "<b>&#42; Fertilización balanceada:</b> Aportar nitrógeno, fósforo, potasio y microelementos (zinc, boro) según análisis de suelo y foliar. <br><br>" +
                    "<b>&#42; Riego tecnificado:</b> Proporcionar agua oportuna evitando el estrés hídrico y los encharcamientos. <br><br>" +
                    "<b>&#42; Monitoreo fitosanitario continuo:</b> Revisar periódicamente el huerto para detectar cualquier brote temprano.",
            causes = "No aplica. La hoja está sana y vigorosa.",
            treatment = "No aplica. Continuar con el plan de manejo agronómico preventivo.",
        )

    private val infoOtherDisease =
        DiseaseInfo(
            name = "Otras Enfermedades",
            description =
                "La hoja de palto muestra síntomas de una patología foliar diferente a las plagas específicas del modelo principal (ácaros o minador). " +
                    "Puede tratarse de enfermedades fúngicas comunes como Antracnosis (<i>Colletotrichum gloeosporioides</i>), Mancha angular (<i>Pseudocercospora purpurea</i>), Roña (<i>Sphaceloma perseae</i>) o deficiencias de nutrientes.",
            prevention =
                "Medidas fitosanitarias generales: <br><br>" +
                    "<b>&#42; Podas de aireación:</b> Facilitar la circulación de aire y entrada de luz para reducir la humedad relativa interna del árbol. <br><br>" +
                    "<b>&#42; Desinfección de herramientas:</b> Esterilizar tijeras y serruchos de poda con hipoclorito de sodio o alcohol al 70%. <br><br>" +
                    "<b>&#42; Cicatrización de heridas:</b> Aplicar pasta fúngica o cúprica en cortes de poda para evitar infecciones.",
            causes = "Agente fitopatógeno fúngico, bacteriano o alteración fisiológica fuera del catálogo de las 3 plagas entomológicas principales.",
            treatment =
                "Recomendaciones de manejo: <br><br>" +
                    "<b>&#42; Diagnóstico técnico:</b> Consultar a un ingeniero agrónomo o laboratorio fitopatológico para identificar el patógeno exacto. <br><br>" +
                    "<b>&#42; Tratamiento fungicida:</b> En caso de hongos foliares, aplicar fungicidas cúpricos o azoxistrobina de forma dirigida según dosis técnica. <br><br>" +
                    "<b>&#42; Eliminación de material enfermo:</b> Recoger hojas y ramas caídas para evitar la dispersión de esporas.",
        )

    private val infoInconclusive =
        DiseaseInfo(
            name = "Diagnóstico Incierto",
            description =
                "El análisis visual no alcanzó el nivel mínimo de certeza necesario (65%). " +
                    "Esto ocurre habitualmente cuando la hoja presenta síntomas ambiguos, sombras o reflejos excesivos, necrosis avanzada difícil de atribuir a un único agente, o cuando la fotografía fue tomada por el envés (cara inferior) en lugar del haz.",
            prevention =
                "Consejos para una captura óptima: <br><br>" +
                    "<b>&#42; Cara de la hoja:</b> Fotografiar preferentemente el <b>haz</b> (la cara superior expuesta al sol). <br><br>" +
                    "<b>&#42; Iluminación uniforme:</b> Usar luz natural difusa y evitar proyectar sombras directas del teléfono sobre la hoja. <br><br>" +
                    "<b>&#42; Fondo neutro:</b> Colocar la hoja sobre una superficie plana y sin reflejos.",
            causes = "Certeza estadística insuficiente del modelo de visión artificial o condiciones complejas en la toma.",
            treatment = "Tomar una nueva fotografía enfocando de cerca la zona afectada siguiendo las pautas de captura óptima.",
        )

    // ──────────────────────────────────────────────────────────────────────────
    // Base de datos de enfermedades — Mapeo completo (Inglés + Español + Latín)
    // ──────────────────────────────────────────────────────────────────────────
    val diseaseDatabase =
        mapOf(
            // ─── CLASES EXACTAS DEL DATASET / NOTEBOOK PALTO SCAN ─────────────
            "CrystalMite_Damage_Early" to infoCrystalMiteEarly,
            "CrystalMite_Damage_Intermediate" to infoCrystalMiteIntermediate,
            "CrystalMite_Damage_Advanced" to infoCrystalMiteAdvanced,

            "LeafMiner_Damage_Early" to infoLeafMinerEarly,
            "LeafMiner_Damage_Intermediate" to infoLeafMinerIntermediate,
            "LeafMiner_Damage_Advanced" to infoLeafMinerAdvanced,

            "RedMite_Damage_Early" to infoRedMiteEarly,
            "RedMite_Damage_Intermediate" to infoRedMiteIntermediate,
            "RedMite_Damage_Advanced" to infoRedMiteAdvanced,

            "Healthy" to infoHealthy,
            "OtherDisease" to infoOtherDisease,
            "OtherDiseases" to infoOtherDisease,
            "Diagnóstico Incierto" to infoInconclusive,

            // ─── NOMBRES COMPLETOS EN ESPAÑOL ─────────────────────────────────
            infoCrystalMiteEarly.name to infoCrystalMiteEarly,
            infoCrystalMiteIntermediate.name to infoCrystalMiteIntermediate,
            infoCrystalMiteAdvanced.name to infoCrystalMiteAdvanced,

            infoLeafMinerEarly.name to infoLeafMinerEarly,
            infoLeafMinerIntermediate.name to infoLeafMinerIntermediate,
            infoLeafMinerAdvanced.name to infoLeafMinerAdvanced,

            infoRedMiteEarly.name to infoRedMiteEarly,
            infoRedMiteIntermediate.name to infoRedMiteIntermediate,
            infoRedMiteAdvanced.name to infoRedMiteAdvanced,

            infoHealthy.name to infoHealthy,
            infoOtherDisease.name to infoOtherDisease,
            infoInconclusive.name to infoInconclusive,

            // ─── ALIASES EN LATÍN / ALTERNATIVOS ──────────────────────────────
            "Caloptilia_perseae_Inicial" to infoLeafMinerEarly,
            "Caloptilia_perseae_Intermedio" to infoLeafMinerIntermediate,
            "Caloptilia_perseae_Avanzado" to infoLeafMinerAdvanced,

            "Oligonychus_perseae_Inicial" to infoCrystalMiteEarly,
            "Oligonychus_perseae_Intermedio" to infoCrystalMiteIntermediate,
            "Oligonychus_perseae_Avanzado" to infoCrystalMiteAdvanced,

            "Oligonychus_punicae_Inicial" to infoRedMiteEarly,
            "Oligonychus_punicae_Intermedio" to infoRedMiteIntermediate,
            "Oligonychus_punicae_Avanzado" to infoRedMiteAdvanced,
        )

    /**
     * Retorna el nombre legible y en español de la enfermedad para la interfaz de usuario.
     * Si no se encuentra en la base de datos, retorna el valor original.
     */
    fun getDisplayName(key: String?): String {
        if (key.isNullOrBlank()) return "No identificado"
        if (key == "No detectado" || key == "No reconocido" || key == "Desconocido" || key == "Error") return key
        return diseaseDatabase[key]?.name ?: key
    }
}
