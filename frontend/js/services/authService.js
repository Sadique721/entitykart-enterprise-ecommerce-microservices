/**
 * Authentication Service
 */
app.service('authService', ['apiService', '$rootScope', function(apiService, $rootScope) {
    var currentUser = null;
    var token = null;

    function isTokenExpired(token) {
        if (!token) return true;
        try {
            var base64Url = token.split('.')[1];
            var base64 = base64Url.replace(/-/g, '+').replace(/_/g, '/');
            var jsonPayload = decodeURIComponent(atob(base64).split('').map(function(c) {
                return '%' + ('00' + c.charCodeAt(0).toString(16)).slice(-2);
            }).join(''));
            var payload = JSON.parse(jsonPayload);
            if (payload && payload.exp) {
                var now = Math.floor(Date.now() / 1000);
                return payload.exp < now;
            }
            return false;
        } catch (e) {
            return true;
        }
    }

    this.init = function() {
        var savedToken = localStorage.getItem('ekToken');
        var savedUser = localStorage.getItem('ekUser');
        var savedRefreshToken = localStorage.getItem('ekRefreshToken');
        
        if (savedToken) {
            if (isTokenExpired(savedToken)) {
                if (savedRefreshToken) {
                    this.refreshToken().catch(function() {
                        this.logout();
                    }.bind(this));
                } else {
                    this.logout();
                }
            } else {
                token = savedToken;
                try {
                    currentUser = JSON.parse(savedUser);
                } catch (e) {
                    currentUser = null;
                }
            }
        }
    };

    this.register = function(userData) {
        return apiService.post('/api/users/register', userData)
            .then(function(response) {
                return response.data;
            });
    };

    this.login = function(credentials) {
        return apiService.post('/api/users/login', credentials)
            .then(function(response) {
                token = response.data.token;
                currentUser = {
                    id: response.data.userId,
                    name: response.data.name,
                    email: response.data.email,
                    role: response.data.role,
                    profilePicURL: response.data.profilePicURL
                };
                localStorage.setItem('ekToken', token);
                localStorage.setItem('ekUser', JSON.stringify(currentUser));
                if (response.data.refreshToken) {
                    localStorage.setItem('ekRefreshToken', response.data.refreshToken);
                }
                $rootScope.$broadcast('auth:login', currentUser);
                return currentUser;
            })
            .catch(function(error) {
                var msg = (error.data && error.data.message) ? error.data.message : 'Invalid email or password.';
                throw { data: { message: msg } };
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

    this.updateCurrentUser = function(user) {
        currentUser = {
            id: user.id,
            name: user.name,
            email: user.email,
            role: user.role,
            gender: user.gender,
            contactNum: user.contactNum,
            profilePicURL: user.profilePicURL
        };
        localStorage.setItem('ekUser', JSON.stringify(currentUser));
        $rootScope.$broadcast('auth:login', currentUser);
    };

    this.fetchProfile = function() {
        if (!currentUser || !currentUser.id) return Promise.resolve(null);
        return apiService.get('/api/users/' + currentUser.id)
            .then(function(response) {
                var user = response.data;
                currentUser = {
                    id: user.id,
                    name: user.name,
                    email: user.email,
                    role: user.role,
                    gender: user.gender,
                    contactNum: user.contactNum,
                    profilePicURL: user.profilePicURL
                };
                localStorage.setItem('ekUser', JSON.stringify(currentUser));
                $rootScope.$broadcast('auth:login', currentUser);
                return currentUser;
            });
    };

    this.updateProfileApi = function(payload) {
        return apiService.put('/api/users/' + payload.id, payload)
            .then(function(response) {
                return response.data;
            });
    };


    this.logout = function() {
        token = null;
        currentUser = null;
        localStorage.removeItem('ekToken');
        localStorage.removeItem('ekUser');
        localStorage.removeItem('ekRefreshToken');
        $rootScope.$broadcast('auth:logout');
    };

    this.refreshToken = function() {
        var rToken = localStorage.getItem('ekRefreshToken');
        if (!rToken) {
            return Promise.reject('No refresh token available');
        }
        return apiService.post('/api/users/refresh-token', { refreshToken: rToken })
            .then(function(response) {
                token = response.data.token;
                currentUser = {
                    id: response.data.userId,
                    name: response.data.name,
                    email: response.data.email,
                    role: response.data.role,
                    profilePicURL: response.data.profilePicURL
                };
                localStorage.setItem('ekToken', token);
                localStorage.setItem('ekUser', JSON.stringify(currentUser));
                if (response.data.refreshToken) {
                    localStorage.setItem('ekRefreshToken', response.data.refreshToken);
                }
                $rootScope.$broadcast('auth:login', currentUser);
                return token;
            });
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

    $rootScope.$on('auth:logout', function() {
        token = null;
        currentUser = null;
    });
}]);
