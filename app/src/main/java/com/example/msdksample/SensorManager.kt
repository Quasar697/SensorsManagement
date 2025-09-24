package com.example.msdksample

import android.util.Log
import dji.v5.common.callback.CommonCallbacks
import dji.v5.common.error.IDJIError
import dji.v5.manager.KeyManager
import dji.v5.manager.datacenter.MediaDataCenter
import dji.v5.manager.aircraft.perception.ObstacleAvoidanceManager
import dji.v5.manager.datacenter.flight.FlightDataCenter
import dji.v5.common.flight.FlightModeState
import dji.v5.common.flight.FlightState
import dji.v5.manager.datacenter.flight.FlightControlState
import dji.v5.manager.datacenter.battery.BatteryDataCenter
import dji.sdk.keyvalue.key.FlightControllerKey
import dji.sdk.keyvalue.key.AirLinkKey
import dji.sdk.keyvalue.key.BatteryKey
import dji.sdk.keyvalue.value.common.LocationCoordinate3D
import dji.sdk.keyvalue.value.common.Attitude
import kotlinx.coroutines.*

class SensorManager {
    companion object {
        private const val TAG = "SensorManager"
    }

    private var sensorJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.Main)

    // Callback per ricevere i dati dei sensori
    interface SensorDataCallback {
        fun onFlightDataUpdated(
            altitude: Double,
            velocityX: Double,
            velocityY: Double,
            velocityZ: Double,
            attitude: Attitude?
        )
        fun onLocationUpdated(location: LocationCoordinate3D?)
        fun onBatteryDataUpdated(percentage: Int, voltage: Double)
        fun onObstacleDataUpdated(obstacles: Map<String, Double>)
        fun onFlightStateUpdated(state: String)
    }

    private var callback: SensorDataCallback? = null

    fun setCallback(callback: SensorDataCallback) {
        this.callback = callback
    }

    /**
     * Avvia la lettura periodica di tutti i sensori
     */
    fun startSensorReading(intervalMs: Long = 1000) {
        stopSensorReading() // Ferma eventuali letture precedenti

        sensorJob = scope.launch {
            while (isActive) {
                try {
                    readAllSensors()
                    delay(intervalMs)
                } catch (e: Exception) {
                    Log.e(TAG, "Errore nella lettura dei sensori: ${e.message}")
                }
            }
        }

        Log.d(TAG, "Lettura sensori avviata")
    }

    /**
     * Ferma la lettura dei sensori
     */
    fun stopSensorReading() {
        sensorJob?.cancel()
        sensorJob = null
        Log.d(TAG, "Lettura sensori fermata")
    }

    /**
     * Legge tutti i sensori disponibili
     */
    private suspend fun readAllSensors() = withContext(Dispatchers.IO) {
        readFlightControlData()
        readPerceptionData()
        readGPSData()
        readBatteryData()
        readCompassData()
        readGimbalData()
    }

    /**
     * Legge i dati di controllo del volo (velocità, altitudine, attitude, ecc.)
     */
    private fun readFlightControlData() {
        try {
            val flightControlManager = FlightControlManager.getInstance()

            // Ottieni i dati di stato del volo
            val flightControlData = flightControlManager.flightControlData

            flightControlData?.let { data ->
                Log.d(TAG, """
                    === FLIGHT CONTROL DATA ===
                    Altitudine: ${data.altitude}m
                    Velocità verticale: ${data.velocityZ} m/s
                    Velocità X: ${data.velocityX} m/s
                    Velocità Y: ${data.velocityY} m/s
                    Pitch: ${data.attitude?.pitch}°
                    Roll: ${data.attitude?.roll}°
                    Yaw: ${data.attitude?.yaw}°
                    Modalità volo: ${data.flightMode}
                    GPS Signal: ${data.gpsSignalLevel}
                """.trimIndent())

                callback?.onFlightControlDataUpdated(data)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Errore lettura FlightControl: ${e.message}")
        }
    }

    /**
     * Legge i dati dei sensori di percezione (ostacoli, distanze)
     */
    private fun readPerceptionData() {
        try {
            val perceptionManager = PerceptionManager.getInstance()

            // Dati di percezione degli ostacoli
            val perceptionData = perceptionManager.perceptionInfo

            perceptionData?.let { data ->
                Log.d(TAG, """
                    === PERCEPTION DATA ===
                    Ostacolo frontale: ${data.obstacleAvoidanceType}
                    Distanza ostacolo avanti: ${data.upwardObstacleDistance}m
                    Distanza ostacolo dietro: ${data.backwardObstacleDistance}m
                    Distanza ostacolo sinistra: ${data.leftObstacleDistance}m
                    Distanza ostacolo destra: ${data.rightObstacleDistance}m
                    Distanza ostacolo sotto: ${data.downwardObstacleDistance}m
                """.trimIndent())

                callback?.onPerceptionDataUpdated(data)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Errore lettura Perception: ${e.message}")
        }
    }

    /**
     * Legge i dati GPS
     */
    private fun readGPSData() {
        try {
            val flightControlManager = FlightControlManager.getInstance()
            val locationData = flightControlManager.flightControlData?.location

            locationData?.let { location ->
                val lat = location.latitude
                val lon = location.longitude
                val alt = location.altitude

                Log.d(TAG, """
                    === GPS DATA ===
                    Latitudine: $lat°
                    Longitudine: $lon°
                    Altitudine: ${alt}m
                    Satelliti: ${flightControlManager.flightControlData?.gpsSignalLevel}
                """.trimIndent())

                callback?.onGPSDataUpdated(lat, lon, alt)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Errore lettura GPS: ${e.message}")
        }
    }

    /**
     * Legge i dati della batteria
     */
    private fun readBatteryData() {
        try {
            val flightControlManager = FlightControlManager.getInstance()
            val batteryData = flightControlManager.flightControlData?.batteryState

            batteryData?.let { battery ->
                val percentage = battery.chargeRemainingInPercent
                val voltage = battery.voltage / 1000.0 // Converti da mV a V

                Log.d(TAG, """
                    === BATTERY DATA ===
                    Percentuale: ${percentage}%
                    Voltaggio: ${voltage}V
                    Corrente: ${battery.current}mA
                    Temperatura: ${battery.temperature}°C
                    Cicli: ${battery.numberOfDischarge}
                """.trimIndent())

                callback?.onBatteryDataUpdated(percentage, voltage)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Errore lettura batteria: ${e.message}")
        }
    }

    /**
     * Legge i dati della bussola
     */
    private fun readCompassData() {
        try {
            val flightControlManager = FlightControlManager.getInstance()
            val compassData = flightControlManager.flightControlData?.attitude

            compassData?.let { attitude ->
                val heading = attitude.yaw // Direzione in gradi

                Log.d(TAG, """
                    === COMPASS DATA ===
                    Direzione (Yaw): ${heading}°
                    Inclinazione (Pitch): ${attitude.pitch}°
                    Rollio (Roll): ${attitude.roll}°
                """.trimIndent())

                callback?.onCompassDataUpdated(heading)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Errore lettura bussola: ${e.message}")
        }
    }

    /**
     * Legge i dati del gimbal (se presente)
     */
    private fun readGimbalData() {
        try {
            // Nota: Il gimbal potrebbe richiedere un manager specifico
            // Questo è un esempio base - potrebbe essere necessario utilizzare
            // il GimbalManager specifico per il modello di drone

            val flightControlManager = FlightControlManager.getInstance()
            // I dati del gimbal potrebbero essere disponibili tramite altri manager
            // Questo è un placeholder per la struttura

            Log.d(TAG, "=== GIMBAL DATA === (Placeholder)")
            // callback?.onGimbalDataUpdated(pitch, roll, yaw)

        } catch (e: Exception) {
            Log.e(TAG, "Errore lettura gimbal: ${e.message}")
        }
    }

    /**
     * Legge informazioni specifiche sui sensori di visione
     */
    fun readVisionSensors() {
        try {
            val perceptionManager = PerceptionManager.getInstance()

            // Stato dei sensori di visione
            val visionSystemState = perceptionManager.perceptionInfo

            visionSystemState?.let { vision ->
                Log.d(TAG, """
                    === VISION SENSORS ===
                    Sistema visione attivo: ${vision.isObstacleAvoidanceEnabled}
                    Precisione posizionamento: ${vision.positioningSolution}
                    Modalità volo RTH: ${vision.isGoingHome}
                """.trimIndent())
            }

        } catch (e: Exception) {
            Log.e(TAG, "Errore lettura sensori visione: ${e.message}")
        }
    }

    /**
     * Ottieni un singolo valore di un sensore specifico
     */
    fun getCurrentAltitude(): Double? {
        return try {
            FlightControlManager.getInstance().flightControlData?.altitude
        } catch (e: Exception) {
            Log.e(TAG, "Errore ottenimento altitudine: ${e.message}")
            null
        }
    }

    fun getCurrentSpeed(): Triple<Double, Double, Double>? {
        return try {
            val data = FlightControlManager.getInstance().flightControlData
            data?.let {
                Triple(it.velocityX, it.velocityY, it.velocityZ)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Errore ottenimento velocità: ${e.message}")
            null
        }
    }

    fun getCurrentGPS(): Triple<Double, Double, Double>? {
        return try {
            val location = FlightControlManager.getInstance().flightControlData?.location
            location?.let {
                Triple(it.latitude, it.longitude, it.altitude)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Errore ottenimento GPS: ${e.message}")
            null
        }
    }

    fun getBatteryPercentage(): Int? {
        return try {
            FlightControlManager.getInstance().flightControlData?.batteryState?.chargeRemainingInPercent
        } catch (e: Exception) {
            Log.e(TAG, "Errore ottenimento batteria: ${e.message}")
            null
        }
    }
}