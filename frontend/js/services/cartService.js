/**
 * Cart Management Service
 * v2.0 — Cart image enrichment, full payment flow, processPayment method
 */
app.service('cartService', ['apiService', 'authService', '$rootScope', '$q', function(apiService, authService, $rootScope, $q) {
    var self = this;
    var localCart = [];

    // Helper to get customer ID safely
    function getCustomerId() {
        var user = authService.getCurrentUser();
        return user ? user.id : null;
    }

    // ─── Enrich cart items with product image URLs ────────────────────────────
    function enrichCartImages(items) {
        if (!items || items.length === 0) return $q.resolve(items);

        var promises = items.map(function(item) {
            // If image already present, skip enrichment
            if (item.mainImageURL) return $q.resolve(item);

            return apiService.get('/api/products/' + item.productId)
                .then(function(res) {
                    item.mainImageURL = res.data.mainImageURL || '';
                    return item;
                })
                .catch(function() {
                    // Fallback: look up in localStorage-cached products
                    try {
                        var cachedStr = localStorage.getItem('ekProductCache');
                        if (cachedStr) {
                            var cache = JSON.parse(cachedStr);
                            var found = cache.find(function(p) { return p.productId == item.productId; });
                            if (found) item.mainImageURL = found.mainImageURL || '';
                        }
                    } catch (e) { /* ignore */ }
                    return item;
                });
        });

        return $q.all(promises);
    }

    this.addToCart = function(productId, productName, quantity, price, mainImageURL) {
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
                    if (!existing.mainImageURL && mainImageURL) {
                        existing.mainImageURL = mainImageURL;
                    }
                } else {
                    localCart.push({
                        cartItemId: Date.now(),
                        productId: productId,
                        productName: productName || ('Product ' + productId),
                        quantity: quantity,
                        price: price,
                        subtotal: quantity * price,
                        mainImageURL: mainImageURL || ''
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
                var items = response.data;
                return enrichCartImages(items);
            })
            .catch(function() {
                self.initLocalCart();
                return enrichCartImages(localCart);
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

    this.checkout = function(addressId, paymentMethod, paymentData) {
        var customerId = getCustomerId();
        if (!customerId) return $q.reject('Not logged in');

        var params = {
            customerId: customerId,
            addressId: addressId || 1001,
            paymentMode: (paymentMethod || 'cod').toUpperCase()
        };

        if (paymentData) {
            if (paymentData.cardNumber) params.cardNumber = paymentData.cardNumber;
            if (paymentData.expiry) params.expiry = paymentData.expiry;
            if (paymentData.cvv) params.cvv = paymentData.cvv;
            if (paymentData.upiId) params.upiId = paymentData.upiId;
            if (paymentData.bankName) params.bankName = paymentData.bankName;
            if (paymentData.walletType) params.walletType = paymentData.walletType;
            if (paymentData.emiTenure) params.emiTenure = paymentData.emiTenure;
        }

        return apiService.post('/api/cart/checkout', null, params)
            .then(function(response) {
                $rootScope.$broadcast('cart:updated');
                return response.data; // returns order object with orderId
            })
            .catch(function() {
                // If api fails, we mock successful checkout creation
                return self.getCart().then(function(items) {
                    if (items.length === 0) {
                        throw new Error('Cart is empty');
                    }
                    
                    return self.getCartTotal().then(function(total) {
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

    // ─── Process payment after order creation ─────────────────────────────────
    this.processPayment = function(orderId, amount, paymentMethod, paymentData, customerEmail, customerName) {
        var method = (paymentMethod || 'cod').toLowerCase();
        var user = authService.getCurrentUser();
        customerEmail = customerEmail || (user ? user.email : '');
        customerName = customerName || (user ? user.name : '');

        if (method === 'card') {
            var expiryParts = (paymentData.expiry || '12/26').split('/');
            var cardPayload = {
                orderId: orderId,
                amount: amount,
                paymentMode: 'CARD',
                cardNumber: (paymentData.cardNumber || '').replace(/\s/g, ''),
                expiryMonth: expiryParts[0] || '12',
                expiryYear: expiryParts[1] || '26',
                cvv: paymentData.cvv || '',
                customerEmail: customerEmail,
                customerName: customerName
            };
            return apiService.post('/api/payments/process-card', cardPayload)
                .then(function(res) { return res.data; })
                .catch(function() {
                    return self._mockPaymentSuccess(orderId, 'CARD', 'CARD_' + Date.now());
                });
        } else if (method === 'upi') {
            return apiService.post('/api/payments/process-offline', null, {
                orderId: orderId,
                amount: amount,
                paymentMode: 'UPI',
                customerEmail: customerEmail,
                customerName: customerName
            })
            .then(function(res) { return res.data; })
            .catch(function() {
                return self._mockPaymentSuccess(orderId, 'UPI', 'UPI_' + Date.now());
            });
        } else if (method === 'netbanking') {
            return apiService.post('/api/payments/process-netbanking', null, {
                orderId: orderId,
                amount: amount,
                bankName: paymentData.bankName || 'SBI',
                customerEmail: customerEmail,
                customerName: customerName
            })
            .then(function(res) { return res.data; })
            .catch(function() {
                return self._mockPaymentSuccess(orderId, 'NET_BANKING', 'NB_' + Date.now());
            });
        } else if (method === 'wallet') {
            return apiService.post('/api/payments/process-wallet', null, {
                orderId: orderId,
                amount: amount,
                walletType: paymentData.walletType || 'PAYTM',
                customerEmail: customerEmail,
                customerName: customerName
            })
            .then(function(res) { return res.data; })
            .catch(function() {
                return self._mockPaymentSuccess(orderId, 'WALLET', 'WLT_' + Date.now());
            });
        } else if (method === 'emi') {
            return apiService.post('/api/payments/process-emi', null, {
                orderId: orderId,
                amount: amount,
                cardNumber: (paymentData.cardNumber || '').replace(/\s/g, ''),
                emiTenure: paymentData.emiTenure || 3,
                customerEmail: customerEmail,
                customerName: customerName
            })
            .then(function(res) { return res.data; })
            .catch(function() {
                return self._mockPaymentSuccess(orderId, 'CARD', 'EMI_' + Date.now());
            });
        } else {
            // COD — no upfront payment; transaction assigned on delivery
            return apiService.post('/api/payments/process-offline', null, {
                orderId: orderId,
                amount: amount,
                paymentMode: 'COD',
                customerEmail: customerEmail,
                customerName: customerName
            })
            .then(function(res) { return res.data; })
            .catch(function() {
                return self._mockPaymentSuccess(orderId, 'COD', 'COD_PENDING_' + orderId);
            });
        }
    };

    // Internal mock helper
    this._mockPaymentSuccess = function(orderId, mode, ref) {
        var mockPayments = JSON.parse(localStorage.getItem('ekMockPayments') || '[]');
        var mockPayment = {
            paymentId: Date.now(),
            orderId: orderId,
            paymentMode: mode,
            transactionRef: ref,
            paymentStatus: mode === 'COD' ? 'PENDING' : 'SUCCESS',
            paymentDate: new Date().toISOString()
        };
        mockPayments.push(mockPayment);
        localStorage.setItem('ekMockPayments', JSON.stringify(mockPayments));
        return mockPayment;
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
