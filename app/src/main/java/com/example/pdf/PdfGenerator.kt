package com.example.pdf

import android.content.Context
import android.graphics.*
import android.graphics.pdf.PdfDocument
import android.os.Environment
import android.util.Base64
import android.util.Log
import com.example.data.model.Vehicle
import com.example.data.model.VehicleInspection
import java.io.File
import java.io.FileOutputStream

object PdfGenerator {
    private const val TAG = "PdfGenerator"

    /**
     * Decode a base64 signature string into an Android Bitmap
     */
    private fun base64ToBitmap(base64Str: String?): Bitmap? {
        if (base64Str == null || base64Str.isEmpty()) return null
        return try {
            val cleanString = if (base64Str.contains(",")) {
                base64Str.substring(base64Str.indexOf(",") + 1)
            } else {
                base64Str
            }
            val decodedBytes = Base64.decode(cleanString, Base64.DEFAULT)
            BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size)
        } catch (e: Exception) {
            Log.e(TAG, "Error decoding signature bitmap", e)
            null
        }
    }

    /**
     * Generates a compact, summary-oriented PDF and saves it to external storage cache.
     * Returns the output File, or null if generation fails.
     */
    fun generateCompactInspectionPdf(
        context: Context,
        inspection: VehicleInspection,
        vehicle: Vehicle
    ): File? {
        try {
            val pdfDocument = PdfDocument()
            
            // Standard A4 dimensions: 595 x 842 points
            val width = 595
            val height = 842
            val pageInfo = PdfDocument.PageInfo.Builder(width, height, 1).create()
            val page = pdfDocument.startPage(pageInfo)
            val canvas = page.canvas

            // Paint configurations
            val textPaint = Paint().apply {
                isAntiAlias = true
                color = Color.BLACK
                textSize = 10f
            }

            val titlePaint = Paint().apply {
                isAntiAlias = true
                color = Color.parseColor("#0066CC") // Segovia Blue Primary
                textSize = 15f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            }

            val subtitlePaint = Paint().apply {
                isAntiAlias = true
                color = Color.parseColor("#00A86B") // Green Secondary
                textSize = 8f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            }

            val sectionPaint = Paint().apply {
                isAntiAlias = true
                color = Color.parseColor("#0066CC")
                textSize = 11f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            }

            val accentBgPaint = Paint().apply {
                color = Color.parseColor("#E6F2FF") // Elegant light blue background for tables/boxes
                style = Paint.Style.FILL
            }

            val greenBgPaint = Paint().apply {
                color = Color.parseColor("#E6F7F0") // Elegant light green background
                style = Paint.Style.FILL
            }

            val borderPaint = Paint().apply {
                color = Color.parseColor("#CBD5E1") // Slate 300
                style = Paint.Style.STROKE
                strokeWidth = 1f
            }

            var currentY = 25f

            // 1. Draw Styled Header (without background box)
            val textOffsetLeft = 20f

            // Segovia Aseo Logo text and tagline: only display "Inspección de Vehículos" as the main header title
            canvas.drawText("SEGOVIA ASEO S.A. E.S.P.", textOffsetLeft, currentY + 22f, titlePaint)
            
            canvas.drawText("Inspección de Vehículos", textOffsetLeft, currentY + 40f, Paint().apply {
                isAntiAlias = true
                color = Color.parseColor("#475569") // Slate 600
                textSize = 10f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            })

            canvas.drawText("¡Siempre limpia para ti!", textOffsetLeft, currentY + 52f, Paint().apply {
                isAntiAlias = true
                color = Color.parseColor("#64748B") // Slate 500
                textSize = 7.5f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.ITALIC)
            })
            
            // Date block on top-right in header
            val rightTextPaint = Paint().apply {
                isAntiAlias = true
                color = Color.parseColor("#1E293B")
                textSize = 8.5f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            }
            canvas.drawText("REGISTRO: #${inspection.id.takeLast(6)}", 430f, currentY + 22f, rightTextPaint)
            canvas.drawText("FECHA: ${inspection.date}", 430f, currentY + 38f, rightTextPaint)

            currentY += 75f

            // 2. Metadata Columns (Side-by-side Cards: Vehicle Info & Inspection Info)
            val halfWidth = (width - 40f) / 2f
            
            // Left Card: Vehicle Info
            canvas.drawRoundRect(20f, currentY, 20f + halfWidth, currentY + 70f, 6f, 6f, accentBgPaint)
            canvas.drawRoundRect(20f, currentY, 20f + halfWidth, currentY + 70f, 6f, 6f, borderPaint)
            
            val vehTitlePaint = Paint().apply {
                isAntiAlias = true
                color = Color.parseColor("#0066CC")
                textSize = 90f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                textSize = 9.5f
            }
            canvas.drawText("INFORMACIÓN DEL VEHÍCULO", 30f, currentY + 16f, vehTitlePaint)
            
            val infoValPaint = Paint().apply {
                isAntiAlias = true
                color = Color.parseColor("#1E293B")
                textSize = 8.5f
            }
            canvas.drawText("Vehículo: ${vehicle.name}", 30f, currentY + 32f, infoValPaint)
            canvas.drawText("Placa: ${vehicle.licensePlate}", 30f, currentY + 45f, infoValPaint)
            canvas.drawText("Tipo: ${vehicle.type}", 30f, currentY + 58f, infoValPaint)

            // Right Card: Inspection Metadata
            val rightCardX = 20f + halfWidth + 10f
            canvas.drawRoundRect(rightCardX, currentY, 575f, currentY + 70f, 6f, 6f, greenBgPaint)
            canvas.drawRoundRect(rightCardX, currentY, 575f, currentY + 70f, 6f, 6f, borderPaint)
            
            val inspTitlePaint = Paint().apply {
                isAntiAlias = true
                color = Color.parseColor("#00A86B")
                textSize = 9.5f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            }
            canvas.drawText("DETALLES OPERACIONALES", rightCardX + 10f, currentY + 16f, inspTitlePaint)
            canvas.drawText("Inspector: ${inspection.inspector}", rightCardX + 10f, currentY + 32f, infoValPaint)
            canvas.drawText("Inicio: ${inspection.startTime}", rightCardX + 10f, currentY + 45f, infoValPaint)
            val finishTimeStr = inspection.endTime ?: "No finalizada"
            canvas.drawText("Fin: $finishTimeStr", rightCardX + 10f, currentY + 58f, infoValPaint)

            currentY += 85f

            // 3. Compact Checklist Layout (3-Column compact listing)
            canvas.drawText("RESUMEN DE LISTA DE CHEQUEO (MICRO-GRILLA)", 20f, currentY, sectionPaint)
            currentY += 10f
            
            val gridStartRowY = currentY
            val colCount = 3
            val colWidth = 175f // fits beautifully inside width 555
            val rowHeight = 12f
            
            // Draw compact background border grid for checked items
            val gridItemsCount = inspection.items.size
            val itemsPerCol = Math.ceil(gridItemsCount.toDouble() / colCount).toInt()
            val gridTotalHeight = itemsPerCol * rowHeight + 10f
            
            canvas.drawRoundRect(20f, currentY, 575f, currentY + gridTotalHeight, 4f, 4f, Paint().apply {
                color = Color.parseColor("#F8FAFC") // slate 50
                style = Paint.Style.FILL
            })
            canvas.drawRoundRect(20f, currentY, 575f, currentY + gridTotalHeight, 4f, 4f, borderPaint)
            
            val itemTextPaint = Paint().apply {
                isAntiAlias = true
                color = Color.parseColor("#334155") // Slate 700
                textSize = 7f
            }
            
            for (i in 0 until gridItemsCount) {
                val colIndex = i % colCount
                val rowIndex = i / colCount
                
                val item = inspection.items[i]
                val itemX = 25f + (colIndex * colWidth)
                val itemY = gridStartRowY + 10f + (rowIndex * rowHeight)
                
                // Draw Status Indicator
                val statusText: String
                val statusColor: Int
                if (item.status == "pass") {
                    statusText = "✓"
                    statusColor = Color.parseColor("#15803D") // Forest Green
                } else if (item.status == "fail") {
                    statusText = "✗"
                    statusColor = Color.parseColor("#b91c1c") // Alert Red
                } else {
                    statusText = "–"
                    statusColor = Color.GRAY
                }
                
                val bulletPaint = Paint().apply {
                    isAntiAlias = true
                    color = statusColor
                    textSize = 7.5f
                    typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                }
                
                canvas.drawText(statusText, itemX, itemY, bulletPaint)
                
                // Truncate name if it's too long to prevent row overlap
                val displayName = if (item.name.length > 34) item.name.take(32) + ".." else item.name
                canvas.drawText(displayName, itemX + 11f, itemY, itemTextPaint)
            }
            
            currentY += gridTotalHeight + 15f

            // 4. Highlighted Failures & Deficiencies (Summary Focus)
            val failedItems = inspection.items.filter { it.status == "fail" }
            if (failedItems.isNotEmpty()) {
                canvas.drawText("ALERTAS DE DEFICIENCIA Y HALLAZGOS", 20f, currentY, Paint().apply {
                    isAntiAlias = true
                    color = Color.parseColor("#b91c1c") // Highlight in Red
                    textSize = 10.5f
                    typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                })
                currentY += 8f
                
                val tableHeaderY = currentY
                // Draw Table Header
                canvas.drawRect(20f, tableHeaderY, 575f, tableHeaderY + 15f, Paint().apply {
                    color = Color.parseColor("#FEE2E2") // Alert Light Red
                    style = Paint.Style.FILL
                })
                canvas.drawRect(20f, tableHeaderY, 575f, tableHeaderY + 15f, borderPaint)
                
                val errHeaderPaint = Paint().apply {
                    isAntiAlias = true
                    color = Color.parseColor("#991B1B")
                    textSize = 7.5f
                    typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                }
                canvas.drawText("ELEMENTO REPORTADO CON FALLO", 25f, tableHeaderY + 11f, errHeaderPaint)
                canvas.drawText("DESCRIPCIÓN DEL DEFECTO Y OBSERVACIÓN", 200f, tableHeaderY + 11f, errHeaderPaint)
                
                currentY += 15f
                
                failedItems.forEach { failedItem ->
                    canvas.drawRect(20f, currentY, 575f, currentY + 22f, Paint().apply {
                        color = Color.WHITE
                        style = Paint.Style.FILL
                    })
                    canvas.drawRect(20f, currentY, 575f, currentY + 22f, borderPaint)
                    
                    val errValPaint = Paint().apply {
                        isAntiAlias = true
                        color = Color.parseColor("#b91c1c")
                        textSize = 7.5f
                        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                    }
                    canvas.drawText(failedItem.name, 25f, currentY + 14f, errValPaint)
                    
                    val noteText = failedItem.notes ?: "Sin anomalías text u observaciones adicionales"
                    val itemNotePaint = Paint().apply {
                        isAntiAlias = true
                        color = Color.parseColor("#1E293B")
                        textSize = 7.5f
                    }
                    canvas.drawText(noteText.take(80), 200f, currentY + 14f, itemNotePaint)
                    
                    currentY += 22f
                }
                currentY += 10f
            } else {
                // If there are no failed items, nice green status certificate banner!
                canvas.drawRoundRect(20f, currentY, 575f, currentY + 25f, 4f, 4f, greenBgPaint)
                canvas.drawRoundRect(20f, currentY, 575f, currentY + 25f, 4f, 4f, borderPaint)
                
                val noErrorsPaint = Paint().apply {
                    isAntiAlias = true
                    color = Color.parseColor("#15803D")
                    textSize = 8.5f
                    typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                }
                canvas.drawText("✓ VEHÍCULO SIN ANOMALÍAS DETECTADAS: El vehículo cumple con los estándares mínimos operacionales.", 30f, currentY + 16f, noErrorsPaint)
                currentY += 35f
            }

            // 5. General Notes (if any)
            if (!inspection.notes.isNullOrEmpty()) {
                canvas.drawText("OBSERVACIONES GENERALES", 20f, currentY, sectionPaint)
                currentY += 6f
                
                canvas.drawRoundRect(20f, currentY, 575f, currentY + 30f, 4f, 4f, Paint().apply {
                    color = Color.parseColor("#F1F5F9") // slate 100
                    style = Paint.Style.FILL
                })
                canvas.drawRoundRect(20f, currentY, 575f, currentY + 30f, 4f, 4f, borderPaint)
                
                val gNotesPaint = Paint().apply {
                    isAntiAlias = true
                    color = Color.parseColor("#334155")
                    textSize = 7.5f
                }
                val textWrap = if (inspection.notes.length > 130) inspection.notes.take(127) + "..." else inspection.notes
                canvas.drawText(textWrap, 28f, currentY + 17f, gNotesPaint)
                currentY += 40f
            }

            // Margin bottom limit validation
            val availableRemaining = height - currentY
            if (availableRemaining < 120f) {
                // If signatures won't fit, start a new page. But to keep it compact on 1 page we align tightly.
                currentY = height - 120f
            }

            // 6. Signatures Section at the Bottom
            // Divider
            canvas.drawLine(20f, currentY, 575f, currentY, borderPaint)
            currentY += 10f

            // Frame Boxes for signatures side-by-side
            val sigBoxWidth = (width - 50f) / 2f
            
            // Left Box: Inspector
            canvas.drawRoundRect(20f, currentY, 20f + sigBoxWidth, currentY + 74f, 4f, 4f, Paint().apply {
                color = Color.parseColor("#FAFAFA")
                style = Paint.Style.FILL
            })
            canvas.drawRoundRect(20f, currentY, 20f + sigBoxWidth, currentY + 74f, 4f, 4f, borderPaint)
            
            // Draw inspector signature bitmap
            val inspectorSigBmp = base64ToBitmap(inspection.firmaInspector)
            if (inspectorSigBmp != null) {
                val destRect = RectF(40f, currentY + 8f, 20f + sigBoxWidth - 20f, currentY + 54f)
                canvas.drawBitmap(inspectorSigBmp, null, destRect, Paint().apply { isFilterBitmap = true })
            } else {
                canvas.drawText("[Sin Firma de Inspector]", 65f, currentY + 35f, Paint().apply {
                    isAntiAlias = true
                    color = Color.GRAY
                    textSize = 8f
                })
            }
            
            val signatureLabelPaint = Paint().apply {
                isAntiAlias = true
                color = Color.parseColor("#475569")
                textSize = 7.5f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            }
            canvas.drawText("FIRMA INSPECTOR: ${inspection.inspector}", 25f, currentY + 68f, signatureLabelPaint)

            // Right Box: Assistant (Mechanic/Driver)
            val rightSigBoxX = 20f + sigBoxWidth + 10f
            canvas.drawRoundRect(rightSigBoxX, currentY, 575f, currentY + 74f, 4f, 4f, Paint().apply {
                color = Color.parseColor("#FAFAFA")
                style = Paint.Style.FILL
            })
            canvas.drawRoundRect(rightSigBoxX, currentY, 575f, currentY + 74f, 4f, 4f, borderPaint)

            // Draw assistant signature bitmap
            val assistantSigBmp = base64ToBitmap(inspection.firmaAsistente)
            if (assistantSigBmp != null) {
                val destRect = RectF(rightSigBoxX + 20f, currentY + 8f, 555f, currentY + 54f)
                canvas.drawBitmap(assistantSigBmp, null, destRect, Paint().apply { isFilterBitmap = true })
            } else {
                canvas.drawText("[Sin Firma del Conductor/Mecánico]", rightSigBoxX + 35f, currentY + 35f, Paint().apply {
                    isAntiAlias = true
                    color = Color.GRAY
                    textSize = 8f
                })
            }
            
            val assistantName = inspection.nombreAsistente ?: "Por firmar"
            val assistantCargo = inspection.cargoAsistente ?: "Conductor/Mecánico"
            canvas.drawText("FIRMA $assistantCargo: $assistantName", rightSigBoxX + 10f, currentY + 68f, signatureLabelPaint)

            // Finish the page details
            pdfDocument.finishPage(page)

            // Save PDF out to Cache memory
            val fileName = "inspeccion_${inspection.id}.pdf"
            val cacheFile = File(context.cacheDir, fileName)
            val outStream = FileOutputStream(cacheFile)
            pdfDocument.writeTo(outStream)
            pdfDocument.close()
            outStream.close()
            
            Log.d(TAG, "Successfully generated compact target PDF at ${cacheFile.absolutePath}")
            return cacheFile
        } catch (e: Exception) {
            Log.e(TAG, "Error compiling compact PDF report: ${e.message}", e)
            return null
        }
    }
}
