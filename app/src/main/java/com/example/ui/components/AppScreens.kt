package com.example.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.foundation.border
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.geometry.Offset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.model.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import android.print.PrintManager
import android.webkit.WebView
import android.content.Context
import android.content.Intent
import com.example.ui.UserRole
import com.example.ui.MonthlyReport
import com.example.ui.TabunganViewModel
import java.util.*
import java.text.SimpleDateFormat
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.platform.LocalContext

fun printReceiptText(context: Context, docName: String, plainText: String) {
    try {
        val printManager = context.getSystemService(Context.PRINT_SERVICE) as PrintManager
        val webView = WebView(context)
        
        val formattedHtmlText = plainText
            .replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace(" ", "&nbsp;")
            .replace("\n", "<br>")
            
        val htmlContent = """
            <html>
            <head>
            <style>
                body {
                    font-family: 'Courier New', Courier, monospace;
                    font-size: 14px;
                    line-height: 1.3;
                    margin: 5% 5% 5% 5%;
                    color: #000000;
                }
            </style>
            </head>
            <body>$formattedHtmlText</body>
            </html>
        """.trimIndent()
        
        webView.loadDataWithBaseURL(null, htmlContent, "text/html", "UTF-8", null)
        val printAdapter = webView.createPrintDocumentAdapter(docName)
        printManager.print(docName, printAdapter, android.print.PrintAttributes.Builder().build())
    } catch (e: Exception) {
        e.printStackTrace()
    }
}

fun shareReceiptText(context: Context, subject: String, body: String) {
    try {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, subject)
            putExtra(Intent.EXTRA_TEXT, body)
        }
        context.startActivity(Intent.createChooser(intent, "Bagikan Bukti Transaksi"))
    } catch (e: Exception) {
        e.printStackTrace()
    }
}

enum class AppTab(val title: String, val icon: androidx.compose.ui.graphics.vector.ImageVector) {
    DASHBOARD("Dasbor", Icons.Default.Home),
    SISWA("Nasabah", Icons.Default.Person),
    TABUNGAN("Tabungan", Icons.Default.AccountBalanceWallet),
    KAS_UTAMA("Kas Utama", Icons.Default.Assessment),
    SETOR_TABUNGAN("Setor Tabungan", Icons.Default.Send),
    LOAN_SCHOOL("P. Sekolah", Icons.Default.School),
    LOAN_TEACHER("P. Guru", Icons.Default.SupervisedUserCircle)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainAppScreen(viewModel: TabunganViewModel) {
    val context = LocalContext.current
    val currentRole by viewModel.currentRole.collectAsState()
    var currentTab by remember { mutableStateOf(AppTab.DASHBOARD) }
    val notifications by viewModel.notifications.collectAsState()
    var showChangePasswordDialog by remember { mutableStateOf(false) }
    var showThemeDialog by remember { mutableStateOf(false) }
    var showAboutDialog by remember { mutableStateOf(false) }
    var showResetDatabaseDialog by remember { mutableStateOf(false) }
    var showResetSuccessDialog by remember { mutableStateOf(false) }
    var showExportSuccess by remember { mutableStateOf(false) }
    var showExportError by remember { mutableStateOf(false) }
    var showIdentityDialog by remember { mutableStateOf(false) }

    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/csv")
    ) { uri ->
        uri?.let {
            val success = viewModel.writeCsvToUri(context, uri)
            if (success) {
                showExportSuccess = true
            } else {
                showExportError = true
            }
        }
    }

    val allowedTabs = remember(currentRole) {
        when (currentRole) {
            UserRole.BENDAHARA -> AppTab.values().toList()
            UserRole.ADMIN -> listOf(AppTab.SISWA, AppTab.TABUNGAN)
            UserRole.KEPALA -> listOf(AppTab.DASHBOARD, AppTab.KAS_UTAMA, AppTab.TABUNGAN, AppTab.SETOR_TABUNGAN)
            null -> emptyList()
        }
    }

    LaunchedEffect(currentRole) {
        if (currentRole != null) {
            if (currentTab !in allowedTabs && allowedTabs.isNotEmpty()) {
                currentTab = allowedTabs.first()
            }
        }
    }

    if (currentRole == null) {
        LoginScreen(viewModel = viewModel)
    } else {
        Scaffold(
            topBar = {
                Column {
                    TopAppBar(
                        title = {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AccountBalance,
                                    contentDescription = "Bank",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(32.dp)
                                )
                                Text(
                                    "Tabungan Siswa",
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.titleMedium
                                )
                            }
                        },
                        actions = {
                            var menuExpanded by remember { mutableStateOf(false) }

                            AssistChip(
                                onClick = { menuExpanded = true },
                                label = { Text(currentRole?.label ?: "") },
                                leadingIcon = {
                                    val rIcon = when (currentRole) {
                                        UserRole.BENDAHARA -> Icons.Default.AdminPanelSettings
                                        UserRole.ADMIN -> Icons.Default.AssignmentInd
                                        UserRole.KEPALA -> Icons.Default.SupervisorAccount
                                        else -> Icons.Default.Person
                                    }
                                    Icon(rIcon, contentDescription = null, modifier = Modifier.size(16.dp))
                                },
                                modifier = Modifier.padding(end = 4.dp)
                            )

                            IconButton(onClick = { menuExpanded = true }) {
                                Icon(Icons.Default.MoreVert, contentDescription = "Menu Opsi")
                            }

                             DropdownMenu(
                                expanded = menuExpanded,
                                onDismissRequest = { menuExpanded = false }
                            ) {
                                DropdownMenuItem(
                                    text = { Text("Cetak/Ekspor Excel") },
                                    leadingIcon = { Icon(Icons.Default.Description, contentDescription = null, modifier = Modifier.size(18.dp), tint = Color(0xFF2E7D32)) },
                                    onClick = {
                                        menuExpanded = false
                                        exportLauncher.launch("Laporan_Tabungan_Siswa_${System.currentTimeMillis()}.csv")
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Ganti Password") },
                                    leadingIcon = { Icon(Icons.Default.LockReset, contentDescription = null, modifier = Modifier.size(18.dp)) },
                                    onClick = {
                                        menuExpanded = false
                                        showChangePasswordDialog = true
                                    }
                                )
                                if (currentRole == UserRole.BENDAHARA) {
                                    DropdownMenuItem(
                                        text = { Text("Identitas Sekolah & Bendahara") },
                                        leadingIcon = { Icon(Icons.Default.ManageAccounts, contentDescription = null, modifier = Modifier.size(18.dp), tint = Color(0xFFE65100)) },
                                        onClick = {
                                            menuExpanded = false
                                            showIdentityDialog = true
                                        }
                                    )
                                }
                                DropdownMenuItem(
                                    text = { Text("Tema Tampilan (Mata)") },
                                    leadingIcon = { Icon(Icons.Default.Palette, contentDescription = null, modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.primary) },
                                    onClick = {
                                        menuExpanded = false
                                        showThemeDialog = true
                                     }
                                 )
                                DropdownMenuItem(
                                    text = { Text("Tentang Aplikasi & Pengembang") },
                                    leadingIcon = { Icon(Icons.Default.Info, contentDescription = null, modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.primary) },
                                    onClick = {
                                        menuExpanded = false
                                        showAboutDialog = true
                                    }
                                )
                                if (currentRole == UserRole.BENDAHARA) {
                                    DropdownMenuItem(
                                        text = { Text("Mulai Tugas Baru (Reset Data)") },
                                        leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.error) },
                                        onClick = {
                                            menuExpanded = false
                                            showResetDatabaseDialog = true
                                        }
                                    )
                                }
                                DropdownMenuItem(
                                    text = { Text("Keluar (Logout)") },
                                    leadingIcon = { Icon(Icons.Default.ExitToApp, contentDescription = null, modifier = Modifier.size(18.dp)) },
                                    onClick = {
                                        menuExpanded = false
                                        viewModel.logout()
                                    }
                                )
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp)
                        )
                    )

                    // In-app notifications banner
                    AnimatedVisibility(
                        visible = notifications.isNotEmpty(),
                        enter = slideInVertically() + fadeIn(),
                        exit = slideOutVertically() + fadeOut()
                    ) {
                        notifications.firstOrNull()?.let { notif ->
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(8.dp)
                                    .testTag("notification_banner"),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                                ),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .padding(12.dp)
                                        .fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.NotificationsActive,
                                        contentDescription = "Notifikasi",
                                        modifier = Modifier.size(28.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(notif.title, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                        Text(notif.message, fontSize = 12.sp)
                                    }
                                    IconButton(
                                        onClick = { viewModel.removeNotification(notif.id) },
                                        modifier = Modifier.testTag("dismiss_notification_button")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Close,
                                            contentDescription = "Tutup",
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            },
            bottomBar = {
                NavigationBar(
                    modifier = Modifier.windowInsetsPadding(WindowInsets.navigationBars)
                ) {
                    allowedTabs.forEach { tab ->
                        NavigationBarItem(
                            selected = currentTab == tab,
                            onClick = { currentTab = tab },
                            icon = { Icon(tab.icon, contentDescription = tab.title) },
                            label = {
                                Text(
                                    text = tab.title,
                                    fontSize = if (allowedTabs.size > 5) 8.5.sp else 10.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    maxLines = 1,
                                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                                    softWrap = false
                                )
                            },
                            alwaysShowLabel = allowedTabs.size <= 4,
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = MaterialTheme.colorScheme.primary,
                                selectedTextColor = MaterialTheme.colorScheme.primary,
                                indicatorColor = MaterialTheme.colorScheme.primaryContainer
                            ),
                            modifier = Modifier.testTag("tab_item_${tab.name.lowercase()}")
                        )
                    }
                }
            }
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                when (currentTab) {
                    AppTab.DASHBOARD -> DashboardScreen(viewModel, onNavigateToTab = { currentTab = it })
                    AppTab.SISWA -> SiswaScreen(viewModel)
                    AppTab.TABUNGAN -> TabunganScreen(viewModel)
                    AppTab.KAS_UTAMA -> KasUtamaScreen(viewModel)
                    AppTab.SETOR_TABUNGAN -> SetorTabunganScreen(viewModel)
                    AppTab.LOAN_SCHOOL -> SchoolLoansScreen(viewModel)
                    AppTab.LOAN_TEACHER -> TeacherLoansScreen(viewModel)
                }
            }
        }

        // Dialog Tampilan / Tema
        if (showThemeDialog) {
            val themePrefState by viewModel.themePreference.collectAsState()
            AlertDialog(
                onDismissRequest = { showThemeDialog = false },
                title = { Text("Pilih Tema Tampilan", fontWeight = FontWeight.Bold) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            "Pilih mode tampilan aplikasi sesuai kenyamanan Anda:",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.outline
                        )
                        
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { viewModel.updateThemePreference("LIGHT") }
                                .padding(vertical = 8.dp)
                        ) {
                            RadioButton(
                                selected = themePrefState == "LIGHT",
                                onClick = { viewModel.updateThemePreference("LIGHT") }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Mode Terang (Biasa)")
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { viewModel.updateThemePreference("DARK") }
                                .padding(vertical = 8.dp)
                        ) {
                            RadioButton(
                                selected = themePrefState == "DARK",
                                onClick = { viewModel.updateThemePreference("DARK") }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Mode Gelap (Mata Nyaman)")
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { viewModel.updateThemePreference("SYSTEM") }
                                .padding(vertical = 8.dp)
                        ) {
                            RadioButton(
                                selected = themePrefState == "SYSTEM",
                                onClick = { viewModel.updateThemePreference("SYSTEM") }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Otomatis (Sesuai Sistem)")
                        }
                    }
                },
                confirmButton = {
                    Button(onClick = { showThemeDialog = false }) {
                        Text("Selesai")
                    }
                }
            )
        }

        // Dialog Ganti Password
        if (showChangePasswordDialog) {
            val role = currentRole!!
            var oldPwd by remember { mutableStateOf("") }
            var newPwd by remember { mutableStateOf("") }
            var changeError by remember { mutableStateOf("") }
            var changeSuccess by remember { mutableStateOf("") }

            AlertDialog(
                onDismissRequest = { showChangePasswordDialog = false },
                title = { Text("Ganti Password - ${role.label}", fontWeight = FontWeight.Bold) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(
                            "Simpan password rahasia baru untuk akses ${role.label}.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.outline
                        )

                        OutlinedTextField(
                            value = oldPwd,
                            onValueChange = { oldPwd = it },
                            label = { Text("Password Lama") },
                            visualTransformation = PasswordVisualTransformation(),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )

                        OutlinedTextField(
                            value = newPwd,
                            onValueChange = { newPwd = it },
                            label = { Text("Password Baru (Rahasia)") },
                            visualTransformation = PasswordVisualTransformation(),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )

                        if (changeError.isNotEmpty()) {
                            Text(changeError, color = MaterialTheme.colorScheme.error, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }

                        if (changeSuccess.isNotEmpty()) {
                            Text(changeSuccess, color = Color(0xFF2E7D32), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            if (oldPwd.isBlank() || newPwd.isBlank()) {
                                changeError = "Sandi lama dan baru wajib diisi!"
                            } else if (viewModel.updatePassword(role, oldPwd.trim(), newPwd.trim())) {
                                changeSuccess = "Password berhasil diperbarui secara rahasia!"
                                changeError = ""
                                oldPwd = ""
                                newPwd = ""
                            } else {
                                changeError = "Password lama tidak cocok!"
                                changeSuccess = ""
                            }
                        }
                    ) {
                        Text("Simpan")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showChangePasswordDialog = false }) {
                        Text("Tutup")
                    }
                }
            )
        }

        if (showExportSuccess) {
            AlertDialog(
                onDismissRequest = { showExportSuccess = false },
                title = { Text("Ekspor Berhasil", fontWeight = FontWeight.Bold) },
                text = { Text("Daftar nasabah beserta seluruh riwayat transaksi mereka berhasil disimpan sebagai file Excel (.csv) di perangkat Anda.") },
                confirmButton = {
                    Button(onClick = { showExportSuccess = false }) {
                        Text("Ok / Selesai")
                    }
                }
            )
        }

        if (showExportError) {
            AlertDialog(
                onDismissRequest = { showExportError = false },
                title = { Text("Ekspor Gagal", fontWeight = FontWeight.Bold) },
                text = { Text("Terjadi kesalahan saat mengekspor data Excel.") },
                confirmButton = {
                    Button(onClick = { showExportError = false }) {
                        Text("Tutup")
                    }
                }
            )
        }

