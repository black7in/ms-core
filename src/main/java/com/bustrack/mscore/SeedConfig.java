package com.bustrack.mscore;

import com.bustrack.mscore.boletos.Boleto;
import com.bustrack.mscore.boletos.BoletoRepository;
import com.bustrack.mscore.buses.Bus;
import com.bustrack.mscore.buses.BusRepository;
import com.bustrack.mscore.choferes.Chofer;
import com.bustrack.mscore.choferes.ChoferRepository;
import com.bustrack.mscore.clientes.Cliente;
import com.bustrack.mscore.clientes.ClienteRepository;
import com.bustrack.mscore.common.enums.*;
import com.bustrack.mscore.facturas.Factura;
import com.bustrack.mscore.facturas.FacturaRepository;
import com.bustrack.mscore.horarios.Horario;
import com.bustrack.mscore.horarios.HorarioRepository;
import com.bustrack.mscore.rutas.Ruta;
import com.bustrack.mscore.rutas.RutaRepository;
import com.bustrack.mscore.tarifas.Tarifa;
import com.bustrack.mscore.tarifas.TarifaRepository;
import com.bustrack.mscore.terminales.Terminal;
import com.bustrack.mscore.terminales.TerminalRepository;
import com.bustrack.mscore.usuarios.Usuario;
import com.bustrack.mscore.usuarios.UsuarioRepository;
import com.bustrack.mscore.viajes.AsientoViaje;
import com.bustrack.mscore.viajes.AsientoViajeRepository;
import com.bustrack.mscore.viajes.Viaje;
import com.bustrack.mscore.viajes.ViajeRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

@Configuration
public class SeedConfig {

