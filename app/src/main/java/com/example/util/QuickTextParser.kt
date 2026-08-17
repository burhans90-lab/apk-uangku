package com.example.util

import com.example.data.model.TransactionCategory
import com.example.data.model.TransactionEntity
import com.example.data.model.TransactionType
import java.util.Locale

data class ParsedTransaction(
    val title: String,
    val amount: Double,
    val type: TransactionType,
    val category: TransactionCategory,
    val matchedFromHistory: Boolean = false
)

object QuickTextParser {

    /**
     * Highly optimized dictionary of Indonesian everyday keywords & slang mapped to categories.
     * Ordered by specificity: multi-word phrases first, followed by specific single keywords.
     */
    private val CATEGORY_KEYWORD_MAP: Map<TransactionCategory, List<String>> = mapOf(
        TransactionCategory.MAKANAN to listOf(
            // Minuman & Minuman Kekinian
            "es teh manis", "es teh", "teh manis", "teh botol", "teh pucuk", "teh tarik", "thai tea", "matcha latte",
            "kopi susu", "kopi hitam", "kopi tubruk", "kopi kenangan", "janji jiwa", "point coffee", "fore coffee",
            "cappuccino", "espresso", "americano", "latte", "jus alpukat", "jus mangga", "jus jeruk", "jus buah",
            "air mineral", "le minerale", "aqua botol", "cleo", "vit", "nestle pure", "air minum", "pop ice",
            "es kelapa", "kelapa muda", "es degan", "es cendol", "es dawet", "es campur", "es teler", "es cincau",
            "boba", "chatime", "mixue", "smoothies", "milkshake", "nutrisari", "floridina", "buavita", "sirup marjan",
            "wedang ronde", "wedang jahe", "stmj", "bandrek", "sekoteng", "minuman", "minum", "haus",

            // Makanan Pokok & Warung / Restoran
            "nasi goreng", "nasi uduk", "nasi padang", "nasi kuning", "nasi liwet", "nasi bakar", "nasi rames",
            "nasi campur", "nasi kucing", "nasi pecel", "nasi bebek", "nasi rawon", "nasi gudeg", "nasi timbel",
            "mie ayam", "mie goreng", "mie rebus", "mie kuah", "mie aceh", "mie celor", "mie jebew", "mie gacoan",
            "bakso malang", "bakso urat", "bakso telur", "bakso bakar", "bakso aci", "mie bakso", "soto ayam",
            "soto betawi", "soto lamongan", "soto madura", "soto babat", "soto mie", "ayam geprek", "ayam bakar",
            "ayam penyet", "ayam goreng", "ayam crispy", "ayam rica", "ayam serundeng", "bebek goreng", "bebek bakar",
            "bebek sinjay", "bebek madura", "ikan bakar", "ikan goreng", "pecel lele", "lele goreng", "gurame bakar",
            "nila bakar", "seafood", "cumi bakar", "udang saus", "kepiting", "rendang", "gulai", "rawon", "gudeg",
            "pecel", "sate ayam", "sate kambing", "sate padang", "sate taichan", "seblak", "seblak prasmanan",
            "kwetiau", "bihun goreng", "bihun kuah", "capcay", "fuyunghai", "koloke", "kebab", "burger", "pizza",
            "hotdog", "sandwich", "steak", "chicken steak", "ramen", "udon", "dimsum", "siomay", "batagor",
            "pempek", "tekwan", "lontong sayur", "lontong kari", "ketoprak", "gado gado", "gado-gado", "karedok",
            "bubur ayam", "bubur kacang", "burjo", "catering", "katering", "makan siang", "makan malam", "sarapan",
            "lunch", "dinner", "warteg", "warmindo", "angkringan", "cafe", "kafe", "resto", "restoran",
            "mcdonalds", "mcd", "kfc", "hokben", "richeese", "burger king", "solaria", "d'cost", "subway", "a&w",
            "nasi", "mie", "bakso", "soto", "ayam", "bebek", "ikan", "lele", "sate", "seblak", "burger", "pizza",
            "dimsum", "siomay", "batagor", "pempek", "bubur", "kopi", "teh", "jus",
            "makan", "lauk", "kuliner"
        ),

        TransactionCategory.JAJAN to listOf(
            "martabak manis", "martabak telur", "terang bulan", "roti bakar", "roti canai", "roti boy", "roti o",
            "donat jco", "dunkin donuts", "pisang goreng", "tahu bulat", "tahu crispy", "tahu jeletot", "tahu gejrot",
            "cireng", "cimol", "cilok", "cilor", "makaroni ngehe", "makaroni pedas", "telur gulung", "semol",
            "gorengan", "bakwan", "mendoan", "tempe kemul", "risol mayo", "risoles", "pastel", "lemper", "kue pukis",
            "kue cubit", "klepon", "onde onde", "odading", "cakwe", "croissant", "pastry", "churros",
            "eskrim", "es krim", "ice cream", "aice", "walls", "cornetto", "magnum", "chiki", "taro", "lays",
            "potabee", "kusuka", "chitato", "oreo", "pocky", "chocolatos", "wafer tango", "silverqueen", "cadbury",
            "cokelat", "coklat", "permen", "yupi", "popcorn", "keripik singkong", "keripik kaca", "kerupuk seblak",
            "martabak", "donat", "roti", "kue", "keripik", "kerupuk", "wafer",
            "jajan", "snack", "camilan", "cemilan", "nyemil", "ngemil"
        ),

        TransactionCategory.KEBUTUHAN_DAPUR to listOf(
            "gas lpg", "gas elpiji", "gas 3kg", "gas 12kg", "isi ulang gas", "aqua galon", "galon le minerale",
            "isi ulang galon", "isi ulang air", "minyak goreng", "minyak kelapa", "minyak bimoli", "minyak tropical",
            "beras ramos", "beras pandan", "beras merah", "beras ketan", "beras 5kg", "bawang merah", "bawang putih",
            "bawang bombay", "cabai rawit", "cabe merah", "cabe keriting", "tomat segar", "sayur mayur", "sayuran",
            "kangkung", "bayam", "wortel", "kentang", "buncis", "kubis", "sawi", "tempe mentah", "tahu mentah",
            "telur ayam", "telor ayam", "telur puyuh", "telur bebek", "daging sapi", "ayam potong", "ikan basah",
            "garam dapur", "gula pasir", "gula merah", "gula aren", "kecap manis", "saos tiram", "saus sambal",
            "royco", "masako", "santan kara", "tepung terigu", "tepung bumbu", "tepung tapioka", "mentega", "blueband",
            "beras", "minyak", "bawang", "cabai", "cabe", "sayur", "daging", "telur", "telor", "garam", "gula", "kecap", "tepung", "gas", "galon",
            "bumbu dapur", "belanja pasar", "tukang sayur", "sembako", "belanja sayur", "dapur", "bumbu"
        ),

        TransactionCategory.TRANSPORT to listOf(
            "bensin", "pertalite", "pertamax turbo", "pertamax", "solar", "dexlite", "isi bensin", "spbu", "shell v-power",
            "parkir motor", "parkir mobil", "karcis parkir", "etoll", "e-toll", "isi tol", "tarif tol", "gerbang tol",
            "gojek", "goride", "gocar", "gofood ongkir", "grab", "grabike", "grabcar", "maxim", "indrive", "ojol", "ojek",
            "taksi", "bluebird", "angkot", "mikrolet", "transjakarta", "busway", "damri", "bus antar kota",
            "kereta api", "krl commuter", "commuter line", "mrt jakarta", "lrt jakarta", "lrt jabodebek", "kai access",
            "tiket pesawat", "garuda", "lion air", "citilink", "super air jet", "tiket kapal", "kapal feri", "penyeberangan",
            "travel shuttle", "ongkir", "ongkos kirim", "j&t", "jne", "sicepat", "anteraja", "pos indonesia"
        ),

        TransactionCategory.PERAWATAN_KENDARAAN to listOf(
            "ganti oli", "oli mesin", "oli motor", "oli mobil", "oli shell", "oli castrol", "oli motul", "oli yamalube",
            "servis motor", "servis mobil", "service motor", "service rutin", "tune up", "bengkel resmi", "ahass", "auto2000",
            "tambal ban", "ganti ban", "ban motor", "ban mobil", "ban tubeless", "isi angin", "nitrogen", "aki motor", "aki mobil",
            "kampas rem", "kampas kopling", "ganti busi", "busi motor", "filter udara", "filter oli", "rantai motor",
            "cuci motor", "cuci mobil", "steam motor", "steam mobil", "salon mobil", "poles mobil", "spooring", "balancing",
            "helm", "kaca helm", "sparepart", "onderdil", "stnk", "pajak motor", "pajak mobil", "perpanjang sim", "bengkel"
        ),

        TransactionCategory.BELANJA to listOf(
            "indomaret", "alfamart", "alfamidi", "superindo", "hypermart", "transmart", "lotte mart", "farmer market",
            "shopee", "tokopedia", "lazada", "tiktok shop", "zalora", "blibli", "uniqlo", "h&m", "zara", "matahari",
            "kaos", "kemeja", "celana jeans", "celana pendek", "rok", "gamis", "hijab", "jilbab", "jaket", "hoodie",
            "sweater", "sepatu sneakers", "sepatu kerja", "sandal", "tas ransel", "tas selempang", "dompet", "topi", "kaos kaki",
            "sabun mandi", "lifebuoy", "biore", "dettol", "shampoo", "shampo", "pantene", "clear", "head & shoulders",
            "odol pepsodent", "odol", "pasta gigi", "sikat gigi", "sabun cuci", "deterjen rinso", "deterjen daia", "molto",
            "downy", "pewangi pakaian", "sunlight", "mama lemon", "tisu paseo", "tisu wajah", "tissue",
            "skincare", "serum wajah", "sunscreen", "toner", "moisturizer", "facial wash", "micellar water",
            "lipstik", "bedak", "cushion", "parfum", "body lotion", "deodorant", "pomade", "alat cukur",
            "buku tulis", "pulpen", "atk kantor", "baterai", "lampu philips", "perabotan", "belanja"
        ),

        TransactionCategory.NAFKAH_KELUARGA to listOf(
            "nafkah istri", "nafkah keluarga", "uang belanja bulanan", "jatah bulanan", "uang belanja istri",
            "pempers", "popok bayi", "diapers", "mamypoko", "mamy poko", "sweety silver", "merries",
            "susu formula", "susu bayi", "morinaga", "sgm ananda", "sgm eksplor", "dancow 1+", "chil kid", "pediasure",
            "bubur bayi", "biskuit bayi", "perlengkapan bayi", "botol susu", "minyak telon", "telon my baby",
            "telon habbie", "sabun bayi", "baby oil", "mainan anak", "baju anak", "uang saku anak", "uang jajan anak",
            "nafkah", "istri", "anak"
        ),

        TransactionCategory.KESEHATAN to listOf(
            "paracetamol", "panadol", "bodrex", "sanmol", "biogesic", "amoxicillin", "tolak angin", "antangin",
            "minyak kayu putih", "salonpas", "koyo cabe", "betadine", "hansaplast", "perban", "alkohol medis",
            "vitamin c", "enervon c", "imboost", "stimuno", "sangobion", "suplemen", "madu tj", "madu uray",
            "apotek k24", "apotek kimia farma", "apotik", "resep dokter", "berobat dokter", "dokter umum",
            "dokter spesialis", "dokter anak", "dokter gigi", "cabut gigi", "tambal gigi", "scaling gigi",
            "rumah sakit", "rsud", "puskesmas", "klinik pratama", "tes darah", "cek lab", "rapid test",
            "kacamata optik", "periksa mata", "tensimeter", "termometer", "fisioterapi", "obat", "berobat", "sakit"
        ),

        TransactionCategory.PENDIDIKAN to listOf(
            "spp sekolah", "spp bulanan", "ukt kuliah", "uang kuliah", "uang semester", "daftar ulang", "biaya pendaftaran",
            "les matematika", "les inggris", "bimbel ganesha", "bimbel inten", "ruangguru", "zenius", "kursus stir",
            "buku paket", "buku pelajaran", "buku cetak", "novel edukasi", "fotocopy materi", "print tugas", "jilid skripsi",
            "uang kas kelas", "seragam sekolah", "tas sekolah anak", "sepatu sekolah", "ujian nasional", "ujian semester",
            "wisuda", "toga", "seminar", "workshop", "webinar", "pendidikan", "sekolah", "kuliah", "les"
        ),

        TransactionCategory.TAGIHAN to listOf(
            "token pln", "token listrik", "listrik pascabayar", "tagihan listrik", "pln mobile", "air pdam", "tagihan air",
            "indihome", "biznet", "first media", "myrepublic", "oxygen", "wifi rumah", "tagihan wifi",
            "pulsa telkomsel", "pulsa indosat", "pulsa xl", "pulsa tri", "pulsa smartfren", "pulsa axis", "pulsa byu",
            "paket data", "kuota internet", "kuota data", "bpjs kesehatan", "bpjs ketenagakerjaan", "iuran bpjs",
            "sewa kos", "uang kos", "kost", "kontrakan rumah", "sewa rumah", "iuran sampah", "iuran keamanan", "ipl apartemen",
            "cicilan motor", "cicilan mobil", "angsuran kpr", "paylater", "spaylater", "gopaylater", "kredivo", "akulaku",
            "tagihan kartu kredit", "pajak pbb", "tagihan", "iuran", "pulsa", "listrik"
        ),

        TransactionCategory.HIBURAN to listOf(
            "nonton bioskop", "tiket bioskop", "cinema xxi", "cgv blitz", "cinepolis", "mtix", "tix id",
            "topup game", "top up diamond", "diamond mlbb", "mobile legends", "free fire", "genshin impact", "valorant",
            "steam wallet", "playstation plus", "sewa ps", "rental playstation", "billiard", "biliar", "karaoke",
            "netflix bulanan", "spotify premium", "youtube premium", "disney hotstar", "vidio premier",
            "membership gym", "fitness club", "sewa lapangan futsal", "sewa lapangan badminton", "tiket kolam renang",
            "liburan keluarga", "staycation", "booking hotel", "tiket masuk wisata", "tiket ancol", "dufan", "taman safari",
            "tiket konser", "konser musik", "hobi", "game", "hiburan", "nonton", "bioskop"
        ),

        TransactionCategory.INVESTASI to listOf(
            "beli saham", "reksadana bibit", "reksa dana", "bareksa", "ajaib sekuritas", "indopremier", "ipot",
            "tabungan emas", "emas antam", "pegadaian emas", "pluang", "toko crypto", "indodax", "tokocrypto",
            "binance", "bitcoin", "ethereum", "deposito bank", "sukuk negara", "ori surat berharga", "p2p lending",
            "investasi", "saham", "crypto", "kripto", "reksadana"
        ),

        TransactionCategory.GAJI to listOf(
            "gaji pokok", "gaji bulanan", "paycheck", "salary", "upah kerja", "honorarium", "honor mengajar",
            "thr lebaran", "bonus tahunan", "bonus performa", "insentif penjualan", "komisi", "uang lembur", "gaji"
        ),

        TransactionCategory.TPP to listOf(
            "tpp bulanan", "tpp pns", "tpp asn", "tunjangan kinerja", "tukin pns", "tukin", "tpp"
        ),

        TransactionCategory.SERTIFIKASI to listOf(
            "tunjangan sertifikasi", "tpg guru", "tunjangan profesi", "dana sertifikasi", "serti", "sertifikasi"
        ),

        TransactionCategory.UANG_SAKU to listOf(
            "uang saku bulanan", "uang saku magang", "uang saku kkn", "dikasih ortu", "dikasih orang tua",
            "transferan ortu", "uang jajan", "pesangon", "amplop kondangan", "hadiah uang", "angpao", "uang saku", "saku"
        )
    )