        if (showIdentityDialog) {
            val sNameFlow by viewModel.schoolName.collectAsState()
            val tNameFlow by viewModel.treasurerName.collectAsState()
            var sNameInput by remember { mutableStateOf(sNameFlow) }
            var tNameInput by remember { mutableStateOf(tNameFlow) }
            var saveSuccessMessage by remember { mutableStateOf("") }

            AlertDialog(
                onDismissRequest = { showIdentityDialog = false },
                title = { Text("Pengaturan Identitas", fontWeight = FontWeight.Bold) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(
                            "Atur identitas Sekolah dan Bendahara secara fleksibel guna dicantumkan pada seluruh lembar laporan cetak (kuitansi) serta ringkasan di dashboard.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.outline
                        )

                        OutlinedTextField(
                            value = sNameInput,
                            onValueChange = { sNameInput = it },
                            label = { Text("Nama Sekolah") },
                            placeholder = { Text("Contoh: MIS CIBUNGUR I") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )

                        OutlinedTextField(
                            value = tNameInput,
                            onValueChange = { tNameInput = it },
                            label = { Text("Nama Bendahara") },
                            placeholder = { Text("Contoh: Cepi Sopyan, S.Pd.I") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )

                        if (saveSuccessMessage.isNotEmpty()) {
                            Text(
                                text = saveSuccessMessage,
                                color = if (saveSuccessMessage.contains("berhasil")) Color(0xFF2E7D32) else MaterialTheme.colorScheme.error,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            if (sNameInput.isNotBlank() && tNameInput.isNotBlank()) {
                                viewModel.updateIdentitySettings(sNameInput, tNameInput)
                                saveSuccessMessage = "Identitas berhasil disimpan & diperbarui!"
                            } else {
                                saveSuccessMessage = "Nama sekolah & bendahara tidak boleh kosong!"
                            }
                        }
                    ) {
                        Text("Simpan")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showIdentityDialog = false }) {
                        Text("Tutup")
                    }
                }
            )
        }

        if (showAboutDialog) {
            AlertDialog(
                onDismissRequest = { showAboutDialog = false },
                icon = {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = "Tentang Aplikasi",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(36.dp)
                    )
                },
                title = { 
                    Text(
                        "Tentang Aplikasi", 
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleLarge
                    ) 
                },
                text = {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            "Aplikasi Tabungan Siswa",
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 18.sp,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            "Sistem Manajemen Keuangan & Tabungan Terintegrasi Sekolah.",
                            fontSize = 13.sp,
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                "Dikembangkan Oleh:",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.outline
                            )
                            Text(
                                "Cepi Sopyan",
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        Text(
                            "Versi 1.0.0",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                },
                confirmButton = {
                    Button(onClick = { showAboutDialog = false }) {
                        Text("Tutup Info")
                    }
                }
            )
        }

        if (showResetDatabaseDialog) {
            AlertDialog(
                onDismissRequest = { showResetDatabaseDialog = false },
                icon = {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = "Peringatan Wape Out",
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(40.dp)
                    )
                },
                title = {
                    Text(
                        "Mulai Tugas / Tahun Baru?",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.error
                    )
                },
                text = {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            "Tindakan ini akan menghapus seluruh data secara PERMANEN dari aplikasi:",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Column(
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                            modifier = Modifier.padding(start = 8.dp)
                        ) {
                            Text("• Data Seluruh Siswa & Kelas", fontSize = 12.sp, color = Color.Gray)
                            Text("• Riwayat Transaksi Setor & Tarik", fontSize = 12.sp, color = Color.Gray)
                            Text("• Pinjaman Pengeluran Sekolah", fontSize = 12.sp, color = Color.Gray)
                            Text("• Pinjaman / Angsuran Guru & Staf", fontSize = 12.sp, color = Color.Gray)
                            Text("• Saldo Modal & Catatan Kas Utama", fontSize = 12.sp, color = Color.Gray)
                        }
                        
                        Box(
                            modifier = Modifier
                                .background(
                                    MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
                                    RoundedCornerShape(8.dp)
                                )
                                .padding(10.dp)
                        ) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.Top
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Info,
                                    contentDescription = "Info Cadangan",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(18.dp)
                                )
                                Text(
                                    "REKOMENDASI CADANGAN:\nSilakan lakukan 'Cetak/Ekspor Excel' terlebih dahulu melalui menu opsi di kanan atas sebagai bukti arsip fisik sebelum melakukan pembersihan ini.",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                        }

                        Text(
                            "Apakah Anda yakin ingin menghapus data dan siap memulai lembar kerja baru?",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            viewModel.clearAllDatabase {
                                showResetDatabaseDialog = false
                                showResetSuccessDialog = true
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error
                        )
                    ) {
                        Text("Ya, Hapus Semua & Reset", fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showResetDatabaseDialog = false }) {
                        Text("Batal")
                    }
                }
            )
        }

        if (showResetSuccessDialog) {
            AlertDialog(
                onDismissRequest = { showResetSuccessDialog = false },
                icon = {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = "Success reset",
                        tint = Color(0xFF2E7D32),
                        modifier = Modifier.size(40.dp)
                    )
                },
                title = {
                    Text(
                        "Pembersihan Selesai!",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleLarge,
                        color = Color(0xFF2E7D32)
                    )
                },
                text = {
                    Text(
                        "Selamat! Seluruh database lokal aplikasi telah berhasil dikosongkan secara total.\n\nAplikasi sekarang sudah bersih dan siap digunakan kembali untuk memulai pencatatan tugas atau tahun ajaran baru.",
                        fontSize = 13.sp,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                },
                confirmButton = {
                    Button(
                        onClick = { showResetSuccessDialog = false },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32))
                    ) {
                        Text("Mulai Lembar Baru")
                    }
                }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(viewModel: TabunganViewModel) {
    var selectedRole by remember { mutableStateOf<UserRole?>(null) }
    var passwordInput by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f),
                        MaterialTheme.colorScheme.background
                    )
                )
            )
            .windowInsetsPadding(WindowInsets.systemBars),
        contentAlignment = Alignment.Center
    ) {
        val currentPrefTheme by viewModel.themePreference.collectAsState()
        IconButton(
            onClick = {
                val next = when (currentPrefTheme) {
                    "SYSTEM" -> "LIGHT"
                    "LIGHT" -> "DARK"
                    else -> "SYSTEM"
                }
                viewModel.updateThemePreference(next)
            },
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(16.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Palette,
                contentDescription = "Ganti Tema",
                tint = MaterialTheme.colorScheme.primary
            )
        }

        Column(
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // App Logo
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.AccountBalance,
                    contentDescription = "Logo",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(48.dp)
                )
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    "Tabungan Siswa",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    "Sistem Otorisasi Multi-Akses Portal Keuangan",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline,
                    textAlign = TextAlign.Center
                )
            }

            if (selectedRole == null) {
                // Role Card Selector
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            "Pilih Peran Pengguna",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        UserRole.values().forEach { role ->
                            val rIcon = when (role) {
                                UserRole.BENDAHARA -> Icons.Default.Lock
                                UserRole.ADMIN -> Icons.Default.Assignment
                                UserRole.KEPALA -> Icons.Default.AccountBox
                            }
                            val cardColor = when (role) {
                                UserRole.BENDAHARA -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.15f)
                                UserRole.ADMIN -> MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.15f)
                                UserRole.KEPALA -> MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.15f)
                            }
                            val tintColor = when (role) {
                                UserRole.BENDAHARA -> MaterialTheme.colorScheme.primary
                                UserRole.ADMIN -> MaterialTheme.colorScheme.secondary
                                UserRole.KEPALA -> MaterialTheme.colorScheme.tertiary
                            }

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(cardColor)
                                    .clickable {
                                        selectedRole = role
                                        passwordInput = ""
                                        errorMessage = ""
                                    }
                                    .padding(14.dp)
                                    .testTag("role_select_${role.name.lowercase()}"),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(CircleShape)
                                        .background(tintColor.copy(alpha = 0.15f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = rIcon,
                                        contentDescription = null,
                                        tint = tintColor
                                    )
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        role.label,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        role.description,
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.outline
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        "Kata Sandi Bawaan: ${viewModel.getDefaultPassword(role)}",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }
                    }
                }
            } else {
                val role = selectedRole!!
                val rIcon = when (role) {
                    UserRole.BENDAHARA -> Icons.Default.Lock
                    UserRole.ADMIN -> Icons.Default.Assignment
                    UserRole.KEPALA -> Icons.Default.AccountBox
                }

                // Password Verification Form
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            IconButton(
                                onClick = { selectedRole = null },
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(Icons.Default.ArrowBack, contentDescription = "Kembali")
                            }
                            Icon(rIcon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp))
                            Text(
                                "Masuk: ${role.label}",
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.titleMedium
                            )
                        }

                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(
                                "Masukkan Password Rahasia:",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.outline
                            )
                            OutlinedTextField(
                                value = passwordInput,
                                onValueChange = { passwordInput = it },
                                placeholder = { Text("Kata sandi rahasia...") },
                                visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                                trailingIcon = {
                                    val icon = if (passwordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility
                                    IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                        Icon(icon, contentDescription = "Tampilkan Password")
                                    }
                                },
                                singleLine = true,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("password_input_field"),
                                shape = RoundedCornerShape(10.dp)
                            )
                        }

                        if (errorMessage.isNotEmpty()) {
                            Text(
                                errorMessage,
                                color = MaterialTheme.colorScheme.error,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }

                        Button(
                            onClick = {
                                if (viewModel.verifyPassword(role, passwordInput.trim())) {
                                    viewModel.login(role)
                                } else {
                                    errorMessage = "Kata sandi salah! Silahkan periksa sandi rahasia Anda."
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(46.dp)
                                .testTag("login_submit_button"),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("Verifikasi & Masuk", fontWeight = FontWeight.Bold)
                        }

                        // Sandbox / Initial Seed Guideline helper
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                                .padding(10.dp)
                        ) {
                            Text(
                                "⚠️ Info Sandi Bawaan (Dapat Diubah):",
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                "Kata sandi rahasia untuk ${role.label} adalah:\n\"${viewModel.getDefaultPassword(role)}\"",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Penyedia Layanan Finansial Sekolah",
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.8f),
                textAlign = TextAlign.Center
            )
            Text(
                text = "Dikembangkan oleh: Cepi Sopyan",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
fun DashboardScreen(viewModel: TabunganViewModel, onNavigateToTab: (AppTab) -> Unit) {
    val currentRole by viewModel.currentRole.collectAsState()
    val schoolName by viewModel.schoolName.collectAsState()
    val treasurerName by viewModel.treasurerName.collectAsState()
    val totalTabungan by viewModel.totalTabunganSiswa.collectAsState()
    val outSchool by viewModel.outstandingPinjamanSekolah.collectAsState()
    val outTeacher by viewModel.outstandingPinjamanGuru.collectAsState()
    val sumAdmin by viewModel.totalPendapatanAdmin.collectAsState()
    val realKas by viewModel.saldoKasRiil.collectAsState()
    val totalSetorKoperasi by viewModel.totalSetorKoperasi.collectAsState()
    val saldoBendahara by viewModel.saldoBendahara.collectAsState()
    val transList by viewModel.transaksiList.collectAsState()
    val siswaList by viewModel.siswaList.collectAsState()
    val adminKasList by viewModel.adminKasList.collectAsState()
    val pinjamanSekolahList by viewModel.pinjamanSekolahList.collectAsState()
    val pinjamanGuruList by viewModel.pinjamanGuruList.collectAsState()

    var txToDelete by remember { mutableStateOf<Transaksi?>(null) }

    if (txToDelete != null) {
        val tx = txToDelete!!
        val matchingSiswa = siswaList.find { it.id == tx.siswaId }
        AlertDialog(
            onDismissRequest = { txToDelete = null },
            title = { Text("Koreksi / Hapus Transaksi", fontWeight = FontWeight.Bold) },
            text = {
                Text("Apakah Anda yakin ingin menghapus data transaksi ini?\n\n" +
                        "Nasabah: ${matchingSiswa?.nama ?: "-"}\n" +
                        "Tipe: ${if (tx.tipe == "SETOR") "Setoran (+)" else "Penarikan (-)"}\n" +
                        "Jumlah: ${viewModel.formatRupiah(tx.jumlah)}\n\n" +
                        "Tindakan ini akan mengoreksi kembali saldo nasabah dan kas seketika.")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteTransaksi(tx.id)
                        txToDelete = null
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Hapus/Koreksi")
                }
            },
            dismissButton = {
                TextButton(onClick = { txToDelete = null }) {
                    Text("Batal")
                }
            }
        )
    }

    // Filter mutations for today's transactions
    val todayStart = Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis

    val todayMutations = remember(transList) {
        transList.filter { it.tanggal >= todayStart }
    }

    val todaySetor = remember(todayMutations) {
        todayMutations.filter { it.tipe == "SETOR" }.sumOf { it.jumlah }
    }
    val todayTarik = remember(todayMutations) {
        todayMutations.filter { it.tipe == "TARIK" }.sumOf { it.jumlah }
    }
    val todayBiayaAdmin = remember(todayMutations) {
        todayMutations.sumOf { it.biayaAdmin }
    }

    val todayAdminMovements = remember(adminKasList) {
        adminKasList.filter { it.tanggal >= todayStart }
    }
    val todayManualAdminKasIncome = remember(todayAdminMovements) {
        todayAdminMovements.filter {
            (it.tipe == "PENDAPATAN_ADMIN" && !it.keterangan.contains("Admin Transaksi", ignoreCase = true)) ||
            it.tipe == "MODAL_AWAL"
        }.sumOf { it.jumlah }
    }
    val todayKasKeluar = remember(todayAdminMovements) {
        todayAdminMovements.filter { it.tipe == "PENGELUARAN_OPERASIONAL" }.sumOf { it.jumlah }
    }

    val todaySchoolDisbursed = remember(pinjamanSekolahList) {
        pinjamanSekolahList.filter { it.tanggalPinjam >= todayStart }.sumOf { it.jumlahPinjam }
    }
    val todayTeacherDisbursed = remember(pinjamanGuruList) {
        pinjamanGuruList.filter { it.tanggalPinjam >= todayStart }.sumOf { it.jumlahPinjam }
    }
    val todayTotalDisbursed = todaySchoolDisbursed + todayTeacherDisbursed

    val todayTotalInflow = todaySetor + todayBiayaAdmin + todayManualAdminKasIncome
    val todayTotalOutflow = todayTarik + todayKasKeluar + todayTotalDisbursed
    val todayNetBalance = todayTotalInflow - todayTotalOutflow

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.primary),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.School,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = schoolName,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 15.sp,
                            color = MaterialTheme.colorScheme.primary,
                            letterSpacing = 0.5.sp
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(12.dp)
                            )
                            Text(
                                text = "Bendahara: $treasurerName",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }
        }

        if (currentRole == UserRole.KEPALA) {
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Default.Info, contentDescription = null, tint = MaterialTheme.colorScheme.tertiary)
                        Column {
                            Text(
                                "Akses Kepala Sekolah (Monitoring)",
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onTertiaryContainer
                            )
                            Text(
                                "Anda masuk dalam mode pemantauan data keuangan. Tombol input dan manipulasi dinonaktifkan.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.8f)
                            )
                        }
                    }
                }
            }
        }

        item {
            // Dashboard Welcome Card with gradient
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("welcome_card"),
                shape = RoundedCornerShape(16.dp)
            ) {
                Box(
                    modifier = Modifier
                        .background(
                            Brush.linearGradient(
                                colors = listOf(
                                    MaterialTheme.colorScheme.primary,
                                    MaterialTheme.colorScheme.secondary
                                )
                            )
                        )
                        .padding(20.dp)
                ) {
                    Column {
                        Text(
                            "Sisa Uang di Pegangan Bendahara",
                            color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f),
                            fontWeight = FontWeight.Medium,
                            fontSize = 14.sp
                        )
                        Text(
                            viewModel.formatRupiah(saldoBendahara),
                            color = MaterialTheme.colorScheme.onPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 28.sp,
                            modifier = Modifier.padding(vertical = 4.dp)
                        )
                        Divider(color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.3f), modifier = Modifier.padding(vertical = 8.dp))
                        Text(
                            "Estimasi dana tunai riil di pegangan bendahara (Kas Riil dikurangi total setor ke kepala/bank)",
                            color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f),
                            fontSize = 11.sp
                        )
                    }
                }
            }
        }

        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("saldo_hari_ini_card"),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                ),
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Header
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Event,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = "Saldo Hari Ini (Berjalan)",
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        
                        Text(
                            text = viewModel.formatDateShort(System.currentTimeMillis()),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }

                    Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.08f))

                    // 3 columns layout: Inflow, Outflow, Net Balance
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Inflow Column
                        Column(
                            modifier = Modifier.weight(1.1f),
                            horizontalAlignment = Alignment.Start
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.TrendingUp,
                                    contentDescription = null,
                                    tint = Color(0xFF2E7D32),
                                    modifier = Modifier.size(14.dp)
                                )
                                Text(
                                    text = "Keuangan Masuk",
                                    fontSize = 11.sp,
                                    color = Color.Gray,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = viewModel.formatRupiah(todayTotalInflow),
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = Color(0xFF2E7D32)
                            )
                        }

                        // Outflow Column
                        Column(
                            modifier = Modifier.weight(1.1f),
                            horizontalAlignment = Alignment.Start
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.TrendingDown,
                                    contentDescription = null,
                                    tint = Color(0xFFC62828),
                                    modifier = Modifier.size(14.dp)
                                )
                                Text(
                                    text = "Keuangan Keluar",
                                    fontSize = 11.sp,
                                    color = Color.Gray,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = viewModel.formatRupiah(todayTotalOutflow),
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = Color(0xFFC62828)
                            )
                        }

                        // Net Balance Column
                        Column(
                            modifier = Modifier.weight(1.2f),
                            horizontalAlignment = Alignment.End
                        ) {
                            Text(
                                text = "Hasil Bersih",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = (if (todayNetBalance >= 0) "+" else "") + viewModel.formatRupiah(todayNetBalance),
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 14.sp,
                                color = if (todayNetBalance >= 0) Color(0xFF2E7D32) else Color(0xFFC62828)
                            )
                        }
                    }

                    // A small helper notice
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                MaterialTheme.colorScheme.surface,
                                RoundedCornerShape(6.dp)
                            )
                            .padding(8.dp)
                    ) {
                        Text(
                            text = "💡 Menggambarkan total keuangan riil yang diperoleh pada tanggal berjalan (Setoran & Admin dikurangi Penarikan, Pengeluaran & Pinjaman Hari Ini).",
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.outline,
                            lineHeight = 13.sp
                        )
                    }
                }
            }
        }

        item {
            Text("Informasi Keuangan", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
        }

        item {
            // Grid flow of secondary data matrices details
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                    FinanceSummaryCard(
                        title = "Tabungan Siswa",
                        amount = totalTabungan,
                        subText = "${siswaList.size} Nasabah terdaftar",
                        icon = Icons.Default.People,
                        color = Color(0xFF2E7D32), // Emerald Green
                        modifier = Modifier.weight(1f).testTag("total_tabungan_card"),
                        onClick = {
                            if (currentRole == UserRole.BENDAHARA || currentRole == UserRole.ADMIN) {
                                onNavigateToTab(AppTab.SISWA)
                            }
                        }
                    )
                    FinanceSummaryCard(
                        title = "Saldo Kas Admin",
                        amount = sumAdmin,
                        subText = "Akumulasi biaya admin",
                        icon = Icons.Default.ManageAccounts,
                        color = Color(0xFF00695C), 
                        modifier = Modifier.weight(1f).testTag("saldo_admin_card")
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                    FinanceSummaryCard(
                        title = "Talangan Sekolah",
                        amount = outSchool,
                        subText = "Dipakai instansi sekolah",
                        icon = Icons.Default.School,
                        color = Color(0xFFC62828), // Soft Red
                        modifier = Modifier.weight(1f).testTag("talangan_sekolah_card"),
                        onClick = {
                            if (currentRole == UserRole.BENDAHARA) {
                                onNavigateToTab(AppTab.LOAN_SCHOOL)
                            }
                        }
                    )
                    FinanceSummaryCard(
                        title = "Pinjaman Guru",
                        amount = outTeacher,
                        subText = "Dipakai tenaga pengajar",
                        icon = Icons.Default.SupervisedUserCircle,
                        color = Color(0xFFD84315), // Deep Orange
                        modifier = Modifier.weight(1f).testTag("pinjaman_guru_card"),
                        onClick = {
                            if (currentRole == UserRole.BENDAHARA) {
                                onNavigateToTab(AppTab.LOAN_TEACHER)
                            }
                        }
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                    FinanceSummaryCard(
                        title = "Kas Riil Tabungan",
                        amount = realKas,
                        subText = "Total kekayaan sistem",
                        icon = Icons.Default.AccountBalance,
                        color = Color(0xFF1565C0), // Dark Blue
                        modifier = Modifier.weight(1f).testTag("kas_riil_overall_card")
                    )
                    FinanceSummaryCard(
                        title = "Disetor ke Bank/Kepala",
                        amount = totalSetorKoperasi,
                        subText = "Penyerahan tertulis",
                        icon = Icons.Default.Send,
                        color = Color(0xFF2E7D32), // Dark Green
                        modifier = Modifier.weight(1f).testTag("disetor_bank_kepala_card"),
                        onClick = {
                            if (currentRole == UserRole.BENDAHARA || currentRole == UserRole.KEPALA) {
                                onNavigateToTab(AppTab.SETOR_TABUNGAN)
                            }
                        }
                    )
                }
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Mutasi Harian Siswa",
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    "Hari Ini",
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary,
                    fontSize = 13.sp
                )
            }
        }

        if (todayMutations.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp).fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.LibraryBooks,
                            contentDescription = "Empty",
                            tint = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                            modifier = Modifier.size(40.dp)
                        )
                        Text(
                            "Belum ada pencatatan penarikan atau setoran siswa hari ini.",
                            textAlign = TextAlign.Center,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                }
            }
        } else {
            items(todayMutations) { mutation ->
                val matchingSiswa = siswaList.find { it.id == mutation.siswaId }
                MutationItem(
                    siswaName = matchingSiswa?.nama ?: "Siswa Terdaftar",
                    siswaClass = matchingSiswa?.kelas ?: "-",
                    mutation = mutation,
                    viewModel = viewModel,
                    onDeleteClick = { txToDelete = mutation }
                )
            }
        }
    }
}

@Composable
fun FinanceSummaryCard(
    title: String,
    amount: Double,
    subText: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null
) {
    Card(
        modifier = modifier.then(
            if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier
        ),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    title,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.outline
                )
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .clip(CircleShape)
                        .background(color.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = color,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
            Text(
                java.text.NumberFormat.getCurrencyInstance(Locale("id", "ID"))
                    .format(amount).replace("Rp", "Rp ").replace(",00", ""),
                fontSize = 16.sp,
                fontWeight = FontWeight.ExtraBold,
                color = color,
                modifier = Modifier.padding(vertical = 4.dp)
            )
            Text(
                subText,
                fontSize = 10.sp,
                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.8f)
            )
        }
    }
}

