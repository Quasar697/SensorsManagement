package com.example.msdksample

import android.util.Log
import kotlinx.coroutines.*

// ✅ SOLO import che esistono realmente in MSDK v5.11.0
import dji.v5.manager.SDKManager
import dji.v5.manager.aircraft.perception.PerceptionManager
import dji.v5.manager.aircraft.rtk.RTKCenter
import dji.v5.common.callback.CommonCallbacks
import dji.v5.common.error.IDJIError

/**
 * SensorManager per DJI Mini 3 Pro
 * Gestisce lettura e monitoraggio sensori con MSDK v5.11.0
 *
 * SENSORI SUPPORTATI MINI 3 PRO:
 * ✅ Vision System (Front/Back/Down)
 * ✅ APAS 4.0 Obstacle Avoidance
 * ✅ Vision Positioning
 * ✅ RTK/GPS System
 * ✅ Battery Status
 * ❌ Precision Landing (non supportato)
 * ❌ Side sensors (non disponibili)
 */
class SensorManager {

    companion object {
        private const val TAG = "SensorManager_Mini3Pro"
    }

    private var sensorJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    // Callback interface per ricevere dati sensori
    interface SensorDataCallback {
        fun onVisionSystemUpdated(
            isVisionEnabled: Boolean,
            isObstacleAvoidanceEnabled: Boolean,
            status: String
        )

        fun onObstacleDataUpdated(
            hasObstacleData: Boolean,
            obstacleInfo: String
        )

        fun onGPSDataUpdated(
            isGPSAvailable: Boolean,
            satelliteCount: Int,
            positioningSolution: String,
            status: String
        )

        fun onBatteryDataUpdated(
            percentage: Int,
            voltage: Double,
            isCharging: Boolean,
            status: String
        )

        fun onFlightDataUpdated(
            isAvailable: Boolean,
            info: String
        )

        fun onSensorError(sensorType: String, error: String)
    }

    private var callback: SensorDataCallback? = null

    fun setCallback(callback: SensorDataCallback) {
        this.callback = callback
    }

    /**
     * Avvia il monitoraggio periodico di tutti i sensori
     */
    fun startSensorReading(intervalMs: Long = 1000) {
        stopSensorReading() // Ferma eventuali letture precedenti

        sensorJob = scope.launch {
            Log.i(TAG, "🚀 Avvio monitoraggio sensori Mini 3 Pro (intervallo: ${intervalMs}ms)")

            while (isActive) {
                try {
                    if (isDroneConnected()) {
                        readAllSensors()
                    } else {
                        notifyDisconnectedState()
                    }
                    delay(intervalMs)
                } catch (e: Exception) {
                    Log.e(TAG, "❌ Errore nel ciclo di lettura sensori: ${e.message}")
                    callback?.onSensorError("Sistema", e.message ?: "Errore sconosciuto")
                    delay(intervalMs * 2) // Ritardo maggiore in caso di errore
                }
            }
        }

        Log.d(TAG, "✅ Monitoraggio sensori avviato")
    }

    /**
     * Ferma il monitoraggio dei sensori
     */
    fun stopSensorReading() {
        sensorJob?.cancel()
        sensorJob = null
        Log.d(TAG, "⏹️ Monitoraggio sensori fermato")
    }

    /**
     * Verifica se il drone è connesso
     */
    private fun isDroneConnected(): Boolean {
        return SDKManager.getInstance().getProduct() != null
    }

    /**
     * Legge tutti i sensori disponibili
     */
    private suspend fun readAllSensors() = withContext(Dispatchers.IO) {
        try {
            // Lettura parallela di tutti i sensori
            val visionJob = async { readVisionSensors() }
            val obstacleJob = async { readObstacleSensors() }
            val gpsJob = async { readGPSSensors() }
            val batteryJob = async { readBatterySensor() }
            val flightJob = async { readFlightData() }

            // Attende il completamento di tutti
            awaitAll(visionJob, obstacleJob, gpsJob, batteryJob, flightJob)

        } catch (e: Exception) {
            Log.e(TAG, "❌ Errore lettura sensori parallela: ${e.message}")
        }
    }