    private val INCOME_SPECIFIC_KEYWORDS = listOf(
        "gaji", "salary", "paycheck", "upah", "honor", "thr", "bonus", "insentif", "komisi", "lembur",
        "tpp", "tukin", "tunjangan", "sertifikasi", "serti", "tpg", "uang saku", "saku", "pesangon",
        "pemasukan", "income", "transfer masuk", "terima transfer", "jual", "penjualan", "hasil jualan",
        "cuan", "omset", "omzet", "dividen", "cashback", "refund", "dikasih", "hadiah", "angpao", "klaim"
    )

    /**
     * Primary Parser: Converts raw user quick input (e.g. "Es teh manis 5rb") into a structured transaction.
     * Uses Adaptive History Memory + Comprehensive Everyday Indonesian Dictionary.
     */
    fun parse(input: String, history: List<TransactionEntity> = emptyList()): ParsedTransaction? {
        val trimmed = input.trim()
        if (trimmed.isEmpty()) return null

        // 1. Amount Extraction (handles: 5rb, 50k, 25.000, 100ribu, 1.5jt, 2.5juta, Rp 15000, 15000)
        // Ensure suffixes like 'k' don't match 'kg' or 'km'
        val amountRegex = Regex("""(?:rp\.?\s*|idr\s*)?(\d+[\d.,]*)\s*(k|rb|ribu|jt|juta)?(?![a-zA-Z])""", RegexOption.IGNORE_CASE)
        val matches = amountRegex.findAll(trimmed).filter { m ->
            val numStr = m.groupValues[1].replace(".", "").replace(",", ".")
            numStr.toDoubleOrNull() != null
        }.toList()
        if (matches.isEmpty()) return null

        // Pick the match most likely to be the price:
        // Prioritize numbers with explicit suffix (rb, k, jt, ribu) or currency prefix, taking the last occurrence (e.g. "2 porsi sate 40rb" -> 40rb)
        val targetMatch = matches.findLast { it.groupValues[2].isNotEmpty() || it.value.contains("rp", ignoreCase = true) || it.value.contains("idr", ignoreCase = true) }
            ?: matches.last()

        val rawNumStr = targetMatch.groupValues[1].replace(".", "").replace(",", ".")
        val num = rawNumStr.toDoubleOrNull() ?: return null
        val suffix = targetMatch.groupValues[2].lowercase(Locale.ROOT)

        val multiplier = when (suffix) {
            "k", "rb", "ribu" -> 1000.0
            "jt", "juta" -> 1000000.0
            else -> 1.0
        }

        val amount = num * multiplier
        if (amount <= 0) return null

        // 2. Title Candidate extraction
        var titleCandidate = trimmed.removeRange(targetMatch.range).trim()
            .replace(Regex("""^\s*[-+:=@]\s*|\s*[-+:=@]\s*$"""), "")
            .trim()

        if (titleCandidate.isEmpty()) {
            titleCandidate = "Pencatatan Cepat"
        }

        val normalizedInput = trimmed.lowercase(Locale.ROOT)
        val normalizedTitle = titleCandidate.lowercase(Locale.ROOT)

        // 3. Detect Transaction Type (Income vs Expense)
        val detectedType = detectType(normalizedInput)

        // 4. Adaptive History Memory Check (Step 1 of Categorization)
        var matchedHistory = false
        var detectedCategory: TransactionCategory? = findCategoryFromHistory(normalizedTitle, detectedType, history)

        if (detectedCategory != null) {
            matchedHistory = true
        } else {
            // 5. Dictionary Keyword Scoring (Step 2 of Categorization)
            detectedCategory = detectCategoryFromDictionary(normalizedInput, detectedType)
        }

        // Final fallback if nothing matched
        val finalCategory = detectedCategory ?: if (detectedType == TransactionType.INCOME) TransactionCategory.LAINNYA else TransactionCategory.LAINNYA

        // Clean & capitalize title nicely
        val cleanedTitle = formatTitle(titleCandidate)

        return ParsedTransaction(
            title = cleanedTitle,
            amount = amount,
            type = detectedType,
            category = finalCategory,
            matchedFromHistory = matchedHistory
        )
    }

