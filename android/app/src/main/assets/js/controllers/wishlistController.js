/**
 * Wishlist View Controller
 */
app.controller('wishlistController', [
    '$scope', '$location', 'wishlistService', 'productService', 'cartService', 'authService', '$q',
    function($scope, $location, wishlistService, productService, cartService, authService, $q) {
        
        $scope.wishlistItems = [];

        $scope.initWishlist = function() {
            if (!authService.isLoggedIn()) {
                $location.path('/login');
                return;
            }
            $scope.loadWishlist();
        };

        $scope.loadWishlist = function() {
            wishlistService.getWishlist().then(function(items) {
                // Fetch product metadata for each item in the wishlist
                var promises = items.map(function(item) {
                    return productService.getProduct(item.productId)
                        .then(function(prod) {
                            return prod;
                        })
                        .catch(function() {
                            // Map minimal data if product api fails
                            return {
                                productId: item.productId,
                                productName: item.productName || ('Product ' + item.productId),
                                price: 0.00,
                                brand: 'Unknown'
                            };
                        });
                });

                $q.all(promises).then(function(products) {
                    $scope.wishlistItems = products;
                });
            });
        };

        $scope.addToCart = function(product) {
            cartService.addToCart(product.productId, product.productName, 1, product.price)
                .then(function() {
                    $scope.$emit('showToast', {
                        title: 'Added to Cart',
                        message: product.productName + ' added successfully.',
                        type: 'success'
                    });
                    
                    // Proactively remove from wishlist once added to cart (standard e-commerce pattern)
                    $scope.removeFromWishlist(product);
                });
        };

        $scope.removeFromWishlist = function(product) {
            wishlistService.removeFromWishlist(product.productId)
                .then(function() {
                    $scope.$emit('showToast', {
                        title: 'Removed from Wishlist',
                        message: product.productName + ' removed.',
                        type: 'info'
                    });
                    $scope.loadWishlist();
                });
        };
        $scope.addAllToCart = function() {
            if ($scope.wishlistItems.length === 0) return;
            var promises = $scope.wishlistItems.map(function(product) {
                return cartService.addToCart(product.productId, product.productName, 1, product.price)
                    .then(function() {
                        return wishlistService.removeFromWishlist(product.productId);
                    });
            });

            $q.all(promises).then(function() {
                $scope.$emit('showToast', {
                    title: 'All Items Added',
                    message: 'All wishlist items have been moved to your cart.',
                    type: 'success'
                });
                $scope.loadWishlist();
            });
        };
    }
]);
