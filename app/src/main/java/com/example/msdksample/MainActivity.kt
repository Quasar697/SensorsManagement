package com.example.msdksample

import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import kotlinx.coroutines.*

// DJI SDK v5 imports - VERSIONE STABILE 5.8.0
import dji.v5.manager.SDKManager
import dji.v5.manager.aircraft.perception.PerceptionManager
import dji.v5.manager.aircraft.flightcontrol.FlightControlManager
import dji.v5.manager.aircraft.rtk.RTKCenter
import dji.v5.common.callback.CommonCallbacks
import dji.v5.common.error.IDJIError

/**
 * MainActivity per DJI Mini 3 Pro con sistema sensori completo
 *
 * FUNZIONALITÀ SUPPORTATE DAL MINI 3 PRO:
 * ✅ Vision Sensors (Front/Back/Down)
 * ✅ APAS 4.0 Obstacle Avoidance
 * ✅ Vision Positioning (standard)
 * ✅ GPS/GNSS Positioning
 * ✅ Battery Monitoring
 * ✅ Flight Telemetry
 *
 * LIMITAZIONI MINI 3 PRO:
 * ❌ Precision Landing (non supportato)
 * ❌ Sensori laterali (solo front/back/down)
 */
class MainActivity : AppCompatActivity() {

    private val TAG = "MainActivity_Mini3Pro"

    // UI Components Base
    private lateinit var statusText: TextView
    private lateinit var connectButton: Button
    private lateinit var takeoffButton: Button
    private lateinit var landButton: Button

    // Sensor Status Display
    private lateinit var visionStatusText: TextView
    private lateinit var obstacleDistanceText: TextView
    private lateinit var gpsStatusText: TextView
    private lateinit var batteryStatusText: TextView
    private lateinit var altitudeText: TextView
    private lateinit var velocityText: TextView

    // Control Buttons per Mini 3 Pro
    private lateinit var enableObstacleAvoidanceButton: Button
    private lateinit var disableObstacleAvoidanceButton: Button
    private lateinit var visionPositioningButton: Button // Sostituisce Precision Landing

    // Coroutine per aggiornamenti sensori
    private var sensorUpdateJob: Job? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        initUI()
        setupButtonListeners()
        startSensorMonitoring()
        updateStatus()

