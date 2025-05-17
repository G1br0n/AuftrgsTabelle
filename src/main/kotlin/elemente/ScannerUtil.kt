package elemente

import java.io.BufferedReader
import java.io.File
import java.io.IOException
import java.io.InputStreamReader
import java.util.concurrent.TimeUnit
import javax.imageio.ImageIO
import org.apache.pdfbox.pdmodel.PDDocument
import org.apache.pdfbox.pdmodel.PDPage
import org.apache.pdfbox.pdmodel.PDPageContentStream
import org.apache.pdfbox.pdmodel.common.PDRectangle
import org.apache.pdfbox.pdmodel.graphics.image.LosslessFactory

object ScannerUtil {
    /**
     * Ermittelt das SANE-Gerät:
     * 1) Umgebungsvariable SCAN_DEVICE_OVERRIDE
     * 2) scanimage -L
     * 3) sane-find-scanner -q
     */
    @Throws(IOException::class)
    private fun detectScannerDevice(): String {
        // 1) Override
        System.getenv("SCAN_DEVICE_OVERRIDE")?.takeIf { it.isNotBlank() }?.let {
            return it
        }

        // 2) scanimage -L
        val scanimageOut = execCommand("scanimage", "-L")
        for (line in scanimageOut) {
            if (line.startsWith("device")) {
                return line
                    .substringAfter("device `")
                    .substringBefore("`")
            }
        }

        // 3) sane-find-scanner -q
        val findOut = execCommand("sane-find-scanner", "-q")
        for (line in findOut) {
            // Beispiel: "found USB scanner (vendor=0x04f9, product=0x0276) at libusb:001:007"
            if ("at libusb:" in line) {
                return line.substringAfter("at ").trim()
            }
        }

        // Kein Gerät gefunden – Fehler mit Diagnose
        val errMsg = buildString {
            appendLine("Kein SANE-Gerät gefunden!")
            appendLine("--- scanimage -L output:")
            scanimageOut.forEach { appendLine(it) }
            appendLine("--- sane-find-scanner -q output:")
            findOut.forEach { appendLine(it) }
            appendLine("Falls Du einen Brother WLAN-Scanner nutzt, setze bitte:")
            appendLine("  export SCAN_DEVICE_OVERRIDE=\"brsaneconfig4 --device-list\"")
        }
        throw IOException(errMsg)
    }

    /**
     * Scan via scanimage auf das ermittelte Gerät und speichert als PNG.
     */
    @Throws(IOException::class)
    fun scanToPng(outputFile: File, timeoutMin: Long = 2): Boolean {
        val device = detectScannerDevice()
        val cmd = listOf(
            "scanimage",
            "-d", device,
            "--format=png",
            "--output-file=${outputFile.absolutePath}"
        )
        val process = ProcessBuilder(cmd)
            .redirectErrorStream(true)
            .start()
        val finished = process.waitFor(timeoutMin, TimeUnit.MINUTES)
        val log = process.inputStream.bufferedReader().use { it.readText() }
        if (!finished || process.exitValue() != 0) {
            throw IOException("Scan-Befehl fehlgeschlagen (exit=${process.exitValue()}).\nLog:\n$log")
        }
        return true
    }

    /**
     * Wandelt eine PNG-Datei in ein PDF um.
     */
    @Throws(IOException::class)
    fun pngToPdf(png: File, pdf: File) {
        PDDocument().use { doc ->
            val img = ImageIO.read(png)
                ?: throw IOException("Fehler beim Lesen des PNG: ${png.absolutePath}")
            val page = PDPage(PDRectangle(img.width.toFloat(), img.height.toFloat()))
            doc.addPage(page)
            val pdImg = LosslessFactory.createFromImage(doc, img)
            PDPageContentStream(doc, page).use { cs ->
                cs.drawImage(pdImg, 0f, 0f)
            }
            doc.save(pdf)
        }
    }

    /** Führt externen Befehl aus und liefert alle Ausgaben als Liste zurück */
    private fun execCommand(vararg cmd: String): List<String> {
        val proc = ProcessBuilder(*cmd).redirectErrorStream(true).start()
        val out = mutableListOf<String>()
        BufferedReader(InputStreamReader(proc.inputStream)).use { r ->
            r.lineSequence().forEach { out += it }
        }
        proc.waitFor(10, TimeUnit.SECONDS)
        return out
    }
}
