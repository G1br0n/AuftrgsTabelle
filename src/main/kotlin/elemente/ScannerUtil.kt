package elemente

import androidx.compose.runtime.rememberCoroutineScope
import java.io.File
import java.util.concurrent.TimeUnit
import javax.imageio.ImageIO
import org.apache.pdfbox.pdmodel.PDDocument
import org.apache.pdfbox.pdmodel.PDPage
import org.apache.pdfbox.pdmodel.PDPageContentStream
import org.apache.pdfbox.pdmodel.common.PDRectangle
import org.apache.pdfbox.pdmodel.graphics.image.LosslessFactory

/**
 * Hilfsobjekt zum Scannen und PDF-Erzeugen von Stundenzetteln.
 */
object ScannerUtil {
    /**
     * Startet einen Scan via CLI-Tool (z. B. scanimage) und speichert als PNG.
     * @return true, wenn Scan erfolgreich war.
     */
    fun scanToPng(outputFile: File, timeoutMin: Long = 2): Boolean {
        val cmd = listOf(
            "scanimage",
            "--format=png",
            "--output-file=${outputFile.absolutePath}"
        )
        val process = ProcessBuilder(cmd)
            .redirectErrorStream(true)
            .inheritIO()
            .start()
        return process.waitFor(timeoutMin, TimeUnit.MINUTES) && process.exitValue() == 0
    }

    /**
     * Wandelt eine PNG-Datei in ein PDF um.
     */
    fun pngToPdf(png: File, pdf: File) {
        PDDocument().use { doc ->
            val img = ImageIO.read(png)
            val page = PDPage(PDRectangle(img.width.toFloat(), img.height.toFloat()))
            doc.addPage(page)
            val pdImg = LosslessFactory.createFromImage(doc, img)
            PDPageContentStream(doc, page).use { cs ->
                cs.drawImage(pdImg, 0f, 0f)
            }
            doc.save(pdf)
        }
    }
}

// In eurer UI-Datei (z.B. AufträgeView.kt) innerhalb des Dialog-Blocks:

