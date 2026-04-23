package com.vilacanaa.app

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
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
import java.text.Normalizer
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
        val nome = findViewById<EditText>(R.id.nome).text.toString().trim()
        val servico = findViewById<EditText>(R.id.servico).text.toString().trim()
        val endereco = findViewById<EditText>(R.id.endereco).text.toString().trim()

        val material = findViewById<EditText>(R.id.material).text.toString()
            .replace(",", ".")
            .toDoubleOrNull() ?: 0.0

        val mao = findViewById<EditText>(R.id.mao).text.toString()
            .replace(",", ".")
            .toDoubleOrNull() ?: 0.0

        val porcentagem = findViewById<EditText>(R.id.porcentagem).text.toString()
            .replace(",", ".")
            .toDoubleOrNull() ?: 0.0

        val prazo = findViewById<EditText>(R.id.prazo).text.toString().trim()
        val cor = findViewById<EditText>(R.id.cor).text.toString().trim()
        val info = findViewById<EditText>(R.id.info).text.toString().trim()

        val pagamento = findViewById<Spinner>(R.id.pagamento).selectedItem.toString()

        var total = material + mao
        var desconto = 0.0

        if (pagamento.contains("vista", ignoreCase = true) && porcentagem > 0.0) {
            desconto = total * (porcentagem / 100.0)
            total -= desconto
        }

        resultadoFinal = """
Cliente: ${if (nome.isNotEmpty()) nome else "-"}
Serviço: ${if (servico.isNotEmpty()) servico else "-"}
Endereço: ${if (endereco.isNotEmpty()) endereco else "-"}

Material: R$ ${"%.2f".format(Locale("pt", "BR"), material)}
Mão de obra: R$ ${"%.2f".format(Locale("pt", "BR"), mao)}
Desconto: R$ ${"%.2f".format(Locale("pt", "BR"), desconto)}
Total final: R$ ${"%.2f".format(Locale("pt", "BR"), total)}

Pagamento: $pagamento
Prazo: ${if (prazo.isNotEmpty()) prazo else "-"}
Cor: ${if (cor.isNotEmpty()) cor else "-"}

Observações:
${if (info.isNotEmpty()) info else "-"}
        """.trimIndent()

        findViewById<TextView>(R.id.resultado).text = resultadoFinal
    }

    fun gerarPdf(view: View) {
        if (resultadoFinal.isEmpty()) {
            Toast.makeText(this, "Gere o orçamento primeiro", Toast.LENGTH_SHORT).show()
            return
        }

        try {
            val pdfDocument = PdfDocument()
            val pageWidth = 595
            val pageHeight = 842

            val pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, 1).create()
            val page = pdfDocument.startPage(pageInfo)
            val canvas = page.canvas

            canvas.drawColor(Color.WHITE)

            val paint = Paint(Paint.ANTI_ALIAS_FLAG)
            val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG)
            val subTitlePaint = Paint(Paint.ANTI_ALIAS_FLAG)
            val sectionPaint = Paint(Paint.ANTI_ALIAS_FLAG)
            val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG)
            val valuePaint = Paint(Paint.ANTI_ALIAS_FLAG)
            val contactPaint = Paint(Paint.ANTI_ALIAS_FLAG)
            val linePaint = Paint(Paint.ANTI_ALIAS_FLAG)
            val boxPaint = Paint(Paint.ANTI_ALIAS_FLAG)
            val totalBoxPaint = Paint(Paint.ANTI_ALIAS_FLAG)
            val totalPaint = Paint(Paint.ANTI_ALIAS_FLAG)

            val azulEscuro = Color.parseColor("#0F172A")
            val azul = Color.parseColor("#1D4ED8")
            val azulClaro = Color.parseColor("#E0ECFF")
            val cinzaClaro = Color.parseColor("#E2E8F0")
            val verde = Color.parseColor("#15803D")
            val verdeClaro = Color.parseColor("#DCFCE7")

            // HEADER
            paint.color = azulEscuro
            canvas.drawRect(0f, 0f, pageWidth.toFloat(), 110f, paint)

            try {
                val logo = BitmapFactory.decodeResource(resources, R.drawable.logo_vila_canaa)
                val scaledLogo = Bitmap.createScaledBitmap(logo, 80, 80, true)
                canvas.drawBitmap(scaledLogo, 25f, 15f, null)
            } catch (_: Exception) {}

            titlePaint.color = Color.WHITE
            titlePaint.textSize = 18f
            titlePaint.isFakeBoldText = true
            canvas.drawText("METALÚRGICA VILA CANAÃ", 120f, 42f, titlePaint)

            subTitlePaint.color = Color.parseColor("#CBD5E1")
            subTitlePaint.textSize = 12f
            canvas.drawText("Orçamento de serviços", 120f, 62f, subTitlePaint)

            contactPaint.color = Color.parseColor("#93C5FD")
            contactPaint.textSize = 11f
            canvas.drawText("WhatsApp: (74) 8144-8399", 120f, 82f, contactPaint)
            canvas.drawText("Instagram: @metalurgica._vilacanaa", 120f, 97f, contactPaint)

            // CONTEÚDO
            val linhas = resultadoFinal.split("\n")
            var y = 220f

            sectionPaint.color = azul
            sectionPaint.textSize = 15f
            sectionPaint.isFakeBoldText = true
            canvas.drawText("DADOS DO ORÇAMENTO", 35f, y, sectionPaint)

            y += 18f
            canvas.drawLine(35f, y, 560f, y, linePaint)
            y += 24f

            valuePaint.color = azulEscuro
            valuePaint.textSize = 14f

            for (linha in linhas) {
                if (linha.startsWith("Total final:")) continue

                val quebradas = quebrarLinha(linha, 64)
                for (parte in quebradas) {
                    canvas.drawText(parte, 35f, y, valuePaint)
                    y += 22f
                }
            }

            // TOTAL
            val totalLinha = linhas.find { it.startsWith("Total final:") } ?: ""

            y += 10f
            totalBoxPaint.color = verdeClaro
            val totalRect = RectF(30f, y - 24f, 565f, y + 24f)
            canvas.drawRoundRect(totalRect, 16f, 16f, totalBoxPaint)

            totalPaint.color = verde
            totalPaint.textSize = 18f
            totalPaint.isFakeBoldText = true
            canvas.drawText(totalLinha, 48f, y + 7f, totalPaint)

            pdfDocument.finishPage(page)

            val file = File(getExternalFilesDir(null), "orcamento_${gerarNomeSeguro()}.pdf")
            arquivoPdf = file

            pdfDocument.writeTo(FileOutputStream(file))
            pdfDocument.close()

            Toast.makeText(this, "PDF gerado!", Toast.LENGTH_SHORT).show()

        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "Erro ao gerar PDF", Toast.LENGTH_LONG).show()
        }
    }

    fun compartilhar(view: View) {
        if (arquivoPdf == null || !arquivoPdf!!.exists()) {
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

    private fun quebrarLinha(texto: String, max: Int): List<String> {
        if (texto.length <= max) return listOf(texto)
        val palavras = texto.split(" ")
        val linhas = mutableListOf<String>()
        var atual = ""

        for (p in palavras) {
            if ((atual + " " + p).length <= max) {
                atual = if (atual.isEmpty()) p else "$atual $p"
            } else {
                linhas.add(atual)
                atual = p
            }
        }
        if (atual.isNotEmpty()) linhas.add(atual)
        return linhas
    }

    private fun gerarNomeSeguro(): String {
        val data = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        return Normalizer.normalize(data, Normalizer.Form.NFD)
            .replace("[^a-zA-Z0-9_-]".toRegex(), "")
    }
}