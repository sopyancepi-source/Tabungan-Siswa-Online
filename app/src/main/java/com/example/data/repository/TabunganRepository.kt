package com.example.data.repository

import com.example.data.dao.TabunganDao
import com.example.data.model.*
import kotlinx.coroutines.flow.Flow

class TabunganRepository(private val dao: TabunganDao) {
    val allSiswa: Flow<List<Siswa>> = dao.getAllSiswa()
    val allTransaksi: Flow<List<Transaksi>> = dao.getAllTransaksi()
    val allPinjamanSekolah: Flow<List<PinjamanSekolah>> = dao.getAllPinjamanSekolah()
    val allPinjamanGuru: Flow<List<PinjamanGuru>> = dao.getAllPinjamanGuru()
    val allAdminKas: Flow<List<AdminKas>> = dao.getAllAdminKas()
    val allSetorKoperasi: Flow<List<SetorKoperasi>> = dao.getAllSetorKoperasi()

    fun getTransaksiBySiswa(siswaId: Int): Flow<List<Transaksi>> = dao.getTransaksiBySiswa(siswaId)

    suspend fun getSiswaById(id: Int): Siswa? = dao.getSiswaById(id)

    suspend fun insertSiswa(siswa: Siswa): Long = dao.insertSiswa(siswa)

    suspend fun deleteSiswa(id: Int) = dao.deleteSiswa(id)

    suspend fun insertTransaksi(transaksi: Transaksi): Long = dao.insertTransaksi(transaksi)

    suspend fun deleteTransaksi(id: Int) = dao.deleteTransaksi(id)

    suspend fun insertPinjamanSekolah(pinjaman: PinjamanSekolah): Long = dao.insertPinjamanSekolah(pinjaman)

    suspend fun updatePinjamanSekolah(pinjaman: PinjamanSekolah) = dao.updatePinjamanSekolah(pinjaman)

    suspend fun deletePinjamanSekolah(id: Int) = dao.deletePinjamanSekolah(id)

    suspend fun insertPinjamanGuru(pinjaman: PinjamanGuru): Long = dao.insertPinjamanGuru(pinjaman)

    suspend fun updatePinjamanGuru(pinjaman: PinjamanGuru) = dao.updatePinjamanGuru(pinjaman)

    suspend fun deletePinjamanGuru(id: Int) = dao.deletePinjamanGuru(id)

    suspend fun insertAdminKas(kas: AdminKas): Long = dao.insertAdminKas(kas)

    suspend fun deleteAdminKas(id: Int) = dao.deleteAdminKas(id)

    suspend fun insertSetorKoperasi(setor: SetorKoperasi): Long = dao.insertSetorKoperasi(setor)

    suspend fun deleteSetorKoperasi(id: Int) = dao.deleteSetorKoperasi(id)

    suspend fun clearAllDatabase() {
        dao.clearTransaksi()
        dao.clearSiswa()
        dao.clearPinjamanSekolah()
        dao.clearPinjamanGuru()
        dao.clearAdminKas()
        dao.clearSetorKoperasi()
    }

    suspend fun replaceAllSiswa(list: List<Siswa>) = dao.replaceAllSiswa(list)
    suspend fun replaceAllTransaksi(list: List<Transaksi>) = dao.replaceAllTransaksi(list)
    suspend fun replaceAllPinjamanSekolah(list: List<PinjamanSekolah>) = dao.replaceAllPinjamanSekolah(list)
    suspend fun replaceAllPinjamanGuru(list: List<PinjamanGuru>) = dao.replaceAllPinjamanGuru(list)
    suspend fun replaceAllAdminKas(list: List<AdminKas>) = dao.replaceAllAdminKas(list)
    suspend fun replaceAllSetorKoperasi(list: List<SetorKoperasi>) = dao.replaceAllSetorKoperasi(list)
}