@Composable
fun MutationItem(
    siswaName: String,
    siswaClass: String,
    mutation: Transaksi,
    viewModel: TabunganViewModel,
    onDeleteClick: (() -> Unit)? = null
) {
    val schoolName by viewModel.schoolName.collectAsState()
    val treasurerName by viewModel.treasurerName.collectAsState()

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(12.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(
                            if (mutation.tipe == "SETOR") Color(0xFFE8F5E9) else Color(0xFFFFEBEE)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (mutation.tipe == "SETOR") Icons.Default.TrendingUp else Icons.Default.TrendingDown,
                        contentDescription = mutation.tipe,
                        tint = if (mutation.tipe == "SETOR") Color(0xFF2E7D32) else Color(0xFFC62828),
                        modifier = Modifier.size(18.dp)
                    )
                }

                Column {
                    Text(siswaName, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    Text("Kelas $siswaClass • ${viewModel.formatDate(mutation.tanggal)}", fontSize = 10.sp, color = MaterialTheme.colorScheme.outline)
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "${if (mutation.tipe == "SETOR") "+" else "-"} ${viewModel.formatRupiah(mutation.jumlah)}",
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 13.sp,
                        color = if (mutation.tipe == "SETOR") Color(0xFF2E7D32) else Color(0xFFC62828)
                    )
                    if (mutation.biayaAdmin > 0) {
                        Text("Admin: ${viewModel.formatRupiah(mutation.biayaAdmin)}", fontSize = 9.sp, color = MaterialTheme.colorScheme.outline)
                    }
                }
                
                val currentRole by viewModel.currentRole.collectAsState()
                if (onDeleteClick != null && (currentRole == UserRole.BENDAHARA || currentRole == UserRole.ADMIN)) {
                    IconButton(
                        onClick = onDeleteClick,
                        modifier = Modifier.size(32.dp).testTag("delete_mutation_${mutation.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Hapus Transaksi",
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SiswaScreen(viewModel: TabunganViewModel) {
    val context = LocalContext.current
    val siswaList by viewModel.siswaList.collectAsState()
    val balances by viewModel.siswaBalances.collectAsState()
    val transList by viewModel.transaksiList.collectAsState()

    var showLocalExportSuccess by remember { mutableStateOf(false) }
    var showLocalExportError by remember { mutableStateOf(false) }

    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/csv")
    ) { uri ->
        uri?.let {
            val success = viewModel.writeCsvToUri(context, uri)
            if (success) {
                showLocalExportSuccess = true
            } else {
                showLocalExportError = true
            }
        }
    }

    var searchQuery by remember { mutableStateOf("") }
    var expandRegForm by remember { mutableStateOf(false) }

    // Forms states
    var nameInput by remember { mutableStateOf("") }
    var classInput by remember { mutableStateOf("") }
    var regNumInput by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf("") }

    // Quick transaction states
    var selectedSiswaForTx by remember { mutableStateOf<Siswa?>(null) }
    var selectedSiswaForHistory by remember { mutableStateOf<Siswa?>(null) }

    val filteredSiswa = remember(siswaList, searchQuery) {
        if (searchQuery.isBlank()) {
            siswaList
        } else {
            siswaList.filter {
                it.nama.contains(searchQuery, ignoreCase = true) ||
                        it.kelas.contains(searchQuery, ignoreCase = true) ||
                        it.nomorInduk.contains(searchQuery, ignoreCase = true)
            }
        }
    }

    if (showLocalExportSuccess) {
        AlertDialog(
            onDismissRequest = { showLocalExportSuccess = false },
            title = { Text("Ekspor Berhasil", fontWeight = FontWeight.Bold) },
            text = { Text("Daftar nasabah beserta seluruh riwayat transaksi mereka berhasil disimpan sebagai file Excel (.csv) di perangkat Anda.") },
            confirmButton = {
                Button(onClick = { showLocalExportSuccess = false }) {
                    Text("Ok / Selesai")
                }
            }
        )
    }

    if (showLocalExportError) {
        AlertDialog(
            onDismissRequest = { showLocalExportError = false },
            title = { Text("Ekspor Gagal", fontWeight = FontWeight.Bold) },
            text = { Text("Terjadi kesalahan saat mengekspor data Excel.") },
            confirmButton = {
                Button(onClick = { showLocalExportError = false }) {
                    Text("Tutup")
                }
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Search & Register Button Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Cari siswa (Nama/Kelas/NIS)...", fontSize = 13.sp) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .weight(1f)
                    .testTag("siswa_search_bar"),
                singleLine = true
            )

            Button(
                onClick = {
                    exportLauncher.launch("Laporan_Tabungan_Siswa_${System.currentTimeMillis()}.csv")
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF2E7D32), // Emerald Green
                    contentColor = Color.White
                ),
                contentPadding = PaddingValues(12.dp),
                modifier = Modifier
                    .testTag("export_excel_button"),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Description,
                        contentDescription = "Cetak Excel"
                    )
                    Text("Excel", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
            }

            Button(
                onClick = { expandRegForm = !expandRegForm },
                contentPadding = PaddingValues(12.dp),
                modifier = Modifier
                    .testTag("toggle_register_form_button"),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(
                    imageVector = if (expandRegForm) Icons.Default.Close else Icons.Default.PersonAdd,
                    contentDescription = "Pendaftaran"
                )
            }
        }

        // Expandable Registration Form
        AnimatedVisibility(
            visible = expandRegForm,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut()
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("registration_form_card"),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        "Pendaftaran Nasabah Baru",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium
                    )

                    OutlinedTextField(
                        value = nameInput,
                        onValueChange = { nameInput = it },
                        label = { Text("Nama Lengkap Siswa") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("input_student_name"),
                        singleLine = true
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        OutlinedTextField(
                            value = classInput,
                            onValueChange = { classInput = it },
                            label = { Text("Kelas") },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("input_student_class"),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = regNumInput,
                            onValueChange = { regNumInput = it },
                            label = { Text("No. Induk (NIS)") },
                            modifier = Modifier
                                .weight(1.2f)
                                .testTag("input_student_nis"),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                        )
                    }

                    if (errorMessage.isNotEmpty()) {
                        Text(
                            errorMessage,
                            color = MaterialTheme.colorScheme.error,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    Button(
                        onClick = {
                            if (nameInput.isBlank() || classInput.isBlank() || regNumInput.isBlank()) {
                                errorMessage = "Semua kolom form pendaftaran wajib diisi!"
                            } else {
                                viewModel.registerSiswa(
                                    nama = nameInput.trim(),
                                    kelas = classInput.trim(),
                                    nomorInduk = regNumInput.trim()
                                ) {
                                    // Reset inputs on success
                                    nameInput = ""
                                    classInput = ""
                                    regNumInput = ""
                                    errorMessage = ""
                                    expandRegForm = false
                                }
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("submit_student_button"),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Simpan Data Siswa")
                    }
                }
            }
        }

        // Student Data Directory List
        Text(
            "Hasil Pencarian Siswa (${filteredSiswa.size})",
            fontWeight = FontWeight.Bold,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.outline
        )

        if (filteredSiswa.isEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
            ) {
                Column(
                    modifier = Modifier
                        .padding(32.dp)
                        .fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.SearchOff,
                        contentDescription = "Empty",
                        tint = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                        modifier = Modifier.size(48.dp)
                    )
                    Text(
                        "Siswa tidak ditemukan.",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleSmall
                    )
                    Text(
                        "Silahkan sesuaikan kata kunci pencarian atau daftarkan siswa baru terlebih dahulu.",
                        textAlign = TextAlign.Center,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(filteredSiswa) { siswa ->
                    val balance = balances[siswa.id] ?: 0.0
                    SiswaItem(
                        siswa = siswa,
                        balance = balance,
                        viewModel = viewModel,
                        onActionClick = { selectedSiswaForTx = siswa },
                        onHistoryClick = { selectedSiswaForHistory = siswa },
                        onDeleteClick = { viewModel.deleteSiswa(siswa.id) }
                    )
                }
            }
        }
    }

    // Modal Operations Dialog
    selectedSiswaForTx?.let { siswa ->
        QuickTransactionDialog(
            siswa = siswa,
            currentBalance = balances[siswa.id] ?: 0.0,
            viewModel = viewModel,
            onDismiss = { selectedSiswaForTx = null }
        )
    }

    // History logs Dialog
    selectedSiswaForHistory?.let { siswa ->
        SiswaHistoryDialog(
            siswa = siswa,
            transactions = transList.filter { it.siswaId == siswa.id },
            viewModel = viewModel,
            onDismiss = { selectedSiswaForHistory = null }
        )
    }
}

@Composable
fun SiswaItem(
    siswa: Siswa,
    balance: Double,
    viewModel: TabunganViewModel,
    onActionClick: () -> Unit,
    onHistoryClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    var expandedMenu by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(siswa.nama, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    Text("NIS: ${siswa.nomorInduk} • Kelas ${siswa.kelas}", fontSize = 11.sp, color = MaterialTheme.colorScheme.outline)
                }

                Box {
                    IconButton(onClick = { expandedMenu = true }) {
                        Icon(Icons.Default.MoreVert, contentDescription = "Pilihan")
                    }
                    DropdownMenu(
                        expanded = expandedMenu,
                        onDismissRequest = { expandedMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Transaksi Setor/Tarik") },
                            onClick = {
                                expandedMenu = false
                                onActionClick()
                            },
                            leadingIcon = { Icon(Icons.Default.Input, contentDescription = null, modifier = Modifier.size(18.dp)) }
                        )
                        DropdownMenuItem(
                            text = { Text("Riwayat Tabungan", color = MaterialTheme.colorScheme.primary) },
                            onClick = {
                                expandedMenu = false
                                onHistoryClick()
                            },
                            leadingIcon = { Icon(Icons.Default.History, contentDescription = null, modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.primary) }
                        )
                        Divider()
                        DropdownMenuItem(
                            text = { Text("Hapus Siswa", color = MaterialTheme.colorScheme.error) },
                            onClick = {
                                expandedMenu = false
                                onDeleteClick()
                            },
                            leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.error) }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.12f))
            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Total Saldo Tabungan", fontSize = 10.sp, color = MaterialTheme.colorScheme.outline)
                    Text(
                        viewModel.formatRupiah(balance),
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 15.sp,
                        color = if (balance >= 0) Color(0xFF2E7D32) else Color(0xFFC62828)
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = onActionClick,
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.height(34.dp)
                    ) {
                        Text("Transaksi", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuickTransactionDialog(
    siswa: Siswa,
    currentBalance: Double,
    viewModel: TabunganViewModel,
    onDismiss: () -> Unit
) {
    var tipe by remember { mutableStateOf("SETOR") }
    var jumlahInput by remember { mutableStateOf("") }
    var adminFeeInput by remember { mutableStateOf("1000") } // Defaults to 1.000 Rp admin code
    var infoInput by remember { mutableStateOf("") }
    var alertMsg by remember { mutableStateOf("") }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
                .testTag("quick_trans_dialog"),
            shape = RoundedCornerShape(16.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier.padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(
                    "Catat Transaksi Siswa",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = MaterialTheme.colorScheme.primary
                )

                Text(
                    "Siswa: ${siswa.nama} (Kelas ${siswa.kelas})",
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 13.sp
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Saldo Saat Ini:", fontSize = 11.sp, color = MaterialTheme.colorScheme.outline)
                    Text(viewModel.formatRupiah(currentBalance), fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }

                // Selector: SETOR vs TARIK
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = { tipe = "SETOR" },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (tipe == "SETOR") Color(0xFF2E7D32) else MaterialTheme.colorScheme.surfaceVariant,
                            contentColor = if (tipe == "SETOR") Color.White else MaterialTheme.colorScheme.onSurface
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("select_setor_button"),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Setor Tunai", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }

                    Button(
                        onClick = { tipe = "TARIK" },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (tipe == "TARIK") Color(0xFFC62828) else MaterialTheme.colorScheme.surfaceVariant,
                            contentColor = if (tipe == "TARIK") Color.White else MaterialTheme.colorScheme.onSurface
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("select_tarik_button"),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Tarik Tunai", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }

                OutlinedTextField(
                    value = jumlahInput,
                    onValueChange = { jumlahInput = it },
                    label = { Text("Jumlah Nominal (Rp)") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_tx_amount"),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true
                )

                OutlinedTextField(
                    value = adminFeeInput,
                    onValueChange = { adminFeeInput = it },
                    label = { Text("Biaya Admin Sekolah (Rp)") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_tx_fee"),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true
                )

                OutlinedTextField(
                    value = infoInput,
                    onValueChange = { infoInput = it },
                    label = { Text("Keterangan Tambahan") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_tx_notes"),
                    singleLine = true
                )

                if (alertMsg.isNotEmpty()) {
                    Text(
                        alertMsg,
                        color = MaterialTheme.colorScheme.error,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Batal")
                    }

                    Button(
                        onClick = {
                            val amount = jumlahInput.toDoubleOrNull() ?: 0.0
                            val fee = adminFeeInput.toDoubleOrNull() ?: 0.0
                            
                            if (amount <= 0.0) {
                                alertMsg = "Jumlah transaksi harus lebih besar dari Rp 0!"
                            } else if (tipe == "TARIK" && amount > currentBalance) {
                                alertMsg = "Saldo siswa tidak mencukupi untuk penarikan!"
                            } else {
                                viewModel.addTransaksi(
                                    siswaId = siswa.id,
                                    tipe = tipe,
                                    jumlah = amount,
                                    biayaAdmin = fee,
                                    keterangan = infoInput.trim().ifEmpty { "Transaksi $tipe" }
                                ) {
                                    onDismiss()
                                }
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (tipe == "SETOR") Color(0xFF2E7D32) else Color(0xFFC62828)
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("submit_tx_button"),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Simpan", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun SiswaHistoryDialog(
    siswa: Siswa,
    transactions: List<Transaksi>,
    viewModel: TabunganViewModel,
    onDismiss: () -> Unit
) {
    val schoolName by viewModel.schoolName.collectAsState()
    val treasurerName by viewModel.treasurerName.collectAsState()
    var txToDelete by remember { mutableStateOf<Transaksi?>(null) }
    var selectedTxForReceipt by remember { mutableStateOf<Transaksi?>(null) }

    if (txToDelete != null) {
        val tx = txToDelete!!
        AlertDialog(
            onDismissRequest = { txToDelete = null },
            title = { Text("Koreksi / Hapus Transaksi", fontWeight = FontWeight.Bold) },
            text = {
                Text("Apakah Anda yakin ingin menghapus data transaksi ini?\n\n" +
                        "Tipe: ${if (tx.tipe == "SETOR") "Setoran (+)" else "Penarikan (-)"}\n" +
                        "Jumlah: ${viewModel.formatRupiah(tx.jumlah)}\n" +
                        "Tanggal: ${viewModel.formatDate(tx.tanggal)}\n\n" +
                        "Tindakan ini akan mengoreksi kembali saldo nasabah dan kas seketika.")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteTransaksi(tx.id)
                        txToDelete = null
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Hapus/Koreksi")
                }
            },
            dismissButton = {
                TextButton(onClick = { txToDelete = null }) {
                    Text("Batal")
                }
            }
        )
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.75f)
                .padding(8.dp),
            shape = RoundedCornerShape(16.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(16.dp)
                    .fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Riwayat Tabungan", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        Text(siswa.nama, fontSize = 12.sp, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold)
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Tutup")
                    }
                }

                Divider()

                if (transactions.isEmpty()) {
                    Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                        Text(
                            "Belum ada riwayat transaksi.",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.weight(1f).fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(transactions) { tx ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f), RoundedCornerShape(8.dp))
                                    .padding(10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                        Icon(
                                            imageVector = if (tx.tipe == "SETOR") Icons.Default.TrendingUp else Icons.Default.TrendingDown,
                                            contentDescription = null,
                                            tint = if (tx.tipe == "SETOR") Color(0xFF2E7D32) else Color(0xFFC62828),
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Text(
                                            if (tx.tipe == "SETOR") "Setoran Masuk" else "Penarikan Dana",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.sp,
                                            color = if (tx.tipe == "SETOR") Color(0xFF2E7D32) else Color(0xFFC62828)
                                        )
                                    }
                                    Text(viewModel.formatDate(tx.tanggal), fontSize = 9.sp, color = Color.Gray)
                                    if (tx.keterangan.isNotEmpty()) {
                                        Text(tx.keterangan, fontSize = 10.sp, fontStyle = androidx.compose.ui.text.font.FontStyle.Italic)
                                    }
                                }

                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Column(horizontalAlignment = Alignment.End) {
                                        Text(
                                            "${if (tx.tipe == "SETOR") "+" else "-"} ${viewModel.formatRupiah(tx.jumlah)}",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.sp,
                                            color = if (tx.tipe == "SETOR") Color(0xFF2E7D32) else Color(0xFFC62828)
                                        )
                                        if (tx.biayaAdmin > 0) {
                                            Text("Admin: ${viewModel.formatRupiah(tx.biayaAdmin)}", fontSize = 8.sp, color = Color.Gray)
                                        }
                                    }

                                    IconButton(
                                        onClick = { selectedTxForReceipt = tx },
                                        modifier = Modifier.size(28.dp).testTag("print_dialog_mutation_${tx.id}")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Print,
                                            contentDescription = "Cetak Slip",
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }

                                    val currentRole by viewModel.currentRole.collectAsState()
                                    if (currentRole == UserRole.BENDAHARA || currentRole == UserRole.ADMIN) {
                                        IconButton(
                                            onClick = { txToDelete = tx },
                                            modifier = Modifier.size(28.dp).testTag("delete_dialog_mutation_${tx.id}")
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Delete,
                                                contentDescription = "Hapus Transaksi",
                                                tint = MaterialTheme.colorScheme.error,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                Button(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Tutup Riwayat")
                }
            }
        }
    }

    selectedTxForReceipt?.let { tx ->
        Dialog(onDismissRequest = { selectedTxForReceipt = null }) {
            Card(
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                modifier = Modifier.padding(16.dp).fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("Simulasi Cetak Slip", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    
                    val formattedTime = SimpleDateFormat("HH:mm:ss", Locale("id", "ID")).format(Date(tx.tanggal))
                    val bodyText = 
                        "       SLIP MUTASI TABUNGAN\n" +
                        "         ${schoolName.uppercase(Locale("id", "ID"))}\n" +
                        "=================================\n" +
                        "Nasabah  : ${siswa.nama}\n" +
                        "NIS      : ${siswa.nomorInduk}\n" +
                        "Kelas    : ${siswa.kelas}\n" +
                        "---------------------------------\n" +
                        "Tanggal  : ${viewModel.formatDateShort(tx.tanggal)}\n" +
                        "Waktu    : $formattedTime WIB\n" +
                        "Petugas  : $treasurerName\n" +
                        "---------------------------------\n" +
                        "Tipe     : ${if (tx.tipe == "SETOR") "SETORAN MASUK (+)" else "PENARIKAN TUNAI (-)"}\n" +
                        "Jumlah   : ${viewModel.formatRupiah(tx.jumlah)}\n" +
                        "Admin    : ${viewModel.formatRupiah(tx.biayaAdmin)}\n" +
                        "Ket      : ${if (tx.keterangan.isEmpty()) "-" else tx.keterangan}\n" +
                        "=================================\n" +
                        "     [TERVERIFIKASI SISTEM]\n" +
                        "   Simulasi Printer POS-58mm"
                        
                    Box(
                        modifier = Modifier
                            .weight(1f, fill = false)
                            .heightIn(max = 240.dp)
                            .background(Color(0xFFF5F5F5), RoundedCornerShape(8.dp))
                            .verticalScroll(rememberScrollState())
                            .padding(12.dp)
                            .fillMaxWidth()
                    ) {
                        Text(
                            text = bodyText,
                            fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                            fontSize = 11.sp,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    
                    val context = LocalContext.current
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedButton(
                            onClick = { shareReceiptText(context, "Slip Mutasi Tabungan", bodyText) },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Bagikan")
                        }
                        
                        Button(
                            onClick = { printReceiptText(context, "Slip_Mutasi_Tabungan", bodyText) },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Print, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Unduh/Cetak")
                        }
                    }

                    OutlinedButton(
                        onClick = { selectedTxForReceipt = null },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Tutup Preview")
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TabunganScreen(viewModel: TabunganViewModel) {
    val currentRole by viewModel.currentRole.collectAsState()
    val transList by viewModel.transaksiList.collectAsState()
    val siswaList by viewModel.siswaList.collectAsState()
    val balances by viewModel.siswaBalances.collectAsState()
    val schoolName by viewModel.schoolName.collectAsState()
    val treasurerName by viewModel.treasurerName.collectAsState()

    var txToDelete by remember { mutableStateOf<Transaksi?>(null) }

    val reportCalendar = remember { java.util.Calendar.getInstance() }
    var selectedMonthIndex by remember { mutableStateOf(reportCalendar.get(java.util.Calendar.MONTH)) }
    var selectedYear by remember { mutableStateOf(reportCalendar.get(java.util.Calendar.YEAR)) }

    if (txToDelete != null) {
        val tx = txToDelete!!
        val matchingSiswa = siswaList.find { it.id == tx.siswaId }
        AlertDialog(
            onDismissRequest = { txToDelete = null },
            title = { Text("Koreksi / Hapus Transaksi", fontWeight = FontWeight.Bold) },
            text = {
                Text("Apakah Anda yakin ingin menghapus data transaksi ini?\n\n" +
                        "Nasabah: ${matchingSiswa?.nama ?: "-"}\n" +
                        "Tipe: ${if (tx.tipe == "SETOR") "Setoran (+)" else "Penarikan (-)"}\n" +
                        "Jumlah: ${viewModel.formatRupiah(tx.jumlah)}\n\n" +
                        "Tindakan ini akan mengoreksi kembali saldo nasabah dan kas seketika.")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteTransaksi(tx.id)
                        txToDelete = null
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Hapus/Koreksi")
                }
            },
            dismissButton = {
                TextButton(onClick = { txToDelete = null }) {
                    Text("Batal")
                }
            }
        )
    }

    val tabs = remember(currentRole) {
        if (currentRole == UserRole.KEPALA) {
            listOf("Semua Mutasi" to 1, "Lap. Keuangan" to 2)
        } else {
            listOf("Input Tab." to 0, "Semua Mutasi" to 1, "Lap. Keuangan" to 2)
        }
    }

    var activeTab by remember(currentRole) { 
        mutableStateOf(if (currentRole == UserRole.KEPALA) 1 else 0) 
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Tab Headers using Scrollable or regular TabRow
        TabRow(selectedTabIndex = tabs.indexOfFirst { it.second == activeTab }.coerceAtLeast(0)) {
            tabs.forEach { (title, index) ->
                Tab(selected = activeTab == index, onClick = { activeTab = index }) {
                    Text(title, modifier = Modifier.padding(12.dp), fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
            }
        }

        when (activeTab) {
            0 -> {
                // Form to Input Transactions
                Card(
                    modifier = Modifier.fillMaxWidth().testTag("tx_entry_card"),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    var selectedSiswaIndex by remember { mutableStateOf(-1) }
                    var dropdownExpanded by remember { mutableStateOf(false) }
                    var txType by remember { mutableStateOf("SETOR") }
                    var nominalInput by remember { mutableStateOf("") }
                    var adminFeeInput by remember { mutableStateOf("1000") }
                    var infoInput by remember { mutableStateOf("") }
                    var alertMsg by remember { mutableStateOf("") }

                    Column(
                        modifier = Modifier
                            .padding(16.dp)
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text("Pencatatan Harian Tabungan Siswa", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)

                        // Student dropdown selector
                        Box(modifier = Modifier.fillMaxWidth()) {
                            OutlinedTextField(
                                value = if (selectedSiswaIndex in siswaList.indices) siswaList[selectedSiswaIndex].nama else "Pilih Nasabah Siswa",
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("Nama Siswa") },
                                trailingIcon = { Icon(Icons.Default.ArrowDropDown, contentDescription = null) },
                                modifier = Modifier.fillMaxWidth()
                            )
                            // Overlaid click-capturing box
                            Box(
                                modifier = Modifier
                                    .matchParentSize()
                                    .clickable { dropdownExpanded = true }
                                    .testTag("tx_student_selector")
                            )

                            DropdownMenu(
                                expanded = dropdownExpanded,
                                onDismissRequest = { dropdownExpanded = false },
                                modifier = Modifier.fillMaxWidth(0.9f)
                            ) {
                                if (siswaList.isEmpty()) {
                                    DropdownMenuItem(
                                        text = { Text("Tidak ada siswa terdaftar. Tambahkan di menu Nasabah dahulu.") },
                                        onClick = { dropdownExpanded = false }
                                    )
                                } else {
                                    siswaList.forEachIndexed { idx, siswa ->
                                        val bal = balances[siswa.id] ?: 0.0
                                        DropdownMenuItem(
                                            text = { Text("${siswa.nama} (Kelas ${siswa.kelas}) — Saldo: ${viewModel.formatRupiah(bal)}") },
                                            onClick = {
                                                selectedSiswaIndex = idx
                                                dropdownExpanded = false
                                            }
                                        )
                                    }
                                }
                            }
                        }

                        // Type of mutation
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Button(
                                onClick = { txType = "SETOR" },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (txType == "SETOR") Color(0xFF2E7D32) else MaterialTheme.colorScheme.surfaceVariant,
                                    contentColor = if (txType == "SETOR") Color.White else MaterialTheme.colorScheme.onSurface
                                ),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text("SETOR (Simulasi)", fontWeight = FontWeight.Bold)
                            }

                            Button(
                                onClick = { txType = "TARIK" },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (txType == "TARIK") Color(0xFFC62828) else MaterialTheme.colorScheme.surfaceVariant,
                                    contentColor = if (txType == "TARIK") Color.White else MaterialTheme.colorScheme.onSurface
                                ),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text("PENARIKAN", fontWeight = FontWeight.Bold)
                            }
                        }

                        OutlinedTextField(
                            value = nominalInput,
                            onValueChange = { nominalInput = it },
                            label = { Text("Jumlah Nominal Tabungan (Rp)") },
                            modifier = Modifier.fillMaxWidth().testTag("tx_input_amount"),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true
                        )

                        OutlinedTextField(
                            value = adminFeeInput,
                            onValueChange = { adminFeeInput = it },
                            label = { Text("Biaya Admin Sekolah (Rp)") },
                            modifier = Modifier.fillMaxWidth().testTag("tx_input_fee"),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true
                        )

                        OutlinedTextField(
                            value = infoInput,
                            onValueChange = { infoInput = it },
                            label = { Text("Keterangan Opsional") },
                            modifier = Modifier.fillMaxWidth().testTag("tx_input_remarks"),
                            singleLine = true
                        )

                        if (alertMsg.isNotEmpty()) {
                            Text(alertMsg, color = MaterialTheme.colorScheme.error, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }

                        Button(
                            onClick = {
                                if (selectedSiswaIndex !in siswaList.indices) {
                                    alertMsg = "Harap tentukan/pilih siswa terlebih dahulu!"
                                    return@Button
                                }
                                val siswa = siswaList[selectedSiswaIndex]
                                val amount = nominalInput.toDoubleOrNull() ?: 0.0
                                val fee = adminFeeInput.toDoubleOrNull() ?: 0.0
                                val curBal = balances[siswa.id] ?: 0.0

                                if (amount <= 0.0) {
                                    alertMsg = "Nominal tabungan harus bernilai positif!"
                                } else if (txType == "TARIK" && amount > curBal) {
                                    alertMsg = "Gagal memproses penarikan! Saldo siswa tidak mencukupi."
                                } else {
                                    viewModel.addTransaksi(
                                        siswaId = siswa.id,
                                        tipe = txType,
                                        jumlah = amount,
                                        biayaAdmin = fee,
                                        keterangan = infoInput.trim().ifEmpty { "Pencatatan $txType" }
                                    ) {
                                        // Reset
                                        nominalInput = ""
                                        infoInput = ""
                                        alertMsg = ""
                                        selectedSiswaIndex = -1
                                    }
                                }
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary
                            ),
                            modifier = Modifier.fillMaxWidth().testTag("tx_submit_entry"),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("Simpan & Catat Transaksi")
                        }
                    }
                }
            }

            1 -> {
                // Semua Mutasi Tabungan list
                if (transList.isEmpty()) {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Default.Inbox, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(36.dp))
                        Text("Belum ada transaksi tabungan.", color = Color.Gray, fontSize = 13.sp)
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(transList) { mutation ->
                            val s = siswaList.find { it.id == mutation.siswaId }
                            MutationItem(
                                siswaName = s?.nama ?: "Siswa Terdaftar",
                                siswaClass = s?.kelas ?: "-",
                                mutation = mutation,
                                viewModel = viewModel,
                                onDeleteClick = { txToDelete = mutation }
                            )
                        }
                    }
                }
            }

            2 -> {
                // Lap. Keuangan section (Automatic monthly compilation)
                val reports = remember(transList) { viewModel.getMonthlyReports() }

                LazyColumn(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    item {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f))
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Icon(Icons.Default.Print, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                    Text("Laba & Laporan Otomatis", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                }
                                Text(
                                    "Aplikasi merangkum mutasi setoran, penarikan, dan laba administrasi bulanan nasabah secara otomatis di bawah ini.",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(top = 4.dp)
                                )
                            }
                        }
                    }

                    item {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                            modifier = Modifier.fillMaxWidth(),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Event,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                    Text(
                                        text = "Cari & Cetak Laporan Pilihan",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                                
                                Text(
                                    text = "Silakan tentukan bulan dan tahun laporannya di bawah ini untuk melihat rangkuman serta mencetak kuitansi kas.",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )

                                var monthDropdownExpanded by remember { mutableStateOf(false) }
                                var yearDropdownExpanded by remember { mutableStateOf(false) }

                                val indonesianMonths = listOf(
                                    "Januari", "Februari", "Maret", "April", "Mei", "Juni",
                                    "Juli", "Agustus", "September", "Oktober", "November", "Desember"
                                )
                                val availableYears = (2024..2030).toList()

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    // Month Selector dropdown
                                    Box(modifier = Modifier.weight(1.2f)) {
                                        OutlinedTextField(
                                            value = indonesianMonths[selectedMonthIndex],
                                            onValueChange = {},
                                            readOnly = true,
                                            label = { Text("Pilih Bulan") },
                                            trailingIcon = { Icon(Icons.Default.ArrowDropDown, contentDescription = null) },
                                            modifier = Modifier.fillMaxWidth()
                                        )
                                        Box(
                                            modifier = Modifier
                                                .matchParentSize()
                                                .clickable { monthDropdownExpanded = true }
                                        )
                                        DropdownMenu(
                                            expanded = monthDropdownExpanded,
                                            onDismissRequest = { monthDropdownExpanded = false }
                                        ) {
                                            indonesianMonths.forEachIndexed { index, mName ->
                                                DropdownMenuItem(
                                                    text = { Text(mName) },
                                                    onClick = {
                                                        selectedMonthIndex = index
                                                        monthDropdownExpanded = false
                                                    }
                                                )
                                            }
                                        }
                                    }

                                    // Year Selector dropdown
                                    Box(modifier = Modifier.weight(0.8f)) {
                                        OutlinedTextField(
                                            value = selectedYear.toString(),
                                            onValueChange = {},
                                            readOnly = true,
                                            label = { Text("Tahun") },
                                            trailingIcon = { Icon(Icons.Default.ArrowDropDown, contentDescription = null) },
                                            modifier = Modifier.fillMaxWidth()
                                        )
                                        Box(
                                            modifier = Modifier
                                                .matchParentSize()
                                                .clickable { yearDropdownExpanded = true }
                                        )
                                        DropdownMenu(
                                            expanded = yearDropdownExpanded,
                                            onDismissRequest = { yearDropdownExpanded = false }
                                        ) {
                                            availableYears.forEach { yr ->
                                                DropdownMenuItem(
                                                    text = { Text(yr.toString()) },
                                                    onClick = {
                                                        selectedYear = yr
                                                        yearDropdownExpanded = false
                                                    }
                                                )
                                            }
                                        }
                                    }
                                }

                                // We calculate the dynamic MonthlyReport for the chosen Month and Year!
                                val targetMonthYear = "${indonesianMonths[selectedMonthIndex]} $selectedYear"
                                val customReport = remember(transList, selectedMonthIndex, selectedYear) {
                                    val filteredTrans = transList.filter { tx ->
                                        try {
                                            val cal = java.util.Calendar.getInstance().apply { timeInMillis = tx.tanggal }
                                            cal.get(java.util.Calendar.MONTH) == selectedMonthIndex && cal.get(java.util.Calendar.YEAR) == selectedYear
                                        } catch (e: Exception) {
                                            false
                                        }
                                    }
                                    var tSetor = 0.0
                                    var cSetor = 0
                                    var tTarik = 0.0
                                    var cTarik = 0
                                    var tAdmin = 0.0
                                    
                                    for (t in filteredTrans) {
                                        if (t.tipe == "SETOR") {
                                            tSetor += t.jumlah
                                            cSetor++
                                        } else {
                                            tTarik += t.jumlah
                                            cTarik++
                                        }
                                        tAdmin += t.biayaAdmin
                                    }
                                    
                                    MonthlyReport(
                                        monthYear = targetMonthYear,
                                        totalSetor = tSetor,
                                        jumlahSetorCount = cSetor,
                                        totalTarik = tTarik,
                                        jumlahTarikCount = cTarik,
                                        totalBiayaAdmin = tAdmin
                                    )
                                }

                                Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))

                                // Visual presentation of the chosen month's data
                                Text(
                                    text = "Rangkuman Data: $targetMonthYear",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.primary
                                )

                                Row(modifier = Modifier.fillMaxWidth()) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text("Total Setoran", fontSize = 10.sp, color = Color.Gray)
                                        Text(
                                            "${viewModel.formatRupiah(customReport.totalSetor)} (${customReport.jumlahSetorCount} tx)",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.sp,
                                            color = Color(0xFF2E7D32)
                                        )
                                    }
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text("Total Penarikan", fontSize = 10.sp, color = Color.Gray)
                                        Text(
                                            "${viewModel.formatRupiah(customReport.totalTarik)} (${customReport.jumlahTarikCount} tx)",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.sp,
                                            color = Color(0xFFC62828)
                                        )
                                    }
                                }

                                Row(modifier = Modifier.fillMaxWidth()) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text("Laba Admin", fontSize = 10.sp, color = Color.Gray)
                                        Text(
                                            viewModel.formatRupiah(customReport.totalBiayaAdmin),
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.sp,
                                            color = Color(0xFF004D40)
                                        )
                                    }
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text("Arus Kas Netto", fontSize = 10.sp, color = Color.Gray)
                                        val customNet = customReport.totalSetor - customReport.totalTarik
                                        Text(
                                            (if (customNet >= 0) "+" else "") + viewModel.formatRupiah(customNet),
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.sp,
                                            color = if (customNet >= 0) Color(0xFF2E7D32) else Color(0xFFC62828)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(4.dp))

                                // Render the print card for this computed report!
                                var showCustomPrintByDialog by remember { mutableStateOf(false) }

                                Button(
                                    onClick = { showCustomPrintByDialog = true },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE65100)), // Rich Accent Color
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Icon(Icons.Default.Print, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Text("Cetak Laporan Bulan Terpilih", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                    }
                                }

                                if (showCustomPrintByDialog) {
                                    val transListState by viewModel.transaksiList.collectAsState()
                                    val siswaListState by viewModel.siswaList.collectAsState()

                                    Dialog(onDismissRequest = { showCustomPrintByDialog = false }) {
                                        Card(
                                            shape = RoundedCornerShape(16.dp),
                                            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                                            modifier = Modifier.padding(8.dp).fillMaxWidth()
                                        ) {
                                            Column(
                                                modifier = Modifier.padding(16.dp),
                                                verticalArrangement = Arrangement.spacedBy(12.dp),
                                                horizontalAlignment = Alignment.CenterHorizontally
                                            ) {
                                                Text("Cetak Laporan Bulanan (Pilihan)", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                                
                                                val monthSdf = SimpleDateFormat("MMMM yyyy", Locale("id", "ID"))
                                                val monthlyTrans = transListState.filter { tx ->
                                                    try {
                                                        monthSdf.format(Date(tx.tanggal)) == customReport.monthYear
                                                    } catch (e: Exception) {
                                                        false
                                                    }
                                                }

                                                val bodyText = buildString {
                                                    append("         ${schoolName.uppercase(Locale("id", "ID"))}\n")
                                                    append("      REKAPITULASI PELAPORAN\n")
                                                    append("         KEUANGAN BULANAN\n")
                                                    append("=================================\n")
                                                    append("Periode  : ${customReport.monthYear}\n")
                                                    append("Tanggal  : ${SimpleDateFormat("dd/MM/yyyy HH:mm", Locale("id", "ID")).format(Date())}\n")
                                                    append("=================================\n")
                                                    append("Setoran  : ${viewModel.formatRupiah(customReport.totalSetor)}\n")
                                                    append("           (${customReport.jumlahSetorCount} transaksi)\n")
                                                    append("- - - - - - - - - - - - - - - - -\n")
                                                    append("Penarikan: ${viewModel.formatRupiah(customReport.totalTarik)}\n")
                                                    append("           (${customReport.jumlahTarikCount} transaksi)\n")
                                                    append("=================================\n")
                                                    append("PENDAPATAN ADMIN: ${viewModel.formatRupiah(customReport.totalBiayaAdmin)}\n")
                                                    append("Arus Kas Netto  : ${viewModel.formatRupiah(customReport.totalSetor - customReport.totalTarik)}\n")
                                                    append("=================================\n")
                                                    append("         MUTASI TRANSAKSI\n")
                                                    append("---------------------------------\n")
                                                    if (monthlyTrans.isEmpty()) {
                                                        append("  Tidak ada transaksi bulan ini\n")
                                                    } else {
                                                        monthlyTrans.forEachIndexed { idx, tx ->
                                                            val s = siswaListState.find { it.id == tx.siswaId }
                                                            val name = s?.nama ?: "Nasabah"
                                                            val sClass = s?.kelas ?: "-"
                                                            val dateStr = SimpleDateFormat("dd/MM/yyyy", Locale("id", "ID")).format(Date(tx.tanggal))
                                                            val typeLabel = if (tx.tipe == "SETOR") "Setor [+]" else "Tarik [-]"
                                                            append("${idx + 1}. $dateStr | $name ($sClass)\n")
                                                            append("   $typeLabel: ${viewModel.formatRupiah(tx.jumlah)}\n")
                                                            if (tx.biayaAdmin > 0) {
                                                                append("   Admin   : ${viewModel.formatRupiah(tx.biayaAdmin)}\n")
                                                            }
                                                            append("---------------------------------\n")
                                                        }
                                                    }
                                                    append("Bendahara: $treasurerName\n")
                                                    append("=================================\n")
                                                    append("     [TERVERIFIKASI SISTEM]\n")
                                                    append("   Simulasi Printer POS-58mm")
                                                }

                                                Box(
                                                    modifier = Modifier
                                                        .weight(1f, fill = false)
                                                        .heightIn(max = 280.dp)
                                                        .background(Color(0xFFF5F5F5), RoundedCornerShape(8.dp))
                                                        .verticalScroll(rememberScrollState())
                                                        .padding(12.dp)
                                                        .fillMaxWidth()
                                                ) {
                                                    Text(
                                                        text = bodyText,
                                                        fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                                                        fontSize = 11.sp,
                                                        modifier = Modifier.fillMaxWidth()
                                                    )
                                                }

                                                val context = LocalContext.current
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    OutlinedButton(
                                                        onClick = { shareReceiptText(context, "Laporan Bulanan ${customReport.monthYear}", bodyText) },
                                                        modifier = Modifier.weight(1f)
                                                    ) {
                                                        Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                                                        Spacer(modifier = Modifier.width(4.dp))
                                                        Text("Bagikan")
                                                    }
                                                    
                                                    Button(
                                                        onClick = { printReceiptText(context, "Laporan_Bulanan_${customReport.monthYear.replace(" ", "_")}", bodyText) },
                                                        modifier = Modifier.weight(1f)
                                                    ) {
                                                        Icon(Icons.Default.Print, contentDescription = null, modifier = Modifier.size(16.dp))
                                                        Spacer(modifier = Modifier.width(4.dp))
                                                        Text("Unduh/Cetak")
                                                    }
                                                }

                                                OutlinedButton(
                                                    onClick = { showCustomPrintByDialog = false },
                                                    modifier = Modifier.fillMaxWidth()
                                                ) {
                                                    Text("Tutup Preview")
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    if (reports.isEmpty()) {
                        item {
                            Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                                Text("Laporan mutasi periodik kosong.", color = Color.Gray, fontSize = 12.sp)
                            }
                        }
                    } else {
                        items(reports) { r ->
                            MonthlyReportCard(report = r, viewModel = viewModel)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun MonthlyReportCard(report: MonthlyReport, viewModel: TabunganViewModel) {
    val schoolName by viewModel.schoolName.collectAsState()
    val treasurerName by viewModel.treasurerName.collectAsState()
    var showDialog by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(report.monthYear, fontWeight = FontWeight.ExtraBold, fontSize = 16.sp, color = MaterialTheme.colorScheme.primary)
                IconButton(onClick = { showDialog = true }, modifier = Modifier.testTag("print_button_${report.monthYear.lowercase()}")) {
                    Icon(Icons.Default.Print, contentDescription = "Cetak", tint = MaterialTheme.colorScheme.outline)
                }
            }

            Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))

            Row(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Total Setoran", fontSize = 10.sp, color = Color.Gray)
                    Text(
                        "${viewModel.formatRupiah(report.totalSetor)} (${report.jumlahSetorCount} tx)",
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        color = Color(0xFF2E7D32)
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text("Total Penarikan", fontSize = 10.sp, color = Color.Gray)
                    Text(
                        "${viewModel.formatRupiah(report.totalTarik)} (${report.jumlahTarikCount} tx)",
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        color = Color(0xFFC62828)
                    )
                }
            }

            Row(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Laba Admin", fontSize = 10.sp, color = Color.Gray)
                    Text(
                        viewModel.formatRupiah(report.totalBiayaAdmin),
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        color = Color(0xFF004D40)
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text("Arus Kas Netto", fontSize = 10.sp, color = Color.Gray)
                    val netto = report.totalSetor - report.totalTarik
                    Text(
                        (if (netto >= 0) "+" else "") + viewModel.formatRupiah(netto),
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        color = if (netto >= 0) Color(0xFF2E7D32) else Color(0xFFC62828)
                    )
                }
            }
        }
    }

    if (showDialog) {
        val transList by viewModel.transaksiList.collectAsState()
        val siswaList by viewModel.siswaList.collectAsState()

        Dialog(onDismissRequest = { showDialog = false }) {
            Card(
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                modifier = Modifier.padding(8.dp).fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("Cetak Laporan Bulanan", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    
                    val monthSdf = SimpleDateFormat("MMMM yyyy", Locale("id", "ID"))
                    val monthlyTrans = transList.filter { tx ->
                        try {
                            monthSdf.format(Date(tx.tanggal)) == report.monthYear
                        } catch (e: Exception) {
                            false
                        }
                    }

                    val bodyText = buildString {
                        append("         ${schoolName.uppercase(Locale("id", "ID"))}\n")
                        append("      REKAPITULASI PELAPORAN\n")
                        append("         KEUANGAN BULANAN\n")
                        append("=================================\n")
                        append("Periode  : ${report.monthYear}\n")
                        append("Tanggal  : ${SimpleDateFormat("dd/MM/yyyy HH:mm", Locale("id", "ID")).format(Date())}\n")
                        append("=================================\n")
                        append("Setoran  : ${viewModel.formatRupiah(report.totalSetor)}\n")
                        append("           (${report.jumlahSetorCount} transaksi)\n")
                        append("- - - - - - - - - - - - - - - - -\n")
                        append("Penarikan: ${viewModel.formatRupiah(report.totalTarik)}\n")
                        append("           (${report.jumlahTarikCount} transaksi)\n")
                        append("=================================\n")
                        append("PENDAPATAN ADMIN: ${viewModel.formatRupiah(report.totalBiayaAdmin)}\n")
                        append("Arus Kas Netto  : ${viewModel.formatRupiah(report.totalSetor - report.totalTarik)}\n")
                        append("=================================\n")
                        append("         MUTASI TRANSAKSI\n")
                        append("---------------------------------\n")
                        if (monthlyTrans.isEmpty()) {
                            append("  Tidak ada transaksi bulan ini\n")
                        } else {
                            monthlyTrans.forEachIndexed { idx, tx ->
                                val s = siswaList.find { it.id == tx.siswaId }
                                val name = s?.nama ?: "Nasabah"
                                val sClass = s?.kelas ?: "-"
                                val dateStr = SimpleDateFormat("dd/MM/yyyy", Locale("id", "ID")).format(Date(tx.tanggal))
                                val typeLabel = if (tx.tipe == "SETOR") "Setor [+]" else "Tarik [-]"
                                append("${idx + 1}. $dateStr | $name ($sClass)\n")
                                append("   $typeLabel: ${viewModel.formatRupiah(tx.jumlah)}\n")
                                if (tx.biayaAdmin > 0) {
                                    append("   Admin   : ${viewModel.formatRupiah(tx.biayaAdmin)}\n")
                                }
                                append("---------------------------------\n")
                            }
                        }
                        append("Bendahara: $treasurerName\n")
                        append("=================================\n")
                        append("     [TERVERIFIKASI SISTEM]\n")
                        append("   Simulasi Printer POS-58mm")
                    }

                    Box(
                        modifier = Modifier
                            .weight(1f, fill = false)
                            .heightIn(max = 280.dp)
                            .background(Color(0xFFF5F5F5), RoundedCornerShape(8.dp))
                            .verticalScroll(rememberScrollState())
                            .padding(12.dp)
                            .fillMaxWidth()
                    ) {
                        Text(
                            text = bodyText,
                            fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                            fontSize = 11.sp,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    val context = LocalContext.current
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedButton(
                            onClick = { shareReceiptText(context, "Laporan Bulanan ${report.monthYear}", bodyText) },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Bagikan")
                        }
                        
                        Button(
                            onClick = { printReceiptText(context, "Laporan_Bulanan_${report.monthYear.replace(" ", "_")}", bodyText) },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Print, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Unduh/Cetak")
                        }
                    }

                    OutlinedButton(
                        onClick = { showDialog = false },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Tutup Preview")
                    }
                }
            }
        }
    }
}

@Composable
fun SchoolLoansScreen(viewModel: TabunganViewModel) {
    val context = LocalContext.current
    val loans by viewModel.pinjamanSekolahList.collectAsState()
    val schoolName by viewModel.schoolName.collectAsState()
    val treasurerName by viewModel.treasurerName.collectAsState()

    var descInput by remember { mutableStateOf("") }
    var amountInput by remember { mutableStateOf("") }
    var errorMsg by remember { mutableStateOf("") }

    var showExportSuccess by remember { mutableStateOf(false) }
    var showExportError by remember { mutableStateOf(false) }
    var showPrintSchoolSummaryDialog by remember { mutableStateOf(false) }

    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/csv")
    ) { uri ->
        uri?.let {
            val success = viewModel.writeLoansCsvToUri(context, uri)
            if (success) {
                showExportSuccess = true
            } else {
                showExportError = true
            }
        }
    }

    if (showExportSuccess) {
        AlertDialog(
            onDismissRequest = { showExportSuccess = false },
            title = { Text("Ekspor Berhasil", fontWeight = FontWeight.Bold) },
            text = { Text("Rekapitulasi data pinjaman sekolah dan guru berhasil disimpan sebagai file Excel (.csv) di perangkat Anda.") },
            confirmButton = {
                Button(onClick = { showExportSuccess = false }) {
                    Text("Ok / Selesai")
                }
            }
        )
    }

    if (showExportError) {
        AlertDialog(
            onDismissRequest = { showExportError = false },
            title = { Text("Ekspor Gagal", fontWeight = FontWeight.Bold) },
            text = { Text("Terjadi kesalahan saat mengekspor data Excel.") },
            confirmButton = {
                Button(onClick = { showExportError = false }) {
                    Text("Tutup")
                }
            }
        )
    }

    // Repay state
    var selectedLoanForPay by remember { mutableStateOf<PinjamanSekolah?>(null) }
    var showBulkPaymentDialog by remember { mutableStateOf(false) }

    val activeLoans = loans.filter { !it.lunas }
    val totalOutstanding = activeLoans.sumOf { it.jumlahPinjam - it.jumlahBayar }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Loan Form Entry
        item {
            Card(
                modifier = Modifier.fillMaxWidth().testTag("school_loan_form"),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.School,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            "Dana Talangan Instansi Sekolah",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = descInput,
                            onValueChange = { descInput = it },
                            label = { Text("Deskripsi / Tujuan", fontSize = 11.sp) },
                            modifier = Modifier.weight(1.2f).testTag("input_school_desc"),
                            textStyle = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.sp),
                            singleLine = true
                        )

                        OutlinedTextField(
                            value = amountInput,
                            onValueChange = { amountInput = it },
                            label = { Text("Nominal Pinjam", fontSize = 11.sp) },
                            modifier = Modifier.weight(1f).testTag("input_school_loan_amount"),
                            textStyle = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.sp),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true
                        )
                    }

                    if (errorMsg.isNotEmpty()) {
                        Text(errorMsg, color = MaterialTheme.colorScheme.error, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }

                    Button(
                        onClick = {
                            val amount = amountInput.toDoubleOrNull() ?: 0.0
                            if (descInput.isBlank()) {
                                errorMsg = "Harap cantumkan keterangan/deskripsi!"
                            } else if (amount <= 0.0) {
                                errorMsg = "Jumlah pinjaman dana talangan harus positif!"
                            } else {
                                viewModel.addPinjamanSekolah(descInput.trim(), amount) {
                                    descInput = ""
                                    amountInput = ""
                                    errorMsg = ""
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth().height(36.dp).testTag("submit_school_loan"),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFC62828)),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        Text("Catat Pinjaman Sekolah", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }
            }
        }

        if (activeLoans.isNotEmpty() && totalOutstanding > 0) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth().testTag("bulk_payment_card"),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    "Bayar Sekaligus (Gabungan)",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    "Total sisa hutang (${activeLoans.size} pengeluaran): ${viewModel.formatRupiah(totalOutstanding)}",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            
                            Button(
                                onClick = { showBulkPaymentDialog = true },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.primary
                                ),
                                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.height(34.dp)
                            ) {
                                Text("Bayar Lump-Sum", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }

        item {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "Daftar Pinjaman Berjalan (${activeLoans.size})",
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleMedium,
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Button(
                        onClick = {
                            exportLauncher.launch("Rekap_Pinjaman_Sekolah_dan_Guru_${System.currentTimeMillis()}.csv")
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF2E7D32), // Emerald Green
                            contentColor = Color.White
                        ),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Description,
                                contentDescription = "Ekspor Excel Pinjaman",
                                modifier = Modifier.size(16.dp)
                            )
                            Text("Ekspor Excel", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                        }
                    }

                    Button(
                        onClick = {
                            showPrintSchoolSummaryDialog = true
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFE65100), // Orange Accent
                            contentColor = Color.White
                        ),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Print,
                                contentDescription = "Cetak Rekap Pinjaman",
                                modifier = Modifier.size(16.dp)
                            )
                            Text("Cetak Rekap Aktif", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                        }
                    }
                }
            }
        }

        if (loans.isEmpty()) {
            item {
                Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                    Text("Belum ada dana talangan sekolah tercatat.", color = Color.Gray, fontSize = 12.sp)
                }
            }
        } else {
            items(loans) { loan ->
                SchoolLoanItem(
                    loan = loan,
                    viewModel = viewModel,
                    onPayClick = { selectedLoanForPay = loan },
                    onDeleteClick = { viewModel.deletePinjamanSekolah(loan.id) }
                )
            }
        }
    }

    if (showBulkPaymentDialog) {
        var bulkAmountInput by remember { mutableStateOf("") }
        var bulkErrorMsg by remember { mutableStateOf("") }
        
        AlertDialog(
            onDismissRequest = { showBulkPaymentDialog = false },
            icon = {
                Icon(
                    imageVector = Icons.Default.Payment,
                    contentDescription = "Bayar Sekaligus",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(32.dp)
                )
            },
            title = { Text("Bayar Gabungan Sekaligus", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        "Sekolah dapat membayar satu nominal untuk mengurangi atau melunasi beberapa file pengeluaran dana talangan sekaligus secara bersama-sama.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        "Total Sisa Hutang Sekolah: ${viewModel.formatRupiah(totalOutstanding)}",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                    OutlinedTextField(
                        value = bulkAmountInput,
                        onValueChange = { bulkAmountInput = it },
                        label = { Text("Nominal Pembayaran (Rp)") },
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true
                    )
                    Text(
                        "💡 Pembayaran Anda akan didistribusikan secara berurutan, diprioritaskan otomatis untuk melunasi pinjaman/dana talangan tertua terlebih dahulu.",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.outline
                    )
                    if (bulkErrorMsg.isNotEmpty()) {
                        Text(bulkErrorMsg, color = MaterialTheme.colorScheme.error, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val amt = bulkAmountInput.toDoubleOrNull() ?: 0.0
                        if (amt <= 0.0) {
                            bulkErrorMsg = "Nominal pembayaran harus lebih besar dari 0!"
                        } else if (amt > totalOutstanding) {
                            bulkErrorMsg = "Nominal pembayaran melebihi total sisa hutang (${viewModel.formatRupiah(totalOutstanding)})!"
                        } else {
                            viewModel.bayarPinjamanSekolahSekaligus(amt) {
                                showBulkPaymentDialog = false
                            }
                        }
                    }
                ) {
                    Text("Konfirmasi Pembayaran", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showBulkPaymentDialog = false }) {
                    Text("Batal")
                }
            }
        )
    }

    // Payment Dialog
    selectedLoanForPay?.let { loan ->
        PaymentDialog(
            title = "Pembayaran Dana Talangan",
            borrowerInfo = loan.deskripsi,
            totalAmount = loan.jumlahPinjam,
            paidAmount = loan.jumlahBayar,
            viewModel = viewModel,
            onDismiss = { selectedLoanForPay = null },
            onSubmitPayment = { bayarNominal ->
                viewModel.bayarPinjamanSekolah(loan, bayarNominal) {
                    selectedLoanForPay = null
                }
            }
        )
    }

    if (showPrintSchoolSummaryDialog) {
        Dialog(onDismissRequest = { showPrintSchoolSummaryDialog = false }) {
            Card(
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                modifier = Modifier.padding(8.dp).fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("Cetak Rekap Dana Talangan Sekolah", fontWeight = FontWeight.Bold, fontSize = 15.sp)

                    val unpaidLoans = loans.filter { !it.lunas }
                    val totalTagihanAktif = unpaidLoans.sumOf { it.jumlahPinjam - it.jumlahBayar }

                    val bodyText = buildString {
                        append("         ${schoolName.uppercase(Locale("id", "ID"))}\n")
                        append("      REKAPITULASI PIUTANG KAS\n")
                        append("       DANA TALANGAN SEKOLAH\n")
                        append("=================================\n")
                        append("Hari/Tgl : ${SimpleDateFormat("dd/MM/yyyy HH:mm", Locale("id", "ID")).format(Date())}\n")
                        append("=================================\n")
                        
                        if (unpaidLoans.isEmpty()) {
                            append("  Tidak ada dana talangan aktif.\n")
                        } else {
                            unpaidLoans.sortedBy { it.tanggalPinjam }.forEachIndexed { idx, loan ->
                                val dateStr = SimpleDateFormat("dd/MM/yyyy", Locale("id", "ID")).format(Date(loan.tanggalPinjam))
                                val statusLabel = when {
                                    loan.lunas -> "LUNAS"
                                    loan.jumlahBayar > 0.0 -> "CICIL"
                                    else -> "BELUM LUNAS"
                                }
                                append("#${idx + 1} $dateStr\n")
                                append("   Ket      : ${loan.deskripsi}\n")
                                append("   Pinjaman : ${viewModel.formatRupiah(loan.jumlahPinjam)}\n")
                                if (loan.jumlahBayar > 0.0) {
                                    append("   Dibayar  : ${viewModel.formatRupiah(loan.jumlahBayar)}\n")
                                    val sisa = loan.jumlahPinjam - loan.jumlahBayar
                                    append("   Sisa     : ${viewModel.formatRupiah(sisa)}\n")
                                }
                                append("   Status   : $statusLabel\n")
                                append("- - - - - - - - - - - - - - - - -\n")
                            }
                        }
                        
                        append("TOTAL OUTSTANDING SEKOLAH:\n")
                        append(">> ${viewModel.formatRupiah(totalTagihanAktif)}\n")
                        append("=================================\n")
                        append("Bendahara: $treasurerName\n")
                        append("=================================\n")
                        append("     [TERVERIFIKASI SISTEM]\n")
                        append("   Simulasi Printer POS-58mm")
                    }

                    Box(
                        modifier = Modifier
                            .weight(1f, fill = false)
                            .heightIn(max = 280.dp)
                            .background(Color(0xFFF5F5F5), RoundedCornerShape(8.dp))
                            .verticalScroll(rememberScrollState())
                            .padding(12.dp)
                            .fillMaxWidth()
                    ) {
                        Text(
                            text = bodyText,
                            fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                            fontSize = 11.sp,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedButton(
                            onClick = { shareReceiptText(context, "Rekap Dana Talangan Sekolah", bodyText) },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Bagikan")
                        }
                        
                        Button(
                            onClick = { printReceiptText(context, "Rekap_Dana_Talangan_Sekolah", bodyText) },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Print, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Unduh/Cetak")
                        }
                    }

                    OutlinedButton(
                        onClick = { showPrintSchoolSummaryDialog = false },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Tutup Preview")
                    }
                }
            }
        }
    }
}

@Composable
fun SchoolLoanItem(
    loan: PinjamanSekolah,
    viewModel: TabunganViewModel,
    onPayClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    val schoolName by viewModel.schoolName.collectAsState()
    val treasurerName by viewModel.treasurerName.collectAsState()
    val remaining = loan.jumlahPinjam - loan.jumlahBayar
    var showPrintReceipt by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column {
                    Text(loan.deskripsi, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Text("Tanggal: ${viewModel.formatDateShort(loan.tanggalPinjam)}", fontSize = 10.sp, color = MaterialTheme.colorScheme.outline)
                }

                Box(
                    modifier = Modifier
                        .background(
                            if (loan.lunas) Color(0xFFE8F5E9) else Color(0xFFFFEBEE),
                            RoundedCornerShape(4.dp)
                        )
                        .padding(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    Text(
                        if (loan.lunas) "LUNAS" else "BELUM LUNAS",
                        fontWeight = FontWeight.Bold,
                        fontSize = 9.sp,
                        color = if (loan.lunas) Color(0xFF2E7D32) else Color(0xFFC62828)
                    )
                }
            }

            Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.08f))

            Row(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Jumlah Pinjam", fontSize = 10.sp, color = Color.Gray)
                    Text(viewModel.formatRupiah(loan.jumlahPinjam), fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text("Total Dibayar", fontSize = 10.sp, color = Color.Gray)
                    Text(viewModel.formatRupiah(loan.jumlahBayar), fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color(0xFF2E7D32))
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text("Sisa Hutang", fontSize = 10.sp, color = Color.Gray)
                    Text(viewModel.formatRupiah(remaining), fontWeight = FontWeight.Bold, fontSize = 12.sp, color = if (remaining > 0) Color(0xFFC62828) else Color(0xFF2E7D32))
                }
            }

            if (!loan.lunas) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onDeleteClick) {
                        Icon(Icons.Default.Delete, contentDescription = "Hapus", tint = MaterialTheme.colorScheme.error)
                    }
                    IconButton(onClick = { showPrintReceipt = true }) {
                        Icon(Icons.Default.Print, contentDescription = "Cetak", tint = MaterialTheme.colorScheme.primary)
                    }
                    Button(
                        onClick = onPayClick,
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 4.dp),
                        modifier = Modifier.height(32.dp)
                    ) {
                        Text("Bayar Cicilan", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    loan.tanggalLunas?.let { tLunas ->
                        Text("Dilunasi pada: ${viewModel.formatDate(tLunas)}", fontSize = 10.sp, fontStyle = androidx.compose.ui.text.font.FontStyle.Italic, color = Color.Gray)
                    }
                    IconButton(onClick = { showPrintReceipt = true }) {
                        Icon(Icons.Default.Print, contentDescription = "Cetak", tint = MaterialTheme.colorScheme.primary)
                    }
                }
            }
        }
    }

    if (showPrintReceipt) {
        Dialog(onDismissRequest = { showPrintReceipt = false }) {
            Card(
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                modifier = Modifier.padding(16.dp).fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("Simulasi Cetak Slip", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    
                    val bodyText = 
                        "       BUKTI PINJAMAN KEUANGAN\n" +
                        "         ${schoolName.uppercase(Locale("id", "ID"))}\n" +
                        "=================================\n" +
                        "ID Slip  : TAL-SCH-${loan.id}\n" +
                        "Tanggal  : ${viewModel.formatDate(loan.tanggalPinjam)}\n" +
                        "Desk     : ${loan.deskripsi}\n" +
                        "---------------------------------\n" +
                        "Jumlah   : ${viewModel.formatRupiah(loan.jumlahPinjam)}\n" +
                        "Dibayar  : ${viewModel.formatRupiah(loan.jumlahBayar)}\n" +
                        "Sisa     : ${viewModel.formatRupiah(loan.jumlahPinjam - loan.jumlahBayar)}\n" +
                        "Status   : ${if (loan.lunas) "LUNAS SEPENUHNYA" else "BELUM LUNAS"}\n" +
                        "---------------------------------\n" +
                        "Sifat    : Internal Keuangan\n" +
                        "Otorisator: $treasurerName\n" +
                        "=================================\n" +
                        "     [TERVERIFIKASI SISTEM]\n" +
                        "   Simulasi Printer POS-58mm"
                        
                    Box(
                        modifier = Modifier
                            .weight(1f, fill = false)
                            .heightIn(max = 240.dp)
                            .background(Color(0xFFF5F5F5), RoundedCornerShape(8.dp))
                            .verticalScroll(rememberScrollState())
                            .padding(12.dp)
                            .fillMaxWidth()
                    ) {
                        Text(
                            text = bodyText,
                            fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                            fontSize = 11.sp,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    
                    val context = LocalContext.current
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedButton(
                            onClick = { shareReceiptText(context, "Slip Pinjaman Sekolah", bodyText) },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Bagikan")
                        }
                        
                        Button(
                            onClick = { printReceiptText(context, "Slip_Pinjaman_Sekolah", bodyText) },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Print, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Unduh/Cetak")
                        }
                    }

                    OutlinedButton(
                        onClick = { showPrintReceipt = false },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Tutup Preview")
                    }
                }
            }
        }
    }
}

