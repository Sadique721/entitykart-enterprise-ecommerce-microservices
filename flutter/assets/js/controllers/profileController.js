/**
 * Profile Controller
 */
app.controller('profileController', ['$scope', '$rootScope', '$location', 'authService', 'productService', 
function($scope, $rootScope, $location, authService, productService) {
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

    $scope.initProfile = function() {
        if (!authService.isLoggedIn()) {
            $location.path('/login');
            return;
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
            $scope.$applyAsync();
        }).catch(function(err) {
            $scope.errorMessage = 'Failed to load user profile. Please check connection.';
            $scope.$applyAsync();
        });
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
            $rootScope.$broadcast('showToast', {
                title: 'Image Uploaded',
                message: 'Profile picture uploaded to Cloudinary.',
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

        if ($scope.profileData.password && $scope.profileData.password.trim() !== '') {
            if ($scope.profileData.password.length < 6) {
                $scope.errorMessage = 'Password must be at least 6 characters.';
                return;
            }
            payload.password = $scope.profileData.password;
        }

        authService.updateProfileApi(payload).then(function(updatedUser) {
            authService.updateCurrentUser(updatedUser);
            $scope.successMessage = 'Profile updated successfully.';
            $scope.profileData.password = ''; // Clear password field
            $rootScope.$broadcast('showToast', {
                title: 'Profile Updated',
                message: 'Your profile has been saved.',
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
}]);
