package com.bustrack.mscore.boletos;

import com.bustrack.mscore.blockchain.BlockchainService;
import com.bustrack.mscore.clientes.ClienteRepository;
import com.bustrack.mscore.common.enums.*;
import com.bustrack.mscore.facturas.Factura;
import com.bustrack.mscore.facturas.FacturaRepository;
import com.bustrack.mscore.tarifas.TarifaRepository;
import com.bustrack.mscore.viajes.*;
import com.bustrack.mscore.storage.StorageService;
import com.bustrack.mscore.webhooks.WebhooksService;
import com.bustrack.mscore.usuarios.UsuarioRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.lowagie.text.Document;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.Image;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.PdfWriter;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.io.ByteArrayOutputStream;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.*;

@Service
@Transactional
public class BoletoService {

    private static final Logger log = LoggerFactory.getLogger(BoletoService.class);

    private final BoletoRepository boletoRepo;
    private final AsientoViajeRepository asientoRepo;
    private final ViajeRepository viajeRepo;
    private final ClienteRepository clienteRepo;
    private final TarifaRepository tarifaRepo;
    private final FacturaRepository facturaRepo;
    private final StorageService storage;
    private final WebhooksService webhooks;
    private final UsuarioRepository usuarioRepo;
    private final BlockchainService blockchain;

    @org.springframework.beans.factory.annotation.Value("${app.frontend-url:http://localhost:4200}")
    private String frontendUrl;

    public BoletoService(BoletoRepository boletoRepo, AsientoViajeRepository asientoRepo,
                         ViajeRepository viajeRepo, ClienteRepository clienteRepo,
                         TarifaRepository tarifaRepo, FacturaRepository facturaRepo,
                         StorageService storage, WebhooksService webhooks,
                         UsuarioRepository usuarioRepo, BlockchainService blockchain) {
        this.boletoRepo = boletoRepo;
        this.asientoRepo = asientoRepo;
        this.viajeRepo = viajeRepo;
        this.clienteRepo = clienteRepo;
        this.tarifaRepo = tarifaRepo;
        this.facturaRepo = facturaRepo;
        this.storage = storage;
        this.webhooks = webhooks;
        this.usuarioRepo = usuarioRepo;
        this.blockchain = blockchain;
    }

    public Boleto findById(String id) {
        return boletoRepo.findById(id).orElseThrow(() -> new RuntimeException("Boleto no encontrado"));
    }

    public Boleto venderBoleto(String viajeId, String asientoId, String clienteId,
                                String vendedorId, Double precioVenta, String nit, String razonSocial) {
        var viaje = viajeRepo.findById(viajeId)
                .orElseThrow(() -> new RuntimeException("Viaje no encontrado"));
        if (viaje.getEstado() != EstadoViaje.PROGRAMADO)
            throw new RuntimeException("Viaje no está programado");

        var asiento = asientoRepo.findByIdAndViajeIdForUpdate(asientoId, viajeId)
                .orElseThrow(() -> new RuntimeException("Asiento no disponible"));
        if (asiento.getEstado() != EstadoAsiento.LIBRE)
            throw new RuntimeException("Asiento no disponible");

        var tarifa = tarifaRepo.findTarifaActual(viaje.getHorario().getRuta().getId(),
                determinarTipoDia(viaje.getFecha()), viaje.getFecha())
                .orElseThrow(() -> new RuntimeException("No hay tarifa vigente"));

        asiento.setEstado(EstadoAsiento.VENDIDO);
        asientoRepo.save(asiento);

        var vendedor = usuarioRepo.findById(vendedorId).orElseThrow();
        var cliente = clienteRepo.findById(clienteId).orElseThrow();
        var precio = precioVenta != null
                ? java.math.BigDecimal.valueOf(precioVenta)
                : tarifa.getPrecioBase();
        var boleto = Boleto.builder().viaje(viaje).asiento(asiento).cliente(cliente)
                .vendedor(vendedor).precioPagado(precio)
                .fechaVenta(LocalDateTime.now()).estado(EstadoBoleto.VIGENTE).qrCode("").build();
        var saved = boletoRepo.save(boleto);
        saved.setQrCode(saved.getId());
        boletoRepo.save(saved);

        var anio = LocalDateTime.now().getYear();
        var numFactura = "BTBO-" + anio + "-" + String.format("%06d", boletoRepo.count() + 1);
        var factura = facturaRepo.save(Factura.builder().boleto(saved).numeroFactura(numFactura)
                .nit(nit).razonSocial(razonSocial).nombreCliente(cliente.getNombre())
                .monto(precio).build());

        try {
            procesarPostVenta(saved, factura);
        } catch (Exception e) {
            throw new RuntimeException("Error procesando venta: " + e.getMessage());
        }

        return boletoRepo.findById(saved.getId()).orElse(saved);
    }

