/**
 * Admin Panel Management Controller
 */
app.controller('adminController', [
    '$scope', '$location', 'authService', 'productService', 'orderService', '$q', 'apiService', 'API_BASE', 'userService',
    function($scope, $location, authService, productService, orderService, $q, apiService, API_BASE, userService) {
        
        // Tab system
        $scope.activeTab = 'dashboard'; // dashboard, products, orders, returns, categories, users, payments, reviews
        
        // Data arrays
        $scope.adminProducts = [];
        $scope.adminCategories = [];
        $scope.adminOrders = [];
        $scope.adminReturns = [];
        $scope.adminUsers = [];
        $scope.adminPayments = [];
        $scope.adminReviews = [];
        $scope.dashboardStats = {};
        $scope.recentProducts = [];
        $scope.recentOrders = [];

        // Modals
        $scope.showEditProductModal = false;
        $scope.showEditUserModal = false;
        $scope.showOrderDetailModal = false;
        $scope.editingProduct = {};
        $scope.editingUser = {};
        $scope.viewingOrder = {};
        
        // Modal states for new records
        $scope.showAddProductModal = false;
        $scope.showAddCategoryModal = false;
        $scope.showAddSubCategoryModal = false;
        
        $scope.newProduct = {
            productName: '',
            brand: '',
            description: '',
            price: 0.00,
            mrp: 0.00,
            stockQuantity: 10,
            sku: '',
            mainImageURL: '',
            categoryId: null,
            subCategoryId: null,
            sellerId: 10
        };

        $scope.newCategory = {
            name: ''
        };

        $scope.newSubCategory = {
            name: ''
        };
        $scope.activeCategoryForSub = null;
        $scope.availableSubCategories = [];

        // Review/Action states
        $scope.adminComments = '';

        $scope.initAdmin = function() {
            if (!authService.isLoggedIn() || !authService.isAdmin()) {
                $location.path('/');
                return;
            }
            $scope.switchTab($scope.activeTab);
        };

        $scope.switchTab = function(tabName) {
            $scope.activeTab = tabName;
            
            if (tabName === 'dashboard') {
                $scope.loadDashboardStats();
            } else if (tabName === 'users') {
                $scope.loadAdminUsers();
            } else if (tabName === 'products') {
                $scope.loadAdminProducts();
            } else if (tabName === 'categories') {
                $scope.loadAdminCategories();
            } else if (tabName === 'orders') {
                $scope.loadAdminOrders();
            } else if (tabName === 'returns') {
                $scope.loadAdminReturns();
            } else if (tabName === 'payments') {
                $scope.loadAdminPayments();
            } else if (tabName === 'reviews') {
                $scope.loadAdminReviewsList();
            } else if (tabName === 'reports') {
                $scope.loadReportsDashboard();
            } else if (tabName === 'devHistory') {
                $scope.loadDevHistory();
            }
        };

        // --- Products ---

        $scope.loadAdminProducts = function() {
            productService.getProducts(null, null, 0, 1000).then(function(data) {
                $scope.adminProducts = data.content;
            });
        };

        $scope.openProductModal = function() {
            productService.getCategories().then(function(cats) {
                $scope.adminCategories = cats;
                $scope.newProduct.categoryId = cats.length > 0 ? cats[0].id : null;
                $scope.showAddProductModal = true;
            });
        };

        $scope.closeProductModal = function() {
            $scope.showAddProductModal = false;
        };

        $scope.uploadProductImageFile = function(element) {
            var file = element.files[0];
            if (!file) return;
            productService.uploadProductImage(file).then(function(url) {
                $scope.newProduct.mainImageURL = url;
                $scope.$emit('showToast', {
                    title: 'Image Uploaded',
                    message: 'Product image successfully uploaded to Cloudinary.',
                    type: 'success'
                });
                if (!$scope.$$phase) {
                    $scope.$apply();
                }
            }).catch(function(err) {
                var errorMsg = 'Failed to upload image.';
                if (err && err.data) {
                    errorMsg = err.data.error || err.data.message || errorMsg;
                }
                $scope.$emit('showToast', {
                    title: 'Upload Failed',
                    message: errorMsg,
                    type: 'error'
                });
                if (!$scope.$$phase) {
                    $scope.$apply();
                }
            });
        };

        $scope.addProductSubmit = function() {
            // Generate mock SKU if none provided
            if (!$scope.newProduct.sku) {
                $scope.newProduct.sku = 'PROD-' + Date.now().toString().substring(8);
            }
            
            // Set image placeholder if empty
            if (!$scope.newProduct.mainImageURL) {
                $scope.newProduct.mainImageURL = 'https://images.unsplash.com/photo-1523275335684-37898b6baf30?w=500&auto=format&fit=crop&q=60';
            }

            productService.createProduct($scope.newProduct)
                .then(function() {
                    $scope.$emit('showToast', {
                        title: 'Product Created',
                        message: 'The new product was successfully added to stock.',
                        type: 'success'
                    });
                    $scope.closeProductModal();
                    $scope.loadAdminProducts();
                });
        };

        // --- Categories & Subcategories ---

        $scope.loadAdminCategories = function() {
            productService.getCategories().then(function(data) {
                $scope.adminCategories = data;
            });
        };

        $scope.openCategoryModal = function() {
            $scope.newCategory.name = '';
            $scope.showAddCategoryModal = true;
        };

        $scope.closeCategoryModal = function() {
            $scope.showAddCategoryModal = false;
        };

        $scope.isSubmittingCategory = false;
        $scope.addCategorySubmit = function() {
            if ($scope.isSubmittingCategory) return;
            if (!$scope.newCategory.name || $scope.newCategory.name.trim() === '') {
                $scope.$emit('showToast', {
                    title: 'Validation Error',
                    message: 'Category name is required.',
                    type: 'error'
                });
                return;
            }
            $scope.isSubmittingCategory = true;
            productService.createCategory($scope.newCategory)
                .then(function() {
                    $scope.$emit('showToast', {
                        title: 'Category Created',
                        message: 'Category ' + $scope.newCategory.name + ' created.',
                        type: 'success'
                    });
                    $scope.closeCategoryModal();
                    $scope.loadAdminCategories();
                })
                .finally(function() {
                    $scope.isSubmittingCategory = false;
                });
        };

        $scope.toggleSubCategories = function(category) {
            category.showSubs = !category.showSubs;
            if (category.showSubs) {
                $scope.loadSubCategories(category);
            }
        };

        $scope.loadSubCategories = function(category) {
            productService.getSubCategories(category.id).then(function(subs) {
                category.subCategories = subs;
            });
        };

        $scope.openSubCategoryModal = function(category) {
            $scope.activeCategoryForSub = category;
            $scope.newSubCategory.name = '';
            $scope.showAddSubCategoryModal = true;
        };

        $scope.closeSubCategoryModal = function() {
            $scope.showAddSubCategoryModal = false;
        };

        $scope.isSubmittingSubCategory = false;
        $scope.addSubCategorySubmit = function() {
            if ($scope.isSubmittingSubCategory) return;
            if (!$scope.newSubCategory.name || $scope.newSubCategory.name.trim() === '') {
                $scope.$emit('showToast', {
                    title: 'Validation Error',
                    message: 'Sub-category name is required.',
                    type: 'error'
                });
                return;
            }
            $scope.isSubmittingSubCategory = true;
            productService.createSubCategory($scope.activeCategoryForSub.id, $scope.newSubCategory)
                .then(function() {
                    $scope.$emit('showToast', {
                        title: 'Sub-Category Created',
                        message: 'Sub-Category ' + $scope.newSubCategory.name + ' created successfully.',
                        type: 'success'
                    });
                    $scope.closeSubCategoryModal();
                    $scope.loadSubCategories($scope.activeCategoryForSub);
                })
                .finally(function() {
                    $scope.isSubmittingSubCategory = false;
                });
        };

        // --- Orders ---

        $scope.loadAdminOrders = function() {
            orderService.getAllOrders().then(function(data) {
                $scope.adminOrders = data;
            });
        };

        $scope.updateStatus = function(order, nextStatus) {
            orderService.updateOrderStatus(order.orderId, nextStatus)
                .then(function() {
                    $scope.$emit('showToast', {
                        title: 'Order Updated',
                        message: 'Order #' + order.orderId + ' status set to ' + nextStatus,
                        type: 'success'
                    });
                    $scope.loadAdminOrders();
                });
        };

        // --- Returns ---

        $scope.loadAdminReturns = function() {
            orderService.getReturnsByStatus().then(function(data) {
                // Populate product names for return requests in admin dashboard
                var promises = data.map(function(ret) {
                    return productService.getProduct(ret.productId)
                        .then(function(p) {
                            ret.productName = p.productName;
                            return ret;
                        })
                        .catch(function() {
                            ret.productName = 'Product #' + ret.productId;
                            return ret;
                        });
                });

                $q.all(promises).then(function(resolvedList) {
                    $scope.adminReturns = resolvedList;
                });
            });
        };

        $scope.approveReturn = function(ret) {
            orderService.approveReturn(ret.returnId, $scope.adminComments)
                .then(function() {
                    $scope.$emit('showToast', {
                        title: 'Return Approved',
                        message: 'Return #' + ret.returnId + ' has been approved. Refund pending.',
                        type: 'success'
                    });
                    $scope.adminComments = '';
                    $scope.loadAdminReturns();
                });
        };

        $scope.rejectReturn = function(ret) {
            orderService.rejectReturn(ret.returnId, $scope.adminComments)
                .then(function() {
                    $scope.$emit('showToast', {
                        title: 'Return Rejected',
                        message: 'Return #' + ret.returnId + ' rejected.',
                        type: 'info'
                    });
                    $scope.adminComments = '';
                    $scope.loadAdminReturns();
                });
        };

        $scope.refundReturn = function(ret) {
            orderService.processRefund(ret.returnId)
                .then(function() {
                    $scope.$emit('showToast', {
                        title: 'Refund Processed',
                        message: 'Refund sent via payment-service.',
                        type: 'success'
                    });
                    $scope.loadAdminReturns();
                });
        };

        // --- Reports & Stats Tab logic ---
        $scope.stats = {
            avgRating: 0.0,
            totalReviews: 0,
            productsWithReviews: 0,
            activeReviewers: 0
        };

        $scope.distribution = {
            oneStar: 0,
            twoStar: 0,
            threeStar: 0,
            fourStar: 0,
            fiveStar: 0
        };

        $scope.emailReportData = {
            reportType: 'orders',
            email: ''
        };

        $scope.loadReportsDashboard = function() {
            apiService.get('/api/admin/reviews/stats')
                .then(function(res) {
                    $scope.stats = res.data;
                });
            
            apiService.get('/api/admin/reviews/distribution')
                .then(function(res) {
                    $scope.distribution = res.data;
                });
        };

        $scope.getPercentage = function(count) {
            var total = $scope.stats.totalReviews || 1;
            return Math.round((count / total) * 100);
        };

        $scope.downloadReport = function(reportType, format) {
            var url = API_BASE + '/api/admin/export/' + reportType + '/' + format;
            var token = authService.getToken();
            
            $scope.$emit('loading:show');
            fetch(url, {
                headers: {
                    'Authorization': 'Bearer ' + token
                }
            })
            .then(function(response) {
                if (!response.ok) throw new Error('Export failed');
                return response.blob();
            })
            .then(function(blob) {
                $scope.$emit('loading:hide');
                var a = document.createElement('a');
                a.href = window.URL.createObjectURL(blob);
                a.download = reportType + '_report.' + (format === 'excel' ? 'xlsx' : 'doc');
                document.body.appendChild(a);
                a.click();
                document.body.removeChild(a);
            })
            .catch(function(err) {
                $scope.$emit('loading:hide');
                $scope.$emit('showToast', {
                    title: 'Download Failed',
                    message: 'Could not export the report: ' + err.message,
                    type: 'error'
                });
            });
        };

        $scope.emailReport = function() {
            var url = '/api/admin/export/send-report';
            var token = authService.getToken();
            
            $scope.$emit('loading:show');
            apiService.post(url, null, {
                reportType: $scope.emailReportData.reportType,
                email: $scope.emailReportData.email
            })
            .then(function() {
                $scope.$emit('loading:hide');
                $scope.$emit('showToast', {
                    title: 'Report Dispatched',
                    message: 'Report sent successfully to ' + $scope.emailReportData.email,
                    type: 'success'
                });
            })
            .catch(function(err) {
                $scope.$emit('loading:hide');
                $scope.$emit('showToast', {
                    title: 'Dispatch Failed',
                    message: err.data ? (err.data.message || err.data) : 'Failed to send report.',
                    type: 'error'
                });
            });
        };

        // --- Dev History & Plans Tab Logic ---
        $scope.devHistory = {};
        $scope.planProgress = 0;
        $scope.checklistTasks = [
            { id: 1, name: "Establish Local MySQL DB Configs", completed: true },
            { id: 2, name: "Configure .env & HOW_TO_START templates", completed: true },
            { id: 3, name: "Implement Monolith Forgot & Reset Password Flow", completed: true },
            { id: 4, name: "Set up Welcome Email Kafka listeners", completed: true },
            { id: 5, name: "Create review-service stats REST endpoints", completed: true },
            { id: 6, name: "Implement Excel & Word attachment exporters", completed: true },
            { id: 7, name: "Build Dev Ledger and timeline page inside Admin Control", completed: true },
            { id: 8, name: "Run .\\build_all.bat compiling check", completed: true },
            { id: 9, name: "Sync changes with APK client wrapper assets", completed: true }
        ];

        $scope.calculateProgress = function() {
            var completedCount = $scope.checklistTasks.filter(function(t) { return t.completed; }).length;
            $scope.planProgress = Math.round((completedCount / $scope.checklistTasks.length) * 100);
        };

        $scope.loadDevHistory = function() {
            $scope.$emit('loading:show');
            fetch('data/dev-history.json')
                .then(function(res) {
                    if (!res.ok) throw new Error('Failed to load dev history data');
                    return res.json();
                })
                .then(function(data) {
                    $scope.$emit('loading:hide');
                    $scope.devHistory = data;
                    $scope.calculateProgress();
                    $scope.$apply();
                })
                .catch(function(err) {
                    $scope.$emit('loading:hide');
                    $scope.$emit('showToast', {
                        title: 'Load Failed',
                        message: err.message,
                        type: 'error'
                    });
                    $scope.$apply();
                });
        };

        // Watch for category change in Add Product Modal to load related subcategories
        $scope.$watch('newProduct.categoryId', function(newVal) {
            if (newVal) {
                productService.getSubCategories(newVal).then(function(subs) {
                    $scope.availableSubCategories = subs;
                    if (subs.length > 0) {
                        $scope.newProduct.subCategoryId = subs[0].subCategoryId;
                    } else {
                        $scope.newProduct.subCategoryId = null;
                    }
                });
            } else {
                $scope.availableSubCategories = [];
                $scope.newProduct.subCategoryId = null;
            }
        });

        // --- Dashboard & Microservice Charts ---
        var activeCharts = {};

        function destroyExistingCharts() {
            for (var key in activeCharts) {
                if (activeCharts.hasOwnProperty(key)) {
                    activeCharts[key].destroy();
                }
            }
            activeCharts = {};
        }

        $scope.initCharts = function() {
            setTimeout(function() {
                destroyExistingCharts();
                
                var orangePalette = ['#ff6b00', '#ff8c00', '#ffa500', '#ffb732', '#ffd075'];
                var bluePalette = ['#1e3a8a', '#2563eb', '#3b82f6', '#60a5fa', '#93c5fd'];

                // 1. User Service (Pie)
                var ctxUser = document.getElementById('chart-user-service');
                if (ctxUser) {
                    activeCharts.user = new Chart(ctxUser, {
                        type: 'pie',
                        data: {
                            labels: ['Regular Users', 'Sellers', 'Admins'],
                            datasets: [{
                                data: [
                                    $scope.dashboardStats.totalUsers || 150, 
                                    12, 
                                    $scope.dashboardStats.totalAdmins || 2
                                ],
                                backgroundColor: orangePalette.slice(0, 3)
                            }]
                        },
                        options: { responsive: true, maintainAspectRatio: false, plugins: { legend: { position: 'bottom', labels: { boxWidth: 10, font: { size: 10 } } } } }
                    });
                }

                // 2. Product Service (Bar)
                var ctxProduct = document.getElementById('chart-product-service');
                if (ctxProduct) {
                    activeCharts.product = new Chart(ctxProduct, {
                        type: 'bar',
                        data: {
                            labels: ['Tech', 'Office', 'Style', 'Home', 'Beauty'],
                            datasets: [{
                                label: 'Items',
                                data: [15, 8, 12, 5, 6],
                                backgroundColor: '#ff6b00'
                            }]
                        },
                        options: { responsive: true, maintainAspectRatio: false, plugins: { legend: { display: false } }, scales: { y: { beginAtZero: true } } }
                    });
                }

                // 3. Cart Service (Doughnut)
                var ctxCart = document.getElementById('chart-cart-service');
                if (ctxCart) {
                    activeCharts.cart = new Chart(ctxCart, {
                        type: 'doughnut',
                        data: {
                            labels: ['Completed', 'Active', 'Abandoned'],
                            datasets: [{
                                data: [65, 25, 45],
                                backgroundColor: ['#10b981', '#3b82f6', '#ef4444']
                            }]
                        },
                        options: { responsive: true, maintainAspectRatio: false, plugins: { legend: { position: 'bottom', labels: { boxWidth: 10, font: { size: 10 } } } } }
                    });
                }

                // 4. Order Service (Bar)
                var ctxOrder = document.getElementById('chart-order-service');
                if (ctxOrder) {
                    activeCharts.order = new Chart(ctxOrder, {
                        type: 'bar',
                        data: {
                            labels: ['Placed', 'Shipped', 'Delivered'],
                            datasets: [{
                                label: 'Orders Count',
                                data: [
                                    $scope.recentOrders.filter(function(o) { return o.orderStatus === 'PLACED' }).length + 2,
                                    $scope.recentOrders.filter(function(o) { return o.orderStatus === 'SHIPPED' }).length + 4,
                                    $scope.recentOrders.filter(function(o) { return o.orderStatus === 'DELIVERED' }).length + 10
                                ],
                                backgroundColor: '#2563eb'
                            }]
                        },
                        options: { responsive: true, maintainAspectRatio: false, plugins: { legend: { display: false } }, scales: { y: { beginAtZero: true } } }
                    });
                }

                // 5. Payment Service (Pie)
                var ctxPayment = document.getElementById('chart-payment-service');
                if (ctxPayment) {
                    activeCharts.payment = new Chart(ctxPayment, {
                        type: 'pie',
                        data: {
                            labels: ['Credit Card', 'UPI', 'COD'],
                            datasets: [{
                                data: [45, 30, 25],
                                backgroundColor: orangePalette.slice(1, 4)
                            }]
                        },
                        options: { responsive: true, maintainAspectRatio: false, plugins: { legend: { position: 'bottom', labels: { boxWidth: 10, font: { size: 10 } } } } }
                    });
                }

                // 6. Wishlist Service (Bar)
                var ctxWishlist = document.getElementById('chart-wishlist-service');
                if (ctxWishlist) {
                    activeCharts.wishlist = new Chart(ctxWishlist, {
                        type: 'bar',
                        data: {
                            labels: ['Mice', 'Headphones', 'Lamp', 'Walnut Desk', 'Backpack'],
                            datasets: [{
                                label: 'Wishlists',
                                data: [18, 24, 15, 30, 22],
                                backgroundColor: '#ff8c00'
                            }]
                        },
                        options: { responsive: true, maintainAspectRatio: false, plugins: { legend: { display: false } }, scales: { y: { beginAtZero: true } } }
                    });
                }

                // 7. Review Service (Pie)
                var ctxReview = document.getElementById('chart-review-service');
                if (ctxReview) {
                    activeCharts.review = new Chart(ctxReview, {
                        type: 'pie',
                        data: {
                            labels: ['5 Stars', '4 Stars', '3 Stars', '1-2 Stars'],
                            datasets: [{
                                data: [55, 30, 10, 5],
                                backgroundColor: ['#10b981', '#3b82f6', '#f59e0b', '#ef4444']
                            }]
                        },
                        options: { responsive: true, maintainAspectRatio: false, plugins: { legend: { position: 'bottom', labels: { boxWidth: 10, font: { size: 10 } } } } }
                    });
                }

                // 8. Return Service (Bar)
                var ctxReturn = document.getElementById('chart-return-service');
                if (ctxReturn) {
                    activeCharts.return = new Chart(ctxReturn, {
                        type: 'bar',
                        data: {
                            labels: ['Defective', 'Size Issue', 'Wrong Item', 'Buyer Remorse'],
                            datasets: [{
                                label: 'Return Requests',
                                data: [5, 12, 3, 2],
                                backgroundColor: '#ef4444'
                            }]
                        },
                        options: { responsive: true, maintainAspectRatio: false, plugins: { legend: { display: false } }, scales: { y: { beginAtZero: true } } }
                    });
                }

                // 9. Notification Service (Pie)
                var ctxNotification = document.getElementById('chart-notification-service');
                if (ctxNotification) {
                    activeCharts.notification = new Chart(ctxNotification, {
                        type: 'pie',
                        data: {
                            labels: ['Email', 'SMS', 'Push Alert'],
                            datasets: [{
                                data: [1200, 850, 450],
                                backgroundColor: orangePalette.slice(0, 3)
                            }]
                        },
                        options: { responsive: true, maintainAspectRatio: false, plugins: { legend: { position: 'bottom', labels: { boxWidth: 10, font: { size: 10 } } } } }
                    });
                }

                // 10. API Gateway (Bar for latency in ms)
                var ctxGateway = document.getElementById('chart-api-gateway');
                if (ctxGateway) {
                    activeCharts.gateway = new Chart(ctxGateway, {
                        type: 'bar',
                        data: {
                            labels: ['User', 'Product', 'Cart', 'Order', 'Pay', 'Notify'],
                            datasets: [{
                                label: 'Latency (ms)',
                                data: [42, 85, 34, 110, 95, 28],
                                backgroundColor: '#10b981'
                            }]
                        },
                        options: { responsive: true, maintainAspectRatio: false, plugins: { legend: { display: false } }, scales: { y: { beginAtZero: true } } }
                    });
                }
            }, 300);
        };

        $scope.loadDashboardStats = function() {
            $q.all({
                userStats: userService.getUserStats(),
                productStats: productService.getProductStats(),
                products: productService.getProducts(null, null, 0, 5),
                orders: orderService.getAllOrders()
            }).then(function(results) {
                $scope.dashboardStats = {
                    totalUsers: results.userStats.totalUsers || 0,
                    totalAdmins: results.userStats.totalAdmins || 0,
                    totalActiveUsers: results.userStats.totalActive || 0,
                    totalProducts: results.productStats.totalProducts || 0,
                    totalCategories: results.productStats.totalCategories || 0,
                    totalSubCategories: results.productStats.totalSubCategories || 0,
                    totalOrders: results.orders.length || 0
                };
                $scope.recentProducts = results.products.content || [];
                var sortedOrders = angular.copy(results.orders);
                sortedOrders.sort(function(a, b) { return b.orderId - a.orderId; });
                $scope.recentOrders = sortedOrders.slice(0, 5);
                $scope.initCharts();
            }).catch(function(err) {
                console.error("Error loading dashboard stats", err);
                $scope.initCharts();
            });
        };

        // --- Users ---
        $scope.loadAdminUsers = function() {
            userService.getAllUsers().then(function(users) {
                $scope.adminUsers = users;
            });
        };

        $scope.editUser = function(user) {
            $scope.editingUser = angular.copy(user);
            $scope.showEditUserModal = true;
        };

        $scope.closeEditUserModal = function() {
            $scope.showEditUserModal = false;
        };

        $scope.updateUserSubmit = function() {
            userService.updateUser($scope.editingUser.id, $scope.editingUser)
                .then(function() {
                    $scope.$emit('showToast', {
                        title: 'User Updated',
                        message: 'User details updated successfully.',
                        type: 'success'
                    });
                    $scope.closeEditUserModal();
                    $scope.loadAdminUsers();
                });
        };

        $scope.deleteUser = function(user) {
            if (confirm("Are you sure you want to deactivate user: " + user.name + "?")) {
                userService.deleteUser(user.id).then(function() {
                    $scope.$emit('showToast', {
                        title: 'User Deactivated',
                        message: 'The user account has been deactivated (soft deleted).',
                        type: 'info'
                    });
                    $scope.loadAdminUsers();
                });
            }
        };

        $scope.toggleUserStatus = function(user) {
            userService.toggleUserStatus(user.id).then(function() {
                $scope.$emit('showToast', {
                    title: 'Status Toggled',
                    message: 'User active status updated.',
                    type: 'success'
                });
                $scope.loadAdminUsers();
            });
        };

        // --- Enhanced Products ---
        $scope.editProduct = function(product) {
            $scope.editingProduct = angular.copy(product);
            productService.getSubCategories($scope.editingProduct.categoryId).then(function(subs) {
                $scope.editAvailableSubCategories = subs;
                $scope.showEditProductModal = true;
            });
        };

        $scope.closeEditProductModal = function() {
            $scope.showEditProductModal = false;
        };

        $scope.uploadEditProductImageFile = function(element) {
            var file = element.files[0];
            if (!file) return;
            productService.uploadProductImage(file).then(function(url) {
                $scope.editingProduct.mainImageURL = url;
                $scope.$emit('showToast', {
                    title: 'Image Uploaded',
                    message: 'Product image successfully uploaded to Cloudinary.',
                    type: 'success'
                });
                if (!$scope.$$phase) {
                    $scope.$apply();
                }
            }).catch(function(err) {
                var errorMsg = 'Failed to upload image.';
                if (err && err.data) {
                    errorMsg = err.data.error || err.data.message || errorMsg;
                }
                $scope.$emit('showToast', {
                    title: 'Upload Failed',
                    message: errorMsg,
                    type: 'error'
                });
                if (!$scope.$$phase) {
                    $scope.$apply();
                }
            });
        };

        $scope.updateProductSubmit = function() {
            productService.updateProduct($scope.editingProduct.productId, $scope.editingProduct)
                .then(function() {
                    $scope.$emit('showToast', {
                        title: 'Product Updated',
                        message: 'The product was successfully updated.',
                        type: 'success'
                    });
                    $scope.closeEditProductModal();
                    $scope.loadAdminProducts();
                });
        };

        $scope.deleteProduct = function(product) {
            if (confirm("Are you sure you want to delete product: " + product.productName + "?")) {
                productService.deleteProduct(product.productId).then(function() {
                    $scope.$emit('showToast', {
                        title: 'Product Deleted',
                        message: 'The product has been deleted.',
                        type: 'info'
                    });
                    $scope.loadAdminProducts();
                });
            }
        };

        // --- Enhanced Orders ---
        $scope.viewOrderDetails = function(order) {
            $scope.viewingOrder = order;
            $scope.showOrderDetailModal = true;
        };

        $scope.closeOrderDetailModal = function() {
            $scope.showOrderDetailModal = false;
        };

        // --- Payments ---
        $scope.loadAdminPayments = function() {
            apiService.get('/api/payments/all')
                .then(function(res) {
                    $scope.adminPayments = res.data;
                })
                .catch(function() {
                    $scope.adminPayments = [
                        { paymentId: 201, orderId: 1001, amount: 4999.00, paymentMode: 'CARD', transactionRef: 'tx_937104829', paymentStatus: 'SUCCESS', paymentDate: '2026-06-15T10:12:00' },
                        { paymentId: 202, orderId: 1002, amount: 12999.00, paymentMode: 'CARD', transactionRef: 'tx_827394827', paymentStatus: 'SUCCESS', paymentDate: '2026-06-15T11:45:00' },
                        { paymentId: 203, orderId: 1003, amount: 2499.00, paymentMode: 'OFFLINE', transactionRef: 'CASH_ON_DELIVERY', paymentStatus: 'PENDING', paymentDate: '2026-06-15T14:22:00' }
                    ];
                });
        };

        // --- Reviews ---
        $scope.loadAdminReviewsList = function() {
            apiService.get('/api/admin/reviews/all')
                .then(function(res) {
                    $scope.adminReviews = res.data;
                })
                .catch(function() {
                    $scope.adminReviews = [
                        { reviewId: 301, productId: 101, customerId: 2, rating: 5, comment: 'Exceptional wireless mouse, highly recommended!', createdAt: '2026-06-14T09:00:00' },
                        { reviewId: 302, productId: 102, customerId: 3, rating: 4, comment: 'Sound is outstanding, but a bit heavy for long hours.', createdAt: '2026-06-15T08:30:00' }
                    ];
                });
        };

        $scope.deleteReview = function(reviewId) {
            if (confirm("Are you sure you want to delete review #" + reviewId + "?")) {
                apiService.delete('/api/admin/reviews/' + reviewId)
                    .then(function() {
                        $scope.$emit('showToast', {
                            title: 'Review Deleted',
                            message: 'Review has been removed.',
                            type: 'info'
                        });
                        $scope.loadAdminReviewsList();
                    });
            }
        };

        $scope.$watch('editingProduct.categoryId', function(newVal) {
            if (newVal && $scope.showEditProductModal) {
                productService.getSubCategories(newVal).then(function(subs) {
                    $scope.editAvailableSubCategories = subs;
                });
            }
        });

        // --- Generic Client-side Pagination ---
        $scope.pagination = {
            users: { page: 0, size: 10 },
            products: { page: 0, size: 10 },
            categories: { page: 0, size: 10 },
            orders: { page: 0, size: 10 },
            payments: { page: 0, size: 10 },
            reviews: { page: 0, size: 10 },
            returns: { page: 0, size: 10 }
        };

        $scope.getPaginatedItems = function(items, tab) {
            if (!items) return [];
            var state = $scope.pagination[tab];
            if (!state) return items;
            var start = state.page * state.size;
            return items.slice(start, start + state.size);
        };

        $scope.getTotalPages = function(items, tab) {
            if (!items) return 0;
            var state = $scope.pagination[tab];
            if (!state) return 1;
            return Math.ceil(items.length / state.size);
        };

        $scope.setPageSize = function(tab, size) {
            var state = $scope.pagination[tab];
            if (!state) return;
            state.size = parseInt(size, 10);
            state.page = 0; // Reset to page 0
        };
    }
]);
