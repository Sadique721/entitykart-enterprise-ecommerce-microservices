package com.entitykart.productservice.config;

import com.entitykart.productservice.entity.CategoryEntity;
import com.entitykart.productservice.entity.ProductEntity;
import com.entitykart.productservice.entity.SubCategoryEntity;
import com.entitykart.productservice.repository.CategoryRepository;
import com.entitykart.productservice.repository.ProductRepository;
import com.entitykart.productservice.repository.SubCategoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class DatabaseSeeder implements CommandLineRunner {

    private final CategoryRepository categoryRepository;
    private final SubCategoryRepository subCategoryRepository;
    private final ProductRepository productRepository;

    @Override
    public void run(String... args) throws Exception {
        if (categoryRepository.count() == 0) {
            seedCategoriesAndProducts();
        } else {
            log.info("Database already seeded with categories.");
        }
    }

    private void seedCategoriesAndProducts() {
        log.info("Seeding categories, subcategories and products...");
        
        // 12 Premium Categories
        String[] catNames = {
            "Electronics", "Home & Office", "Apparel & Wear", "Sports & Fitness",
            "Books & Stationary", "Beauty & Grooming", "Automotive", "Toys & Games",
            "Health & Wellness", "Kitchen & Dining", "Pet Supplies", "Tools & Garden"
        };
        
        // Subcategories for each category
        String[][] subCatNames = {
            {"Mobile Phones", "Laptops & Computers", "Audio & Headphones"},
            {"Smart Lighting", "Office Furnishings", "Desk Organizers"},
            {"Men's Fashion", "Women's Fashion", "Premium Watches"},
            {"Yoga & Cardio", "Strength Training", "Outdoor Gear"},
            {"Business & Finance", "Fiction Novels", "Luxury Notebooks"},
            {"Skincare", "Hair Care", "Fragrances"},
            {"Car Electronics", "Car Care & Cleaning", "Interior Accessories"},
            {"Board Games", "Action Figures", "Educational Toys"},
            {"Supplements", "Fitness Trackers", "Wellness Teas"},
            {"Cookware", "Coffee & Tea Makers", "Tableware"},
            {"Dog Food & Treats", "Cat Toys", "Pet Grooming"},
            {"Power Tools", "Garden Decor", "Patio Furniture"}
        };

        List<CategoryEntity> savedCats = new ArrayList<>();
        List<SubCategoryEntity> savedSubs = new ArrayList<>();

        for (int i = 0; i < catNames.length; i++) {
            CategoryEntity cat = new CategoryEntity();
            cat.setCategoryName(catNames[i]);
            cat.setActive(true);
            cat.setCreatedAt(LocalDateTime.now());
            cat = categoryRepository.save(cat);
            savedCats.add(cat);

            for (String subName : subCatNames[i]) {
                SubCategoryEntity sub = new SubCategoryEntity();
                sub.setChildCategory(subName);
                sub.setCategoryId(cat.getCategoryId());
                sub.setActive(true);
                sub = subCategoryRepository.save(sub);
                savedSubs.add(sub);
            }
        }
        
        log.info("Seeded {} categories and {} subcategories.", savedCats.size(), savedSubs.size());

        // Now seed 1000 products
        List<ProductEntity> products = new ArrayList<>();
        
        String[] brands = {
            "AeroTech", "AuraSound", "WoodSmith", "Horology", "NomadGear", 
            "Lumina", "ApexLeather", "BioFit", "NovaLight", "CraftCore", 
            "VeloCity", "OptiStream", "Summit", "Pulse", "HydroFlow", "EcoGroove"
        };

        String[] adjectives = {
            "Pro", "Wireless", "Premium", "Minimalist", "Classic", 
            "Smart", "Eco-Friendly", "Ergonomic", "Ultra-Quiet", "High-Precision", 
            "Heavy-Duty", "Waterproof", "Rechargeable", "Vintage", "Modern", "Sleek"
        };

        String[] unsplashImages = {
            "https://images.unsplash.com/photo-1615663245857-ac93bb7c39e7?w=500", // mouse
            "https://images.unsplash.com/photo-1505740420928-5e560c06d30e?w=500", // headphones
            "https://images.unsplash.com/photo-1587829741301-dc798b83add3?w=500", // keyboard
            "https://images.unsplash.com/photo-1523275335684-37898b6baf30?w=500", // watch
            "https://images.unsplash.com/photo-1507473885765-e6ed057f782c?w=500", // lamp
            "https://images.unsplash.com/photo-1553062407-98eeb64c6a62?w=500", // backpack
            "https://images.unsplash.com/photo-1627124765138-04f3f1764bc5?w=500", // wallet
            "https://images.unsplash.com/photo-1608043152269-423dbba4e7e1?w=500", // speaker
            "https://images.unsplash.com/photo-1514432324607-a09d9b4aefdd?w=500", // mug
            "https://images.unsplash.com/photo-1516035069371-29a1b244cc32?w=500", // camera
            "https://images.unsplash.com/photo-1485955900006-10f4d324d411?w=500", // plant
            "https://images.unsplash.com/photo-1541643600914-78b084683601?w=500", // fragrance
            "https://images.unsplash.com/photo-1511499767150-a48a237f0083?w=500", // sunglasses
            "https://images.unsplash.com/photo-1531346878377-a5be20888e57?w=500", // notebook
            "https://images.unsplash.com/photo-1632292224971-0d45778b3617?w=500", // desk mat
            "https://images.unsplash.com/photo-1583863788434-e58a36330cf0?w=500", // charger
            "https://images.unsplash.com/photo-1527443224154-c4a3942d3acf?w=500", // stand
            "https://images.unsplash.com/photo-1602143407151-7111542de6e8?w=500", // bottle
            "https://images.unsplash.com/photo-1518455027359-f3f8164ba6bd?w=500", // desk
            "https://images.unsplash.com/photo-1506439773649-6e0eb8cfb237?w=500"  // chair
        };

        for (int i = 0; i < 1000; i++) {
            CategoryEntity cat = savedCats.get(i % savedCats.size());
            final Long catId = cat.getCategoryId();
            List<SubCategoryEntity> catSubs = savedSubs.stream()
                .filter(s -> s.getCategoryId().equals(catId))
                .toList();
            SubCategoryEntity sub = catSubs.get(i % catSubs.size());

            ProductEntity product = new ProductEntity();
            String brand = brands[i % brands.length];
            String adj = adjectives[i % adjectives.length];
            String catName = cat.getCategoryName();
            String subName = sub.getChildCategory();
            
            // Build a descriptive name
            String prodName = brand + " " + adj + " " + subName.replaceAll("s$", "") + " " + (100 + i);
            product.setProductName(prodName);
            product.setDescription("Experience the ultimate in quality with the " + prodName + ". Designed for premium performance, it brings top-tier innovation and sleek style to the " + catName + " category. Features a modern aesthetic, reliable durability, and cutting-edge specifications ideal for daily use.");
            product.setBrand(brand);
            
            // Prices: range from 299 to 49999
            double basePrice = 299 + (i * 49.7) % 45000;
            double mrpVal = basePrice * (1.15 + (i * 0.03) % 0.25);
            
            product.setPrice(BigDecimal.valueOf(Math.round(basePrice)));
            product.setMrp(BigDecimal.valueOf(Math.round(mrpVal)));
            product.setStockQuantity(10 + (i * 7) % 140);
            product.setSku("SKU-" + cat.getCategoryId() + "-" + sub.getSubCategoryId() + "-" + String.format("%04d", i));
            product.setMainImageURL(unsplashImages[i % unsplashImages.length]);
            product.setCategoryId(cat.getCategoryId());
            product.setSubCategoryId(sub.getSubCategoryId());
            product.setSellerId(10L);
            product.setCreatedAt(LocalDateTime.now());
            product.setStatus("Available");
            
            products.add(product);

            // Batch save in chunks of 100
            if (products.size() >= 100) {
                productRepository.saveAll(products);
                products.clear();
            }
        }
        
        if (!products.isEmpty()) {
            productRepository.saveAll(products);
        }

        log.info("Successfully seeded 1000 products.");
    }
}
