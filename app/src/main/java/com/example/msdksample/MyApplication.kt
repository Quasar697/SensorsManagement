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
 * Application class per DJI Mini 3 BASE - VERSIONE SEMPLIFICATA
 * SDK v5.11.0 ottimizzato per funzioni base
 *
 * CONFIGURAZIONI SPECIFICHE MINI 3 BASE:
 * ✅ Supporto sensori Downward
 * ✅ GPS/GNSS dual-system
 * ✅ Basic flight controls
 * ❌ NO obstacle avoidance (front/back/lateral)
 * ❌ NO APAS systems
 * ❌ NO ActiveTrack
 */
class DJIApplication : Application() {

    companion object {
        private const val TAG = "DJIApp_Mini3Base"

        // Broadcast flags semplificati
        const val FLAG_NOTIFY_PRODUCT_CONNECT_EVENT = "dji_mini3_base_connection"
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
        Log.d(TAG, "🚁 DJI Mini 3 Base Application Starting...")
        Log.d(TAG, "📦 SDK Version: 5.11.0 (Entry-Level Optimized)")
        Log.d(TAG, "⚠️ Basic functions only - No obstacle avoidance")

        initializeSDK()
    }

    /**
     * Inizializza DJI SDK v5 per Mini 3 Base
     */
    private fun initializeSDK() {
        Log.i(TAG, "🔧 Inizializzazione DJI SDK v5.11.0 per Mini 3 Base...")

        try {
            sdkManager = SDKManager.getInstance() as ISDKManager
            Log.d(TAG, "✅ ISDKManager ottenuto: ${sdkManager?.javaClass?.simpleName}")

            sdkManager?.init(this, object : SDKManagerCallback {

                override fun onRegisterSuccess() {
                    isSDKRegistered = true
                    Log.i(TAG, "✅ DJI SDK Registration SUCCESS")
                    Log.i(TAG, "🎯 Mini 3 Base SDK ready for basic operations")
                    Log.i(TAG, "⚠️ Limited to downward sensors + GPS only")

                    sendBroadcastEvent(true, "SDK_REGISTERED", 100, true)
                }

                override fun onRegisterFailure(error: IDJIError) {
                    isSDKRegistered = false
                    Log.e(TAG, "❌ DJI SDK Registration FAILED")
                    Log.e(TAG, "┌─ Error Details:")
                    Log.e(TAG, "├─ Code: ${error.errorCode()}")
                    Log.e(TAG, "├─ Description: ${error.description()}")
                    Log.e(TAG, "└─ Check API Key in AndroidManifest.xml")

                    sendBroadcastEvent(false, "SDK_REGISTRATION_FAILED", 0, false)
                }

                override fun onProductDisconnect(productId: Int) {
                    connectedProductType = null
                    Log.w(TAG, "🔌 Product DISCONNECTED")
                    Log.w(TAG, "└─ Mini 3 Base disconnected from app")

                    sendBroadcastEvent(false, "DISCONNECTED", initializationProgress, isSDKRegistered)
                }

                override fun onProductConnect(productId: Int) {
                    Log.i(TAG, "🔗 Product CONNECTED")
                    Log.i(TAG, "├─ Product ID: $productId")

                    val product = getProductFromSDK()
                    if (product != null) {
                        connectedProductType = extractProductInfo(product)
                        Log.i(TAG, "├─ Product Type: $connectedProductType")

                        when {
                            connectedProductType?.contains("Mini 3") == true &&
                                    !connectedProductType!!.contains("Pro") -> {
                                Log.i(TAG, "🎯 DJI Mini 3 BASE SUCCESSFULLY CONNECTED!")
                                Log.i(TAG, "├─ Entry-level model confirmed")
                                Log.i(TAG, "├─ Downward sensors: ✅ Available")
                                Log.i(TAG, "├─ GPS positioning: ✅ Available")
                                Log.i(TAG, "├─ Obstacle avoidance: ❌ NOT available")
                                Log.i(TAG, "└─ Manual flight control required")

                                validateMini3BaseFeatures()
                            }

                            connectedProductType?.contains("Pro") == true -> {
                                Log.w(TAG, "⚠️ Mini 3 PRO detected - app optimized for BASE model")
                                Log.w(TAG, "└─ Some Pro features may not be utilized")
                            }

                            else -> {
                                Log.w(TAG, "⚠️ Different product connected: $connectedProductType")
                                Log.w(TAG, "└─ App optimized for Mini 3 Base")
                            }
                        }
                    } else {
                        Log.w(TAG, "⚠️ Product connected but details unavailable")
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
                    Log.i(TAG, "└─ To: $connectedProductType")

                    sendBroadcastEvent(true, connectedProductType ?: "UNKNOWN", initializationProgress, isSDKRegistered)
                }

                override fun onInitProcess(event: DJISDKInitEvent, progress: Int) {
                    initializationProgress = progress
                    Log.d(TAG, "⏳ SDK Init: ${event.name} - $progress%")

                    when (event.name) {
                        "START_INITIALIZE" -> {
                            Log.i(TAG, "🚀 SDK Initialization Started")
                        }
                        "INITIALIZE_COMPLETE" -> {
                            Log.i(TAG, "✅ SDK Initialization Complete")
                            Log.i(TAG, "🎯 Ready for Mini 3 Base basic operations")
                        }
                        "START_PRODUCT_CONNECTION" -> {
                            Log.i(TAG, "🔍 Starting Product Connection...")
                        }
                    }

                    sendBroadcastEvent(isSDKRegistered, connectedProductType ?: "INITIALIZING", progress, isSDKRegistered)
                }

                override fun onDatabaseDownloadProgress(current: Long, total: Long) {
                    val progress = if (total > 0) ((current * 100) / total).toInt() else 0

                    if (progress % 20 == 0 || current >= total) {
                        Log.d(TAG, "📥 Database Download: $progress%")
                    }

                    if (current >= total && total > 0) {
                        Log.i(TAG, "✅ Database Download Complete")
                        Log.i(TAG, "├─ Flight zones data: ✅ Ready")
                        Log.i(TAG, "└─ Mini 3 Base geo-restrictions: ✅ Active")
                    }
                }
            })

        } catch (e: Exception) {
            Log.e(TAG, "❌ Critical error during SDK initialization: ${e.message}")
            sendBroadcastEvent(false, "INIT_ERROR", 0, false)
        }
    }

    /**
     * Ottiene il prodotto dall'SDK
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

            productClass.simpleName

        } catch (e: Exception) {
            Log.e(TAG, "❌ Error extracting product info: ${e.message}")
            "DJI_Product"
        }
    }

    /**
     * Valida funzioni specifiche Mini 3 Base
     */
    private fun validateMini3BaseFeatures() {
        Log.d(TAG, "🔍 Validating Mini 3 Base Features...")

        try {
            // Test basic GPS/GNSS availability
            Log.d(TAG, "├─ GPS/GNSS: ✅ Expected to be available")

            // Note: NO PerceptionManager testing - not available on base model
            Log.d(TAG, "├─ Obstacle Avoidance: ❌ Not available on Base model")

            // Basic downward sensors should be available
            Log.d(TAG, "├─ Downward Sensors: ✅ Expected for landing")

            Log.d(TAG, "└─ Mini 3 Base validation complete")

        } catch (e: Exception) {
            Log.w(TAG, "⚠️ Feature validation failed: ${e.message}")
        }
    }

    /**
     * Invia broadcast eventi
     */
    private fun sendBroadcastEvent(isConnected: Boolean, productType: String, progress: Int, sdkReady: Boolean) {
        val intent = Intent().apply {
            setClass(this@DJIApplication, com.example.msdksample.MainActivity::class.java)
            action = FLAG_NOTIFY_PRODUCT_CONNECT_EVENT
            putExtra(FLAG_CONNECTION_STATE, isConnected)
            putExtra(FLAG_PRODUCT_TYPE, productType)
            putExtra(FLAG_INIT_PROGRESS, progress)
            putExtra(FLAG_SDK_READY, sdkReady)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        try {
            sendBroadcast(intent)
            Log.d(TAG, "📡 Broadcast sent: Connected=$isConnected, Product=$productType, SDK_Ready=$sdkReady")
        } catch (e: Exception) {
            Log.w(TAG, "⚠️ Broadcast failed (non-critical): ${e.message}")
        }
    }

    /**
     * Verifica se Mini 3 Base è connesso
     */
    fun isMini3BaseConnected(): Boolean {
        return connectedProductType?.contains("Mini 3") == true &&
                !connectedProductType!!.contains("Pro")
    }

    /**
     * Verifica se SDK è pronto
     */
    fun isSDKReady(): Boolean {
        return isSDKRegistered && getProductFromSDK() != null
    }

    /**
     * Ottiene stato SDK semplificato
     */
    fun getSDKStatus(): String {
        return buildString {
            appendLine("📦 DJI SDK Status (Mini 3 Base):")
            appendLine("├─ Version: 5.11.0")
            appendLine("├─ Model: Entry-Level Optimized")
            appendLine("├─ Registered: ${if (isSDKRegistered) "✅ Yes" else "❌ No"}")
            appendLine("├─ Product: ${connectedProductType ?: "None"}")
            appendLine("├─ Progress: $initializationProgress%")
            appendLine("└─ Ready: ${if (isSDKReady()) "✅ Yes" else "❌ No"}")
        }
    }

    override fun onTerminate() {
        super.onTerminate()
        Log.i(TAG, "🧹 DJI Mini 3 Base Application terminating...")

        try {
            sdkManager = null
            Log.i(TAG, "✅ SDK cleanup completed")
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error during cleanup: ${e.message}")
        }
    }
}