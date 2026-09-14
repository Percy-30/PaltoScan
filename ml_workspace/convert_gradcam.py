import os
# ¡ESTA LÍNEA ES LA MAGIA QUE ARREGLA EL ERROR EN COLAB!
os.environ["TF_USE_LEGACY_KERAS"] = "1" 

import tensorflow as tf
from tensorflow import keras

# 1. Definir rutas 
ruta_guardado_modelo = '/content/drive/MyDrive/MY PROJECT/modelos/fine_tune_model_fine_tune_true.keras'
ruta_tflite_gradcam = '/content/drive/MyDrive/MY PROJECT/modelos/fine_tune_model_gradcam.tflite'

# 2. Cargar el modelo base
print(f"Intentando cargar el modelo desde: {ruta_guardado_modelo}")
modelo_cnn = tf.keras.models.load_model(ruta_guardado_modelo, compile=False)

# 3. Encontrar la última capa convolucional para Grad-CAM
# (Buscamos la última que no sea de aplanado o densa)
last_conv_layer = None
for layer in reversed(modelo_cnn.layers):
    if len(layer.output_shape) == 4: # Nos aseguramos que sea una matriz 2D/3D (Batch, W, H, C)
        last_conv_layer = layer
        break

if last_conv_layer is None:
    print("Error: No se encontró ninguna capa de matriz en el modelo.")
else:
    print(f"Última capa espacial encontrada: {last_conv_layer.name} con forma {last_conv_layer.output_shape}")

    # 4. Crear el NUEVO modelo con DOS salidas
    gradcam_model = tf.keras.Model(
        inputs=modelo_cnn.inputs,
        outputs=[last_conv_layer.output, modelo_cnn.output]
    )

    # 5. Configurar el convertidor
    print("Iniciando conversión a TFLite...")
    converter = tf.lite.TFLiteConverter.from_keras_model(gradcam_model)
    
    converter.optimizations = [tf.lite.Optimize.DEFAULT]
    converter.target_spec.supported_types = [tf.float16]
    converter.experimental_new_converter = True

    # 6. Convertir y guardar
    tflite_model = converter.convert()
    
    os.makedirs(os.path.dirname(ruta_tflite_gradcam), exist_ok=True)
    
    with open(ruta_tflite_gradcam, 'wb') as f:
        f.write(tflite_model)
        
    print(f'¡Éxito! Modelo TFLite (Grad-CAM) exportado en: {ruta_tflite_gradcam}')
