package com.example

import com.example.data.model.TransactionCategory
import com.example.data.model.TransactionEntity
import com.example.data.model.TransactionType
import com.example.util.QuickTextParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class QuickTextParserTest {

    @Test
    fun testEsTehManisDetection() {
        val result = QuickTextParser.parse("Es teh manis 5rb")
        assertNotNull(result)
        assertEquals(5000.0, result!!.amount, 0.01)
        assertEquals(TransactionType.EXPENSE, result.type)
        assertEquals(TransactionCategory.MAKANAN, result.category)
        assertEquals("Es Teh Manis", result.title)
    }

    @Test
    fun testNasiPadang() {
        val res = QuickTextParser.parse("Nasi padang 25.000")
        assertNotNull(res)
        assertEquals(25000.0, res!!.amount, 0.01)
        assertEquals(TransactionCategory.MAKANAN, res.category)
    }

    @Test
    fun testKopiSusu() {
        val res = QuickTextParser.parse("Kopi susu 18k")
        assertNotNull(res)
        assertEquals(18000.0, res!!.amount, 0.01)
        assertEquals(TransactionCategory.MAKANAN, res.category)
    }

    @Test
    fun testMartabak() {
        val res = QuickTextParser.parse("Beli martabak 35k")
        assertNotNull(res)
        assertEquals(35000.0, res!!.amount, 0.01)
        assertEquals(TransactionCategory.JAJAN, res.category)
    }

    @Test
    fun testBensin() {
        val res = QuickTextParser.parse("Bensin 30k")
        assertNotNull(res)
        assertEquals(30000.0, res!!.amount, 0.01)
        assertEquals(TransactionCategory.TRANSPORT, res.category)
    }

    @Test
    fun testGantiOli() {
        val res = QuickTextParser.parse("Ganti oli 65rb")
        assertNotNull(res)
        assertEquals(65000.0, res!!.amount, 0.01)
        assertEquals(TransactionCategory.PERAWATAN_KENDARAAN, res.category)
    }

    @Test
    fun testTokenListrik() {
        val res = QuickTextParser.parse("Token listrik 100k")
        assertNotNull(res)
        assertEquals(100000.0, res!!.amount, 0.01)
        assertEquals(TransactionCategory.TAGIHAN, res.category)
    }

    @Test
    fun testBeras() {
        val res = QuickTextParser.parse("Beli beras 5kg 75rb")
        assertNotNull(res)
        assertEquals(75000.0, res!!.amount, 0.01)
        assertEquals(TransactionCategory.KEBUTUHAN_DAPUR, res.category)
    }

    @Test
    fun testGaji() {
        val res = QuickTextParser.parse("Gaji bulanan 6jt")
        assertNotNull(res)
        assertEquals(6000000.0, res!!.amount, 0.01)
        assertEquals(TransactionType.INCOME, res.type)
        assertEquals(TransactionCategory.GAJI, res.category)
    }

    @Test
    fun testAdaptiveHistoryMemory() {
        val history = listOf(
            TransactionEntity(
                id = 1,
                title = "Es Cincau Hijau",
                amount = 8000.0,
                type = TransactionType.EXPENSE,
                category = TransactionCategory.MAKANAN
            ),
            TransactionEntity(
                id = 2,
                title = "Voucher MLBB",
                amount = 50000.0,
                type = TransactionType.EXPENSE,
                category = TransactionCategory.HIBURAN
            )
        )

        val result = QuickTextParser.parse("Es cincau hijau 10rb", history)
        assertNotNull(result)
        assertEquals(TransactionCategory.MAKANAN, result!!.category)
        assertTrue(result.matchedFromHistory)
    }
}
