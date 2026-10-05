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
    private final VirtualTryOnAssetRepository tryOnAssetRepository;
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

        // 5. Seed Products (30 MODÈLES OFFICIELS - 5 PAR MARQUE)
        // --- RAY-BAN (5 Modèles) ---
        Product p1 = Product.builder().reference("RB-3025-001").name("Ray-Ban Aviator Classic").brandEntity(rayban).legacyBrand("Ray-Ban").categoryEntity(catSoleil).legacyCategory("LUNETTES_SOLEIL").productType(ProductType.SUNGLASSES).gender(Gender.MIXTE).targetAge(TargetAge.ADULT).frameShape(FrameShape.AVIATEUR).material("METAL").imageUrl("https://images.unsplash.com/photo-1572635196237-14b3f281503f?w=600&auto=format&fit=crop&q=80").virtualTryOnEnabled(true).tryOn3dAvailable(true).model3dUrl("/uploads/models/eyewear_3d_p1.glb").description("Monture iconique en goutte d'eau créée à l'origine pour les pilotes américains en 1937.").build();
        Product p_rb2 = Product.builder().reference("RB-2140-001").name("Ray-Ban Wayfarer Classic").brandEntity(rayban).legacyBrand("Ray-Ban").categoryEntity(catSoleil).legacyCategory("LUNETTES_SOLEIL").productType(ProductType.SUNGLASSES).gender(Gender.MIXTE).targetAge(TargetAge.ADULT).frameShape(FrameShape.CARRE).material("ACETATE").imageUrl("https://images.unsplash.com/photo-1511499767150-a48a237f0083?w=600&auto=format&fit=crop&q=80").virtualTryOnEnabled(true).tryOn3dAvailable(true).model3dUrl("/uploads/models/eyewear_3d_p1.glb").description("Le modèle le plus reconnaissable de l'histoire des lunettes de soleil.").build();
        Product p_rb3 = Product.builder().reference("RB-3016-001").name("Ray-Ban Clubmaster Classic").brandEntity(rayban).legacyBrand("Ray-Ban").categoryEntity(catSoleil).legacyCategory("LUNETTES_SOLEIL").productType(ProductType.SUNGLASSES).gender(Gender.MIXTE).targetAge(TargetAge.ADULT).frameShape(FrameShape.PANTOS).material("ACETATE").imageUrl("https://images.unsplash.com/photo-1577803645773-f96470509666?w=600&auto=format&fit=crop&q=80").virtualTryOnEnabled(true).tryOn3dAvailable(true).model3dUrl("/uploads/models/eyewear_3d_p1.glb").description("Inspirées des années 50, portées par les intellectuels et les pionniers du style.").build();
        Product p_rb4 = Product.builder().reference("RB-4165-001").name("Ray-Ban Justin Color Mix").brandEntity(rayban).legacyBrand("Ray-Ban").categoryEntity(catSoleil).legacyCategory("LUNETTES_SOLEIL").productType(ProductType.SUNGLASSES).gender(Gender.HOMME).targetAge(TargetAge.ADULT).frameShape(FrameShape.RECTANGULAIRE).material("ACETATE").imageUrl("https://images.unsplash.com/photo-1591076482161-42ce6da69f67?w=600&auto=format&fit=crop&q=80").virtualTryOnEnabled(true).tryOn3dAvailable(true).model3dUrl("/uploads/models/eyewear_3d_p1.glb").description("Finition caoutchoutée audacieuse et verres dégradés modernes.").build();
        Product p_rb5 = Product.builder().reference("RX-3447V-001").name("Ray-Ban Round Metal Optique").brandEntity(rayban).legacyBrand("Ray-Ban").categoryEntity(catVue).legacyCategory("LUNETTES_VUE").productType(ProductType.OPTICAL_FRAME).gender(Gender.MIXTE).targetAge(TargetAge.ADULT).frameShape(FrameShape.ROND).material("METAL").imageUrl("https://images.unsplash.com/photo-1508296695146-257a814070b4?w=600&auto=format&fit=crop&q=80").virtualTryOnEnabled(true).tryOn3dAvailable(true).model3dUrl("/uploads/models/eyewear_3d_p1.glb").description("Monture rétro et épurée en métal fin avec branches gravées.").build();

        // --- OAKLEY (5 Modèles) ---
        Product p2 = Product.builder().reference("OO-9102-01").name("Oakley Holbrook Stealth").brandEntity(oakley).legacyBrand("Oakley").categoryEntity(catSoleil).legacyCategory("LUNETTES_SOLEIL").productType(ProductType.SUNGLASSES).gender(Gender.HOMME).targetAge(TargetAge.ADULT).frameShape(FrameShape.CARRE).material("ACETATE").imageUrl("https://images.unsplash.com/photo-1511499767150-a48a237f0083?w=600&auto=format&fit=crop&q=80").virtualTryOnEnabled(true).tryOn3dAvailable(true).model3dUrl("/uploads/models/eyewear_3d_p2.glb").description("Style intemporel et moderne combiné à la technologie de verre Prizm.").build();
        Product p_ok2 = Product.builder().reference("OO-9013-01").name("Oakley Frogskins Classic").brandEntity(oakley).legacyBrand("Oakley").categoryEntity(catSoleil).legacyCategory("LUNETTES_SOLEIL").productType(ProductType.SUNGLASSES).gender(Gender.MIXTE).targetAge(TargetAge.ADULT).frameShape(FrameShape.CARRE).material("ACETATE").imageUrl("https://images.unsplash.com/photo-1572635196237-14b3f281503f?w=600&auto=format&fit=crop&q=80").virtualTryOnEnabled(true).tryOn3dAvailable(true).model3dUrl("/uploads/models/eyewear_3d_p2.glb").description("Le pop art des années 80 réinventé avec la précision optique Oakley.").build();
        Product p_ok3 = Product.builder().reference("OO-9014-01").name("Oakley Gascan Tactical").brandEntity(oakley).legacyBrand("Oakley").categoryEntity(catSoleil).legacyCategory("LUNETTES_SOLEIL").productType(ProductType.SUNGLASSES).gender(Gender.HOMME).targetAge(TargetAge.ADULT).frameShape(FrameShape.RECTANGULAIRE).material("ACETATE").imageUrl("https://images.unsplash.com/photo-1591076482161-42ce6da69f67?w=600&auto=format&fit=crop&q=80").virtualTryOnEnabled(true).tryOn3dAvailable(true).model3dUrl("/uploads/models/eyewear_3d_p2.glb").description("Lignes épurées et angulaires gravées dans le matériau résistant O Matter.").build();
        Product p_ok4 = Product.builder().reference("OO-9208-01").name("Oakley Radar EV Path Sport").brandEntity(oakley).legacyBrand("Oakley").categoryEntity(catSoleil).legacyCategory("LUNETTES_SOLEIL").productType(ProductType.SUNGLASSES).gender(Gender.HOMME).targetAge(TargetAge.ADULT).frameShape(FrameShape.AUTRE).material("PLASTIQUE").imageUrl("https://images.unsplash.com/photo-1577803645773-f96470509666?w=600&auto=format&fit=crop&q=80").virtualTryOnEnabled(true).tryOn3dAvailable(true).model3dUrl("/uploads/models/eyewear_3d_p2.glb").description("Champ de vision étendu vers le haut pour une performance sportive ultime.").build();
        Product p_ok5 = Product.builder().reference("OX-8105-01").name("Oakley Pitchman R Optique").brandEntity(oakley).legacyBrand("Oakley").categoryEntity(catVue).legacyCategory("LUNETTES_VUE").productType(ProductType.OPTICAL_FRAME).gender(Gender.HOMME).targetAge(TargetAge.ADULT).frameShape(FrameShape.ROND).material("ACETATE").imageUrl("https://images.unsplash.com/photo-1508296695146-257a814070b4?w=600&auto=format&fit=crop&q=80").virtualTryOnEnabled(true).tryOn3dAvailable(true).model3dUrl("/uploads/models/eyewear_3d_p2.glb").description("Charnières sans vis Hollowpoint et branches fines en acier inoxydable.").build();

        // --- GUCCI (5 Modèles) ---
        Product p3 = Product.builder().reference("GG-0123-002").name("Gucci Elegance Titanium").brandEntity(gucci).legacyBrand("Gucci").categoryEntity(catVue).legacyCategory("LUNETTES_VUE").productType(ProductType.OPTICAL_FRAME).gender(Gender.FEMME).targetAge(TargetAge.ADULT).frameShape(FrameShape.OVALE).material("TITANE").imageUrl("https://images.unsplash.com/photo-1577803645773-f96470509666?w=600&auto=format&fit=crop&q=80").virtualTryOnEnabled(true).tryOn3dAvailable(true).model3dUrl("/uploads/models/eyewear_3d_p3.glb").description("Monture optique ultra-légère en titane avec gravures artisanales.").build();
        Product p_gc2 = Product.builder().reference("GG-0608OK-001").name("Gucci Square Acetate Optique").brandEntity(gucci).legacyBrand("Gucci").categoryEntity(catVue).legacyCategory("LUNETTES_VUE").productType(ProductType.OPTICAL_FRAME).gender(Gender.HOMME).targetAge(TargetAge.ADULT).frameShape(FrameShape.CARRE).material("ACETATE").imageUrl("https://images.unsplash.com/photo-1591076482161-42ce6da69f67?w=600&auto=format&fit=crop&q=80").virtualTryOnEnabled(true).tryOn3dAvailable(true).model3dUrl("/uploads/models/eyewear_3d_p3.glb").description("Design carré sophistiqué rehaussé de la bande Web emblématique.").build();
        Product p_gc3 = Product.builder().reference("GG-0208S-001").name("Gucci Cat Eye Vintage").brandEntity(gucci).legacyBrand("Gucci").categoryEntity(catSoleil).legacyCategory("LUNETTES_SOLEIL").productType(ProductType.SUNGLASSES).gender(Gender.FEMME).targetAge(TargetAge.ADULT).frameShape(FrameShape.PAPILLON).material("ACETATE").imageUrl("https://images.unsplash.com/photo-1572635196237-14b3f281503f?w=600&auto=format&fit=crop&q=80").virtualTryOnEnabled(true).tryOn3dAvailable(true).model3dUrl("/uploads/models/eyewear_3d_p3.glb").description("Silhouette œil-de-chat rétro avec logo GG entrelacé doré.").build();
        Product p_gc4 = Product.builder().reference("GG-0528S-001").name("Gucci Oversized Pilot").brandEntity(gucci).legacyBrand("Gucci").categoryEntity(catSoleil).legacyCategory("LUNETTES_SOLEIL").productType(ProductType.SUNGLASSES).gender(Gender.MIXTE).targetAge(TargetAge.ADULT).frameShape(FrameShape.AVIATEUR).material("METAL").imageUrl("https://images.unsplash.com/photo-1511499767150-a48a237f0083?w=600&auto=format&fit=crop&q=80").virtualTryOnEnabled(true).tryOn3dAvailable(true).model3dUrl("/uploads/models/eyewear_3d_p3.glb").description("Lunettes pilote oversize avec double pont et finitions dorées.").build();
        Product p_gc5 = Product.builder().reference("GG-0396O-001").name("Gucci Rimless Executive Optique").brandEntity(gucci).legacyBrand("Gucci").categoryEntity(catVue).legacyCategory("LUNETTES_VUE").productType(ProductType.OPTICAL_FRAME).gender(Gender.HOMME).targetAge(TargetAge.ADULT).frameShape(FrameShape.RECTANGULAIRE).material("TITANE").imageUrl("https://images.unsplash.com/photo-1508296695146-257a814070b4?w=600&auto=format&fit=crop&q=80").virtualTryOnEnabled(true).tryOn3dAvailable(true).model3dUrl("/uploads/models/eyewear_3d_p3.glb").description("Monture percée haut de gamme pour un look discret et luxueux.").build();

        // --- TOM FORD (5 Modèles) ---
        Product p4 = Product.builder().reference("TF-5542-001").name("Tom Ford Wayfarer Modern").brandEntity(tomford).legacyBrand("Tom Ford").categoryEntity(catVue).legacyCategory("LUNETTES_VUE").productType(ProductType.OPTICAL_FRAME).gender(Gender.MIXTE).targetAge(TargetAge.ADULT).frameShape(FrameShape.RECTANGULAIRE).material("ACETATE").imageUrl("https://images.unsplash.com/photo-1591076482161-42ce6da69f67?w=600&auto=format&fit=crop&q=80").virtualTryOnEnabled(true).tryOn3dAvailable(true).model3dUrl("/uploads/models/eyewear_3d_p4.glb").description("Silhouette classique avec la charnière signature 'T' en métal.").build();
        Product p_tf2 = Product.builder().reference("TF-0237-01A").name("Tom Ford Snowdon Sunglasses").brandEntity(tomford).legacyBrand("Tom Ford").categoryEntity(catSoleil).legacyCategory("LUNETTES_SOLEIL").productType(ProductType.SUNGLASSES).gender(Gender.HOMME).targetAge(TargetAge.ADULT).frameShape(FrameShape.CARRE).material("ACETATE").imageUrl("https://images.unsplash.com/photo-1511499767150-a48a237f0083?w=600&auto=format&fit=crop&q=80").virtualTryOnEnabled(true).tryOn3dAvailable(true).model3dUrl("/uploads/models/eyewear_3d_p4.glb").description("Modèle mythique porté dans le film James Bond Spectre.").build();
        Product p_tf3 = Product.builder().reference("FT-5555B-001").name("Tom Ford Blue Block Optique").brandEntity(tomford).legacyBrand("Tom Ford").categoryEntity(catVue).legacyCategory("LUNETTES_VUE").productType(ProductType.OPTICAL_FRAME).gender(Gender.MIXTE).targetAge(TargetAge.ADULT).frameShape(FrameShape.PANTOS).material("ACETATE").imageUrl("https://images.unsplash.com/photo-1508296695146-257a814070b4?w=600&auto=format&fit=crop&q=80").virtualTryOnEnabled(true).tryOn3dAvailable(true).model3dUrl("/uploads/models/eyewear_3d_p4.glb").description("Monture équipée de verres anti-lumière bleue prêts à porter.").build();
        Product p_tf4 = Product.builder().reference("TF-0248-05N").name("Tom Ford Henry Clubmaster").brandEntity(tomford).legacyBrand("Tom Ford").categoryEntity(catSoleil).legacyCategory("LUNETTES_SOLEIL").productType(ProductType.SUNGLASSES).gender(Gender.HOMME).targetAge(TargetAge.ADULT).frameShape(FrameShape.PANTOS).material("METAL").imageUrl("https://images.unsplash.com/photo-1577803645773-f96470509666?w=600&auto=format&fit=crop&q=80").virtualTryOnEnabled(true).tryOn3dAvailable(true).model3dUrl("/uploads/models/eyewear_3d_p4.glb").description("Style vintage Browline revisité avec des détails métal dorés.").build();
        Product p_tf5 = Product.builder().reference("TF-0371-01B").name("Tom Ford Anoushka Cat Eye").brandEntity(tomford).legacyBrand("Tom Ford").categoryEntity(catSoleil).legacyCategory("LUNETTES_SOLEIL").productType(ProductType.SUNGLASSES).gender(Gender.FEMME).targetAge(TargetAge.ADULT).frameShape(FrameShape.PAPILLON).material("ACETATE").imageUrl("https://images.unsplash.com/photo-1572635196237-14b3f281503f?w=600&auto=format&fit=crop&q=80").virtualTryOnEnabled(true).tryOn3dAvailable(true).model3dUrl("/uploads/models/eyewear_3d_p4.glb").description("Forme papillon exagérée pour un style glamorous et affirmé.").build();

        // --- PERSOL (5 Modèles) ---
        Product p5 = Product.builder().reference("PO-0649-24").name("Persol Cellor Original").brandEntity(persol).legacyBrand("Persol").categoryEntity(catVue).legacyCategory("LUNETTES_VUE").productType(ProductType.OPTICAL_FRAME).gender(Gender.HOMME).targetAge(TargetAge.ADULT).frameShape(FrameShape.PANTOS).material("ACETATE").imageUrl("https://images.unsplash.com/photo-1508296695146-257a814070b4?w=600&auto=format&fit=crop&q=80").virtualTryOnEnabled(true).tryOn3dAvailable(true).model3dUrl("/uploads/models/eyewear_3d_p5.glb").description("Artisanat italien, système flexible Meflecto pour un confort inégalé.").build();
        Product p_ps2 = Product.builder().reference("PO-0714-SM").name("Persol 714 Steve McQueen Folding").brandEntity(persol).legacyBrand("Persol").categoryEntity(catSoleil).legacyCategory("LUNETTES_SOLEIL").productType(ProductType.SUNGLASSES).gender(Gender.HOMME).targetAge(TargetAge.ADULT).frameShape(FrameShape.AVIATEUR).material("ACETATE").imageUrl("https://images.unsplash.com/photo-1572635196237-14b3f281503f?w=600&auto=format&fit=crop&q=80").virtualTryOnEnabled(true).tryOn3dAvailable(true).model3dUrl("/uploads/models/eyewear_3d_p5.glb").description("Les premières lunettes pliables de l'histoire, devenues légendaires.").build();
        Product p_ps3 = Product.builder().reference("PO-3105V-24").name("Persol 3105V Optique").brandEntity(persol).legacyBrand("Persol").categoryEntity(catVue).legacyCategory("LUNETTES_VUE").productType(ProductType.OPTICAL_FRAME).gender(Gender.MIXTE).targetAge(TargetAge.ADULT).frameShape(FrameShape.PANTOS).material("ACETATE").imageUrl("https://images.unsplash.com/photo-1591076482161-42ce6da69f67?w=600&auto=format&fit=crop&q=80").virtualTryOnEnabled(true).tryOn3dAvailable(true).model3dUrl("/uploads/models/eyewear_3d_p5.glb").description("Monture pantos emblématique avec la flèche Supreme argentée.").build();
        Product p_ps4 = Product.builder().reference("PO-3166V-95").name("Persol Calligrapher Edition").brandEntity(persol).legacyBrand("Persol").categoryEntity(catVue).legacyCategory("LUNETTES_VUE").productType(ProductType.OPTICAL_FRAME).gender(Gender.HOMME).targetAge(TargetAge.ADULT).frameShape(FrameShape.ROND).material("METAL").imageUrl("https://images.unsplash.com/photo-1577803645773-f96470509666?w=600&auto=format&fit=crop&q=80").virtualTryOnEnabled(true).tryOn3dAvailable(true).model3dUrl("/uploads/models/eyewear_3d_p5.glb").description("Édition spéciale combinant acétate gravé et pont en métal travaillé.").build();
        Product p_ps5 = Product.builder().reference("PO-3225S-24").name("Persol Key West Sunglasses").brandEntity(persol).legacyBrand("Persol").categoryEntity(catSoleil).legacyCategory("LUNETTES_SOLEIL").productType(ProductType.SUNGLASSES).gender(Gender.HOMME).targetAge(TargetAge.ADULT).frameShape(FrameShape.RECTANGULAIRE).material("ACETATE").imageUrl("https://images.unsplash.com/photo-1511499767150-a48a237f0083?w=600&auto=format&fit=crop&q=80").virtualTryOnEnabled(true).tryOn3dAvailable(true).model3dUrl("/uploads/models/eyewear_3d_p5.glb").description("Esprit vintage des années 90 avec profil rectangulaire galbé.").build();

        // --- BAUSCH & LOMB (5 Modèles) ---
        Product p6 = Product.builder().reference("BL-BIOTRUE-30").name("BioTrue Monthly Precision").brandEntity(bausch).legacyBrand("Bausch & Lomb").categoryEntity(catLentilles).legacyCategory("LENTILLES").productType(ProductType.CONTACT_LENSES).gender(Gender.MIXTE).targetAge(TargetAge.ALL).frameShape(FrameShape.AUTRE).imageUrl("https://images.unsplash.com/photo-1584308666744-24d5c474f2ae?w=600&auto=format&fit=crop&q=80").virtualTryOnEnabled(false).description("Lentilles de contact mensuelles à très haute hydratation.").build();
        Product p_bl2 = Product.builder().reference("BL-PUREVIS-AST").name("PureVision 2 HD Astigmatism").brandEntity(bausch).legacyBrand("Bausch & Lomb").categoryEntity(catLentilles).legacyCategory("LENTILLES").productType(ProductType.CONTACT_LENSES).gender(Gender.MIXTE).targetAge(TargetAge.ALL).frameShape(FrameShape.AUTRE).imageUrl("https://images.unsplash.com/photo-1584308666744-24d5c474f2ae?w=600&auto=format&fit=crop&q=80").virtualTryOnEnabled(false).description("Lentilles toriques pour la correction de l'astigmatisme avec optique HD.").build();
        Product p_bl3 = Product.builder().reference("BL-SOFLENS-90").name("SofLens Daily Disposable").brandEntity(bausch).legacyBrand("Bausch & Lomb").categoryEntity(catLentilles).legacyCategory("LENTILLES").productType(ProductType.CONTACT_LENSES).gender(Gender.MIXTE).targetAge(TargetAge.ALL).frameShape(FrameShape.AUTRE).imageUrl("https://images.unsplash.com/photo-1584308666744-24d5c474f2ae?w=600&auto=format&fit=crop&q=80").virtualTryOnEnabled(false).description("Boîte de 90 lentilles journalières jetables ultra confortables.").build();
        Product p_bl4 = Product.builder().reference("BL-ULTRA-MULTI").name("Ultra MoistureSeal Multifocal").brandEntity(bausch).legacyBrand("Bausch & Lomb").categoryEntity(catLentilles).legacyCategory("LENTILLES").productType(ProductType.CONTACT_LENSES).gender(Gender.MIXTE).targetAge(TargetAge.ALL).frameShape(FrameShape.AUTRE).imageUrl("https://images.unsplash.com/photo-1584308666744-24d5c474f2ae?w=600&auto=format&fit=crop&q=80").virtualTryOnEnabled(false).description("Lentilles progressives mensuelles avec technologie d'hydratation 16 heures.").build();
        Product p_bl5 = Product.builder().reference("BL-CARE-250").name("Bausch & Lomb RGP Care Kit").brandEntity(bausch).legacyBrand("Bausch & Lomb").categoryEntity(catLentilles).legacyCategory("LENTILLES").productType(ProductType.CARE_PRODUCTS).gender(Gender.MIXTE).targetAge(TargetAge.ALL).frameShape(FrameShape.AUTRE).imageUrl("https://images.unsplash.com/photo-1584308666744-24d5c474f2ae?w=600&auto=format&fit=crop&q=80").virtualTryOnEnabled(false).description("Solution tout-en-un de décontamination et rinçage 250ml.").build();

        productRepository.saveAll(List.of(
            p1, p_rb2, p_rb3, p_rb4, p_rb5,
            p2, p_ok2, p_ok3, p_ok4, p_ok5,
            p3, p_gc2, p_gc3, p_gc4, p_gc5,
            p4, p_tf2, p_tf3, p_tf4, p_tf5,
            p5, p_ps2, p_ps3, p_ps4, p_ps5,
            p6, p_bl2, p_bl3, p_bl4, p_bl5
        ));

        // Variantes vendables (SKU, barcode, couleur, prix) — déclinaisons pour tests
        ProductVariant v1 = ProductVariant.builder().product(p1).sku("RB-3025-001-GOLD").barcode("805289005398").color("Or / Vert G15").size("58-14-135").purchasePrice(new BigDecimal("90.00")).sellingPrice(new BigDecimal("165.00")).active(true).build();
        ProductVariant v2 = ProductVariant.builder().product(p1).sku("RB-3025-002-BLACK").barcode("805289005399").color("Noir Mat / G15").size("58-14-135").purchasePrice(new BigDecimal("90.00")).sellingPrice(new BigDecimal("165.00")).active(true).build();
        ProductVariant v3 = ProductVariant.builder().product(p1).sku("RB-3025-003-GREEN").barcode("805289005400").color("Argent / Miroir").size("58-14-135").purchasePrice(new BigDecimal("95.00")).sellingPrice(new BigDecimal("175.00")).active(true).build();
        ProductVariant v4 = ProductVariant.builder().product(p2).sku("OO-9102-01-BLK").barcode("888392001122").color("Noir Mat / Prizm Black").size("55-18-137").purchasePrice(new BigDecimal("75.00")).sellingPrice(new BigDecimal("142.00")).active(true).build();
        ProductVariant v5 = ProductVariant.builder().product(p3).sku("GG-0123-002-GLD").barcode("805637600123").color("Doré / Vert").size("52-16-140").purchasePrice(new BigDecimal("180.00")).sellingPrice(new BigDecimal("340.00")).active(true).build();
        ProductVariant v6 = ProductVariant.builder().product(p4).sku("TF-5542-001-BLK").barcode("664689005542").color("Shiny Black T").size("53-17-145").purchasePrice(new BigDecimal("190.00")).sellingPrice(new BigDecimal("360.00")).active(true).build();
        ProductVariant v7 = ProductVariant.builder().product(p5).sku("PO-0649-24-HAV").barcode("805289000649").color("Havana / Green").size("54-20-140").purchasePrice(new BigDecimal("140.00")).sellingPrice(new BigDecimal("280.00")).active(true).build();
        
        variantRepository.saveAll(List.of(v1, v2, v3, v4, v5, v6, v7));

        // 3D Virtual Try-On Assets (100% Gratuit, Local, Statut PUBLISHED)
        VirtualTryOnAsset a1 = VirtualTryOnAsset.builder().variant(v1).modelUrl("/uploads/models/eyewear_3d_p1.glb").thumbnailUrl(p1.getImageUrl()).format("GLB").status(TryOnAssetStatus.PUBLISHED).version(1).scale(1.0).positionX(0.0).positionY(0.0).positionZ(0.0).rotationX(0.0).rotationY(0.0).rotationZ(0.0).build();
        VirtualTryOnAsset a2 = VirtualTryOnAsset.builder().variant(v2).modelUrl("/uploads/models/eyewear_3d_p1.glb").thumbnailUrl(p1.getImageUrl()).format("GLB").status(TryOnAssetStatus.PUBLISHED).version(1).scale(1.0).positionX(0.0).positionY(0.0).positionZ(0.0).rotationX(0.0).rotationY(0.0).rotationZ(0.0).build();
        VirtualTryOnAsset a3 = VirtualTryOnAsset.builder().variant(v3).modelUrl("/uploads/models/eyewear_3d_p1.glb").thumbnailUrl(p1.getImageUrl()).format("GLB").status(TryOnAssetStatus.PUBLISHED).version(1).scale(1.0).positionX(0.0).positionY(0.0).positionZ(0.0).rotationX(0.0).rotationY(0.0).rotationZ(0.0).build();
        VirtualTryOnAsset a4 = VirtualTryOnAsset.builder().variant(v4).modelUrl("/uploads/models/eyewear_3d_p2.glb").thumbnailUrl(p2.getImageUrl()).format("GLB").status(TryOnAssetStatus.PUBLISHED).version(1).scale(1.0).positionX(0.0).positionY(0.0).positionZ(0.0).rotationX(0.0).rotationY(0.0).rotationZ(0.0).build();
        VirtualTryOnAsset a5 = VirtualTryOnAsset.builder().variant(v5).modelUrl("/uploads/models/eyewear_3d_p3.glb").thumbnailUrl(p3.getImageUrl()).format("GLB").status(TryOnAssetStatus.PUBLISHED).version(1).scale(1.0).positionX(0.0).positionY(0.0).positionZ(0.0).rotationX(0.0).rotationY(0.0).rotationZ(0.0).build();
        VirtualTryOnAsset a6 = VirtualTryOnAsset.builder().variant(v6).modelUrl("/uploads/models/eyewear_3d_p4.glb").thumbnailUrl(p4.getImageUrl()).format("GLB").status(TryOnAssetStatus.PUBLISHED).version(1).scale(1.0).positionX(0.0).positionY(0.0).positionZ(0.0).rotationX(0.0).rotationY(0.0).rotationZ(0.0).build();
        VirtualTryOnAsset a7 = VirtualTryOnAsset.builder().variant(v7).modelUrl("/uploads/models/eyewear_3d_p5.glb").thumbnailUrl(p5.getImageUrl()).format("GLB").status(TryOnAssetStatus.PUBLISHED).version(1).scale(1.0).positionX(0.0).positionY(0.0).positionZ(0.0).rotationX(0.0).rotationY(0.0).rotationZ(0.0).build();
        tryOnAssetRepository.saveAll(List.of(a1, a2, a3, a4, a5, a6, a7));

        // Stocks réels par variante et par magasin (table stocks = source de vérité)
        stockRepository.saveAll(List.of(
                Stock.builder().productVariant(v1).store(s1).quantity(10).reservedQuantity(2).minimumStock(5).maximumStock(60).reorderPoint(8).build(),
                Stock.builder().productVariant(v1).store(s2).quantity(4).reservedQuantity(0).minimumStock(3).maximumStock(40).reorderPoint(5).build(),
                Stock.builder().productVariant(v1).store(s3).quantity(2).reservedQuantity(0).minimumStock(2).maximumStock(30).reorderPoint(4).build(),
                Stock.builder().productVariant(v2).store(s1).quantity(5).reservedQuantity(0).minimumStock(4).maximumStock(40).reorderPoint(6).build(),
                Stock.builder().productVariant(v2).store(s2).quantity(1).reservedQuantity(0).minimumStock(2).maximumStock(20).reorderPoint(3).build(),
                Stock.builder().productVariant(v3).store(s1).quantity(8).reservedQuantity(1).minimumStock(5).maximumStock(50).reorderPoint(7).build(),
                Stock.builder().productVariant(v4).store(s1).quantity(9).reservedQuantity(1).minimumStock(4).maximumStock(50).reorderPoint(6).build(),
                Stock.builder().productVariant(v5).store(s1).quantity(6).reservedQuantity(0).minimumStock(2).maximumStock(30).reorderPoint(4).build(),
                Stock.builder().productVariant(v6).store(s1).quantity(7).reservedQuantity(0).minimumStock(3).maximumStock(30).reorderPoint(5).build(),
                Stock.builder().productVariant(v7).store(s1).quantity(5).reservedQuantity(0).minimumStock(2).maximumStock(25).reorderPoint(4).build()
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
