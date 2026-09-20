# Rancangan & Analisis Fitur Super Admin: Enterprise Command & Analytics Center

Dokumen ini memuat analisis mendalam dan rancangan fitur lengkap untuk **Super Admin** pada aplikasi SIRAM, mencakup analitik lanjutan, pelaporan keuangan, dan kontrol operasional.

---

## 1. Modul Analitik & Statistik Lanjutan (`Super Admin Analytics`)
Super Admin memerlukan wawasan mendalam (*insights*) mengenai performa bank sampah secara keseluruhan:
* **Grafik Tren Daur Ulang Bulanan:** Visualisasi data tonase sampah berdasarkan kategori (Plastik, Kardus, Kertas, Logam, Organik) dalam bentuk diagram batang/garis.
* **Metrik Produktivitas Kurir:** Perbandingan jumlah penjemputan dan tonase yang berhasil diselesaikan oleh masing-masing kurir di lapangan.
* **Pertumbuhan Nasabah:** Grafik pertumbuhan jumlah pendaftaran nasabah baru per minggu dan tingkat keaktifan nasabah dalam menyetor sampah.

---

## 2. Modul Laporan Keuangan & Akuntansi (`Financial Reports`)
Untuk transparansi dan pembukuan resmi bank sampah:
* **Rekapitulasi Kas & Saldo:**
  * Total seluruh saldo yang dimiliki nasabah.
  * Total akumulasi dana yang telah berhasil ditransfer (*pencairan sukses*).
  * Saldo cadangan operasional sistem.
* **Pusat Ekspor Laporan (PDF / Excel):**
  * Fitur cetak laporan keuangan bulanan/tahunan secara otomatis menggunakan `PdfGeneratorHelper` untuk keperluan audit internal maupun eksternal.
* **Riwayat Mutasi Sistem:** Log lengkap seluruh penambahan saldo (dari hasil setoran/jemput) dan pengurangan saldo (dari penarikan).

---

## 3. Modul Pemantauan Armada Multi-GPS (`Live Fleet Command Map`)
* **Peta Komando Terpusat:** Satu layar peta interaktif yang memuat **seluruh kurir aktif** secara bersamaan.
* **Detail Kurir:** Mengklik *marker* kurir di peta akan menampilkan pop-up berisi:
  * Nama Kurir & Nomor HP.
  * Status Tugas (Sedang Menjemput / Standby).
  * Koordinat GPS terakhir dan waktu pembaruan (*Last Seen*).

---

## 4. Modul Manajemen Sistem & Hak Akses (`System Control`)
* **Pusat Kontrol Harga Global:** Penentuan standar harga beli per kg untuk seluruh jenis sampah yang berlaku secara nasional di aplikasi.
* **Manajemen Akun Staff & Kurir:**
  * Tambah akun kurir/admin baru dengan pilihan *role* (`admin` / `super_admin`).
  * Blokir atau ubah status akun operasional yang tidak aktif.
* **Log Aktivitas Sistem (*Audit Trail*):** Catatan siapa admin yang memvalidasi suatu transaksi, kapan harga diubah, dan kapan pencairan disetujui.
