package com.example.empati.data.lemonade

data class LemonadeStatusSummary(
    val title: String,
    val detail: String,
    val ready: Boolean,
    val missingAliases: List<String> = emptyList()
)

/**
 * Menerjemahkan hasil pemeriksaan server menjadi dua kalimat untuk pengguna.
 * Dipisah dari Compose supaya bisa diuji tanpa Android.
 */
object LemonadeStatusPresenter {

    fun summarize(status: LemonadeStatus?, error: String?): LemonadeStatusSummary = when {
        error != null -> LemonadeStatusSummary(
            title = "Server belum tersambung",
            detail = "Pastikan Lemonade berjalan di port 13305, lalu periksa lagi.",
            ready = false
        )

        status == null -> LemonadeStatusSummary(
            title = "Belum diperiksa",
            detail = "Periksa kesiapan server Lemonade sebelum mulai berkonsultasi.",
            ready = false
        )

        status.isReady -> LemonadeStatusSummary(
            title = "Siap dipakai",
            detail = "Empati memakai model lokal di port 13305. Tidak ada data yang keluar dari perangkat.",
            ready = true
        )

        else -> LemonadeStatusSummary(
            title = "Sebagian siap",
            detail = "Alias belum terpasang: ${status.missingAliases.joinToString(", ")}. " +
                "Jalankan scripts/setup-lemonade.ps1 untuk melengkapinya.",
            ready = false,
            missingAliases = status.missingAliases
        )
    }
}
