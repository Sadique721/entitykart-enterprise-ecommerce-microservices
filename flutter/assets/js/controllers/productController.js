/**
 * Product Browsing and Detail Controller
 * v1.5.0 — Bug fixes + new features: sort, price filter, recently viewed, notify-me, share
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

        // Sort & Filter
        $scope.sortOption = 'default';
        $scope.priceMin = null;
        $scope.priceMax = null;

        // Details page parameters
        $scope.detailQty = 1;
        $scope.reviews = [];
        $scope.ratingStats = null;
        $scope.isWishlisted = false;
        $scope.activeImageIndex = 0;

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

        // --- Flash Sale Countdown ---
        $scope.flashSaleProducts = [];
        $scope.countdownTime = { hours: 0, minutes: 0, seconds: 0 };

        function startFlashSaleCountdown() {
            // Flash sale ends in 8 hours from now
            var endTime = new Date();
            endTime.setHours(endTime.getHours() + 8);

            var timer = setInterval(function() {
                var now = new Date();
                var diff = endTime - now;
                if (diff <= 0) {
                    clearInterval(timer);
                    $scope.$apply(function() {
                        $scope.countdownTime = { hours: 0, minutes: 0, seconds: 0 };
                    });
                    return;
                }
                var h = Math.floor(diff / 3600000);
                var m = Math.floor((diff % 3600000) / 60000);
                var s = Math.floor((diff % 60000) / 1000);
                $scope.$apply(function() {
                    $scope.countdownTime = {
                        hours: h < 10 ? '0' + h : '' + h,
                        minutes: m < 10 ? '0' + m : '' + m,
                        seconds: s < 10 ? '0' + s : '' + s
                    };
                });
            }, 1000);

            $scope.$on('$destroy', function() { clearInterval(timer); });
        }

        // --- Recently Viewed ---
        $scope.recentlyViewed = [];

        function loadRecentlyViewed() {
            var rv = JSON.parse(localStorage.getItem('ekRecentlyViewed') || '[]');
            $scope.recentlyViewed = rv.slice(0, 6); // max 6
        }

        function saveToRecentlyViewed(product) {
            var rv = JSON.parse(localStorage.getItem('ekRecentlyViewed') || '[]');
            // Remove if already exists
            rv = rv.filter(function(p) { return p.productId !== product.productId; });
            rv.unshift({
                productId: product.productId,
                productName: product.productName,
                brand: product.brand,
                price: product.price,
                mrp: product.mrp,
                mainImageURL: product.mainImageURL,
                discountPercent: product.discountPercent,
                stockQuantity: product.stockQuantity
            });
            if (rv.length > 10) rv = rv.slice(0, 10);
            localStorage.setItem('ekRecentlyViewed', JSON.stringify(rv));
        }

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

        $scope.getCategoryIcon = function(cat) {
            if (!cat) return 'fa-box';
            var name = (cat.name || '').toLowerCase().trim();
            if (name.indexOf('electronic') > -1) {
                return 'fa-laptop-code';
            } else if (name.indexOf('fashion') > -1 || name.indexOf('clothing') > -1) {
                return 'fa-shirt';
            } else if (name.indexOf('home') > -1 || name.indexOf('kitchen') > -1) {
                return 'fa-couch';
            } else if (name.indexOf('beauty') > -1 || name.indexOf('personal care') > -1 || name.indexOf('cosmetic') > -1) {
                return 'fa-spa';
            } else if (name.indexOf('grocery') > -1 || name.indexOf('groceries') > -1 || name.indexOf('food') > -1) {
                return 'fa-basket-shopping';
            } else if (name.indexOf('sports') > -1 || name.indexOf('fitness') > -1 || name.indexOf('gym') > -1) {
                return 'fa-dumbbell';
            } else if (name.indexOf('book') > -1 || name.indexOf('education') > -1) {
                return 'fa-book';
            } else if (name.indexOf('toy') > -1 || name.indexOf('game') > -1) {
                return 'fa-gamepad';
            } else if (name.indexOf('automotive') > -1 || name.indexOf('car') > -1 || name.indexOf('bike') > -1) {
                return 'fa-car';
            } else if (name.indexOf('health') > -1 || name.indexOf('wellness') > -1 || name.indexOf('medical') > -1) {
                return 'fa-heart-pulse';
            }
            return 'fa-tags';
        };

        $scope.initHome = function() {
            $scope.loadUserWishlist();
            loadRecentlyViewed();
            // Load featured items
            productService.getProducts(null, null, 0, 8).then(function(data) {
                $scope.products = data.content;
                // Flash sale products = items with discount > 20%
                $scope.flashSaleProducts = data.content.filter(function(p) {
                    return p.discountPercent >= 20;
                }).slice(0, 4);
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

            startFlashSaleCountdown();

            $scope.$on('$destroy', function() {
                clearInterval(sliderInterval);
            });
        };

        $scope.initCatalog = function() {
            $scope.loadUserWishlist();
            productService.getCategories().then(function(cats) {
                $scope.categories = cats;
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
            productService.getProducts($scope.catFilterId, null, $scope.currentPage, 12)
                .then(function(data) {
                    var items = data.content;

                    // Filter by subcategory
                    if ($scope.subFilterId) {
                        items = items.filter(function(p) {
                            return p.subCategoryId == $scope.subFilterId;
                        });
                    }

                    // Client-side search filter
                    if ($scope.searchQuery) {
                        var q = $scope.searchQuery.toLowerCase();
                        items = items.filter(function(p) {
                            return p.productName.toLowerCase().indexOf(q) > -1 ||
                                   (p.brand || '').toLowerCase().indexOf(q) > -1 ||
                                   (p.description || '').toLowerCase().indexOf(q) > -1;
                        });
                    }

                    // Price range filter
                    if ($scope.priceMin !== null && $scope.priceMin !== '') {
                        items = items.filter(function(p) { return p.price >= parseFloat($scope.priceMin); });
                    }
                    if ($scope.priceMax !== null && $scope.priceMax !== '') {
                        items = items.filter(function(p) { return p.price <= parseFloat($scope.priceMax); });
                    }

                    // Sort
                    if ($scope.sortOption === 'price_asc') {
                        items.sort(function(a, b) { return a.price - b.price; });
                    } else if ($scope.sortOption === 'price_desc') {
                        items.sort(function(a, b) { return b.price - a.price; });
                    } else if ($scope.sortOption === 'discount') {
                        items.sort(function(a, b) { return (b.discountPercent || 0) - (a.discountPercent || 0); });
                    } else if ($scope.sortOption === 'newest') {
                        items.sort(function(a, b) { return b.productId - a.productId; });
                    }

                    $scope.products = items;
                    $scope.totalPages = data.totalPages;
                });
        };

        $scope.applyFilters = function() {
            $scope.currentPage = 0;
            $scope.loadProducts();
        };

        $scope.clearPriceFilter = function() {
            $scope.priceMin = null;
            $scope.priceMax = null;
            $scope.loadProducts();
        };

        $scope.filterByCategory = function(category) {
            $scope.selectedCategory = category;
            $scope.catFilterId = category ? category.id : null;
            $scope.subFilterId = null;
            $scope.currentPage = 0;
            if ($location.path() !== '/products') {
                $location.path('/products').search('categoryId', $scope.catFilterId);
            } else {
                $location.search('categoryId', $scope.catFilterId);
                $scope.loadProducts();
            }
        };

        $scope.filterBySubCategory = function(sub, event) {
            if (event) event.stopPropagation();
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
            $scope.sortOption = 'default';
            $scope.priceMin = null;
            $scope.priceMax = null;
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
                    $scope.activeImageIndex = 0;

                    // Build gallery images (use mainImageURL + additional if available)
                    $scope.galleryImages = [];
                    if (product.mainImageURL) $scope.galleryImages.push(product.mainImageURL);
                    if (product.additionalImages && product.additionalImages.length > 0) {
                        product.additionalImages.forEach(function(img) {
                            $scope.galleryImages.push(img);
                        });
                    }
                    // If only 1 image, generate alternate views for demo
                    if ($scope.galleryImages.length <= 1 && product.mainImageURL) {
                        $scope.galleryImages = [
                            product.mainImageURL,
                            product.mainImageURL + '&sat=-100',
                            product.mainImageURL + '&bri=20'
                        ];
                    }

                    // Save to recently viewed
                    saveToRecentlyViewed(product);

                    // Check wishlist status
                    if (authService.isLoggedIn()) {
                        wishlistService.isWishlisted(productId).then(function(status) {
                            $scope.isWishlisted = status;
                        });
                    }

                    // Load reviews
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

        $scope.setActiveImage = function(index) {
            $scope.activeImageIndex = index;
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
                        // BUG FIX: was missing space between quantity and 'x'
                        message: quantity + ' × ' + product.productName + ' added successfully.',
                        type: 'success'
                    });
                });
        };

        // BUG FIX: buyNow now redirects to /checkout directly (was /cart before)
        $scope.buyNow = function(product) {
            cartService.addToCart(product.productId, product.productName, 1, product.price)
                .then(function() {
                    $scope.$emit('showToast', {
                        title: 'Proceeding to Checkout',
                        message: 'Redirecting to secure checkout...',
                        type: 'success'
                    });
                    $location.path('/checkout');
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
                        $scope.isWishlisted = false;
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
                        $scope.isWishlisted = true;
                        $scope.$emit('showToast', {
                            title: 'Added to Wishlist',
                            message: product.productName + ' saved.',
                            type: 'success'
                        });
                    });
            }
        };

        // --- Notify Me (Out of Stock) ---
        $scope.notifyMeEmail = '';
        $scope.notifyMeSubmitted = false;

        $scope.submitNotifyMe = function(product) {
            if (!$scope.notifyMeEmail) {
                $scope.notifyMeEmail = authService.isLoggedIn() ?
                    (authService.getCurrentUser().email || '') : '';
            }
            if (!$scope.notifyMeEmail) {
                $scope.$emit('showToast', {
                    title: 'Email Required',
                    message: 'Please enter your email address.',
                    type: 'error'
                });
                return;
            }
            // Save locally
            var notifications = JSON.parse(localStorage.getItem('ekNotifyMe') || '[]');
            notifications.push({
                productId: product.productId,
                productName: product.productName,
                email: $scope.notifyMeEmail,
                savedAt: new Date().toISOString()
            });
            localStorage.setItem('ekNotifyMe', JSON.stringify(notifications));
            $scope.notifyMeSubmitted = true;
            $scope.$emit('showToast', {
                title: 'Notification Registered',
                message: 'We\'ll email you at ' + $scope.notifyMeEmail + ' when this is back in stock.',
                type: 'success'
            });
        };

        // --- Share Product ---
        $scope.shareProduct = function(product) {
            var shareData = {
                title: product.productName,
                text: product.brand + ' - ' + product.productName + ' at ₹' + product.price,
                url: window.location.href
            };
            if (navigator.share) {
                navigator.share(shareData).catch(function() {});
            } else {
                // Fallback: copy to clipboard
                var dummy = document.createElement('input');
                document.body.appendChild(dummy);
                dummy.value = window.location.href;
                dummy.select();
                document.execCommand('copy');
                document.body.removeChild(dummy);
                $scope.$emit('showToast', {
                    title: 'Link Copied',
                    message: 'Product URL copied to clipboard.',
                    type: 'info'
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
                    $scope.newReview = { rating: 5, comment: '' };
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
