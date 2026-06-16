/**
 * Return Requests Tracking Controller
 */
app.controller('returnController', [
    '$scope', '$location', 'orderService', 'authService', 'productService', '$q',
    function($scope, $location, orderService, authService, productService, $q) {
        
        $scope.returnsList = [];

        $scope.initReturns = function() {
            if (!authService.isLoggedIn()) {
                $location.path('/login');
                return;
            }
            $scope.loadReturns();
        };

        $scope.loadReturns = function() {
            orderService.getCustomerReturns().then(function(returns) {
                // Fetch product details for return displays
                var promises = returns.map(function(ret) {
                    return productService.getProduct(ret.productId)
                        .then(function(prod) {
                            ret.productName = prod.productName;
                            ret.mainImageURL = prod.mainImageURL;
                            return ret;
                        })
                        .catch(function() {
                            ret.productName = 'Product #' + ret.productId;
                            return ret;
                        });
                });

                $q.all(promises).then(function(resolvedReturns) {
                    $scope.returnsList = resolvedReturns.sort(function(a, b) {
                        return new Date(b.requestedAt) - new Date(a.requestedAt);
                    });
                });
            });
        };
    }
]);
