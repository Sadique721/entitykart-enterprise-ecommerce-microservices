/**
 * Orders and Returns Service
 */
app.service('orderService', ['apiService', 'authService', '$q', function(apiService, authService, $q) {
    var self = this;

    function getCustomerId() {
        var user = authService.getCurrentUser();
        return user ? user.id : null;
    }

    // --- Orders ---

    this.getOrder = function(orderId) {
        return apiService.get('/api/orders/' + orderId)
            .then(function(response) {
                return response.data;
            })
            .catch(function() {
                var mockOrders = JSON.parse(localStorage.getItem('ekMockOrders') || '[]');
                var order = mockOrders.find(function(o) { return o.orderId == orderId; });
                if (order) return order;
                throw new Error('Order not found');
            });
    };

    this.getCustomerOrders = function() {
        var customerId = getCustomerId();
        if (!customerId) return $q.resolve([]);

        return apiService.get('/api/orders/customer/' + customerId)
            .then(function(response) {
                return response.data;
            })
            .catch(function() {
                var mockOrders = JSON.parse(localStorage.getItem('ekMockOrders') || '[]');
                return mockOrders.filter(function(o) { return o.customerId == customerId; });
            });
    };

    this.updateOrderStatus = function(orderId, status) {
        return apiService.put('/api/orders/' + orderId + '/status', null, { status: status })
            .then(function(response) {
                return response.data;
            })
            .catch(function() {
                var mockOrders = JSON.parse(localStorage.getItem('ekMockOrders') || '[]');
                var order = mockOrders.find(function(o) { return o.orderId == orderId; });
                if (order) {
                    order.orderStatus = status;
                    if (status === 'DELIVERED') {
                        order.paymentStatus = 'PAID';
                    }
                    localStorage.setItem('ekMockOrders', JSON.stringify(mockOrders));
                    return order;
                }
                throw new Error('Order not found');
            });
    };

    this.getAllOrders = function() {
        // Fetch all orders (admin feature)
        // Spring Boot may or may not expose a global get all orders, we fallback to our mock
        return apiService.get('/api/orders/all')
            .then(function(response) {
                return response.data;
            })
            .catch(function() {
                return JSON.parse(localStorage.getItem('ekMockOrders') || '[]');
            });
    };

    // --- Returns & Refunds ---

    this.requestReturn = function(returnRequest) {
        // Transform UI model to backend ReturnRequest
        var payload = {
            orderId: returnRequest.orderId,
            productId: returnRequest.productId,
            quantity: returnRequest.quantity,
            reason: returnRequest.reason + (returnRequest.comments ? " - " + returnRequest.comments : "")
        };
        
        return apiService.post('/api/returns', payload)
            .then(function(response) {
                var ret = response.data;
                ret.returnStatus = ret.status === 'PENDING' ? 'REQUESTED' : ret.status;
                ret.refundStatus = ret.status === 'REFUNDED' ? 'PROCESSED' : (ret.status === 'APPROVED' ? 'PENDING' : 'NONE');
                ret.requestedAt = ret.createdAt;
                return ret;
            })
            .catch(function() {
                var mockReturns = JSON.parse(localStorage.getItem('ekMockReturns') || '[]');
                
                // Verify duplicate request
                var exists = mockReturns.some(function(r) { return r.orderId == payload.orderId && r.productId == payload.productId; });
                if (exists) {
                    throw { data: { message: 'Return already requested for this item' } };
                }

                var newReturn = {
                    returnId: Date.now(),
                    orderId: payload.orderId,
                    customerId: getCustomerId(),
                    productId: payload.productId,
                    quantity: payload.quantity,
                    reason: payload.reason,
                    returnStatus: 'REQUESTED',
                    refundStatus: 'PENDING',
                    requestedAt: new Date().toISOString(),
                    processedAt: null,
                    adminComments: null
                };

                mockReturns.push(newReturn);
                localStorage.setItem('ekMockReturns', JSON.stringify(mockReturns));
                return newReturn;
            });
    };

    this.getCustomerReturns = function() {
        var customerId = getCustomerId();
        if (!customerId) return $q.resolve([]);

        return apiService.get('/api/returns/my')
            .then(function(response) {
                var data = response.data;
                return data.map(function(ret) {
                    ret.returnStatus = ret.status === 'PENDING' ? 'REQUESTED' : ret.status;
                    ret.refundStatus = ret.status === 'REFUNDED' ? 'PROCESSED' : (ret.status === 'APPROVED' ? 'PENDING' : 'NONE');
                    ret.requestedAt = ret.createdAt;
                    return ret;
                });
            })
            .catch(function() {
                var mockReturns = JSON.parse(localStorage.getItem('ekMockReturns') || '[]');
                return mockReturns.filter(function(r) { return r.customerId == customerId; });
            });
    };

    this.getReturnsByStatus = function(status) {
        var params = {};
        var backendStatus = status;
        if (status === 'REQUESTED') backendStatus = 'PENDING';
        if (backendStatus) params.status = backendStatus;
        
        return apiService.get('/api/admin/returns', params)
            .then(function(response) {
                var data = response.data;
                return data.map(function(ret) {
                    ret.returnStatus = ret.status === 'PENDING' ? 'REQUESTED' : ret.status;
                    ret.refundStatus = ret.status === 'REFUNDED' ? 'PROCESSED' : (ret.status === 'APPROVED' ? 'PENDING' : 'NONE');
                    ret.requestedAt = ret.createdAt;
                    return ret;
                });
            })
            .catch(function() {
                var mockReturns = JSON.parse(localStorage.getItem('ekMockReturns') || '[]');
                if (status) {
                    return mockReturns.filter(function(r) { return r.returnStatus === status; });
                }
                return mockReturns;
            });
    };

    this.approveReturn = function(returnId, adminComments) {
        var payload = {
            decision: "APPROVED",
            adminNote: adminComments
        };
        return apiService.patch('/api/admin/returns/' + returnId + '/decision', payload)
            .then(function(response) {
                var ret = response.data;
                ret.returnStatus = ret.status === 'PENDING' ? 'REQUESTED' : ret.status;
                ret.refundStatus = ret.status === 'REFUNDED' ? 'PROCESSED' : (ret.status === 'APPROVED' ? 'PENDING' : 'NONE');
                ret.requestedAt = ret.createdAt;
                return ret;
            })
            .catch(function() {
                var mockReturns = JSON.parse(localStorage.getItem('ekMockReturns') || '[]');
                var ret = mockReturns.find(function(r) { return r.returnId == returnId; });
                if (ret) {
                    ret.returnStatus = 'APPROVED';
                    ret.adminComments = adminComments;
                    ret.processedAt = new Date().toISOString();
                    localStorage.setItem('ekMockReturns', JSON.stringify(mockReturns));
                    return ret;
                }
                throw new Error('Return request not found');
            });
    };

    this.rejectReturn = function(returnId, adminComments) {
        var payload = {
            decision: "REJECTED",
            adminNote: adminComments,
            rejectionReason: adminComments
        };
        return apiService.patch('/api/admin/returns/' + returnId + '/decision', payload)
            .then(function(response) {
                var ret = response.data;
                ret.returnStatus = ret.status === 'PENDING' ? 'REQUESTED' : ret.status;
                ret.refundStatus = ret.status === 'REFUNDED' ? 'PROCESSED' : (ret.status === 'APPROVED' ? 'PENDING' : 'NONE');
                ret.requestedAt = ret.createdAt;
                return ret;
            })
            .catch(function() {
                var mockReturns = JSON.parse(localStorage.getItem('ekMockReturns') || '[]');
                var ret = mockReturns.find(function(r) { return r.returnId == returnId; });
                if (ret) {
                    ret.returnStatus = 'REJECTED';
                    ret.adminComments = adminComments;
                    ret.processedAt = new Date().toISOString();
                    localStorage.setItem('ekMockReturns', JSON.stringify(mockReturns));
                    return ret;
                }
                throw new Error('Return request not found');
            });
    };

    this.processRefund = function(returnId) {
        return apiService.post('/api/admin/returns/' + returnId + '/refund')
            .then(function(response) {
                var ret = response.data;
                ret.returnStatus = ret.status === 'PENDING' ? 'REQUESTED' : ret.status;
                ret.refundStatus = ret.status === 'REFUNDED' ? 'PROCESSED' : (ret.status === 'APPROVED' ? 'PENDING' : 'NONE');
                ret.requestedAt = ret.createdAt;
                return ret;
            })
            .catch(function() {
                var mockReturns = JSON.parse(localStorage.getItem('ekMockReturns') || '[]');
                var ret = mockReturns.find(function(r) { return r.returnId == returnId; });
                if (ret) {
                    if (ret.returnStatus !== 'APPROVED') {
                        throw new Error('Return must be approved before refund processing');
                    }
                    ret.refundStatus = 'PROCESSED';
                    ret.returnStatus = 'REFUNDED';
                    localStorage.setItem('ekMockReturns', JSON.stringify(mockReturns));
                    return ret;
                }
                throw new Error('Return request not found');
            });
    };
}]);
