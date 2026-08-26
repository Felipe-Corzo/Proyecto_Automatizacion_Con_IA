package com.logitrack.service;

import com.logitrack.model.EstadoOrden;
import com.logitrack.model.OrdenCompra;
import com.lowagie.text.*;
import com.lowagie.text.pdf.*;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.awt.Color;

@Service
public class PdfGeneratorService {

    public byte[] crearPdfOrden(OrdenCompra orden) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Document document = new Document(PageSize.A4, 50, 50, 50, 50);

        try {
            PdfWriter writer = PdfWriter.getInstance(document, out);
            
            // Si la orden está en estado BORRADOR, inyectamos la marca de agua usando eventos de página de OpenPDF [9]
            if (orden.getEstado() == EstadoOrden.BORRADOR) {
                writer.setPageEvent(new PdfPageEventHelper() {
                    @Override
                    public void onEndPage(PdfWriter writer, Document document) {
                        PdfContentByte canvas = writer.getDirectContentUnder();
                        BaseFont font;
                        try {
                            font = BaseFont.createFont(BaseFont.HELVETICA_BOLD, BaseFont.WINANSI, BaseFont.EMBEDDED);
                        } catch (Exception e) {
                            return;
                        }
                        canvas.saveState();
                        // Color gris muy claro para simular semitransparencia legible [9]
                        canvas.setColorFill(new Color(220, 220, 220)); 
                        canvas.beginText();
                        canvas.setFontAndSize(font, 80);
                        // Escribir la marca de agua cruzada en un ángulo diagonal de 45 grados [9]
                        canvas.showTextAligned(Element.ALIGN_CENTER, "BORRADOR", 297, 421, 45);
                        canvas.endText();
                        canvas.restoreState();
                    }
                });
            }

            document.open();

            // Dibujar cabecera y contenidos legibles de la orden de compra [9]
            Paragraph title = new Paragraph("ORDEN DE COMPRA NRO: " + orden.getId(), FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18, Color.DARK_GRAY));
            title.setAlignment(Element.ALIGN_CENTER);
            document.add(title);
            document.add(new Paragraph("\n"));

            document.add(new Paragraph("Fecha de Creación: " + orden.getFechaCreacion().toString()));
            document.add(new Paragraph("Estado Actual: " + orden.getEstado().name()));
            document.add(new Paragraph("Proveedor Principal: " + orden.getProveedor().getNombre()));
            document.add(new Paragraph("Bodega de Destino: " + orden.getBodegaDestino().getNombre()));
            document.add(new Paragraph("\n"));

            // Crear tabla de productos de la orden
            PdfPTable table = new PdfPTable(4);
            table.setWidthPercentage(100);
            table.addCell("Producto");
            table.addCell("Cantidad");
            table.addCell("Precio Unitario");
            table.addCell("Total");

            table.addCell(orden.getProducto().getNombre());
            table.addCell(String.valueOf(orden.getCantidad()));
            table.addCell("$" + orden.getPrecioUnitario().toString());
            table.addCell("$" + orden.getTotal().toString());

            document.add(table);
            document.close();

        } catch (DocumentException e) {
            e.printStackTrace();
        }

        return out.toByteArray();
    }
}