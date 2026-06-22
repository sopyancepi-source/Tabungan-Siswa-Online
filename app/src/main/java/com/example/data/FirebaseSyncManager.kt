package com.example.data

import android.content.Context
import android.util.Log
import com.example.data.model.*
import com.example.data.repository.TabunganRepository
import com.google.firebase.database.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class FirebaseSyncManager(
    private val context: Context,
    private val repository: TabunganRepository
) {
    private val databaseUrl = "https://tabsis-online-d3d5a-default-rtdb.asia-southeast1.firebasedatabase.app/"
    private val database = FirebaseDatabase.getInstance(databaseUrl).apply {
        try {
            setPersistenceEnabled(true)
            // Keep critical tables synced locally
            getReference("siswa").keepSynced(true)
            getReference("transaksi").keepSynced(true)
            getReference("pinjaman_sekolah").keepSynced(true)
            getReference("pinjaman_guru").keepSynced(true)
            getReference("admin_kas").keepSynced(true)
            getReference("setor_koperasi").keepSynced(true)
            getReference("settings").keepSynced(true)
        } catch (e: java.lang.Exception) {
            Log.e("FirebaseSyncManager", "Failed to configure disk persistence / syncing: ${e.message}")
        }
    }
    private val scope = CoroutineScope(Dispatchers.IO)

    // Thread-safe maps to handle synchronization states per table node
    private val isDownloadingMap = java.util.concurrent.ConcurrentHashMap<String, Boolean>()
    private val isInitializedMap = java.util.concurrent.ConcurrentHashMap<String, Boolean>()

    // Cached variables to prevent infinite update loops
    @Volatile
    private var lastSiswaList: List<Siswa>? = null

    @Volatile
    private var lastTransaksiList: List<Transaksi>? = null

    @Volatile
    private var lastPinjamanSekolahList: List<PinjamanSekolah>? = null

    @Volatile
    private var lastPinjamanGuruList: List<PinjamanGuru>? = null

    @Volatile
    private var lastAdminKasList: List<AdminKas>? = null

    @Volatile
    private var lastSetorKoperasiList: List<SetorKoperasi>? = null

    private val sharedPrefs by lazy {
        context.getSharedPreferences("user_credentials_v1", Context.MODE_PRIVATE)
    }

    fun startSyncing() {
        Log.d("FirebaseSyncManager", "Starting Real-Time Firebase Database Sync...")
        setupConnectionListener()
        setupFirebaseListeners()
        setupRoomFlowUploads()
    }

    /**
     * Listen to active connection changes to optimize reconnection and force online sync.
     */
    private fun setupConnectionListener() {
        database.getReference(".info/connected").addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val connected = snapshot.getValue(Boolean::class.java) ?: false
                Log.d("FirebaseSyncManager", "Firebase connection status: $connected")
                if (connected) {
                    Log.d("FirebaseSyncManager", "Firebase is connected and sync is active.")
                } else {
                    Log.d("FirebaseSyncManager", "Firebase is currently offline/disconnected. Changes will be synchronized automatically upon reconnection.")
                }
            }
            override fun onCancelled(error: DatabaseError) {
                Log.e("FirebaseSyncManager", "Connection listener cancelled: ${error.message}")
            }
        })
    }

    /**
     * Listen to modifications made in Firebase and update local Room DB.
     * Overwrites/syncs local data upon changes in Firebase.
     */
    private fun setupFirebaseListeners() {
        // 1. Siswa Listener
        database.getReference("siswa").addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                scope.launch {
                    try {
                        val list = mutableListOf<Siswa>()
                        for (child in snapshot.children) {
                            val item = child.getValue(Siswa::class.java)
                            if (item != null) {
                                list.add(item)
                            }
                        }
                        if (lastSiswaList == list) {
                            // Perfect identity, mark initialized and exit to avoid circular loops
                            isInitializedMap["siswa"] = true
                            return@launch
                        }
                        
                        isDownloadingMap["siswa"] = true
                        lastSiswaList = list
                        repository.replaceAllSiswa(list)
                        isInitializedMap["siswa"] = true
                        isDownloadingMap["siswa"] = false
                        Log.d("FirebaseSyncManager", "Siswa synced from Firebase: ${list.size} items")
                    } catch (e: Exception) {
                        isDownloadingMap["siswa"] = false
                        Log.e("FirebaseSyncManager", "Error syncing Siswa: ${e.message}")
                    }
                }
            }
            override fun onCancelled(error: DatabaseError) {
                Log.e("FirebaseSyncManager", "Firebase Siswa cancelled: ${error.message}")
            }
        })

        // 2. Transaksi Listener
        database.getReference("transaksi").addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                scope.launch {
                    try {
                        val list = mutableListOf<Transaksi>()
                        for (child in snapshot.children) {
                            val item = child.getValue(Transaksi::class.java)
                            if (item != null) {
                                list.add(item)
                            }
                        }
                        if (lastTransaksiList == list) {
                            isInitializedMap["transaksi"] = true
                            return@launch
                        }
                        
                        isDownloadingMap["transaksi"] = true
                        lastTransaksiList = list
                        repository.replaceAllTransaksi(list)
                        isInitializedMap["transaksi"] = true
                        isDownloadingMap["transaksi"] = false
                        Log.d("FirebaseSyncManager", "Transaksi synced from Firebase: ${list.size} items")
                    } catch (e: Exception) {
                        isDownloadingMap["transaksi"] = false
                        Log.e("FirebaseSyncManager", "Error syncing Transaksi: ${e.message}")
                    }
                }
            }
            override fun onCancelled(error: DatabaseError) {
                Log.e("FirebaseSyncManager", "Firebase Transaksi cancelled: ${error.message}")
            }
        })

        // 3. Pinjaman Sekolah Listener
        database.getReference("pinjaman_sekolah").addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                scope.launch {
                    try {
                        val list = mutableListOf<PinjamanSekolah>()
                        for (child in snapshot.children) {
                            val item = child.getValue(PinjamanSekolah::class.java)
                            if (item != null) {
                                list.add(item)
                            }
                        }
                        if (lastPinjamanSekolahList == list) {
                            isInitializedMap["pinjaman_sekolah"] = true
                            return@launch
                        }
                        
                        isDownloadingMap["pinjaman_sekolah"] = true
                        lastPinjamanSekolahList = list
                        repository.replaceAllPinjamanSekolah(list)
                        isInitializedMap["pinjaman_sekolah"] = true
                        isDownloadingMap["pinjaman_sekolah"] = false
                        Log.d("FirebaseSyncManager", "PinjamanSekolah synced from Firebase: ${list.size} items")
                    } catch (e: Exception) {
                        isDownloadingMap["pinjaman_sekolah"] = false
                        Log.e("FirebaseSyncManager", "Error syncing PinjamanSekolah: ${e.message}")
                    }
                }
            }
            override fun onCancelled(error: DatabaseError) {
                Log.e("FirebaseSyncManager", "Firebase PinjamanSekolah cancelled: ${error.message}")
            }
        })

        // 4. Pinjaman Guru Listener
        database.getReference("pinjaman_guru").addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                scope.launch {
                    try {
                        val list = mutableListOf<PinjamanGuru>()
                        for (child in snapshot.children) {
                            val item = child.getValue(PinjamanGuru::class.java)
                            if (item != null) {
                                list.add(item)
                            }
                        }
                        if (lastPinjamanGuruList == list) {
                            isInitializedMap["pinjaman_guru"] = true
                            return@launch
                        }
                        
                        isDownloadingMap["pinjaman_guru"] = true
                        lastPinjamanGuruList = list
                        repository.replaceAllPinjamanGuru(list)
                        isInitializedMap["pinjaman_guru"] = true
                        isDownloadingMap["pinjaman_guru"] = false
                        Log.d("FirebaseSyncManager", "PinjamanGuru synced from Firebase: ${list.size} items")
                    } catch (e: Exception) {
                        isDownloadingMap["pinjaman_guru"] = false
                        Log.e("FirebaseSyncManager", "Error syncing PinjamanGuru: ${e.message}")
                    }
                }
            }
            override fun onCancelled(error: DatabaseError) {
                Log.e("FirebaseSyncManager", "Firebase PinjamanGuru cancelled: ${error.message}")
            }
        })

        // 5. Admin Kas Listener
        database.getReference("admin_kas").addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                scope.launch {
                    try {
                        val list = mutableListOf<AdminKas>()
                        for (child in snapshot.children) {
                            val item = child.getValue(AdminKas::class.java)
                            if (item != null) {
                                list.add(item)
                            }
                        }
                        if (lastAdminKasList == list) {
                            isInitializedMap["admin_kas"] = true
                            return@launch
                        }
                        
                        isDownloadingMap["admin_kas"] = true
                        lastAdminKasList = list
                        repository.replaceAllAdminKas(list)
                        isInitializedMap["admin_kas"] = true
                        isDownloadingMap["admin_kas"] = false
                        Log.d("FirebaseSyncManager", "AdminKas synced from Firebase: ${list.size} items")
                    } catch (e: Exception) {
                        isDownloadingMap["admin_kas"] = false
                        Log.e("FirebaseSyncManager", "Error syncing AdminKas: ${e.message}")
                    }
                }
            }
            override fun onCancelled(error: DatabaseError) {
                Log.e("FirebaseSyncManager", "Firebase AdminKas cancelled: ${error.message}")
            }
        })

        // 6. Setor Koperasi Listener
        database.getReference("setor_koperasi").addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                scope.launch {
                    try {
                        val list = mutableListOf<SetorKoperasi>()
                        for (child in snapshot.children) {
                            val item = child.getValue(SetorKoperasi::class.java)
                            if (item != null) {
                                list.add(item)
                            }
                        }
                        if (lastSetorKoperasiList == list) {
                            isInitializedMap["setor_koperasi"] = true
                            return@launch
                        }
                        
                        isDownloadingMap["setor_koperasi"] = true
                        lastSetorKoperasiList = list
                        repository.replaceAllSetorKoperasi(list)
                        isInitializedMap["setor_koperasi"] = true
                        isDownloadingMap["setor_koperasi"] = false
                        Log.d("FirebaseSyncManager", "SetorKoperasi synced from Firebase: ${list.size} items")
                    } catch (e: Exception) {
                        isDownloadingMap["setor_koperasi"] = false
                        Log.e("FirebaseSyncManager", "Error syncing SetorKoperasi: ${e.message}")
                    }
                }
            }
            override fun onCancelled(error: DatabaseError) {
                Log.e("FirebaseSyncManager", "Firebase SetorKoperasi cancelled: ${error.message}")
            }
        })

        // 7. Settings & Passwords listener
        database.getReference("settings").addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                try {
                    val editor = sharedPrefs.edit()
                    snapshot.child("schoolName").getValue(String::class.java)?.let {
                        editor.putString("school_name_pref", it)
                    }
                    snapshot.child("treasurerName").getValue(String::class.java)?.let {
                        editor.putString("treasurer_name_pref", it)
                    }
                    snapshot.child("pwd_BENDAHARA").getValue(String::class.java)?.let {
                        editor.putString("pwd_BENDAHARA", it)
                    }
                    snapshot.child("pwd_ADMIN").getValue(String::class.java)?.let {
                        editor.putString("pwd_ADMIN", it)
                    }
                    snapshot.child("pwd_KEPALA").getValue(String::class.java)?.let {
                        editor.putString("pwd_KEPALA", it)
                    }
                    editor.apply()
                    Log.d("FirebaseSyncManager", "Settings & credentials synced from Firebase.")
                } catch (e: Exception) {
                    Log.e("FirebaseSyncManager", "Error syncing settings: ${e.message}")
                }
            }
            override fun onCancelled(error: DatabaseError) {
                Log.e("FirebaseSyncManager", "Firebase settings cancelled: ${error.message}")
            }
        })
    }

    /**
     * Listen to local Room Flow changes and upload them immediately to Firebase
     * ONLY if our app has successfully downloaded/connected to Firebase at least once
     * and we are not currently downloading changes from Firebase for that specific table.
     */
    private fun setupRoomFlowUploads() {
        // Siswa Upload
        scope.launch {
            repository.allSiswa.collectLatest { list ->
                val isInitialized = isInitializedMap["siswa"] ?: false
                val isDownloading = isDownloadingMap["siswa"] ?: false
                
                if (!isInitialized) {
                    Log.d("FirebaseSyncManager", "Suppressed Siswa upload: Node is not yet initialized from Firebase.")
                    return@collectLatest
                }
                if (isDownloading) {
                    Log.d("FirebaseSyncManager", "Suppressed Siswa upload: Node is currently downloading modifications from Firebase.")
                    return@collectLatest
                }
                if (lastSiswaList == list) {
                    Log.d("FirebaseSyncManager", "Siswa list is identical to last seen, skipping upload.")
                    return@collectLatest
                }
                
                lastSiswaList = list
                Log.d("FirebaseSyncManager", "Uploading updated Siswa list to Firebase... Total: ${list.size}")
                database.getReference("siswa").setValue(list)
            }
        }

        // Transaksi Upload
        scope.launch {
            repository.allTransaksi.collectLatest { list ->
                val isInitialized = isInitializedMap["transaksi"] ?: false
                val isDownloading = isDownloadingMap["transaksi"] ?: false
                
                if (!isInitialized) {
                    Log.d("FirebaseSyncManager", "Suppressed Transaksi upload: Node is not yet initialized from Firebase.")
                    return@collectLatest
                }
                if (isDownloading) {
                    Log.d("FirebaseSyncManager", "Suppressed Transaksi upload: Node is currently downloading modifications from Firebase.")
                    return@collectLatest
                }
                if (lastTransaksiList == list) {
                    Log.d("FirebaseSyncManager", "Transaksi list is identical to last seen, skipping upload.")
                    return@collectLatest
                }
                
                lastTransaksiList = list
                Log.d("FirebaseSyncManager", "Uploading updated Transaksi list to Firebase... Total: ${list.size}")
                database.getReference("transaksi").setValue(list)
            }
        }

        // Pinjaman Sekolah Upload
        scope.launch {
            repository.allPinjamanSekolah.collectLatest { list ->
                val isInitialized = isInitializedMap["pinjaman_sekolah"] ?: false
                val isDownloading = isDownloadingMap["pinjaman_sekolah"] ?: false
                
                if (!isInitialized) {
                    Log.d("FirebaseSyncManager", "Suppressed PinjamanSekolah upload: Node is not yet initialized from Firebase.")
                    return@collectLatest
                }
                if (isDownloading) {
                    Log.d("FirebaseSyncManager", "Suppressed PinjamanSekolah upload: Node is currently downloading modifications from Firebase.")
                    return@collectLatest
                }
                if (lastPinjamanSekolahList == list) {
                    Log.d("FirebaseSyncManager", "PinjamanSekolah list is identical to last seen, skipping upload.")
                    return@collectLatest
                }
                
                lastPinjamanSekolahList = list
                Log.d("FirebaseSyncManager", "Uploading updated PinjamanSekolah list to Firebase... Total: ${list.size}")
                database.getReference("pinjaman_sekolah").setValue(list)
            }
        }

        // Pinjaman Guru Upload
        scope.launch {
            repository.allPinjamanGuru.collectLatest { list ->
                val isInitialized = isInitializedMap["pinjaman_guru"] ?: false
                val isDownloading = isDownloadingMap["pinjaman_guru"] ?: false
                
                if (!isInitialized) {
                    Log.d("FirebaseSyncManager", "Suppressed PinjamanGuru upload: Node is not yet initialized from Firebase.")
                    return@collectLatest
                }
                if (isDownloading) {
                    Log.d("FirebaseSyncManager", "Suppressed PinjamanGuru upload: Node is currently downloading modifications from Firebase.")
                    return@collectLatest
                }
                if (lastPinjamanGuruList == list) {
                    Log.d("FirebaseSyncManager", "PinjamanGuru list is identical to last seen, skipping upload.")
                    return@collectLatest
                }
                
                lastPinjamanGuruList = list
                Log.d("FirebaseSyncManager", "Uploading updated PinjamanGuru list to Firebase... Total: ${list.size}")
                database.getReference("pinjaman_guru").setValue(list)
            }
        }

        // Admin Kas Upload
        scope.launch {
            repository.allAdminKas.collectLatest { list ->
                val isInitialized = isInitializedMap["admin_kas"] ?: false
                val isDownloading = isDownloadingMap["admin_kas"] ?: false
                
                if (!isInitialized) {
                    Log.d("FirebaseSyncManager", "Suppressed AdminKas upload: Node is not yet initialized from Firebase.")
                    return@collectLatest
                }
                if (isDownloading) {
                    Log.d("FirebaseSyncManager", "Suppressed AdminKas upload: Node is currently downloading modifications from Firebase.")
                    return@collectLatest
                }
                if (lastAdminKasList == list) {
                    Log.d("FirebaseSyncManager", "AdminKas list is identical to last seen, skipping upload.")
                    return@collectLatest
                }
                
                lastAdminKasList = list
                Log.d("FirebaseSyncManager", "Uploading updated AdminKas list to Firebase... Total: ${list.size}")
                database.getReference("admin_kas").setValue(list)
            }
        }

        // Setor Koperasi Upload
        scope.launch {
            repository.allSetorKoperasi.collectLatest { list ->
                val isInitialized = isInitializedMap["setor_koperasi"] ?: false
                val isDownloading = isDownloadingMap["setor_koperasi"] ?: false
                
                if (!isInitialized) {
                    Log.d("FirebaseSyncManager", "Suppressed SetorKoperasi upload: Node is not yet initialized from Firebase.")
                    return@collectLatest
                }
                if (isDownloading) {
                    Log.d("FirebaseSyncManager", "Suppressed SetorKoperasi upload: Node is currently downloading modifications from Firebase.")
                    return@collectLatest
                }
                if (lastSetorKoperasiList == list) {
                    Log.d("FirebaseSyncManager", "SetorKoperasi list is identical to last seen, skipping upload.")
                    return@collectLatest
                }
                
                lastSetorKoperasiList = list
                Log.d("FirebaseSyncManager", "Uploading updated SetorKoperasi list to Firebase... Total: ${list.size}")
                database.getReference("setor_koperasi").setValue(list)
            }
        }
    }

    /**
     * Explicit trigger to push settings to Firebase when updated by the user locally.
     */
    fun pushSettingsToFirebase(schoolName: String, treasurerName: String) {
        val settingsRef = database.getReference("settings")
        settingsRef.child("schoolName").setValue(schoolName.trim().uppercase())
        settingsRef.child("treasurerName").setValue(treasurerName.trim())
    }

    /**
     * Explicit trigger to push a modified password to Firebase.
     */
    fun pushPasswordToFirebase(roleName: String, passwordValue: String) {
        database.getReference("settings").child("pwd_$roleName").setValue(passwordValue)
    }
}
