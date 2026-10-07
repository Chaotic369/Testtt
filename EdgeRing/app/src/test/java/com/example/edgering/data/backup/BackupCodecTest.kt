package com.example.edgering.data.backup

import com.example.edgering.data.model.Limits
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BackupCodecTest {
    private fun failedCode(text: String): BackupIssueCode =
        (BackupCodec.decode(text) as DecodeResult.Failed).issue.code

    @Test
    fun encodeThenDecodeGivesTheSameFile() {
        val original = TestBackups.valid()
        val decoded = BackupCodec.decode(BackupCodec.encode(original))
        assertEquals(DecodeResult.Ok(original), decoded)
    }

    @Test
    fun encodedTextCarriesTheFormatMarkerAndVersion() {
        val text = BackupCodec.encode(TestBackups.valid())
        assertTrue(text.contains("\"format\":\"edgering-backup\""))
        assertTrue(text.contains("\"schemaVersion\":1"))
    }

    @Test
    fun unknownKeysAreIgnored() {
        val text = """{"format":"edgering-backup","schemaVersion":1,"added_later":{"x":1}}"""
        assertEquals(DecodeResult.Ok(BackupFile()), BackupCodec.decode(text))
    }

    @Test
    fun garbageAndWrongShapesAreMalformedNotCrashes() {
        assertEquals(BackupIssueCode.MALFORMED, failedCode(""))
        assertEquals(BackupIssueCode.MALFORMED, failedCode("not json"))
        assertEquals(BackupIssueCode.MALFORMED, failedCode("[1,2,3]"))
        assertEquals(BackupIssueCode.MALFORMED, failedCode("""{"zones":5}"""))
        assertEquals(BackupIssueCode.MALFORMED, failedCode("""{"zones":[{"name":"missing id"}]}"""))
        assertEquals(BackupIssueCode.MALFORMED, failedCode("""{"schemaVersion":"one"}"""))
    }

    @Test
    fun truncatedFileIsMalformed() {
        val text = BackupCodec.encode(TestBackups.valid())
        assertEquals(BackupIssueCode.MALFORMED, failedCode(text.substring(0, text.length / 2)))
    }

    @Test
    fun oversizedInputIsRejectedWithoutParsing() {
        assertEquals(BackupIssueCode.TOO_LARGE, failedCode("x".repeat(Limits.MAX_BACKUP_CHARS + 1)))
    }

    @Test
    fun valuesThatDecodeButAreWrongAreCaughtByTheValidatorNotTheCodec() {
        val text = """{"format":"other","schemaVersion":1}"""
        val ok = BackupCodec.decode(text) as DecodeResult.Ok
        assertFalse(BackupValidator.validate(ok.file).isEmpty())
    }
}