        // Mostra info specifiche Mini 3 Pro
        showMini3ProCapabilities()
    }

    private fun initUI() {
        // UI Base
        statusText = findViewById(R.id.statusText)
        connectButton = findViewById(R.id.connectButton)
        takeoffButton = findViewById(R.id.takeoffButton)
        landButton = findViewById(R.id.landButton)

        // Sensor Display
        visionStatusText = findViewById(R.id.visionStatusText)
        obstacleDistanceText = findViewById(R.id.obstacleDistanceText)
        gpsStatusText = findViewById(R.id.gpsStatusText)
        batteryStatusText = findViewById(R.id.batteryStatusText)
        altitudeText = findViewById(R.id.altitudeText)
        velocityText = findViewById(R.id.velocityText)

        // Control Buttons
        enableObstacleAvoidanceButton = findViewById(R.id.enableObstacleAvoidanceButton)
        disableObstacleAvoidanceButton = findViewById(R.id.disableObstacleAvoidanceButton)
        visionPositioningButton = findViewById(R.id.visionPositioningButton)
    }

    private fun setupButtonListeners() {
        connectButton.setOnClickListener { checkConnection() }
        takeoffButton.setOnClickListener { takeoff() }
        landButton.setOnClickListener { land() }
        enableObstacleAvoidanceButton.setOnClickListener { enableAPAS() }
        disableObstacleAvoidanceButton.setOnClickListener { disableAPAS() }
        visionPositioningButton.setOnClickListener { toggleVisionPositioning() }
    }

    private fun startSensorMonitoring() {
        sensorUpdateJob = CoroutineScope(Dispatchers.Main).launch {
            while (isActive) {
                updateAllSensorData()
                delay(500) // Aggiorna ogni 500ms
            }
        }
    }

    private fun updateAllSensorData() {
        val product = SDKManager.getInstance().getProduct()
        if (product == null) {
            showDisconnectedState()
            return
        }

        try {
            updateVisionSensorsStatus()
            updateObstacleDistances()
            updateGPSStatus()
            updateBatteryStatus()
            updateFlightData()
        } catch (e: Exception) {
            runOnUiThread {
                Toast.makeText(this, "⚠️ Errore lettura sensori: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    /**
     * Aggiorna stato dei Vision Sensors del Mini 3 Pro
     * Front + Back + Down sensors (NO laterali)
     */
    private fun updateVisionSensorsStatus() {
        try {
            val perceptionManager = PerceptionManager.getInstance()

            val isVisionEnabled = perceptionManager.isVisionPositioningSensorEnabled()
            val isObstacleAvoidanceEnabled = perceptionManager.isObstacleAvoidanceEnabled()

            runOnUiThread {
                val status = buildString {
                    append("👁️ Vision System: ")
                    when {
                        isVisionEnabled && isObstacleAvoidanceEnabled -> append("✅ COMPLETO (Vision + APAS 4.0)")
                        isVisionEnabled -> append("✅ ATTIVO (solo positioning)")
                        else -> append("❌ DISATTIVO")
                    }
                    appendLine()
                    append("🎯 Sensori: Front ✅ | Back ✅ | Down ✅ | Side ❌")
                }
                visionStatusText.text = status
            }
        } catch (e: Exception) {
            runOnUiThread {
                visionStatusText.text = "❓ Vision System: Errore lettura"
            }
        }
    }

    /**
     * Legge distanze ostacoli dai sensori Mini 3 Pro
     * Solo Front/Back/Down disponibili
     */
    private fun updateObstacleDistances() {
        try {
            val perceptionManager = PerceptionManager.getInstance()
            val obstacleData = perceptionManager.obstacleData

            runOnUiThread {
                if (obstacleData != null) {
                    val status = buildString {
                        var hasObstacles = false

                        if (obstacleData.frontObstacleDistance > 0) {
                            append("🔴 FRONTALE: ${String.format("%.1f", obstacleData.frontObstacleDistance)}m ")
                            hasObstacles = true
                        }

                        if (obstacleData.backObstacleDistance > 0) {
                            append("🔴 POSTERIORE: ${String.format("%.1f", obstacleData.backObstacleDistance)}m ")
                            hasObstacles = true
                        }

                        if (obstacleData.downObstacleDistance > 0) {
                            append("⬇️ TERRENO: ${String.format("%.1f", obstacleData.downObstacleDistance)}m")
                            hasObstacles = true
                        }

                        if (!hasObstacles) {
                            append("✅ Nessun ostacolo rilevato")
                        }
                    }
                    obstacleDistanceText.text = status
                } else {
                    obstacleDistanceText.text = "📡 Sensori ostacoli: N/A"
                }
            }
        } catch (e: Exception) {
            runOnUiThread {
                obstacleDistanceText.text = "❓ Ostacoli: Errore lettura"
            }
        }
    }

    /**
     * Aggiorna stato GPS/GNSS con info satelliti
     */
    private fun updateGPSStatus() {
        try {
            val flightControlManager = FlightControlManager.getInstance()
            val rtkCenter = RTKCenter.getInstance()

            val locationData = flightControlManager.aircraftLocationData
            val rtkState = rtkCenter.rtkSystemState

            runOnUiThread {
                if (locationData != null && rtkState != null) {
                    val location = locationData.aircraftLocation
                    val accuracy = locationData.gpsSignalLevel
                    val satellites = rtkState.satelliteCount

                    val status = buildString {
                        appendLine("🛰️ GPS: $satellites satelliti | Precisione: $accuracy")
                        append("📍 ${String.format("%.6f", location.latitude)}, ${String.format("%.6f", location.longitude)}")

                        // Indicatore qualità GPS
                        when {
                            satellites >= 8 && accuracy >= 4 -> append(" 🟢")
                            satellites >= 6 && accuracy >= 3 -> append(" 🟡")
                            else -> append(" 🔴")
                        }
                    }
                    gpsStatusText.text = status
                } else {
                    gpsStatusText.text = "🛰️ GPS: Ricerca satelliti..."
                }
            }
        } catch (e: Exception) {
            runOnUiThread {
                gpsStatusText.text = "❓ GPS: Errore lettura"
            }
        }
    }

    /**
     * Monitora batteria con allerte intelligenti
     */
    private fun updateBatteryStatus() {
        try {
            val product = SDKManager.getInstance().getProduct()
            val battery = product?.battery

            runOnUiThread {
                if (battery != null) {
                    val percentage = battery.chargeRemainingInPercent
                    val voltage = battery.voltage
                    val isCharging = battery.isCharging

                    val status = buildString {
                        append("🔋 Batteria: $percentage% | ${String.format("%.1f", voltage)}V")

                        if (isCharging) {
                            append(" ⚡ In carica")
                        }

                        // Indicatori stato batteria
                        when {
                            percentage <= 10 -> append(" 🚨 CRITICA!")
                            percentage <= 20 -> append(" ⚠️ BASSA")
                            percentage <= 30 -> append(" 🟡 Pianifica rientro")
                            else -> append(" ✅ Buona")
                        }
                    }
                    batteryStatusText.text = status

                    // Allerta automatica batteria critica
                    if (percentage <= 15 && !isCharging) {
                        Toast.makeText(this@MainActivity, "🚨 BATTERIA CRITICA! Atterraggio consigliato!", Toast.LENGTH_LONG).show()
                    }
                } else {
                    batteryStatusText.text = "🔋 Batteria: N/A"
                }
            }
        } catch (e: Exception) {
            runOnUiThread {
                batteryStatusText.text = "❓ Batteria: Errore lettura"
            }
        }
    }

    /**
     * Dati di volo in tempo reale
     */
    private fun updateFlightData() {
        try {
            val flightControlManager = FlightControlManager.getInstance()
            val flightData = flightControlManager.flightControlData

            runOnUiThread {
                if (flightData != null) {
                    val altitude = flightData.altitude
                    val velocityX = flightData.velocityX
                    val velocityY = flightData.velocityY
                    val velocityZ = flightData.velocityZ

                    val horizontalSpeed = kotlin.math.sqrt((velocityX * velocityX + velocityY * velocityY).toDouble())

                    altitudeText.text = "📏 Altitudine: ${String.format("%.1f", altitude)}m"

                    val velocityStatus = buildString {
                        append("🚀 Velocità: ${String.format("%.1f", horizontalSpeed)}m/s")
                        append(" | Verticale: ${String.format("%.1f", velocityZ)}m/s")

                        // Indicatore direzione verticale
                        when {
                            velocityZ > 0.5f -> append(" ⬆️")
                            velocityZ < -0.5f -> append(" ⬇️")
                            else -> append(" ➡️")
                        }
                    }
                    velocityText.text = velocityStatus
                } else {
                    altitudeText.text = "📏 Altitudine: N/A"
                    velocityText.text = "🚀 Velocità: N/A"
                }
            }
        } catch (e: Exception) {
            runOnUiThread {
                altitudeText.text = "❓ Altitudine: Errore"
                velocityText.text = "❓ Velocità: Errore"
            }
        }
    }

    /**
     * Abilita APAS 4.0 (sistema principale Mini 3 Pro)
     */
    private fun enableAPAS() {
        try {
            val perceptionManager = PerceptionManager.getInstance()

            perceptionManager.setObstacleAvoidanceEnabled(true, object : CommonCallbacks.CompletionCallback {
                override fun onSuccess() {
                    runOnUiThread {
                        Toast.makeText(this@MainActivity, "✅ APAS 4.0 abilitato!", Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onFailure(error: IDJIError) {
                    runOnUiThread {
                        Toast.makeText(this@MainActivity, "❌ Errore APAS: ${error.description()}", Toast.LENGTH_LONG).show()
                    }
                }
            })
        } catch (e: Exception) {
            Toast.makeText(this, "Errore controllo APAS: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    /**
     * Disabilita APAS 4.0 (ATTENZIONE!)
     */
    private fun disableAPAS() {
        try {
            val perceptionManager = PerceptionManager.getInstance()

            perceptionManager.setObstacleAvoidanceEnabled(false, object : CommonCallbacks.CompletionCallback {
                override fun onSuccess() {
                    runOnUiThread {
                        Toast.makeText(this@MainActivity, "⚠️ APAS 4.0 disabilitato! Volo manuale!", Toast.LENGTH_LONG).show()
                    }
                }

                override fun onFailure(error: IDJIError) {
                    runOnUiThread {
                        Toast.makeText(this@MainActivity, "❌ Errore disabilitazione APAS: ${error.description()}", Toast.LENGTH_LONG).show()
                    }
                }
            })
        } catch (e: Exception) {
            Toast.makeText(this, "Errore controllo APAS: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    /**
     * Toggle Vision Positioning (sostituisce Precision Landing non supportato)
     */
    private fun toggleVisionPositioning() {
        try {
            val perceptionManager = PerceptionManager.getInstance()
            val currentState = perceptionManager.isVisionPositioningSensorEnabled()

            perceptionManager.setVisionPositioningEnabled(!currentState, object : CommonCallbacks.CompletionCallback {
                override fun onSuccess() {
                    runOnUiThread {
                        val newState = if (!currentState) "attivato" else "disattivato"
                        Toast.makeText(this@MainActivity, "👁️ Vision Positioning $newState", Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onFailure(error: IDJIError) {
                    runOnUiThread {
                        Toast.makeText(this@MainActivity, "❌ Errore Vision Positioning: ${error.description()}", Toast.LENGTH_LONG).show()
                    }
                }
            })
        } catch (e: Exception) {
            Toast.makeText(this, "Errore Vision Positioning: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    // Funzioni di volo base
    private fun takeoff() {
        val flightControlManager = FlightControlManager.getInstance()
        flightControlManager.startTakeoff(object : CommonCallbacks.CompletionCallback {
            override fun onSuccess() {
                runOnUiThread {
                    Toast.makeText(this@MainActivity, "🛫 Decollo avviato!", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(error: IDJIError) {
                runOnUiThread {
                    Toast.makeText(this@MainActivity, "❌ Decollo fallito: ${error.description()}", Toast.LENGTH_LONG).show()
                }
            }
        })
    }

    private fun land() {
        val flightControlManager = FlightControlManager.getInstance()
        flightControlManager.startLanding(object : CommonCallbacks.CompletionCallback {
            override fun onSuccess() {
                runOnUiThread {
                    Toast.makeText(this@MainActivity, "🛬 Atterraggio avviato!", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(error: IDJIError) {
                runOnUiThread {
                    Toast.makeText(this@MainActivity, "❌ Atterraggio fallito: ${error.description()}", Toast.LENGTH_LONG).show()
                }
            }
        })
    }

    private fun checkConnection() {
        val product = SDKManager.getInstance().getProduct()
        val isConnected = product != null

        val status = if (isConnected) {
            val productName = product?.productType?.name ?: "Sconosciuto"
            "✅ Connesso: $productName"
        } else {
            "❌ Disconnesso"
        }

        statusText.text = status
        Toast.makeText(this, status, Toast.LENGTH_SHORT).show()

        // Aggiorna stato bottoni
        val buttonsEnabled = isConnected
        takeoffButton.isEnabled = buttonsEnabled
        landButton.isEnabled = buttonsEnabled
        enableObstacleAvoidanceButton.isEnabled = buttonsEnabled
        disableObstacleAvoidanceButton.isEnabled = buttonsEnabled
        visionPositioningButton.isEnabled = buttonsEnabled
    }

    private fun updateStatus() {
        runOnUiThread {
            val product = SDKManager.getInstance().getProduct()
            val isConnected = product != null

            statusText.text = if (isConnected) {
                val productName = product?.productType?.name ?: "Sconosciuto"
                "🚁 Connesso: $productName"
            } else {
                "❌ In attesa di connessione..."
            }

            // Update buttons
            takeoffButton.isEnabled = isConnected
            landButton.isEnabled = isConnected
            enableObstacleAvoidanceButton.isEnabled = isConnected
            disableObstacleAvoidanceButton.isEnabled = isConnected
            visionPositioningButton.isEnabled = isConnected
        }
    }

    private fun showDisconnectedState() {
        runOnUiThread {
            visionStatusText.text = "👁️ Vision System: Disconnesso"
            obstacleDistanceText.text = "📡 Ostacoli: N/A"
            gpsStatusText.text = "🛰️ GPS: N/A"
            batteryStatusText.text = "🔋 Batteria: N/A"
            altitudeText.text = "📏 Altitudine: N/A"
            velocityText.text = "🚀 Velocità: N/A"
        }
    }

    /**
     * Mostra capacità specifiche Mini 3 Pro all'avvio
     */
    private fun showMini3ProCapabilities() {
        Toast.makeText(this, """
            🎯 DJI Mini 3 Pro Capabilities:
            ✅ APAS 4.0 Obstacle Avoidance
            ✅ Vision Positioning  
            ✅ 3-Direction Sensors (F/B/D)
            ❌ Precision Landing (non supportato)
            ❌ Side Sensors (non disponibili)
        """.trimIndent(), Toast.LENGTH_LONG).show()
    }

    override fun onDestroy() {
        super.onDestroy()
        sensorUpdateJob?.cancel()
    }
}