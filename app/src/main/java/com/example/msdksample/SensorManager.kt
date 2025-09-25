package com.example.msdksample

import android.util.Log
import kotlinx.coroutines.*

// ✅ Import corretti per TUTTE le interfacce
import dji.v5.manager.interfaces.ISDKManager
import dji.v5.manager.interfaces.IPerceptionManager
import dji.v5.manager.interfaces.IRTKCenter
import dji.v5.manager.SDKManager
import dji.v5.common.callback.CommonCallbacks
import dji.v5.common.error.IDJIError

/**
 * SensorManager per DJI Mini 3 Pro - VERSIONE CORRETTA
 * Usa le interfacce corrette e API compatibili con minSdk 24
 */
class SensorManager {

    companion object {
        private const val TAG = "SensorManager_Mini3Pro"
    }

    private var sensorJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    // Callback semplificato
    interface SensorDataCallback {
        fun onConnectionStatusUpdated(isConnected: Boolean, productInfo: String)
        fun onSensorStatusUpdated(sensorType: String, status: String, isAvailable: Boolean)
        fun onSensorError(sensorType: String, error: String)
        fun onRawDataReceived(dataType: String, data: String)
    }

    private var callback: SensorDataCallback? = null

    fun setCallback(callback: SensorDataCallback) {
        this.callback = callback
    }

    /**
     * Avvia il monitoraggio con API sicure
     */
    fun startSensorReading(intervalMs: Long = 2000) {
        stopSensorReading()

        sensorJob = scope.launch {
            Log.i(TAG, "🔍 Avvio esplorazione API MSDK v5.11.0 (Safe Mode)...")

            while (isActive) {
                try {
                    exploreAvailableAPIs()
                    delay(intervalMs)
                } catch (e: Exception) {
                    Log.e(TAG, "❌ Errore esplorazione: ${e.message}")
                    callback?.onSensorError("Sistema", e.message ?: "Errore sconosciuto")
                    delay(intervalMs)
                }
            }
        }

        Log.d(TAG, "✅ Esplorazione avviata")
    }

    fun stopSensorReading() {
        sensorJob?.cancel()
        sensorJob = null
        Log.d(TAG, "⏹️ Esplorazione fermata")
    }

    /**
     * Esplora API usando le interfacce corrette
     */
    private suspend fun exploreAvailableAPIs() = withContext(Dispatchers.IO) {

        // 1. Test SDKManager (classe concreta)
        exploreSDKManager()

        // 2. Test IPerceptionManager (interfaccia)
        explorePerceptionManager()

        // 3. Test IRTKCenter (interfaccia)
        exploreRTKCenter()
    }

    /**
     * Esplora SDKManager (ora interfaccia ISDKManager)
     */
    private fun exploreSDKManager() {
        try {
            Log.d(TAG, "🔍 Esplorando ISDKManager...")

            val sdkManager = SDKManager.getInstance() // Ritorna ISDKManager!
            Log.d(TAG, "✅ SDKManager.getInstance() funziona")
            Log.d(TAG, "📋 Tipo: ${sdkManager::class.java.simpleName}")

            // Test metodi comuni senza usare reflection avanzata
            try {
                val methods = sdkManager::class.java.methods
                Log.d(TAG, "📋 Metodi ISDKManager trovati: ${methods.size}")

                // Cerca metodi che contengono parole chiave
                val productMethods = methods.filter {
                    it.name.lowercase().contains("product")
                }.map { it.name }

                val registerMethods = methods.filter {
                    it.name.lowercase().contains("register")
                }.map { it.name }

                Log.d(TAG, "🎯 Metodi product: $productMethods")
                Log.d(TAG, "📝 Metodi register: $registerMethods")

                callback?.onRawDataReceived("ISDKManager", "Methods: Product=$productMethods, Register=$registerMethods")

            } catch (e: Exception) {
                Log.w(TAG, "❌ Errore enumerazione metodi ISDKManager: ${e.message}")
            }

            // Test specifici per metodi probabili
            testSDKManagerMethods(sdkManager as ISDKManager)

        } catch (e: Exception) {
            Log.e(TAG, "❌ Errore ISDKManager: ${e.message}")
            callback?.onSensorError("ISDKManager", e.message ?: "Errore sconosciuto")
        }
    }

