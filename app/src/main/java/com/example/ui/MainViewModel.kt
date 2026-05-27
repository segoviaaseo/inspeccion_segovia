package com.example.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import android.widget.Toast
import androidx.core.content.FileProvider
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.api.CloudSyncApi
import com.example.data.db.AppDatabase
import com.example.data.model.InspectionItem
import com.example.data.model.Vehicle
import com.example.data.model.VehicleInspection
import com.example.pdf.PdfGenerator
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

enum class AppView {
    LIST, NEW, DETAIL, VEHICLES, DOCUMENTS, SETTINGS
}

class MainViewModel(private val context: Context) : ViewModel() {
    private val TAG = "MainViewModel"

    private val db = AppDatabase.getDatabase(context)
    private val vehicleDao = db.vehicleDao()
    private val inspectionDao = db.inspectionDao()

    // Expose flows from db
    val vehicles: StateFlow<List<Vehicle>> = vehicleDao.getAllVehiclesFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val inspections: StateFlow<List<VehicleInspection>> = inspectionDao.getAllInspectionsFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // UI Navigation State
    private val _currentView = MutableStateFlow(AppView.LIST)
    val currentView: StateFlow<AppView> = _currentView.asStateFlow()

    fun navigateTo(view: AppView) {
        _currentView.value = view
    }

    // Shared preferences for settings (Inspector Name and server URL)
    private val prefs = context.getSharedPreferences("segovia_aseo_prefs", Context.MODE_PRIVATE)

    private val _inspectorName = MutableStateFlow(prefs.getString("inspector_name", "Inspector") ?: "Inspector")
    val inspectorName: StateFlow<String> = _inspectorName.asStateFlow()

    fun setInspectorName(name: String) {
        _inspectorName.value = name
        prefs.edit().putString("inspector_name", name).apply()
    }

    private val _serverUrl = MutableStateFlow(prefs.getString("server_url", "") ?: "")
    val serverUrl: StateFlow<String> = _serverUrl.asStateFlow()

    fun setServerUrl(url: String) {
        _serverUrl.value = url
        prefs.edit().putString("server_url", url).apply()
    }

    // Active inspection builder
    private val _currentInspection = MutableStateFlow<VehicleInspection?>(null)
    val currentInspection: StateFlow<VehicleInspection?> = _currentInspection.asStateFlow()

    // Inspection selection for Details view
    private val _selectedInspection = MutableStateFlow<VehicleInspection?>(null)
    val selectedInspection: StateFlow<VehicleInspection?> = _selectedInspection.asStateFlow()

    // Chosen vehicle ID to start inspection
    private val _selectedVehicleId = MutableStateFlow("")
    val selectedVehicleId: StateFlow<String> = _selectedVehicleId.asStateFlow()

    fun setSelectedVehicleId(id: String) {
        _selectedVehicleId.value = id
    }

