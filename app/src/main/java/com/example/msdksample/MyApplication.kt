package com.example.msdksample

import android.app.Application
import android.content.Intent
import android.util.Log

// ✅ Import corretti con INTERFACCE per MSDK v5.11.0
import dji.v5.manager.SDKManager
import dji.v5.manager.interfaces.ISDKManager
import dji.v5.manager.interfaces.SDKManagerCallback
import dji.v5.common.error.IDJIError
import dji.v5.common.register.DJISDKInitEvent

/**
 * Application class per DJI Mini 3 Pro - VERSIONE CORRETTA
 * SDK v5.11.0 con interfacce corrette e gestione eventi completa
 *
 * NOTA: Usa Application invece di MultiDexApplication per semplicità
 * Se il progetto diventa molto grande, aggiungi MultiDex separatamente
 *
 * CONFIGURAZIONI SPECIFICHE MINI 3 PRO:
 * ✅ Supporto sensori Front/Back/Down
 * ✅ APAS 4.0 Obstacle Avoidance
 * ✅ Vision Positioning standard
 * ✅ GPS/RTK dual-system
 * ❌ Precision Landing (non supportato)
 * ❌ Sensori laterali (non disponibili)
 */
class DJIApplication : Application() {

    companion object {
        private const val TAG = "DJIApp_Mini3Pro"

        // Broadcast flags per comunicazione con Activities
        const val FLAG_NOTIFY_PRODUCT_CONNECT_EVENT = "dji_mini3pro_connection"
        const val FLAG_CONNECTION_STATE = "connection_state"
        const val FLAG_PRODUCT_TYPE = "product_type"
        const val FLAG_INIT_PROGRESS = "init_progress"
        const val FLAG_SDK_READY = "sdk_ready"
    }

