package com.example.ui.screens

import android.widget.Space
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.foundation.Image
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.layout.ContentScale
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.InspectionItem
import com.example.data.model.Vehicle
import com.example.data.model.VehicleInspection
import com.example.ui.AppView
import com.example.ui.MainViewModel
import com.example.ui.SignatureCanvas
import com.example.ui.theme.LightBlueBg
import com.example.ui.theme.LightGreenBg
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InspectionAppUi(viewModel: MainViewModel) {
    val currentView by viewModel.currentView.collectAsStateWithLifecycle()
    val isSyncing by viewModel.isSyncing.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column(
                        verticalArrangement = Arrangement.Center,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "SEGOVIA ASEO S.A. E.S.P.",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Inspección de Vehículos",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White.copy(alpha = 0.9f)
                        )
                    }
                },
                actions = {
                    if (isSyncing) {
                        CircularProgressIndicator(
                            color = Color.White,
                            modifier = Modifier
                                .size(24.dp)
                                .padding(end = 6.dp),
                            strokeWidth = 2.5.dp
                        )
                    } else {
                        IconButton(onClick = { viewModel.performDatabaseCloudSync() }) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "Sincronizar nube",
                                tint = Color.White
                            )
                        }
                    }
                    IconButton(onClick = { viewModel.navigateTo(AppView.SETTINGS) }) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Configuración",
                            tint = Color.White
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            )
        },
        bottomBar = {
            if (currentView == AppView.LIST || currentView == AppView.VEHICLES || currentView == AppView.DOCUMENTS) {
                NavigationBar(
                    containerColor = Color.White,
                    tonalElevation = 8.dp
                ) {
                    NavigationBarItem(
                        selected = currentView == AppView.LIST,
                        onClick = { viewModel.navigateTo(AppView.LIST) },
                        icon = { Icon(Icons.Default.Home, contentDescription = "Inicio") },
                        label = { Text("Registros") },
                        modifier = Modifier.testTag("nav_home")
                    )
                    NavigationBarItem(
                        selected = currentView == AppView.DOCUMENTS,
                        onClick = { viewModel.navigateTo(AppView.DOCUMENTS) },
                        icon = { Icon(Icons.Default.CheckCircle, contentDescription = "SOAT/RTM") },
                        label = { Text("Documentos") },
                        modifier = Modifier.testTag("nav_docs")
                    )
                    NavigationBarItem(
                        selected = currentView == AppView.VEHICLES,
                        onClick = { viewModel.navigateTo(AppView.VEHICLES) },
                        icon = { Icon(Icons.Default.Build, contentDescription = "Flota") },
                        label = { Text("Vehículos") },
                        modifier = Modifier.testTag("nav_vehicles")
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(Color(0xFFF1F5F9)) // Smooth light grey background
        ) {
            when (currentView) {
                AppView.LIST -> HistoryListScreen(viewModel)
                AppView.NEW -> NewInspectionScreen(viewModel)
                AppView.DETAIL -> InspectionDetailScreen(viewModel)
                AppView.VEHICLES -> VehiclesManagementScreen(viewModel)
                AppView.DOCUMENTS -> DocumentsMonitorScreen(viewModel)
                AppView.SETTINGS -> CloudSettingsScreen(viewModel)
            }
        }
    }
}

// Helper date parsing values
private fun getDocumentStatusColor(expiryDateStr: String?): Color {
    if (expiryDateStr.isNullOrEmpty()) return Color.Gray
    return try {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val today = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.time
        val expiry = dateFormat.parse(expiryDateStr) ?: return Color.Gray
        val diff = expiry.time - today.time
        val days = Math.ceil(diff.toDouble() / (1000 * 60 * 60 * 24)).toInt()
        when {
            days < 0 -> Color(0xFFEF4444) // Vibrant Alert Red
            days <= 30 -> Color(0xFFF59E0B) // Amber Expiry Warning
            else -> Color(0xFF10B981) // Emerald Safe Green
        }
    } catch (e: Exception) {
        Color.Gray
    }
}

private fun formatDaysLeft(expiryDateStr: String?): String {
    if (expiryDateStr.isNullOrEmpty()) return "No registrada"
    return try {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val today = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.time
        val expiry = dateFormat.parse(expiryDateStr) ?: return "Formato inválido"
        val diff = expiry.time - today.time
        val days = Math.ceil(diff.toDouble() / (1000 * 60 * 60 * 24)).toInt()
        when {
            days < 0 -> "Vencido hace ${Math.abs(days)} días"
            days == 0 -> "Vence hoy mismo ⚠️"
            days <= 30 -> "Vence en $days días ⚠️"
            else -> "Vigente ($days días)"
        }
    } catch (e: Exception) {
        "Error"
    }
}

@Composable
fun HistoryListScreen(viewModel: MainViewModel) {
    val inspections by viewModel.inspections.collectAsStateWithLifecycle()
    val vehicles by viewModel.vehicles.collectAsStateWithLifecycle()
    val selectedVehicleId by viewModel.selectedVehicleId.collectAsStateWithLifecycle()
    val inspectorName by viewModel.inspectorName.collectAsStateWithLifecycle()
    val customInspectionDate by viewModel.customInspectionDate.collectAsStateWithLifecycle()

    var inspectorInput by remember { mutableStateOf(inspectorName) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Corporate Heading Hero Badge
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("hero_banner")
            ) {
                Column(
                    modifier = Modifier
                        .background(
                            Brush.linearGradient(
                                colors = listOf(Color(0xFF0066CC), Color(0xFF00A86B))
                            )
                        )
                        .padding(20.dp)
                ) {
                    Text(
                        text = "Sistema Preoperacional",
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Complete el chequeo e inspección obligatoria de su vehículo para garantizar la seguridad operacional.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFFA7F3D0),
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }
        }

        // Inspector config block
        item {
            Card(
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Responsable de Inspección",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1E293B)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = inspectorInput,
                        onValueChange = {
                            inspectorInput = it
                            viewModel.setInspectorName(it)
                        },
                        placeholder = { Text("Nombre del Inspector") },
                        singleLine = true,
                        leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
                        modifier = Modifier.fillMaxWidth().testTag("inspector_input")
                    )
                }
            }
        }

        // New inspection launcher
        item {
            Card(
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Comenzar Registro Preoperacional",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1E293B)
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    
                    var expanded by remember { mutableStateOf(false) }
                    val currentSelection = vehicles.find { it.id == selectedVehicleId }

                    Box(modifier = Modifier.fillMaxWidth()) {
                        OutlinedButton(
                            onClick = { expanded = true },
                            modifier = Modifier.fillMaxWidth().testTag("vehicle_selector_dropdown")
                        ) {
                            Text(
                                text = currentSelection?.let { "${it.name} (${it.licensePlate})" } ?: "Seleccionar Vehículo",
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(Modifier.weight(1f))
                            Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                        }
                        
                        DropdownMenu(
                            expanded = expanded,
                            onDismissRequest = { expanded = false },
                            modifier = Modifier.fillMaxWidth(0.9f)
                        ) {
                            vehicles.forEach { vehicle ->
                                DropdownMenuItem(
                                    text = { Text("${vehicle.name} [Placa: ${vehicle.licensePlate}]") },
                                    onClick = {
                                        viewModel.setSelectedVehicleId(vehicle.id)
                                        expanded = false
                                    }
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Date configuration field
                    val context = LocalContext.current
                    OutlinedButton(
                        onClick = {
                            val calendar = Calendar.getInstance()
                            val dateParts = customInspectionDate.split("-")
                            val initYear = if (dateParts.size == 3) dateParts[0].toIntOrNull() ?: calendar.get(Calendar.YEAR) else calendar.get(Calendar.YEAR)
                            val initMonth = if (dateParts.size == 3) (dateParts[1].toIntOrNull() ?: 1) - 1 else calendar.get(Calendar.MONTH)
                            val initDay = if (dateParts.size == 3) dateParts[2].toIntOrNull() ?: calendar.get(Calendar.DAY_OF_MONTH) else calendar.get(Calendar.DAY_OF_MONTH)

                            android.app.DatePickerDialog(
                                context,
                                { _, year, month, dayOfMonth ->
                                    val formattedMonth = String.format("%02d", month + 1)
                                    val formattedDay = String.format("%02d", dayOfMonth)
                                    viewModel.setCustomInspectionDate("$year-$formattedMonth-$formattedDay")
                                },
                                initYear,
                                initMonth,
                                initDay
                            ).show()
                        },
                        modifier = Modifier.fillMaxWidth().height(48.dp).testTag("custom_date_selector")
                    ) {
                        Icon(
                            imageVector = Icons.Default.DateRange,
                            contentDescription = "Cambiar Fecha",
                            tint = Color(0xFF0066CC)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Fecha de Trabajo: $customInspectionDate",
                            color = Color(0xFF1E293B)
                        )
                        Spacer(Modifier.weight(1f))
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Editar",
                            tint = Color(0xFF64748B),
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Button(
                        onClick = { viewModel.startNewInspection() },
                        enabled = selectedVehicleId.isNotEmpty() && inspectorInput.isNotBlank(),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("start_inspection_btn"),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF0066CC)
                        )
                    ) {
                        Icon(Icons.Default.PlayArrow, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Iniciar Inspección")
                    }
                }
            }
        }

        // Checklist Logs block
        item {
            Text(
                text = "Historial de Inspecciones",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF1E293B)
            )
        }

        if (inspections.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White)
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp).fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = null,
                            tint = Color.LightGray,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "No hay registros preoperacionales",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.Gray,
                            textAlign = TextAlign.Center
                        )
                        Text(
                            text = "Inicie un chequeo seleccionando un vehículo.",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.LightGray,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        } else {
            items(inspections, key = { it.id }) { inspection ->
                val matchingVehicle = vehicles.find { it.id == inspection.vehicleId }
                
                Card(
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = BorderStroke(1.dp, Color(0xFFE2E8F0))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = matchingVehicle?.name ?: "Vehículo Eliminado",
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = Color(0xFF1E293B)
                                )
                                Text(
                                    text = "Placa: ${matchingVehicle?.licensePlate ?: "N/A"}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = Color(0xFF0066CC),
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                            
                            // Synchronized state badge
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                if (inspection.isSynced) {
                                    Icon(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = "Sincronizado",
                                        tint = Color(0xFF10B981)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "Nube",
                                        color = Color(0xFF10B981),
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold
                                    )
                                } else {
                                    Icon(
                                        imageVector = Icons.Default.Warning,
                                        contentDescription = "Solo Local",
                                        tint = Color(0xFFF59E0B)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "Local",
                                        color = Color(0xFFF59E0B),
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }

                        HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp), color = Color(0xFFF1F5F9))

                        Row(
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column {
                                Text(
                                    text = "Inspector: ${inspection.inspector}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color(0xFF64748B)
                                )
                                Text(
                                    text = "Fecha: ${inspection.date}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color(0xFF64748B)
                                )
                            }
                            
                            // Deficiencies summary box
                            val fails = inspection.items.count { it.status == "fail" }
                            if (fails > 0) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(Color(0xFFFEE2E2))
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Text(
                                        text = "$fails Fallas 🚨",
                                        color = Color(0xFFB91C1C),
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            } else {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(Color(0xFFD1FAE5))
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Text(
                                        text = "Sin Fallas ✓",
                                        color = Color(0xFF065F46),
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Action Controls
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            OutlinedButton(
                                onClick = { viewModel.viewInspectionDetails(inspection) },
                                modifier = Modifier.weight(1f).testTag("action_view_${inspection.id}"),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text("Ver Reporte", fontSize = 11.sp)
                            }
                            
                            Button(
                                onClick = {
                                    matchingVehicle?.let { vehicle ->
                                        viewModel.generateAndShareInspectionPdf(inspection, vehicle)
                                    }
                                },
                                modifier = Modifier.weight(1f).testTag("action_pdf_${inspection.id}"),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00A86B)),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Icon(
                                    Icons.Default.Share, 
                                    contentDescription = null, 
                                    modifier = Modifier.size(13.dp)
                                )
                                Spacer(modifier = Modifier.width(3.dp))
                                Text("PDF", fontSize = 11.sp, color = Color.White)
                            }

                            IconButton(
                                onClick = { viewModel.deleteInspection(inspection) },
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color(0xFFFEF2F2))
                                    .testTag("action_delete_${inspection.id}")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = "Eliminar Registro",
                                    tint = Color(0xFFEF4444)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun NewInspectionScreen(viewModel: MainViewModel) {
    val currentInspection by viewModel.currentInspection.collectAsStateWithLifecycle()
    val vehicles by viewModel.vehicles.collectAsStateWithLifecycle()
    val context = LocalContext.current

    if (currentInspection == null) return
    val inspection = currentInspection!!
    val matchingVehicle = vehicles.find { it.id == inspection.vehicleId }

    val technicalItems = inspection.items.filter { it.category == "technical" }
    val safetyItems = inspection.items.filter { it.category == "safety" }
    val legalItems = inspection.items.filter { it.category == "legal" }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                IconButton(onClick = { viewModel.navigateTo(AppView.LIST) }) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Chequeo Preoperacional",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1E293B)
                )
            }
        }

        // Vehicle summary card
        item {
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = LightBlueBg),
                border = BorderStroke(1.dp, Color(0xFF0066CC).copy(alpha = 0.3f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = matchingVehicle?.name ?: "Desconocido",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium,
                        color = Color(0xFF0066CC)
                    )
                    Text(
                        text = "Patente/Placa: ${matchingVehicle?.licensePlate ?: "N/A"}",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF1E293B)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Tipo: ${matchingVehicle?.type ?: "N/A"} | Iniciado: ${inspection.startTime}",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF64748B)
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                val calendar = Calendar.getInstance()
                                val dateParts = inspection.date.split("-")
                                val initYear = if (dateParts.size == 3) dateParts[0].toIntOrNull() ?: calendar.get(Calendar.YEAR) else calendar.get(Calendar.YEAR)
                                val initMonth = if (dateParts.size == 3) (dateParts[1].toIntOrNull() ?: 1) - 1 else calendar.get(Calendar.MONTH)
                                val initDay = if (dateParts.size == 3) dateParts[2].toIntOrNull() ?: calendar.get(Calendar.DAY_OF_MONTH) else calendar.get(Calendar.DAY_OF_MONTH)

                                android.app.DatePickerDialog(
                                    context,
                                    { _, year, month, dayOfMonth ->
                                        val formattedMonth = String.format("%02d", month + 1)
                                        val formattedDay = String.format("%02d", dayOfMonth)
                                        viewModel.updateInspectionDate("$year-$formattedMonth-$formattedDay")
                                    },
                                    initYear,
                                    initMonth,
                                    initDay
                                ).show()
                            }
                            .background(Color.White, RoundedCornerShape(8.dp))
                            .border(1.dp, Color(0xFF0066CC).copy(alpha = 0.2f), RoundedCornerShape(8.dp))
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.DateRange,
                            contentDescription = "Cambiar Fecha",
                            tint = Color(0xFF0066CC),
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Fecha de Inspección: ${inspection.date} (Toque para cambiar)",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF0066CC)
                        )
                    }
                }
            }
        }

        // Group 1: Technical Mechanical checks
        item {
            CategoryHeading(title = "Inspección Técnico-Mecánica", subtitle = "Grupo de revisión motriz y sistemas")
        }

        items(technicalItems, key = { it.id }) { item ->
            ChecklistRow(item = item, onStatusChanged = { status, notes ->
                viewModel.updateInspectionItem(item.id, status, notes)
            })
        }

        // Group 2: Safety equipment check
        item {
            CategoryHeading(title = "Seguridad, Emergencia y Botiquín", subtitle = "Verificación de herramientas contra riesgos")
        }

        items(safetyItems, key = { it.id }) { item ->
            ChecklistRow(item = item, onStatusChanged = { status, notes ->
                viewModel.updateInspectionItem(item.id, status, notes)
            })
        }

        // Group 3: Legal properties check
        item {
            CategoryHeading(title = "Documentación Legal", subtitle = "Soporte reglamentario vigente requerido")
        }

        items(legalItems, key = { it.id }) { item ->
            ChecklistRow(item = item, onStatusChanged = { status, notes ->
                viewModel.updateInspectionItem(item.id, status, notes)
            })
        }

        // Notes box
        item {
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = BorderStroke(1.dp, Color(0xFFE2E8F0))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Observaciones Generales",
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1E293B)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = inspection.notes ?: "",
                        onValueChange = { viewModel.updateInspectionGeneralNotes(it) },
                        placeholder = { Text("Alguna novedad adicional sobre el vehículo...") },
                        modifier = Modifier.fillMaxWidth(),
                        maxLines = 4
                    )
                }
            }
        }

        // Signature capture
        item {
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = BorderStroke(1.dp, Color(0xFFE2E8F0))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Compromiso de Autenticidad y Firmas",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium,
                        color = Color(0xFF1E293B)
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    var nombreAsis by remember { mutableStateOf(inspection.nombreAsistente ?: "") }
                    var cargoAsis by remember { mutableStateOf(inspection.cargoAsistente ?: "Conductor") }

                    OutlinedTextField(
                        value = nombreAsis,
                        onValueChange = {
                            nombreAsis = it
                            viewModel.updateInspectionSignatures(nombreAsistente = it)
                        },
                        label = { Text("Nombre del Conductor / Mecánico") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text("Cargo del Firmante", style = MaterialTheme.typography.labelMedium)
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        listOf("Conductor", "Mecánico").forEach { role ->
                            FilterChip(
                                selected = cargoAsis == role,
                                onClick = {
                                    cargoAsis = role
                                    viewModel.updateInspectionSignatures(cargoAsistente = role)
                                },
                                label = { Text(role) }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Draw Inspector signature
                    SignatureCanvas(
                        label = "Firma del Inspector",
                        onSignatureChanged = { base64 ->
                            viewModel.updateInspectionSignatures(firmaInspector = base64)
                        }
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Draw assistant signature
                    SignatureCanvas(
                        label = "Firma del $cargoAsis",
                        onSignatureChanged = { base64 ->
                            viewModel.updateInspectionSignatures(firmaAsistente = base64)
                        }
                    )
                }
            }
        }

        // Save launcher button
        item {
            Button(
                onClick = {
                    viewModel.completeInspection {
                        // Action complete callback
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("submit_inspection_final"),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0066CC)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.Check, contentDescription = null)
                Spacer(modifier = Modifier.width(6.dp))
                Text("Guardar y Completar Chequeo", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }
            Spacer(modifier = Modifier.height(48.dp))
        }
    }
}

@Composable
fun CategoryHeading(title: String, subtitle: String) {
    Column(modifier = Modifier.padding(vertical = 8.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF0066CC)
        )
        Text(
            text = subtitle,
            style = MaterialTheme.typography.labelSmall,
            color = Color(0xFF64748B)
        )
    }
}

@Composable
fun ChecklistRow(
    item: InspectionItem,
    onStatusChanged: (String, String?) -> Unit
) {
    Card(
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = item.name,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp,
                    color = Color(0xFF1E293B),
                    modifier = Modifier.weight(1f)
                )

                // Pass / Fail Selection options
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // PASS button
                    OutlinedIconToggleButton(
                        checked = item.status == "pass",
                        onCheckedChange = { if (it) onStatusChanged("pass", null) },
                        modifier = Modifier
                            .size(36.dp)
                            .testTag("item_${item.id}_pass"),
                        shape = CircleShape,
                        colors = IconButtonDefaults.outlinedIconToggleButtonColors(
                            checkedContainerColor = Color(0xFFD1FAE5),
                            containerColor = Color(0xFFF8FAFC)
                        ),
                        border = BorderStroke(
                            1.dp,
                            if (item.status == "pass") Color(0xFF10B981) else Color(0xFFCBD5E1)
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = "Pasa",
                            tint = if (item.status == "pass") Color(0xFF065F46) else Color(0xFF94A3B8),
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    // FAIL button
                    OutlinedIconToggleButton(
                        checked = item.status == "fail",
                        onCheckedChange = { if (it) onStatusChanged("fail", item.notes) },
                        modifier = Modifier
                            .size(36.dp)
                            .testTag("item_${item.id}_fail"),
                        shape = CircleShape,
                        colors = IconButtonDefaults.outlinedIconToggleButtonColors(
                            checkedContainerColor = Color(0xFFFEE2E2),
                            containerColor = Color(0xFFF8FAFC)
                        ),
                        border = BorderStroke(
                            1.dp,
                            if (item.status == "fail") Color(0xFFEF4444) else Color(0xFFCBD5E1)
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = "No pasa",
                            tint = if (item.status == "fail") Color(0xFF991B1B) else Color(0xFF94A3B8),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            // Expand deficiency comment field on failure status
            if (item.status == "fail") {
                Spacer(modifier = Modifier.height(10.dp))
                
                var noteVal by remember { mutableStateOf(item.notes ?: "") }

                OutlinedTextField(
                    value = noteVal,
                    onValueChange = {
                        noteVal = it
                        onStatusChanged("fail", it)
                    },
                    placeholder = { Text("Indique el motivo de la falla obligatorio *") },
                    textStyle = MaterialTheme.typography.bodySmall,
                    isError = noteVal.isBlank(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("notes_input_${item.id}"),
                    singleLine = true
                )
            }
        }
    }
}

@Composable
fun InspectionDetailScreen(viewModel: MainViewModel) {
    val selectedInspection by viewModel.selectedInspection.collectAsStateWithLifecycle()
    val vehicles by viewModel.vehicles.collectAsStateWithLifecycle()

    if (selectedInspection == null) return
    val inspection = selectedInspection!!
    val vehicle = vehicles.find { it.id == inspection.vehicleId }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                IconButton(onClick = { viewModel.navigateTo(AppView.LIST) }) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Resumen de Registro",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1E293B)
                )
            }
        }

        // Summary details card
        item {
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = BorderStroke(1.dp, Color(0xFFE2E8F0))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "Inspección #${inspection.id.takeLast(6)}",
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF64748B)
                        )
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(if (inspection.completed) Color(0xFFD1FAE5) else Color(0xFFFEF3C7))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = if (inspection.completed) "Completada" else "En Progreso",
                                color = if (inspection.completed) Color(0xFF065F46) else Color(0xFFD97706),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Vehículo", fontWeight = FontWeight.Bold, color = Color(0xFF0066CC))
                            Text(vehicle?.name ?: "Desconocido", color = Color(0xFF1E293B))
                            Text("Placa: ${vehicle?.licensePlate ?: "N/A"}", fontWeight = FontWeight.SemiBold)
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Inspector", fontWeight = FontWeight.Bold, color = Color(0xFF00A86B))
                            Text(inspection.inspector, color = Color(0xFF1E293B))
                            Text("Fecha: ${inspection.date}", color = Color(0xFF64748B), fontSize = 12.sp)
                        }
                    }
                }
            }
        }

        // Score Card totalizers
        item {
            val total = inspection.items.size
            val passed = inspection.items.count { it.status == "pass" }
            val failed = inspection.items.count { it.status == "fail" }
            val unreviewed = inspection.items.count { it.status == "not-checked" }

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                ScoreCard(title = "Correcto", value = passed.toString(), color = Color(0xFF10B981), modifier = Modifier.weight(1f))
                ScoreCard(title = "Fallas", value = failed.toString(), color = Color(0xFFEF4444), modifier = Modifier.weight(1f))
                ScoreCard(title = "Sobrantes", value = unreviewed.toString(), color = Color.Gray, modifier = Modifier.weight(1f))
            }
        }

        // Action Print sharing button at top for accessibility
        item {
            Button(
                onClick = {
                    vehicle?.let { viewModel.generateAndShareInspectionPdf(inspection, it) }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("detail_share_pdf"),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0066CC))
            ) {
                Icon(Icons.Default.Share, contentDescription = null)
                Spacer(modifier = Modifier.width(6.dp))
                Text("Compartir o Imprimir PDF Compacto", fontWeight = FontWeight.Bold)
            }
        }

        // Failure items list (The compact focus requested by user!)
        val fails = inspection.items.filter { it.status == "fail" }
        item {
            Text(
                text = "Hallazgos Reportados (${fails.size})",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF1E293B)
            )
        }

        if (fails.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    colors = CardDefaults.cardColors(containerColor = LightGreenBg)
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Check, contentDescription = null, tint = Color(0xFF065F46))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Excelente: No se detectaron fallos críticos o anomalías.",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF065F46)
                        )
                    }
                }
            }
        } else {
            items(fails) { failItem ->
                Card(
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, Color(0xFFFCA5A5)),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFEF2F2))
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Warning, contentDescription = null, tint = Color(0xFFEF4444))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = failItem.name,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF1E293B)
                            )
                        }
                        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = Color(0xFFFCA5A5))
                        Text(
                            text = "Observación/Fallo:",
                            fontWeight = FontWeight.SemiBold,
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFF991B1B)
                        )
                        Text(
                            text = failItem.notes ?: "Sin observación adicional",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF1E293B)
                        )
                    }
                }
            }
        }

        // Sub-checklist logs collapsed view
        item {
            CategoryHeading(title = "Listado de Parámetros Completado", subtitle = "Auditoría de todos los ítems revisados")
        }

        items(inspection.items) { item ->
            Card(
                shape = RoundedCornerShape(8.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = BorderStroke(1.dp, Color(0xFFF1F5F9))
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = item.name,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF1E293B),
                        modifier = Modifier.weight(1f)
                    )
                    
                    val bColor: Color
                    val bText: String
                    when (item.status) {
                        "pass" -> {
                            bColor = Color(0xFF10B981)
                            bText = "Pása"
                        }
                        "fail" -> {
                            bColor = Color(0xFFEF4444)
                            bText = "Falla"
                        }
                        else -> {
                            bColor = Color.Gray
                            bText = "S/R"
                        }
                    }

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(bColor.copy(alpha = 0.15f))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = bText,
                            color = bColor,
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                }
            }
        }
        
        // Assistant panel values
        item {
            if (!inspection.nombreAsistente.isNullOrBlank()) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Entregado y Firmado por:",
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF1E293B)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(text = "Nombre: ${inspection.nombreAsistente}", fontSize = 14.sp)
                        Text(text = "Cargo: ${inspection.cargoAsistente ?: "Conductor"}", fontSize = 13.sp, color = Color.Gray)
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
fun ScoreCard(title: String, value: String, color: Color, modifier: Modifier = Modifier) {
    Card(
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.12f)),
        border = BorderStroke(1.dp, color.copy(alpha = 0.3f)),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(title, style = MaterialTheme.typography.labelSmall, color = color, fontWeight = FontWeight.Bold)
            Text(value, style = MaterialTheme.typography.titleLarge, color = color, fontWeight = FontWeight.Bold)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VehiclesManagementScreen(viewModel: MainViewModel) {
    val vehicles by viewModel.vehicles.collectAsStateWithLifecycle()

    var nameInput by remember { mutableStateOf("") }
    var plateInput by remember { mutableStateOf("") }
    var typeInput by remember { mutableStateOf("") }
    var soatInput by remember { mutableStateOf("") }
    var rtmInput by remember { mutableStateOf("") }

    var isEditing by remember { mutableStateOf<Vehicle?>(null) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                text = "Gestión de Flota",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF1E293B)
            )
        }

        // Add / Edit Entry form
        item {
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = BorderStroke(1.dp, Color(0xFFE2E8F0))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = if (isEditing == null) "Agregar Nuevo Vehículo" else "Editar Vehículo",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium,
                        color = Color(0xFF0066CC)
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = nameInput,
                        onValueChange = { nameInput = it },
                        label = { Text("Nombre del Vehículo *") },
                        placeholder = { Text("Ej: Volqueta Chevrolet") },
                        modifier = Modifier.fillMaxWidth().testTag("vehicle_name_input"),
                        singleLine = true
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = plateInput,
                        onValueChange = { plateInput = it.uppercase() },
                        label = { Text("Patente / Placa *") },
                        placeholder = { Text("Ej: LHJ747") },
                        modifier = Modifier.fillMaxWidth().testTag("vehicle_plate_input"),
                        singleLine = true
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = typeInput,
                        onValueChange = { typeInput = it },
                        label = { Text("Tipo / Categoría *") },
                        placeholder = { Text("Ej: Compactador / Volqueta") },
                        modifier = Modifier.fillMaxWidth().testTag("vehicle_type_input"),
                        singleLine = true
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = soatInput,
                        onValueChange = { soatInput = it },
                        label = { Text("Vencimiento SOAT (AAAA-MM-DD)") },
                        placeholder = { Text("Ej: 2026-09-30") },
                        modifier = Modifier.fillMaxWidth().testTag("vehicle_soat_input"),
                        singleLine = true
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = rtmInput,
                        onValueChange = { rtmInput = it },
                        label = { Text("Vencimiento Técnico-Mecánica (AAAA-MM-DD)") },
                        placeholder = { Text("Ej: 2026-06-19") },
                        modifier = Modifier.fillMaxWidth().testTag("vehicle_rtm_input"),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Button(
                            onClick = {
                                if (nameInput.isBlank() || plateInput.isBlank() || typeInput.isBlank()) {
                                    return@Button
                                }
                                val id = isEditing?.id ?: System.currentTimeMillis().toString()
                                val vehicle = Vehicle(
                                    id = id,
                                    name = nameInput,
                                    licensePlate = plateInput,
                                    type = typeInput,
                                    soatExpiry = soatInput.takeIf { it.isNotBlank() },
                                    rtmExpiry = rtmInput.takeIf { it.isNotBlank() }
                                )
                                viewModel.saveVehicle(vehicle)
                                
                                // Reset inputs
                                nameInput = ""
                                plateInput = ""
                                typeInput = ""
                                soatInput = ""
                                rtmInput = ""
                                isEditing = null
                            },
                            modifier = Modifier.weight(1f).testTag("save_vehicle_done"),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0066CC))
                        ) {
                            Text(if (isEditing == null) "Agregar" else "Guardar")
                        }

                        if (isEditing != null) {
                            OutlinedButton(
                                onClick = {
                                    isEditing = null
                                    nameInput = ""
                                    plateInput = ""
                                    typeInput = ""
                                    soatInput = ""
                                    rtmInput = ""
                                },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Cancelar")
                            }
                        }
                    }
                }
            }
        }

        // Vehicles listed grid table
        item {
            Text(
                text = "Vehículos Registrados (${vehicles.size})",
                fontWeight = FontWeight.Bold,
                color = Color(0xFF1E293B)
            )
        }

        items(vehicles) { vehicle ->
            Card(
                shape = RoundedCornerShape(10.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = vehicle.name,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF1E293B)
                        )
                        Text(
                            text = "Placa: ${vehicle.licensePlate} | Tipo: ${vehicle.type}",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF64748B)
                        )
                    }

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        IconButton(onClick = {
                            isEditing = vehicle
                            nameInput = vehicle.name
                            plateInput = vehicle.licensePlate
                            typeInput = vehicle.type
                            soatInput = vehicle.soatExpiry ?: ""
                            rtmInput = vehicle.rtmExpiry ?: ""
                        }, modifier = Modifier.testTag("edit_vehicle_${vehicle.id}")) {
                            Icon(Icons.Default.Edit, contentDescription = "Editar", tint = Color(0xFF0066CC))
                        }
                        
                        IconButton(onClick = {
                            viewModel.deleteVehicle(vehicle)
                        }, modifier = Modifier.testTag("delete_vehicle_${vehicle.id}")) {
                            Icon(Icons.Default.Delete, contentDescription = "Eliminar", tint = Color(0xFFEF4444))
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun DocumentsMonitorScreen(viewModel: MainViewModel) {
    val vehicles by viewModel.vehicles.collectAsStateWithLifecycle()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                text = "Vencimientos SOAT & RTM",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF1E293B)
            )
            Text(
                text = "Supervise la vigencia preoperacional legal de la flota de Segovia Aseo.",
                style = MaterialTheme.typography.bodySmall,
                color = Color.Gray
            )
        }

        if (vehicles.isEmpty()) {
            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "Registre vehículos para monitorear sus documentos.",
                        modifier = Modifier.padding(16.dp),
                        color = Color.Gray
                    )
                }
            }
        } else {
            items(vehicles) { vehicle ->
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column {
                                Text(
                                    text = vehicle.name,
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.titleMedium,
                                    color = Color(0xFF1E293B)
                                )
                                Text(
                                    text = "Patente: ${vehicle.licensePlate}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = Color(0xFF0066CC),
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color(0xFFF1F5F9))
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = vehicle.type,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp,
                                    color = Color(0xFF475569)
                                )
                            }
                        }

                        HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = Color(0xFFF1F5F9))

                        // Compact side-by-side compliance indicators (Optimized for small mobile displays!)
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            // SOAT box info
                            val soatColor = getDocumentStatusColor(vehicle.soatExpiry)
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .border(1.5.dp, soatColor.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                                    .background(soatColor.copy(alpha = 0.08f))
                                    .padding(10.dp)
                            ) {
                                Column {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.Lock, contentDescription = null, tint = soatColor, modifier = Modifier.size(13.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("SOAT", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = soatColor)
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = vehicle.soatExpiry ?: "Por registrar",
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 12.sp,
                                        color = Color(0xFF1E293B)
                                    )
                                    Text(
                                        text = formatDaysLeft(vehicle.soatExpiry),
                                        fontSize = 10.sp,
                                        color = soatColor,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }

                            // RTM box info
                            val rtmColor = getDocumentStatusColor(vehicle.rtmExpiry)
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .border(1.5.dp, rtmColor.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                                    .background(rtmColor.copy(alpha = 0.08f))
                                    .padding(10.dp)
                            ) {
                                Column {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.Build, contentDescription = null, tint = rtmColor, modifier = Modifier.size(13.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("RTM", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = rtmColor)
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = vehicle.rtmExpiry ?: "Por registrar",
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 12.sp,
                                        color = Color(0xFF1E293B)
                                    )
                                    Text(
                                        text = formatDaysLeft(vehicle.rtmExpiry),
                                        fontSize = 10.sp,
                                        color = rtmColor,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CloudSettingsScreen(viewModel: MainViewModel) {
    val serverUrl by viewModel.serverUrl.collectAsStateWithLifecycle()
    var urlInput by remember { mutableStateOf(serverUrl) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                IconButton(onClick = { viewModel.navigateTo(AppView.LIST) }) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Configuración Nube",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1E293B)
                )
            }
        }

        item {
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = BorderStroke(1.dp, Color(0xFFE2E8F0))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Base de Datos de Segovia Aseo",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium,
                        color = Color(0xFF0066CC)
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Establezca la dirección del servidor de su oficina o sistema central web. " +
                                "La aplicación realizará envíos de los informes en JSON de manera automatizada tras cada chequeo.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray
                    )
                    Spacer(modifier = Modifier.height(14.dp))

                    OutlinedTextField(
                        value = urlInput,
                        onValueChange = { urlInput = it },
                        label = { Text("URL de Servidor Nube / Webhook") },
                        placeholder = { Text("Ej: https://miservidor.com/api/") },
                        leadingIcon = { Icon(Icons.Default.Share, contentDescription = null) },
                        modifier = Modifier.fillMaxWidth().testTag("server_url_input"),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Button(
                        onClick = {
                            viewModel.setServerUrl(urlInput)
                            viewModel.performDatabaseCloudSync()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0066CC)),
                        modifier = Modifier.fillMaxWidth().testTag("save_server_url")
                    ) {
                        Text("Guardar y Probar Conexión")
                    }
                }
            }
        }

        item {
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = BorderStroke(1.dp, Color(0xFFE2E8F0))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Estado de Sincronización Automática",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleSmall,
                        color = Color(0xFF1F2937)
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = if (serverUrl.isEmpty()) {
                            "Modo Demostración Activo. Los reportes se almacenan con seguridad local (Room Database) " +
                            "y simulan envios exitosos a la base de datos de Segovia Aseo."
                        } else {
                            "Conectado a la API: $serverUrl"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = if (serverUrl.isEmpty()) Color(0xFF7C2D12) else Color(0xFF065F46)
                    )
                }
            }
        }
    }
}
