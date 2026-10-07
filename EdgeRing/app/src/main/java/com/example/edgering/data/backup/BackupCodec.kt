package com.example.edgering.data.backup

import com.example.edgering.data.model.Limits
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json

sealed interface DecodeResult {
    data class Ok(val file: BackupFile) : DecodeResult
    data class Failed(val issue: BackupIssue) : DecodeResult
}

/** JSON text <-> [BackupFile]. Decoding never throws; it reports a [BackupIssue] instead. */
object BackupCodec {
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    fun encode(file: BackupFile): String = json.encodeToString(BackupFile.serializer(), file)

    fun decode(text: String): DecodeResult {
        if (text.length > Limits.MAX_BACKUP_CHARS) {
            return DecodeResult.Failed(BackupIssue(BackupIssueCode.TOO_LARGE))
        }
        return try {
            DecodeResult.Ok(json.decodeFromString(BackupFile.serializer(), text))
        } catch (e: SerializationException) {
            DecodeResult.Failed(BackupIssue(BackupIssueCode.MALFORMED))
        } catch (e: IllegalArgumentException) {
            DecodeResult.Failed(BackupIssue(BackupIssueCode.MALFORMED))
        }
    }
}
