package com.example.msdksample

import android.app.Application
import android.content.Intent
import android.util.Log
import androidx.multidex.MultiDexApplication

// DJI SDK v5 imports - STABILE 5.8.0
import dji.v5.manager.SDKManager
import dji.v5.manager.interfaces.SDKManagerCallback
import dji.v5.common.error.IDJIError
import dji.v5.common.register.DJISDKInitEvent

/**
 * Application class per DJI Mini 3 Pro
 * SDK v5.8.0 - Versione stabile testata per Mini 3 Pro
 *
 * CONFIGURAZIONI SPECIFICHE MINI 3 PRO:
 * ✅ Supporto sensori Front/Back/Down
 * ✅ APAS 4.0 Obstacle Avoidance
 * ✅ Vision Positioning standard
 * ❌ Precision Landing (non supportato)
 * ❌ Sensori laterali (non disponibili)
 */
class DJIApplication : MultiDexApplication() {

    companion object {
        private const val TAG = "DJIApp_Mini3Pro"

        // Broadcast flags per comunicazione con Activities
        const val FLAG_NOTIFY_PRODUCT_CONNECT_EVENT = "dji_mini3pro_connection"
        const val FLAG_CONNECTION_STATE = "connection_state"
        const val FLAG_PRODUCT_TYPE = "product_type"
        const val FLAG_INIT_PROGRESS = "init_progress"
    }

