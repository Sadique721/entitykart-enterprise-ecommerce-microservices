/**
 * EntityKart AngularJS Application Config
 * v3.0.0 — Local Network Only (no Render/cloud fallback)
 *
 * APK: MainActivity discovers gateway IP on local WiFi and injects it.
 *      The app waits on a loading screen until gateway is found.
 * Browser: Uses localhost:9900 (docker-compose gateway).
 */
var app = angular.module('entitykartApp', ['ngRoute']);

// Gateway port — same across local + docker environments
var LOCAL_GATEWAY_PORT = '9900';

app.constant('API_BASE', (function () {
    if (typeof window !== 'undefined') {
        var protocol = window.location.protocol;
        var host     = window.location.hostname;

        // ── Mobile WebView / APK Context ──────────────────────────────────────
        // MainActivity shows a loading screen, discovers the LAN gateway,
        // then injects the URL via AndroidBridge and loads this page.
        // By the time this runs, AndroidBridge.getApiBase() should return the URL.
        if (protocol === 'file:' ||
            (window.AndroidConfig && window.AndroidConfig.apiBase) ||
            (window.AndroidBridge && typeof window.AndroidBridge.getApiBase === 'function')) {

            // 1. AndroidBridge — set by MainActivity after LAN discovery
            if (window.AndroidBridge && typeof window.AndroidBridge.getApiBase === 'function') {
                var bridgeUrl = window.AndroidBridge.getApiBase();
                if (bridgeUrl && bridgeUrl.trim().length > 0) { return bridgeUrl; }
            }
            // 2. AndroidConfig (legacy)
            if (window.AndroidConfig && window.AndroidConfig.apiBase) {
                return window.AndroidConfig.apiBase;
            }
            // 3. Window global injected by WebViewClient.onPageFinished
            if (window.ENTITYKART_API_BASE && window.ENTITYKART_API_BASE.trim().length > 0) {
                return window.ENTITYKART_API_BASE;
            }
            // 4. localStorage — persisted from last successful session by MainActivity
            try {
                var savedIp   = localStorage.getItem('API_IP');
                var savedPort = localStorage.getItem('API_PORT') || LOCAL_GATEWAY_PORT;
                if (savedIp && savedIp.trim().length > 0) {
                    return 'http://' + savedIp + ':' + savedPort;
                }
            } catch (e) { /* ignore storage errors in restricted contexts */ }

            // 5. Gateway not yet found — MainActivity is still scanning
            //    Return an obvious placeholder; will be overridden by injectApiBase()
            return 'http://0.0.0.0:' + LOCAL_GATEWAY_PORT;
        }

        // ── Web Browser Mode (Development) ───────────────────────────────────
        if (host === 'localhost' || host === '127.0.0.1') {
            // docker-compose gateway always runs on 9900
            var activePort = localStorage.getItem('API_PORT') || LOCAL_GATEWAY_PORT;
            return protocol + '//' + host + ':' + activePort;
        }

        // ── Production (deployed behind Nginx) ────────────────────────────────
        return protocol + '//' + window.location.host;
    }
    return 'http://localhost:' + LOCAL_GATEWAY_PORT;
})());

// Route Configurations
app.config(['$routeProvider', '$httpProvider', function ($routeProvider, $httpProvider) {

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
app.run(['$rootScope', '$location', 'authService', function ($rootScope, $location, authService) {
    // Restore auth state from localStorage BEFORE any route guard fires.
    authService.init();

    // Pages that are always publicly accessible — never redirect these
    var publicPages = ['/login', '/register', '/forgot-password', '/reset-password', '/products', '/product', '/'];

    // Pages that require a logged-in user
    var restrictedPages = ['/checkout', '/orders', '/returns', '/wishlist', '/admin', '/profile'];

    $rootScope.$on('$routeChangeStart', function (event, next, current) {
        var path = $location.path();

        // --- Guard: Never block access to public pages ---
        var isPublic = publicPages.some(function (page) {
            return path === page || path.indexOf(page) === 0;
        });
        if (isPublic) {
            return;
        }

        // --- Guard: Restricted pages need authentication ---
        var isRestricted = restrictedPages.some(function (page) {
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