@Composable
fun TeacherLoansScreen(viewModel: TabunganViewModel) {
    val context = LocalContext.current
    val loans by viewModel.pinjamanGuruList.collectAsState()
    val schoolName by viewModel.schoolName.collectAsState()
    val treasurerName by viewModel.treasurerName.collectAsState()

    var nameInput by remember { mutableStateOf("") }
    var amountInput by remember { mutableStateOf("") }
    var errorMsg by remember { mutableStateOf("") }

    var showExportSuccess by remember { mutableStateOf(false) }
    var showExportError by remember { mutableStateOf(false) }
    var showPrintSummaryDialog by remember { mutableStateOf(false) }

    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/csv")
    ) { uri ->
        uri?.let {
            val success = viewModel.writeLoansCsvToUri(context, uri)
            if (success) {
                showExportSuccess = true
            } else {
                showExportError = true
            }
        }
    }

    if (showExportSuccess) {
        AlertDialog(
            onDismissRequest = { showExportSuccess = false },
            title = { Text("Ekspor Berhasil", fontWeight = FontWeight.Bold) },
            text = { Text("Rekapitulasi data pinjaman sekolah dan guru berhasil disimpan sebagai file Excel (.csv) di perangkat Anda.") },
            confirmButton = {
                Button(onClick = { showExportSuccess = false }) {
                    Text("Ok / Selesai")
                }
            }
        )
    }

    if (showExportError) {
        AlertDialog(
            onDismissRequest = { showExportError = false },
            title = { Text("Ekspor Gagal", fontWeight = FontWeight.Bold) },
            text = { Text("Terjadi kesalahan saat mengekspor data Excel.") },
            confirmButton = {
                Button(onClick = { showExportError = false }) {
                    Text("Tutup")
                }
            }
        )
    }

    // Repay state
    var selectedLoanForPay by remember { mutableStateOf<PinjamanGuru?>(null) }
    var selectedTeacherForBulkPay by remember { mutableStateOf<Pair<String, List<PinjamanGuru>>?>(null) }
    var exportTeacherName by remember { mutableStateOf<String?>(null) }

    val teacherExportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/csv")
    ) { uri ->
        uri?.let {
            val name = exportTeacherName
            if (name != null) {
                val success = viewModel.writeSingleGuruLoansCsvToUri(context, name, uri)
                if (success) {
                    showExportSuccess = true
                } else {
                    showExportError = true
                }
            }
        }
    }

    val groupedMap = remember(loans) {
        loans.groupBy { it.namaGuru.trim() }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Loan Form Entry
        item {
            Card(
                modifier = Modifier.fillMaxWidth().testTag("teacher_loan_form"),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.SupervisedUserCircle,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            "Pinjaman Tenaga Pendidik / Guru",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = nameInput,
                            onValueChange = { nameInput = it },
                            label = { Text("Nama Lengkap Guru", fontSize = 11.sp) },
                            modifier = Modifier.weight(1.2f).testTag("input_teacher_name"),
                            textStyle = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.sp),
                            singleLine = true
                        )

                        OutlinedTextField(
                            value = amountInput,
                            onValueChange = { amountInput = it },
                            label = { Text("Nominal Pinjam", fontSize = 11.sp) },
                            modifier = Modifier.weight(1f).testTag("input_teacher_amount"),
                            textStyle = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.sp),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true
                        )
                    }

                    if (errorMsg.isNotEmpty()) {
                        Text(errorMsg, color = MaterialTheme.colorScheme.error, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }

                    Button(
                        onClick = {
                            val amount = amountInput.toDoubleOrNull() ?: 0.0
                            if (nameInput.isBlank()) {
                                errorMsg = "Harap cantumkan nama pendidik!"
                            } else if (amount <= 0.0) {
                                errorMsg = "Jumlah pinjaman harus bernilai positif!"
                            } else {
                                viewModel.addPinjamanGuru(nameInput.trim(), amount) {
                                    nameInput = ""
                                    amountInput = ""
                                    errorMsg = ""
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth().height(36.dp).testTag("submit_teacher_loan"),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD84315)),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        Text("Keluarkan Pinjaman Guru", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }
            }
        }

        item {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "Daftar Pinjaman Guru / Staf (${groupedMap.size} Peminjam)",
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleMedium,
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Button(
                        onClick = {
                            exportLauncher.launch("Rekap_Pinjaman_Sekolah_dan_Guru_${System.currentTimeMillis()}.csv")
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF2E7D32), // Emerald Green
                            contentColor = Color.White
                        ),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Description,
                                contentDescription = "Ekspor Excel Pinjaman",
                                modifier = Modifier.size(16.dp)
                            )
                            Text("Ekspor Excel", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                        }
                    }

                    Button(
                        onClick = {
                            showPrintSummaryDialog = true
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFE65100), // Orange Accent
                            contentColor = Color.White
                        ),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Print,
                                contentDescription = "Cetak Rekap Pinjaman",
                                modifier = Modifier.size(16.dp)
                            )
                            Text("Cetak Rekap Aktif", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                        }
                    }
                }
            }
        }

        if (groupedMap.isEmpty()) {
            item {
                Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                    Text("Belum ada pinjaman guru tercatat.", color = Color.Gray, fontSize = 12.sp)
                }
            }
        } else {
            val teacherEntries = groupedMap.entries.toList().sortedBy { it.key.uppercase() }
            items(teacherEntries) { (namaGuru, teacherLoans) ->
                GroupedTeacherLoanCard(
                    namaGuru = namaGuru,
                    loansList = teacherLoans,
                    viewModel = viewModel,
                    onPayCombinedClick = {
                        selectedTeacherForBulkPay = namaGuru to teacherLoans
                    },
                    onPayIndividualClick = { loan ->
                        selectedLoanForPay = loan
                    },
                    onDeleteIndividualClick = { loan ->
                        viewModel.deletePinjamanGuru(loan.id)
                    },
                    onExportIndividualClick = {
                        exportTeacherName = namaGuru
                        teacherExportLauncher.launch("Rincian_Pinjaman_${namaGuru.replace(" ", "_")}_${System.currentTimeMillis()}.csv")
                    }
                )
            }
        }
    }

    // Payment Dialog for Individual loan
    selectedLoanForPay?.let { loan ->
        PaymentDialog(
            title = "Cicilan Pinjaman Guru",
            borrowerInfo = loan.namaGuru,
            totalAmount = loan.jumlahPinjam,
            paidAmount = loan.jumlahBayar,
            viewModel = viewModel,
            onDismiss = { selectedLoanForPay = null },
            onSubmitPayment = { bayarNominal ->
                viewModel.bayarPinjamanGuru(loan, bayarNominal) {
                    selectedLoanForPay = null
                }
            }
        )
    }

    // Payment Dialog for Combined teacher loans
    selectedTeacherForBulkPay?.let { (namaGuru, gLoans) ->
        val totalAmount = gLoans.sumOf { it.jumlahPinjam }
        val paidAmount = gLoans.sumOf { it.jumlahBayar }
        PaymentDialog(
            title = "Pembayaran Gabungan Guru",
            borrowerInfo = namaGuru,
            totalAmount = totalAmount,
            paidAmount = paidAmount,
            viewModel = viewModel,
            onDismiss = { selectedTeacherForBulkPay = null },
            onSubmitPayment = { bayarNominal ->
                viewModel.bayarPinjamanGuruSekaligus(namaGuru, bayarNominal) {
                    selectedTeacherForBulkPay = null
                }
            }
        )
    }

    if (showPrintSummaryDialog) {
        Dialog(onDismissRequest = { showPrintSummaryDialog = false }) {
            Card(
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                modifier = Modifier.padding(8.dp).fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("Cetak Rekap Pinjaman Pendidik", fontWeight = FontWeight.Bold, fontSize = 15.sp)

                    val unpaidTeachers = groupedMap.filter { entry ->
                        entry.value.any { !it.lunas }
                    }

                    val totalTagihanAktif = unpaidTeachers.values.flatten().sumOf { it.jumlahPinjam - it.jumlahBayar }

                    val bodyText = buildString {
                        append("         ${schoolName.uppercase(Locale("id", "ID"))}\n")
                        append("      REKAPITULASI PIUTANG KAS\n")
                        append("         PINJAMAN GURU & STAF\n")
                        append("=================================\n")
                        append("Hari/Tgl : ${SimpleDateFormat("dd/MM/yyyy HH:mm", Locale("id", "ID")).format(Date())}\n")
                        append("=================================\n")
                        
                        if (unpaidTeachers.isEmpty()) {
                            append("   Tidak ada tagihan pinjaman\n")
                            append("        guru yang aktif.\n")
                        } else {
                            unpaidTeachers.entries.sortedBy { it.key.uppercase() }.forEach { (namaGuru, teacherLoans) ->
                                append("GURU: ${namaGuru.uppercase()}\n")
                                append("---------------------------------\n")
                                teacherLoans.forEachIndexed { idx, loan ->
                                    val dateStr = SimpleDateFormat("dd/MM/yyyy", Locale("id", "ID")).format(Date(loan.tanggalPinjam))
                                    val statusLabel = when {
                                        loan.lunas -> "LUNAS"
                                        loan.jumlahBayar > 0.0 -> "CICIL"
                                        else -> "BELUM LUNAS"
                                    }
                                    append(" #${idx + 1} $dateStr\n")
                                    append("    Pinjaman : ${viewModel.formatRupiah(loan.jumlahPinjam)}\n")
                                    if (loan.jumlahBayar > 0.0) {
                                        append("    Dibayar  : ${viewModel.formatRupiah(loan.jumlahBayar)}\n")
                                        val sisa = loan.jumlahPinjam - loan.jumlahBayar
                                        append("    Sisa     : ${viewModel.formatRupiah(sisa)}\n")
                                    }
                                    append("    Status   : $statusLabel\n")
                                }
                                val teacherTotalSisa = teacherLoans.sumOf { it.jumlahPinjam - it.jumlahBayar }
                                append("- - - - - - - - - - - - - - - - -\n")
                                append("TOTAL HUTANG : ${viewModel.formatRupiah(teacherTotalSisa)}\n")
                                append("=================================\n")
                            }
                        }
                        
                        append("TOTAL TAGIHAN AKTIF:\n")
                        append(">> ${viewModel.formatRupiah(totalTagihanAktif)}\n")
                        append("=================================\n")
                        append("Bendahara: $treasurerName\n")
                        append("=================================\n")
                        append("     [TERVERIFIKASI SISTEM]\n")
                        append("   Simulasi Printer POS-58mm")
                    }

                    Box(
                        modifier = Modifier
                            .weight(1f, fill = false)
                            .heightIn(max = 280.dp)
                            .background(Color(0xFFF5F5F5), RoundedCornerShape(8.dp))
                            .verticalScroll(rememberScrollState())
                            .padding(12.dp)
                            .fillMaxWidth()
                    ) {
                        Text(
                            text = bodyText,
                            fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                            fontSize = 11.sp,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedButton(
                            onClick = { shareReceiptText(context, "Rekap Pinjaman Guru", bodyText) },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Bagikan")
                        }
                        
                        Button(
                            onClick = { printReceiptText(context, "Rekap_Pinjaman_Guru", bodyText) },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Print, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Unduh/Cetak")
                        }
                    }

                    OutlinedButton(
                        onClick = { showPrintSummaryDialog = false },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Tutup Preview")
                    }
                }
            }
        }
    }
}

