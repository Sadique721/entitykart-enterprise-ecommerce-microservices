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
