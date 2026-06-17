/**
 * Entitykart AngularJS Application Config
 */
var app = angular.module('entitykartApp', ['ngRoute']);

// Base URL for the API Gateway – dynamically detected from current host
app.constant('API_BASE', (function() {
    if (typeof window !== 'undefined') {
        var protocol = window.location.protocol;
        var host = window.location.hostname;
        
        var savedIp = localStorage.getItem('API_IP');
        var savedPort = localStorage.getItem('API_PORT') || '9080';

        // Mobile WebView / APK context: check localStorage custom IP first
        if (protocol === 'file:' || (window.AndroidBridge && typeof window.AndroidBridge.getApiBase === 'function')) {
            if (savedIp && savedIp !== 'localhost' && savedIp !== '127.0.0.1' && savedIp.trim() !== '') {
                return 'http://' + savedIp.trim() + ':' + savedPort;
            }
            if (window.AndroidBridge && typeof window.AndroidBridge.getApiBase === 'function') {
                return window.AndroidBridge.getApiBase();
            }
            return 'http://192.168.1.6:9080'; // Default fallback IP
        }

        // Web browser mode: use localStorage configurations
        var defaultIp = 'localhost';
        var defaultPort = '9080';

        var activeIp = localStorage.getItem('API_IP') || defaultIp;
        var activePort = localStorage.getItem('API_PORT') || defaultPort;

        localStorage.setItem('API_IP', activeIp);
        localStorage.setItem('API_PORT', activePort);

        return window.location.protocol + '//' + (host === 'localhost' || host === '127.0.0.1' ? host : activeIp) + ':' + activePort;
    }
    return 'http://localhost:9080';
})());

// Route Configurations
app.config(['$routeProvider', '$httpProvider', function($routeProvider, $httpProvider) {

    $routeProvider
        .when('/', {
            templateUrl: 'views/home.html',
            controller: 'productController'
        })
        .when('/products', {
            templateUrl: 'views/products.html',
            controller: 'productController'
        })
        .when('/product/:productId', {
            templateUrl: 'views/product-detail.html',
            controller: 'productController'
        })
        .when('/login', {
            templateUrl: 'views/login.html',
            controller: 'authController'
        })
        .when('/register', {
            templateUrl: 'views/register.html',
            controller: 'authController'
        })
        .when('/forgot-password', {
            templateUrl: 'views/forgot-password.html',
            controller: 'authController'
        })
        .when('/reset-password', {
            templateUrl: 'views/reset-password.html',
            controller: 'authController'
        })
        .when('/cart', {
            templateUrl: 'views/cart.html',
            controller: 'cartController'
        })
        .when('/checkout', {
            templateUrl: 'views/checkout.html',
            controller: 'cartController'
        })
        .when('/orders', {
            templateUrl: 'views/orders.html',
            controller: 'orderController'
        })
        .when('/returns', {
            templateUrl: 'views/returns.html',
            controller: 'returnController'
        })
        .when('/wishlist', {
            templateUrl: 'views/wishlist.html',
            controller: 'wishlistController'
        })
        .when('/admin', {
            templateUrl: 'views/admin.html',
            controller: 'adminController'
        })
        .when('/profile', {
            templateUrl: 'views/profile.html',
            controller: 'profileController'
        })
        .otherwise({
            redirectTo: '/'
        });

    // Add interceptor to pass authentication tokens and handle common response statuses globally
    $httpProvider.interceptors.push('apiInterceptor');
}]);

// Global app initialization
app.run(['$rootScope', '$location', 'authService', function($rootScope, $location, authService) {
    // IMPORTANT: Restore auth state from localStorage BEFORE any route guard fires.
    // This prevents the edge-case where a refreshed page sees the user as logged out
    // for a brief moment and incorrectly redirects login/register pages.
    authService.init();

    // Pages that are always publicly accessible — never redirect these
    var publicPages = ['/login', '/register', '/forgot-password', '/reset-password', '/products', '/product', '/'];

    // Pages that require a logged-in user
    var restrictedPages = ['/checkout', '/orders', '/returns', '/wishlist', '/admin', '/profile'];

    $rootScope.$on('$routeChangeStart', function(event, next, current) {
        var path = $location.path();

        // --- Guard: Never block access to public pages ---
        var isPublic = publicPages.some(function(page) {
            return path === page || path.indexOf(page) === 0;
        });
        if (isPublic) {
            return; // Always allow public pages through, no redirect
        }

        // --- Guard: Restricted pages need authentication ---
        var isRestricted = restrictedPages.some(function(page) {
            return path.indexOf(page) === 0;
        });

        if (isRestricted && !authService.isLoggedIn()) {
            event.preventDefault();
            $rootScope.$broadcast('showToast', {
                title: 'Login Required',
                message: 'Please sign in to access this page.',
                type: 'error'
            });
            $location.path('/login');
            return;
        }

        // --- Guard: Admin section needs ADMIN role ---
        if (path.indexOf('/admin') === 0 && !authService.isAdmin()) {
            event.preventDefault();
            $rootScope.$broadcast('showToast', {
                title: 'Access Denied',
                message: 'Admin privileges required.',
                type: 'error'
            });
            $location.path('/');
        }
    });
}]);