    // Custom inspection date configuration
    private val _customInspectionDate = MutableStateFlow(
        SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Calendar.getInstance().time)
    )
    val customInspectionDate: StateFlow<String> = _customInspectionDate.asStateFlow()

    fun setCustomInspectionDate(date: String) {
        _customInspectionDate.value = date
    }

    // Syncing state indicators
    private val _isSyncing = MutableStateFlow(false)
    val isSyncing: StateFlow<Boolean> = _isSyncing.asStateFlow()

    init {
        // Pre-fill mock data if database is empty
        viewModelScope.launch {
            val existingVehicles = vehicleDao.getAllVehicles()
            if (existingVehicles.isEmpty()) {
                Log.d(TAG, "Initializing default sample vehicles in Room...")
                val samples = listOf(
                    Vehicle("1", "Chevrolet C70", "LHJ747", "Camion", "2025-09-27", "2025-06-20"),
                    Vehicle("2", "Volkswagen", "TDY440", "Camion", "2025-09-27", "2026-03-10"),
                    Vehicle("3", "International", "OMG782", "Camion", "2026-01-24", "2025-08-25"),
                    Vehicle("4", "Chevrolet NPR", "OMK279", "Camion", "2025-08-15", "2026-01-03"),
                    Vehicle("5", "Chevrolet NPR", "TTY231", "Camion", "2026-06-16", "2025-06-19"),
                    Vehicle("6", "Chevrolet C70", "KEG277", "Volqueta", "2025-12-28", "2026-02-22"),
                    Vehicle("7", "Chevrolet C70", "LHI938", "Volqueta", "2025-09-30", "2025-10-01"),
                    Vehicle("8", "Vaisand", "831AEV", "Motocarguero", "2025-10-11", "2025-10-11")
                )
                samples.forEach { vehicleDao.insertVehicle(it) }
            }
        }
    }

    // Default inspection checklist template
    private val defaultChecklistTemplates = listOf(
        // Technical Category
        Triple("1", "Luces delanteras", "technical"),
        Triple("2", "Luces traseras", "technical"),
        Triple("3", "Luces de freno", "technical"),
        Triple("4", "Luces intermitentes", "technical"),
        Triple("5", "Alarma de reversa", "technical"),
        Triple("6", "Estado de las llantas", "technical"),
        Triple("7", "Estado de fugas en general", "technical"),
        Triple("8", "Estado de los mandos hidraulicos (Compactadores)", "technical"),
        Triple("8a", "Funcionamiento placa eyectora", "technical"),
        Triple("8b", "Funcionamiento placa barredora", "technical"),
        Triple("8c", "Funcionamiento placa deslizante", "technical"),
        Triple("9", "Nivel de combustible", "technical"),
        Triple("10", "Nivel de aceite hidraulico", "technical"),
        Triple("11", "Nivel de aceite motor", "technical"),
        Triple("12", "Nivel de refrigerante", "technical"),
        Triple("13", "Nivel de líquido de frenos", "technical"),
        Triple("14", "Nivel de líquido limpiaparabrisas", "technical"),
        Triple("15", "Frenos", "technical"),
        Triple("16", "Cinturones de seguridad", "technical"),
        Triple("17", "Bocina", "technical"),
        Triple("18", "Limpiaparabrisas", "technical"),
        Triple("19", "Espejos", "technical"),
        Triple("20", "Llanta de repuesto", "technical"),
        Triple("21", "Caja de herramientas básicas", "technical"),
        
        // Safety Category
        Triple("22", "Botiquín", "safety"),
        Triple("23", "Guantes anticorte", "safety"),
        Triple("24", "Conos de seguridad", "safety"),
        Triple("25", "Extintor", "safety"),
        Triple("26", "Linterna", "safety"),
        
        // Legal Category
        Triple("27", "SOAT vigente", "legal"),
        Triple("28", "RTM vigente", "legal"),
        Triple("29", "Licencia de conducir vigente", "legal"),
        Triple("30", "Tarjeta de propiedad", "legal")
    )

    /**
     * Start a new pre-operational inspection sheet
     */
    fun startNewInspection() {
        val vehicleId = _selectedVehicleId.value
        if (vehicleId.isEmpty()) return

        val now = Calendar.getInstance().time
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val timeFormat = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
        
        // Use the configured customInspectionDate if set, otherwise fallback to today's date
        val dateStr = if (_customInspectionDate.value.isNotEmpty()) {
            _customInspectionDate.value
        } else {
            dateFormat.format(now)
        }
        val timeStr = timeFormat.format(now)

        val items = defaultChecklistTemplates.map { (id, name, cat) ->
            InspectionItem(id = id, name = name, status = "not-checked", notes = null, category = cat)
        }

        _currentInspection.value = VehicleInspection(
            id = System.currentTimeMillis().toString(),
            date = dateStr,
            vehicleId = vehicleId,
            inspector = _inspectorName.value,
            startTime = timeStr,
            items = items,
            completed = false,
            isSynced = false
        )
        
        _currentView.value = AppView.NEW
    }

    /**
     * Dynamically update the dynamic date of an active inspection sheet
     */
    fun updateInspectionDate(newDate: String) {
        val ongoing = _currentInspection.value ?: return
        _currentInspection.value = ongoing.copy(date = newDate)
    }

    /**
     * Update an individual checklist item's progress
     */
    fun updateInspectionItem(itemId: String, status: String, notes: String? = null) {
        val ongoing = _currentInspection.value ?: return
        val updatedItems = ongoing.items.map { item ->
            if (item.id == itemId) {
                // If it is changed to fail, keep the previous note if not custom-specified
                item.copy(status = status, notes = notes ?: item.notes)
            } else {
                item
            }
        }
        _currentInspection.value = ongoing.copy(items = updatedItems)
    }

    fun updateInspectionGeneralNotes(notes: String) {
        val ongoing = _currentInspection.value ?: return
        _currentInspection.value = ongoing.copy(notes = notes)
    }

    fun updateInspectionSignatures(
        firmaInspector: String? = null,
        firmaAsistente: String? = null,
        nombreAsistente: String? = null,
        cargoAsistente: String? = null
    ) {
        val ongoing = _currentInspection.value ?: return
        _currentInspection.value = ongoing.copy(
            firmaInspector = firmaInspector ?: ongoing.firmaInspector,
            firmaAsistente = firmaAsistente ?: ongoing.firmaAsistente,
            nombreAsistente = nombreAsistente ?: ongoing.nombreAsistente,
            cargoAsistente = cargoAsistente ?: ongoing.cargoAsistente
        )
    }

    /**
     * Complete the inspection, saves to Room, and immediately schedules cloud sync backup.
     */
    fun completeInspection(onSuccess: () -> Unit) {
        val ongoing = _currentInspection.value ?: return

        // Integrity validation: make sure failed checklist items have notes
        val incompleteFailed = ongoing.items.filter { it.status == "fail" && it.notes.isNullOrBlank() }
        if (incompleteFailed.isNotEmpty()) {
            Toast.makeText(context, "Por favor describa las anomalías en comentarios (Fallas)", Toast.LENGTH_LONG).show()
            return
        }

        val now = Calendar.getInstance().time
        val timeFormat = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
        val finishTimeStr = timeFormat.format(now)

        val completedRecord = ongoing.copy(
            endTime = finishTimeStr,
            completed = true
        )

        viewModelScope.launch {
            // Save inside local Room Db first (offline-first security)
            inspectionDao.insertInspection(completedRecord)
            
            _currentInspection.value = null
            _selectedVehicleId.value = ""
            
            Toast.makeText(context, "Inspección Guardada Localmente", Toast.LENGTH_SHORT).show()
            onSuccess()
            _currentView.value = AppView.LIST

            // Fire and forget server uploading sync
            performDatabaseCloudSync()
        }
    }

    /**
     * Delete an inspection audit history log
     */
    fun deleteInspection(inspection: VehicleInspection) {
        viewModelScope.launch {
            inspectionDao.deleteInspection(inspection)
            Toast.makeText(context, "Inspección Eliminada", Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * Selects and loads the detail view for an inspection
     */
    fun viewInspectionDetails(inspection: VehicleInspection) {
        _selectedInspection.value = inspection
        _currentView.value = AppView.DETAIL
    }

    /**
     * Share or print the inspection PDF report
     */
    fun generateAndShareInspectionPdf(inspection: VehicleInspection, vehicle: Vehicle) {
        viewModelScope.launch {
            val pdfFile = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                PdfGenerator.generateCompactInspectionPdf(context, inspection, vehicle)
            }
            if (pdfFile != null && pdfFile.exists()) {
                try {
                    val authority = "${context.packageName}.fileprovider"
                    val contentUri = FileProvider.getUriForFile(context, authority, pdfFile)
                    
                    val shareIntent = Intent(Intent.ACTION_SEND).apply {
                        type = "application/pdf"
                        putExtra(Intent.EXTRA_STREAM, contentUri)
                        putExtra(Intent.EXTRA_SUBJECT, "Informe Preoperacional Placa: ${vehicle.licensePlate}")
                        putExtra(Intent.EXTRA_TEXT, "Generado de forma compacta para Segovia Aseo")
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    }
                    
                    val chooser = Intent.createChooser(shareIntent, "Compartir Reporte PDF Compacto").apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    context.startActivity(chooser)
                } catch (e: Exception) {
                    Toast.makeText(context, "Error al generar el intent: ${e.message}", Toast.LENGTH_LONG).show()
                }
            } else {
                Toast.makeText(context, "Error al escribir el archivo compacto PDF", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // Vehicle list maintenance functions (CRUD)
    fun saveVehicle(vehicle: Vehicle) {
        viewModelScope.launch {
            vehicleDao.insertVehicle(vehicle)
            Toast.makeText(context, "Vehículo Guardado Correctamente", Toast.LENGTH_SHORT).show()
            
            // Sync up to remote database background payload
            val url = _serverUrl.value
            if (url.isNotEmpty()) {
                CloudSyncApi.uploadVehicle(vehicle, url)
            }
        }
    }

    fun deleteVehicle(vehicle: Vehicle) {
        viewModelScope.launch {
            // Safety constraint: check if historical databases use this vehicle
            val allInsps = inspectionDao.getAllInspections()
            val usesCount = allInsps.count { it.vehicleId == vehicle.id }
            if (usesCount > 0) {
                Toast.makeText(context, "No se puede eliminar: tiene $usesCount inspecciones asociadas", Toast.LENGTH_LONG).show()
                return@launch
            }
            vehicleDao.deleteVehicle(vehicle)
            Toast.makeText(context, "Vehículo Eliminado", Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * Upload unresolved/offline entries directly to the server.
     */
    fun performDatabaseCloudSync() {
        if (_isSyncing.value) return
        
        viewModelScope.launch {
            _isSyncing.value = true
            val unsyncedList = inspectionDao.getUnsyncedInspections()
            
            if (unsyncedList.isEmpty()) {
                _isSyncing.value = false
                Log.d(TAG, "No pending cloud backups found.")
                return@launch
            }

            var successCounter = 0
            val targetUrl = _serverUrl.value
            
            unsyncedList.forEach { insp ->
                val result = CloudSyncApi.uploadInspection(insp, targetUrl)
                if (result) {
                    inspectionDao.markInspectionAsSynced(insp.id)
                    successCounter++
                }
            }

            _isSyncing.value = false
            if (successCounter > 0) {
                Toast.makeText(context, "Sincronizados $successCounter reportes en la nube", Toast.LENGTH_SHORT).show()
            } else if (targetUrl.isNotEmpty()) {
                Toast.makeText(context, "Error al conectar con servidor de Segovia Aseo", Toast.LENGTH_SHORT).show()
            }
        }
    }
}

class MainViewModelFactory(private val context: Context) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MainViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return MainViewModel(context) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