    /**
     * ✅ Legge i sensori del sistema Vision (APAS 4.0 + Vision Positioning)
     */
    private fun readVisionSensors() {
        try {
            val perceptionManager = PerceptionManager.getInstance()

            val isVisionEnabled = perceptionManager.isVisionPositioningSensorEnabled()
            val isObstacleAvoidanceEnabled = perceptionManager.isObstacleAvoidanceEnabled()

            val status = buildString {
                append("Vision System Status:\n")
                append("• Vision Positioning: ${if (isVisionEnabled) "✅ Attivo" else "❌ Disattivo"}\n")
                append("• APAS 4.0: ${if (isObstacleAvoidanceEnabled) "✅ Attivo" else "❌ Disattivo"}\n")
                append("• Sensori disponibili: Front, Back, Down\n")
                append("• Sensori laterali: Non disponibili (limitazione Mini 3 Pro)")
            }

            callback?.onVisionSystemUpdated(isVisionEnabled, isObstacleAvoidanceEnabled, status)

            Log.d(TAG, """
                === VISION SENSORS ===
                Vision Positioning: $isVisionEnabled
                APAS 4.0: $isObstacleAvoidanceEnabled
                Configurazione: Mini 3 Pro (3-direzioni)
            """.trimIndent())

        } catch (e: Exception) {
            Log.e(TAG, "❌ Errore lettura Vision Sensors: ${e.message}")
            callback?.onSensorError("Vision System", e.message ?: "Errore sconosciuto")
        }
    }

    /**
     * ✅ Legge i dati dei sensori ostacoli
     */
    private fun readObstacleSensors() {
        try {
            val perceptionManager = PerceptionManager.getInstance()
            val obstacleData = perceptionManager.obstacleData

            if (obstacleData != null) {
                val obstacleInfo = buildString {
                    append("Dati ostacoli disponibili ✅\n")
                    append("• Tipo dati: ${obstacleData::class.simpleName}\n")
                    append("• Sensori attivi: Front, Back, Down\n")
                    append("• Analisi dettagliata: In sviluppo\n")
                    append("• Mini 3 Pro: Nessun sensore laterale")
                }

                callback?.onObstacleDataUpdated(true, obstacleInfo)

                Log.d(TAG, """
                    === OBSTACLE SENSORS ===
                    Dati disponibili: Sì
                    Classe dati: ${obstacleData::class.simpleName}
                    Status: Funzionante
                """.trimIndent())

            } else {
                callback?.onObstacleDataUpdated(false, "Nessun dato ostacoli disponibile")
                Log.w(TAG, "⚠️ Nessun dato ostacoli ricevuto")
            }

        } catch (e: Exception) {
            Log.e(TAG, "❌ Errore lettura Obstacle Sensors: ${e.message}")
            callback?.onSensorError("Obstacle Sensors", e.message ?: "Errore sconosciuto")
        }
    }

    /**
     * ✅ Legge i dati GPS/RTK
     */
    private fun readGPSSensors() {
        try {
            val rtkCenter = RTKCenter.getInstance()
            val rtkState = rtkCenter.rtkSystemState

            if (rtkState != null) {
                val satelliteCount = rtkState.satelliteCount
                val positioningSolution = rtkState.positioningSolution?.toString() ?: "Sconosciuto"

                val status = buildString {
                    append("Sistema RTK/GPS:\n")
                    append("• Satelliti: $satelliteCount")
                    when {
                        satelliteCount >= 8 -> append(" 🟢 Eccellente")
                        satelliteCount >= 6 -> append(" 🟡 Buono")
                        satelliteCount >= 4 -> append(" 🟠 Sufficiente")
                        else -> append(" 🔴 Insufficiente")
                    }
                    append("\n• Soluzione positioning: $positioningSolution")
                    append("\n• Precisione: ${if (satelliteCount >= 6) "Alta" else "Bassa"}")
                    append("\n• Mini 3 Pro: GPS/GLONASS dual-system")
                }

                callback?.onGPSDataUpdated(true, satelliteCount, positioningSolution, status)

                Log.d(TAG, """
                    === GPS/RTK SYSTEM ===
                    Satelliti: $satelliteCount
                    Positioning: $positioningSolution
                    Qualità: ${if (satelliteCount >= 6) "Buona" else "Scarsa"}
                """.trimIndent())

            } else {
                callback?.onGPSDataUpdated(false, 0, "Non disponibile", "Sistema GPS/RTK non disponibile")
                Log.w(TAG, "⚠️ Sistema RTK/GPS non disponibile")
            }

        } catch (e: Exception) {
            Log.e(TAG, "❌ Errore lettura GPS: ${e.message}")
            callback?.onSensorError("GPS/RTK", e.message ?: "Errore sconosciuto")
        }
    }

