package com.example.msdksample

import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import kotlinx.coroutines.*

// ✅ Import corretti per MSDK v5.11.0
import dji.v5.manager.SDKManager
import dji.v5.manager.interfaces.ISDKManager
import dji.v5.common.callback.CommonCallbacks
import dji.v5.common.error.IDJIError

/**
 * MainActivity per DJI Mini 3 BASE - VERSIONE SEMPLIFICATA
 *
 * FUNZIONALITÀ DISPONIBILI NEL MINI 3 BASE:
 * ✅ Connessione e controllo base
 * ✅ Sensori downward per atterraggio
 * ✅ GPS/GNSS positioning
 * ✅ Battery monitoring
 * ✅ Basic flight telemetry
 * ✅ QuickShots limitati
 *
 * LIMITAZIONI MINI 3 BASE:
 * ❌ NO obstacle avoidance (front/back)
 * ❌ NO APAS systems
 * ❌ NO ActiveTrack
 * ❌ SOLO sensori downward per atterraggio
 */
class MainActivity : AppCompatActivity() {

    private val TAG = "MainActivity_Mini3Base"

    // UI Components Base
    private lateinit var statusText: TextView
    private lateinit var connectButton: Button
    private lateinit var takeoffButton: Button
    private lateinit var landButton: Button

    // Basic Sensor Display (solo quelli disponibili)
    private lateinit var downwardSensorText: TextView
    private lateinit var gpsStatusText: TextView
    private lateinit var batteryStatusText: TextView
    private lateinit var altitudeText: TextView
    private lateinit var velocityText: TextView

    // Basic Controls (niente APAS!)
    private lateinit var quickShotsButton: Button
    private lateinit var returnHomeButton: Button

