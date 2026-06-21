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
import kotlinx.coroutines.withContext

class FirebaseSyncManager(
    private val context: Context,
    private val repository: TabunganRepository
) {
    private val databaseUrl = "https://tabsis-online-d3d5a-default-rtdb.asia-southeast1.firebasedatabase.app/"
    private val database = FirebaseDatabase.getInstance(databaseUrl)
    private val scope = CoroutineScope(Dispatchers.IO)

    // Flag to prevent update loop (uploading down-synced data back to Firebase)
    @Volatile
    private var isDownloadingLocalChanges = false

    private val sharedPrefs by lazy {
        context.getSharedPreferences("user_credentials_v1", Context.MODE_PRIVATE)
    }

    fun startSyncing() {
        Log.d("FirebaseSyncManager", "Starting Real-Time Firebase Database Sync...")
        setupFirebaseListeners()
        setupRoomFlowUploads()
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
                        isDownloadingLocalChanges = true
                        repository.replaceAllSiswa(list)
                        isDownloadingLocalChanges = false
                        Log.d("FirebaseSyncManager", "Siswa synced from Firebase: ${list.size} items")
                    } catch (e: Exception) {
                        isDownloadingLocalChanges = false
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
                        isDownloadingLocalChanges = true
                        repository.replaceAllTransaksi(list)
                        isDownloadingLocalChanges = false
                        Log.d("FirebaseSyncManager", "Transaksi synced from Firebase: ${list.size} items")
                    } catch (e: Exception) {
                        isDownloadingLocalChanges = false
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
                        isDownloadingLocalChanges = true
                        repository.replaceAllPinjamanSekolah(list)
                        isDownloadingLocalChanges = false
                        Log.d("FirebaseSyncManager", "PinjamanSekolah synced from Firebase: ${list.size} items")
                    } catch (e: Exception) {
                        isDownloadingLocalChanges = false
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
                        isDownloadingLocalChanges = true
                        repository.replaceAllPinjamanGuru(list)
                        isDownloadingLocalChanges = false
                        Log.d("FirebaseSyncManager", "PinjamanGuru synced from Firebase: ${list.size} items")
                    } catch (e: Exception) {
                        isDownloadingLocalChanges = false
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
                        isDownloadingLocalChanges = true
                        repository.replaceAllAdminKas(list)
                        isDownloadingLocalChanges = false
                        Log.d("FirebaseSyncManager", "AdminKas synced from Firebase: ${list.size} items")
                    } catch (e: Exception) {
                        isDownloadingLocalChanges = false
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
                        isDownloadingLocalChanges = true
                        repository.replaceAllSetorKoperasi(list)
                        isDownloadingLocalChanges = false
                        Log.d("FirebaseSyncManager", "SetorKoperasi synced from Firebase: ${list.size} items")
                    } catch (e: Exception) {
                        isDownloadingLocalChanges = false
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
     * if we are not currently downloading changes from Firebase.
     */
    private fun setupRoomFlowUploads() {
        // Siswa Upload
        scope.launch {
            repository.allSiswa.collectLatest { list ->
                if (!isDownloadingLocalChanges) {
                    Log.d("FirebaseSyncManager", "Uploading updated Siswa list to Firebase... Total: ${list.size}")
                    database.getReference("siswa").setValue(list)
                }
            }
        }

        // Transaksi Upload
        scope.launch {
            repository.allTransaksi.collectLatest { list ->
                if (!isDownloadingLocalChanges) {
                    Log.d("FirebaseSyncManager", "Uploading updated Transaksi list to Firebase... Total: ${list.size}")
                    database.getReference("transaksi").setValue(list)
                }
            }
        }

        // Pinjaman Sekolah Upload
        scope.launch {
            repository.allPinjamanSekolah.collectLatest { list ->
                if (!isDownloadingLocalChanges) {
                    Log.d("FirebaseSyncManager", "Uploading updated PinjamanSekolah list to Firebase... Total: ${list.size}")
                    database.getReference("pinjaman_sekolah").setValue(list)
                }
            }
        }

        // Pinjaman Guru Upload
        scope.launch {
            repository.allPinjamanGuru.collectLatest { list ->
                if (!isDownloadingLocalChanges) {
                    Log.d("FirebaseSyncManager", "Uploading updated PinjamanGuru list to Firebase... Total: ${list.size}")
                    database.getReference("pinjaman_guru").setValue(list)
                }
            }
        }

        // Admin Kas Upload
        scope.launch {
            repository.allAdminKas.collectLatest { list ->
                if (!isDownloadingLocalChanges) {
                    Log.d("FirebaseSyncManager", "Uploading updated AdminKas list to Firebase... Total: ${list.size}")
                    database.getReference("admin_kas").setValue(list)
                }
            }
        }

        // Setor Koperasi Upload
        scope.launch {
            repository.allSetorKoperasi.collectLatest { list ->
                if (!isDownloadingLocalChanges) {
                    Log.d("FirebaseSyncManager", "Uploading updated SetorKoperasi list to Firebase... Total: ${list.size}")
                    database.getReference("setor_koperasi").setValue(list)
                }
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