    /**
     * Test metodi specifici ISDKManager
     */
    private fun testSDKManagerMethods(sdkManager: ISDKManager) {
        // Lista di metodi probabili da testare
        val methodsToTest = listOf(
            "getProduct",
            "hasRegistered",
            "isRegistered",
            "getConnectedProduct",
            "getCurrentProduct"
        )

        methodsToTest.forEach { methodName ->
            try {
                val method = sdkManager::class.java.getMethod(methodName)
                val result = method.invoke(sdkManager)
                Log.d(TAG, "✅ $methodName() = $result")
                callback?.onRawDataReceived("SDKManager", "$methodName: $result")

                // Se è un metodo product, prova ad estrarre info
                if (methodName.lowercase().contains("product") && result != null) {
                    extractProductInfo(result)
                }

            } catch (e: NoSuchMethodException) {
                Log.w(TAG, "❌ Metodo $methodName() non esiste")
            } catch (e: Exception) {
                Log.w(TAG, "❌ Errore $methodName(): ${e.message}")
            }
        }
    }

    /**
     * Estrae informazioni dal prodotto
     */
    private fun extractProductInfo(product: Any) {
        try {
            val productClass = product::class.java
            Log.d(TAG, "🎯 Prodotto trovato: ${productClass.simpleName}")

            // Cerca metodi comuni per il prodotto
            val methods = productClass.methods.filter { method ->
                method.name.lowercase().let { name ->
                    name.contains("type") || name.contains("name") || name.contains("model") ||
                            name.contains("battery") || name.contains("version")
                }
            }

            methods.forEach { method ->
                try {
                    val result = method.invoke(product)
                    Log.d(TAG, "✅ Product.${method.name}() = $result")
                    callback?.onRawDataReceived("Product", "${method.name}: $result")
                } catch (e: Exception) {
                    Log.w(TAG, "❌ Product.${method.name}() fallito: ${e.message}")
                }
            }

            callback?.onConnectionStatusUpdated(true, "Prodotto: ${productClass.simpleName}")

        } catch (e: Exception) {
            Log.e(TAG, "❌ Errore estrazione info prodotto: ${e.message}")
        }
    }

    /**
     * Esplora IPerceptionManager
     */
    private fun explorePerceptionManager() {
        try {
            Log.d(TAG, "🔍 Esplorando IPerceptionManager...")

            val perceptionManager = dji.v5.manager.aircraft.perception.PerceptionManager.getInstance()
            Log.d(TAG, "✅ PerceptionManager.getInstance() funziona")
            Log.d(TAG, "📋 Tipo: ${perceptionManager::class.java.simpleName}")

            // Test dell'interfaccia IPerceptionManager
            testPerceptionMethods(perceptionManager as IPerceptionManager)

        } catch (e: Exception) {
            Log.e(TAG, "❌ Errore PerceptionManager: ${e.message}")
            callback?.onSensorError("PerceptionManager", e.message ?: "Errore sconosciuto")
        }
    }

    /**
     * Test metodi IPerceptionManager (interfaccia corretta)
     */
    private fun testPerceptionMethods(perceptionManager: IPerceptionManager) {
        // Lista metodi probabili da testare
        val methodsToTest = listOf(
            "isVisionPositioningSensorEnabled",
            "isObstacleAvoidanceEnabled",
            "getObstacleData",
            "obstacleData",
            "isVisionSensorEnabled",
            "getVisionSystemState"
        )

        methodsToTest.forEach { methodName ->
            try {
                val method = perceptionManager::class.java.getMethod(methodName)
                val result = method.invoke(perceptionManager)
                Log.d(TAG, "✅ Perception.$methodName() = $result")
                callback?.onRawDataReceived("Perception", "$methodName: $result")

            } catch (e: NoSuchMethodException) {
                Log.w(TAG, "❌ Metodo Perception.$methodName() non esiste")
            } catch (e: Exception) {
                Log.w(TAG, "❌ Errore Perception.$methodName(): ${e.message}")
            }
        }

        // Test metodi setter per APAS
        testAPASMethods(perceptionManager)
    }

    /**
     * Test specifici per controllo APAS
     */
    private fun testAPASMethods(perceptionManager: IPerceptionManager) {
        val setterMethods = listOf(
            "setObstacleAvoidanceEnabled",
            "setVisionPositioningEnabled",
            "enableObstacleAvoidance",
            "enableVisionPositioning"
        )

        setterMethods.forEach { methodName ->
            try {
                val methods = perceptionManager::class.java.methods.filter { it.name == methodName }

                methods.forEach { method ->
                    val paramCount = method.parameterTypes.size // Compatibile API 24+
                    Log.d(TAG, "🔍 Metodo $methodName ha $paramCount parametri")

                    when (paramCount) {
                        1 -> Log.d(TAG, "  • Parametro: ${method.parameterTypes[0].simpleName}")
                        2 -> Log.d(TAG, "  • Parametri: ${method.parameterTypes[0].simpleName}, ${method.parameterTypes[1].simpleName}")
                    }

                    callback?.onRawDataReceived("APAS Method", "$methodName($paramCount params)")
                }

            } catch (e: Exception) {
                Log.w(TAG, "❌ Errore analisi $methodName: ${e.message}")
            }
        }
    }

    /**
     * Esplora IRTKCenter
     */
    private fun exploreRTKCenter() {
        try {
            Log.d(TAG, "🔍 Esplorando IRTKCenter...")

            val rtkCenter = dji.v5.manager.aircraft.rtk.RTKCenter.getInstance()
            Log.d(TAG, "✅ RTKCenter.getInstance() funziona")
            Log.d(TAG, "📋 Tipo: ${rtkCenter::class.java.simpleName}")

            // Test dell'interfaccia IRTKCenter
            testRTKMethods(rtkCenter as IRTKCenter)

        } catch (e: Exception) {
            Log.e(TAG, "❌ Errore RTKCenter: ${e.message}")
            callback?.onSensorError("RTKCenter", e.message ?: "Errore sconosciuto")
        }
    }

    /**
     * Test metodi IRTKCenter (interfaccia corretta)
     */
    private fun testRTKMethods(rtkCenter: IRTKCenter) {
        val methodsToTest = listOf(
            "getRTKSystemState",
            "rtkSystemState",
            "getSystemState",
            "getSatelliteCount",
            "getPositioningSolution",
            "isRTKEnabled",
            "getRTKStatus"
        )

        methodsToTest.forEach { methodName ->
            try {
                val method = rtkCenter::class.java.getMethod(methodName)
                val result = method.invoke(rtkCenter)
                Log.d(TAG, "✅ RTK.$methodName() = $result")
                callback?.onRawDataReceived("RTK", "$methodName: $result")

                // Se ottiene uno stato RTK, analizzalo
                if (result != null && methodName.lowercase().contains("state")) {
                    analyzeRTKState(result)
                }

            } catch (e: NoSuchMethodException) {
                Log.w(TAG, "❌ Metodo RTK.$methodName() non esiste")
            } catch (e: Exception) {
                Log.w(TAG, "❌ Errore RTK.$methodName(): ${e.message}")
            }
        }
    }

