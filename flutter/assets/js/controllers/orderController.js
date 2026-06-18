/**
 * Customer Orders Management Controller
 * v1.5.0 — Added cancel order, download invoice features
 */
app.controller('orderController', [
    '$scope', '$location', 'orderService', 'authService', 'apiService',
    function($scope, $location, orderService, authService, apiService) {

        $scope.orders = [];
        $scope.selectedOrder = null;
        $scope.orderFilter = 'ALL';

        // Return request state
        $scope.showReturnModal = false;
        $scope.returnItem = null;
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
                $scope.orders = data.sort(function(a, b) {
                    return new Date(b.orderDate) - new Date(a.orderDate);
                });
            });
        };

        $scope.viewOrderDetails = function(order) {
            $scope.selectedOrder = ($scope.selectedOrder && $scope.selectedOrder.orderId === order.orderId) ? null : order;
        };

        $scope.isStatusReached = function(currentStatus, checkStatus) {
            var statusOrder = ['PENDING_PAYMENT', 'PLACED', 'SHIPPED', 'DELIVERED'];
            var currentIdx = statusOrder.indexOf(currentStatus);
            var checkIdx = statusOrder.indexOf(checkStatus);
            return currentIdx >= checkIdx && currentIdx !== -1;
        };

        // --- Cancel Order ---
        $scope.canCancelOrder = function(order) {
            return order.orderStatus === 'PLACED' || order.orderStatus === 'PENDING_PAYMENT';
        };

        $scope.cancelOrder = function(order, event) {
            if (event) event.stopPropagation();
            if (!confirm('Are you sure you want to cancel order #' + order.orderId + '? This action cannot be undone.')) {
                return;
            }
            orderService.updateOrderStatus(order.orderId, 'CANCELLED')
                .then(function() {
                    order.orderStatus = 'CANCELLED';
                    $scope.$emit('showToast', {
                        title: 'Order Cancelled',
                        message: 'Order #' + order.orderId + ' has been cancelled. Refund will be processed within 5-7 business days.',
                        type: 'info'
                    });
                    $scope.loadOrders();
                })
                .catch(function() {
                    $scope.$emit('showToast', {
                        title: 'Cancellation Failed',
                        message: 'Unable to cancel this order. Please contact support.',
                        type: 'error'
                    });
                });
        };

        // --- Download / Print Invoice ---
        $scope.downloadInvoice = function(order, event) {
            if (event) event.stopPropagation();

            var user = authService.getCurrentUser();
            var invoiceHtml = '\
<!DOCTYPE html>\
<html>\
<head>\
  <meta charset="UTF-8">\
  <title>Invoice - Order #' + order.orderId + '</title>\
  <style>\
    body { font-family: Arial, sans-serif; margin: 40px; color: #333; }\
    .header { border-bottom: 2px solid #6366f1; padding-bottom: 15px; margin-bottom: 20px; }\
    .logo { font-size: 28px; font-weight: 700; color: #6366f1; }\
    .logo span { color: #f97316; }\
    h2 { color: #374151; margin: 0 0 5px; }\
    .meta-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 20px; margin: 20px 0; }\
    .meta-box { background: #f9fafb; padding: 15px; border-radius: 8px; }\
    .meta-box label { font-weight: 600; font-size: 0.8rem; color: #6b7280; text-transform: uppercase; }\
    table { width: 100%; border-collapse: collapse; margin: 20px 0; }\
    th { background: #6366f1; color: white; padding: 10px 12px; text-align: left; }\
    td { padding: 10px 12px; border-bottom: 1px solid #e5e7eb; }\
    tr:nth-child(even) td { background: #f9fafb; }\
    .totals-row { display: flex; justify-content: flex-end; }\
    .totals { min-width: 300px; }\
    .total-line { display: flex; justify-content: space-between; padding: 6px 0; }\
    .total-line.grand { font-size: 1.2rem; font-weight: 700; color: #6366f1; border-top: 2px solid #6366f1; padding-top: 10px; margin-top: 5px; }\
    .footer { text-align: center; margin-top: 40px; color: #9ca3af; font-size: 0.85rem; }\
  </style>\
</head>\
<body>\
<div class="header">\
  <div class="logo">Entity<span>kart</span></div>\
  <p style="margin:5px 0;color:#6b7280;">Tax Invoice / Bill of Supply</p>\
</div>\
<h2>Invoice for Order #' + order.orderId + '</h2>\
<div class="meta-grid">\
  <div class="meta-box"><label>Order Date</label><br>' + new Date(order.orderDate).toLocaleDateString('en-IN', { day: '2-digit', month: 'long', year: 'numeric' }) + '</div>\
  <div class="meta-box"><label>Payment Status</label><br>' + (order.paymentStatus || 'PENDING') + '</div>\
  <div class="meta-box"><label>Customer</label><br>' + (user ? user.name : 'Customer') + '<br>' + (user ? user.email : '') + '</div>\
  <div class="meta-box"><label>Order Status</label><br>' + order.orderStatus + '</div>\
</div>\
<table>\
  <thead><tr><th>#</th><th>Product</th><th>Qty</th><th>Unit Price</th><th>Subtotal</th></tr></thead>\
  <tbody>';

            if (order.items && order.items.length > 0) {
                order.items.forEach(function(item, idx) {
                    invoiceHtml += '<tr>\
                      <td>' + (idx + 1) + '</td>\
                      <td>' + (item.productName || 'Product #' + item.productId) + '</td>\
                      <td>' + item.quantity + '</td>\
                      <td>₹' + parseFloat(item.price || 0).toFixed(2) + '</td>\
                      <td>₹' + parseFloat(item.subtotal || (item.price * item.quantity) || 0).toFixed(2) + '</td>\
                    </tr>';
                });
            }

            var subtotal = parseFloat(order.totalAmount || 0);
            var gst = subtotal * 0.18;
            var total = subtotal + gst;

            invoiceHtml += '</tbody></table>\
<div class="totals-row"><div class="totals">\
  <div class="total-line"><span>Subtotal</span><span>₹' + subtotal.toFixed(2) + '</span></div>\
  <div class="total-line"><span>GST (18%)</span><span>₹' + gst.toFixed(2) + '</span></div>\
  <div class="total-line"><span>Shipping</span><span>FREE</span></div>\
  <div class="total-line grand"><span>Grand Total</span><span>₹' + total.toFixed(2) + '</span></div>\
</div></div>\
<div class="footer">\
  <p>Thank you for shopping with EntityKart!</p>\
  <p>For support: support@entitykart.com | +1 (555) 019-2834</p>\
  <p>This is a computer-generated invoice and does not require a signature.</p>\
</div>\
</body></html>';

            var blob = new Blob([invoiceHtml], { type: 'text/html' });
            var url = URL.createObjectURL(blob);
            var link = document.createElement('a');
            link.href = url;
            link.download = 'Invoice-Order-' + order.orderId + '.html';
            document.body.appendChild(link);
            link.click();
            document.body.removeChild(link);
            URL.revokeObjectURL(url);

            $scope.$emit('showToast', {
                title: 'Invoice Downloaded',
                message: 'Invoice for Order #' + order.orderId + ' saved to your downloads.',
                type: 'success'
            });
        };

        // --- Return Requests ---

        $scope.openReturnDialog = function(item, event) {
            if (event) event.stopPropagation();
            $scope.returnItem = item;
            $scope.returnRequest.orderId = $scope.selectedOrder.orderId;
            $scope.returnRequest.productId = item.productId;
            $scope.returnRequest.quantity = item.quantity;
            $scope.returnRequest.reason = '';
            $scope.returnRequest.comments = '';
            $scope.showReturnModal = true;
        };

        $scope.closeReturnDialog = function() {
            $scope.showReturnModal = false;
            $scope.returnItem = null;
        };

        $scope.submitReturnRequest = function() {
            if (!$scope.returnRequest.reason) {
                $scope.$emit('showToast', { title: 'Select Reason', message: 'Please select a reason for the return.', type: 'error' });
                return;
            }
            orderService.requestReturn($scope.returnRequest)
                .then(function() {
                    $scope.$emit('showToast', { title: 'Return Requested', message: 'Your return request has been submitted for review.', type: 'success' });
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