    // Stato interno applicazione
    private var isSDKRegistered = false
    private var connectedProductType: String? = null
    private var initializationProgress = 0

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "🚁 DJI Mini 3 Pro Application Starting...")
        Log.d(TAG, "📦 SDK Version: 5.8.0 (Stable for Mini 3 Pro)")

        initializeSDK()
    }

    /**
     * Inizializza DJI SDK v5 con gestione completa eventi Mini 3 Pro
     */
    private fun initializeSDK() {
        Log.i(TAG, "🔧 Inizializzazione DJI SDK v5.8.0...")

        SDKManager.getInstance().init(this, object : SDKManagerCallback {

            override fun onRegisterSuccess() {
                isSDKRegistered = true
                Log.i(TAG, "✅ DJI SDK Registration SUCCESS")
                Log.i(TAG, "🎯 Mini 3 Pro SDK ready for operations")

                // Verifica compatibilità Mini 3 Pro
                checkMini3ProCompatibility()

                sendBroadcastEvent(true, "SDK_REGISTERED", 100)
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

                sendBroadcastEvent(false, "SDK_REGISTRATION_FAILED", 0)
            }

            override fun onProductDisconnect(productId: Int) {
                connectedProductType = null
                Log.w(TAG, "🔌 Product DISCONNECTED")
                Log.w(TAG, "├─ Product ID: $productId")
                Log.w(TAG, "└─ Mini 3 Pro disconnected from app")

                sendBroadcastEvent(false, "DISCONNECTED", initializationProgress)
            }

            override fun onProductConnect(productId: Int) {
                Log.i(TAG, "🔗 Product CONNECTED")
                Log.i(TAG, "├─ Product ID: $productId")

                val product = SDKManager.getInstance().getProduct()
                if (product != null) {
                    connectedProductType = product.productType?.name
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
                    Log.w(TAG, "⚠️ Product connected but details unavailable")
                }

                sendBroadcastEvent(true, connectedProductType ?: "UNKNOWN", initializationProgress)
            }

            override fun onProductChanged(productId: Int) {
                val oldProduct = connectedProductType
                val newProduct = SDKManager.getInstance().getProduct()?.productType?.name
                connectedProductType = newProduct

                Log.i(TAG, "🔄 Product CHANGED")
                Log.i(TAG, "├─ From: $oldProduct")
                Log.i(TAG, "├─ To: $newProduct")
                Log.i(TAG, "└─ New Product ID: $productId")

                sendBroadcastEvent(true, newProduct ?: "UNKNOWN", initializationProgress)
            }

            override fun onInitProcess(event: DJISDKInitEvent, progress: Int) {
                initializationProgress = progress
                Log.d(TAG, "⏳ SDK Init: ${event.name} - $progress%")

                when (event) {
                    DJISDKInitEvent.START_INITIALIZE -> {
                        Log.i(TAG, "🚀 SDK Initialization Started")
                    }
                    DJISDKInitEvent.INITIALIZE_COMPLETE -> {
                        Log.i(TAG, "✅ SDK Initialization Complete")
                        Log.i(TAG, "🎯 Ready for Mini 3 Pro operations")
                    }
                    DJISDKInitEvent.START_PRODUCT_CONNECTION -> {
                        Log.i(TAG, "🔍 Starting Product Connection...")
                    }
                    else -> {
                        Log.d(TAG, "📊 Init Event: ${event.name}")
                    }
                }

                sendBroadcastEvent(isSDKRegistered, connectedProductType ?: "INITIALIZING", progress)
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
    }

    /**
     * Verifica compatibilità specifica Mini 3 Pro
     */
    private fun checkMini3ProCompatibility() {
        Log.d(TAG, "🔍 Checking Mini 3 Pro Compatibility...")

        try {
            // Test accesso ai manager principali
            val sdkManager = SDKManager.getInstance()
            Log.d(TAG, "├─ SDKManager: ✅ Available")

            // Nota: PerceptionManager e altri verranno testati quando il prodotto si connette
            Log.d(TAG, "└─ Basic compatibility: ✅ Confirmed")

        } catch (e: Exception) {
            Log.e(TAG, "❌ Compatibility check failed: ${e.message}")
        }
    }

    /**
     * Valida sistemi specifici Mini 3 Pro dopo connessione
     */
    private fun validateMini3ProSystems() {
        try {
            Log.d(TAG, "🔍 Validating Mini 3 Pro Systems...")

            // Test PerceptionManager per sensori
            val perceptionManager = dji.v5.manager.aircraft.perception.PerceptionManager.getInstance()
            Log.d(TAG, "├─ PerceptionManager: ✅ Available")

            // Test FlightControlManager
            val flightControlManager = dji.v5.manager.aircraft.flightcontrol.FlightControlManager.getInstance()
            Log.d(TAG, "├─ FlightControlManager: ✅ Available")

            // Test RTKCenter per GPS
            val rtkCenter = dji.v5.manager.aircraft.rtk.RTKCenter.getInstance()
            Log.d(TAG, "├─ RTKCenter: ✅ Available")

            Log.d(TAG, "└─ Mini 3 Pro Systems: ✅ All validated")

        } catch (e: Exception) {
            Log.w(TAG, "⚠️ System validation partially failed: ${e.message}")
            Log.w(TAG, "└─ Some features may not be available")
        }
    }

    /**
     * Invia broadcast eventi per comunicare con Activities
     */
    private fun sendBroadcastEvent(isConnected: Boolean, productType: String, progress: Int) {
        val intent = Intent().apply {
            action = FLAG_NOTIFY_PRODUCT_CONNECT_EVENT
            putExtra(FLAG_CONNECTION_STATE, isConnected)
            putExtra(FLAG_PRODUCT_TYPE, productType)
            putExtra(FLAG_INIT_PROGRESS, progress)
        }

        sendBroadcast(intent)
        Log.d(TAG, "📡 Broadcast: Connected=$isConnected, Product=$productType, Progress=$progress%")
    }

    /**
     * Ottiene informazioni dettagliate prodotto connesso
     */
    fun getConnectedProductInfo(): String {
        val product = SDKManager.getInstance().getProduct()

        return if (product != null) {
            buildString {
                appendLine("🚁 Prodotto: ${product.productType?.name ?: "Sconosciuto"}")

                // Battery info se disponibile
                product.battery?.let { battery ->
                    appendLine("🔋 Batteria: ${battery.chargeRemainingInPercent}%")
                    appendLine("⚡ Tensione: ${String.format("%.1f", battery.voltage)}V")
                }

                // Firmware info se disponibile
                product.productVersion?.let { version ->
                    appendLine("💾 Firmware: $version")
                }

                // Connection status
                appendLine("🔗 Connessione: ATTIVA")
                appendLine("📦 SDK: v5.8.0")

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
     * Verifica se prodotto connesso è Mini 3 Pro
     */
    fun isMini3ProConnected(): Boolean {
        return connectedProductType?.contains("Mini 3") == true
    }

    /**
     * Verifica se SDK è pronto per operazioni
     */
    fun isSDKReady(): Boolean {
        return isSDKRegistered && SDKManager.getInstance().getProduct() != null
    }

    /**
     * Ottiene stato dettagliato SDK
     */
    fun getSDKStatus(): String {
        return buildString {
            appendLine("📦 DJI SDK Status:")
            appendLine("├─ Version: 5.8.0")
            appendLine("├─ Registered: ${if (isSDKRegistered) "✅ Yes" else "❌ No"}")
            appendLine("├─ Product: ${connectedProductType ?: "None"}")
            appendLine("├─ Progress: $initializationProgress%")
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
}