@Composable
fun GroupedTeacherLoanCard(
    namaGuru: String,
    loansList: List<PinjamanGuru>,
    viewModel: TabunganViewModel,
    onPayCombinedClick: () -> Unit,
    onPayIndividualClick: (PinjamanGuru) -> Unit,
    onDeleteIndividualClick: (PinjamanGuru) -> Unit,
    onExportIndividualClick: () -> Unit
) {
    val schoolName by viewModel.schoolName.collectAsState()
    val treasurerName by viewModel.treasurerName.collectAsState()
    var expanded by remember { mutableStateOf(false) }
    var selectedLoanForReceipt by remember { mutableStateOf<PinjamanGuru?>(null) }
    val totalPinjam = loansList.sumOf { it.jumlahPinjam }
    val totalBayar = loansList.sumOf { it.jumlahBayar }
    val remaining = totalPinjam - totalBayar
    val activeCount = loansList.filter { !it.lunas }.size
    val isAllLunas = remaining <= 0.0

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isAllLunas) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f) else MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            // Header Row: Name & Status Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = namaGuru,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Box(
                    modifier = Modifier
                        .background(
                            if (isAllLunas) Color(0xFFE8F5E9) else Color(0xFFFFEBEE),
                            RoundedCornerShape(4.dp)
                        )
                        .padding(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = if (isAllLunas) "LUNAS" else "$activeCount AKTIF",
                        fontWeight = FontWeight.Bold,
                        fontSize = 9.sp,
                        color = if (isAllLunas) Color(0xFF2E7D32) else Color(0xFFC62828)
                    )
                }
            }

            Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.08f))

            // Overall financials
            Row(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Total Pinjam", fontSize = 10.sp, color = Color.Gray)
                    Text(viewModel.formatRupiah(totalPinjam), fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text("Total Dibayar", fontSize = 10.sp, color = Color.Gray)
                    Text(viewModel.formatRupiah(totalBayar), fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color(0xFF2E7D32))
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text("Sisa Hutang", fontSize = 10.sp, color = Color.Gray)
                    Text(viewModel.formatRupiah(remaining), fontWeight = FontWeight.Bold, fontSize = 12.sp, color = if (remaining > 0) Color(0xFFC62828) else Color(0xFF2E7D32))
                }
            }

            // Expanded/Collapsible details, or Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Rincian & Export actions
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(
                        onClick = { expanded = !expanded },
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                        modifier = Modifier.height(30.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(
                                if (expanded) "Sembunyikan" else "Rincian (${loansList.size})",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Icon(
                                imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }

                    IconButton(
                        onClick = onExportIndividualClick,
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.FileDownload,
                            contentDescription = "Ekspor Rincian Guru",
                            tint = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                // If not completely lunas, show bulk pay button
                if (!isAllLunas) {
                    Button(
                        onClick = onPayCombinedClick,
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 4.dp),
                        modifier = Modifier.height(30.dp)
                    ) {
                        Text("Bayar Sekaligus", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            // If expanded, show individual loan cards inside!
            if (expanded) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                            RoundedCornerShape(8.dp)
                        )
                        .padding(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        "Daftar Detail Pinjaman Pendidik:",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.outline
                    )

                    loansList.forEachIndexed { index, loan ->
                        val itemRemaining = loan.jumlahPinjam - loan.jumlahBayar
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                        ) {
                            Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(
                                            "Pinjaman #${index + 1}",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.sp,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            viewModel.formatDateShort(loan.tanggalPinjam),
                                            fontSize = 9.sp,
                                            color = Color.Gray
                                        )
                                    }

                                    Box(
                                        modifier = Modifier
                                            .background(
                                                if (loan.lunas) Color(0xFFE8F5E9) else Color(0xFFFFEBEE),
                                                RoundedCornerShape(3.dp)
                                            )
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            if (loan.lunas) "LUNAS" else "BELUM LUNAS",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 8.sp,
                                            color = if (loan.lunas) Color(0xFF2E7D32) else Color(0xFFC62828)
                                        )
                                    }
                                }

                                Row(modifier = Modifier.fillMaxWidth()) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text("Pinjam", fontSize = 9.sp, color = Color.Gray)
                                        Text(viewModel.formatRupiah(loan.jumlahPinjam), fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                    }
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text("Angsuran", fontSize = 9.sp, color = Color.Gray)
                                        Text(viewModel.formatRupiah(loan.jumlahBayar), fontWeight = FontWeight.Bold, fontSize = 11.sp, color = Color(0xFF2E7D32))
                                    }
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text("Sisa", fontSize = 9.sp, color = Color.Gray)
                                        Text(viewModel.formatRupiah(itemRemaining), fontWeight = FontWeight.Bold, fontSize = 11.sp, color = if (itemRemaining > 0) Color(0xFFC62828) else Color(0xFF2E7D32))
                                    }
                                }

                                if (!loan.lunas) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.End,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        IconButton(
                                            onClick = { onDeleteIndividualClick(loan) },
                                            modifier = Modifier.size(24.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Delete,
                                                contentDescription = "Hapus",
                                                tint = MaterialTheme.colorScheme.error,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }

                                        Spacer(modifier = Modifier.width(8.dp))

                                        IconButton(
                                            onClick = { selectedLoanForReceipt = loan },
                                            modifier = Modifier.size(24.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Print,
                                                contentDescription = "Cetak Slip",
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }

                                        Spacer(modifier = Modifier.width(8.dp))

                                        TextButton(
                                            onClick = { onPayIndividualClick(loan) },
                                            modifier = Modifier.height(24.dp),
                                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
                                        ) {
                                            Text("Bayar Cicilan", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                } else {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        loan.tanggalLunas?.let { tLunas ->
                                            Text(
                                                "Lunas: ${viewModel.formatDateShort(tLunas)}",
                                                fontSize = 9.sp,
                                                fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                                                color = Color.Gray
                                            )
                                        }
                                        IconButton(
                                            onClick = { selectedLoanForReceipt = loan },
                                            modifier = Modifier.size(24.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Print,
                                                contentDescription = "Cetak Slip",
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(16.dp)
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
    }

    selectedLoanForReceipt?.let { sLoan ->
        Dialog(onDismissRequest = { selectedLoanForReceipt = null }) {
            Card(
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                modifier = Modifier.padding(16.dp).fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("Simulasi Cetak Slip", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    
                    val sLoanRemaining = sLoan.jumlahPinjam - sLoan.jumlahBayar
                    val bodyText = 
                        "       BUKTI PINJAMAN KEUANGAN\n" +
                        "         ${schoolName.uppercase(Locale("id", "ID"))}\n" +
                        "=================================\n" +
                        "ID Slip  : TAL-TEA-${sLoan.id}\n" +
                        "Nama Guru: ${sLoan.namaGuru}\n" +
                        "Tanggal  : ${viewModel.formatDate(sLoan.tanggalPinjam)}\n" +
                        "---------------------------------\n" +
                        "Plafon   : ${viewModel.formatRupiah(sLoan.jumlahPinjam)}\n" +
                        "Angsuran : ${viewModel.formatRupiah(sLoan.jumlahBayar)}\n" +
                        "Sisa     : ${viewModel.formatRupiah(sLoanRemaining)}\n" +
                        "Status   : ${if (sLoan.lunas) "LUNAS SEPENUHNYAN" else "BELUM LUNAS"}\n" +
                        "---------------------------------\n" +
                        "Pemberitahu: $treasurerName\n" +
                        "\n" +
                        "Tanda Tangan Penerima / Guru:\n\n" +
                        "      ( ____________________ )\n" +
                        "=================================\n" +
                        "     [TERVERIFIKASI SISTEM]\n" +
                        "   Simulasi Printer POS-58mm"
                        
                    Box(
                        modifier = Modifier
                            .weight(1f, fill = false)
                            .heightIn(max = 240.dp)
                            .background(Color(0xFFF5F5F5), RoundedCornerShape(8.dp))
                            .verticalScroll(rememberScrollState())
                            .padding(12.dp)
                            .fillMaxWidth()
                    ) {
                        Text(
                            text = bodyText,
                            fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                            fontSize = 11.sp,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    
                    val context = LocalContext.current
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedButton(
                            onClick = { shareReceiptText(context, "Slip Pinjaman Guru", bodyText) },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Bagikan")
                        }
                        
                        Button(
                            onClick = { printReceiptText(context, "Slip_Pinjaman_Guru", bodyText) },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Print, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Unduh/Cetak")
                        }
                    }

                    OutlinedButton(
                        onClick = { selectedLoanForReceipt = null },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Tutup Preview")
                    }
                }
            }
        }
    }
}

@Composable
fun PaymentDialog(
    title: String,
    borrowerInfo: String,
    totalAmount: Double,
    paidAmount: Double,
    viewModel: TabunganViewModel,
    onDismiss: () -> Unit,
    onSubmitPayment: (Double) -> Unit
) {
    var bayarAmountInput by remember { mutableStateOf("") }
    var alertMsg by remember { mutableStateOf("") }
    val remaining = totalAmount - paidAmount

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(16.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier.padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(title, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = MaterialTheme.colorScheme.primary)
                Text("Atas Nama: $borrowerInfo", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Total Pinjam:", fontSize = 11.sp, color = Color.Gray)
                    Text(viewModel.formatRupiah(totalAmount), fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Sudah Dibayar:", fontSize = 11.sp, color = Color.Gray)
                    Text(viewModel.formatRupiah(paidAmount), fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color(0xFF2E7D32))
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Sisa Sisa Hutang:", fontSize = 11.sp, color = Color.Gray)
                    Text(viewModel.formatRupiah(remaining), fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color(0xFFC62828))
                }

                Divider()

                OutlinedTextField(
                    value = bayarAmountInput,
                    onValueChange = { bayarAmountInput = it },
                    label = { Text("Jumlah Angsuran (Rp)") },
                    modifier = Modifier.fillMaxWidth().testTag("payment_input_amount"),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true
                )

                if (alertMsg.isNotEmpty()) {
                    Text(alertMsg, color = MaterialTheme.colorScheme.error, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    // Quick pay full option
                    Button(
                        onClick = {
                            onSubmitPayment(remaining)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondaryContainer, contentColor = MaterialTheme.colorScheme.onSecondaryContainer),
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Bayar Lunas", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }

                    Button(
                        onClick = {
                            val bayarVal = bayarAmountInput.toDoubleOrNull() ?: 0.0
                            if (bayarVal <= 0.0) {
                                alertMsg = "Nominal pembayaran angsuran harus valid!"
                            } else if (bayarVal > remaining) {
                                alertMsg = "Nominal melebihi sisa pinjaman!"
                            } else {
                                onSubmitPayment(bayarVal)
                            }
                        },
                        modifier = Modifier.weight(1.2f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Repay", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }

                OutlinedButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(8.dp)) {
                    Text("Batal")
                }
            }
        }
    }
}

// ======================== KAS UTAMA COOPERATIVE SCREEN ========================

@Composable
fun SchoolCoopDonutChart(
    kasRiil: Double,
    pSekolah: Double,
    pGuru: Double,
    viewModel: TabunganViewModel,
    modifier: Modifier = Modifier
) {
    val total = (kasRiil + pSekolah + pGuru).coerceAtLeast(1.0)
    val sweepKas = (kasRiil / total) * 360f
    val sweepSekolah = (pSekolah / total) * 360f
    val sweepGuru = (pGuru / total) * 360f

    // Beautiful styling colors
    val colorKas = Color(0xFF2E7D32)      // Elegant Green
    val colorSekolah = Color(0xFF1565C0)  // Deep Blue
    val colorGuru = Color(0xFFAD1457)     // Warm Burgundy
    
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                "Proporsi Aset Koperasi Sekolah", 
                fontWeight = FontWeight.Bold, 
                fontSize = 14.sp, 
                color = MaterialTheme.colorScheme.primary
            )
            
            Box(
                modifier = Modifier.size(160.dp),
                contentAlignment = Alignment.Center
            ) {
                androidx.compose.foundation.Canvas(modifier = Modifier.fillMaxSize()) {
                    val strokeWidth = 24.dp.toPx()
                    val sizeMin = size.minDimension - strokeWidth
                    val startAngle = -90f
                    
                    // Fallback empty circle
                    if (kasRiil == 0.0 && pSekolah == 0.0 && pGuru == 0.0) {
                        drawArc(
                            color = Color.Gray.copy(alpha = 0.3f),
                            startAngle = startAngle,
                            sweepAngle = 360f,
                            useCenter = false,
                            style = androidx.compose.ui.graphics.drawscope.Stroke(width = strokeWidth),
                            size = androidx.compose.ui.geometry.Size(sizeMin, sizeMin),
                            topLeft = androidx.compose.ui.geometry.Offset(strokeWidth / 2, strokeWidth / 2)
                        )
                    } else {
                        // Draw Kas Riil
                        if (sweepKas > 0) {
                            drawArc(
                                color = colorKas,
                                startAngle = startAngle,
                                sweepAngle = sweepKas.toFloat(),
                                useCenter = false,
                                style = androidx.compose.ui.graphics.drawscope.Stroke(width = strokeWidth, cap = androidx.compose.ui.graphics.StrokeCap.Round),
                                size = androidx.compose.ui.geometry.Size(sizeMin, sizeMin),
                                topLeft = androidx.compose.ui.geometry.Offset(strokeWidth / 2, strokeWidth / 2)
                            )
                        }
                        
                        // Draw P Sekolah
                        if (sweepSekolah > 0) {
                            drawArc(
                                color = colorSekolah,
                                startAngle = startAngle + sweepKas.toFloat(),
                                sweepAngle = sweepSekolah.toFloat(),
                                useCenter = false,
                                style = androidx.compose.ui.graphics.drawscope.Stroke(width = strokeWidth, cap = androidx.compose.ui.graphics.StrokeCap.Round),
                                size = androidx.compose.ui.geometry.Size(sizeMin, sizeMin),
                                topLeft = androidx.compose.ui.geometry.Offset(strokeWidth / 2, strokeWidth / 2)
                            )
                        }
                        
                        // Draw P Guru
                        if (sweepGuru > 0) {
                            drawArc(
                                color = colorGuru,
                                startAngle = startAngle + sweepKas.toFloat() + sweepSekolah.toFloat(),
                                sweepAngle = sweepGuru.toFloat(),
                                useCenter = false,
                                style = androidx.compose.ui.graphics.drawscope.Stroke(width = strokeWidth, cap = androidx.compose.ui.graphics.StrokeCap.Round),
                                size = androidx.compose.ui.geometry.Size(sizeMin, sizeMin),
                                topLeft = androidx.compose.ui.geometry.Offset(strokeWidth / 2, strokeWidth / 2)
                            )
                        }
                    }
                }
                
                // Text in center of Donut
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Total Aset", fontSize = 9.sp, color = MaterialTheme.colorScheme.outline)
                    Text(
                        formatShortRupiah(kasRiil + pSekolah + pGuru), 
                        fontWeight = FontWeight.Black, 
                        fontSize = 15.sp, 
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
            
            // Legend
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                LegendRow(color = colorKas, label = "Kas Riil Tunai", value = kasRiil, total = total, viewModel = viewModel)
                LegendRow(color = colorSekolah, label = "Piutang Sekolah", value = pSekolah, total = total, viewModel = viewModel)
                LegendRow(color = colorGuru, label = "Piutang Guru", value = pGuru, total = total, viewModel = viewModel)
            }
        }
    }
}

fun formatShortRupiah(value: Double): String {
    return when {
        value >= 1_000_000_000 -> String.format("Rp %.1f M", value / 1_000_000_000)
        value >= 1_000_000 -> String.format("Rp %.1f Jt", value / 1_000_000)
        value >= 1_000 -> String.format("Rp %.1f Rb", value / 1_000)
        else -> String.format("Rp %.0f", value)
    }
}

@Composable
fun LegendRow(color: Color, label: String, value: Double, total: Double, viewModel: TabunganViewModel) {
    val percentage = if (total > 0) (value / total) * 100 else 0.0
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .clip(CircleShape)
                    .background(color)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(label, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface)
        }
        Text(
            "${String.format("%.1f", percentage)}% (${viewModel.formatRupiah(value)})",
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun KasUtamaScreen(viewModel: TabunganViewModel) {
    val context = LocalContext.current
    val currentRole by viewModel.currentRole.collectAsState()
    val adminKasLocal by viewModel.adminKasList.collectAsState()
    
    val totalTabunganSiswa by viewModel.totalTabunganSiswa.collectAsState()
    val outstandingPinjamanSekolah by viewModel.outstandingPinjamanSekolah.collectAsState()
    val outstandingPinjamanGuru by viewModel.outstandingPinjamanGuru.collectAsState()
    val totalPendapatanAdmin by viewModel.totalPendapatanAdmin.collectAsState()
    val saldoKasRiil by viewModel.saldoKasRiil.collectAsState()
    val totalSetorKoperasi by viewModel.totalSetorKoperasi.collectAsState()
    val saldoBendahara by viewModel.saldoBendahara.collectAsState()

    var showLocalExportSuccess by remember { mutableStateOf(false) }
    var showLocalExportError by remember { mutableStateOf(false) }

    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/csv")
    ) { uri ->
        uri?.let {
            val success = viewModel.writeAdminKasCsvToUri(context, uri)
            if (success) {
                showLocalExportSuccess = true
            } else {
                showLocalExportError = true
            }
        }
    }

    var expandForm by remember { mutableStateOf(false) }
    var nominalInput by remember { mutableStateOf("") }
    var keteranganInput by remember { mutableStateOf("") }
    var tipeSelected by remember { mutableStateOf("PENGELUARAN_OPERASIONAL") } // or "MODAL_AWAL"
    var formError by remember { mutableStateOf("") }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .testTag("kas_utama_container"),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        "Buku Kas Utama",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        "Alokasi Dana & Log Kas Koperasi Sekolah",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                }

                Button(
                    onClick = {
                        exportLauncher.launch("Buku_Kas_Utama_${System.currentTimeMillis()}.csv")
                    },
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Icon(Icons.Default.Print, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Cetak CSV", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        // Summary Cards Grid
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                // Main Cash in hand card showing saldoBendahara (Uang di Tangan/Laci Bendahara)
                Card(
                    modifier = Modifier.fillMaxWidth().testTag("real_cash_in_hand_card"),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text("Sisa Uang Tunai di Tangan Bendahara (Pegangan Fisik)", fontSize = 11.sp, color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f))
                        Text(
                            viewModel.formatRupiah(saldoBendahara),
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Black,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            "Formula: (Total Tabungan + Admin - Piutang Berjalan) - Setoran Keluar",
                            fontSize = 9.sp,
                            fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.6f)
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Card(
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.8f))
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text("Total Kas Riil Sistem", fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(
                                viewModel.formatRupiah(saldoKasRiil),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Card(
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.8f))
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text("Disetor ke Bank/Kepala", fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(
                                viewModel.formatRupiah(totalSetorKoperasi),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF2E7D32)
                            )
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Card(
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.7f))
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text("Total Tabungan Siswa", fontSize = 9.sp, color = MaterialTheme.colorScheme.onSecondaryContainer)
                            Text(
                                viewModel.formatRupiah(totalTabunganSiswa),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        }
                    }

                    Card(
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.7f))
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text("Laba Jasa/Admin Kas", fontSize = 9.sp, color = MaterialTheme.colorScheme.onTertiaryContainer)
                            Text(
                                viewModel.formatRupiah(totalPendapatanAdmin),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onTertiaryContainer
                            )
                        }
                    }
                }
            }
        }

        // Donut Chart Item
        item {
            SchoolCoopDonutChart(
                kasRiil = saldoKasRiil,
                pSekolah = outstandingPinjamanSekolah,
                pGuru = outstandingPinjamanGuru,
                viewModel = viewModel,
                modifier = Modifier.fillMaxWidth()
            )
        }

        // Bendahara Booking Input Form
        if (currentRole == UserRole.BENDAHARA) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { expandForm = !expandForm },
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Default.Edit, 
                                    contentDescription = null, 
                                    tint = MaterialTheme.colorScheme.primary, 
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Pencatatan Buku Kas Baru", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            }
                            Text(
                                if (expandForm) "Sembunyikan" else "Buka Form",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.SemiBold
                            )
                        }

                        if (expandForm) {
                            Spacer(modifier = Modifier.height(14.dp))
                            
                            // Select Tipe
                            Text("Pilih Jenis Aliran Kas:", fontSize = 11.sp, color = MaterialTheme.colorScheme.outline)
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Button(
                                    onClick = { tipeSelected = "PENGELUARAN_OPERASIONAL" },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (tipeSelected == "PENGELUARAN_OPERASIONAL") Color(0xFFC62828) else MaterialTheme.colorScheme.surfaceVariant,
                                        contentColor = if (tipeSelected == "PENGELUARAN_OPERASIONAL") Color.White else MaterialTheme.colorScheme.onSurface
                                    ),
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(8.dp),
                                    contentPadding = PaddingValues(0.dp)
                                ) {
                                    Text("Operasional (Keluar)", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                }

                                Button(
                                    onClick = { tipeSelected = "MODAL_AWAL" },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (tipeSelected == "MODAL_AWAL") Color(0xFF2E7D32) else MaterialTheme.colorScheme.surfaceVariant,
                                        contentColor = if (tipeSelected == "MODAL_AWAL") Color.White else MaterialTheme.colorScheme.onSurface
                                    ),
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(8.dp),
                                    contentPadding = PaddingValues(0.dp)
                                ) {
                                    Text("Suntik Modal (Masuk)", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            OutlinedTextField(
                                value = nominalInput,
                                onValueChange = { nominalInput = it },
                                label = { Text("Jumlah Nominal (Rp)") },
                                modifier = Modifier.fillMaxWidth().testTag("kas_input_amount"),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                singleLine = true
                            )

                            Spacer(modifier = Modifier.height(10.dp))

                            OutlinedTextField(
                                value = keteranganInput,
                                onValueChange = { keteranganInput = it },
                                label = { Text("Keterangan Penjelasan") },
                                modifier = Modifier.fillMaxWidth().testTag("kas_input_remarks"),
                                singleLine = true
                            )

                            if (formError.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(formError, color = MaterialTheme.colorScheme.error, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            Button(
                                onClick = {
                                    val amount = nominalInput.toDoubleOrNull() ?: 0.0
                                    val desc = keteranganInput.trim()
                                    if (amount <= 0.0) {
                                        formError = "Masukan nominal jumlah uang yang valid!"
                                    } else if (desc.isEmpty()) {
                                        formError = "Keterangan penulisan catatan kas wajib diisi!"
                                    } else {
                                        formError = ""
                                        viewModel.addAdminKas(tipeSelected, amount, desc) {
                                            nominalInput = ""
                                            keteranganInput = ""
                                            expandForm = false
                                        }
                                    }
                                },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text("Simpan Transaksi Kas")
                            }
                        }
                    }
                }
            }
        }

        // Log list Title
        item {
            Text(
                "Riwayat Riil Aliran Buku Kas",
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // List elements
        if (adminKasLocal.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f))
                ) {
                    Box(modifier = Modifier.padding(24.dp).fillMaxWidth(), contentAlignment = Alignment.Center) {
                        Text("Belum ada pencatatan kas utama koperasi.", color = MaterialTheme.colorScheme.outline, fontSize = 12.sp, textAlign = TextAlign.Center)
                    }
                }
            }
        } else {
            items(adminKasLocal.sortedByDescending { it.tanggal }) { row ->
                val dateStr = SimpleDateFormat("dd-MM-yyyy HH:mm", Locale("id", "ID")).format(Date(row.tanggal))
                
                val flowColor = when (row.tipe) {
                    "MODAL_AWAL", "PENDAPATAN_ADMIN" -> Color(0xFF2E7D32)
                    else -> Color(0xFFC62828)
                }

                val typeLabel = when (row.tipe) {
                    "MODAL_AWAL" -> "MODAL MASUK"
                    "PENDAPATAN_ADMIN" -> "PENDAPATAN ADMIN"
                    "PENGELUARAN_OPERASIONAL" -> "OPERASIONAL KELUAR"
                    else -> row.tipe
                }

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .clip(CircleShape)
                                        .background(flowColor)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(typeLabel, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = flowColor)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(dateStr, fontSize = 9.sp, color = Color.Gray)
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(row.keterangan, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                        }

                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            val sign = if (row.tipe == "MODAL_AWAL" || row.tipe == "PENDAPATAN_ADMIN") "+" else "-"
                            Text(
                                "$sign ${viewModel.formatRupiah(row.jumlah)}",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = flowColor
                            )

                            // Only Bendahara can delete manually registered items (not the automated transaction fees)
                            if (currentRole == UserRole.BENDAHARA && row.tipe != "PENDAPATAN_ADMIN") {
                                IconButton(
                                    onClick = { viewModel.deleteAdminKas(row.id) },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(
                                        Icons.Default.Delete, 
                                        contentDescription = "Hapus", 
                                        tint = MaterialTheme.colorScheme.error, 
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Export Feedback dialogs
    if (showLocalExportSuccess) {
        AlertDialog(
            onDismissRequest = { showLocalExportSuccess = false },
            title = { Text("Export Berhasil!") },
            text = { Text("Buku Kas Utama sukses diexport dalam format CSV kompatibel Excel.") },
            confirmButton = {
                Button(onClick = { showLocalExportSuccess = false }) { Text("Tutup") }
            }
        )
    }

    if (showLocalExportError) {
        AlertDialog(
            onDismissRequest = { showLocalExportError = false },
            title = { Text("Export Gagal!") },
            text = { Text("Gagal menyimpan file CSV Laporan Kas. Coba ulangi dengan perizinan yang benar.") },
            confirmButton = {
                Button(onClick = { showLocalExportError = false }) { Text("Tutup") }
            }
        )
    }
}

// ======================== SIMULASI & KALKULATOR KOPERASI SCREEN ========================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SimulasiScreen(viewModel: TabunganViewModel) {
    var selectedSimulasiTab by remember { mutableStateOf(0) }
    val tabTitles = listOf("Celengan Impian", "Kalkulator Pinjaman")
    val tabIcons = listOf(Icons.Default.Star, Icons.Default.AccountBalanceWallet)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .testTag("simulasi_screen_container")
    ) {
        TabRow(
            selectedTabIndex = selectedSimulasiTab,
            containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp)
        ) {
            tabTitles.forEachIndexed { index, title ->
                Tab(
                    selected = selectedSimulasiTab == index,
                    onClick = { selectedSimulasiTab = index },
                    icon = { Icon(tabIcons[index], contentDescription = title) },
                    text = { Text(title, fontSize = 11.sp, fontWeight = FontWeight.Bold) }
                )
            }
        }

        when (selectedSimulasiTab) {
            0 -> CelenganImpianTab(viewModel)
            1 -> KalkulatorPinjamanTab(viewModel)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CelenganImpianTab(viewModel: TabunganViewModel) {
    val siswaList by viewModel.siswaList.collectAsState()
    val siswaBalances by viewModel.siswaBalances.collectAsState()

    var selectedSiswa by remember { mutableStateOf<Siswa?>(null) }
    var userDropdownExpanded by remember { mutableStateOf(false) }

    var targetName by remember { mutableStateOf("Membeli Perlengkapan Sekolah") }
    var targetAmountInput by remember { mutableStateOf("500000") }
    var tenorInput by remember { mutableStateOf("10") }
    var selectedInterval by remember { mutableStateOf("Mingguan") } // "Harian", "Mingguan", "Bulanan"

    val currentSiswaBalance = selectedSiswa?.let { siswaBalances[it.id] } ?: 0.0

    val targetAmount = targetAmountInput.toDoubleOrNull() ?: 0.0
    val tenor = (tenorInput.toDoubleOrNull() ?: 1.0).coerceAtLeast(1.0)

    val kekurangan = (targetAmount - currentSiswaBalance).coerceAtLeast(0.0)
    val setoranBerkala = if (targetAmount > 0.1) kekurangan / tenor else 0.0
    val progressPercent = if (targetAmount > 0.1) {
        (currentSiswaBalance / targetAmount).coerceAtMost(1.0)
    } else 0.0

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Column {
                Text(
                    "Celengan Impian Siswa",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    "Simulasi target menabung mandiri untuk mencapai cita-cita nasabah.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline
                )
            }
        }

        // Section: Select Siswa
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        "1. Pilih Nasabah (Siswa) - Opsional",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        "Hubungkan tabungan riil nasabah saat ini untuk menghitung kalkulasi sisa pencapaian.",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.outline
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    Box(modifier = Modifier.fillMaxWidth()) {
                        OutlinedButton(
                            onClick = { userDropdownExpanded = true },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = selectedSiswa?.let { "${it.nama} (Kelas ${it.kelas})" } ?: "Mulai dari Nol (Tanpa Saldo)",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Icon(
                                    imageVector = Icons.Default.ArrowDropDown,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.outline
                                )
                            }
                        }

                        DropdownMenu(
                            expanded = userDropdownExpanded,
                            onDismissRequest = { userDropdownExpanded = false },
                            modifier = Modifier.fillMaxWidth(0.9f)
                        ) {
                            DropdownMenuItem(
                                text = { Text("Mulai dari Nol (Tanpa Saldo Awal)", fontSize = 12.sp) },
                                onClick = {
                                    selectedSiswa = null
                                    userDropdownExpanded = false
                                }
                            )
                            siswaList.forEach { siswa ->
                                val bal = siswaBalances[siswa.id] ?: 0.0
                                DropdownMenuItem(
                                    text = { Text("${siswa.nama} (Kelas ${siswa.kelas}) - Saldo: ${viewModel.formatRupiah(bal)}", fontSize = 12.sp) },
                                    onClick = {
                                        selectedSiswa = siswa
                                        userDropdownExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    if (selectedSiswa != null) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "Saldo Aktif Riil Siswa: " + viewModel.formatRupiah(currentSiswaBalance),
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            color = Color(0xFF2E7D32)
                        )
                    }
                }
            }
        }

        // Section: Dream configuration
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        "2. Deskripsi & Target Finansial",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    OutlinedTextField(
                        value = targetName,
                        onValueChange = { targetName = it },
                        label = { Text("Tuliskan Impian (e.g. Membeli Sepeda, Karyawisata)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedTextField(
                            value = targetAmountInput,
                            onValueChange = { targetAmountInput = it },
                            label = { Text("Target Dana (Rp)") },
                            modifier = Modifier.weight(1.3f),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true
                        )

                        OutlinedTextField(
                            value = tenorInput,
                            onValueChange = { tenorInput = it },
                            label = { Text("Jangka Waktu") },
                            modifier = Modifier.weight(0.9f),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true
                        )
                    }

                    // Select interval frequency
                    Text("Frekuensi Rencana Setoran Menabung:", fontSize = 11.sp, color = MaterialTheme.colorScheme.outline)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val intervals = listOf("Harian", "Mingguan", "Bulanan")
                        intervals.forEach { interval ->
                            val isSel = selectedInterval == interval
                            Button(
                                onClick = { selectedInterval = interval },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (isSel) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                                    contentColor = if (isSel) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                                ),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.weight(1f),
                                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 6.dp)
                            ) {
                                Text(interval, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }

        // Section: Visual Results Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        "✨ Hasil Analisis Rencana Impian",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.primary
                    )

                    // Large Target Title
                    Text(
                        "Target: \"${targetName.ifEmpty { "Cita-cita Mulia" }}\"",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )

                    // Progress indicators
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "Progress Tabungan Aktif",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                            )
                            Text(
                                "${String.format("%.1f", progressPercent * 100)}%",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Black,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }

                        LinearProgressIndicator(
                            progress = progressPercent.toFloat(),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(10.dp)
                                .clip(RoundedCornerShape(5.dp)),
                            color = MaterialTheme.colorScheme.primary,
                            trackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                        )
                    }

                    // Balance stats comparison row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Dana Terkumpul (Saldo)", fontSize = 9.sp, color = Color.Gray)
                            Text(viewModel.formatRupiah(currentSiswaBalance), fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Kekurangan Sisa Dana", fontSize = 9.sp, color = Color.Gray)
                            Text(viewModel.formatRupiah(kekurangan), fontWeight = FontWeight.Bold, fontSize = 13.sp, color = if (kekurangan > 0) Color(0xFFC62828) else Color(0xFF2E7D32))
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Nilai Target Dana", fontSize = 9.sp, color = Color.Gray)
                            Text(viewModel.formatRupiah(targetAmount), fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }
                    }

                    Divider(color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.12f))

                    // Advice Box
                    if (kekurangan <= 0.0 && targetAmount > 0) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFFE8F5E9))
                                .padding(10.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = Color(0xFF2E7D32),
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    "Luar Biasa! Saldo Tabungan " + (selectedSiswa?.nama ?: "") + " sudah melampaui nilai target impian! Impian ini siap diwujudkan sekarang! 🎉",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF1B5E20)
                                )
                            }
                        }
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(
                                "Rekomendasi Setoran Menabung:",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.outline
                            )
                            Text(
                                "${viewModel.formatRupiah(setoranBerkala)} / ${selectedInterval.lowercase().replace("an", "")}",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Black,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                "Cukup sisihkan ${viewModel.formatRupiah(setoranBerkala)} setiap ${selectedInterval.lowercase().replace("an", "")} selama ${tenor.toInt()} ${selectedInterval.lowercase().replace("an", "")}, maka target pembelian \"${targetName}\" akan terwujud sepenuhnya! Semangat menabung!",
                                fontSize = 11.sp,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KalkulatorPinjamanTab(viewModel: TabunganViewModel) {
    var loanAmountInput by remember { mutableStateOf("2000000") }
    var tenorMonthsInput by remember { mutableStateOf("12") }
    var interestRateInput by remember { mutableStateOf("1.5") }
    var bungaTypeSelected by remember { mutableStateOf("FLAT") } // "FLAT", "MENURUN", "TANPA_BUNGA"

    val loanAmount = loanAmountInput.toDoubleOrNull() ?: 0.0
    val tenorMonths = (tenorMonthsInput.toIntOrNull() ?: 1).coerceAtLeast(1)
    val interestRate = interestRateInput.toDoubleOrNull() ?: 0.0

    // Calculations
    val totalPokokBulan = loanAmount / tenorMonths

    val schedule = remember(loanAmount, tenorMonths, interestRate, bungaTypeSelected) {
        val rows = mutableListOf<com.example.ui.components.InstallmentDetailRow>()
        var sisa = loanAmount
        val rateFraction = interestRate / 100.0

        for (m in 1..tenorMonths) {
            val pokok = totalPokokBulan
            val bunga = when (bungaTypeSelected) {
                "FLAT" -> loanAmount * rateFraction
                "MENURUN" -> sisa * rateFraction
                else -> 0.0
            }
            val total = pokok + bunga
            sisa = (sisa - pokok).coerceAtLeast(0.0)
            rows.add(
                com.example.ui.components.InstallmentDetailRow(
                    bulan = m,
                    pokok = pokok,
                    bunga = bunga,
                    total = total,
                    sisa = sisa
                )
            )
        }
        rows
    }

    val totalBungaKeseluruhan = schedule.sumOf { it.bunga }
    val totalPalingBanyakDibayar = loanAmount + totalBungaKeseluruhan
    val angsuranPertamaText = schedule.firstOrNull()?.let { viewModel.formatRupiah(it.total) } ?: "Rp 0"
    val angsuranTerakhirText = schedule.lastOrNull()?.let { viewModel.formatRupiah(it.total) } ?: "Rp 0"

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Column {
                Text(
                    "Simulasi Angsuran Pinjaman",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    "Simulasi kalkulasi pinjaman Simpan Pinjam Sekolah secara akurat.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline
                )
            }
        }

        // Section Inputs
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        "1. Parameter Pengajuan Pinjaman",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    OutlinedTextField(
                        value = loanAmountInput,
                        onValueChange = { loanAmountInput = it },
                        label = { Text("Jumlah Nilai Pinjaman (Rp)") },
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedTextField(
                            value = tenorMonthsInput,
                            onValueChange = { tenorMonthsInput = it },
                            label = { Text("Tenor Jangka (Bulan)") },
                            modifier = Modifier.weight(1f),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true
                        )

                        OutlinedTextField(
                            value = interestRateInput,
                            onValueChange = { interestRateInput = it },
                            label = { Text("Suku Jasa/Bunga (%)") },
                            modifier = Modifier.weight(1.1f),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            enabled = bungaTypeSelected != "TANPA_BUNGA"
                        )
                    }

                    Text("Pilih Jenis Metode Perhitungan Bunga:", fontSize = 11.sp, color = MaterialTheme.colorScheme.outline)
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        val methods = listOf(
                            "FLAT" to "Datar (Flat)",
                            "MENURUN" to "Menurun (Efek)",
                            "TANPA_BUNGA" to "Syariah"
                        )
                        methods.forEach { (code, label) ->
                            val isSel = bungaTypeSelected == code
                            Button(
                                onClick = { bungaTypeSelected = code },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (isSel) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                                    contentColor = if (isSel) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                                ),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.weight(1f),
                                contentPadding = PaddingValues(horizontal = 2.dp, vertical = 6.dp)
                            ) {
                                Text(label, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }

        // Section Outputs Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.4f)),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        "📊 Hasil Simulasi Finansial Pinjaman",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.secondary
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Plafon Pinjaman Pokok", fontSize = 10.sp, color = Color.Gray)
                            Text(viewModel.formatRupiah(loanAmount), fontSize = 15.sp, fontWeight = FontWeight.Bold)
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text("Total Suku Jasa/Bunga", fontSize = 10.sp, color = Color.Gray)
                            Text(viewModel.formatRupiah(totalBungaKeseluruhan), fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color(0xFFAD1457))
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Tenor Waktu Pengembalian", fontSize = 10.sp, color = Color.Gray)
                            Text("$tenorMonths Bulan", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text("Total Pengembalian Bulat", fontSize = 10.sp, color = Color.Gray)
                            Text(viewModel.formatRupiah(totalPalingBanyakDibayar), fontSize = 16.sp, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.onSecondaryContainer)
                        }
                    }

                    Divider(color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.12f))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Angsuran Pokok / Bulan", fontSize = 10.sp, color = Color.Gray)
                            Text(viewModel.formatRupiah(totalPokokBulan), fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                        }
                        
                        Column(horizontalAlignment = Alignment.End) {
                            Text(if (bungaTypeSelected == "MENURUN") "Besar Angsuran (Mulai ~ Akhir)" else "Total Angsuran Sebulan", fontSize = 10.sp, color = Color.Gray)
                            val displayCicilan = if (bungaTypeSelected == "MENURUN") {
                                "$angsuranPertamaText s/d $angsuranTerakhirText"
                            } else {
                                angsuranPertamaText
                            }
                            Text(displayCicilan, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
            }
        }

        // Section schedule details table
        item {
            Text(
                "📋 Proyeksi Skema Cicilan Bulanan",
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        if (loanAmount <= 0) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f))
                ) {
                    Box(modifier = Modifier.padding(16.dp).fillMaxWidth(), contentAlignment = Alignment.Center) {
                        Text("Masukan nominal jumlah pinjaman untuk melihat proyeksi cicilan.", color = Color.Gray, fontSize = 11.sp, textAlign = TextAlign.Center)
                    }
                }
            }
        } else {
            // Header table rows
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f))
                        .padding(vertical = 8.dp, horizontal = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Bln ke-", modifier = Modifier.weight(0.7f), fontSize = 10.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                    Text("Pokok (Rp)", modifier = Modifier.weight(1.3f), fontSize = 10.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.End)
                    Text("Jasa/Bung(Rp)", modifier = Modifier.weight(1.3f), fontSize = 10.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.End)
                    Text("Tot/Cicil(Rp)", modifier = Modifier.weight(1.5f), fontSize = 10.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.End)
                    Text("Sisa Pla(Rp)", modifier = Modifier.weight(1.5f), fontSize = 10.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.End)
                }
            }

            items(schedule) { r ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp, horizontal = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("#${r.bulan}", modifier = Modifier.weight(0.7f), fontSize = 10.sp, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center)
                    Text(viewModel.formatRupiah(r.pokok).replace("Rp ", ""), modifier = Modifier.weight(1.3f), fontSize = 10.sp, textAlign = TextAlign.End)
                    Text(viewModel.formatRupiah(r.bunga).replace("Rp ", ""), modifier = Modifier.weight(1.3f), fontSize = 10.sp, color = if (r.bunga > 0) Color(0xFFAD1457) else Color.Gray, textAlign = TextAlign.End)
                    Text(viewModel.formatRupiah(r.total).replace("Rp ", ""), modifier = Modifier.weight(1.5f), fontSize = 10.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary, textAlign = TextAlign.End)
                    Text(viewModel.formatRupiah(r.sisa).replace("Rp ", ""), modifier = Modifier.weight(1.5f), fontSize = 10.sp, textAlign = TextAlign.End)
                }
            }
        }
    }
}