    public Boleto cancelarBoleto(String id) {
        var b = findById(id);
        if (b.getEstado() != EstadoBoleto.VIGENTE) throw new RuntimeException("Boleto no vigente");
        if (b.getViaje().getEstado() != EstadoViaje.PROGRAMADO)
            throw new RuntimeException("Solo se pueden cancelar boletos de viajes programados");
        b.setEstado(EstadoBoleto.CANCELADO);
        b.setMontoDevuelto(b.getPrecioPagado());
        b.setFechaCancelacion(LocalDateTime.now());
        b.getAsiento().setEstado(EstadoAsiento.LIBRE);
        asientoRepo.save(b.getAsiento());
        return boletoRepo.save(b);
    }

    private void procesarPostVenta(Boleto boleto, Factura factura) throws Exception {
        // 1. PDF del boleto → S3
        var boletoPdf = generarPdfBoleto(boleto);
        var boletoS3Key = "boletos/" + boleto.getViaje().getId() + "/" + boleto.getId() + ".pdf";
        storage.uploadFile(boletoS3Key, boletoPdf);
        boleto.setPdfS3Key(boletoS3Key);
        boletoRepo.save(boleto);

        // 2. Hash de datos de factura → blockchain → txHash → PDF con txHash → S3
        var hashData = calcularHash((factura.getNumeroFactura() + factura.getMonto() + factura.getNombreCliente()).getBytes());
        factura.setHashSha256(hashData);
        var txHash = blockchain.registrarFactura(hashData, factura.getNumeroFactura(), factura.getMonto());
        factura.setBlockchainTxHash(txHash);
        factura.setBlockchainEstado(BlockchainEstado.CONFIRMADO);

        var facturaPdf = generarPdfFactura(factura, boleto, txHash, hashData);
        var facturaS3Key = "facturas/" + factura.getId() + ".pdf";
        storage.uploadFile(facturaS3Key, facturaPdf);
        factura.setPdfS3Key(facturaS3Key);
        facturaRepo.save(factura);

        // 3. Notificación WhatsApp (async, no bloquea la respuesta)
        var cliente = boleto.getCliente();
        if (cliente.getTelefono() != null && !cliente.getTelefono().isEmpty()) {
            final var pdfUrl = storage.getDownloadUrl(boletoS3Key);
            Thread.ofVirtual().start(() -> {
                try {
                    webhooks.notifyVenta(Map.of(
                        "pasajero", Map.of("nombre", cliente.getNombre(),
                            "telefono", "591" + cliente.getTelefono().replaceAll("^\\+591|^\\+", "")),
                        "viaje", Map.of(
                            "ruta", Map.of(
                                "origen", boleto.getViaje().getHorario().getRuta().getOrigen().getCiudad(),
                                "destino", boleto.getViaje().getHorario().getRuta().getDestino().getCiudad()),
                            "fecha", boleto.getViaje().getFecha(),
                            "horaSalida", boleto.getViaje().getHorario().getHoraSalida(),
                            "carrilAsignado", boleto.getViaje().getCarrilAsignado() != null
                                    ? boleto.getViaje().getCarrilAsignado() : ""),
                        "boleto", Map.of(
                            "numeroAsiento", String.valueOf(boleto.getAsiento().getNumeroAsiento()),
                            "precioPagado", String.valueOf(boleto.getPrecioPagado()),
                            "urlPdf", pdfUrl)
                    ));
                } catch (Exception ignored) {}
            });
        }
    }

