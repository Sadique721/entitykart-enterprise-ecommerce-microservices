/**
 * Cart and Checkout Flow Controller
 * v1.5.0 — Bug fixes: payment validation per method, + coupon code, save-for-later
 */
app.controller('cartController', [
    '$scope', '$location', 'cartService', 'authService', 'orderService', 'apiService',
    function($scope, $location, cartService, authService, orderService, apiService) {

        // === Address Management ===
        $scope.userAddresses = [];
        $scope.selectedAddressId = null;
        $scope.showAddAddressForm = false;
        $scope.newAddress = {};

        // === Cart Data ===
        $scope.cartItems = [];
        $scope.cartTotal = 0.00;
        $scope.cartLoading = false;

        // === Coupon Code ===
        $scope.couponCode = '';
        $scope.couponApplied = null;
        $scope.couponDiscount = 0;
        $scope.availableCoupons = [
            { code: 'WELCOME10', discount: 10, type: 'percent', description: '10% off on first order' },
            { code: 'SAVE100', discount: 100, type: 'flat', description: '₹100 flat off on orders above ₹999' },
            { code: 'FLASH20', discount: 20, type: 'percent', description: '20% off — Flash Sale special' }
        ];

        // === Payment Data ===
        $scope.paymentData = {
            cardName: '',
            cardNumber: '',
            expiry: '',
            cvv: '',
            upiId: ''
        };

        // ========== Coupon Functions ==========
        $scope.applyCoupon = function() {
            var code = ($scope.couponCode || '').toUpperCase().trim();
            if (!code) {
                $scope.$emit('showToast', { title: 'Enter Coupon', message: 'Please enter a coupon code.', type: 'error' });
                return;
            }
            var found = $scope.availableCoupons.find(function(c) { return c.code === code; });
            if (!found) {
                $scope.$emit('showToast', { title: 'Invalid Coupon', message: 'Coupon code "' + code + '" is not valid.', type: 'error' });
                return;
            }
            // Check minimum order for flat discount
            if (found.code === 'SAVE100' && $scope.cartTotal < 999) {
                $scope.$emit('showToast', { title: 'Minimum Order Required', message: 'SAVE100 requires a minimum cart value of ₹999.', type: 'error' });
                return;
            }
            $scope.couponApplied = found;
            if (found.type === 'percent') {
                $scope.couponDiscount = Math.round($scope.cartTotal * found.discount / 100);
            } else {
                $scope.couponDiscount = found.discount;
            }
            $scope.$emit('showToast', { title: 'Coupon Applied! 🎉', message: found.description + '. You save ₹' + $scope.couponDiscount + '!', type: 'success' });
        };

        $scope.removeCoupon = function() {
            $scope.couponApplied = null;
            $scope.couponDiscount = 0;
            $scope.couponCode = '';
            $scope.$emit('showToast', { title: 'Coupon Removed', message: 'Coupon has been removed.', type: 'info' });
        };

        $scope.getFinalTotal = function() {
            return Math.max(0, ($scope.cartTotal * 1.18) - $scope.couponDiscount);
        };

        // ========== Load Addresses from Backend ==========
        $scope.loadAddresses = function() {
            if (!authService.isLoggedIn()) return;
            apiService.get('/api/users/addresses')
                .then(function(res) {
                    $scope.userAddresses = res.data;
                    var defaultAddr = $scope.userAddresses.find(function(a) { return a.isDefault === true; });
                    if (defaultAddr) {
                        $scope.selectedAddressId = defaultAddr.id;
                    } else if ($scope.userAddresses.length > 0) {
                        $scope.selectedAddressId = $scope.userAddresses[0].id;
                    } else {
                        $scope.selectedAddressId = null;
                    }
                })
                .catch(function(err) {
                    console.warn('Failed to load addresses from backend. Using empty list.', err);
                });
        };

        // ========== Add New Address ==========
        $scope.addNewAddress = function() {
            var user = authService.getCurrentUser();
            if (!user) return;

            var addressData = {
                userId: user.id,
                fullName: $scope.newAddress.fullName,
                phone: $scope.newAddress.phone,
                streetAddress: $scope.newAddress.streetAddress,
                city: $scope.newAddress.city,
                state: $scope.newAddress.state,
                zipCode: $scope.newAddress.zipCode,
                isDefault: $scope.userAddresses.length === 0
            };

            apiService.post('/api/users/addresses', addressData)
                .then(function(res) {
                    $scope.userAddresses.push(res.data);
                    if (res.data.isDefault) $scope.selectedAddressId = res.data.id;
                    $scope.showAddAddressForm = false;
                    $scope.newAddress = {};
                    $scope.$emit('showToast', { title: 'Address Added', message: 'New shipping address saved successfully.', type: 'success' });
                })
                .catch(function() {
                    // Mock fallback for demo
                    var mockAddr = angular.copy(addressData);
                    mockAddr.id = Date.now();
                    mockAddr.isDefault = $scope.userAddresses.length === 0;
                    $scope.userAddresses.push(mockAddr);
                    $scope.selectedAddressId = mockAddr.id;
                    $scope.showAddAddressForm = false;
                    $scope.newAddress = {};
                    $scope.$emit('showToast', { title: 'Address Added', message: 'New shipping address saved.', type: 'success' });
                });
        };

        // ========== Cart Functions ==========
        $scope.initCart = function() {
            if (!authService.isLoggedIn()) {
                $location.path('/login');
                return;
            }
            $scope.loadCartData();
        };

        $scope.loadCartData = function() {
            $scope.cartLoading = true;
            cartService.getCart().then(function(items) {
                $scope.cartItems = items;
                $scope.cartLoading = false;
            });
            cartService.getCartTotal().then(function(total) {
                $scope.cartTotal = total;
            });
        };

        $scope.adjustQuantity = function(item, amount) {
            var newQty = item.quantity + amount;
            if (newQty >= 1) {
                cartService.updateQuantity(item.productId, newQty)
                    .then(function() {
                        $scope.loadCartData();
                    });
            }
        };

        $scope.removeItem = function(item) {
            cartService.removeItem(item.productId)
                .then(function() {
                    $scope.$emit('showToast', { title: 'Item Removed', message: item.productName + ' was removed from your cart.', type: 'info' });
                    $scope.loadCartData();
                });
        };

        // Save for Later — moves item from cart to wishlist
        $scope.saveForLater = function(item) {
            cartService.removeItem(item.productId).then(function() {
                // Add to wishlist service
                apiService.post('/api/wishlist/add', null, { productId: item.productId })
                    .catch(function() {
                        // Mock: store locally
                        var wl = JSON.parse(localStorage.getItem('ekWishlistLocal') || '[]');
                        if (!wl.find(function(w) { return w.productId === item.productId; })) {
                            wl.push({ productId: item.productId, productName: item.productName, price: item.price });
                            localStorage.setItem('ekWishlistLocal', JSON.stringify(wl));
                        }
                    });
                $scope.$emit('showToast', { title: 'Saved for Later', message: item.productName + ' moved to your wishlist.', type: 'success' });
                $scope.loadCartData();
            });
        };

        $scope.clearCart = function() {
            cartService.clearCart()
                .then(function() {
                    $scope.$emit('showToast', { title: 'Cart Cleared', message: 'All items have been removed.', type: 'info' });
                    $scope.couponApplied = null;
                    $scope.couponDiscount = 0;
                    $scope.loadCartData();
                });
        };

        // ========== Checkout Flow ==========
        $scope.initCheckout = function() {
            if (!authService.isLoggedIn()) {
                $location.path('/login');
                return;
            }

            $scope.checkoutStep = 1;
            $scope.paymentMethod = 'card';
            $scope.loadAddresses();

            cartService.getCart().then(function(items) {
                if (items.length === 0) {
                    $scope.$emit('showToast', { title: 'Cart Empty', message: 'Your cart is empty. Add products first.', type: 'error' });
                    $location.path('/products');
                } else {
                    $scope.cartItems = items;
                    cartService.getCartTotal().then(function(total) {
                        $scope.cartTotal = total;
                    });
                }
            });
        };

        // ========== SINGLE submitCheckout Function ==========
        $scope.submitCheckout = function() {
            // Check address
            if (!$scope.selectedAddressId) {
                $scope.$emit('showToast', { title: 'Address Required', message: 'Please select or add a shipping address.', type: 'error' });
                return;
            }

            // --- BUG FIX: Validate payment fields only for the selected payment method ---
            var pmt = $scope.paymentData;
            if ($scope.paymentMethod === 'card') {
                if (!pmt.cardName || !pmt.cardNumber || !pmt.expiry || !pmt.cvv) {
                    $scope.$emit('showToast', { title: 'Card Details Incomplete', message: 'Please fill in all card details.', type: 'error' });
                    return;
                }
                // Basic card number validation (16 digits)
                var cleanCard = pmt.cardNumber.replace(/\s/g, '');
                if (cleanCard.length < 13 || cleanCard.length > 19 || !/^\d+$/.test(cleanCard)) {
                    $scope.$emit('showToast', { title: 'Invalid Card', message: 'Please enter a valid card number.', type: 'error' });
                    return;
                }
            } else if ($scope.paymentMethod === 'upi') {
                if (!pmt.upiId || pmt.upiId.indexOf('@') === -1) {
                    $scope.$emit('showToast', { title: 'UPI ID Required', message: 'Please enter a valid UPI ID (e.g., name@upi).', type: 'error' });
                    return;
                }
            }
            // COD: no additional validation needed

            cartService.checkout($scope.selectedAddressId)
                .then(function(order) {
                    $scope.$emit('showToast', {
                        title: '🎉 Order Placed Successfully!',
                        message: 'Order #' + (order.orderId || '') + ' confirmed. You\'ll receive a confirmation email shortly.',
                        type: 'success'
                    });
                    $location.path('/orders');
                })
                .catch(function(err) {
                    $scope.$emit('showToast', {
                        title: 'Checkout Failed',
                        message: err.data ? err.data.message : 'Please check your inputs and try again.',
                        type: 'error'
                    });
                });
        };
    }
]);