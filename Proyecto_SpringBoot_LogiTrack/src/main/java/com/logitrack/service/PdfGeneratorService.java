package com.logitrack.service;

import com.logitrack.exception.BadRequestException;
import com.logitrack.model.EstadoOrden;
import com.logitrack.model.OrdenCompra;
import com.lowagie.text.*;
import com.lowagie.text.pdf.*;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.awt.Color;
import java.math.BigDecimal;
import java.util.List;

@Service
public class PdfGeneratorService {

    public byte[] crearPdfOrden(OrdenCompra orden) {
        if (orden == null) {
            throw new BadRequestException("La orden de compra no puede ser nula");
        }
        if (orden.getProveedor() == null) {
            throw new BadRequestException("La orden de compra debe tener un proveedor asociado");
        }
        if (orden.getBodegaDestino() == null) {
            throw new BadRequestException("La orden de compra debe tener una bodega de destino asociada");
        }
        if (orden.getProducto() == null) {
            throw new BadRequestException("La orden de compra debe tener un producto asociado");
        }
        if (orden.getFechaCreacion() == null) {
            throw new BadRequestException("La orden de compra debe tener una fecha de creación");
        }
        if (orden.getEstado() == null) {
            throw new BadRequestException("La orden de compra debe tener un estado");
        }
        if (orden.getCantidad() == null) {
            throw new BadRequestException("La orden de compra debe tener una cantidad");
        }
        if (orden.getPrecioUnitario() == null) {
            throw new BadRequestException("La orden de compra debe tener un precio unitario");
        }
        if (orden.getTotal() == null) {
            throw new BadRequestException("La orden de compra debe tener un total calculado");
        }

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

            // Si está en BORRADOR, agregar marca visible en el contenido del documento
            if (orden.getEstado() == EstadoOrden.BORRADOR) {
                Paragraph watermark = new Paragraph("BORRADOR", FontFactory.getFont(FontFactory.HELVETICA_BOLD, 24, new Color(200, 200, 200)));
                watermark.setAlignment(Element.ALIGN_CENTER);
                document.add(watermark);
                document.add(new Paragraph("\n"));
            }

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

            List<com.logitrack.model.OrdenCompraDetalle> detalles = orden.getDetalles();
            if (detalles == null || detalles.isEmpty()) {
                com.logitrack.model.OrdenCompraDetalle detalle = new com.logitrack.model.OrdenCompraDetalle();
                detalle.setProducto(orden.getProducto());
                detalle.setCantidad(orden.getCantidad());
                detalle.setPrecioUnitario(orden.getPrecioUnitario());
                detalles = List.of(detalle);
            }
            for (com.logitrack.model.OrdenCompraDetalle detalle : detalles) {
                table.addCell(detalle.getProducto().getNombre());
                table.addCell(String.valueOf(detalle.getCantidad()));
                table.addCell("$" + detalle.getPrecioUnitario());
                table.addCell("$" + detalle.getPrecioUnitario().multiply(BigDecimal.valueOf(detalle.getCantidad())));
            }
            PdfPCell totalLabel = new PdfPCell(new Phrase("TOTAL DE LA ORDEN", FontFactory.getFont(FontFactory.HELVETICA_BOLD, 11)));
            totalLabel.setColspan(3);
            totalLabel.setHorizontalAlignment(Element.ALIGN_RIGHT);
            table.addCell(totalLabel);
            PdfPCell totalValue = new PdfPCell(new Phrase("$" + orden.getTotal(), FontFactory.getFont(FontFactory.HELVETICA_BOLD, 11)));
            totalValue.setHorizontalAlignment(Element.ALIGN_RIGHT);
            table.addCell(totalValue);

            document.add(table);
            document.close();

        } catch (DocumentException e) {
            throw new BadRequestException("Error generando el PDF: " + e.getMessage(), e);
        }

        return out.toByteArray();
    }
}