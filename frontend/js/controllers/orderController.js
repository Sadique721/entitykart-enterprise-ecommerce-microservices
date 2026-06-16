/**
 * Customer Orders Management Controller
 */
app.controller('orderController', [
    '$scope', '$location', 'orderService', 'authService',
    function($scope, $location, orderService, authService) {

        $scope.orders = [];
        $scope.selectedOrder = null;
        $scope.orderFilter = 'ALL'; // Filter: ALL, PLACED, SHIPPED, DELIVERED, CANCELLED

        // Return request state
        $scope.showReturnModal = false;
        $scope.returnItem = null; // Specific order item being returned
        $scope.returnRequest = {
            orderItemId: null,
            reason: '',
            comments: ''
        };

        $scope.initOrders = function() {
            if (!authService.isLoggedIn()) {
                $location.path('/login');
                return;
            }
            $scope.loadOrders();
        };

        $scope.loadOrders = function() {
            orderService.getCustomerOrders().then(function(data) {
                // Sort orders by date descending
                $scope.orders = data.sort(function(a, b) {
                    return new Date(b.orderDate) - new Date(a.orderDate);
                });
            });
        };

        $scope.viewOrderDetails = function(order) {
            $scope.selectedOrder = ($scope.selectedOrder && $scope.selectedOrder.orderId === order.orderId) ? null : order;
        };

        /**
         * Determines if a given status milestone has been reached in the order flow.
         * Order flow: PLACED → SHIPPED → DELIVERED
         */
        $scope.isStatusReached = function(currentStatus, checkStatus) {
            var statusOrder = ['PENDING_PAYMENT', 'PLACED', 'SHIPPED', 'DELIVERED'];
            var currentIdx = statusOrder.indexOf(currentStatus);
            var checkIdx = statusOrder.indexOf(checkStatus);
            return currentIdx >= checkIdx && currentIdx !== -1;
        };

        // --- Return Requests ---

        $scope.openReturnDialog = function(item, event) {
            if (event) event.stopPropagation(); // Avoid triggering details toggle

            $scope.returnItem = item;
            $scope.returnRequest.orderId = $scope.selectedOrder.orderId;
            $scope.returnRequest.productId = item.productId;
            $scope.returnRequest.quantity = item.quantity;
            $scope.returnRequest.reason = ''; // Force user to choose
            $scope.returnRequest.comments = '';
            $scope.showReturnModal = true;
        };

        $scope.closeReturnDialog = function() {
            $scope.showReturnModal = false;
            $scope.returnItem = null;
        };

        $scope.submitReturnRequest = function() {
            if (!$scope.returnRequest.reason) {
                $scope.$emit('showToast', {
                    title: 'Validation Error',
                    message: 'Please select a reason for the return.',
                    type: 'error'
                });
                return;
            }

            orderService.requestReturn($scope.returnRequest)
                .then(function() {
                    $scope.$emit('showToast', {
                        title: 'Return Requested',
                        message: 'Your return request has been submitted for review.',
                        type: 'success'
                    });
                    $scope.closeReturnDialog();
                    $location.path('/returns');
                })
                .catch(function(err) {
                    $scope.$emit('showToast', {
                        title: 'Return Request Failed',
                        message: err.data ? err.data.message : 'Return already requested or invalid item.',
                        type: 'error'
                    });
                    $scope.closeReturnDialog();
                });
        };
    }
]);
