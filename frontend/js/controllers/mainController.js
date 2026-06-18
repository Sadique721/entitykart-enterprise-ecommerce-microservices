/**
 * Main application controller managing global UI state, navigation, loading, and toast alerts.
 * v1.5.0 — Added hamburger menu, fixed search propagation
 */
app.controller('mainController', ['$scope', '$location', 'authService', 'cartService', function($scope, $location, authService, cartService) {
    
    // Core states
    $scope.userMenuOpen = false;
    $scope.loading = false;
    $scope.toasts = [];
    $scope.searchQuery = '';
    $scope.cartCount = 0;
    $scope.mobileMenuOpen = false;

    // Initialize cart count
    function updateCartCount() {
        if (authService.isLoggedIn()) {
            cartService.getCart().then(function(items) {
                var totalQty = 0;
                items.forEach(function(item) {
                    totalQty += item.quantity;
                });
                $scope.cartCount = totalQty;
            });
        } else {
            $scope.cartCount = 0;
        }
    }

    updateCartCount();

    // Listeners for global events
    $scope.$on('loading:show', function() {
        $scope.loading = true;
    });

    $scope.$on('loading:hide', function() {
        $scope.loading = false;
    });

    $scope.$on('cart:updated', function() {
        updateCartCount();
    });

    $scope.$on('auth:login', function() {
        updateCartCount();
    });

    $scope.$on('auth:logout', function() {
        $scope.cartCount = 0;
        $scope.userMenuOpen = false;
        $location.path('/');
    });

    $scope.$on('showToast', function(event, args) {
        $scope.addToast(args.title, args.message, args.type);
    });

    // Toast Management
    $scope.addToast = function(title, message, type) {
        // Prevent duplicate/spam toasts
        for (var i = 0; i < $scope.toasts.length; i++) {
            if ($scope.toasts[i].title === title && $scope.toasts[i].message === message) {
                return; // Duplicate toast already active, ignore
            }
        }

        var toast = {
            title: title,
            message: message,
            type: type || 'info' // success, error, info
        };
        $scope.toasts.push(toast);

        // Auto remove toast after 5 seconds
        setTimeout(function() {
            $scope.$apply(function() {
                var idx = $scope.toasts.indexOf(toast);
                if (idx > -1) {
                    $scope.toasts.splice(idx, 1);
                }
            });
        }, 5000);
    };

    $scope.removeToast = function(index) {
        $scope.toasts.splice(index, 1);
    };

    // User authentication shortcuts
    $scope.isLoggedIn = function() {
        return authService.isLoggedIn();
    };

    $scope.isAdmin = function() {
        return authService.isAdmin();
    };

    $scope.getUserName = function() {
        var user = authService.getCurrentUser();
        return user ? user.name : '';
    };

    $scope.getUserProfilePic = function() {
        var user = authService.getCurrentUser();
        return user ? user.profilePicURL : '';
    };

    $scope.getUserInitials = function() {
        var name = $scope.getUserName();
        if (!name) return 'U';
        var parts = name.split(' ');
        var initials = parts[0].charAt(0).toUpperCase();
        if (parts.length > 1) {
            initials += parts[parts.length - 1].charAt(0).toUpperCase();
        }
        return initials;
    };

    $scope.logout = function() {
        authService.logout();
        $scope.addToast('Logged Out', 'You have been successfully logged out.', 'success');
    };

    // UI helpers
    $scope.toggleUserMenu = function() {
        $scope.userMenuOpen = !$scope.userMenuOpen;
    };

    $scope.closeUserMenu = function() {
        $scope.userMenuOpen = false;
    };

    $scope.isActive = function(viewLocation) {
        return viewLocation === $location.path();
    };

    $scope.isLoading = function() {
        return $scope.loading;
    };

    $scope.getCartCount = function() {
        return $scope.cartCount;
    };

    $scope.searchCategory = '';
    $scope.searchProducts = function() {
        var params = {};
        if ($scope.searchQuery) {
            params.query = $scope.searchQuery;
        }
        if ($scope.searchCategory) {
            params.categoryId = $scope.searchCategory;
        }
        // BUG FIX: Store query in routeParams before clearing it so products page can read it
        $location.path('/products').search(params);
        // Only clear after navigation so the products controller can read $routeParams.query
        // $scope.searchQuery = '';  // Intentionally removed — causes race condition with route init
    };

    $scope.filterCategoryFromSearch = function() {
        if ($scope.searchCategory) {
            $scope.searchProducts();
        }
    };

    // Mobile hamburger menu
    $scope.toggleMobileMenu = function() {
        $scope.mobileMenuOpen = !$scope.mobileMenuOpen;
    };
    $scope.closeMobileMenu = function() {
        $scope.mobileMenuOpen = false;
    };

    $scope.isMobileWebView = function() {
        return (window.location.protocol === 'file:' || !!window.AndroidBridge);
    };

    // Backend environment selector
    $scope.backendMenuOpen = false;
    $scope.customIp = localStorage.getItem('API_IP') || '';

    $scope.toggleBackendMenu = function() {
        $scope.backendMenuOpen = !$scope.backendMenuOpen;
    };
    $scope.getActivePort = function() {
        return localStorage.getItem('API_PORT') || '9080';
    };
    $scope.getActiveHost = function() {
        var ip = localStorage.getItem('API_IP') || 'localhost';
        var port = localStorage.getItem('API_PORT') || '9080';
        return 'http://' + ip + ':' + port;
    };
    $scope.selectBackend = function(port) {
        localStorage.setItem('API_PORT', port);
        $scope.backendMenuOpen = false;
        $scope.addToast('Backend Switched', 'Switched API Gateway to port ' + port + ' (reloading...)', 'success');
        setTimeout(function() {
            window.location.reload();
        }, 1000);
    };
    $scope.saveCustomIp = function(ip) {
        if (!ip) return;
        localStorage.setItem('API_IP', ip);
        $scope.backendMenuOpen = false;
        $scope.addToast('IP Address Saved', 'Configured active host as ' + ip + ' (reloading...)', 'success');
        setTimeout(function() {
            window.location.reload();
        }, 1000);
    };
}]);