    /**
     * Determines whether the transaction is an Income or Expense.
     */
    fun detectType(text: String): TransactionType {
        val lower = text.lowercase(Locale.ROOT)
        return if (INCOME_SPECIFIC_KEYWORDS.any { lower.contains(it) }) {
            TransactionType.INCOME
        } else {
            TransactionType.EXPENSE
        }
    }

    /**
     * Smart category detection for any text (can also be called from Add/Edit Transaction Form).
     */
    fun detectCategory(
        text: String,
        type: TransactionType = detectType(text),
        history: List<TransactionEntity> = emptyList()
    ): TransactionCategory {
        val lower = text.lowercase(Locale.ROOT).trim()
        if (lower.isEmpty()) {
            return if (type == TransactionType.INCOME) TransactionCategory.GAJI else TransactionCategory.MAKANAN
        }

        // 1. Check history first
        val fromHistory = findCategoryFromHistory(lower, type, history)
        if (fromHistory != null) return fromHistory

        // 2. Check dictionary
        val fromDict = detectCategoryFromDictionary(lower, type)
        if (fromDict != null) return fromDict

        return if (type == TransactionType.INCOME) TransactionCategory.GAJI else TransactionCategory.LAINNYA
    }

    /**
     * Adaptive Memory: Searches recent transaction history to find how the user previously categorized similar titles.
     */
    private fun findCategoryFromHistory(
        normalizedTitle: String,
        type: TransactionType,
        history: List<TransactionEntity>
    ): TransactionCategory? {
        if (history.isEmpty() || normalizedTitle.length < 2) return null

        val filteredHistory = history.filter { it.type == type }
        if (filteredHistory.isEmpty()) return null

        // Pass 1: Exact match with historical title (case-insensitive)
        val exactMatch = filteredHistory.firstOrNull {
            it.title.trim().equals(normalizedTitle, ignoreCase = true)
        }
        if (exactMatch != null) return exactMatch.category

        // Pass 2: History title contains input or input contains history title (length >= 3)
        val substringMatch = filteredHistory.firstOrNull {
            val hTitle = it.title.trim().lowercase(Locale.ROOT)
            (hTitle.length >= 3 && normalizedTitle.contains(hTitle)) ||
            (normalizedTitle.length >= 3 && hTitle.contains(normalizedTitle))
        }
        if (substringMatch != null) return substringMatch.category

        // Pass 3: Token overlap match (e.g. user typed "es teh manis solo", history has "es teh manis")
        val inputTokens = normalizedTitle.split(Regex("""\s+""")).filter { it.length > 2 }
        if (inputTokens.isNotEmpty()) {
            val tokenMatch = filteredHistory.maxByOrNull { hTx ->
                val hTokens = hTx.title.lowercase(Locale.ROOT).split(Regex("""\s+""")).filter { it.length > 2 }
                inputTokens.count { token -> hTokens.contains(token) }
            }
            if (tokenMatch != null) {
                val hTokens = tokenMatch.title.lowercase(Locale.ROOT).split(Regex("""\s+""")).filter { it.length > 2 }
                val overlapCount = inputTokens.count { token -> hTokens.contains(token) }
                if (overlapCount >= 2 || (inputTokens.size == 1 && overlapCount == 1)) {
                    return tokenMatch.category
                }
            }
        }

        return null
    }