    @Bean
    CommandLineRunner seed(UsuarioRepository uRepo, TerminalRepository tRepo, BusRepository bRepo,
                           ChoferRepository cRepo, RutaRepository rRepo, HorarioRepository hRepo,
                           TarifaRepository taRepo, ClienteRepository clRepo, ViajeRepository vRepo,
                           AsientoViajeRepository aRepo, BoletoRepository boRepo, FacturaRepository fRepo,
                           PasswordEncoder encoder) {
        return args -> {
            if (uRepo.count() > 0) { System.out.println("Seed ya ejecutado."); return; }
            System.out.println("=== Iniciando seed (puede tardar 1-2 min) ===");

            var rnd = new Random(42);
            var factCounter = new AtomicInteger(1);

            // ── Terminales ──────────────────────────────────────────────────
            var tScz = tRepo.save(Terminal.builder().nombre("Terminal Bimodal Santa Cruz")
                    .ciudad("Santa Cruz").direccion("Av. Cañoto s/n, 3er anillo").build());
            var tCbb = tRepo.save(Terminal.builder().nombre("Terminal Cochabamba")
                    .ciudad("Cochabamba").direccion("Av. Ayacucho y Av. Heroínas").build());
            var tLpz = tRepo.save(Terminal.builder().nombre("Terminal La Paz")
                    .ciudad("La Paz").direccion("Av. Perú s/n, Zona Cementerio").build());

            // ── Usuarios staff ───────────────────────────────────────────────
            String pA = encoder.encode("admin123"), pS = encoder.encode("supervisor123"),
                   pV = encoder.encode("vendedor123"), pC = encoder.encode("chofer123");

            var uAdm    = uRepo.save(u("Administrador",      "admin@bustrack.com",          pA, Rol.ADMIN,      null));
            uRepo.save(u("Supervisor SCZ",  "supervisor.scz@bustrack.com",  pS, Rol.SUPERVISOR, tScz));
            uRepo.save(u("Supervisor CBB",  "supervisor.cbb@bustrack.com",  pS, Rol.SUPERVISOR, tCbb));
            uRepo.save(u("Supervisor LPZ",  "supervisor.lpz@bustrack.com",  pS, Rol.SUPERVISOR, tLpz));
            var uVScz   = uRepo.save(u("Vendedor SCZ",   "vendedor.scz@bustrack.com",   pV, Rol.VENDEDOR,  tScz));
            var uVCbb   = uRepo.save(u("Vendedor CBB",   "vendedor.cbb@bustrack.com",   pV, Rol.VENDEDOR,  tCbb));
            var uVLpz   = uRepo.save(u("Vendedor LPZ",   "vendedor.lpz@bustrack.com",   pV, Rol.VENDEDOR,  tLpz));
            var vendedores = List.of(uAdm, uVScz, uVCbb, uVLpz);
            System.out.println("7 usuarios staff");

            // ── Buses ────────────────────────────────────────────────────────
            String[][] bd = {
                {"3421-ABC","Mercedes-Benz","O-500","2020"},
                {"2897-XYZ","Scania","K-410","2021"},
                {"4512-DEF","Volvo","B12R","2019"},
                {"3788-GHI","Mercedes-Benz","O-500","2022"},
                {"5901-MNO","Scania","K-360","2020"},
                {"6234-PQR","Volvo","B9R","2021"},
                {"7845-STU","Mercedes-Benz","O-400","2018"},
                {"8123-VWX","Scania","K-440","2023"},
                {"9234-YZA","Volvo","B11R","2022"},
                {"1045-BCD","Mercedes-Benz","O-500RSD","2024"},
            };
            var buses = new ArrayList<Bus>();
            for (var d : bd)
                buses.add(bRepo.save(Bus.builder().placa(d[0]).marca(d[1]).modelo(d[2])
                        .anio(Integer.parseInt(d[3])).build()));
            System.out.println(buses.size() + " buses");

            // ── Choferes ─────────────────────────────────────────────────────
            String[][] cd = {
                {"5841234","Pedro Mamani",     "71234567","LIC-001234"},
                {"6234512","Luis Quispe",       "72456123","LIC-005678"},
                {"4782341","Roberto Condori",  "73891234","LIC-009012"},
                {"3921456","Carlos Flores",    "74123456","LIC-013456"},
                {"7234891","Jaime Choque",     "75678901","LIC-017890"},
                {"8123457","Marco Vargas",     "76234567","LIC-021234"},
                {"2348765","Juan Gutierrez",   "77890123","LIC-025678"},
                {"9876543","Victor Rojas",     "78345678","LIC-029012"},
                {"1237654","Hugo Lima",        "79901234","LIC-033456"},
                {"6543987","Daniel Torrez",    "70456789","LIC-037890"},
                {"3219876","Oscar Medina",     "71234560","LIC-041234"},
                {"7654329","Raul Salinas",     "72456124","LIC-045678"},
            };
            var choferes = new ArrayList<Chofer>();
            for (var d : cd) {
                var uc = uRepo.save(u(d[1], d[1].toLowerCase().replace(" ",".") + "@bustrack.com", pC, Rol.CHOFER, null));
                choferes.add(cRepo.save(Chofer.builder().usuario(uc).ci(d[0]).nombre(d[1])
                        .telefono(d[2]).licenciaNumero(d[3]).licenciaCategoria("C").licenciaVence("2028-12-31").build()));
            }
            System.out.println(choferes.size() + " choferes");

            // ── Rutas (todas las combinaciones entre 3 terminales) ────────────
            record RDef(Terminal o, Terminal d, String km, int min) {}
            var rdefs = List.of(
                new RDef(tScz, tCbb, "480",  480),
                new RDef(tCbb, tScz, "480",  480),
                new RDef(tScz, tLpz, "900",  720),
                new RDef(tLpz, tScz, "900",  720),
                new RDef(tCbb, tLpz, "450",  420),
                new RDef(tLpz, tCbb, "450",  420)
            );
            var rutas = new ArrayList<Ruta>();
            for (var r : rdefs)
                rutas.add(rRepo.save(Ruta.builder().origen(r.o()).destino(r.d())
                        .distanciaKm(new BigDecimal(r.km())).duracionEstimadaMin(r.min()).build()));
            System.out.println(rutas.size() + " rutas");

            // ── Horarios (mañana 06-08 + tarde 18-20, cada 30 min) ───────────
            String[] slots = {"06:00","06:30","07:00","07:30","08:00","18:00","18:30","19:00","19:30","20:00"};
            var horarios = new ArrayList<Horario>();
            for (var ruta : rutas)
                for (var slot : slots)
                    horarios.add(hRepo.save(Horario.builder().ruta(ruta).horaSalida(slot)
                            .diasSemana(List.of(1,2,3,4,5,6,7)).build()));
            System.out.println(horarios.size() + " horarios (6 rutas × 10 slots)");

            // ── Tarifas ──────────────────────────────────────────────────────
            // índice:  0=SCZ-CBB, 1=CBB-SCZ, 2=SCZ-LPZ, 3=LPZ-SCZ, 4=CBB-LPZ, 5=LPZ-CBB
            BigDecimal[][] tp = {
                {bd("70"),  bd("85")},
                {bd("70"),  bd("85")},
                {bd("130"), bd("155")},
                {bd("130"), bd("155")},
                {bd("65"),  bd("80")},
                {bd("65"),  bd("80")},
            };
            for (int i = 0; i < rutas.size(); i++) {
                taRepo.save(Tarifa.builder().ruta(rutas.get(i)).tipoDia(TipoDia.LUNES_JUEVES)
                        .precioBase(tp[i][0]).vigenteDesde("2026-01-01").build());
                taRepo.save(Tarifa.builder().ruta(rutas.get(i)).tipoDia(TipoDia.VIERNES_DOMINGO)
                        .precioBase(tp[i][1]).vigenteDesde("2026-01-01").build());
            }
            System.out.println("12 tarifas");

            // ── Clientes (200) ───────────────────────────────────────────────
            String[] nombres = {"Juan","María","Carlos","Ana","Miguel","Sofía","Diego","Valentina","Andrés","Isabella",
                "Mateo","Lucía","Alejandro","Emma","Sebastián","Mía","Nicolás","Camila","Gabriel","Zoé",
                "Santiago","Valeria","Tomás","Renata","Facundo","Martina","Bruno","Emilia","Thiago","Catalina",
                "Felipe","Daniela","Rodrigo","Florencia","Javier","Antonella","Cristian","Natalia","Eduardo","Paola",
                "Roberto","Claudia","Fernando","Patricia","Mario","Verónica","Sergio","Mónica","Hugo","Adriana"};
            String[] apellidos = {"Pérez","López","Gutiérrez","Mendoza","Torres","Reyes","Vargas","Cruz","Lima","Rojas",
                "Condori","Mamani","Quispe","Flores","Choque","Blanco","Ramos","Salazar","Vega","Morales",
                "Suárez","García","Martínez","Fernández","Díaz","Romero","Sánchez","Ruiz","Herrera","Medina",
                "Aguilar","Castro","Ortega","Delgado","Ramírez","Molina","Silva","Paredes","Espinoza","Fuentes"};
            var clientes = new ArrayList<Cliente>();
            for (int i = 0; i < 200; i++) {
                String nom = nombres[i % nombres.length];
                String ape = apellidos[(i / nombres.length + i) % apellidos.length];
                String ci  = String.format("11%05d", i + 1);
                String tel = String.format("7%07d", 6000000 + i);
                String email = i % 3 == 0 ? (nom.toLowerCase().replaceAll("[áéíóú]","a") + i + "@email.com") : null;
                clientes.add(clRepo.save(Cliente.builder().ci(ci).nombre(nom + " " + ape).telefono(tel).email(email).build()));
            }
            System.out.println(clientes.size() + " clientes");

            // ── Viajes: Mayo 2026 (histórico) ────────────────────────────────
            // Estrategia: 15 asientos fijos por viaje (5-13 vendidos + resto libre)
            // para mantener ~28K asientos y ~14K boletos en total manejables.
            int viajesCount = 0, boletosCount = 0;

            for (int dia = 1; dia <= 31; dia++) {
                var fecha = LocalDate.of(2026, 5, dia);
                boolean finde = fecha.getDayOfWeek().getValue() >= 5;

                for (int hi = 0; hi < horarios.size(); hi++) {
                    var horario = horarios.get(hi);
                    int rutaIdx = hi / 10;
                    var bus    = buses.get(viajesCount % buses.size());
                    var chofer = choferes.get(viajesCount % choferes.size());

                    var viaje = vRepo.save(Viaje.builder()
                            .horario(horario).fecha(fecha.toString()).bus(bus).choferTitular(chofer)
                            .estado(EstadoViaje.FINALIZADO)
                            .carrilAsignado(String.valueOf(1 + viajesCount % 10))
                            .build());
                    viajesCount++;

                    int numVendidos = 5 + rnd.nextInt(9); // 5-13
                    int totalSeat   = 44;
                    var asientoList = new ArrayList<AsientoViaje>(totalSeat);
                    for (int s = 1; s <= totalSeat; s++)
                        asientoList.add(AsientoViaje.builder().viaje(viaje).numeroAsiento(s)
                                .estado(s <= numVendidos ? EstadoAsiento.VENDIDO : EstadoAsiento.LIBRE).build());
                    aRepo.saveAll(asientoList);

                    var precio = finde ? tp[rutaIdx][1] : tp[rutaIdx][0];
                    var fechaVenta = fecha.atTime(6 + rnd.nextInt(10), rnd.nextInt(60));

                    var batchBoletos   = new ArrayList<Boleto>(numVendidos);
                    var batchFacturas  = new ArrayList<Factura>(numVendidos);

                    for (int s = 0; s < numVendidos; s++) {
                        var asiento  = asientoList.get(s);
                        var cliente  = clientes.get(rnd.nextInt(clientes.size()));
                        var vendedor = vendedores.get(rnd.nextInt(vendedores.size()));

                        var boleto = boRepo.save(Boleto.builder()
                                .viaje(viaje).asiento(asiento).cliente(cliente).vendedor(vendedor)
                                .precioPagado(precio).fechaVenta(fechaVenta)
                                .qrCode("").estado(EstadoBoleto.ABORDADO).build());
                        boleto.setQrCode(boleto.getId());
                        boRepo.save(boleto);

                        batchFacturas.add(Factura.builder().boleto(boleto)
                                .numeroFactura(String.format("BTBO-2026-%06d", factCounter.getAndIncrement()))
                                .nombreCliente(cliente.getNombre()).monto(precio).build());
                        boletosCount++;
                    }
                    fRepo.saveAll(batchFacturas);
                }

                if (dia % 7 == 0 || dia == 31)
                    System.out.printf("  Mayo: día %2d procesado — %d viajes, %d boletos%n", dia, viajesCount, boletosCount);
            }

            System.out.println("=== Seed completo ===");
            System.out.printf("Mayo: %d viajes históricos, %d boletos%n", viajesCount, boletosCount);
            System.out.println("admin@bustrack.com / admin123");
            System.out.println("vendedor.scz@bustrack.com / vendedor123");
            System.out.println("pedro.mamani@bustrack.com / chofer123");
        };
    }

    private static Usuario u(String nombre, String email, String pass, Rol rol, Terminal terminal) {
        return Usuario.builder().nombre(nombre).email(email).passwordHash(pass).rol(rol).terminal(terminal).build();
    }

    private static BigDecimal bd(String v) {
        return new BigDecimal(v).setScale(2, RoundingMode.HALF_UP);
    }
}