data class InstallmentDetailRow(
    val bulan: Int,
    val pokok: Double,
    val bunga: Double,
    val total: Double,
    val sisa: Double
)

// imports required for signature pad drawings
@Composable
fun SignaturePad(
    clearTrigger: Int,
    onSignatureChange: (String) -> Unit,
    modifier: java.lang.String? = null, // unused placeholder but helps match signature
    customModifier: androidx.compose.ui.Modifier = androidx.compose.ui.Modifier
) {
    val currentStroke = remember { mutableStateListOf<Offset>() }
    val allStrokes = remember { mutableStateListOf<List<Offset>>() }

    LaunchedEffect(clearTrigger) {
        if (clearTrigger > 0) {
            allStrokes.clear()
            currentStroke.clear()
        }
    }

    Box(
        modifier = customModifier
            .background(Color.White, RoundedCornerShape(8.dp))
            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
            .pointerInput(allStrokes, currentStroke) {
                detectDragGestures(
                    onDragStart = { offset: Offset ->
                        currentStroke.clear()
                        currentStroke.add(offset)
                    },
                    onDragEnd = {
                        if (currentStroke.isNotEmpty()) {
                            allStrokes.add(currentStroke.toList())
                            currentStroke.clear()
                            val serialized = allStrokes.joinToString("|") { stroke ->
                                stroke.joinToString(";") { "${it.x},${it.y}" }
                            }
                            onSignatureChange(serialized)
                        }
                    },
                    onDragCancel = {
                        currentStroke.clear()
                    },
                    onDrag = { change: PointerInputChange, _ ->
                        currentStroke.add(change.position)
                    }
                )
            }
    ) {
        Canvas(modifier = androidx.compose.ui.Modifier.fillMaxSize()) {
            allStrokes.forEach { stroke ->
                if (stroke.size > 1) {
                    val path = Path().apply {
                        moveTo(stroke[0].x, stroke[0].y)
                        for (i in 1 until stroke.size) {
                            lineTo(stroke[i].x, stroke[i].y)
                        }
                    }
                    drawPath(
                        path, 
                        color = Color.Black, 
                        style = Stroke(
                            width = 4f, 
                            cap = StrokeCap.Round, 
                            join = StrokeJoin.Round
                        )
                    )
                }
            }
            if (currentStroke.size > 1) {
                val path = Path().apply {
                    moveTo(currentStroke[0].x, currentStroke[0].y)
                    for (i in 1 until currentStroke.size) {
                        lineTo(currentStroke[i].x, currentStroke[i].y)
                    }
                }
                drawPath(
                    path, 
                    color = Color.Black, 
                    style = Stroke(
                        width = 4f, 
                        cap = StrokeCap.Round, 
                        join = StrokeJoin.Round
                    )
                )
            }
        }
    }
}

