package com.siaa.core.data

import android.content.Context
import org.json.JSONObject
import java.io.File
import java.io.InputStream
import java.security.KeyFactory
import java.security.MessageDigest
import java.security.Signature
import java.security.spec.X509EncodedKeySpec
import java.util.Base64
import java.util.zip.ZipInputStream

/**
 * Installs signed content packs without replacing the APK. Packs are staged,
 * cryptographically verified and atomically swapped into app-private storage.
 */
class ContentPackManager(private val context: Context) {
    private val root get() = File(context.filesDir, ACTIVE_DIR)

    data class InstallResult(val id: String, val version: String, val fileCount: Int)

    fun hasActivePack(): Boolean = File(root, "manifest.json").isFile
    fun activeRoot(): File? = root.takeIf { hasActivePack() }

    fun install(input: InputStream): InstallResult {
        val staging = File(context.cacheDir, "siaa-content-staging-${System.nanoTime()}")
        staging.deleteRecursively(); staging.mkdirs()
        var count = 0
        var totalBytes = 0L
        ZipInputStream(input.buffered()).use { zis ->
            var e = zis.nextEntry
            while (e != null) {
                val safe = e.name.replace('\\','/').trimStart('/')
                require(!safe.contains("..")) { "Ruta insegura en content pack" }
                if (!e.isDirectory && safe.isNotBlank()) {
                    require(++count <= MAX_FILES) { "Content pack con demasiados archivos" }
                    val out = File(staging, safe)
                    require(out.canonicalPath.startsWith(staging.canonicalPath + File.separator)) { "Ruta fuera del staging" }
                    out.parentFile?.mkdirs()
                    out.outputStream().use { output ->
                        val buffer = ByteArray(64 * 1024)
                        var fileBytes = 0L
                        while (true) {
                            val n = zis.read(buffer)
                            if (n <= 0) break
                            fileBytes += n
                            totalBytes += n
                            require(fileBytes <= MAX_FILE_BYTES && totalBytes <= MAX_TOTAL_BYTES) { "Content pack excede límites de tamaño" }
                            output.write(buffer, 0, n)
                        }
                    }
                }
                zis.closeEntry(); e = zis.nextEntry
            }
        }
        try {
            val manifestFile = File(staging, "manifest.json")
            require(manifestFile.isFile) { "El pack no contiene manifest.json" }
            val manifest = JSONObject(manifestFile.readText())
            require(manifest.optString("schemaVersion") == ContentSeeder.SUPPORTED_SCHEMA_VERSION) { "schemaVersion no compatible" }
            val checksums = manifest.getJSONObject("checksums")
            REQUIRED.forEach { name ->
                val f = File(staging, name)
                require(f.isFile) { "Falta $name" }
                val expected = checksums.optString(name)
                require(expected.isNotBlank() && sha256(f).equals(expected, true)) { "Checksum inválido para $name" }
            }
            val packChecksums = manifest.optJSONObject("packChecksums")
            packChecksums?.keys()?.forEach { name ->
                require(isSafeRelativePath(name)) { "Ruta insegura en packChecksums: $name" }
                val f = File(staging, name)
                require(f.isFile) { "Falta archivo firmado $name" }
                val expected = packChecksums.getString(name)
                require(expected.isNotBlank() && sha256(f).equals(expected, true)) { "Checksum inválido para $name" }
            }
            verifySignature(manifest)
            val old = File(context.filesDir, "$ACTIVE_DIR.old")
            old.deleteRecursively()
            if (root.exists() && !root.renameTo(old)) error("No se pudo preparar rollback del pack activo")
            if (!staging.renameTo(root)) {
                old.renameTo(root)
                error("No se pudo activar el pack")
            }
            old.deleteRecursively()
            return InstallResult(manifest.optString("id"), manifest.optString("version"), count)
        } catch (t: Throwable) {
            staging.deleteRecursively(); throw t
        }
    }

    fun rollbackToBundled() { root.deleteRecursively() }

    private fun verifySignature(manifest: JSONObject) {
        val sig = manifest.optJSONObject("signature") ?: error("Pack sin firma")
        require(sig.optString("algorithm") == "SHA256withECDSA") { "Algoritmo de firma no compatible" }
        require(sig.optString("keyId") == KEY_ID) { "Clave de firma no reconocida" }
        val publicKeyBytes = Base64.getDecoder().decode(PUBLIC_KEY_DER_BASE64)
        val publicKey = KeyFactory.getInstance("EC").generatePublic(X509EncodedKeySpec(publicKeyBytes))
        val verifier = Signature.getInstance("SHA256withECDSA")
        verifier.initVerify(publicKey)
        verifier.update(canonicalPayload(manifest).toByteArray(Charsets.UTF_8))
        require(verifier.verify(Base64.getDecoder().decode(sig.getString("value")))) { "Firma del content pack inválida" }
    }

    companion object {
        const val ACTIVE_DIR = "siaa-content-active"
        private const val KEY_ID = "siaa-content-v24-dev-p256"
        private const val MAX_FILES = 64
        private const val MAX_FILE_BYTES = 32L * 1024L * 1024L
        private const val MAX_TOTAL_BYTES = 96L * 1024L * 1024L
        private const val PUBLIC_KEY_DER_BASE64 = "MFkwEwYHKoZIzj0CAQYIKoZIzj0DAQcDQgAEJpab1wOPtT+JY535B9/Cz9CKSXsGe7a32ZrWUt6zQJ0HazAJSKo2kaeYI/RQkq44Pbw81v4idQQ7S5fqHAbdPQ=="
        private val REQUIRED = listOf("kcs.json","edges.json","exercises.json","lexemes.json")

        fun canonicalPayload(manifest: JSONObject): String {
            val c = manifest.getJSONObject("checksums")
            val parts = mutableListOf(
                manifest.optString("id"), manifest.optString("version"), manifest.optString("schemaVersion")
            )
            parts += REQUIRED.map { "$it=${c.optString(it)}" }
            manifest.optJSONObject("packChecksums")?.let { extra ->
                val names = mutableListOf<String>()
                extra.keys().forEach { names += it }
                parts += names.sorted().map { "$it=${extra.optString(it)}" }
            }
            return parts.joinToString("|")
        }

        private fun isSafeRelativePath(name: String): Boolean {
            val normalized = name.replace('\\', '/').trimStart('/')
            return normalized.isNotBlank() && !normalized.contains("..") && !normalized.startsWith("/")
        }

        private fun sha256(file: File): String {
            val md=MessageDigest.getInstance("SHA-256")
            file.inputStream().use { input -> val buf=ByteArray(64*1024); while(true){ val n=input.read(buf); if(n<=0) break; md.update(buf,0,n) } }
            return md.digest().joinToString("") { "%02x".format(it) }
        }
    }
}
