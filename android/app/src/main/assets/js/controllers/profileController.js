/**
 * Profile Controller
 */
app.controller('profileController', [
    '$scope', '$rootScope', '$location', 'authService', 'productService', 'apiService',
    function($scope, $rootScope, $location, authService, productService, apiService) {
        
        $scope.profileData = {
            id: null,
            name: '',
            email: '',
            contactNum: '',
            gender: '',
            profilePicURL: '',
            role: '',
            active: true,
            password: ''
        };

        $scope.errorMessage = '';
        $scope.successMessage = '';
        $scope.isUploading = false;
        $scope.activeTab = 'profile';

        // Address Management States
        $scope.addresses = [];
        $scope.showAddressForm = false;
        $scope.isEditingAddress = false;
        $scope.addressData = {
            id: null,
            fullName: '',
            phone: '',
            streetAddress: '',
            city: '',
            state: '',
            zipCode: '',
            isDefault: false
        };

        $scope.initProfile = function() {
            if (!authService.isLoggedIn()) {
                $location.path('/login');
                return;
            }

            // Detect active tab from query search parameters (e.g. ?tab=addresses)
            var searchParams = $location.search();
            if (searchParams.tab) {
                $scope.activeTab = searchParams.tab;
            } else {
                $scope.activeTab = 'profile';
            }

            authService.fetchProfile().then(function(profile) {
                if (profile) {
                    $scope.profileData.id = profile.id;
                    $scope.profileData.name = profile.name;
                    $scope.profileData.email = profile.email;
                    $scope.profileData.contactNum = profile.contactNum || '';
                    $scope.profileData.gender = profile.gender || '';
                    $scope.profileData.profilePicURL = profile.profilePicURL || '';
                    $scope.profileData.role = profile.role;
                    $scope.profileData.active = profile.active;
                    $scope.profileData.password = '';
                }
                
                if ($scope.activeTab === 'addresses') {
                    $scope.loadAddresses();
                }
                $scope.$applyAsync();
            }).catch(function(err) {
                $scope.errorMessage = 'Failed to load user profile. Please check connection.';
                $scope.$applyAsync();
            });
        };

        $scope.setTab = function(tab) {
            $scope.activeTab = tab;
            $scope.errorMessage = '';
            $scope.successMessage = '';
            $location.search('tab', tab); // Update URL parameter
            
            if (tab === 'addresses') {
                $scope.loadAddresses();
            }
        };

        $scope.togglePasswordVis = function() {
            $scope.showPassword = !$scope.showPassword;
        };

        $scope.uploadProfilePic = function(element) {
            var file = element.files[0];
            if (!file) return;

            $scope.isUploading = true;
            $scope.errorMessage = '';
            $scope.successMessage = '';
            $scope.$applyAsync();

            productService.uploadProductImage(file).then(function(url) {
                $scope.profileData.profilePicURL = url;
                $scope.isUploading = false;
                
                // Immediately save the uploaded picture in user profile
                $scope.updateProfile();
                
                $rootScope.$broadcast('showToast', {
                    title: 'Image Uploaded',
                    message: 'Profile picture uploaded successfully.',
                    type: 'success'
                });
                $scope.$applyAsync();
            }).catch(function(err) {
                $scope.isUploading = false;
                $rootScope.$broadcast('showToast', {
                    title: 'Upload Failed',
                    message: 'Failed to upload profile picture.',
                    type: 'error'
                });
                $scope.$applyAsync();
            });
        };

        // ========== Profile Detail Actions ==========
        $scope.updateProfile = function() {
            $scope.errorMessage = '';
            $scope.successMessage = '';

            if (!$scope.profileData.name) {
                $scope.errorMessage = 'Name is required.';
                return;
            }

            var payload = {
                id: $scope.profileData.id,
                name: $scope.profileData.name,
                email: $scope.profileData.email,
                contactNum: $scope.profileData.contactNum,
                gender: $scope.profileData.gender,
                profilePicURL: $scope.profileData.profilePicURL,
                role: $scope.profileData.role,
                active: $scope.profileData.active
            };

            authService.updateProfileApi(payload).then(function(updatedUser) {
                authService.updateCurrentUser(updatedUser);
                $scope.successMessage = 'Profile updated successfully.';
                $rootScope.$broadcast('showToast', {
                    title: 'Profile Updated',
                    message: 'Your profile details have been saved.',
                    type: 'success'
                });
                $scope.$applyAsync();
            }).catch(function(err) {
                var msg = 'Failed to update profile. Please try again.';
                if (err && err.data) {
                    if (typeof err.data === 'string') msg = err.data;
                    else if (err.data.message) msg = err.data.message;
                }
                $scope.errorMessage = msg;
                $scope.$applyAsync();
            });
        };

        // ========== Credentials & Security Actions ==========
        $scope.changeEmail = function() {
            $scope.errorMessage = '';
            $scope.successMessage = '';

            if (!$scope.profileData.email) {
                $scope.errorMessage = 'Email address is required.';
                return;
            }

            var payload = {
                id: $scope.profileData.id,
                name: $scope.profileData.name,
                email: $scope.profileData.email,
                contactNum: $scope.profileData.contactNum,
                gender: $scope.profileData.gender,
                profilePicURL: $scope.profileData.profilePicURL,
                role: $scope.profileData.role,
                active: $scope.profileData.active
            };

            authService.updateProfileApi(payload).then(function(updatedUser) {
                authService.updateCurrentUser(updatedUser);
                $scope.successMessage = 'Email address updated successfully.';
                $rootScope.$broadcast('showToast', {
                    title: 'Email Updated',
                    message: 'Your registered email has been updated.',
                    type: 'success'
                });
                $scope.$applyAsync();
            }).catch(function(err) {
                var msg = 'Failed to update email address. Please try again.';
                if (err && err.data) {
                    if (typeof err.data === 'string') msg = err.data;
                    else if (err.data.message) msg = err.data.message;
                }
                $scope.errorMessage = msg;
                $scope.$applyAsync();
            });
        };

        $scope.updatePassword = function() {
            $scope.errorMessage = '';
            $scope.successMessage = '';

            if (!$scope.profileData.password || $scope.profileData.password.trim() === '') {
                $scope.errorMessage = 'Password cannot be empty.';
                return;
            }
            if ($scope.profileData.password.length < 6) {
                $scope.errorMessage = 'Password must be at least 6 characters.';
                return;
            }

            var payload = {
                id: $scope.profileData.id,
                name: $scope.profileData.name,
                email: $scope.profileData.email,
                contactNum: $scope.profileData.contactNum,
                gender: $scope.profileData.gender,
                profilePicURL: $scope.profileData.profilePicURL,
                role: $scope.profileData.role,
                active: $scope.profileData.active,
                password: $scope.profileData.password
            };

            authService.updateProfileApi(payload).then(function(updatedUser) {
                authService.updateCurrentUser(updatedUser);
                $scope.successMessage = 'Password updated successfully.';
                $scope.profileData.password = ''; // Clear field
                $rootScope.$broadcast('showToast', {
                    title: 'Password Updated',
                    message: 'Your account password has been updated.',
                    type: 'success'
                });
                $scope.$applyAsync();
            }).catch(function(err) {
                var msg = 'Failed to update password. Please try again.';
                if (err && err.data) {
                    if (typeof err.data === 'string') msg = err.data;
                    else if (err.data.message) msg = err.data.message;
                }
                $scope.errorMessage = msg;
                $scope.$applyAsync();
            });
        };

        // ========== Address Management Actions ==========
        $scope.loadAddresses = function() {
            apiService.get('/api/users/addresses')
                .then(function(res) {
                    $scope.addresses = res.data;
                    $scope.$applyAsync();
                })
                .catch(function(err) {
                    $scope.errorMessage = 'Failed to load shipping addresses.';
                    $scope.$applyAsync();
                });
        };

        $scope.openAddressForm = function() {
            $scope.isEditingAddress = false;
            $scope.showAddressForm = true;
            $scope.addressData = {
                id: null,
                fullName: '',
                phone: '',
                streetAddress: '',
                city: '',
                state: '',
                zipCode: '',
                isDefault: false
            };
        };

        $scope.closeAddressForm = function() {
            $scope.showAddressForm = false;
            $scope.isEditingAddress = false;
        };

        $scope.editAddress = function(addr) {
            $scope.isEditingAddress = true;
            $scope.addressData = angular.copy(addr);
            $scope.showAddressForm = true;
        };

        $scope.saveAddress = function() {
            $scope.errorMessage = '';
            $scope.successMessage = '';

            var payload = {
                fullName: $scope.addressData.fullName,
                phone: $scope.addressData.phone,
                streetAddress: $scope.addressData.streetAddress,
                city: $scope.addressData.city,
                state: $scope.addressData.state,
                zipCode: $scope.addressData.zipCode,
                isDefault: $scope.addressData.isDefault
            };

            var request;
            if ($scope.isEditingAddress) {
                request = apiService.put('/api/users/addresses/' + $scope.addressData.id, payload);
            } else {
                payload.isDefault = $scope.addresses.length === 0 ? true : payload.isDefault;
                request = apiService.post('/api/users/addresses', payload);
            }

            request.then(function() {
                $scope.closeAddressForm();
                $scope.loadAddresses();
                $rootScope.$broadcast('showToast', {
                    title: $scope.isEditingAddress ? 'Address Updated' : 'Address Added',
                    message: $scope.isEditingAddress ? 'Shipping address updated successfully.' : 'New shipping address saved.',
                    type: 'success'
                });
            }).catch(function(err) {
                $scope.errorMessage = 'Failed to save address. Please check input.';
                $scope.$applyAsync();
            });
        };

        $scope.deleteAddress = function(id) {
            if (!confirm('Are you sure you want to delete this address?')) return;
            
            apiService.delete('/api/users/addresses/' + id)
                .then(function() {
                    $scope.loadAddresses();
                    $rootScope.$broadcast('showToast', {
                        title: 'Address Deleted',
                        message: 'Shipping address removed.',
                        type: 'info'
                    });
                })
                .catch(function() {
                    $scope.errorMessage = 'Failed to delete address.';
                    $scope.$applyAsync();
                });
        };

        $scope.setDefaultAddress = function(addr) {
            var payload = angular.copy(addr);
            payload.isDefault = true;
            
            apiService.put('/api/users/addresses/' + addr.id, payload)
                .then(function() {
                    $scope.loadAddresses();
                    $rootScope.$broadcast('showToast', {
                        title: 'Default Address Set',
                        message: addr.fullName + ' is now your default shipping address.',
                        type: 'success'
                    });
                })
                .catch(function() {
                    $scope.errorMessage = 'Failed to update default address.';
                    $scope.$applyAsync();
                });
        };

        // ========== Account Control Actions (Soft Delete) ==========
        $scope.confirmDeactivate = function() {
            var email = $scope.profileData.email;
            if (!confirm('WARNING: Are you sure you want to deactivate your account? \nThis will soft-delete your data and log you out immediately.')) {
                return;
            }
            
            apiService.post('/api/users/deactivate')
                .then(function() {
                    authService.logout();
                    $rootScope.$broadcast('showToast', {
                        title: 'Account Deactivated',
                        message: 'Your account has been soft-deleted successfully.',
                        type: 'info'
                    });
                    $location.path('/login');
                })
                .catch(function(err) {
                    $scope.errorMessage = 'Failed to deactivate account. Please contact support.';
                    $scope.$applyAsync();
                });
        };

    }
]);
