/**
 * Authentication Service
 */
app.service('authService', ['apiService', '$rootScope', function(apiService, $rootScope) {
    var currentUser = null;
    var token = null;

    this.init = function() {
        var savedToken = localStorage.getItem('ekToken');
        var savedUser = localStorage.getItem('ekUser');
        if (savedToken && savedUser) {
            token = savedToken;
            try {
                currentUser = JSON.parse(savedUser);
            } catch (e) {
                currentUser = null;
            }
        }
    };

    this.register = function(userData) {
        return apiService.post('/api/users/register', userData)
            .then(function(response) {
                // If registration succeeds, save user in our local mock database for easy demo logins
                var mockDb = JSON.parse(localStorage.getItem('ekMockUsers') || '[]');
                mockDb.push({
                    id: response.data.id || Date.now(),
                    name: userData.name,
                    email: userData.email,
                    password: userData.password,
                    role: userData.role || 'USER'
                });
                localStorage.setItem('ekMockUsers', JSON.stringify(mockDb));
                return response.data;
            });
    };

    this.login = function(credentials) {
        // Try calling the gateway backend first, in case user-service or gateway auth is configured
        return apiService.post('/api/users/login', credentials)
            .then(function(response) {
                token = response.data.token;
                currentUser = {
                    id: response.data.userId,
                    name: response.data.name,
                    email: response.data.email,
                    role: response.data.role
                };
                localStorage.setItem('ekToken', token);
                localStorage.setItem('ekUser', JSON.stringify(currentUser));
                $rootScope.$broadcast('auth:login', currentUser);
                return currentUser;
            })
            .catch(function(error) {
                // FALLBACK MOCK AUTHENTICATION: Keep prototype fully functional without login endpoints
                console.warn('Real login API not available. Using local mock authentication.');
                
                // Check local mock database
                var mockDb = JSON.parse(localStorage.getItem('ekMockUsers') || '[]');
                
                // Add default admin and user account for testing convenience
                if (mockDb.length === 0) {
                    mockDb = [
                        { id: 1, name: 'Demo Customer', email: 'customer@example.com', password: 'password', role: 'USER' },
                        { id: 2, name: 'System Admin', email: 'admin@example.com', password: 'adminpassword', role: 'ADMIN' }
                    ];
                    localStorage.setItem('ekMockUsers', JSON.stringify(mockDb));
                }

                var user = mockDb.find(function(u) {
                    return u.email === credentials.email && u.password === credentials.password;
                });

                if (user) {
                    token = 'mock-jwt-token-for-user-' + user.id;
                    currentUser = {
                        id: user.id,
                        name: user.name,
                        email: user.email,
                        role: user.role
                    };
                    localStorage.setItem('ekToken', token);
                    localStorage.setItem('ekUser', JSON.stringify(currentUser));
                    $rootScope.$broadcast('auth:login', currentUser);
                    
                    // Show a toast that it's a mock login
                    $rootScope.$broadcast('showToast', {
                        title: 'Logged In (Mock)',
                        message: 'Welcome ' + user.name + '! Role: ' + user.role,
                        type: 'info'
                    });
                    
                    return currentUser;
                } else {
                    throw { data: { message: 'Invalid email or password.' } };
                }
            });
    };

    this.forgotPassword = function(email) {
        return apiService.post('/api/users/forgot-password?email=' + encodeURIComponent(email));
    };

    this.resetPassword = function(token, newPassword) {
        return apiService.post('/api/users/reset-password', {
            token: token,
            newPassword: newPassword
        });
    };

    this.logout = function() {
        token = null;
        currentUser = null;
        localStorage.removeItem('ekToken');
        localStorage.removeItem('ekUser');
        $rootScope.$broadcast('auth:logout');
    };

    this.isLoggedIn = function() {
        return currentUser !== null;
    };

    this.isAdmin = function() {
        return currentUser && currentUser.role === 'ADMIN';
    };

    this.getCurrentUser = function() {
        return currentUser;
    };

    this.getToken = function() {
        return token;
    };
}]);
