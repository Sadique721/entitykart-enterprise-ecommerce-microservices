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

    // Human-readable messages for common HTTP status codes
    function getStatusMessage(status) {
        switch (status) {
            case 400: return 'Invalid request. Please check your input and try again.';
            case 401: return 'Session expired. Please sign in again.';
            case 403: return 'You do not have permission to perform this action.';
            case 404: return 'The requested resource was not found.';
            case 409: return 'A conflict occurred. The record may already exist.';
            case 422: return 'Validation failed. Please check your input.';
            case 429: return 'Too many requests. Please wait a moment and try again.';
            case 500: return 'Internal server error. Our team has been notified.';
            case 502: return 'Service is starting up on Render. Please wait 30-60 seconds and try again.';
            case 503: return 'Service temporarily unavailable. Please wait and retry.';
            case 504: return 'Request timed out. The service may be overloaded — please retry.';
            default:  return 'An unexpected error occurred. Please try again.';
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

            var status = rejection.status;

            // ── 401: Session expired ─────────────────────────────────────────
            if (status === 401) {
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

            // ── 403: Forbidden ───────────────────────────────────────────────
            if (status === 403) {
                $rootScope.$broadcast('showToast', {
                    title: 'Access Denied',
                    message: 'You do not have permission to perform this action.',
                    type: 'error'
                });
                return $q.reject(rejection);
            }

            // ── Network offline / CORS blocked ───────────────────────────────
            if (status === -1) {
                // Silently reject — avoid spamming toasts on slow/offline networks
                return $q.reject(rejection);
            }

            // ── 502/503/504: Backend service starting up (Render cold start) ─
            if (status === 502 || status === 503 || status === 504) {
                $rootScope.$broadcast('showToast', {
                    title: 'Service Starting',
                    message: getStatusMessage(status),
                    type: 'error'
                });
                return $q.reject(rejection);
            }

            // ── All other errors: extract message from response body ─────────
            var errorMsg = getStatusMessage(status);
            if (rejection.data) {
                if (typeof rejection.data === 'string' && rejection.data.trim().charAt(0) !== '<') {
                    // Only use raw string if it's not an HTML error page (like nginx 502 HTML)
                    errorMsg = rejection.data;
                } else if (rejection.data && rejection.data.message) {
                    errorMsg = rejection.data.message;
                } else if (rejection.data && rejection.data.error) {
                    errorMsg = rejection.data.error;
                }
            }

            $rootScope.$broadcast('showToast', {
                title: 'Error ' + (status > 0 ? status : ''),
                message: errorMsg,
                type: 'error'
            });

            return $q.reject(rejection);
        }
    };
}]);


app.service('apiService', ['$http', 'API_BASE', function($http, API_BASE) {

    this.get = function(url, params, headers) {
        return $http({
            method: 'GET',
            url: API_BASE + url,
            params: params,
            headers: headers
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