    /**
     * ✅ Legge i dati della batteria
     */
    private fun readBatterySensor() {
        try {
            val product = SDKManager.getInstance().getProduct()
            val battery = product?.battery

            if (battery != null) {
                val percentage = battery.chargeRemainingInPercent
                val voltage = battery.voltage
                val isCharging = battery.isCharging

                val status = buildString {
                    append("Batteria Mini 3 Pro:\n")
                    append("• Carica: $percentage%\n")
                    append("• Tensione: ${String.format("%.1f", voltage)}V\n")
                    append("• Stato: ${if (isCharging) "⚡ In carica" else "🔋 In uso"}\n")

                    // Indicazioni stato
                    when {
                        percentage <= 10 -> append("• Livello: 🚨 CRITICO - Atterraggio immediato!")
                        percentage <= 20 -> append("• Livello: ⚠️ BASSO - Pianifica rientro")
                        percentage <= 30 -> append("• Livello: 🟡 MEDIO - Monitora")
                        else -> append("• Livello: ✅ BUONO")
                    }

                    append("\n• Capacità: 2453mAh (Mini 3 Pro)")
                }

                callback?.onBatteryDataUpdated(percentage, voltage, isCharging, status)

                Log.d(TAG, """
                    === BATTERY STATUS ===
                    Percentuale: $percentage%
                    Tensione: ${String.format("%.2f", voltage)}V
                    In carica: $isCharging
                    Stato: ${when {
                    percentage <= 15 -> "CRITICO"
                    percentage <= 30 -> "BASSO"
                    else -> "BUONO"
                }}
                """.trimIndent())

            } else {
                callback?.onBatteryDataUpdated(0, 0.0, false, "Dati batteria non disponibili")
                Log.w(TAG, "⚠️ Dati batteria non disponibili")
            }

        } catch (e: Exception) {
            Log.e(TAG, "❌ Errore lettura batteria: ${e.message}")
            callback?.onSensorError("Battery", e.message ?: "Errore sconosciuto")
        }
    }

    /**
     * Placeholder per dati di volo (API da identificare)
     */
    private fun readFlightData() {
        try {
            val product = SDKManager.getInstance().getProduct()

            if (product != null) {
                val info = buildString {
                    append("Dati di volo:\n")
                    append("• Altitudine: API da implementare\n")
                    append("• Velocità: API da implementare\n")
                    append("• Attitude: API da implementare\n")
                    append("• Stato: Drone connesso\n")
                    append("• Modello: ${product.productType?.name ?: "DJI Mini 3 Pro"}")
                }

                callback?.onFlightDataUpdated(true, info)

                Log.d(TAG, """
                    === FLIGHT DATA ===
                    Drone: ${product.productType?.name ?: "Mini 3 Pro"}
                    Status: Connesso
                    API volo: Da implementare
                """.trimIndent())

            } else {
                callback?.onFlightDataUpdated(false, "Drone disconnesso")
            }

        } catch (e: Exception) {
            Log.e(TAG, "❌ Errore lettura dati volo: ${e.message}")
            callback?.onSensorError("Flight Data", e.message ?: "Errore sconosciuto")
        }
    }

    /**
     * Notifica stato disconnesso a tutti i callback
     */
    private fun notifyDisconnectedState() {
        callback?.onVisionSystemUpdated(false, false, "Drone disconnesso - Vision System non disponibile")
        callback?.onObstacleDataUpdated(false, "Drone disconnesso - Sensori ostacoli non disponibili")
        callback?.onGPSDataUpdated(false, 0, "Non disponibile", "Drone disconnesso - GPS non disponibile")
        callback?.onBatteryDataUpdated(0, 0.0, false, "Drone disconnesso - Batteria non disponibile")
        callback?.onFlightDataUpdated(false, "Drone disconnesso - Dati volo non disponibili")
    }

    /**
     * ✅ Controllo APAS 4.0 - Abilita sistema anticollisione
     */
    fun enableObstacleAvoidance(callback: (Boolean, String) -> Unit) {
        try {
            val perceptionManager = PerceptionManager.getInstance()

            perceptionManager.setObstacleAvoidanceEnabled(true, object : CommonCallbacks.CompletionCallback {
                override fun onSuccess() {
                    Log.i(TAG, "✅ APAS 4.0 abilitato con successo")
                    callback(true, "APAS 4.0 abilitato con successo")
                }

                override fun onFailure(error: IDJIError) {
                    Log.e(TAG, "❌ Errore abilitazione APAS 4.0: ${error.description()}")
                    callback(false, "Errore APAS 4.0: ${error.description()}")
                }
            })

        } catch (e: Exception) {
            Log.e(TAG, "❌ Eccezione abilitazione APAS: ${e.message}")
            callback(false, "Eccezione: ${e.message}")
        }
    }

    /**
     * ✅ Controllo APAS 4.0 - Disabilita sistema anticollisione
     */
    fun disableObstacleAvoidance(callback: (Boolean, String) -> Unit) {
        try {
            val perceptionManager = PerceptionManager.getInstance()

            perceptionManager.setObstacleAvoidanceEnabled(false, object : CommonCallbacks.CompletionCallback {
                override fun onSuccess() {
                    Log.i(TAG, "⚠️ APAS 4.0 disabilitato - ATTENZIONE: Volo manuale!")
                    callback(true, "APAS 4.0 disabilitato - Volo manuale attivo")
                }

                override fun onFailure(error: IDJIError) {
                    Log.e(TAG, "❌ Errore disabilitazione APAS 4.0: ${error.description()}")
                    callback(false, "Errore disabilitazione APAS: ${error.description()}")
                }
            })

        } catch (e: Exception) {
            Log.e(TAG, "❌ Eccezione disabilitazione APAS: ${e.message}")
            callback(false, "Eccezione: ${e.message}")
        }
    }

