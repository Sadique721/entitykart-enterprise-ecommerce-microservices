/**
 * Wishlist Management Service
 */
app.service('wishlistService', ['apiService', 'authService', '$rootScope', '$q', function(apiService, authService, $rootScope, $q) {
    var self = this;
    var localWishlist = [];

    function getCustomerId() {
        var user = authService.getCurrentUser();
        return user ? user.id : null;
    }

    this.addToWishlist = function(productId) {
        var customerId = getCustomerId();
        if (!customerId) {
            $rootScope.$broadcast('showToast', {
                title: 'Sign In Required',
                message: 'Please log in to add items to your wishlist.',
                type: 'info'
            });
            return $q.reject('Not logged in');
        }

        var params = {
            customerId: customerId,
            productId: productId
        };

        return apiService.post('/api/wishlist/add', null, params)
            .then(function() {
                $rootScope.$broadcast('wishlist:updated');
                return true;
            })
            .catch(function() {
                self.initLocalWishlist();
                if (!localWishlist.includes(productId)) {
                    localWishlist.push(productId);
                    self.saveLocalWishlist();
                }
                $rootScope.$broadcast('wishlist:updated');
                return true;
            });
    };

    this.removeFromWishlist = function(productId) {
        var customerId = getCustomerId();
        if (!customerId) return $q.reject('Not logged in');

        var params = {
            customerId: customerId,
            productId: productId
        };

        return apiService.delete('/api/wishlist/remove', params)
            .then(function() {
                $rootScope.$broadcast('wishlist:updated');
                return true;
            })
            .catch(function() {
                self.initLocalWishlist();
                localWishlist = localWishlist.filter(function(id) { return id != productId; });
                self.saveLocalWishlist();
                $rootScope.$broadcast('wishlist:updated');
                return true;
            });
    };

    this.clearWishlist = function() {
        var customerId = getCustomerId();
        if (!customerId) return $q.reject('Not logged in');

        return apiService.delete('/api/wishlist/clear', { customerId: customerId })
            .then(function() {
                $rootScope.$broadcast('wishlist:updated');
                return true;
            })
            .catch(function() {
                localWishlist = [];
                self.saveLocalWishlist();
                $rootScope.$broadcast('wishlist:updated');
                return true;
            });
    };

    this.getWishlist = function() {
        var customerId = getCustomerId();
        if (!customerId) return $q.resolve([]);

        return apiService.get('/api/wishlist', { customerId: customerId })
            .then(function(response) {
                return response.data;
            })
            .catch(function() {
                self.initLocalWishlist();
                // Map list of productIds to simulated DTOs
                return localWishlist.map(function(id) {
                    return {
                        wishlistItemId: Date.now() + id,
                        productId: id,
                        productName: 'Product ' + id
                    };
                });
            });
    };

    this.isWishlisted = function(productId) {
        return this.getWishlist().then(function(items) {
            return items.some(function(item) {
                return item.productId == productId;
            });
        });
    };

    this.initLocalWishlist = function() {
        var customerId = getCustomerId();
        if (customerId) {
            localWishlist = JSON.parse(localStorage.getItem('ekWishlist_' + customerId) || '[]');
        } else {
            localWishlist = [];
        }
    };

    this.saveLocalWishlist = function() {
        var customerId = getCustomerId();
        if (customerId) {
            localStorage.setItem('ekWishlist_' + customerId, JSON.stringify(localWishlist));
        }
    };
}]);
