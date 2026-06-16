/**
 * Cart Management Service
 */
app.service('cartService', ['apiService', 'authService', '$rootScope', '$q', function(apiService, authService, $rootScope, $q) {
    var self = this;
    var localCart = [];

    // Helper to get customer ID safely
    function getCustomerId() {
        var user = authService.getCurrentUser();
        return user ? user.id : null;
    }

    this.addToCart = function(productId, productName, quantity, price) {
        var customerId = getCustomerId();
        if (!customerId) {
            $rootScope.$broadcast('showToast', {
                title: 'Sign In Required',
                message: 'Please log in to add items to your cart.',
                type: 'info'
            });
            return $q.reject('Not logged in');
        }

        var params = {
            customerId: customerId,
            productId: productId,
            quantity: quantity,
            price: price
        };

        return apiService.post('/api/cart/add', null, params)
            .then(function() {
                $rootScope.$broadcast('cart:updated');
                return true;
            })
            .catch(function() {
                // Mock Fallback
                self.initLocalCart();
                var existing = localCart.find(function(item) {
                    return item.productId == productId;
                });
                if (existing) {
                    existing.quantity += quantity;
                    existing.subtotal = existing.quantity * existing.price;
                } else {
                    localCart.push({
                        cartItemId: Date.now(),
                        productId: productId,
                        productName: productName || ('Product ' + productId),
                        quantity: quantity,
                        price: price,
                        subtotal: quantity * price
                    });
                }
                self.saveLocalCart();
                $rootScope.$broadcast('cart:updated');
                return true;
            });
    };

    this.updateQuantity = function(productId, quantity) {
        var customerId = getCustomerId();
        if (!customerId) return $q.reject('Not logged in');

        var params = {
            customerId: customerId,
            productId: productId,
            quantity: quantity
        };

        return apiService.put('/api/cart/update', null, params)
            .then(function() {
                $rootScope.$broadcast('cart:updated');
                return true;
            })
            .catch(function() {
                self.initLocalCart();
                var item = localCart.find(function(i) { return i.productId == productId; });
                if (item) {
                    item.quantity = quantity;
                    item.subtotal = quantity * item.price;
                    self.saveLocalCart();
                    $rootScope.$broadcast('cart:updated');
                }
                return true;
            });
    };

    this.removeItem = function(productId) {
        var customerId = getCustomerId();
        if (!customerId) return $q.reject('Not logged in');

        var params = {
            customerId: customerId,
            productId: productId
        };

        return apiService.delete('/api/cart/remove', params)
            .then(function() {
                $rootScope.$broadcast('cart:updated');
                return true;
            })
            .catch(function() {
                self.initLocalCart();
                localCart = localCart.filter(function(i) { return i.productId != productId; });
                self.saveLocalCart();
                $rootScope.$broadcast('cart:updated');
                return true;
            });
    };

    this.clearCart = function() {
        var customerId = getCustomerId();
        if (!customerId) return $q.reject('Not logged in');

        return apiService.delete('/api/cart/clear', { customerId: customerId })
            .then(function() {
                $rootScope.$broadcast('cart:updated');
                return true;
            })
            .catch(function() {
                localCart = [];
                self.saveLocalCart();
                $rootScope.$broadcast('cart:updated');
                return true;
            });
    };

    this.getCart = function() {
        var customerId = getCustomerId();
        if (!customerId) {
            return $q.resolve([]);
        }

        return apiService.get('/api/cart', { customerId: customerId })
            .then(function(response) {
                return response.data;
            })
            .catch(function() {
                self.initLocalCart();
                return localCart;
            });
    };

    this.getCartTotal = function() {
        var customerId = getCustomerId();
        if (!customerId) return $q.resolve(0.00);

        return apiService.get('/api/cart/total', { customerId: customerId })
            .then(function(response) {
                return response.data;
            })
            .catch(function() {
                self.initLocalCart();
                var total = 0;
                localCart.forEach(function(item) {
                    total += item.subtotal;
                });
                return total;
            });
    };

    this.checkout = function(addressId) {
        var customerId = getCustomerId();
        if (!customerId) return $q.reject('Not logged in');

        var params = {
            customerId: customerId,
            addressId: addressId || 1001 // Default mock address if none selected
        };

        return apiService.post('/api/cart/checkout', null, params)
            .then(function(response) {
                $rootScope.$broadcast('cart:updated');
                return response.data;
            })
            .catch(function() {
                // If api fails, we mock successful checkout creation
                return self.getCart().then(function(items) {
                    if (items.length === 0) {
                        throw new Error('Cart is empty');
                    }
                    
                    return self.getCartTotal().then(function(total) {
                        // Create mock order in localStorage
                        var mockOrders = JSON.parse(localStorage.getItem('ekMockOrders') || '[]');
                        var newOrder = {
                            orderId: Date.now(),
                            customerId: customerId,
                            addressId: addressId || 1001,
                            totalAmount: total,
                            orderStatus: 'PLACED',
                            paymentStatus: 'PENDING',
                            orderDate: new Date().toISOString(),
                            items: items.map(function(item) {
                                return {
                                    orderItemId: Date.now() + Math.round(Math.random() * 1000),
                                    productId: item.productId,
                                    productName: item.productName,
                                    quantity: item.quantity,
                                    price: item.price,
                                    subtotal: item.subtotal
                                };
                            })
                        };
                        mockOrders.push(newOrder);
                        localStorage.setItem('ekMockOrders', JSON.stringify(mockOrders));
                        
                        // Clear the cart locally
                        self.clearCart();
                        return newOrder;
                    });
                });
            });
    };

    // Sync helper logic for local mock
    this.initLocalCart = function() {
        var customerId = getCustomerId();
        if (customerId) {
            localCart = JSON.parse(localStorage.getItem('ekCart_' + customerId) || '[]');
        } else {
            localCart = [];
        }
    };

    this.saveLocalCart = function() {
        var customerId = getCustomerId();
        if (customerId) {
            localStorage.setItem('ekCart_' + customerId, JSON.stringify(localCart));
        }
    };
}]);