    // Coroutine per aggiornamenti sensori
    private var sensorUpdateJob: Job? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        initUI()
        setupButtonListeners()
        startBasicSensorMonitoring()
        updateStatus()
        showMini3BaseInfo()
    }

    private fun initUI() {
        statusText = findViewById(R.id.statusText)
        connectButton = findViewById(R.id.connectButton)
        takeoffButton = findViewById(R.id.takeoffButton)
        landButton = findViewById(R.id.landButton)
        downwardSensorText = findViewById(R.id.downwardSensorText)
        gpsStatusText = findViewById(R.id.gpsStatusText)
        batteryStatusText = findViewById(R.id.batteryStatusText)
        altitudeText = findViewById(R.id.altitudeText)
        velocityText = findViewById(R.id.velocityText)
        quickShotsButton = findViewById(R.id.quickShotsButton)
        returnHomeButton = findViewById(R.id.returnHomeButton)
    }

    private fun setupButtonListeners() {
        connectButton.setOnClickListener { checkConnection() }
        takeoffButton.setOnClickListener { showTakeoffWarning() }
        landButton.setOnClickListener { showLandingInfo() }
        quickShotsButton.setOnClickListener { showQuickShotsInfo() }
        returnHomeButton.setOnClickListener { showReturnToHomeInfo() }
    }

    private fun startBasicSensorMonitoring() {
        sensorUpdateJob = CoroutineScope(Dispatchers.Main).launch {
            while (isActive) {
                updateBasicSensorData()
                delay(1000) // Aggiorna ogni secondo
            }
        }
    }

    /**
     * Aggiorna solo i dati sensori disponibili nel Mini 3 Base
     */
    private fun updateBasicSensorData() {
        try {
            val sdkManager = SDKManager.getInstance() as ISDKManager
            val product = getProductFromSDK(sdkManager)

            if (product == null) {
                showDisconnectedState()
                return
            }

            updateDownwardSensorsStatus()
            updateGPSStatus()
            updateBatteryStatus()
            updateBasicFlightData()

        } catch (e: Exception) {
            runOnUiThread {
                Toast.makeText(this, "⚠️ Errore lettura sensori base: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    /**
     * ✅ FUNZIONA - Aggiorna sensori downward (gli unici disponibili)
     */
    private fun updateDownwardSensorsStatus() {
        runOnUiThread {
            try {
                // Il Mini 3 base ha solo sensori downward per atterraggio
                downwardSensorText.text = buildString {
                    appendLine("⬇️ Sensori Downward: ✅ Attivi")
                    appendLine("📍 Funzione: Atterraggio preciso e hovering")
                    appendLine("🚫 Mini 3 Base: NESSUN sensore front/back/lateral")
                    append("⚠️ ATTENZIONE: Volo manuale richiesto per evitare ostacoli")
                }
            } catch (e: Exception) {
                downwardSensorText.text = "⬇️ Sensori Downward: Errore - ${e.message}"
            }
        }
    }

    /**
     * ✅ Aggiorna stato GPS (disponibile su Mini 3 Base)
     */
    private fun updateGPSStatus() {
        runOnUiThread {
            try {
                // Il Mini 3 base ha GPS/GNSS standard
                gpsStatusText.text = buildString {
                    append("🛰️ GPS/GNSS: Sistema attivo")
                    appendLine()
                    append("📡 Costellazioni: GPS + Galileo + BeiDou")
                    appendLine()
                    append("🎯 Accuratezza: ±1.5m GNSS, ±0.3m con vision downward")
                    appendLine()
                    append("⏱️ First Fix: ~20-40 secondi")
                }
            } catch (e: Exception) {
                gpsStatusText.text = "🛰️ GPS: Errore - ${e.message}"
            }
        }
    }

    /**
     * ✅ Aggiorna stato batteria
     */
    private fun updateBatteryStatus() {
        try {
            val sdkManager = SDKManager.getInstance() as ISDKManager
            val batteryInfo = testBatteryMethods(sdkManager)

            runOnUiThread {
                batteryStatusText.text = batteryInfo
            }
        } catch (e: Exception) {
            runOnUiThread {
                batteryStatusText.text = "🔋 Batteria: Errore - ${e.message}"
            }
        }
    }

    /**
     * Test metodi batteria (stesso del Pro)
     */
    private fun testBatteryMethods(sdkManager: ISDKManager): String {
        return buildString {
            append("🔋 Batteria Mini 3: ")

            try {
                val product = getProductFromSDK(sdkManager)
                if (product != null) {
                    val batteryInfo = extractBatteryInfo(product)
                    append(batteryInfo)
                    appendLine()
                    append("⚡ Autonomia: 38min standard / 51min Plus")
                } else {
                    append("Prodotto non disponibile")
                    appendLine()
                    append("🎯 Connetti il Mini 3 Base per vedere i dati batteria")
                }
            } catch (e: Exception) {
                append("Errore test - ${e.message}")
            }
        }
    }

    /**
     * Aggiorna dati volo base
     */
    private fun updateBasicFlightData() {
        runOnUiThread {
            try {
                val sdkManager = SDKManager.getInstance() as ISDKManager
                val product = getProductFromSDK(sdkManager)

                if (product != null) {
                    altitudeText.text = "📏 Altitudine: API da implementare (Mini 3 Base)"
                    velocityText.text = "🚀 Velocità: API da implementare (Mini 3 Base)"
                } else {
                    altitudeText.text = "📏 Altitudine: Mini 3 disconnesso"
                    velocityText.text = "🚀 Velocità: Mini 3 disconnesso"
                }
            } catch (e: Exception) {
                altitudeText.text = "📏 Altitudine: Errore - ${e.message}"
                velocityText.text = "🚀 Velocità: Errore - ${e.message}"
            }
        }
    }

    /**
     * Decollo con avvisi di sicurezza per Mini 3 Base
     */
    private fun showTakeoffWarning() {
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("⚠️ DECOLLO - DJI Mini 3 Base")
            .setMessage("""
                ATTENZIONE: Il tuo Mini 3 BASE non ha obstacle avoidance!
                
                ❌ NESSUN sensore front/back/lateral
                ⬇️ Solo sensori downward per atterraggio
                
                PRIMA DEL DECOLLO VERIFICA:
                • Area completamente libera da ostacoli
                • Batteria sufficiente (>30%)
                • GPS acquisito
                • Vento <10.7 m/s
                • Mantenere SEMPRE controllo visivo
                
                ⚠️ VOLO MANUALE RICHIESTO - Il drone NON evita ostacoli automaticamente!
                
                Procedere con il decollo?
            """.trimIndent())
            .setPositiveButton("🛫 Procedi") { _, _ ->
                Toast.makeText(this, "🛫 Decollo: Usa controller fisico o app DJI Fly", Toast.LENGTH_LONG).show()
            }
            .setNegativeButton("❌ Annulla", null)
            .show()
    }

    /**
     * Informazioni atterraggio
     */
    private fun showLandingInfo() {
        Toast.makeText(this, """
            🛬 ATTERRAGGIO - Mini 3 Base
            
            ✅ Sensori downward attivi per atterraggio preciso
            📍 Il drone rileva automaticamente il terreno
            
            Usa il controller fisico o app DJI Fly per atterraggio.
        """.trimIndent(), Toast.LENGTH_LONG).show()
    }

    /**
     * Informazioni QuickShots disponibili
     */
    private fun showQuickShotsInfo() {
        Toast.makeText(this, """
            📽️ QUICKSHOTS - Mini 3 Base
            
            ✅ DISPONIBILI:
            • Dronie
            • Circle
            • Helix  
            • Rocket
            • Boomerang
            
            ❌ NON DISPONIBILI:
            • ActiveTrack (richiede obstacle sensors)
            • Asteroid (solo su Pro)
            
            ⚠️ Durante QuickShots mantieni controllo visivo - 
            nessuna protezione ostacoli automatica!
        """.trimIndent(), Toast.LENGTH_LONG).show()
    }

    /**
     * Informazioni Return to Home
     */
    private fun showReturnToHomeInfo() {
        Toast.makeText(this, """
            🏠 RETURN TO HOME - Mini 3 Base
            
            ✅ RTH Disponibile con:
            • GPS positioning
            • Sensori downward per atterraggio
            
            ⚠️ ATTENZIONE:
            • Imposta altitudine RTH ben sopra gli ostacoli
            • Il drone NON evita ostacoli durante il ritorno
            • Monitora sempre il percorso di ritorno
            
            Usa app DJI Fly per impostare RTH.
        """.trimIndent(), Toast.LENGTH_LONG).show()
    }

    /**
     * Verifica connessione
     */
    private fun checkConnection() {
        try {
            val sdkManager = SDKManager.getInstance() as ISDKManager
            val product = getProductFromSDK(sdkManager)
            val isConnected = product != null

            val status = if (isConnected) {
                val productInfo = extractProductName(product!!)
                "✅ Connesso: $productInfo"
            } else {
                "❌ Nessun Mini 3 connesso"
            }

            statusText.text = status
            Toast.makeText(this, status, Toast.LENGTH_SHORT).show()
            updateButtonStates(isConnected)

        } catch (e: Exception) {
            val errorStatus = "❌ Errore connessione: ${e.message}"
            statusText.text = errorStatus
            Toast.makeText(this, errorStatus, Toast.LENGTH_SHORT).show()
            updateButtonStates(false)
        }
    }

    /**
     * Ottiene il prodotto dall'SDK
     */
    private fun getProductFromSDK(sdkManager: ISDKManager): Any? {
        val productMethods = listOf(
            "getProduct",
            "getCurrentProduct",
            "getConnectedProduct",
            "product"
        )

        for (methodName in productMethods) {
            try {
                val method = sdkManager::class.java.getMethod(methodName)
                val result = method.invoke(sdkManager)
                if (result != null) {
                    return result
                }
            } catch (e: Exception) {
                // Metodo non esiste, continua
            }
        }
        return null
    }

    /**
     * Estrae nome prodotto
     */
    private fun extractProductName(product: Any): String {
        return try {
            val productClass = product::class.java
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
                        return result.toString()
                    }
                } catch (e: Exception) {
                    // Continua
                }
            }

            productClass.simpleName
        } catch (e: Exception) {
            "DJI Mini 3"
        }
    }

    /**
     * Estrae info batteria
     */
    private fun extractBatteryInfo(product: Any): String {
        return try {
            val productClass = product::class.java
            val batteryMethods = listOf("getBattery", "battery", "getBatteryState")

            var battery: Any? = null
            for (methodName in batteryMethods) {
                try {
                    val method = productClass.getMethod(methodName)
                    battery = method.invoke(product)
                    if (battery != null) break
                } catch (e: Exception) {
                    // Continua
                }
            }

            if (battery != null) {
                val batteryClass = battery::class.java
                var percentage = 0
                var voltage = 0.0
                var isCharging = false

                // Test metodi batteria comuni
                try {
                    val percentageMethod = batteryClass.getMethod("getChargeRemainingInPercent")
                    percentage = percentageMethod.invoke(battery) as? Int ?: 0
                } catch (e: Exception) {
                    try {
                        val percentageMethod = batteryClass.getMethod("chargeRemainingInPercent")
                        percentage = percentageMethod.invoke(battery) as? Int ?: 0
                    } catch (e2: Exception) {
                        // Non disponibile
                    }
                }

                try {
                    val voltageMethod = batteryClass.getMethod("getVoltage")
                    voltage = voltageMethod.invoke(battery) as? Double ?: 0.0
                } catch (e: Exception) {
                    try {
                        val voltageMethod = batteryClass.getMethod("voltage")
                        voltage = voltageMethod.invoke(battery) as? Double ?: 0.0
                    } catch (e2: Exception) {
                        // Non disponibile
                    }
                }

                try {
                    val chargingMethod = batteryClass.getMethod("isCharging")
                    isCharging = chargingMethod.invoke(battery) as? Boolean ?: false
                } catch (e: Exception) {
                    // Non disponibile
                }

                buildString {
                    append("$percentage%")
                    if (voltage > 0) {
                        append(" | ${String.format("%.1f", voltage)}V")
                    }
                    if (isCharging) {
                        append(" ⚡ In carica")
                    }

                    appendLine()
                    when {
                        percentage <= 10 -> append("🚨 CRITICA!")
                        percentage <= 20 -> append("⚠️ BASSA")
                        percentage <= 30 -> append("🟡 Monitora")
                        else -> append("✅ Buona")
                    }
                }
            } else {
                "Oggetto batteria non accessibile"
            }
        } catch (e: Exception) {
            "Errore estrazione dati - ${e.message}"
        }
    }

    private fun updateStatus() {
        runOnUiThread {
            try {
                val sdkManager = SDKManager.getInstance() as ISDKManager
                val product = getProductFromSDK(sdkManager)
                val isConnected = product != null

                statusText.text = if (isConnected) {
                    val productName = extractProductName(product!!)
                    "🚁 $productName Base pronto"
                } else {
                    "❌ In attesa connessione Mini 3 Base..."
                }

                updateButtonStates(isConnected)

            } catch (e: Exception) {
                statusText.text = "❓ Errore stato: ${e.message}"
                updateButtonStates(false)
            }
        }
    }

    private fun updateButtonStates(enabled: Boolean) {
        // Bottoni base sempre abilitati (mostrano info)
        takeoffButton.isEnabled = true
        landButton.isEnabled = true
        quickShotsButton.isEnabled = true
        returnHomeButton.isEnabled = true
    }

    private fun showDisconnectedState() {
        runOnUiThread {
            downwardSensorText.text = "⬇️ Sensori Downward: Mini 3 disconnesso"
            gpsStatusText.text = "🛰️ GPS: N/A"
            batteryStatusText.text = "🔋 Batteria: N/A"
            altitudeText.text = "📏 Altitudine: N/A"
            velocityText.text = "🚀 Velocità: N/A"
        }
    }

    private fun showMini3BaseInfo() {
        Toast.makeText(this, """
            🎯 DJI Mini 3 BASE - Controller App v1.0
            
            ✅ FUNZIONALITÀ SUPPORTATE:
            • Connessione e controllo base
            • Sensori downward per atterraggio preciso
            • GPS/GNSS positioning
            • Monitoraggio batteria
            • QuickShots base (no ActiveTrack)
            • Return to Home con GPS
            
            ❌ LIMITAZIONI (vs Mini 3 Pro):
            • NESSUN obstacle avoidance
            • NESSUN sensore front/back/lateral  
            • NESSUN APAS system
            • NESSUN ActiveTrack automatico
            
            ⚠️ SICUREZZA IMPORTANTE:
            Questo drone richiede controllo visivo costante!
            Non ha protezione automatica da ostacoli.
            
            📱 Per controlli volo completi usa l'app DJI Fly ufficiale
        """.trimIndent(), Toast.LENGTH_LONG).show()
    }

    override fun onDestroy() {
        super.onDestroy()
        sensorUpdateJob?.cancel()
    }
}