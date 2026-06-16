/**
 * Product and Category Service
 */
app.service('productService', ['apiService', '$q', '$http', 'API_BASE', function(apiService, $q, $http, API_BASE) {
    
    // A robust list of premium mock products in case API is empty/fails
    var mockProducts = [
        {
            productId: 101,
            productName: 'AeroGlide Pro Wireless Mouse',
            description: 'Ergonomic gaming mouse with ultra-high precision tracking, RGB lighting, and up to 120 hours of battery life. Perfect for professional gamers and creators alike.',
            brand: 'AeroTech',
            price: 4999.00,
            mrp: 6999.00,
            stockQuantity: 15,
            sku: 'MS-AER-GLD-01',
            mainImageURL: 'https://images.unsplash.com/photo-1615663245857-ac93bb7c39e7?w=500&auto=format&fit=crop&q=60',
            categoryId: 1,
            subCategoryId: 11,
            sellerId: 10,
            discountPercent: 28
        },
        {
            productId: 102,
            productName: 'SoundWave ANC Over-Ear Headphones',
            description: 'Active Noise Cancelling headphones featuring 40mm dynamic drivers, memory foam ear cups, and multi-point Bluetooth connectivity. Pure sonic bliss.',
            brand: 'AuraSound',
            price: 12999.00,
            mrp: 18999.00,
            stockQuantity: 8,
            sku: 'HP-SND-WAV-02',
            mainImageURL: 'https://images.unsplash.com/photo-1505740420928-5e560c06d30e?w=500&auto=format&fit=crop&q=60',
            categoryId: 1,
            subCategoryId: 12,
            sellerId: 10,
            discountPercent: 31
        },
        {
            productId: 103,
            productName: 'Minimalist Walnut Desk Organizer',
            description: 'Handcrafted solid walnut wood desk organizer with phone stand, pen slots, and brass trays. Declutter your workspace with timeless premium styling.',
            brand: 'WoodSmith',
            price: 2499.00,
            mrp: 3499.00,
            stockQuantity: 20,
            sku: 'DK-WAL-ORG-03',
            mainImageURL: 'https://images.unsplash.com/photo-1513151233558-d860c5398176?w=500&auto=format&fit=crop&q=60',
            categoryId: 2,
            subCategoryId: 21,
            sellerId: 11,
            discountPercent: 28
        },
        {
            productId: 104,
            productName: 'ChronoClassic Automatic Watch',
            description: 'Sophisticated timepiece featuring Japanese automatic movement, sapphire crystal glass, and a genuine Italian leather strap. Water resistant up to 50 meters.',
            brand: 'Horology',
            price: 18500.00,
            mrp: 25000.00,
            stockQuantity: 4,
            sku: 'WT-CHR-CLA-04',
            mainImageURL: 'https://images.unsplash.com/photo-1524592094714-0f0654e20314?w=500&auto=format&fit=crop&q=60',
            categoryId: 3,
            subCategoryId: 31,
            sellerId: 12,
            discountPercent: 26
        },
        {
            productId: 105,
            productName: 'Nomad Canvas Travel Backpack',
            description: 'Weather-resistant waxed canvas backpack with dedicated 16-inch laptop pocket, hidden passport sleeve, and expandable side compartments for daily commuting or weekend escapes.',
            brand: 'NomadGear',
            price: 5999.00,
            mrp: 8999.00,
            stockQuantity: 12,
            sku: 'BP-NOM-CNV-05',
            mainImageURL: 'https://images.unsplash.com/photo-1553062407-98eeb64c6a62?w=500&auto=format&fit=crop&q=60',
            categoryId: 3,
            subCategoryId: 32,
            sellerId: 12,
            discountPercent: 33
        },
        {
            productId: 106,
            productName: 'Lumina Smart Ambient RGB Lamp',
            description: 'Vibrant smart desk lamp with voice control (Alexa/Google Assistant compatibility), custom presets, sound reactivity, and over 16 million colors to customize your space.',
            brand: 'Lumina',
            price: 3499.00,
            mrp: 4999.00,
            stockQuantity: 18,
            sku: 'LP-LUM-SMT-06',
            mainImageURL: 'https://images.unsplash.com/photo-1507473885765-e6ed057f782c?w=500&auto=format&fit=crop&q=60',
            categoryId: 2,
            subCategoryId: 22,
            sellerId: 11,
            discountPercent: 30
        }
    ];

    var mockCategories = [
        { id: 1, name: 'Electronics', description: 'Premium tech gadgets, audio gear, and peripherals.' },
        { id: 2, name: 'Home & Office', description: 'Functional accessories to style your modern room.' },
        { id: 3, name: 'Apparel & Lifestyle', description: 'Timeless watches, backpacks, and accessories.' }
    ];

    this.getProducts = function(categoryId, sellerId, page, size) {
        var params = {
            page: page || 0,
            size: size || 10
        };
        if (categoryId) params.categoryId = categoryId;
        if (sellerId) params.sellerId = sellerId;

        return apiService.get('/api/products', params)
            .then(function(response) {
                // If API returns an empty list, fallback to mocks
                if (response.data && response.data.content && response.data.content.length > 0) {
                    return response.data;
                }
                
                // Construct pageable mock response structure
                var filtered = mockProducts;
                if (categoryId) {
                    filtered = mockProducts.filter(function(p) { return p.categoryId == categoryId; });
                }
                return {
                    content: filtered,
                    totalElements: filtered.length,
                    totalPages: 1,
                    size: 10,
                    number: 0
                };
            })
            .catch(function() {
                // Return mocks in case of network/gateway failure
                var filtered = mockProducts;
                if (categoryId) {
                    filtered = mockProducts.filter(function(p) { return p.categoryId == categoryId; });
                }
                return {
                    content: filtered,
                    totalElements: filtered.length,
                    totalPages: 1,
                    size: 10,
                    number: 0
                };
            });
    };

    this.getProduct = function(productId) {
        return apiService.get('/api/products/' + productId)
            .then(function(response) {
                return response.data;
            })
            .catch(function() {
                var found = mockProducts.find(function(p) { return p.productId == productId; });
                if (found) return found;
                throw new Error('Product not found');
            });
    };

    this.createProduct = function(productData) {
        return apiService.post('/api/products', productData)
            .then(function(response) {
                return response.data;
            })
            .catch(function() {
                // Mock saving
                var newProduct = angular.copy(productData);
                newProduct.productId = mockProducts.length + 101;
                newProduct.discountPercent = Math.round(((newProduct.mrp - newProduct.price) / newProduct.mrp) * 100);
                mockProducts.push(newProduct);
                return newProduct;
            });
    };

    this.getCategories = function() {
        return apiService.get('/api/categories')
            .then(function(response) {
                if (response.data && response.data.length > 0) {
                    return response.data.map(function(cat) {
                        return {
                            id: cat.categoryId,
                            name: cat.categoryName,
                            active: cat.active
                        };
                    });
                }
                return mockCategories;
            })
            .catch(function() {
                return mockCategories;
            });
    };

    this.createCategory = function(categoryData) {
        var payload = {
            categoryName: categoryData.name,
            active: true
        };
        return apiService.post('/api/categories', payload)
            .then(function(response) {
                var cat = response.data;
                return {
                    id: cat.categoryId,
                    name: cat.categoryName,
                    active: cat.active
                };
            })
            .catch(function() {
                var newCat = angular.copy(categoryData);
                newCat.id = mockCategories.length + 1;
                mockCategories.push(newCat);
                return newCat;
            });
    };

    this.getSubCategories = function(categoryId) {
        return apiService.get('/api/categories/' + categoryId + '/sub-categories')
            .then(function(response) {
                if (response.data && response.data.length > 0) {
                    return response.data.map(function(sub) {
                        return {
                            subCategoryId: sub.subCategoryId,
                            categoryId: sub.categoryId,
                            name: sub.childCategory || sub.name,
                            active: sub.active
                        };
                    });
                }
                // Return fallback mock subcategories if empty
                var mockSubs = [
                    { subCategoryId: 11, categoryId: 1, name: 'Computer Mice', description: 'Gaming and productivity mice.' },
                    { subCategoryId: 12, categoryId: 1, name: 'Headphones', description: 'Over-ear and in-ear audio devices.' },
                    { subCategoryId: 21, categoryId: 2, name: 'Desk Accessories', description: 'Wooden organizers and stands.' },
                    { subCategoryId: 22, categoryId: 2, name: 'Lighting', description: 'Smart ambient lighting.' },
                    { subCategoryId: 31, categoryId: 3, name: 'Timepieces', description: 'Watches and horology.' },
                    { subCategoryId: 32, categoryId: 3, name: 'Bags & Packs', description: 'Backpacks and travel bags.' }
                ];
                return mockSubs.filter(function(sub) { return sub.categoryId == categoryId; });
            })
            .catch(function() {
                // Return fallback mock subcategories on error
                var mockSubs = [
                    { subCategoryId: 11, categoryId: 1, name: 'Computer Mice', description: 'Gaming and productivity mice.' },
                    { subCategoryId: 12, categoryId: 1, name: 'Headphones', description: 'Over-ear and in-ear audio devices.' },
                    { subCategoryId: 21, categoryId: 2, name: 'Desk Accessories', description: 'Wooden organizers and stands.' },
                    { subCategoryId: 22, categoryId: 2, name: 'Lighting', description: 'Smart ambient lighting.' },
                    { subCategoryId: 31, categoryId: 3, name: 'Timepieces', description: 'Watches and horology.' },
                    { subCategoryId: 32, categoryId: 3, name: 'Bags & Packs', description: 'Backpacks and travel bags.' }
                ];
                return mockSubs.filter(function(sub) { return sub.categoryId == categoryId; });
            });
    };

    this.createSubCategory = function(categoryId, subCategoryData) {
        var payload = {
            childCategory: subCategoryData.name,
            active: true
        };
        return apiService.post('/api/categories/' + categoryId + '/sub-categories', payload)
            .then(function(response) {
                var sub = response.data;
                return {
                    subCategoryId: sub.subCategoryId,
                    categoryId: sub.categoryId,
                    name: sub.childCategory,
                    active: sub.active
                };
            })
            .catch(function() {
                var newSub = angular.copy(subCategoryData);
                newSub.subCategoryId = Date.now();
                newSub.categoryId = categoryId;
                return newSub;
            });
    };

    this.uploadProductImage = function(file) {
        var fd = new FormData();
        fd.append('file', file);
        return $http.post(API_BASE + '/api/products/upload-image', fd, {
            transformRequest: angular.identity,
            headers: {'Content-Type': undefined}
        }).then(function(response) {
            return response.data.url;
        });
    };

    this.updateProduct = function(productId, productData) {
        return apiService.put('/api/products/' + productId, productData)
            .then(function(response) {
                return response.data;
            });
    };

    this.deleteProduct = function(productId) {
        return apiService.delete('/api/products/' + productId)
            .then(function(response) {
                return response.data;
            });
    };

    this.getProductStats = function() {
        return apiService.get('/api/products/stats')
            .then(function(response) {
                return response.data;
            });
    };
}]);

