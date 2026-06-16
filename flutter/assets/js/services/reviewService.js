/**
 * Product Review Service
 */
app.service('reviewService', ['apiService', 'authService', '$q', function(apiService, authService, $q) {
    var self = this;

    function getCustomerId() {
        var user = authService.getCurrentUser();
        return user ? user.id : null;
    }

    // Default mock reviews for items
    var defaultReviews = {
        101: [
            { reviewId: 1, productId: 101, customerId: 4, customerName: 'Rohan Sharma', rating: 5, comment: 'Absolutely brilliant gaming mouse! The latency is virtually zero and the shape fits my hand perfectly. Battery life is amazing too.', createdAt: new Date(Date.now() - 3600000 * 24 * 3).toISOString() },
            { reviewId: 2, productId: 101, customerId: 5, customerName: 'Sneha Patel', rating: 4, comment: 'Super smooth feet, glider pads are high quality. Software customization could be slightly better, but physical hardware is flawless.', createdAt: new Date(Date.now() - 3600000 * 24 * 12).toISOString() }
        ],
        102: [
            { reviewId: 3, productId: 102, customerId: 6, customerName: 'Kabir Dev', rating: 5, comment: 'Active noise cancelling is top-tier. Compares easily with brands twice the price. Rich bass response and highly detailed treble.', createdAt: new Date(Date.now() - 3600000 * 24 * 2).toISOString() }
        ]
    };

    this.createReview = function(reviewRequest) {
        // reviewRequest structure: productId, customerId, rating, comment
        reviewRequest.customerId = getCustomerId();
        var user = authService.getCurrentUser();
        var customerName = user ? user.name : 'Verified Customer';

        return apiService.post('/api/reviews', reviewRequest)
            .then(function(response) {
                return response.data;
            })
            .catch(function() {
                var storedReviews = JSON.parse(localStorage.getItem('ekMockReviewsList') || '{}');
                
                var prodReviews = storedReviews[reviewRequest.productId] || defaultReviews[reviewRequest.productId] || [];
                
                var newReview = {
                    reviewId: Date.now(),
                    productId: reviewRequest.productId,
                    customerId: reviewRequest.customerId,
                    customerName: customerName,
                    rating: reviewRequest.rating,
                    comment: reviewRequest.comment,
                    createdAt: new Date().toISOString()
                };

                prodReviews.unshift(newReview);
                storedReviews[reviewRequest.productId] = prodReviews;
                localStorage.setItem('ekMockReviewsList', JSON.stringify(storedReviews));
                
                return newReview;
            });
    };

    this.getProductReviews = function(productId) {
        return apiService.get('/api/reviews/product/' + productId)
            .then(function(response) {
                return response.data.content || response.data;
            })
            .catch(function() {
                var storedReviews = JSON.parse(localStorage.getItem('ekMockReviewsList') || '{}');
                return storedReviews[productId] || defaultReviews[productId] || [];
            });
    };

    this.getProductStats = function(productId) {
        return apiService.get('/api/reviews/product/' + productId + '/stats')
            .then(function(response) {
                return response.data;
            })
            .catch(function() {
                return self.getProductReviews(productId).then(function(reviews) {
                    var totalRating = 0;
                    var ratingCount = reviews.length;
                    var counts = {1:0, 2:0, 3:0, 4:0, 5:0};

                    reviews.forEach(function(r) {
                        totalRating += r.rating;
                        if (counts[r.rating] !== undefined) {
                            counts[r.rating]++;
                        }
                    });

                    var avg = ratingCount > 0 ? (totalRating / ratingCount) : 0;

                    return {
                        productId: productId,
                        averageRating: Math.round(avg * 10) / 10,
                        totalReviews: ratingCount,
                        ratingBreakdown: counts
                    };
                });
            });
    };
}]);
