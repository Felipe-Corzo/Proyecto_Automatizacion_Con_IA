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

    private static final Color AZUL_OSCURO = new Color(20, 45, 80);
    private static final Color AZUL_MEDIO = new Color(35, 70, 120);
    private static final Color AZUL_CLARO = new Color(230, 238, 248);
    private static final Color GRIS_TEXTO = new Color(80, 80, 80);
    private static final Color GRIS_BORDE = new Color(200, 200, 200);

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
        Document document = new Document(PageSize.A4, 40, 40, 50, 40);

        try {
            PdfWriter writer = PdfWriter.getInstance(document, out);

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
                        canvas.setColorFill(new Color(220, 220, 220));
                        canvas.beginText();
                        canvas.setFontAndSize(font, 80);
                        canvas.showTextAligned(Element.ALIGN_CENTER, "BORRADOR", 297, 421, 45);
                        canvas.endText();
                        canvas.restoreState();

                        drawFooter(writer);
                    }
                });
            } else {
                writer.setPageEvent(new PdfPageEventHelper() {
                    @Override
                    public void onEndPage(PdfWriter writer, Document document) {
                        drawFooter(writer);
                    }
                });
            }

            document.open();

            PdfPTable headerTable = new PdfPTable(2);
            headerTable.setWidthPercentage(100);
            headerTable.setWidths(new float[]{60, 40});

            PdfPCell logoCell = new PdfPCell();
            logoCell.setBackgroundColor(AZUL_OSCURO);
            logoCell.setBorder(Rectangle.NO_BORDER);
            logoCell.setPadding(20);
            Paragraph logoText = new Paragraph("LOGITRACK", FontFactory.getFont(FontFactory.HELVETICA_BOLD, 26, Color.WHITE));
            Paragraph logoSub = new Paragraph("Sistema de Gestión Logística", FontFactory.getFont(FontFactory.HELVETICA, 9, new Color(160, 185, 220)));
            logoSub.setSpacingBefore(2);
            logoCell.addElement(logoText);
            logoCell.addElement(logoSub);

            PdfPCell orderInfoCell = new PdfPCell();
            orderInfoCell.setBackgroundColor(AZUL_OSCURO);
            orderInfoCell.setBorder(Rectangle.NO_BORDER);
            orderInfoCell.setPadding(20);
            orderInfoCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
            Paragraph orderLabel = new Paragraph("ORDEN DE COMPRA", FontFactory.getFont(FontFactory.HELVETICA, 10, new Color(160, 185, 220)));
            Paragraph orderNum = new Paragraph("NRO: " + orden.getId(), FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18, Color.WHITE));
            orderNum.setSpacingBefore(4);
            orderInfoCell.addElement(orderLabel);
            orderInfoCell.addElement(orderNum);

            headerTable.addCell(logoCell);
            headerTable.addCell(orderInfoCell);
            document.add(headerTable);

            PdfPTable accentLine = new PdfPTable(1);
            accentLine.setWidthPercentage(100);
            PdfPCell accentCell = new PdfPCell();
            accentCell.setFixedHeight(4);
            accentCell.setBackgroundColor(AZUL_MEDIO);
            accentCell.setBorder(Rectangle.NO_BORDER);
            accentLine.addCell(accentCell);
            document.add(accentLine);

            document.add(new Paragraph(" ", FontFactory.getFont(FontFactory.HELVETICA, 6)));

            if (orden.getEstado() == EstadoOrden.BORRADOR) {
                PdfPTable borradorBanner = new PdfPTable(1);
                borradorBanner.setWidthPercentage(100);
                PdfPCell borradorCell = new PdfPCell(new Phrase("BORRADOR", FontFactory.getFont(FontFactory.HELVETICA_BOLD, 14, new Color(180, 120, 0))));
                borradorCell.setBackgroundColor(new Color(255, 248, 220));
                borradorCell.setBorderColor(new Color(230, 200, 80));
                borradorCell.setHorizontalAlignment(Element.ALIGN_CENTER);
                borradorCell.setPadding(8);
                borradorBanner.addCell(borradorCell);
                document.add(borradorBanner);
                document.add(new Paragraph(" ", FontFactory.getFont(FontFactory.HELVETICA, 4)));
            }

            Font labelFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9, AZUL_OSCURO);
            Font valueFont = FontFactory.getFont(FontFactory.HELVETICA, 10, GRIS_TEXTO);

            PdfPTable infoTable = new PdfPTable(4);
            infoTable.setWidthPercentage(100);
            infoTable.setSpacingAfter(15);

            addInfoField(infoTable, "Fecha de Creación", orden.getFechaCreacion().toString(), labelFont, valueFont);
            addInfoField(infoTable, "Estado", orden.getEstado().name(), labelFont, valueFont);
            addInfoField(infoTable, "Proveedor", orden.getProveedor().getNombre(), labelFont, valueFont);
            addInfoField(infoTable, "Bodega Destino", orden.getBodegaDestino().getNombre(), labelFont, valueFont);

            document.add(infoTable);

            Font headerFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, Color.WHITE);
            PdfPTable table = new PdfPTable(4);
            table.setWidthPercentage(100);
            table.setWidths(new float[]{45, 15, 20, 20});
            table.setHeaderRows(1);

            addHeaderCell(table, "Producto", headerFont);
            addHeaderCell(table, "Cantidad", headerFont);
            addHeaderCell(table, "Precio Unitario", headerFont);
            addHeaderCell(table, "Total", headerFont);

            List<com.logitrack.model.OrdenCompraDetalle> detalles = orden.getDetalles();
            if (detalles == null || detalles.isEmpty()) {
                com.logitrack.model.OrdenCompraDetalle detalle = new com.logitrack.model.OrdenCompraDetalle();
                detalle.setProducto(orden.getProducto());
                detalle.setCantidad(orden.getCantidad());
                detalle.setPrecioUnitario(orden.getPrecioUnitario());
                detalles = List.of(detalle);
            }

            Font cellFont = FontFactory.getFont(FontFactory.HELVETICA, 9, GRIS_TEXTO);
            boolean alternate = false;
            for (com.logitrack.model.OrdenCompraDetalle detalle : detalles) {
                Color rowBg = alternate ? AZUL_CLARO : Color.WHITE;
                addDataCell(table, detalle.getProducto().getNombre(), cellFont, rowBg, Element.ALIGN_LEFT);
                addDataCell(table, String.valueOf(detalle.getCantidad()), cellFont, rowBg, Element.ALIGN_CENTER);
                addDataCell(table, "$" + detalle.getPrecioUnitario(), cellFont, rowBg, Element.ALIGN_RIGHT);
                addDataCell(table, "$" + detalle.getPrecioUnitario().multiply(BigDecimal.valueOf(detalle.getCantidad())), cellFont, rowBg, Element.ALIGN_RIGHT);
                alternate = !alternate;
            }

            document.add(table);
            document.add(new Paragraph(" ", FontFactory.getFont(FontFactory.HELVETICA, 6)));

            PdfPTable totalTable = new PdfPTable(2);
            totalTable.setWidthPercentage(40);
            totalTable.setHorizontalAlignment(Element.ALIGN_RIGHT);

            PdfPCell totalLabel = new PdfPCell(new Phrase("TOTAL DE LA ORDEN", FontFactory.getFont(FontFactory.HELVETICA_BOLD, 11, AZUL_OSCURO)));
            totalLabel.setBackgroundColor(AZUL_CLARO);
            totalLabel.setBorder(Rectangle.NO_BORDER);
            totalLabel.setPadding(10);
            totalLabel.setHorizontalAlignment(Element.ALIGN_RIGHT);

            PdfPCell totalValue = new PdfPCell(new Phrase("$" + orden.getTotal(), FontFactory.getFont(FontFactory.HELVETICA_BOLD, 14, Color.WHITE)));
            totalValue.setBackgroundColor(AZUL_OSCURO);
            totalValue.setBorder(Rectangle.NO_BORDER);
            totalValue.setPadding(10);
            totalValue.setHorizontalAlignment(Element.ALIGN_CENTER);

            totalTable.addCell(totalLabel);
            totalTable.addCell(totalValue);
            document.add(totalTable);

            document.close();

        } catch (DocumentException e) {
            throw new BadRequestException("Error generando el PDF: " + e.getMessage(), e);
        }

        return out.toByteArray();
    }

    private void drawFooter(PdfWriter writer) {
        PdfContentByte footer = writer.getDirectContent();
        footer.saveState();
        footer.setColorFill(AZUL_OSCURO);
        footer.rectangle(0, 0, PageSize.A4.getWidth(), 25);
        footer.fill();
        footer.restoreState();

        footer.beginText();
        try {
            BaseFont footerFont = BaseFont.createFont(BaseFont.HELVETICA, BaseFont.WINANSI, BaseFont.EMBEDDED);
            footer.setFontAndSize(footerFont, 7);
            footer.setColorFill(Color.WHITE);
            footer.showTextAligned(Element.ALIGN_CENTER,
                    "LogiTrack - Sistema de Gestión Logística | logitrack.com",
                    PageSize.A4.getWidth() / 2, 9, 0);
        } catch (Exception ignored) {}
        footer.endText();
    }

    private void addInfoField(PdfPTable table, String label, String value, Font labelFont, Font valueFont) {
        PdfPCell container = new PdfPCell();
        container.setBorder(Rectangle.NO_BORDER);
        container.setPaddingBottom(8);
        Paragraph lbl = new Paragraph(label, labelFont);
        lbl.setSpacingAfter(2);
        Paragraph val = new Paragraph(value, valueFont);
        container.addElement(lbl);
        container.addElement(val);
        table.addCell(container);
    }

    private void addHeaderCell(PdfPTable table, String text, Font font) {
        PdfPCell cell = new PdfPCell(new Phrase(text, font));
        cell.setBackgroundColor(AZUL_OSCURO);
        cell.setPadding(10);
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
        cell.setBorderWidth(0);
        table.addCell(cell);
    }

    private void addDataCell(PdfPTable table, String text, Font font, Color bgColor, int align) {
        PdfPCell cell = new PdfPCell(new Phrase(text, font));
        cell.setBackgroundColor(bgColor);
        cell.setPadding(8);
        cell.setHorizontalAlignment(align);
        cell.setBorderWidth(0.5f);
        cell.setBorderColor(GRIS_BORDE);
        table.addCell(cell);
    }
}