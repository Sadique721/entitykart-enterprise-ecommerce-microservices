/**
 * Product Browsing and Detail Controller
 */
app.controller('productController', [
    '$scope', '$routeParams', '$location', 'productService', 'cartService', 'wishlistService', 'reviewService', 'authService',
    function($scope, $routeParams, $location, productService, cartService, wishlistService, reviewService, authService) {
        
        // Scope variables
        $scope.products = [];
        $scope.categories = [];
        $scope.selectedCategory = null;
        $scope.selectedProduct = null;
        
        // Pagination & search
        $scope.currentPage = 0;
        $scope.totalPages = 1;
        $scope.searchQuery = $routeParams.query || '';
        $scope.catFilterId = $routeParams.categoryId || null;
        $scope.subFilterId = null;

        // Details page parameters
        $scope.detailQty = 1;
        $scope.reviews = [];
        $scope.ratingStats = null;
        $scope.isWishlisted = false;
        
        // New Review submission
        $scope.newReview = {
            rating: 5,
            comment: ''
        };

        // --- Core Catalog Loading ---

        $scope.activeSlide = 0;
        $scope.slides = [
            { image: 'https://images.unsplash.com/photo-1505740420928-5e560c06d30e?w=1200&h=400&fit=crop', title: 'Summer Electronics Bash', subtitle: 'Save up to 40% on top audiophile gear and smart gadgets', url: '#!/products?categoryId=1' },
            { image: 'https://images.unsplash.com/photo-1523275335684-37898b6baf30?w=1200&h=400&fit=crop', title: 'Minimalist Office Styling', subtitle: 'Premium wooden desktop organizers & workspace lighting', url: '#!/products?categoryId=2' },
            { image: 'https://images.unsplash.com/photo-1525966222134-fcfa99b8ae77?w=1200&h=400&fit=crop', title: 'Luxury Lifestyle Watches', subtitle: 'Timeless craftsmanship & elegant watches for any look', url: '#!/products?categoryId=3' }
        ];
        $scope.nextSlide = function() {
            $scope.activeSlide = ($scope.activeSlide + 1) % $scope.slides.length;
        };
        $scope.prevSlide = function() {
            $scope.activeSlide = ($scope.activeSlide - 1 + $scope.slides.length) % $scope.slides.length;
        };
        $scope.setSlide = function(index) {
            $scope.activeSlide = index;
        };

        $scope.wishlistProductIds = [];
        
        $scope.loadUserWishlist = function() {
            if (authService.isLoggedIn()) {
                wishlistService.getWishlist().then(function(items) {
                    $scope.wishlistProductIds = items.map(function(item) {
                        return item.productId;
                    });
                });
            } else {
                $scope.wishlistProductIds = [];
            }
        };

        $scope.isProductWishlisted = function(productId) {
            return $scope.wishlistProductIds.indexOf(productId) > -1;
        };

        $scope.initHome = function() {
            $scope.loadUserWishlist();
            // Load featured items
            productService.getProducts(null, null, 0, 4).then(function(data) {
                $scope.products = data.content;
            });
            // Load categories
            productService.getCategories().then(function(data) {
                $scope.categories = data;
            });
            
            // Auto play slider
            var sliderInterval = setInterval(function() {
                $scope.$apply(function() {
                    $scope.nextSlide();
                });
            }, 5000);

            $scope.$on('$destroy', function() {
                clearInterval(sliderInterval);
            });
        };

        $scope.initCatalog = function() {
            $scope.loadUserWishlist();
            productService.getCategories().then(function(cats) {
                $scope.categories = cats;
                // Preload subcategories for each category
                cats.forEach(function(cat) {
                    productService.getSubCategories(cat.id).then(function(subs) {
                        cat.subCategories = subs;
                    });
                });
                if ($scope.catFilterId) {
                    for (var i = 0; i < cats.length; i++) {
                        if (cats[i].id == $scope.catFilterId) {
                            $scope.selectedCategory = cats[i];
                            break;
                        }
                    }
                }
            });
            $scope.loadProducts();
        };

        $scope.loadProducts = function() {
            productService.getProducts($scope.catFilterId, null, $scope.currentPage, 8)
                .then(function(data) {
                    var items = data.content;
                    
                    // Filter by subcategory if set
                    if ($scope.subFilterId) {
                        items = items.filter(function(p) {
                            return p.subCategoryId == $scope.subFilterId;
                        });
                    }

                    // Client side search filter if query string matches brand or title
                    if ($scope.searchQuery) {
                        var q = $scope.searchQuery.toLowerCase();
                        items = items.filter(function(p) {
                            return p.productName.toLowerCase().indexOf(q) > -1 || 
                                   p.brand.toLowerCase().indexOf(q) > -1 ||
                                   p.description.toLowerCase().indexOf(q) > -1;
                        });
                    }

                    $scope.products = items;
                    $scope.totalPages = data.totalPages;
                });
        };

        $scope.filterByCategory = function(category) {
            $scope.selectedCategory = category;
            $scope.catFilterId = category ? category.id : null;
            $scope.subFilterId = null; // Reset subcategory filter when changing categories
            $scope.currentPage = 0;
            if ($location.path() !== '/products') {
                $location.path('/products').search('categoryId', $scope.catFilterId);
            } else {
                $location.search('categoryId', $scope.catFilterId);
                $scope.loadProducts();
            }
        };

        $scope.filterBySubCategory = function(sub, event) {
            if (event) event.stopPropagation(); // Prevent parent category click handler
            $scope.subFilterId = sub ? sub.subCategoryId : null;
            $scope.currentPage = 0;
            $scope.loadProducts();
        };

        $scope.clearFilters = function() {
            $scope.selectedCategory = null;
            $scope.catFilterId = null;
            $scope.subFilterId = null;
            $scope.searchQuery = '';
            $scope.currentPage = 0;
            $location.search({});
            $scope.loadProducts();
        };

        $scope.changePage = function(direction) {
            var newPage = $scope.currentPage + direction;
            if (newPage >= 0 && newPage < $scope.totalPages) {
                $scope.currentPage = newPage;
                $scope.loadProducts();
            }
        };

        // --- Details Page ---

        $scope.initDetails = function() {
            var productId = $routeParams.productId;
            if (!productId) return;

            productService.getProduct(productId)
                .then(function(product) {
                    $scope.selectedProduct = product;
                    
                    // Check if wishlisted
                    if (authService.isLoggedIn()) {
                        wishlistService.isWishlisted(productId).then(function(status) {
                            $scope.isWishlisted = status;
                        });
                    }

                    // Load reviews and stats
                    $scope.loadProductReviews(productId);
                })
                .catch(function() {
                    $scope.$emit('showToast', {
                        title: 'Product Not Found',
                        message: 'The requested product is not available.',
                        type: 'error'
                    });
                    $location.path('/products');
                });
        };

        $scope.loadProductReviews = function(productId) {
            reviewService.getProductReviews(productId).then(function(reviews) {
                $scope.reviews = reviews;
            });
            reviewService.getProductStats(productId).then(function(stats) {
                $scope.ratingStats = stats;
            });
        };

        // Quantity manipulation
        $scope.adjustQty = function(amount) {
            var newQty = $scope.detailQty + amount;
            if (newQty >= 1 && newQty <= ($scope.selectedProduct.stockQuantity || 10)) {
                $scope.detailQty = newQty;
            }
        };

        // --- Cart and Wishlist Actions ---

        $scope.addToCart = function(product, qty) {
            var quantity = qty || 1;
            cartService.addToCart(product.productId, product.productName, quantity, product.price)
                .then(function() {
                    $scope.$emit('showToast', {
                        title: 'Added to Cart',
                        message: quantity + 'x ' + product.productName + ' added successfully.',
                        type: 'success'
                    });
                });
        };

        $scope.buyNow = function(product) {
            cartService.addToCart(product.productId, product.productName, 1, product.price)
                .then(function() {
                    $scope.$emit('showToast', {
                        title: 'Success',
                        message: 'Redirecting to checkout...',
                        type: 'success'
                    });
                    $location.path('/cart');
                });
        };

        $scope.toggleWishlist = function(product) {
            if (!authService.isLoggedIn()) {
                $scope.$emit('showToast', {
                    title: 'Authentication Required',
                    message: 'Please log in to manage your wishlist.',
                    type: 'info'
                });
                $location.path('/login');
                return;
            }

            if ($scope.isProductWishlisted(product.productId)) {
                wishlistService.removeFromWishlist(product.productId)
                    .then(function() {
                        var index = $scope.wishlistProductIds.indexOf(product.productId);
                        if (index > -1) {
                            $scope.wishlistProductIds.splice(index, 1);
                        }
                        $scope.$emit('showToast', {
                            title: 'Removed from Wishlist',
                            message: product.productName + ' removed.',
                            type: 'info'
                        });
                    });
            } else {
                wishlistService.addToWishlist(product.productId)
                    .then(function() {
                        $scope.wishlistProductIds.push(product.productId);
                        $scope.$emit('showToast', {
                            title: 'Added to Wishlist',
                            message: product.productName + ' added.',
                            type: 'success'
                        });
                    });
            }
        };

        // --- Review Submission ---

        $scope.submitReview = function() {
            if (!authService.isLoggedIn()) {
                $scope.$emit('showToast', {
                    title: 'Authentication Required',
                    message: 'Please log in to submit a review.',
                    type: 'info'
                });
                $location.path('/login');
                return;
            }

            var request = {
                productId: $scope.selectedProduct.productId,
                rating: $scope.newReview.rating,
                comment: $scope.newReview.comment
            };

            reviewService.createReview(request)
                .then(function() {
                    $scope.$emit('showToast', {
                        title: 'Review Submitted',
                        message: 'Thank you for your feedback.',
                        type: 'success'
                    });
                    $scope.newReview.comment = '';
                    $scope.loadProductReviews($scope.selectedProduct.productId);
                });
        };
        // Expose authService check to template
        $scope.isLoggedIn = function() {
            return authService.isLoggedIn();
        };

        // Scroll helper to reviews section
        $scope.scrollToReviews = function() {
            var elem = document.getElementById('reviews-section');
            if (elem) {
                elem.scrollIntoView({ behavior: 'smooth' });
            }
        };

        // Get rating bar width helper
        $scope.getBarWidth = function(star) {
            if (!$scope.ratingStats || !$scope.ratingStats.distribution || !$scope.ratingStats.totalReviews) {
                return 0;
            }
            var count = $scope.ratingStats.distribution[star] || 0;
            return (count / $scope.ratingStats.totalReviews) * 100;
        };
    }
]);
