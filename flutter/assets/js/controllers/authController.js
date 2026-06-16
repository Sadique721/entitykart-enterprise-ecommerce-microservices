/**
 * User Authentication Controller
 * Handles login, registration, forgot-password, and reset-password flows.
 */
app.controller('authController', ['$scope', '$location', 'authService', function($scope, $location, authService) {

    // If a user is already authenticated, redirect them to home (or admin dashboard).
    // This prevents showing the login/register page to users who are already signed in.
    // NOTE: This fires AFTER authService.init() in app.run(), so the state is reliable.
    if (authService.isLoggedIn()) {
        if (authService.isAdmin()) {
            $location.path('/admin');
        } else {
            $location.path('/');
        }
        return; // Stop controller initialization — page will redirect
    }


    $scope.loginData = {
        email: '',
        password: ''
    };

    $scope.registerData = {
        name: '',
        email: '',
        password: '',
        role: 'USER' // Defaults to customer
    };

    $scope.errorMessage = '';

    $scope.login = function() {
        $scope.errorMessage = '';
        authService.login($scope.loginData)
            .then(function(user) {
                $scope.$emit('showToast', {
                    title: 'Welcome Back',
                    message: 'Successfully logged in as ' + user.name,
                    type: 'success'
                });
                if (user.role === 'ADMIN') {
                    $location.path('/admin');
                } else {
                    $location.path('/');
                }
            })
            .catch(function(err) {
                $scope.errorMessage = err.data ? err.data.message : 'Invalid login credentials.';
                $scope.$emit('showToast', {
                    title: 'Login Failed',
                    message: $scope.errorMessage,
                    type: 'error'
                });
            });
    };

    $scope.register = function() {
        $scope.errorMessage = '';
        authService.register($scope.registerData)
            .then(function(response) {
                $scope.$emit('showToast', {
                    title: 'Registration Successful',
                    message: 'Your account has been created. Please log in.',
                    type: 'success'
                });
                $location.path('/login');
            })
            .catch(function(err) {
                $scope.errorMessage = err.data ? err.data.message : 'Registration failed. Try again.';
                $scope.$emit('showToast', {
                    title: 'Registration Failed',
                    message: $scope.errorMessage,
                    type: 'error'
                });
            });
    };

    $scope.forgotEmail = '';
    $scope.resetData = {
        token: '',
        newPassword: ''
    };

    $scope.forgotPassword = function() {
        authService.forgotPassword($scope.forgotEmail)
            .then(function() {
                $scope.$emit('showToast', {
                    title: 'Token Sent',
                    message: 'Verification token sent to your email.',
                    type: 'success'
                });
                $location.path('/reset-password');
            })
            .catch(function(err) {
                var msg = err.data ? err.data.message : 'Failed to send token.';
                $scope.$emit('showToast', {
                    title: 'Request Failed',
                    message: msg,
                    type: 'error'
                });
            });
    };

    $scope.resetPassword = function() {
        authService.resetPassword($scope.resetData.token, $scope.resetData.newPassword)
            .then(function() {
                $scope.$emit('showToast', {
                    title: 'Password Updated',
                    message: 'Your password has been reset successfully.',
                    type: 'success'
                });
                $location.path('/login');
            })
            .catch(function(err) {
                var msg = err.data ? err.data.message : 'Reset failed. Check token validity.';
                $scope.$emit('showToast', {
                    title: 'Reset Failed',
                    message: msg,
                    type: 'error'
                });
            });
    };
}]);
