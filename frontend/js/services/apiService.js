/**
 * API Helper Service and Global HTTP Interceptor
 */
app.factory('apiInterceptor', ['$rootScope', '$q', 'API_BASE', function($rootScope, $q, API_BASE) {
    var activeRequests = 0;

    function showLoading() {
        if (activeRequests === 0) {
            $rootScope.$broadcast('loading:show');
        }
        activeRequests++;
    }

    function hideLoading() {
        activeRequests--;
        if (activeRequests <= 0) {
            activeRequests = 0;
            $rootScope.$broadcast('loading:hide');
        }
    }

    return {
        request: function(config) {
            showLoading();
            // Inject JWT Token from localStorage if exists
            var token = localStorage.getItem('ekToken');
            if (token) {
                config.headers['Authorization'] = 'Bearer ' + token;
            }
            return config;
        },
        response: function(response) {
            hideLoading();
            return response;
        },
        responseError: function(rejection) {
            hideLoading();

            // --- BUG FIX: Do NOT show generic API error for 401/403/network issues already handled below ---
            var errorTitle = 'API Error';
            var errorMsg = 'An unexpected error occurred.';

            if (rejection.status === 401) {
                // Auth expired — clear session and redirect without duplicate error toast
                localStorage.removeItem('ekToken');
                localStorage.removeItem('ekUser');
                $rootScope.$broadcast('auth:logout');
                $rootScope.$broadcast('showToast', {
                    title: 'Session Expired',
                    message: 'Your session has expired. Please sign in again.',
                    type: 'error'
                });
                return $q.reject(rejection);
            }

            if (rejection.status === 403) {
                $rootScope.$broadcast('showToast', {
                    title: 'Access Denied',
                    message: 'You do not have permission to perform this action.',
                    type: 'error'
                });
                return $q.reject(rejection);
            }

            // --- For all other errors, show a generic toast ---
            if (rejection.data) {
                if (typeof rejection.data === 'string') {
                    errorMsg = rejection.data;
                } else if (rejection.data.message) {
                    errorMsg = rejection.data.message;
                }
            } else if (rejection.status === -1) {
                // Network/Gateway offline — do NOT spam toast, just reject
                return $q.reject(rejection);
            }

            $rootScope.$broadcast('showToast', {
                title: errorTitle,
                message: errorMsg,
                type: 'error'
            });

            return $q.reject(rejection);
        }
    };
}]);

app.service('apiService', ['$http', 'API_BASE', function($http, API_BASE) {

    this.get = function(url, params) {
        return $http({
            method: 'GET',
            url: API_BASE + url,
            params: params
        });
    };

    this.post = function(url, data, params) {
        return $http({
            method: 'POST',
            url: API_BASE + url,
            data: data,
            params: params
        });
    };

    this.put = function(url, data, params) {
        return $http({
            method: 'PUT',
            url: API_BASE + url,
            data: data,
            params: params
        });
    };

    // --- BUG FIX: Added missing PATCH method (used by orderService for return decisions) ---
    this.patch = function(url, data, params) {
        return $http({
            method: 'PATCH',
            url: API_BASE + url,
            data: data,
            params: params
        });
    };

    this.delete = function(url, params) {
        return $http({
            method: 'DELETE',
            url: API_BASE + url,
            params: params
        });
    };
}]);
