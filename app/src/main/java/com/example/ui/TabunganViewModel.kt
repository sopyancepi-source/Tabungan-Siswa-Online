package com.example.ui

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.model.*
import com.example.data.repository.TabunganRepository
import com.example.data.FirebaseSyncManager
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

enum class UserRole(val label: String, val description: String) {
    BENDAHARA("Bendahara", "Akses penuh Kelola Kas, Siswa, & Pinjaman"),
    ADMIN("Admin Petugas", "Akses mencatat Nasabah & Transaksi Tabungan"),
    KEPALA("Kepala Sekolah", "Akses visual Dashboard & Monitoring (Laporan)")
}

data class AppNotification(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val message: String,
    val timestamp: Long = System.currentTimeMillis()
)

class TabunganViewModel(
    application: Application,
    private val repository: TabunganRepository,
    private val syncManager: FirebaseSyncManager
) : AndroidViewModel(application) {

    // User Roles login & passcode state
    private val _currentRole = MutableStateFlow<UserRole?>(null)
    val currentRole: StateFlow<UserRole?> = _currentRole.asStateFlow()

    private val sharedPrefs by lazy {
        getApplication<Application>().getSharedPreferences("user_credentials_v1", Context.MODE_PRIVATE)
    }

    // Theme state: "SYSTEM", "LIGHT", "DARK"
    private val _themePreference = MutableStateFlow(sharedPrefs.getString("app_theme_pref", "SYSTEM") ?: "SYSTEM")
    val themePreference: StateFlow<String> = _themePreference.asStateFlow()

    fun updateThemePreference(newTheme: String) {
        _themePreference.value = newTheme
        sharedPrefs.edit().putString("app_theme_pref", newTheme).apply()
    }

    // Dynamic School & Treasurer Identity Configuration (Flexible Settings)
    private val _schoolName = MutableStateFlow(sharedPrefs.getString("school_name_pref", "MIS CIBUNGUR I") ?: "MIS CIBUNGUR I")
    val schoolName: StateFlow<String> = _schoolName.asStateFlow()

    private val _treasurerName = MutableStateFlow(sharedPrefs.getString("treasurer_name_pref", "Cepi Sopyan, S.Pd.I") ?: "Cepi Sopyan, S.Pd.I")
    val treasurerName: StateFlow<String> = _treasurerName.asStateFlow()

    fun updateIdentitySettings(school: String, treasurer: String) {
        _schoolName.value = school.trim().uppercase()
        _treasurerName.value = treasurer.trim()
        sharedPrefs.edit()
            .putString("school_name_pref", school.trim().uppercase())
            .putString("treasurer_name_pref", treasurer.trim())
            .apply()
        syncManager.pushSettingsToFirebase(school, treasurer)
    }

    fun verifyPassword(role: UserRole, input: String): Boolean {
        val stored = sharedPrefs.getString("pwd_${role.name}", getDefaultPassword(role))
        return stored == input
    }

    fun updatePassword(role: UserRole, oldVal: String, newVal: String): Boolean {
        if (verifyPassword(role, oldVal)) {
            sharedPrefs.edit().putString("pwd_${role.name}", newVal).apply()
            syncManager.pushPasswordToFirebase(role.name, newVal)
            return true
        }
        return false
    }

    fun login(role: UserRole) {
        _currentRole.value = role
    }

    fun logout() {
        _currentRole.value = null
    }

    fun getDefaultPassword(role: UserRole): String {
        return when (role) {
            UserRole.BENDAHARA -> "bendahara123"
            UserRole.ADMIN -> "admin123"
            UserRole.KEPALA -> "kepala123"
        }
    }

    // Notifications state
    private val _notifications = MutableStateFlow<List<AppNotification>>(emptyList())
    val notifications: StateFlow<List<AppNotification>> = _notifications.asStateFlow()

    // Data streams
    val siswaList: StateFlow<List<Siswa>> = repository.allSiswa
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val transaksiList: StateFlow<List<Transaksi>> = repository.allTransaksi
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val pinjamanSekolahList: StateFlow<List<PinjamanSekolah>> = repository.allPinjamanSekolah
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val pinjamanGuruList: StateFlow<List<PinjamanGuru>> = repository.allPinjamanGuru
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val adminKasList: StateFlow<List<AdminKas>> = repository.allAdminKas
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allSetorKoperasi: StateFlow<List<SetorKoperasi>> = repository.allSetorKoperasi
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Calculations & State derived from lists
    val totalTabunganSiswa: StateFlow<Double> = transaksiList.map { trans ->
        trans.sumOf { t ->
            if (t.tipe == "SETOR") t.jumlah else -t.jumlah
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val outstandingPinjamanSekolah: StateFlow<Double> = pinjamanSekolahList.map { pinjamans ->
        pinjamans.sumOf { p -> (p.jumlahPinjam - p.jumlahBayar) }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val outstandingPinjamanGuru: StateFlow<Double> = pinjamanGuruList.map { pinjamans ->
        pinjamans.sumOf { p -> (p.jumlahPinjam - p.jumlahBayar) }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val totalPendapatanAdmin: StateFlow<Double> = adminKasList.map { kas ->
        kas.sumOf { k ->
            when (k.tipe) {
                "MODAL_AWAL", "PENDAPATAN_ADMIN" -> k.jumlah
                "PENGELUARAN_OPERASIONAL" -> -k.jumlah
                else -> 0.0
            }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    // Kas Riil = Total Tabungan Siswa - Sisa Pinjaman Sekolah - Sisa Pinjaman Guru + Saldo Admin
    val saldoKasRiil: StateFlow<Double> = combine(
        totalTabunganSiswa,
        outstandingPinjamanSekolah,
        outstandingPinjamanGuru,
        totalPendapatanAdmin
    ) { tabungan, pSekolah, pGuru, admin ->
        tabungan - pSekolah - pGuru + admin
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val totalSetorKoperasi: StateFlow<Double> = allSetorKoperasi.map { list ->
        list.sumOf { s -> s.jumlah }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val saldoBendahara: StateFlow<Double> = combine(
        saldoKasRiil,
        totalSetorKoperasi
    ) { kasRiil, totalSetor ->
        kasRiil - totalSetor
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    // Map of Siswa ID to their current balance
    val siswaBalances: StateFlow<Map<Int, Double>> = transaksiList.map { trans ->
        val balances = mutableMapOf<Int, Double>()
        for (t in trans) {
            val current = balances.getOrDefault(t.siswaId, 0.0)
            if (t.tipe == "SETOR") {
                balances[t.siswaId] = current + t.jumlah
            } else {
                balances[t.siswaId] = current - t.jumlah
            }
        }
        balances
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    init {
        createNotificationChannel()
        // Clear any old MODAL_AWAL entries to ensure the school cash starts at pure 0
        // until teachers, students, or admins record real transactions.
        viewModelScope.launch {
            repository.allAdminKas.first().let { kas ->
                val legacyModal = kas.filter { it.tipe == "MODAL_AWAL" }
                for (row in legacyModal) {
                    repository.deleteAdminKas(row.id)
                }
            }
        }
    }

    fun registerSiswa(nama: String, kelas: String, nomorInduk: String, onSuccess: () -> Unit = {}) {
        viewModelScope.launch {
            repository.insertSiswa(
                Siswa(nama = nama, kelas = kelas, nomorInduk = nomorInduk)
            )
            onSuccess()
        }
    }

    fun deleteSiswa(id: Int, onSuccess: () -> Unit = {}) {
        viewModelScope.launch {
            repository.deleteSiswa(id)
            onSuccess()
        }
    }

    fun deleteTransaksi(id: Int, onSuccess: () -> Unit = {}) {
        viewModelScope.launch {
            repository.deleteTransaksi(id)
            // also delete associated admin fee if exists
            val matchDesc = "Biaya Admin Transaksi #$id"
            adminKasList.value.find { it.keterangan == matchDesc }?.let { adminKas ->
                repository.deleteAdminKas(adminKas.id)
            }
            onSuccess()
        }
    }

    fun addTransaksi(siswaId: Int, tipe: String, jumlah: Double, biayaAdmin: Double, keterangan: String, onSuccess: () -> Unit = {}) {
        viewModelScope.launch {
            val id = repository.insertTransaksi(
                Transaksi(
                    siswaId = siswaId,
                    tipe = tipe,
                    jumlah = jumlah,
                    biayaAdmin = biayaAdmin,
                    keterangan = keterangan
                )
            )
            
            if (biayaAdmin > 0.0) {
                repository.insertAdminKas(
                    AdminKas(
                        tipe = "PENDAPATAN_ADMIN",
                        jumlah = biayaAdmin,
                        keterangan = "Biaya Admin Transaksi #$id"
                    )
                )
            }

            val siswa = repository.getSiswaById(siswaId)
            val namaSiswa = siswa?.nama ?: "Siswa"
            val actionWord = if (tipe == "SETOR") "Setoran" else "Penarikan"
            val formattedAmount = formatRupiah(jumlah)
            val title = "Transaksi $actionWord Berhasil!"
            val message = "$actionWord sebesar $formattedAmount untuk $namaSiswa berhasil diproses."

            addInAppNotification(title, message)
            sendSystemNotification(title, message)

            onSuccess()
        }
    }

    fun addPinjamanSekolah(deskripsi: String, jumlah: Double, onSuccess: () -> Unit = {}) {
        viewModelScope.launch {
            repository.insertPinjamanSekolah(
                PinjamanSekolah(deskripsi = deskripsi, jumlahPinjam = jumlah)
            )
            onSuccess()
        }
    }

    fun addSetorKoperasi(jumlah: Double, penerima: String, ttdOnline: String?, onSuccess: () -> Unit = {}) {
        viewModelScope.launch {
            repository.insertSetorKoperasi(
                SetorKoperasi(jumlah = jumlah, penerima = penerima, ttdOnline = ttdOnline)
            )
            addInAppNotification(
                "Setor Tabungan",
                "Berhasil menyetor ${formatRupiah(jumlah)} kepada $penerima."
            )
            onSuccess()
        }
    }

    fun deleteSetorKoperasi(id: Int) {
        viewModelScope.launch {
            repository.deleteSetorKoperasi(id)
            addInAppNotification(
                "Hapus Setor Tabungan",
                "Riwayat setoran tabungan berhasil dihapus."
            )
        }
    }

    fun generateSetorKoperasiExcelCsvString(): String {
        val list = allSetorKoperasi.value
        val sb = java.lang.StringBuilder()
        sb.append('\ufeff')
        sb.append("LAPORAN DATA TRANSAKSI SETOR TABUNGAN BENDAHARA\r\n")
        val dateFormat = SimpleDateFormat("dd-MM-yyyy HH:mm", Locale("id", "ID"))
        sb.append("Tanggal Cetak;${dateFormat.format(Date())}\r\n")
        sb.append("Aplikasi;Tabungan Siswa Sekolah\r\n")
        sb.append("\r\n")
        sb.append("No;Tanggal;Waktu;Penerima;Jumlah Setor (Rp);Tanda Terima (TTD)\r\n")
        list.forEachIndexed { index, s ->
            val dateStr = SimpleDateFormat("dd-MM-yyyy", Locale("id", "ID")).format(Date(s.tanggal))
            val timeStr = SimpleDateFormat("HH:mm", Locale("id", "ID")).format(Date(s.tanggal))
            val recipient = s.penerima.replace(";", ",")
            val signatureStatus = if (s.ttdOnline.isNullOrEmpty()) "Tidak Ada" else "Ada TTD Online"
            sb.append("${index + 1};$dateStr;$timeStr;$recipient;${s.jumlah.toLong()};$signatureStatus\r\n")
        }
        return sb.toString()
    }

    fun writeSetorKoperasiCsvToUri(context: Context, uri: android.net.Uri): Boolean {
        return try {
            val data = generateSetorKoperasiExcelCsvString()
            context.contentResolver.openOutputStream(uri)?.use { os ->
                os.write(data.toByteArray(charset("UTF-8")))
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    fun bayarPinjamanSekolah(pinjaman: PinjamanSekolah, bayarAmount: Double, onSuccess: () -> Unit = {}) {
        viewModelScope.launch {
            val totalBayar = pinjaman.jumlahBayar + bayarAmount
            val isLunas = totalBayar >= pinjaman.jumlahPinjam
            val updated = pinjaman.copy(
                jumlahBayar = if (totalBayar > pinjaman.jumlahPinjam) pinjaman.jumlahPinjam else totalBayar,
                lunas = isLunas,
                tanggalLunas = if (isLunas) System.currentTimeMillis() else null
            )
            repository.updatePinjamanSekolah(updated)
            onSuccess()
        }
    }

    fun bayarPinjamanSekolahSekaligus(bayarAmount: Double, onSuccess: () -> Unit = {}) {
        viewModelScope.launch {
            var sisaBayar = bayarAmount
            if (sisaBayar <= 0) return@launch
            val activeLoans = pinjamanSekolahList.value.filter { !it.lunas }.sortedBy { it.tanggalPinjam }
            for (loan in activeLoans) {
                if (sisaBayar <= 0) break
                val limit = loan.jumlahPinjam - loan.jumlahBayar
                if (limit <= 0) continue
                val allocated = if (sisaBayar >= limit) limit else sisaBayar
                sisaBayar -= allocated
                val totalBayar = loan.jumlahBayar + allocated
                val isLunas = totalBayar >= loan.jumlahPinjam
                val updated = loan.copy(
                    jumlahBayar = if (totalBayar > loan.jumlahPinjam) loan.jumlahPinjam else totalBayar,
                    lunas = isLunas,
                    tanggalLunas = if (isLunas) System.currentTimeMillis() else null
                )
                repository.updatePinjamanSekolah(updated)
            }
            onSuccess()
        }
    }

    fun deletePinjamanSekolah(id: Int) {
        viewModelScope.launch {
            repository.deletePinjamanSekolah(id)
        }
    }

    fun clearAllDatabase(onSuccess: () -> Unit = {}) {
        viewModelScope.launch {
            repository.clearAllDatabase()
            addInAppNotification("Sistem Direset", "Semua data transaksi, siswa, kas, dan pinjaman telah dibersihkan secara permanen.")
            onSuccess()
        }
    }

    fun addPinjamanGuru(namaGuru: String, jumlah: Double, onSuccess: () -> Unit = {}) {
        viewModelScope.launch {
            repository.insertPinjamanGuru(
                PinjamanGuru(namaGuru = namaGuru, jumlahPinjam = jumlah)
            )
            onSuccess()
        }
    }

    fun bayarPinjamanGuru(pinjaman: PinjamanGuru, bayarAmount: Double, onSuccess: () -> Unit = {}) {
        viewModelScope.launch {
            val totalBayar = pinjaman.jumlahBayar + bayarAmount
            val isLunas = totalBayar >= pinjaman.jumlahPinjam
            val updated = pinjaman.copy(
                jumlahBayar = if (totalBayar > pinjaman.jumlahPinjam) pinjaman.jumlahPinjam else totalBayar,
                lunas = isLunas,
                tanggalLunas = if (isLunas) System.currentTimeMillis() else null
            )
            repository.updatePinjamanGuru(updated)
            onSuccess()
        }
    }

    fun bayarPinjamanGuruSekaligus(namaGuru: String, bayarAmount: Double, onSuccess: () -> Unit = {}) {
        viewModelScope.launch {
            var sisaBayar = bayarAmount
            if (sisaBayar <= 0) return@launch
            val activeLoans = pinjamanGuruList.value
                .filter { it.namaGuru.equals(namaGuru, ignoreCase = true) && !it.lunas }
                .sortedBy { it.tanggalPinjam }
            for (loan in activeLoans) {
                if (sisaBayar <= 0) break
                val limit = loan.jumlahPinjam - loan.jumlahBayar
                if (limit <= 0) continue
                val allocated = if (sisaBayar >= limit) limit else sisaBayar
                sisaBayar -= allocated
                val totalBayar = loan.jumlahBayar + allocated
                val isLunas = totalBayar >= loan.jumlahPinjam
                val updated = loan.copy(
                    jumlahBayar = if (totalBayar > loan.jumlahPinjam) loan.jumlahPinjam else totalBayar,
                    lunas = isLunas,
                    tanggalLunas = if (isLunas) System.currentTimeMillis() else null
                )
                repository.updatePinjamanGuru(updated)
            }
            onSuccess()
        }
    }

    fun deletePinjamanGuru(id: Int) {
        viewModelScope.launch {
            repository.deletePinjamanGuru(id)
        }
    }

    fun addAdminKas(tipe: String, jumlah: Double, keterangan: String, onSuccess: () -> Unit = {}) {
        viewModelScope.launch {
            repository.insertAdminKas(
                AdminKas(tipe = tipe, jumlah = jumlah, keterangan = keterangan)
            )
            onSuccess()
        }
    }

    fun deleteAdminKas(id: Int, onSuccess: () -> Unit = {}) {
        viewModelScope.launch {
            repository.deleteAdminKas(id)
            onSuccess()
        }
    }

    fun addInAppNotification(title: String, message: String) {
        val newNotification = AppNotification(title = title, message = message)
        _notifications.update { current ->
            (listOf(newNotification) + current).take(20)
        }
    }

    fun removeNotification(id: String) {
        _notifications.update { current ->
            current.filter { it.id != id }
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channelId = "transaksi_channel"
            val name = "Notifikasi Transaksi"
            val descriptionText = "Notifikasi real-time untuk setoran dan penarikan tabungan siswa"
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val channel = NotificationChannel(channelId, name, importance).apply {
                description = descriptionText
            }
            val notificationManager: NotificationManager =
                getApplication<Application>().getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun sendSystemNotification(title: String, message: String) {
        try {
            val channelId = "transaksi_channel"
            val builder = NotificationCompat.Builder(getApplication(), channelId)
                .setSmallIcon(android.R.drawable.stat_notify_chat)
                .setContentTitle(title)
                .setContentText(message)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setAutoCancel(true)

            val notificationManager =
                getApplication<Application>().getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.notify(System.currentTimeMillis().toInt(), builder.build())
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun formatRupiah(amount: Double): String {
        val format = java.text.NumberFormat.getCurrencyInstance(Locale("in", "ID"))
        return format.format(amount).replace("Rp", "Rp ").replace(",00", "")
    }

    fun formatDate(timestamp: Long): String {
        val sdf = SimpleDateFormat("dd MMM yyyy, HH:m", Locale("id", "ID"))
        return sdf.format(Date(timestamp))
    }

    fun formatDateShort(timestamp: Long): String {
        val sdf = SimpleDateFormat("dd/MM/yyyy", Locale("id", "ID"))
        return sdf.format(Date(timestamp))
    }

    fun getMonthlyReports(): List<MonthlyReport> {
        val trans = transaksiList.value
        val reports = mutableMapOf<String, MonthlyReport>()
        val sdf = SimpleDateFormat("MMMM yyyy", Locale("id", "ID"))

        for (t in trans) {
            val monthKey = sdf.format(Date(t.tanggal))
            val currentReport = reports.getOrDefault(monthKey, MonthlyReport(monthKey))
            
            if (t.tipe == "SETOR") {
                currentReport.totalSetor += t.jumlah
                currentReport.jumlahSetorCount++
            } else {
                currentReport.totalTarik += t.jumlah
                currentReport.jumlahTarikCount++
            }
            currentReport.totalBiayaAdmin += t.biayaAdmin
            reports[monthKey] = currentReport
        }
        return reports.values.toList().sortedByDescending { it.monthYear }
    }

    fun generateExcelCsvString(): String {
        val localSiswa = siswaList.value
        val localBalances = siswaBalances.value
        val localTransaksi = transaksiList.value

        val sb = java.lang.StringBuilder()
        // Add UTF-8 BOM (Byte Order Mark) so Excel opens it with proper character encoding immediately
        sb.append('\ufeff')
        
        // Header Info
        sb.append("LAPORAN DATA NASABAH DAN TRANSAKSI TABUNGAN SISWA\r\n")
        val dateFormat = SimpleDateFormat("dd-MM-yyyy HH:mm", Locale("id", "ID"))
        sb.append("Tanggal Cetak;${dateFormat.format(Date())}\r\n")
        sb.append("Aplikasi;Tabungan Siswa Sekolah\r\n")
        sb.append("\r\n")

        // Part 1: Summary of customers (Siswa)
        sb.append("1. DAFTAR RINGKASAN SALDO NASABAH (SISWA)\r\n")
        sb.append("No;Nomor Induk (NIS);Nama Siswa;Kelas;Total Saldo (Rp)\r\n")
        localSiswa.forEachIndexed { index, s ->
            val balance = localBalances[s.id] ?: 0.0
            val cleanName = s.nama.replace(";", ",")
            val cleanClass = s.kelas.replace(";", ",")
            val cleanNis = s.nomorInduk.replace(";", ",")
            sb.append("${index + 1};$cleanNis;$cleanName;$cleanClass;${balance.toLong()}\r\n")
        }
        sb.append("\r\n")

        // Part 2: Detailed Transaction Ledger
        sb.append("2. DETAIL RIWAYAT TRANSAKSI SEMUA NASABAH\r\n")
        sb.append("No;Tanggal;Nomor Induk (NIS);Nama Siswa;Kelas;Tipe Transaksi;Nominal (Rp);Biaya Admin (Rp);Keterangan/Catatan\r\n")
        
        val sortedTransactions = localTransaksi.sortedByDescending { it.tanggal }
        sortedTransactions.forEachIndexed { index, t ->
            val s = localSiswa.find { it.id == t.siswaId }
            val sisName = s?.nama?.replace(";", ",") ?: "Tidak Dikenal"
            val sisNis = s?.nomorInduk?.replace(";", ",") ?: "-"
            val sisClass = s?.kelas?.replace(";", ",") ?: "-"
            val dateStr = dateFormat.format(Date(t.tanggal))
            val typeStr = if (t.tipe == "SETOR") "SETORAN" else "PENARIKAN"
            val cleanRemarks = t.keterangan.replace(";", ",").replace("\n", " ").replace("\r", "")
            
            sb.append("${index + 1};$dateStr;$sisNis;$sisName;$sisClass;$typeStr;${t.jumlah.toLong()};${t.biayaAdmin.toLong()};$cleanRemarks\r\n")
        }
        
        return sb.toString()
    }

    fun writeCsvToUri(context: Context, uri: android.net.Uri): Boolean {
        return try {
            val data = generateExcelCsvString()
            context.contentResolver.openOutputStream(uri)?.use { os ->
                os.write(data.toByteArray(Charsets.UTF_8))
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    fun generateLoansExcelCsvString(): String {
        val localSekolah = pinjamanSekolahList.value
        val localGuru = pinjamanGuruList.value

        val sb = java.lang.StringBuilder()
        // Add UTF-8 BOM so Excel opens it with proper character encoding immediately
        sb.append('\ufeff')
        
        // Header Info
        sb.append("LAPORAN DATA PINJAMAN SEKOLAH DAN PINJAMAN GURU\r\n")
        val dateFormat = SimpleDateFormat("dd-MM-yyyy HH:mm", Locale("id", "ID"))
        val dateOnlyFormat = SimpleDateFormat("dd-MM-yyyy", Locale("id", "ID"))
        sb.append("Tanggal Cetak;${dateFormat.format(Date())}\r\n")
        sb.append("Aplikasi;Tabungan Siswa Sekolah\r\n")
        sb.append("\r\n")

        // Part 1: Pinjaman Sekolah (Dana Talangan)
        sb.append("1. REKAPITULASI DANA TALANGAN / PINJAMAN SEKOLAH\r\n")
        sb.append("No;Tanggal Pinjam;Deskripsi/Kebutuhan;Nominal Pinjaman (Rp);Sudah Diangsur (Rp);Sisa Hutang (Rp);Status;Tanggal Lunas\r\n")
        localSekolah.sortedByDescending { it.tanggalPinjam }.forEachIndexed { index, p ->
            val dateStr = dateOnlyFormat.format(Date(p.tanggalPinjam))
            val sisa = p.jumlahPinjam - p.jumlahBayar
            val statusStr = if (p.lunas) "LUNAS" else "BELUM LUNAS"
            val lunasDateStr = p.tanggalLunas?.let { dateOnlyFormat.format(Date(it)) } ?: "-"
            val cleanDesc = p.deskripsi.replace(";", ",")
            sb.append("${index + 1};$dateStr;$cleanDesc;${p.jumlahPinjam.toLong()};${p.jumlahBayar.toLong()};${sisa.toLong()};$statusStr;$lunasDateStr\r\n")
        }
        sb.append("\r\n")

        // Part 2: Pinjaman Guru
        sb.append("2. REKAPITULASI DATA PINJAMAN GURU & STAF\r\n")
        sb.append("No;Tanggal Pinjam;Nama Guru;Nominal Pinjaman (Rp);Sudah Diangsur (Rp);Sisa Hutang (Rp);Status;Tanggal Lunas\r\n")
        localGuru.sortedByDescending { it.tanggalPinjam }.forEachIndexed { index, p ->
            val dateStr = dateOnlyFormat.format(Date(p.tanggalPinjam))
            val sisa = p.jumlahPinjam - p.jumlahBayar
            val statusStr = if (p.lunas) "LUNAS" else "BELUM LUNAS"
            val lunasDateStr = p.tanggalLunas?.let { dateOnlyFormat.format(Date(it)) } ?: "-"
            val cleanName = p.namaGuru.replace(";", ",")
            sb.append("${index + 1};$dateStr;$cleanName;${p.jumlahPinjam.toLong()};${p.jumlahBayar.toLong()};${sisa.toLong()};$statusStr;$lunasDateStr\r\n")
        }

        return sb.toString()
    }

    fun writeLoansCsvToUri(context: Context, uri: android.net.Uri): Boolean {
        return try {
            val data = generateLoansExcelCsvString()
            context.contentResolver.openOutputStream(uri)?.use { os ->
                os.write(data.toByteArray(Charsets.UTF_8))
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    fun generateSingleGuruLoanCsvString(namaGuru: String): String {
        val localGuru = pinjamanGuruList.value.filter { it.namaGuru.equals(namaGuru, ignoreCase = true) }
        val sb = java.lang.StringBuilder()
        sb.append('\ufeff')
        
        sb.append("LAPORAN RINCIAN PINJAMAN GURU & STAF\r\n")
        sb.append("Nama Guru;${namaGuru.uppercase()}\r\n")
        val dateFormat = SimpleDateFormat("dd-MM-yyyy HH:mm", Locale("id", "ID"))
        val dateOnlyFormat = SimpleDateFormat("dd-MM-yyyy", Locale("id", "ID"))
        sb.append("Tanggal Cetak;${dateFormat.format(Date())}\r\n")
        sb.append("Aplikasi;Tabungan Siswa Sekolah\r\n")
        sb.append("\r\n")

        sb.append("No;Tanggal Pinjam;Nominal Pinjaman (Rp);Sudah Diangsur (Rp);Sisa Hutang (Rp);Status;Tanggal Lunas\r\n")
        localGuru.sortedBy { it.tanggalPinjam }.forEachIndexed { index, p ->
            val dateStr = dateOnlyFormat.format(Date(p.tanggalPinjam))
            val sisa = p.jumlahPinjam - p.jumlahBayar
            val statusStr = if (p.lunas) "LUNAS" else "BELUM LUNAS"
            val lunasDateStr = p.tanggalLunas?.let { dateOnlyFormat.format(Date(it)) } ?: "-"
            sb.append("${index + 1};$dateStr;${p.jumlahPinjam.toLong()};${p.jumlahBayar.toLong()};${sisa.toLong()};$statusStr;$lunasDateStr\r\n")
        }
        
        val totalPinjam = localGuru.sumOf { it.jumlahPinjam }
        val totalBayar = localGuru.sumOf { it.jumlahBayar }
        val totalSisa = totalPinjam - totalBayar
        sb.append("\r\n")
        sb.append("GRAND TOTAL;;${totalPinjam.toLong()};${totalBayar.toLong()};${totalSisa.toLong()}\r\n")

        return sb.toString()
    }

    fun writeSingleGuruLoansCsvToUri(context: Context, namaGuru: String, uri: android.net.Uri): Boolean {
        return try {
            val data = generateSingleGuruLoanCsvString(namaGuru)
            context.contentResolver.openOutputStream(uri)?.use { os ->
                os.write(data.toByteArray(Charsets.UTF_8))
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    fun generateAdminKasExcelCsvString(): String {
        val list = adminKasList.value
        val sb = java.lang.StringBuilder()
        sb.append('\ufeff')
        sb.append("LAPORAN BUKU KAS UTAMA KOPERASI SEKOLAH\r\n")
        val dateFormat = SimpleDateFormat("dd-MM-yyyy HH:mm", Locale("id", "ID"))
        val dateOnlyFormat = SimpleDateFormat("dd-MM-yyyy", Locale("id", "ID"))
        sb.append("Tanggal Cetak;${dateFormat.format(Date())}\r\n")
        sb.append("\r\n")
        sb.append("No;Tanggal;Tipe Transaksi;Nominal (Rp);Keterangan\r\n")
        
        list.sortedByDescending { it.tanggal }.forEachIndexed { index, k ->
            val dateStr = dateOnlyFormat.format(Date(k.tanggal))
            val tipeStr = when (k.tipe) {
                "MODAL_AWAL" -> "MODAL AWAL"
                "PENDAPATAN_ADMIN" -> "PENDAPATAN ADMIN"
                "PENGELUARAN_OPERASIONAL" -> "PENGELUARAN OPERASIONAL"
                else -> k.tipe
            }
            val cleanDesc = k.keterangan.replace(";", ",")
            sb.append("${index + 1};$dateStr;$tipeStr;${k.jumlah.toLong()};$cleanDesc\r\n")
        }
        return sb.toString()
    }

    fun writeAdminKasCsvToUri(context: Context, uri: android.net.Uri): Boolean {
        return try {
            val data = generateAdminKasExcelCsvString()
            context.contentResolver.openOutputStream(uri)?.use { os ->
                os.write(data.toByteArray(Charsets.UTF_8))
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
}

data class MonthlyReport(
    val monthYear: String,
    var totalSetor: Double = 0.0,
    var jumlahSetorCount: Int = 0,
    var totalTarik: Double = 0.0,
    var jumlahTarikCount: Int = 0,
    var totalBiayaAdmin: Double = 0.0
)

class TabunganViewModelFactory(
    private val application: Application,
    private val repository: TabunganRepository,
    private val syncManager: FirebaseSyncManager
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(TabunganViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return TabunganViewModel(application, repository, syncManager) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