    private byte[] generarPdfFactura(Factura factura, Boleto boleto, String txHash, String hashSha256) throws Exception {
        var out = new ByteArrayOutputStream();
        var doc = new Document(new Rectangle(540, 400));
        PdfWriter.getInstance(doc, out);
        doc.open();

        Font title = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 14);
        Font bold = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10);
        Font normal = FontFactory.getFont(FontFactory.HELVETICA, 10);
        Font small = FontFactory.getFont(FontFactory.HELVETICA, 7);

        // QR de verificación en esquina superior derecha (posición absoluta, no afecta flujo de texto)
        String verifyUrl = frontendUrl + "/verificar?hash=" + hashSha256;
        byte[] qrBytes = generarQrBytes(verifyUrl, 75);
        Image qrImg = Image.getInstance(qrBytes);
        qrImg.setAbsolutePosition(429, 289);
        doc.add(qrImg);

        doc.add(new Paragraph("BusTrack BO — Factura", title));
        doc.add(new Paragraph("Nro: " + factura.getNumeroFactura(), bold));
        doc.add(new Paragraph("Fecha: " + factura.getFecha().toLocalDate(), normal));
        doc.add(new Paragraph(" ", normal));
        if (factura.getNit() != null)
            doc.add(new Paragraph("NIT: " + factura.getNit(), normal));
        if (factura.getRazonSocial() != null)
            doc.add(new Paragraph("Razón social: " + factura.getRazonSocial(), normal));
        doc.add(new Paragraph("Cliente: " + factura.getNombreCliente(), normal));
        doc.add(new Paragraph(" ", normal));
        var ruta = boleto.getViaje().getHorario().getRuta();
        doc.add(new Paragraph("Ruta: " + ruta.getOrigen().getCiudad() + " → " + ruta.getDestino().getCiudad(), normal));
        doc.add(new Paragraph("Fecha viaje: " + boleto.getViaje().getFecha()
                + "  Hora: " + boleto.getViaje().getHorario().getHoraSalida(), normal));
        doc.add(new Paragraph("Asiento: " + boleto.getAsiento().getNumeroAsiento(), normal));
        doc.add(new Paragraph(" ", normal));
        doc.add(new Paragraph("TOTAL: Bs. " + factura.getMonto(), bold));
        doc.add(new Paragraph(" ", normal));
        doc.add(new Paragraph("Blockchain TX: " + txHash, normal));
        doc.add(new Paragraph("Escanear QR para verificar autenticidad en blockchain", small));

        doc.close();
        return out.toByteArray();
    }

    private byte[] generarPdfBoleto(Boleto boleto) throws Exception {
        var out = new ByteArrayOutputStream();
        var doc = new Document(new Rectangle(540, 240));
        PdfWriter.getInstance(doc, out);
        doc.open();

        Font title = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 14);
        Font normal = FontFactory.getFont(FontFactory.HELVETICA, 10);

        doc.add(new Paragraph("BusTrack BO", title));
        doc.add(new Paragraph("Boleto: " + boleto.getId().substring(0, 8), normal));
        doc.add(new Paragraph("Asiento: " + boleto.getAsiento().getNumeroAsiento(), normal));
        var ruta = boleto.getViaje().getHorario().getRuta();
        doc.add(new Paragraph("Ruta: " + ruta.getOrigen().getCiudad() + " -> " + ruta.getDestino().getCiudad(), normal));
        doc.add(new Paragraph("Fecha: " + boleto.getViaje().getFecha()
                + "  Hora: " + boleto.getViaje().getHorario().getHoraSalida(), normal));
        if (boleto.getViaje().getBus() != null)
            doc.add(new Paragraph("Bus: " + boleto.getViaje().getBus().getPlaca(), normal));
        doc.add(new Paragraph("Cliente: " + boleto.getCliente().getNombre()
                + "  CI: " + boleto.getCliente().getCi(), normal));
        doc.add(new Paragraph("Precio: Bs. " + boleto.getPrecioPagado(), normal));

        byte[] qrBytes = generarQrBytes(boleto.getQrCode(), 100);
        Image qrImg = Image.getInstance(qrBytes);
        qrImg.setAbsolutePosition(410, 100);
        doc.add(qrImg);

        doc.close();
        return out.toByteArray();
    }

    private byte[] generarQrBytes(String content, int size) throws Exception {
        BitMatrix matrix = new QRCodeWriter().encode(content, BarcodeFormat.QR_CODE, size, size);
        var out = new ByteArrayOutputStream();
        MatrixToImageWriter.writeToStream(matrix, "PNG", out);
        return out.toByteArray();
    }

    private String calcularHash(byte[] bytes) {
        try {
            var digest = MessageDigest.getInstance("SHA-256");
            var hash = digest.digest(bytes);
            var sb = new StringBuilder();
            for (byte b : hash) sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (Exception e) {
            return null;
        }
    }

    private TipoDia determinarTipoDia(String fecha) {
        var feriados = java.util.Set.of("01-01","01-22","02-17","02-18","03-23","04-10",
                "05-01","06-19","08-06","11-02","12-25");
        var d = java.time.LocalDate.parse(fecha);
        if (feriados.contains(fecha.substring(5))) return TipoDia.FERIADO;
        if (d.getDayOfWeek().getValue() >= 5) return TipoDia.VIERNES_DOMINGO;
        return TipoDia.LUNES_JUEVES;
    }
}