@Composable
fun SignatureThumbnail(
    serialized: String,
    modifier: androidx.compose.ui.Modifier = androidx.compose.ui.Modifier
) {
    Canvas(modifier = modifier.background(Color.White.copy(alpha = 0.8f), RoundedCornerShape(4.dp))) {
        val strokes = serialized.split("|").filter { it.isNotEmpty() }
        val allPoints = mutableListOf<Offset>()
        val strokeList = strokes.map { strokeStr ->
            strokeStr.split(";").mapNotNull { ptStr ->
                val parts = ptStr.split(",")
                if (parts.size == 2) {
                    val x = parts[0].toFloatOrNull()
                    val y = parts[1].toFloatOrNull()
                    if (x != null && y != null) {
                        val off = Offset(x, y)
                        allPoints.add(off)
                        off
                    } else null
                } else null
            }
        }.filter { it.size > 1 }

        if (allPoints.isNotEmpty()) {
            val minX = allPoints.minOf { it.x }
            val maxX = allPoints.maxOf { it.x }
            val minY = allPoints.minOf { it.y }
            val maxY = allPoints.maxOf { it.y }

            val widthPoints = maxX - minX
            val heightPoints = maxY - minY

            val scaleX = if (widthPoints > 0) (size.width * 0.85f) / widthPoints else 1f
            val scaleY = if (heightPoints > 0) (size.height * 0.85f) / heightPoints else 1f
            val scale = minOf(scaleX, scaleY)

            val offsetX = (size.width - widthPoints * scale) / 2f - minX * scale
            val offsetY = (size.height - heightPoints * scale) / 2f - minY * scale

            strokeList.forEach { points ->
                val path = Path().apply {
                    moveTo(points[0].x * scale + offsetX, points[0].y * scale + offsetY)
                    for (i in 1 until points.size) {
                        lineTo(points[i].x * scale + offsetX, points[i].y * scale + offsetY)
                    }
                }
                drawPath(
                    path = path,
                    color = Color(0xFF1976D2), // Pen blue
                    style = Stroke(
                        width = 3.5f, 
                        cap = StrokeCap.Round, 
                        join = StrokeJoin.Round
                    )
                )
            }
        }
    }
}

