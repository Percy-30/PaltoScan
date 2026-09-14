package com.atpdev.paltoscan.features.diseaseInfo

import com.atpdev.paltoscan.domain.model.DiseaseInfo

object DiseaseRepository {
    val diseaseDatabase =
        mapOf(
            "Antracnosis" to
                DiseaseInfo(
                    name = "Antracnosis",
                    description =
                        "La antracnosis es una enfermedad fúngica causada por el hongo Colletotrichum gloeosporioides. " +
                            "Afecta principalmente los frutos, hojas y ramas del palto, causando lesiones necróticas de color oscuro que pueden hundir los tejidos afectados. " +
                            "En hojas jóvenes provoca manchas marrones irregulares con bordes amarillentos que con el tiempo se necrosan completamente, comprometiendo la capacidad fotosintética del árbol.",
                    prevention =
                        "Para prevenir la aparición de la antracnosis, es importante implementar las siguientes medidas: <br><br>" +
                            "<b>&#42; Poda sanitaria:</b> Eliminar ramas y hojas infectadas para reducir la fuente de inóculo. <br><br>" +
                            "<b>&#42; Ventilación adecuada:</b> Mantener una densidad de copa que permita buena circulación de aire y penetración solar. <br><br>" +
                            "<b>&#42; Control de humedad:</b> Evitar el exceso de humedad en el follaje; usar riego por goteo en lugar de aspersión aérea. <br><br>" +
                            "<b>&#42; Desinfección de herramientas:</b> Limpiar y desinfectar tijeras y herramientas de poda para evitar la propagación del hongo.",
                    causes =
                        "La antracnosis es causada por el hongo Colletotrichum gloeosporioides. Sus esporas se diseminan mediante agua, viento e insectos. <br><br>" +
                            "<b>&#42; Hongo Colletotrichum gloeosporioides. </b><br>" +
                            "<b>&#42; Temperaturas cálidas (25–30°C). </b><br>" +
                            "<b>&#42; Alta humedad relativa (>80%). </b><br>" +
                            "<b>&#42; Heridas o lesiones en la planta. </b>",
                    treatment =
                        "El tratamiento de la antracnosis implica la aplicación de fungicidas específicos y medidas culturales: <br><br>" +
                            "<b>&#42; Fungicidas cúpricos:</b> Aplicar fungicidas a base de cobre (Oxicloruro de cobre) como preventivo. <br><br>" +
                            "<b>&#42; Fungicidas sistémicos:</b> Usar productos como Azoxystrobina o Tebuconazol en infecciones activas. <br><br>" +
                            "<b>&#42; Eliminación de tejido infectado:</b> Podar y destruir las partes afectadas para evitar la dispersión. <br><br>" +
                            "<b>&#42; Mejora del drenaje:</b> Asegurar que el suelo tenga buen drenaje para evitar condiciones favorables al hongo.",
                ),
            "Hoja_Saludable" to
                DiseaseInfo(
                    name = "Hoja Saludable",
                    description = "La planta de palto se encuentra en un estado saludable y sin síntomas visibles de enfermedad. Las hojas presentan coloración verde uniforme, sin manchas, necrosis ni deformaciones.",
                    prevention = "Mantener prácticas agrícolas saludables como la fertilización balanceada, el riego eficiente, la poda sanitaria periódica y el monitoreo constante del cultivo para detectar anomalías a tiempo.",
                    causes = "No aplica. La hoja está sana.",
                    treatment = "No aplica. Se recomienda continuar con el manejo agronómico preventivo.",
                ),
            "Cercospora" to
                DiseaseInfo(
                    name = "Cercospora",
                    description =
                        "La mancha por Cercospora es una enfermedad fúngica causada por Cercospora purpurea. " +
                            "Produce manchas angulares de color amarillo-verdoso en el haz de la hoja y una coloración púrpura o marrón en el envés. " +
                            "En ataques severos puede provocar defoliación prematura y reducción significativa en la producción del palto.",
                    prevention =
                        "Para prevenir la Cercospora en el palto: <br><br>" +
                            "<b>&#42; Monitoreo frecuente:</b> Inspeccionar regularmente el follaje para detectar los primeros síntomas. <br><br>" +
                            "<b>&#42; Densidad de plantación:</b> Mantener distancias adecuadas entre plantas para favorecer la ventilación. <br><br>" +
                            "<b>&#42; Control de malezas:</b> Eliminar malezas que puedan actuar como hospederos alternativos del hongo. <br><br>" +
                            "<b>&#42; Fertilización equilibrada:</b> Evitar exceso de nitrógeno que genera follaje suculento y más susceptible.",
                    causes =
                        "Causada por el hongo Cercospora purpurea, cuyas esporas se diseminan mediante el viento y el agua. <br><br>" +
                            "<b>&#42; Hongo Cercospora purpurea. </b><br>" +
                            "<b>&#42; Temperaturas de 20–28°C. </b><br>" +
                            "<b>&#42; Períodos de humedad prolongados. </b><br>" +
                            "<b>&#42; Exceso de nitrógeno en el follaje. </b>",
                    treatment =
                        "Para tratar la Cercospora en palto: <br><br>" +
                            "<b>&#42; Fungicidas preventivos:</b> Aplicar fungicidas cúpricos al inicio de las lluvias o períodos húmedos. <br><br>" +
                            "<b>&#42; Fungicidas curativos:</b> Usar productos a base de Mancozeb o Clorotalonil en infecciones establecidas. <br><br>" +
                            "<b>&#42; Poda de hojas afectadas:</b> Remover y destruir el material vegetal infectado. <br><br>" +
                            "<b>&#42; Mejorar la ventilación:</b> Realizar podas de formación para abrir la copa del árbol.",
                ),
            "No_Identificado" to
                DiseaseInfo(
                    name = "No Identificado",
                    description = "La imagen no corresponde a ninguna de las categorías reconocidas por PaltoScan, o la calidad de la imagen es insuficiente para realizar un diagnóstico preciso.",
                    prevention = "Asegúrese de tomar la foto con buena iluminación, encuadrando correctamente la hoja de palto. Evite fondos con muchos objetos y asegúrese de que la hoja ocupe la mayor parte de la imagen.",
                    causes = "No disponible. El modelo no pudo identificar la condición de la hoja con suficiente certeza.",
                    treatment = "No disponible. Se recomienda consultar a un agrónomo especialista para un diagnóstico presencial.",
                ),
        )
}
