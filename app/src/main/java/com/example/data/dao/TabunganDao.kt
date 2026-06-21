package com.example.data.dao

import androidx.room.*
import com.example.data.model.*
import kotlinx.coroutines.flow.Flow

@Dao
interface TabunganDao {
    // Siswa Queries
    @Query("SELECT * FROM siswa ORDER BY nama ASC")
    fun getAllSiswa(): Flow<List<Siswa>>

    @Query("SELECT * FROM siswa WHERE id = :id")
    suspend fun getSiswaById(id: Int): Siswa?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSiswa(siswa: Siswa): Long

    @Query("DELETE FROM siswa WHERE id = :id")
    suspend fun deleteSiswa(id: Int)

    // Transaksi Queries
    @Query("SELECT * FROM transaksi ORDER BY tanggal DESC")
    fun getAllTransaksi(): Flow<List<Transaksi>>

    @Query("SELECT * FROM transaksi WHERE siswaId = :siswaId ORDER BY tanggal DESC")
    fun getTransaksiBySiswa(siswaId: Int): Flow<List<Transaksi>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransaksi(transaksi: Transaksi): Long

    @Query("DELETE FROM transaksi WHERE id = :id")
    suspend fun deleteTransaksi(id: Int)

    // Pinjaman Sekolah Queries
    @Query("SELECT * FROM pinjaman_sekolah ORDER BY tanggalPinjam DESC")
    fun getAllPinjamanSekolah(): Flow<List<PinjamanSekolah>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPinjamanSekolah(pinjaman: PinjamanSekolah): Long

    @Update
    suspend fun updatePinjamanSekolah(pinjaman: PinjamanSekolah)

    @Query("DELETE FROM pinjaman_sekolah WHERE id = :id")
    suspend fun deletePinjamanSekolah(id: Int)

    // Pinjaman Guru Queries
    @Query("SELECT * FROM pinjaman_guru ORDER BY tanggalPinjam DESC")
    fun getAllPinjamanGuru(): Flow<List<PinjamanGuru>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPinjamanGuru(pinjaman: PinjamanGuru): Long

    @Update
    suspend fun updatePinjamanGuru(pinjaman: PinjamanGuru)

    @Query("DELETE FROM pinjaman_guru WHERE id = :id")
    suspend fun deletePinjamanGuru(id: Int)

    // Admin Kas Queries
    @Query("SELECT * FROM admin_kas ORDER BY tanggal DESC")
    fun getAllAdminKas(): Flow<List<AdminKas>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAdminKas(kas: AdminKas): Long

    @Query("DELETE FROM admin_kas WHERE id = :id")
    suspend fun deleteAdminKas(id: Int)

    // Setor Koperasi Queries
    @Query("SELECT * FROM setor_koperasi ORDER BY tanggal DESC")
    fun getAllSetorKoperasi(): Flow<List<SetorKoperasi>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSetorKoperasi(setor: SetorKoperasi): Long

    @Query("DELETE FROM setor_koperasi WHERE id = :id")
    suspend fun deleteSetorKoperasi(id: Int)

    @Query("DELETE FROM setor_koperasi")
    suspend fun clearSetorKoperasi()

    // Clear all tables for Reset Application / New Academic Year
    @Query("DELETE FROM siswa")
    suspend fun clearSiswa()

    @Query("DELETE FROM transaksi")
    suspend fun clearTransaksi()

    @Query("DELETE FROM pinjaman_sekolah")
    suspend fun clearPinjamanSekolah()

    @Query("DELETE FROM pinjaman_guru")
    suspend fun clearPinjamanGuru()

    @Query("DELETE FROM admin_kas")
    suspend fun clearAdminKas()

    @Transaction
    suspend fun replaceAllSiswa(list: List<Siswa>) {
        clearSiswa()
        list.forEach { insertSiswa(it) }
    }

    @Transaction
    suspend fun replaceAllTransaksi(list: List<Transaksi>) {
        clearTransaksi()
        list.forEach { insertTransaksi(it) }
    }

    @Transaction
    suspend fun replaceAllPinjamanSekolah(list: List<PinjamanSekolah>) {
        clearPinjamanSekolah()
        list.forEach { insertPinjamanSekolah(it) }
    }

    @Transaction
    suspend fun replaceAllPinjamanGuru(list: List<PinjamanGuru>) {
        clearPinjamanGuru()
        list.forEach { insertPinjamanGuru(it) }
    }

    @Transaction
    suspend fun replaceAllAdminKas(list: List<AdminKas>) {
        clearAdminKas()
        list.forEach { insertAdminKas(it) }
    }

    @Transaction
    suspend fun replaceAllSetorKoperasi(list: List<SetorKoperasi>) {
        clearSetorKoperasi()
        list.forEach { insertSetorKoperasi(it) }
    }
}