@Composable
fun SetorCardItem(
    setor: SetorKoperasi,
    viewModel: TabunganViewModel,
    isDeletable: Boolean
) {
    val schoolName by viewModel.schoolName.collectAsState()
    val treasurerName by viewModel.treasurerName.collectAsState()
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var showPrintDialog by remember { mutableStateOf(false) }
    val dateFormat = SimpleDateFormat("EEEE, dd MMM yyyy HH:mm", Locale("id", "ID"))

    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(Color(0xFFE8F5E9), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Send,
                    contentDescription = null,
                    tint = Color(0xFF2E7D32),
                    modifier = Modifier.size(20.dp)
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    setor.penerima,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
                Text(
                    dateFormat.format(Date(setor.tanggal)),
                    fontSize = 11.sp,
                    color = Color.Gray
                )
                Text(
                    "Setoran ke Koperasi / Bank",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Medium
                )
            }

            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    viewModel.formatRupiah(setor.jumlah),
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = Color(0xFF2E7D32)
                )

                if (!setor.ttdOnline.isNullOrEmpty()) {
                    Box(
                        modifier = Modifier
                            .width(64.dp)
                            .height(36.dp)
                            .border(1.dp, Color.LightGray, RoundedCornerShape(4.dp))
                    ) {
                        SignatureThumbnail(
                            serialized = setor.ttdOnline!!,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                } else {
                    Text(
                        "Tanpa TTD",
                        fontSize = 10.sp,
                        color = Color.LightGray,
                        style = MaterialTheme.typography.labelSmall
                    )
                }
            }

            IconButton(onClick = { showPrintDialog = true }) {
                Icon(
                    imageVector = Icons.Default.Print,
                    contentDescription = "Cetak",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
            }

            if (isDeletable) {
                IconButton(onClick = { showDeleteConfirm = true }) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Hapus",
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Hapus Catatan Setor", fontWeight = FontWeight.Bold) },
            text = { Text("Apakah Anda yakin ingin menghapus catatan setoran tabungan ini? Nominal saldo bendahara akan dikembalikan seperti semula.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteSetorKoperasi(setor.id)
                        showDeleteConfirm = false
                    }
                ) {
                    Text("Hapus", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text("Batal")
                }
            }
        )
    }

    if (showPrintDialog) {
        Dialog(onDismissRequest = { showPrintDialog = false }) {
            Card(
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                modifier = Modifier.padding(16.dp).fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("Simulasi Cetak Slip", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    
                    val bText = 
                        "       SLIP SETORAN TUNAI KAS\n" +
                        "         ${schoolName.uppercase(Locale("id", "ID"))}\n" +
                        "     BENDAHARA KE REKENING/BANK\n" +
                        "=================================\n" +
                        "ID Slip  : SET-KOP-${setor.id}\n" +
                        "Penyetor : $treasurerName\n" +
                        "Penerima : ${setor.penerima}\n" +
                        "Tanggal  : ${dateFormat.format(Date(setor.tanggal))}\n" +
                        "---------------------------------\n" +
                        "Jumlah   : ${viewModel.formatRupiah(setor.jumlah)}\n" +
                        "Status   : SUKSES DISETORKAN\n" +
                        "Sisa Kas : Terkoreksi Otomatis\n" +
                        "---------------------------------\n" +
                        "Ttd Bendahara & Penerima:\n"
                        
                    Column(
                        modifier = Modifier
                            .weight(1f, fill = false)
                            .heightIn(max = 280.dp)
                            .background(Color(0xFFF5F5F5), RoundedCornerShape(8.dp))
                            .verticalScroll(rememberScrollState())
                            .padding(12.dp)
                            .fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = bText,
                            fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                            fontSize = 11.sp,
                            modifier = Modifier.fillMaxWidth()
                        )
                        
                        Spacer(modifier = Modifier.height(10.dp))
                        
                        if (!setor.ttdOnline.isNullOrEmpty()) {
                            Text(
                                "Tanda Tangan Penerima (Digital):",
                                fontSize = 9.sp,
                                color = Color.Gray,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(bottom = 4.dp)
                            )
                            Box(
                                modifier = Modifier
                                    .width(110.dp)
                                    .height(64.dp)
                                    .background(Color.White, RoundedCornerShape(4.dp))
                                    .border(1.dp, Color.Gray.copy(alpha = 0.5f), RoundedCornerShape(4.dp))
                                    .padding(4.dp)
                            ) {
                                SignatureThumbnail(
                                    serialized = setor.ttdOnline!!,
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                        } else {
                            Text(
                                "Tanda Tangan Manual:\n\n\n     ( ___________________ )",
                                fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                                fontSize = 10.sp,
                                color = Color.DarkGray
                            )
                        }
                    }
                    
                    val context = LocalContext.current
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedButton(
                            onClick = { shareReceiptText(context, "Slip Setoran Tunai Kas", bText) },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Bagikan")
                        }
                        
                        Button(
                            onClick = { printReceiptText(context, "Slip_Setoran_Tunai", bText) },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Print, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Unduh/Cetak")
                        }
                    }

                    OutlinedButton(
                        onClick = { showPrintDialog = false },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Tutup Preview")
                    }
                }
            }
        }
    }
}

@Composable
fun SetorTabunganScreen(viewModel: TabunganViewModel) {
    val context = LocalContext.current
    val currentRole by viewModel.currentRole.collectAsState()
    val allSetor by viewModel.allSetorKoperasi.collectAsState()
    val totalSetor by viewModel.totalSetorKoperasi.collectAsState()
    val saldoBendahara by viewModel.saldoBendahara.collectAsState()
    val schoolName by viewModel.schoolName.collectAsState()
    val treasurerName by viewModel.treasurerName.collectAsState()

    var amountInput by remember { mutableStateOf("") }
    var recipientInput by remember { mutableStateOf("Kepala Sekolah") }
    var customRecipientInput by remember { mutableStateOf("") }
    var isCustomRecipient by remember { mutableStateOf(false) }
    var signatureData by remember { mutableStateOf("") }
    var errorMsg by remember { mutableStateOf("") }
    var showSuccessDialog by remember { mutableStateOf(false) }

    var showExportSuccess by remember { mutableStateOf(false) }
    var showExportError by remember { mutableStateOf(false) }
    var showPrintSetorSummaryDialog by remember { mutableStateOf(false) }

    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/csv")
    ) { uri ->
        uri?.let {
            val success = viewModel.writeSetorKoperasiCsvToUri(context, uri)
            if (success) {
                showExportSuccess = true
            } else {
                showExportError = true
            }
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .testTag("setor_tabungan_screen"),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Card(
                modifier = Modifier.fillMaxWidth().testTag("setor_summary_hdr"),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        "Aliran Dana Bendahara",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("Total yang Disetor", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(viewModel.formatRupiah(totalSetor), fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color(0xFF2E7D32))
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text("Sisa Kas di Bendahara", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(viewModel.formatRupiah(saldoBendahara), fontWeight = FontWeight.Bold, fontSize = 16.sp, color = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
            }
        }

        if (currentRole == UserRole.BENDAHARA) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth().testTag("add_setor_card"),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            "Form Penyerahan / Setoran Dana",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleMedium
                        )

                        Text(
                            "Bendahara dapat menyetorkan akumulasi uang tabungan yang dipegang kepada Kepala Sekolah atau menyetorkan langsung ke Bank.",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.outline
                        )

                        OutlinedTextField(
                            value = amountInput,
                            onValueChange = { amountInput = it },
                            label = { Text("Jumlah Setor (Rp)") },
                            modifier = Modifier.fillMaxWidth().testTag("input_setor_amount"),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true
                        )

                        Text("Pilih Penerima Setoran:", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        
                        // Custom robust chip layout
                        val options = listOf("Kepala Sekolah", "Ka Bank / Bank", "Lainnya")
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            options.forEach { opt ->
                                val isSelected = if (opt == "Lainnya") isCustomRecipient else (recipientInput == opt && !isCustomRecipient)
                                Box(
                                    modifier = Modifier
                                        .background(
                                            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                                            shape = RoundedCornerShape(16.dp)
                                        )
                                        .clickable {
                                            if (opt == "Lainnya") {
                                                isCustomRecipient = true
                                            } else {
                                                isCustomRecipient = false
                                                recipientInput = opt
                                            }
                                        }
                                        .padding(horizontal = 12.dp, vertical = 6.dp)
                                ) {
                                    Text(
                                        text = opt,
                                        color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }

                        if (isCustomRecipient) {
                            OutlinedTextField(
                                value = customRecipientInput,
                                onValueChange = { customRecipientInput = it },
                                label = { Text("Nama/Penerima Custom") },
                                modifier = Modifier.fillMaxWidth().testTag("input_setor_recipient_custom"),
                                singleLine = true
                            )
                        }

                        Text("Tanda Tangan Penerima (Tanda Terima Online):", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        Text("Silakan gambar ttd langsung pada area putih di bawah ini.", fontSize = 10.sp, color = Color.Gray)

                        var clearTrigger by remember { mutableStateOf(0) }

                        Box(modifier = Modifier.fillMaxWidth().height(140.dp)) {
                            SignaturePad(
                                clearTrigger = clearTrigger,
                                onSignatureChange = { signatureData = it },
                                customModifier = Modifier.fillMaxSize()
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            TextButton(
                                onClick = {
                                    clearTrigger++
                                    signatureData = ""
                                },
                                contentPadding = PaddingValues(0.dp)
                            ) {
                                Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Hapus coretan", fontSize = 11.sp, color = MaterialTheme.colorScheme.error)
                            }
                            if (signatureData.isNotEmpty()) {
                                Text("✓ TTD Terekam", fontSize = 11.sp, color = Color(0xFF2E7D32), fontWeight = FontWeight.Bold)
                            }
                        }

                        if (errorMsg.isNotEmpty()) {
                            Text(errorMsg, color = MaterialTheme.colorScheme.error, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }

                        Button(
                            onClick = {
                                val amt = amountInput.toDoubleOrNull() ?: 0.0
                                val finalRecipient = if (isCustomRecipient) customRecipientInput.trim() else recipientInput
                                if (amt <= 0) {
                                    errorMsg = "Nominal setoran harus lebih besar dari 0!"
                                } else if (finalRecipient.isBlank()) {
                                    errorMsg = "Penerima tidak boleh kosong!"
                                            } else if (amt > saldoBendahara) {
                                    errorMsg = "Nominal setoran melebihi sisa uang dipegang bendahara!"
                                } else {
                                    viewModel.addSetorKoperasi(
                                        jumlah = amt,
                                        penerima = finalRecipient,
                                        ttdOnline = signatureData.ifEmpty { null }
                                    ) {
                                        amountInput = ""
                                        customRecipientInput = ""
                                        signatureData = ""
                                        clearTrigger++
                                        errorMsg = ""
                                        showSuccessDialog = true
                                    }
                                }
                            },
                            modifier = Modifier.fillMaxWidth().height(48.dp).testTag("button_submit_setor")
                        ) {
                            Icon(Icons.Default.Send, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Kirim & Catat Setoran", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        item {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "Riwayat Setor Tabungan (${allSetor.size})",
                    style = MaterialTheme.typography.titleMedium
                )

                if (allSetor.isNotEmpty()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Button(
                            onClick = {
                                exportLauncher.launch("Laporan_Setor_Tabungan_${System.currentTimeMillis()}.csv")
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF2E7D32),
                                contentColor = Color.White
                            ),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Description,
                                    contentDescription = "Ekspor Excel",
                                    modifier = Modifier.size(16.dp)
                                )
                                Text("Ekspor Excel", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                            }
                        }

                        Button(
                            onClick = {
                                showPrintSetorSummaryDialog = true
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFFE65100), // Orange Accent
                                contentColor = Color.White
                            ),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Print,
                                    contentDescription = "Cetak Rekap Setor",
                                    modifier = Modifier.size(16.dp)
                                )
                                Text("Cetak Rekap Setor", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                            }
                        }
                    }
                }
            }
        }

        if (allSetor.isEmpty()) {
            item {
                Box(
                    modifier = Modifier.fillMaxWidth().padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.Info, contentDescription = null, modifier = Modifier.size(32.dp), tint = Color.Gray)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Belum ada riwayat setoran tabungan.", color = Color.Gray, fontSize = 12.sp)
                    }
                }
            }
        } else {
            items(allSetor) { s ->
                SetorCardItem(
                    setor = s,
                    viewModel = viewModel,
                    isDeletable = currentRole == UserRole.BENDAHARA
                )
            }
        }
    }

    if (showSuccessDialog) {
        AlertDialog(
            onDismissRequest = { showSuccessDialog = false },
            title = { Text("Setoran Tercatat", fontWeight = FontWeight.Bold) },
            text = { Text("Dana berhasil diserahkan dan otomatis mengurangi sisa saldo pegangan bendahara.") },
            confirmButton = {
                Button(onClick = { showSuccessDialog = false }) {
                    Text("OK")
                }
            }
        )
    }

    if (showExportSuccess) {
        AlertDialog(
            onDismissRequest = { showExportSuccess = false },
            title = { Text("Ekspor Selesai", fontWeight = FontWeight.Bold) },
            text = { Text("Laporan transaksi setoran bendahara berhasil diekspor ke format Excel (.csv)!") },
            confirmButton = {
                Button(onClick = { showExportSuccess = false }) {
                    Text("OK")
                }
            }
        )
    }

    if (showExportError) {
        AlertDialog(
            onDismissRequest = { showExportError = false },
            title = { Text("Ekspor Gagal", fontWeight = FontWeight.Bold) },
            text = { Text("Gagal mengespor laporan setoran tabungan. Hubungi pengembang.") },
            confirmButton = {
                Button(onClick = { showExportError = false }) {
                    Text("OK")
                }
            }
        )
    }

    if (showPrintSetorSummaryDialog) {
        Dialog(onDismissRequest = { showPrintSetorSummaryDialog = false }) {
            Card(
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                modifier = Modifier.padding(8.dp).fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("Cetak Rekap Setoran Dana", fontWeight = FontWeight.Bold, fontSize = 15.sp)

                    val totalSeluruhSetor = allSetor.sumOf { it.jumlah }

                    val bodyText = buildString {
                        append("         ${schoolName.uppercase(Locale("id", "ID"))}\n")
                        append("      REKAPITULASI PENYERAHAN\n")
                        append("          SETORAN TABUNGAN\n")
                        append("=================================\n")
                        append("Hari/Tgl : ${SimpleDateFormat("dd/MM/yyyy HH:mm", Locale("id", "ID")).format(Date())}\n")
                        append("=================================\n")
                        
                        if (allSetor.isEmpty()) {
                            append("     Tidak ada riwayat setoran\n")
                            append("          yang tercatat.\n")
                        } else {
                            allSetor.sortedByDescending { it.tanggal }.forEachIndexed { idx, s ->
                                val dateStr = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale("id", "ID")).format(Date(s.tanggal))
                                append("#${idx + 1} Tgl  : $dateStr\n")
                                append("   Jumlah : ${viewModel.formatRupiah(s.jumlah)}\n")
                                append("   Penerima: ${s.penerima}\n")
                                if (!s.ttdOnline.isNullOrEmpty()) {
                                    append("   Ttd     : [Tanda Tangan Online]\n")
                                }
                                append("- - - - - - - - - - - - - - - - -\n")
                            }
                        }
                        
                        append("TOTAL SETORAN TERDATA:\n")
                        append(">> ${viewModel.formatRupiah(totalSeluruhSetor)}\n")
                        append("=================================\n")
                        append("Bendahara: $treasurerName\n")
                        append("=================================\n")
                        append("     [TERVERIFIKASI SISTEM]\n")
                        append("   Simulasi Printer POS-58mm")
                    }

                    Box(
                        modifier = Modifier
                            .weight(1f, fill = false)
                            .heightIn(max = 280.dp)
                            .background(Color(0xFFF5F5F5), RoundedCornerShape(8.dp))
                            .verticalScroll(rememberScrollState())
                            .padding(12.dp)
                            .fillMaxWidth()
                    ) {
                        Text(
                            text = bodyText,
                            fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                            fontSize = 11.sp,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedButton(
                            onClick = { shareReceiptText(context, "Rekap Setoran Tabungan", bodyText) },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Bagikan")
                        }
                        
                        Button(
                            onClick = { printReceiptText(context, "Rekap_Setoran_Tabungan", bodyText) },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Print, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Unduh/Cetak")
                        }
                    }

                    OutlinedButton(
                        onClick = { showPrintSetorSummaryDialog = false },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Tutup Preview")
                    }
                }
            }
        }
    }
}
