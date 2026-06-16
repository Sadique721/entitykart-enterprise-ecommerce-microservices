/**
 * User Management Service
 */
app.service('userService', ['apiService', '$q', function(apiService, $q) {
    
    // Fallback Mock Users for robust preview/fallback
    var mockUsers = [
        {
            id: 1,
            name: 'John Doe',
            email: 'admin@entitykart.com',
            role: 'ADMIN',
            active: true,
            gender: 'Male',
            contactNum: '+1234567890',
            profilePicURL: 'https://images.unsplash.com/photo-1535713875002-d1d0cf377fde?w=150',
            createdAt: '2026-01-01T10:00:00'
        },
        {
            id: 2,
            name: 'Jane Smith',
            email: 'jane@entitykart.com',
            role: 'USER',
            active: true,
            gender: 'Female',
            contactNum: '+1987654321',
            profilePicURL: 'https://images.unsplash.com/photo-1494790108377-be9c29b29330?w=150',
            createdAt: '2026-02-15T14:30:00'
        },
        {
            id: 3,
            name: 'Bob Johnson',
            email: 'bob@entitykart.com',
            role: 'USER',
            active: false,
            gender: 'Male',
            contactNum: '+1555666777',
            profilePicURL: 'https://images.unsplash.com/photo-1570295999919-56ceb5ecca61?w=150',
            createdAt: '2026-03-20T09:15:00'
        }
    ];

    this.getAllUsers = function() {
        return apiService.get('/api/users/all')
            .then(function(response) {
                if (response.data && response.data.length > 0) {
                    return response.data;
                }
                return mockUsers;
            })
            .catch(function() {
                return mockUsers;
            });
    };

    this.getUserById = function(id) {
        return apiService.get('/api/users/' + id)
            .then(function(response) {
                return response.data;
            })
            .catch(function() {
                var user = mockUsers.find(function(u) { return u.id == id; });
                if (user) return user;
                throw new Error("User not found");
            });
    };

    this.updateUser = function(id, dto) {
        return apiService.put('/api/users/' + id, dto)
            .then(function(response) {
                return response.data;
            })
            .catch(function() {
                var index = mockUsers.findIndex(function(u) { return u.id == id; });
                if (index !== -1) {
                    var updated = angular.extend(mockUsers[index], dto);
                    return updated;
                }
                throw new Error("User not found");
            });
    };

    this.deleteUser = function(id) {
        return apiService.delete('/api/users/' + id)
            .then(function(response) {
                return response.data;
            })
            .catch(function() {
                var user = mockUsers.find(function(u) { return u.id == id; });
                if (user) {
                    user.active = false; // Soft delete
                }
                return true;
            });
    };

    this.toggleUserStatus = function(id) {
        return apiService.patch('/api/users/' + id + '/toggle-status')
            .then(function(response) {
                return response.data;
            })
            .catch(function() {
                var user = mockUsers.find(function(u) { return u.id == id; });
                if (user) {
                    user.active = !user.active;
                    return user;
                }
                throw new Error("User not found");
            });
    };

    this.getUserStats = function() {
        return apiService.get('/api/users/stats')
            .then(function(response) {
                return response.data;
            })
            .catch(function() {
                var total = mockUsers.length;
                var admins = mockUsers.filter(function(u) { return u.role === 'ADMIN'; }).length;
                var active = mockUsers.filter(function(u) { return u.active; }).length;
                return {
                    totalUsers: total,
                    totalAdmins: admins,
                    totalActive: active
                };
            });
    };
}]);
