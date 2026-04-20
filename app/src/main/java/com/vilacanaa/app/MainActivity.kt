package com.vilacanaa.app

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : AppCompatActivity() {

    private var resultadoFinal: String = ""
    private var arquivoPdf: File? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val spinner = findViewById<Spinner>(R.id.pagamento)
        val opcoes = arrayOf("À vista", "A prazo")
        spinner.adapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_dropdown_item,
            opcoes
        )
    }

    fun gerarOrcamento(view: View) {
        val nome = findViewById<EditText>(R.id.nome).text.toString()
        val servico = findViewById<EditText>(R.id.servico).text.toString()
        val endereco = findViewById<EditText>(R.id.endereco).text.toString()

        val material = findViewById<EditText>(R.id.material).text.toString().toDoubleOrNull() ?: 0.0
        val mao = findViewById<EditText>(R.id.mao).text.toString().toDoubleOrNull() ?: 0.0
        val porcentagem = findViewById<EditText>(R.id.porcentagem).text.toString().toDoubleOrNull() ?: 0.0

        val prazo = findViewById<EditText>(R.id.prazo).text.toString()
        val cor = findViewById<EditText>(R.id.cor).text.toString()
        val info = findViewById<EditText>(R.id.info).text.toString()

        val pagamento = findViewById<Spinner>(R.id.pagamento).selectedItem.toString()

        var total = material + mao
        var desconto = 0.0

        if (pagamento.contains("vista", ignoreCase = true) && porcentagem > 0.0) {
            desconto = total * (porcentagem / 100.0)
            total -= desconto
        }

        resultadoFinal = """
Cliente: $nome
Serviço: $servico
Endereço: $endereco

Material: R$ ${"%.2f".format(material)}
Mão de obra: R$ ${"%.2f".format(mao)}
Desconto: R$ ${"%.2f".format(desconto)}
Total final: R$ ${"%.2f".format(total)}

Pagamento: $pagamento
Prazo: $prazo
Cor: $cor

Observações:
$info
        """.trimIndent()

        findViewById<TextView>(R.id.resultado).text = resultadoFinal
    }

    fun gerarPdf(view: View) {
        if (resultadoFinal.isEmpty()) {
            Toast.makeText(this, "Gere o orçamento primeiro", Toast.LENGTH_SHORT).show()
            return
        }

        val pdfDocument = PdfDocument()
        val paint = Paint()
        val titlePaint = Paint()
        val footerPaint = Paint()
        val linePaint = Paint()

        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create()
        val page = pdfDocument.startPage(pageInfo)
        val canvas = page.canvas

        canvas.drawColor(Color.WHITE)

        val originalLogo = BitmapFactory.decodeResource(resources, R.drawable.logo)

        val zoomFactor = 1.12f
        val cropWidth = (originalLogo.width / zoomFactor).toInt()
        val cropHeight = (originalLogo.height / zoomFactor).toInt()
        val cropX = (originalLogo.width - cropWidth) / 2
        val cropY = (originalLogo.height - cropHeight) / 2

        val croppedLogo = Bitmap.createBitmap(
            originalLogo,
            cropX,
            cropY,
            cropWidth,
            cropHeight
        )

        val scaledLogo = Bitmap.createScaledBitmap(
            croppedLogo,
            100,
            100,
            true
        )

        canvas.drawBitmap(scaledLogo, 40f, 20f, paint)

        titlePaint.textSize = 20f
        titlePaint.isFakeBoldText = true
        titlePaint.color = Color.BLACK

        canvas.drawText("METALÚRGICA VILA CANAÃ", 160f, 55f, titlePaint)

        paint.textSize = 14f
        paint.color = Color.DKGRAY
        canvas.drawText("Orçamento de serviços", 160f, 82f, paint)

        linePaint.color = Color.LTGRAY
        linePaint.strokeWidth = 2f
        canvas.drawLine(40f, 130f, 555f, 130f, linePaint)

        paint.textSize = 16f
        paint.color = Color.BLACK

        var y = 170
        resultadoFinal.split("\n").forEach { linha ->
            canvas.drawText(linha, 40f, y.toFloat(), paint)
            y += 25
        }

        footerPaint.textSize = 12f
        footerPaint.color = Color.DKGRAY

        val data = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date())

        canvas.drawLine(40f, 780f, 555f, 780f, linePaint)
        canvas.drawText("Gerado em: $data", 40f, 802f, footerPaint)
        canvas.drawText("WhatsApp: (74) 8144-8399", 40f, 820f, footerPaint)
        canvas.drawText("Instagram: @metalurgica._vilacanaa", 300f, 820f, footerPaint)

        pdfDocument.finishPage(page)

        val file = File(
            getExternalFilesDir(null),
            "orcamento_${System.currentTimeMillis()}.pdf"
        )

        arquivoPdf = file

        pdfDocument.writeTo(FileOutputStream(file))
        pdfDocument.close()

        Toast.makeText(this, "PDF gerado!", Toast.LENGTH_SHORT).show()
    }

    fun compartilhar(view: View) {
        if (arquivoPdf == null) {
            Toast.makeText(this, "Gere o PDF primeiro", Toast.LENGTH_SHORT).show()
            return
        }

        val uri: Uri = FileProvider.getUriForFile(
            this,
            applicationContext.packageName + ".provider",
            arquivoPdf!!
        )

        val intent = Intent(Intent.ACTION_SEND)
        intent.type = "application/pdf"
        intent.putExtra(Intent.EXTRA_STREAM, uri)
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)

        startActivity(Intent.createChooser(intent, "Compartilhar PDF"))
    }
}