    /**
     * Analizza lo stato RTK
     */
    private fun analyzeRTKState(rtkState: Any) {
        try {
            val stateClass = rtkState::class.java
            Log.d(TAG, "🛰️ RTK State: ${stateClass.simpleName}")

            val methods = stateClass.methods.filter { method ->
                method.name.lowercase().let { name ->
                    name.contains("satellite") || name.contains("solution") ||
                            name.contains("count") || name.contains("accuracy")
                }
            }

            methods.forEach { method ->
                try {
                    val result = method.invoke(rtkState)
                    Log.d(TAG, "✅ RTKState.${method.name}() = $result")
                    callback?.onRawDataReceived("RTK State", "${method.name}: $result")
                } catch (e: Exception) {
                    Log.w(TAG, "❌ RTKState.${method.name}() fallito: ${e.message}")
                }
            }

        } catch (e: Exception) {
            Log.e(TAG, "❌ Errore analisi RTK state: ${e.message}")
        }
    }

    /**
     * Test controllo APAS con metodi trovati
     */
    fun testAPASControl(enable: Boolean, callback: (Boolean, String) -> Unit) {
        try {
            val perceptionManager = dji.v5.manager.aircraft.perception.PerceptionManager.getInstance() as IPerceptionManager

            // Prova il metodo più probabile per APAS
            try {
                val method = perceptionManager::class.java.getMethod("setObstacleAvoidanceEnabled", Boolean::class.java, CommonCallbacks.CompletionCallback::class.java)

                method.invoke(perceptionManager, enable, object : CommonCallbacks.CompletionCallback {
                    override fun onSuccess() {
                        Log.i(TAG, "✅ APAS ${if (enable) "abilitato" else "disabilitato"}")
                        callback(true, "APAS ${if (enable) "abilitato" else "disabilitato"} con successo")
                    }

                    override fun onFailure(error: IDJIError) {
                        Log.e(TAG, "❌ Errore APAS: ${error.description()}")
                        callback(false, "Errore APAS: ${error.description()}")
                    }
                })

            } catch (e: NoSuchMethodException) {
                // Prova metodo alternativo
                try {
                    val method = perceptionManager::class.java.getMethod("setObstacleAvoidanceEnabled", Boolean::class.java)
                    method.invoke(perceptionManager, enable)
                    callback(true, "APAS ${if (enable) "abilitato" else "disabilitato"} (sync)")
                } catch (e2: Exception) {
                    callback(false, "Nessun metodo APAS funzionante trovato")
                }
            }

        } catch (e: Exception) {
            Log.e(TAG, "❌ Errore test APAS: ${e.message}")
            callback(false, "Eccezione controllo APAS: ${e.message}")
        }
    }

    /**
     * Genera report delle API funzionanti
     */
    fun generateAPIReport(): String {
        return buildString {
            appendLine("🔍 DJI MSDK v5.11.0 - API Report (Corrected)")
            appendLine("═".repeat(50))
            appendLine("📱 Min SDK: 24 | Target SDK: 34")
            appendLine("🚁 Target: DJI Mini 3 Pro")
            appendLine()

            appendLine("✅ INTERFACCE CONFERMATE:")
            appendLine("• ISDKManager - Gestione SDK e prodotto")
            appendLine("• IPerceptionManager - Sensori e APAS")
            appendLine("• IRTKCenter - GPS/RTK System")
            appendLine()

            appendLine("🔧 METODI DA TESTARE:")
            appendLine("• ISDKManager.getProduct() o getCurrentProduct()")
            appendLine("• IPerceptionManager.setObstacleAvoidanceEnabled()")
            appendLine("• IPerceptionManager.setVisionPositioningEnabled()")
            appendLine("• IRTKCenter.getRTKSystemState()")
            appendLine()

            appendLine("⚠️ CORREZIONI FINALI:")
            appendLine("• SDKManager → ISDKManager interface")
            appendLine("• PerceptionManager → IPerceptionManager interface")
            appendLine("• RTKCenter → IRTKCenter interface")
            appendLine("• Tutti i manager sono interfacce, non classi concrete")
        }
    }

    fun cleanup() {
        stopSensorReading()
        scope.cancel()
        callback = null
        Log.i(TAG, "🧹 SensorManager cleanup completato")
    }
}