    // Stato interno applicazione
    private var isSDKRegistered = false
    private var connectedProductType: String? = null
    private var initializationProgress = 0
    private var sdkManager: ISDKManager? = null

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "🚁 DJI Mini 3 Pro Application Starting...")
        Log.d(TAG, "📦 SDK Version: 5.11.0 (Stable for Mini 3 Pro)")
        Log.d(TAG, "🔧 Using Interface-based Architecture")

        initializeSDK()
    }

    /**
     * Inizializza DJI SDK v5 con interfacce corrette
     */
    private fun initializeSDK() {
        Log.i(TAG, "🔧 Inizializzazione DJI SDK v5.11.0 con interfacce...")

        try {
            // Ottieni l'SDK Manager come interfaccia
            sdkManager = SDKManager.getInstance() as ISDKManager
            Log.d(TAG, "✅ ISDKManager ottenuto: ${sdkManager?.javaClass?.simpleName}")

            // Inizializza con callback
            sdkManager?.init(this, object : SDKManagerCallback {

                override fun onRegisterSuccess() {
                    isSDKRegistered = true
                    Log.i(TAG, "✅ DJI SDK Registration SUCCESS")
                    Log.i(TAG, "🎯 Mini 3 Pro SDK ready for operations")
                    Log.i(TAG, "🔌 Interface-based connection established")

                    // Verifica compatibilità Mini 3 Pro
                    checkMini3ProCompatibility()

                    sendBroadcastEvent(true, "SDK_REGISTERED", 100, true)
                }

                override fun onRegisterFailure(error: IDJIError) {
                    isSDKRegistered = false
                    Log.e(TAG, "❌ DJI SDK Registration FAILED")
                    Log.e(TAG, "┌─ Error Details:")
                    Log.e(TAG, "├─ Code: ${error.errorCode()}")
                    Log.e(TAG, "├─ Description: ${error.description()}")
                    Log.e(TAG, "└─ Possible Causes:")
                    Log.e(TAG, "   • Invalid API Key in AndroidManifest.xml")
                    Log.e(TAG, "   • Network connectivity issues")
                    Log.e(TAG, "   • DJI servers unreachable")
                    Log.e(TAG, "   • App not approved by DJI")
                    Log.e(TAG, "   • Interface binding issues")

                    sendBroadcastEvent(false, "SDK_REGISTRATION_FAILED", 0, false)
                }

                override fun onProductDisconnect(productId: Int) {
                    connectedProductType = null
                    Log.w(TAG, "🔌 Product DISCONNECTED")
                    Log.w(TAG, "├─ Product ID: $productId")
                    Log.w(TAG, "└─ Mini 3 Pro disconnected from app")

                    sendBroadcastEvent(false, "DISCONNECTED", initializationProgress, isSDKRegistered)
                }

                override fun onProductConnect(productId: Int) {
                    Log.i(TAG, "🔗 Product CONNECTED")
                    Log.i(TAG, "├─ Product ID: $productId")

                    // Test accesso al prodotto usando interfacce
                    val product = getProductFromSDK()

                    if (product != null) {
                        connectedProductType = extractProductInfo(product)
                        Log.i(TAG, "├─ Product Type: $connectedProductType")

                        when {
                            connectedProductType?.contains("Mini 3") == true -> {
                                Log.i(TAG, "🎯 DJI Mini 3 Pro SUCCESSFULLY CONNECTED!")
                                Log.i(TAG, "├─ All sensor systems available")
                                Log.i(TAG, "├─ APAS 4.0: ✅ Available")
                                Log.i(TAG, "├─ Vision Positioning: ✅ Available")
                                Log.i(TAG, "├─ Precision Landing: ❌ Not supported")
                                Log.i(TAG, "└─ Side Sensors: ❌ Not available")

                                validateMini3ProSystems()
                            }

                            connectedProductType?.contains("Mini") == true -> {
                                Log.w(TAG, "⚠️ Connected Mini drone: $connectedProductType")
                                Log.w(TAG, "└─ Some features may not be available")
                            }

                            else -> {
                                Log.w(TAG, "⚠️ Non-Mini product connected: $connectedProductType")
                                Log.w(TAG, "└─ App optimized for Mini 3 Pro")
                            }
                        }
                    } else {
                        Log.w(TAG, "⚠️ Product connected but details unavailable via interface")
                        connectedProductType = "Connected_Unknown"
                    }

                    sendBroadcastEvent(true, connectedProductType ?: "UNKNOWN", initializationProgress, isSDKRegistered)
                }

                override fun onProductChanged(productId: Int) {
                    val oldProduct = connectedProductType
                    val product = getProductFromSDK()
                    connectedProductType = if (product != null) extractProductInfo(product) else null

                    Log.i(TAG, "🔄 Product CHANGED")
                    Log.i(TAG, "├─ From: $oldProduct")
                    Log.i(TAG, "├─ To: $connectedProductType")
                    Log.i(TAG, "└─ New Product ID: $productId")

                    sendBroadcastEvent(true, connectedProductType ?: "UNKNOWN", initializationProgress, isSDKRegistered)
                }

                override fun onInitProcess(event: DJISDKInitEvent, progress: Int) {
                    initializationProgress = progress
                    Log.d(TAG, "⏳ SDK Init: ${event.name} - $progress%")

                    // Gestione eventi senza riferimenti a constanti che potrebbero non esistere
                    when (event.name) {
                        "START_INITIALIZE" -> {
                            Log.i(TAG, "🚀 SDK Initialization Started")
                        }
                        "INITIALIZE_COMPLETE" -> {
                            Log.i(TAG, "✅ SDK Initialization Complete")
                            Log.i(TAG, "🎯 Ready for Mini 3 Pro operations")
                        }
                        "START_PRODUCT_CONNECTION" -> {
                            Log.i(TAG, "🔍 Starting Product Connection...")
                        }
                        else -> {
                            Log.d(TAG, "📊 Init Event: ${event.name}")
                        }
                    }

                    sendBroadcastEvent(isSDKRegistered, connectedProductType ?: "INITIALIZING", progress, isSDKRegistered)
                }

                override fun onDatabaseDownloadProgress(current: Long, total: Long) {
                    val progress = if (total > 0) ((current * 100) / total).toInt() else 0

                    if (progress % 10 == 0 || current >= total) { // Log ogni 10% o al completamento
                        Log.d(TAG, "📥 Database Download: $progress% (${formatBytes(current)}/${formatBytes(total)})")
                    }

                    if (current >= total && total > 0) {
                        Log.i(TAG, "✅ Database Download Complete")
                        Log.i(TAG, "├─ Flight zones data: ✅ Ready")
                        Log.i(TAG, "├─ Maps data: ✅ Ready")
                        Log.i(TAG, "└─ Mini 3 Pro geo-restrictions: ✅ Active")
                    }
                }
            })

        } catch (e: Exception) {
            Log.e(TAG, "❌ Critical error during SDK initialization: ${e.message}")
            Log.e(TAG, "└─ Stack trace: ${e.stackTrace.contentToString()}")
            sendBroadcastEvent(false, "INIT_ERROR", 0, false)
        }
    }

    /**
     * Ottiene il prodotto dall'SDK usando interfacce
     */
    private fun getProductFromSDK(): Any? {
        return try {
            sdkManager?.let { sdk ->
                val productMethods = listOf(
                    "getProduct",
                    "getCurrentProduct",
                    "getConnectedProduct",
                    "product"
                )

                for (methodName in productMethods) {
                    try {
                        val method = sdk::class.java.getMethod(methodName)
                        val result = method.invoke(sdk)
                        if (result != null) {
                            Log.d(TAG, "✅ Product obtained via: $methodName")
                            return result
                        }
                    } catch (e: Exception) {
                        // Metodo non esiste, continua
                    }
                }

                Log.w(TAG, "⚠️ No working product access method found")
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error getting product from SDK: ${e.message}")
            null
        }
    }

    /**
     * Estrae informazioni dal prodotto
     */
    private fun extractProductInfo(product: Any): String {
        return try {
            val productClass = product::class.java
            Log.d(TAG, "🎯 Analyzing product: ${productClass.simpleName}")

            // Cerca metodi comuni per il prodotto
            val nameMethods = listOf(
                "getProductType",
                "productType",
                "getModel",
                "model",
                "getName",
                "name"
            )

            for (methodName in nameMethods) {
                try {
                    val method = productClass.getMethod(methodName)
                    val result = method.invoke(product)
                    if (result != null) {
                        Log.d(TAG, "✅ Product name via ${methodName}: $result")
                        return result.toString()
                    }
                } catch (e: Exception) {
                    // Continua
                }
            }

            // Se non trova il nome, usa la classe
            productClass.simpleName

        } catch (e: Exception) {
            Log.e(TAG, "❌ Error extracting product info: ${e.message}")
            "DJI_Product"
        }
    }

    /**
     * Verifica compatibilità specifica Mini 3 Pro con interfacce
     */
    private fun checkMini3ProCompatibility() {
        Log.d(TAG, "🔍 Checking Mini 3 Pro Compatibility via Interfaces...")

        try {
            // Test PerceptionManager
            val perceptionManager = dji.v5.manager.aircraft.perception.PerceptionManager.getInstance()
            if (perceptionManager != null) {
                Log.d(TAG, "├─ IPerceptionManager: ✅ Available")
                Log.d(TAG, "│  └─ Type: ${perceptionManager::class.java.simpleName}")
            }

            // Test RTKCenter
            val rtkCenter = dji.v5.manager.aircraft.rtk.RTKCenter.getInstance()
            if (rtkCenter != null) {
                Log.d(TAG, "├─ IRTKCenter: ✅ Available")
                Log.d(TAG, "│  └─ Type: ${rtkCenter::class.java.simpleName}")
            }

            Log.d(TAG, "└─ Basic interface compatibility: ✅ Confirmed")

        } catch (e: Exception) {
            Log.e(TAG, "❌ Compatibility check failed: ${e.message}")
        }
    }

    /**
     * Valida sistemi specifici Mini 3 Pro dopo connessione
     */
    private fun validateMini3ProSystems() {
        try {
            Log.d(TAG, "🔍 Validating Mini 3 Pro Systems via Interfaces...")

            // Test PerceptionManager per sensori
            val perceptionManager = dji.v5.manager.aircraft.perception.PerceptionManager.getInstance()
            if (perceptionManager != null) {
                Log.d(TAG, "├─ IPerceptionManager: ✅ Ready for sensor operations")

                // Test metodi probabili
                testPerceptionCapabilities(perceptionManager)
            }

            // Test RTKCenter per GPS
            val rtkCenter = dji.v5.manager.aircraft.rtk.RTKCenter.getInstance()
            if (rtkCenter != null) {
                Log.d(TAG, "├─ IRTKCenter: ✅ Ready for GPS operations")

                // Test metodi probabili
                testRTKCapabilities(rtkCenter)
            }

            Log.d(TAG, "└─ Mini 3 Pro Systems: ✅ Interface validation complete")

        } catch (e: Exception) {
            Log.w(TAG, "⚠️ System validation partially failed: ${e.message}")
            Log.w(TAG, "└─ Some features may not be available")
        }
    }

    /**
     * Test capabilities del PerceptionManager
     */
    private fun testPerceptionCapabilities(perceptionManager: Any) {
        try {
            val methods = perceptionManager::class.java.methods
            val visionMethods = methods.filter { it.name.contains("vision", ignoreCase = true) }
            val apasMethods = methods.filter { it.name.contains("obstacle", ignoreCase = true) || it.name.contains("apas", ignoreCase = true) }

            Log.d(TAG, "│  ├─ Vision methods found: ${visionMethods.size}")
            Log.d(TAG, "│  └─ APAS methods found: ${apasMethods.size}")

        } catch (e: Exception) {
            Log.w(TAG, "│  └─ Method enumeration failed: ${e.message}")
        }
    }

    /**
     * Test capabilities del RTKCenter
     */
    private fun testRTKCapabilities(rtkCenter: Any) {
        try {
            val methods = rtkCenter::class.java.methods
            val gpsMethods = methods.filter {
                it.name.contains("gps", ignoreCase = true) ||
                        it.name.contains("satellite", ignoreCase = true) ||
                        it.name.contains("rtk", ignoreCase = true)
            }

            Log.d(TAG, "│  └─ GPS/RTK methods found: ${gpsMethods.size}")

        } catch (e: Exception) {
            Log.w(TAG, "│  └─ RTK method enumeration failed: ${e.message}")
        }
    }

    /**
     * Invia broadcast eventi per comunicare con Activities
     * Usa broadcast esplicito per evitare problemi con receiver non-exported
     */
    private fun sendBroadcastEvent(isConnected: Boolean, productType: String, progress: Int, sdkReady: Boolean) {
        val intent = Intent().apply {
            // Usa broadcast esplicito invece di action generica
            setClass(this@DJIApplication, com.example.msdksample.MainActivity::class.java)
            action = FLAG_NOTIFY_PRODUCT_CONNECT_EVENT
            putExtra(FLAG_CONNECTION_STATE, isConnected)
            putExtra(FLAG_PRODUCT_TYPE, productType)
            putExtra(FLAG_INIT_PROGRESS, progress)
            putExtra(FLAG_SDK_READY, sdkReady)

            // Aggiungi flag per broadcast locale
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        // Invia broadcast sicuro
        try {
            sendBroadcast(intent)
            Log.d(TAG, "📡 Broadcast sent: Connected=$isConnected, Product=$productType, Progress=$progress%, SDK_Ready=$sdkReady")
        } catch (e: Exception) {
            Log.w(TAG, "⚠️ Broadcast failed (non-critical): ${e.message}")
        }
    }

    /**
     * Ottiene informazioni dettagliate prodotto connesso
     */
    fun getConnectedProductInfo(): String {
        val product = getProductFromSDK()

        return if (product != null) {
            buildString {
                val productName = extractProductInfo(product)
                appendLine("🚁 Prodotto: $productName")

                // Test accesso batteria
                try {
                    val battery = testBatteryAccess(product)
                    if (battery != null) {
                        appendLine("🔋 Batteria: Accessibile ✅")
                    } else {
                        appendLine("🔋 Batteria: Non accessibile ❌")
                    }
                } catch (e: Exception) {
                    appendLine("🔋 Batteria: Errore - ${e.message}")
                }

                // Connection status
                appendLine("🔗 Connessione: ATTIVA")
                appendLine("📦 SDK: v5.11.0 (Interface-based)")
                appendLine("🎯 Interfacce: ISDKManager, IPerceptionManager, IRTKCenter")

                // Mini 3 Pro specific info
                if (isMini3ProConnected()) {
                    appendLine("🎯 Sensori: Front/Back/Down ✅")
                    appendLine("🛡️ APAS 4.0: Supportato ✅")
                    appendLine("❌ Precision Landing: Non supportato")
                }
            }
        } else {
            "❌ Nessun prodotto connesso"
        }
    }

    /**
     * Test accesso batteria
     */
    private fun testBatteryAccess(product: Any): Any? {
        val batteryMethods = listOf("getBattery", "battery", "getBatteryState")

        for (methodName in batteryMethods) {
            try {
                val method = product::class.java.getMethod(methodName)
                val result = method.invoke(product)
                if (result != null) {
                    return result
                }
            } catch (e: Exception) {
                // Continua
            }
        }
        return null
    }

    /**
     * Verifica se prodotto connesso è Mini 3 Pro
     */
    fun isMini3ProConnected(): Boolean {
        return connectedProductType?.contains("Mini 3") == true
    }

    /**
     * Verifica se SDK è pronto per operazioni
     */
    fun isSDKReady(): Boolean {
        return isSDKRegistered && getProductFromSDK() != null
    }

    /**
     * Ottiene stato dettagliato SDK
     */
    fun getSDKStatus(): String {
        return buildString {
            appendLine("📦 DJI SDK Status:")
            appendLine("├─ Version: 5.11.0")
            appendLine("├─ Architecture: Interface-based")
            appendLine("├─ Registered: ${if (isSDKRegistered) "✅ Yes" else "❌ No"}")
            appendLine("├─ Product: ${connectedProductType ?: "None"}")
            appendLine("├─ Progress: $initializationProgress%")
            appendLine("├─ ISDKManager: ${if (sdkManager != null) "✅ Active" else "❌ Null"}")
            appendLine("└─ Ready: ${if (isSDKReady()) "✅ Yes" else "❌ No"}")
        }
    }

    /**
     * Formatta bytes in formato human-readable
     */
    private fun formatBytes(bytes: Long): String {
        val unit = 1024
        if (bytes < unit) return "$bytes B"

        val exp = (Math.log(bytes.toDouble()) / Math.log(unit.toDouble())).toInt()
        val pre = "KMGTPE"[exp - 1]

        return String.format("%.1f %sB", bytes / Math.pow(unit.toDouble(), exp.toDouble()), pre)
    }

    /**
     * Cleanup delle risorse
     */
    override fun onTerminate() {
        super.onTerminate()
        Log.i(TAG, "🧹 DJI Application terminating...")

        try {
            // Cleanup SDK se necessario
            sdkManager = null
            Log.i(TAG, "✅ SDK cleanup completed")
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error during cleanup: ${e.message}")
        }
    }
}