    /**
     * Dictionary matching with scoring:
     * - Multi-word matches get higher weight (e.g. "es teh" gets higher score than "es")
     * - Exact word boundary matches get prioritized
     */
    private fun detectCategoryFromDictionary(
        text: String,
        type: TransactionType
    ): TransactionCategory? {
        val lowerText = text.lowercase(Locale.ROOT)
        var bestCategory: TransactionCategory? = null
        var highestScore = 0

        val categoriesToTest = if (type == TransactionType.INCOME) {
            listOf(
                TransactionCategory.GAJI,
                TransactionCategory.TPP,
                TransactionCategory.SERTIFIKASI,
                TransactionCategory.UANG_SAKU,
                TransactionCategory.INVESTASI
            )
        } else {
            listOf(
                TransactionCategory.MAKANAN,
                TransactionCategory.JAJAN,
                TransactionCategory.KEBUTUHAN_DAPUR,
                TransactionCategory.TRANSPORT,
                TransactionCategory.PERAWATAN_KENDARAAN,
                TransactionCategory.BELANJA,
                TransactionCategory.NAFKAH_KELUARGA,
                TransactionCategory.KESEHATAN,
                TransactionCategory.PENDIDIKAN,
                TransactionCategory.TAGIHAN,
                TransactionCategory.HIBURAN,
                TransactionCategory.INVESTASI
            )
        }

        for (cat in categoriesToTest) {
            val keywords = CATEGORY_KEYWORD_MAP[cat] ?: continue
            for (kw in keywords) {
                if (lowerText.contains(kw)) {
                    // Score calculation: length of matched phrase * 10 + bonus for word boundaries
                    val score = kw.length * 10 + (if (kw.contains(" ")) 15 else 0)
                    if (score > highestScore) {
                        highestScore = score
                        bestCategory = cat
                    }
                }
            }
        }

        return bestCategory
    }

    private fun formatTitle(raw: String): String {
        val words = raw.trim().split(Regex("""\s+"""))
        return words.joinToString(" ") { word ->
            if (word.equals("tpp", ignoreCase = true) ||
                word.equals("pln", ignoreCase = true) ||
                word.equals("pdam", ignoreCase = true) ||
                word.equals("bpjs", ignoreCase = true) ||
                word.equals("krl", ignoreCase = true) ||
                word.equals("mrt", ignoreCase = true) ||
                word.equals("lrt", ignoreCase = true) ||
                word.equals("bbm", ignoreCase = true) ||
                word.equals("spp", ignoreCase = true) ||
                word.equals("ukt", ignoreCase = true) ||
                word.equals("atk", ignoreCase = true) ||
                word.equals("rs", ignoreCase = true) ||
                word.equals("rsud", ignoreCase = true) ||
                word.equals("sim", ignoreCase = true) ||
                word.equals("stnk", ignoreCase = true)
            ) {
                word.uppercase(Locale.ROOT)
            } else {
                word.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.ROOT) else it.toString() }
            }
        }
    }
}
