package com.example.edgering.data.backup

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BackupValidatorTest {
    private fun codes(file: BackupFile) = BackupValidator.validate(file).map { it.code }

    private fun assertRejectedWith(expected: BackupIssueCode, file: BackupFile) {
        val found = codes(file)
        assertTrue("expected $expected in $found", expected in found)
    }

    @Test
    fun validFileHasNoIssues() = assertEquals(emptyList<BackupIssue>(), BackupValidator.validate(TestBackups.valid()))

    @Test
    fun emptyDataWithOneDefaultTriggerIsValid() {
        val file = BackupFile(triggers = listOf(BackupTrigger(1, "D", "RIGHT", 0.4f, 24, 0f, isDefault = true)))
        assertEquals(emptyList<BackupIssueCode>(), codes(file))
    }

    @Test
    fun wrongFormatMarkerIsRejected() =
        assertEquals(listOf(BackupIssueCode.WRONG_FORMAT), codes(TestBackups.valid().copy(format = "something-else")))

    @Test
    fun newerAndNonPositiveVersionsAreRejected() {
        assertEquals(listOf(BackupIssueCode.UNSUPPORTED_VERSION), codes(TestBackups.valid().copy(schemaVersion = BackupFile.CURRENT_VERSION + 1)))
        assertEquals(listOf(BackupIssueCode.UNSUPPORTED_VERSION), codes(TestBackups.valid().copy(schemaVersion = 0)))
    }

    @Test
    fun tooManyEntriesAreRejectedBeforeAnythingElse() {
        val many = (1L..201L).map { BackupZone(it, "z$it", 0, it.toInt()) }
        assertEquals(listOf(BackupIssueCode.TOO_MANY_ENTRIES), codes(TestBackups.valid().copy(zones = many)))
    }

    @Test
    fun duplicateAndNonPositiveIdsAreRejected() {
        val v = TestBackups.valid()
        assertRejectedWith(BackupIssueCode.DUPLICATE_ID, v.copy(zones = v.zones + v.zones.first()))
        assertRejectedWith(BackupIssueCode.DUPLICATE_ID, v.copy(items = v.items + v.items.first()))
        assertRejectedWith(BackupIssueCode.INVALID_ID, v.copy(zones = listOf(v.zones[0].copy(id = 0), v.zones[1])))
        assertRejectedWith(BackupIssueCode.INVALID_ID, v.copy(items = v.items.map { it.copy(id = -it.id) }))
    }

    @Test
    fun unknownEnumValuesAreRejected() {
        val v = TestBackups.valid()
        assertRejectedWith(BackupIssueCode.UNKNOWN_VALUE, v.copy(items = listOf(v.items[0].copy(type = "HOLOGRAM"))))
        assertRejectedWith(BackupIssueCode.UNKNOWN_VALUE, v.copy(triggers = listOf(v.triggers[0].copy(position = "CEILING"), v.triggers[1])))
    }

    @Test
    fun blankOrOversizedTextIsRejected() {
        val v = TestBackups.valid()
        assertRejectedWith(BackupIssueCode.BAD_TEXT, v.copy(zones = listOf(v.zones[0].copy(name = "  "), v.zones[1])))
        assertRejectedWith(BackupIssueCode.BAD_TEXT, v.copy(zones = listOf(v.zones[0].copy(name = "x".repeat(65)), v.zones[1])))
        assertRejectedWith(BackupIssueCode.BAD_TEXT, v.copy(items = listOf(v.items[0].copy(label = "x".repeat(129)))))
        assertRejectedWith(BackupIssueCode.BAD_TEXT, v.copy(items = listOf(v.items[0].copy(target = "x".repeat(4097)))))
        assertRejectedWith(BackupIssueCode.BAD_TEXT, v.copy(items = listOf(v.items[0].copy(options = "x".repeat(8193)))))
    }

    @Test
    fun negativePositionsAreRejected() {
        val v = TestBackups.valid()
        assertRejectedWith(BackupIssueCode.BAD_POSITION, v.copy(zones = listOf(v.zones[0].copy(position = -1), v.zones[1])))
        assertRejectedWith(BackupIssueCode.BAD_POSITION, v.copy(items = listOf(v.items[0].copy(position = -1))))
    }

    @Test
    fun itemsAndTriggersMustReferenceExistingZones() {
        val v = TestBackups.valid()
        assertRejectedWith(BackupIssueCode.MISSING_ZONE, v.copy(items = listOf(v.items[1].copy(zoneId = 99))))
        assertRejectedWith(BackupIssueCode.MISSING_ZONE, v.copy(triggers = listOf(v.triggers[0], v.triggers[1].copy(targetZoneId = 99))))
    }

    @Test
    fun badTriggerGeometryIsRejected() {
        val v = TestBackups.valid()
        assertRejectedWith(BackupIssueCode.BAD_TRIGGER_GEOMETRY, v.copy(triggers = listOf(v.triggers[0].copy(lengthFraction = 2f), v.triggers[1])))
        assertRejectedWith(BackupIssueCode.BAD_TRIGGER_GEOMETRY, v.copy(triggers = listOf(v.triggers[0].copy(offsetFraction = 0.9f), v.triggers[1])))
        assertRejectedWith(BackupIssueCode.BAD_TRIGGER_GEOMETRY, v.copy(triggers = listOf(v.triggers[0].copy(lengthFraction = Float.NaN), v.triggers[1])))
    }

    @Test
    fun exactlyOneDefaultTriggerIsRequired() {
        val v = TestBackups.valid()
        assertRejectedWith(BackupIssueCode.DEFAULT_TRIGGER_COUNT, v.copy(triggers = listOf(v.triggers[0].copy(isDefault = false), v.triggers[1])))
        assertRejectedWith(BackupIssueCode.DEFAULT_TRIGGER_COUNT, v.copy(triggers = listOf(v.triggers[0], v.triggers[1].copy(isDefault = true))))
        assertRejectedWith(BackupIssueCode.DEFAULT_TRIGGER_COUNT, v.copy(triggers = emptyList()))
    }

    @Test
    fun parentMustExistBeAFolderAndShareTheZone() {
        val v = TestBackups.valid()
        val mail = v.items[0]
        val folder = v.items[1]
        assertRejectedWith(BackupIssueCode.MISSING_PARENT, v.copy(items = listOf(mail.copy(parentItemId = 77), folder)))
        assertRejectedWith(BackupIssueCode.PARENT_NOT_FOLDER, v.copy(items = listOf(mail, folder.copy(type = "APP"))))
        assertRejectedWith(BackupIssueCode.PARENT_ZONE_MISMATCH, v.copy(items = listOf(mail.copy(zoneId = 2), folder)))
    }

    @Test
    fun parentCyclesAreRejected() {
        val v = TestBackups.valid()
        val a = BackupItem(10, 1, parentItemId = 11, position = 0, type = "FOLDER", label = "A")
        val b = BackupItem(11, 1, parentItemId = 10, position = 0, type = "FOLDER", label = "B")
        assertRejectedWith(BackupIssueCode.PARENT_CYCLE, v.copy(items = v.items + a + b))
        val self = BackupItem(12, 1, parentItemId = 12, position = 0, type = "FOLDER", label = "S")
        assertRejectedWith(BackupIssueCode.PARENT_CYCLE, v.copy(items = v.items + self))
    }

    @Test
    fun deepNestingWithoutACycleIsAccepted() {
        val v = TestBackups.valid()
        val chain = (10L..40L).map { id ->
            BackupItem(id, 1, parentItemId = if (id == 10L) null else id - 1, position = 0, type = "FOLDER", label = "F$id")
        }
        assertEquals(emptyList<BackupIssueCode>(), codes(v.copy(items = v.items + chain)))
    }

    @Test
    fun reportedIssuesAreCapped() {
        val v = TestBackups.valid()
        val bad = (1L..100L).map { BackupItem(it + 100, 99, null, 0, "APP", "x") }
        assertEquals(BackupValidator.MAX_REPORTED_ISSUES, BackupValidator.validate(v.copy(items = bad)).size)
    }

    @Test
    fun issueDetailsContainLocationsNeverUserText() {
        val v = TestBackups.valid()
        val issues = BackupValidator.validate(v.copy(items = listOf(v.items[0].copy(label = "SECRET-LABEL", zoneId = 99))))
        assertTrue(issues.isNotEmpty())
        assertTrue(issues.none { it.detail?.contains("SECRET") == true })
    }
}
