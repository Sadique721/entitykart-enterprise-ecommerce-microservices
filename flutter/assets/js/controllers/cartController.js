/**
 * Cart and Checkout Flow Controller
 */
app.controller('cartController', [
    '$scope', '$location', 'cartService', 'authService', 'orderService', 'apiService',
    function($scope, $location, cartService, authService, orderService, apiService) {

        // === Address Management ===
        $scope.userAddresses = [];
        $scope.selectedAddressId = null;
        $scope.showAddAddressForm = false;  // to toggle add address form
        $scope.newAddress = {};

        // === Cart Data ===
        $scope.cartItems = [];
        $scope.cartTotal = 0.00;

        // === Payment Data (same as before) ===
        $scope.paymentData = {
            cardName: '',
            cardNumber: '',
            expiry: '',
            cvv: ''
        };

        // ========== Load Addresses from Backend ==========
        $scope.loadAddresses = function() {
            if (!authService.isLoggedIn()) return;
            apiService.get('/api/users/addresses')
                .then(function(res) {
                    $scope.userAddresses = res.data;
                    // Auto-select default address if exists
                    var defaultAddr = $scope.userAddresses.find(a => a.isDefault === true);
                    if (defaultAddr) {
                        $scope.selectedAddressId = defaultAddr.id;
                    } else if ($scope.userAddresses.length > 0) {
                        $scope.selectedAddressId = $scope.userAddresses[0].id;
                    } else {
                        $scope.selectedAddressId = null;
                    }
                })
                .catch(function(err) {
                    console.error('Failed to load addresses', err);
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
                isDefault: $scope.userAddresses.length === 0  // first address becomes default
            };

            apiService.post('/api/users/addresses', addressData)
                .then(function(res) {
                    $scope.userAddresses.push(res.data);
                    if (res.data.isDefault) $scope.selectedAddressId = res.data.id;
                    $scope.showAddAddressForm = false;
                    $scope.newAddress = {};
                    $scope.$emit('showToast', {
                        title: 'Address Added',
                        message: 'New shipping address saved successfully.',
                        type: 'success'
                    });
                })
                .catch(function(err) {
                    $scope.$emit('showToast', {
                        title: 'Error',
                        message: 'Failed to add address. Please try again.',
                        type: 'error'
                    });
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
            cartService.getCart().then(function(items) {
                $scope.cartItems = items;
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
                    $scope.$emit('showToast', {
                        title: 'Item Removed',
                        message: item.productName + ' was removed from your cart.',
                        type: 'info'
                    });
                    $scope.loadCartData();
                });
        };

        $scope.clearCart = function() {
            cartService.clearCart()
                .then(function() {
                    $scope.$emit('showToast', {
                        title: 'Cart Cleared',
                        message: 'All items have been removed.',
                        type: 'info'
                    });
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

            // Load addresses first
            $scope.loadAddresses();

            // Check if cart is empty
            cartService.getCart().then(function(items) {
                if (items.length === 0) {
                    $scope.$emit('showToast', {
                        title: 'Checkout Restrained',
                        message: 'Your cart is empty. Add products first.',
                        type: 'error'
                    });
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
            // Check if address is selected
            if (!$scope.selectedAddressId) {
                $scope.$emit('showToast', {
                    title: 'Address Required',
                    message: 'Please select or add a shipping address.',
                    type: 'error'
                });
                return;
            }

            // Validate payment info
            var pmt = $scope.paymentData;
            if (!pmt.cardName || !pmt.cardNumber || !pmt.expiry || !pmt.cvv) {
                $scope.$emit('showToast', {
                    title: 'Payment Information Incomplete',
                    message: 'Please fill all payment fields.',
                    type: 'error'
                });
                return;
            }

            // Proceed to checkout with selected addressId
            cartService.checkout($scope.selectedAddressId)
                .then(function(order) {
                    $scope.$emit('showToast', {
                        title: 'Order Placed!',
                        message: 'Your order #' + (order.orderId || '') + ' was successfully registered.',
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