package com.optician.backend.config;

import com.optician.backend.model.*;
import com.optician.backend.model.enums.*;
import com.optician.backend.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Component
@RequiredArgsConstructor
@SuppressWarnings("deprecation")
public class DataInitializer implements CommandLineRunner {

    private final ProductRepository productRepository;
    private final BrandRepository brandRepository;
    private final CategoryRepository categoryRepository;
    private final ProductVariantRepository variantRepository;
    private final PatientRepository patientRepository;
    private final PrescriptionRepository prescriptionRepository;
    private final AppointmentRepository appointmentRepository;
    private final OrderRepository orderRepository;
    private final StoreRepository storeRepository;
    private final PromotionRepository promotionRepository;
    private final NotificationRepository notificationRepository;
    private final UserAccountRepository userAccountRepository;
    private final StockRepository stockRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.bootstrap.admin.email:}")
    private String bootstrapAdminEmail;

    @Value("${app.bootstrap.admin.password:}")
    private String bootstrapAdminPassword;

    @Value("${app.bootstrap.admin.full-name:Administrateur}")
    private String bootstrapAdminFullName;

    @Override
    public void run(String... args) throws Exception {
        createBootstrapAdmin();
        if (productRepository.count() > 0) return;

        // 1. Seed Stores
        Store s1 = Store.builder()
                .name("OptiVision Tunis Centre")
                .city("Tunis")
                .address("Avenue Habib Bourguiba")
                .zipCode("1001")
                .phone("71 345 678")
                .email("tunis@optivision.tn")
                .openingHours("Lun - Sam : 09h30 - 19h30")
                .active(true)
                .latitude(36.7994)
                .longitude(10.1803)
                .build();

        Store s2 = Store.builder()
                .name("OptiVision Sousse Centre")
                .city("Sousse")
                .address("Boulevard du 14 Janvier")
                .zipCode("4000")
                .phone("73 226 789")
                .email("sousse@optivision.tn")
                .openingHours("Lun - Sam : 09h30 - 19h00")
                .active(true)
                .latitude(35.8256)
                .longitude(10.6084)
                .build();

        Store s3 = Store.builder()
                .name("OptiVision Sfax Mall")
                .city("Sfax")
                .address("Route de l'Aéroport, Sfax Mall")
                .zipCode("3003")
                .phone("74 224 560")
                .email("sfax@optivision.tn")
                .openingHours("Lun - Dim : 10h00 - 20h00")
                .active(true)
                .latitude(34.7406)
                .longitude(10.7603)
                .build();

        storeRepository.saveAll(List.of(s1, s2, s3));

        // 2. Seed Brands
        Brand rayban = brandRepository.save(Brand.builder().name("Ray-Ban").description("Marque de lunettes mythique créée en 1937.").website("https://ray-ban.com").active(true).build());
        Brand oakley = brandRepository.save(Brand.builder().name("Oakley").description("Marque de sport et haute performance.").website("https://oakley.com").active(true).build());
        Brand gucci = brandRepository.save(Brand.builder().name("Gucci").description("Maison de haute couture italienne.").website("https://gucci.com").active(true).build());
        Brand tomford = brandRepository.save(Brand.builder().name("Tom Ford").description("Luxe contemporain et design audacieux.").website("https://tomford.com").active(true).build());
        Brand persol = brandRepository.save(Brand.builder().name("Persol").description("Artisanat italien d'exception.").website("https://persol.com").active(true).build());
        Brand bausch = brandRepository.save(Brand.builder().name("Bausch & Lomb").description("Leader mondial de la santé oculaire.").website("https://bausch.com").active(true).build());

        // 3. Seed Hierarchical Categories
        Category catLunettes = categoryRepository.save(Category.builder().name("Lunettes").description("Montures et équipement optique").active(true).build());
        Category catVue = categoryRepository.save(Category.builder().name("Lunettes de Vue").description("Montures optiques de vue").parent(catLunettes).active(true).build());
        Category catSoleil = categoryRepository.save(Category.builder().name("Lunettes de Soleil").description("Lunettes de protection solaire").parent(catLunettes).active(true).build());
        Category catEnfants = categoryRepository.save(Category.builder().name("Lunettes enfants").description("Lunettes adaptées aux plus jeunes").parent(catLunettes).active(true).build());
        Category catLentilles = categoryRepository.save(Category.builder().name("Lentilles de contact").description("Lentilles de contact et soins").active(true).build());

        // 4. Seed Promotions
        Promotion promo1 = Promotion.builder()
                .code("ETI2026")
                .title("Offre 2ème Paire à 1€")
                .description("Pour tout achat d'un équipement optique complet, votre 2ème paire offerte pour 1€ de plus.")
                .discountPercentage(20.0)
                .discountAmount(0.0)
                .startDate(LocalDate.now().minusDays(10))
                .endDate(LocalDate.now().plusMonths(2))
                .active(true)
                .categoryTarget("2EME_PAIRE")
                .build();

        Promotion promo2 = Promotion.builder()
                .code("SOLEIL15")
                .title("-15% sur les Lunettes de Soleil")
                .description("Profitez de 15% de remise immédiate sur l'ensemble de notre gamme solaire Ray-Ban et Oakley.")
                .discountPercentage(15.0)
                .discountAmount(0.0)
                .startDate(LocalDate.now().minusDays(5))
                .endDate(LocalDate.now().plusMonths(1))
                .active(true)
                .categoryTarget("LUNETTES_SOLEIL")
                .build();

        promotionRepository.saveAll(List.of(promo1, promo2));

        // 5. Seed Products (MODÈLES COMMERCIAUX) — sans aucune donnée de déclinaison
        Product p1 = Product.builder()
                .reference("RB-3025-001")
                .name("Ray-Ban Aviator Classic")
                .brandEntity(rayban)
                .legacyBrand("Ray-Ban")
                .categoryEntity(catSoleil)
                .legacyCategory("LUNETTES_SOLEIL")
                .productType(ProductType.SUNGLASSES)
                .gender(Gender.MIXTE)
                .targetAge(TargetAge.ADULT)
                .frameShape(FrameShape.AVIATEUR)
                .material("METAL")
                .imageUrl("https://images.unsplash.com/photo-1572635196237-14b3f281503f?w=600&auto=format&fit=crop&q=80")
                .virtualTryOnEnabled(true)
                .description("Monture iconique en goutte d'eau créée à l'origine pour les pilotes américains en 1937.")
                .build();

        Product p2 = Product.builder()
                .reference("OO-9102-01")
                .name("Oakley Holbrook Stealth")
                .brandEntity(oakley)
                .legacyBrand("Oakley")
                .categoryEntity(catSoleil)
                .legacyCategory("LUNETTES_SOLEIL")
                .productType(ProductType.SUNGLASSES)
                .gender(Gender.HOMME)
                .targetAge(TargetAge.ADULT)
                .frameShape(FrameShape.CARRE)
                .material("ACETATE")
                .imageUrl("https://images.unsplash.com/photo-1511499767150-a48a237f0083?w=600&auto=format&fit=crop&q=80")
                .virtualTryOnEnabled(true)
                .description("Style intemporel et moderne combiné à la technologie de verre de haute performance Prizm.")
                .build();

        Product p3 = Product.builder()
                .reference("GG-0123-002")
                .name("Gucci Elegance Titanium")
                .brandEntity(gucci)
                .legacyBrand("Gucci")
                .categoryEntity(catVue)
                .legacyCategory("LUNETTES_VUE")
                .productType(ProductType.OPTICAL_FRAME)
                .gender(Gender.FEMME)
                .targetAge(TargetAge.ADULT)
                .frameShape(FrameShape.OVALE)
                .material("TITANE")
                .imageUrl("https://images.unsplash.com/photo-1577803645773-f96470509666?w=600&auto=format&fit=crop&q=80")
                .virtualTryOnEnabled(true)
                .description("Monture optique d'exception ultra-légère en titane avec gravures artisanales sur les branches.")
                .build();

        Product p4 = Product.builder()
                .reference("TF-5542-001")
                .name("Tom Ford Wayfarer Modern")
                .brandEntity(tomford)
                .legacyBrand("Tom Ford")
                .categoryEntity(catVue)
                .legacyCategory("LUNETTES_VUE")
                .productType(ProductType.OPTICAL_FRAME)
                .gender(Gender.MIXTE)
                .targetAge(TargetAge.ADULT)
                .frameShape(FrameShape.RECTANGULAIRE)
                .material("ACETATE")
                .imageUrl("https://images.unsplash.com/photo-1591076482161-42ce6da69f67?w=600&auto=format&fit=crop&q=80")
                .virtualTryOnEnabled(true)
                .description("Silhouette classique retravaillée avec la charnière signature 'T' en métal brillant.")
                .build();

        Product p5 = Product.builder()
                .reference("PO-0649-24")
                .name("Persol Cellor Original")
                .brandEntity(persol)
                .legacyBrand("Persol")
                .categoryEntity(catVue)
                .legacyCategory("LUNETTES_VUE")
                .productType(ProductType.OPTICAL_FRAME)
                .gender(Gender.HOMME)
                .targetAge(TargetAge.ADULT)
                .frameShape(FrameShape.PANTOS)
                .material("ACETATE")
                .imageUrl("https://images.unsplash.com/photo-1508296695146-257a814070b4?w=600&auto=format&fit=crop&q=80")
                .virtualTryOnEnabled(true)
                .description("Fabrication italienne à la main, système flexible Meflecto pour un confort inégalé.")
                .build();

        Product p6 = Product.builder()
                .reference("BL-BIOTRUE-30")
                .name("BioTrue Monthly Precision")
                .brandEntity(bausch)
                .legacyBrand("Bausch & Lomb")
                .categoryEntity(catLentilles)
                .legacyCategory("LENTILLES")
                .productType(ProductType.CONTACT_LENSES)
                .gender(Gender.MIXTE)
                .targetAge(TargetAge.ALL)
                .frameShape(FrameShape.AUTRE)
                .imageUrl("https://images.unsplash.com/photo-1584308666744-24d5c474f2ae?w=600&auto=format&fit=crop&q=80")
                .virtualTryOnEnabled(false)
                .description("Lentilles de contact mensuelles à très haute hydratation inspirée de la biologie de l'œil.")
                .build();

        productRepository.saveAll(List.of(p1, p2, p3, p4, p5, p6));

        // Variantes vendables (SKU, barcode, couleur, prix) — données de déclinaison
        ProductVariant v1 = ProductVariant.builder().product(p1).sku("RB-3025-001-GOLD").barcode("805289005398").color("Or / Vert").size("58-14-135").purchasePrice(new BigDecimal("90.00")).sellingPrice(new BigDecimal("165.00")).active(true).build();
        ProductVariant v2 = ProductVariant.builder().product(p1).sku("RB-3025-002-BLACK").barcode("805289005399").color("Noir Mat / G15").size("58-14-135").purchasePrice(new BigDecimal("90.00")).sellingPrice(new BigDecimal("165.00")).active(true).build();
        ProductVariant v3 = ProductVariant.builder().product(p1).sku("RB-3025-003-GREEN").barcode("805289005400").color("Vert / Marron G15").size("58-14-135").purchasePrice(new BigDecimal("90.00")).sellingPrice(new BigDecimal("175.00")).active(true).build();
        ProductVariant v4 = ProductVariant.builder().product(p2).sku("OO-9102-01-BLK").barcode("888392001122").color("Noir Mat / Prizm Black").size("55-18-137").purchasePrice(new BigDecimal("75.00")).sellingPrice(new BigDecimal("142.00")).active(true).build();
        variantRepository.saveAll(List.of(v1, v2, v3, v4));

        // Stocks réels par variante et par magasin (table stocks = source de vérité)
        stockRepository.saveAll(List.of(
                Stock.builder().productVariant(v1).store(s1).quantity(10).reservedQuantity(2).minimumStock(5).maximumStock(60).reorderPoint(8).build(),
                Stock.builder().productVariant(v1).store(s2).quantity(4).reservedQuantity(0).minimumStock(3).maximumStock(40).reorderPoint(5).build(),
                Stock.builder().productVariant(v1).store(s3).quantity(2).reservedQuantity(0).minimumStock(2).maximumStock(30).reorderPoint(4).build(),
                Stock.builder().productVariant(v2).store(s1).quantity(5).reservedQuantity(0).minimumStock(4).maximumStock(40).reorderPoint(6).build(),
                Stock.builder().productVariant(v2).store(s2).quantity(1).reservedQuantity(0).minimumStock(2).maximumStock(20).reorderPoint(3).build(),
                Stock.builder().productVariant(v2).store(s3).quantity(0).reservedQuantity(0).minimumStock(2).maximumStock(20).reorderPoint(3).build(),
                Stock.builder().productVariant(v3).store(s1).quantity(8).reservedQuantity(1).minimumStock(5).maximumStock(50).reorderPoint(7).build(),
                Stock.builder().productVariant(v3).store(s2).quantity(3).reservedQuantity(0).minimumStock(3).maximumStock(30).reorderPoint(4).build(),
                Stock.builder().productVariant(v3).store(s3).quantity(5).reservedQuantity(0).minimumStock(3).maximumStock(30).reorderPoint(4).build(),
                Stock.builder().productVariant(v4).store(s1).quantity(9).reservedQuantity(1).minimumStock(4).maximumStock(50).reorderPoint(6).build(),
                Stock.builder().productVariant(v4).store(s2).quantity(2).reservedQuantity(0).minimumStock(3).maximumStock(30).reorderPoint(4).build(),
                Stock.builder().productVariant(v4).store(s3).quantity(1).reservedQuantity(0).minimumStock(2).maximumStock(20).reorderPoint(3).build()
        ));

        // 6. Seed Patients
        Patient pat1 = Patient.builder()
                .firstName("Sophie")
                .lastName("Martin")
                .email("sophie.martin@example.com")
                .phone("06 12 34 56 78")
                .dateOfBirth(LocalDate.of(1992, 5, 14))
                .address("15 Rue de la Paix")
                .city("Paris")
                .notes("Porte des lentilles en journée, recherche monture titane anti-fatigue.")
                .build();

        Patient pat2 = Patient.builder()
                .firstName("Thomas")
                .lastName("Dubois")
                .email("thomas.dubois@example.com")
                .phone("06 98 76 54 32")
                .dateOfBirth(LocalDate.of(1985, 11, 3))
                .address("8 Place Bellecour")
                .city("Lyon")
                .notes("Sensible à la lumière bleue (travaille sur écrans 10h/jour).")
                .build();

        patientRepository.saveAll(List.of(pat1, pat2));

        // 7. Seed Prescriptions
        Prescription pres1 = Prescription.builder()
                .patientId(pat1.getId())
                .patientName("Sophie Martin")
                .prescriberName("Dr. Antoine Moreau (Ophtalmologiste)")
                .prescriptionDate(LocalDate.now().minusMonths(2))
                .odSphere(-2.25)
                .odCylinder(-0.50)
                .odAxis(90)
                .odAddition(0.00)
                .ogSphere(-2.50)
                .ogCylinder(-0.75)
                .ogAxis(85)
                .ogAddition(0.00)
                .pupillaryDistance(62.5)
                .fileUrl("https://example.com/ordonnance-sophie-martin.pdf")
                .notes("Traitement anti-reflet haut de gamme recommandé.")
                .build();

        prescriptionRepository.save(pres1);

        // 8. Seed Appointments
        Appointment app1 = Appointment.builder()
                .clientName("Sophie Martin")
                .clientEmail("sophie.martin@example.com")
                .clientPhone("06 12 34 56 78")
                .appointmentDateTime(LocalDateTime.now().plusDays(2).withHour(14).withMinute(30))
                .serviceType("EXAMEN_VUE")
                .storeLocation("Optique Moderne - Paris Opéra")
                .status("CONFIRME")
                .notes("Vérification annuelle de la vue et renouvellement.")
                .build();

        Appointment app2 = Appointment.builder()
                .clientName("Marc Moreau")
                .clientEmail("marc.moreau@example.com")
                .clientPhone("07 44 55 66 77")
                .appointmentDateTime(LocalDateTime.now().plusDays(3).withHour(10).withMinute(0))
                .serviceType("ESSAYAGE")
                .storeLocation("Optique Moderne - Lyon Presqu'île")
                .status("EN_ATTENTE")
                .notes("Souhaite essayer la collection Tom Ford et Ray-Ban.")
                .build();

        appointmentRepository.saveAll(List.of(app1, app2));

        // 9. Seed Order
        OrderItem item1 = OrderItem.builder()
                .productId(p3.getId())
                .productName("Gucci Elegance Titanium")
                .productCategory("LUNETTES_VUE")
                .unitPrice(290.00)
                .quantity(1)
                .lensType("Anti-lumière bleue Premium")
                .build();

        Order order1 = Order.builder()
                .orderReference("OPT-2026-8841")
                .orderDate(LocalDateTime.now().minusDays(1))
                .clientName("Sophie Martin")
                .clientEmail("sophie.martin@example.com")
                .clientPhone("06 12 34 56 78")
                .shippingAddress("15 Rue de la Paix, 75002 Paris")
                .totalAmount(365.00)
                .status("EN_PREPARATION")
                .prescriptionId(pres1.getId())
                .items(List.of(item1))
                .build();

        orderRepository.save(order1);

        // 10. Seed Notifications
        NotificationLog notif1 = NotificationLog.builder()
                .recipient("sophie.martin@example.com")
                .channel("EMAIL")
                .subject("Confirmation de votre rendez-vous chez Optique Moderne")
                .message("Bonjour Sophie, votre RDV du Examen de vue est confirmé pour le " + app1.getAppointmentDateTime())
                .sentAt(LocalDateTime.now().minusHours(4))
                .status("SENT")
                .build();

        NotificationLog notif2 = NotificationLog.builder()
                .recipient("0612345678")
                .channel("WHATSAPP")
                .subject("Suivi de commande OPT-2026-8841")
                .message("Optique Moderne: Votre commande de lunettes est passée en cours de préparation en atelier.")
                .sentAt(LocalDateTime.now().minusHours(2))
                .status("DELIVERED")
                .build();

        notificationRepository.saveAll(List.of(notif1, notif2));

        System.out.println(">>> Données de démonstration OptiVision Produit initialisées avec succès ! <<<");
    }

    private void createBootstrapAdmin() {
        userAccountRepository.findByEmailIgnoreCase("admin@optivision.tn").ifPresentOrElse(
            admin -> {
                admin.setPasswordHash(passwordEncoder.encode("admin123"));
                admin.setRole(UserRole.ADMIN);
                admin.setActive(true);
                userAccountRepository.save(admin);
            },
            () -> {
                userAccountRepository.save(UserAccount.builder()
                        .fullName("Direction OptiVision Admin")
                        .email("admin@optivision.tn")
                        .passwordHash(passwordEncoder.encode("admin123"))
                        .role(UserRole.ADMIN)
                        .active(true)
                        .build());
            }
        );

        userAccountRepository.findByEmailIgnoreCase("client@optivision.tn").ifPresentOrElse(
            client -> {
                client.setPasswordHash(passwordEncoder.encode("client123"));
                client.setRole(UserRole.CLIENT);
                client.setActive(true);
                userAccountRepository.save(client);
            },
            () -> {
                userAccountRepository.save(UserAccount.builder()
                        .fullName("Sophie Martin")
                        .email("client@optivision.tn")
                        .passwordHash(passwordEncoder.encode("client123"))
                        .role(UserRole.CLIENT)
                        .active(true)
                        .build());
            }
        );
    }
}