    /**
     * ✅ Controllo Vision Positioning
     */
    fun toggleVisionPositioning(callback: (Boolean, String) -> Unit) {
        try {
            val perceptionManager = PerceptionManager.getInstance()
            val currentState = perceptionManager.isVisionPositioningSensorEnabled()

            perceptionManager.setVisionPositioningEnabled(!currentState, object : CommonCallbacks.CompletionCallback {
                override fun onSuccess() {
                    val newState = if (!currentState) "attivato" else "disattivato"
                    Log.i(TAG, "✅ Vision Positioning $newState")
                    callback(true, "Vision Positioning $newState con successo")
                }

                override fun onFailure(error: IDJIError) {
                    Log.e(TAG, "❌ Errore Vision Positioning: ${error.description()}")
                    callback(false, "Errore Vision Positioning: ${error.description()}")
                }
            })

        } catch (e: Exception) {
            Log.e(TAG, "❌ Eccezione Vision Positioning: ${e.message}")
            callback(false, "Eccezione: ${e.message}")
        }
    }

    /**
     * Ottiene lo stato corrente di tutti i sensori
     */
    fun getCurrentSensorStatus(): String {
        return try {
            val product = SDKManager.getInstance().getProduct()

            if (product == null) {
                "❌ DRONE NON CONNESSO"
            } else {
                buildString {
                    appendLine("🚁 DJI Mini 3 Pro - Stato Sensori")
                    appendLine("═══════════════════════════════════")

                    // Vision System
                    try {
                        val perceptionManager = PerceptionManager.getInstance()
                        val visionEnabled = perceptionManager.isVisionPositioningSensorEnabled()
                        val apasEnabled = perceptionManager.isObstacleAvoidanceEnabled()

                        appendLine("👁️ Vision System:")
                        appendLine("  • Vision Positioning: ${if (visionEnabled) "✅ Attivo" else "❌ Disattivo"}")
                        appendLine("  • APAS 4.0: ${if (apasEnabled) "✅ Attivo" else "❌ Disattivo"}")
                        appendLine("  • Sensori: Front ✅ | Back ✅ | Down ✅ | Side ❌")
                    } catch (e: Exception) {
                        appendLine("👁️ Vision System: ❌ Errore - ${e.message}")
                    }

                    appendLine()

                    // Battery
                    try {
                        val battery = product.battery
                        if (battery != null) {
                            appendLine("🔋 Batteria:")
                            appendLine("  • Carica: ${battery.chargeRemainingInPercent}%")
                            appendLine("  • Tensione: ${String.format("%.1f", battery.voltage)}V")
                            appendLine("  • Stato: ${if (battery.isCharging) "⚡ In carica" else "🔋 In uso"}")
                        } else {
                            appendLine("🔋 Batteria: ❌ Dati non disponibili")
                        }
                    } catch (e: Exception) {
                        appendLine("🔋 Batteria: ❌ Errore - ${e.message}")
                    }

                    appendLine()

                    // GPS
                    try {
                        val rtkCenter = RTKCenter.getInstance()
                        val rtkState = rtkCenter.rtkSystemState

                        if (rtkState != null) {
                            appendLine("🛰️ GPS/RTK:")
                            appendLine("  • Satelliti: ${rtkState.satelliteCount}")
                            appendLine("  • Soluzione: ${rtkState.positioningSolution}")
                            appendLine("  • Qualità: ${if (rtkState.satelliteCount >= 6) "🟢 Buona" else "🟡 Scarsa"}")
                        } else {
                            appendLine("🛰️ GPS/RTK: ❌ Sistema non disponibile")
                        }
                    } catch (e: Exception) {
                        appendLine("🛰️ GPS/RTK: ❌ Errore - ${e.message}")
                    }

                    appendLine()
                    appendLine("📱 App: Mini 3 Pro Controller v1.0")
                    appendLine("📦 SDK: DJI MSDK v5.11.0")
                }
            }
        } catch (e: Exception) {
            "❌ ERRORE STATO SENSORI: ${e.message}"
        }
    }

    /**
     * Cleanup delle risorse
     */
    fun cleanup() {
        stopSensorReading()
        scope.cancel()
        callback = null
        Log.i(TAG, "🧹 SensorManager cleanup completato")
    }
}